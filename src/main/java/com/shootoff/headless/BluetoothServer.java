/*
 * ShootOFF - Software for Laser Dry Fire Training
 * Copyright (C) 2016 phrack
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.shootoff.headless;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.ServiceRegistrationException;
import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.shootoff.headless.protocol.HeartbeatMessage;
import com.shootoff.headless.protocol.Message;
import com.shootoff.headless.protocol.MessageListener;
import com.shootoff.util.TimerPool;
import com.shootoff.util.SwingFXUtils;

import javafx.scene.image.Image;

class BluetoothServer implements HeadlessServer {
	private static final Logger logger = LoggerFactory.getLogger(BluetoothServer.class);
	private static final int HEARTBEAT_INTERVAL = 1000; // ms

	private final AtomicBoolean open = new AtomicBoolean(false);

	private Thread readLoopThread;
	private StreamConnectionNotifier streamConnNotifier;
	private PrintWriter bluetoothOutput;

	BluetoothServer(QRCodeListener qrListener) {
		final Optional<String> bluetoothAddress = getLocalAddress();
		if (!bluetoothAddress.isPresent()) return;

		final Optional<Image> addressQRCode = generateQrCode(bluetoothAddress.get());
		if (!addressQRCode.isPresent()) return;

		if (qrListener != null) qrListener.qrCodeCreated(addressQRCode.get());
	}

	private Optional<String> getLocalAddress() {
		try {
			final LocalDevice localDevice = LocalDevice.getLocalDevice();

			// Insert colons into the address because android needs them
			final StringBuilder addressBuilder = new StringBuilder();
			final String originalAddress = localDevice.getBluetoothAddress();
			for (int i = 0; i < originalAddress.length(); i++) {
				addressBuilder.append(originalAddress.charAt(i));
				if (i > 0 && i < originalAddress.length() - 1 && i % 2 != 0) addressBuilder.append(':');
			}

			return Optional.of(addressBuilder.toString());
		} catch (BluetoothStateException e) {
			logger.error("Failed to access local bluetooth device to fetch its address. Ensure the "
					+ "system's bluetooth service is started with \"sudo systemctl start bluetooth\" "
					+ "and the bluetooth stack is on in the system settings", e);
			return Optional.empty();
		}
	}

	private Optional<Image> generateQrCode(String address) {
		final QRCodeWriter qrCodeWriter = new QRCodeWriter();
		final int width = 300;
		final int height = 300;

		try {
			final BitMatrix byteMatrix = qrCodeWriter.encode(address, BarcodeFormat.QR_CODE, width, height);
			final BufferedImage qrCodeImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			qrCodeImage.createGraphics();

			final Graphics2D graphics = (Graphics2D) qrCodeImage.getGraphics();
			graphics.setColor(Color.WHITE);
			graphics.fillRect(0, 0, width, height);
			graphics.setColor(Color.BLACK);

			for (int i = 0; i < height; i++) {
				for (int j = 0; j < width; j++) {
					if (byteMatrix.get(i, j)) {
						graphics.fillRect(i, j, 1, 1);
					}
				}
			}

			return Optional.of(SwingFXUtils.toFXImage(qrCodeImage, null));
		} catch (WriterException e) {
			logger.error("Failed to encode local bluetooth address as a qr code", e);
			return Optional.empty();
		}
	}

	@Override
	public void startReading(ConnectionListener connectionListener, MessageListener messageListener) {
		final String connectionString = "btspp://localhost:" + new UUID("1101", true) + ";name=ShootOFF SBC";

		try {
			streamConnNotifier = (StreamConnectionNotifier) Connector.open(connectionString);

			final StreamConnection connection = streamConnNotifier.acceptAndOpen();
			open.set(true);

			if (connectionListener != null) connectionListener.connectionEstablished();

			final OutputStream outStream = connection.openOutputStream();
			bluetoothOutput = new PrintWriter(new OutputStreamWriter(outStream));

			final BufferedReader reader = new BufferedReader(new InputStreamReader(connection.openInputStream()));

			readLoopThread = new Thread(() -> {
				while (open.get()) {
					try {
						final String lineRead = reader.readLine();
						logger.trace("Received message via bluetooth: {}", lineRead);
						if (messageListener != null) messageListener.messageReceived(Message.fromJson(lineRead));
					} catch (IOException e) {
						logger.error("Error reading bluetooth socket", e);
					}
				}
			});

			readLoopThread.start();
			startHeartbeat(connectionListener);
		} catch (ServiceRegistrationException e) {
			logger.error("Open /usr/lib/systemd/system/bluetooth.service and ensure bluetoothd is "
					+ "started with --compat. Additionally ensure that /var/run/sdp has o+w: "
					+ "sudo chmod o+w /var/run/sdp", e);
			return;
		} catch (IOException e) {
			logger.error("Error setting up bluetooth read loop", e);
		}
	}

	private void startHeartbeat(ConnectionListener connectionListener) {
		TimerPool.schedule(() -> {
			if (!sendMessage(new HeartbeatMessage())) {
				close();
				connectionListener.bluetoothDisconnected();
			} else {
				startHeartbeat(connectionListener);
			}
		}, HEARTBEAT_INTERVAL);
	}

	@Override
	public boolean sendMessage(Message message) {
		final String jsonMessage = message.toJson();

		logger.trace("Sending message via bluetooth: {}, size: {} kb", jsonMessage, jsonMessage.length() / 1024);

		bluetoothOutput.write(jsonMessage);
		bluetoothOutput.flush();
		return !bluetoothOutput.checkError();
	}

	@Override
	public void close() {
		open.set(false);
		readLoopThread.interrupt();

		try {
			streamConnNotifier.close();
		} catch (IOException e) {
			logger.error("Failed to close bluetooth stream connection notifier", e);
		}

		bluetoothOutput.close();
	}
}
