package com.shootoff.camera;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xuggle.mediatool.IMediaReader;
import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.MediaListenerAdapter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.mediatool.event.IVideoPictureEvent;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IPixelFormat;
import com.xuggle.xuggler.IVideoPicture;
import com.xuggle.xuggler.video.ConverterFactory;
import com.xuggle.xuggler.video.IConverter;

public class RollingRecorder {
	private final Logger logger = LoggerFactory.getLogger(RollingRecorder.class);

	private final ICodec.ID codec;
	private final String extension;
	private final String sessionName;
	private final String cameraName;

	private long startTime;
	private long timestamp;
	private long timeOffset = 0;
	private File relativeVideoFile;
	private File videoFile;
	private IMediaWriter videoWriter;
	private final Object videoWriterLock = new Object();
	private boolean isFirstShotFrame = true;
	private boolean forking = false;
	private boolean recording = true;

	private final List<IVideoPicture> bufferedFrames = new ArrayList<IVideoPicture>();

	private int recordWidth;
	private int recordHeight;

	public RollingRecorder(ICodec.ID codec, String extension, String sessionName, String cameraName,
			CameraManager cameraManager) {
		this.codec = codec;
		this.extension = extension;
		this.sessionName = sessionName;
		this.cameraName = cameraName;

		recordWidth = cameraManager.getFeedWidth();
		recordHeight = cameraManager.getFeedHeight();

		startTime = System.currentTimeMillis();
		relativeVideoFile = new File(
				sessionName + File.separator + "rolling" + String.valueOf(System.nanoTime()) + extension);
		videoFile = new File(System.getProperty("shootoff.sessions") + File.separator + relativeVideoFile.getPath());

		videoWriter = ToolFactory.makeWriter(videoFile.getPath());
		videoWriter.addVideoStream(0, 0, codec, recordWidth, recordHeight);

		logger.debug("Started recording new rolling video: {}", videoFile.getName());
	}

	public void recordFrame(BufferedImage frame) {
		BufferedImage image = ConverterFactory.convertToType(frame, BufferedImage.TYPE_3BYTE_BGR);
		IConverter converter = ConverterFactory.createConverter(image, IPixelFormat.Type.YUV420P);

		timestamp = (System.currentTimeMillis() - startTime) + timeOffset;

		IVideoPicture f = converter.toPicture(image, timestamp * 1000);
		f.setKeyFrame(isFirstShotFrame);
		f.setQuality(0);

		if (forking) {
			synchronized (bufferedFrames) {
				bufferedFrames.add(f);
			}
		} else {
			isFirstShotFrame = false;

			synchronized (videoWriterLock) {
				if (recording) videoWriter.encodeVideo(0, f);
			}

			if (timestamp >= ShotRecorder.RECORD_LENGTH * 3) {
				logger.debug("Rolling video file {}, timestamp = {} ms", relativeVideoFile.getPath(), timestamp);
				fork(false);
			}
		}
	}

