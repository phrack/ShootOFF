package com.shootoff.headless;

import com.shootoff.Closeable;
import com.shootoff.headless.protocol.Message;
import com.shootoff.headless.protocol.MessageListener;

public interface HeadlessServer extends Closeable {
	void startReading(ConnectionListener connectionListener, MessageListener messageListener);

	boolean sendMessage(Message message);
}