	private ForkContext fork(boolean keepOld) {
		forking = true;

		synchronized (videoWriterLock) {
			if (videoWriter.isOpen()) videoWriter.close();
		}

		File relativeVideoFile;
		if (!keepOld) {
			relativeVideoFile = new File(
					sessionName + File.separator + "rolling" + String.valueOf(System.nanoTime()) + extension);
		} else {
			relativeVideoFile = new File(sessionName + File.separator + String.valueOf(System.nanoTime()) + extension);
		}

		File videoFile = new File(
				System.getProperty("shootoff.sessions") + File.separator + relativeVideoFile.getPath());

		IMediaReader reader = ToolFactory.makeReader(this.videoFile.getPath());
		reader.open();
		long startCutTimestamp = (reader.getContainer().getDuration() / 1000) - ShotRecorder.RECORD_LENGTH;
		Cutter cutter = new Cutter(videoFile, codec, startCutTimestamp, recordWidth, recordHeight);
		reader.addListener(cutter);

		logger.debug("Forking video file {} to {}, keepOld = {}, start cutting at = {} ms",
				this.relativeVideoFile.getPath(), relativeVideoFile.getPath(), keepOld, startCutTimestamp);

		while (reader.readPacket() == null)
			;

		ForkContext context = new ForkContext(relativeVideoFile, videoFile, cutter.getLastTimestamp(),
				cutter.getMediaWriter());

		if (keepOld) {
			// We aren't rolling this file because it got too big,
			// the video is being forked (probably because there was
			// a shot)
			File rollingRelativeVideoFile = new File(
					sessionName + File.separator + "rolling" + String.valueOf(System.nanoTime()) + extension);
			File rollingVideoFile = new File(
					System.getProperty("shootoff.sessions") + File.separator + rollingRelativeVideoFile.getPath());

			IMediaReader r = ToolFactory.makeReader(this.videoFile.getPath());
			r.open();
			Cutter copy = new Cutter(rollingVideoFile, codec, 0, recordWidth, recordHeight);
			r.addListener(copy);
			while (r.readPacket() == null)
				;

			timeOffset = copy.getLastTimestamp();

			if (!this.videoFile.delete()) {
				logger.warn("Failed to delete expired rolling video file: {}, keepOld = {}", this.videoFile.getPath(),
						keepOld);
			}

			synchronized (videoWriterLock) {
				this.videoWriter = copy.getMediaWriter();
			}
			this.relativeVideoFile = rollingRelativeVideoFile;
			this.videoFile = rollingVideoFile;
		} else {
			// Start adding new frames to the new video as it's the new
			// canonical video file to peel end frames off of.
			if (!this.videoFile.delete()) {
				logger.warn("Failed to delete expired rolling video file: {}, keepOld = {}", this.videoFile.getPath(),
						keepOld);
			}
			this.relativeVideoFile = relativeVideoFile;
			this.videoFile = videoFile;
			synchronized (videoWriterLock) {
				videoWriter = cutter.getMediaWriter();
			}
			timeOffset = cutter.getLastTimestamp();
		}

		synchronized (bufferedFrames) {
			Iterator<IVideoPicture> it = bufferedFrames.iterator();

			while (it.hasNext()) {
				synchronized (videoWriterLock) {
					videoWriter.encodeVideo(0, it.next());
				}
				it.remove();
			}
		}

		startTime = System.currentTimeMillis();

		forking = false;

		return context;
	}

	public ShotRecorder fork() {
		ForkContext context = fork(true);
		return new ShotRecorder(context.getRelativeVideoFile(), context.getVideoFile(), context.getLastTimestamp(),
				context.getVideoWriter(), cameraName);
	}

	private static class ForkContext {
		private final File relativeVideoFile;
		private final File videoFile;
		private final long lastTimestamp;
		private final IMediaWriter videoWriter;

		public ForkContext(File relativeVideoFile, File videoFile, long lastTimestamp, IMediaWriter videoWriter) {
			this.relativeVideoFile = relativeVideoFile;
			this.videoFile = videoFile;
			this.lastTimestamp = lastTimestamp;
			this.videoWriter = videoWriter;
		}

		public File getRelativeVideoFile() {
			return relativeVideoFile;
		}

		public File getVideoFile() {
			return videoFile;
		}

		public long getLastTimestamp() {
			return lastTimestamp;
		}

		public IMediaWriter getVideoWriter() {
			return videoWriter;
		}
	}

	/**
	 * Cut the end of a video off into its own file starting at
	 * startingTimestamp. For example, if you have a 15 second video and the
	 * startingTimestamp is at 10 seconds, this will create a new video that has
	 * the last 5 seconds of the original video.
	 * 
	 * @author phrack
	 */
	private static class Cutter extends MediaListenerAdapter {
		private final IMediaWriter writer;
		private final long startingTimestamp;
		private long startTimestamp = -1;
		private long lastTimestamp;

		public Cutter(File newVideoFile, ICodec.ID codec, long startingTimestamp /* ms */, int recordWidth,
				int recordHeight) {
			this.startingTimestamp = startingTimestamp * 1000;
			writer = ToolFactory.makeWriter(newVideoFile.getPath());
			writer.addVideoStream(0, 0, codec, recordWidth, recordHeight);
		}

		public void onVideoPicture(IVideoPictureEvent event) {
			// < 0 means the file we are rolling off of has < RECORD_LENGTH
			// seconds of footage
			if (event.getTimeStamp() >= startingTimestamp || startingTimestamp < 0) {
				IVideoPicture picture = event.getPicture();

				if (startTimestamp == -1) {
					startTimestamp = picture.getTimeStamp();
				}

				lastTimestamp = picture.getTimeStamp() - startTimestamp;
				picture.setTimeStamp(lastTimestamp);

				writer.encodeVideo(0, picture);
			}
		}

		public IMediaWriter getMediaWriter() {
			return writer;
		}

		public long getLastTimestamp() {
			return lastTimestamp / 1000;
		}
	}

	public void close() {
		recording = false;
		synchronized (videoWriterLock) {
			videoWriter.close();
		}
		if (!videoFile.delete()) {
			logger.warn("Failed to delete expired rolling video file on close: {}", videoFile.getPath());
		}
	}
}
