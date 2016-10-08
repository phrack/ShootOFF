package com.shootoff;

public interface ObservableCloseable extends Closeable {
	void setOnCloseListener(CloseListener closeListener);

	public interface CloseListener {
		void closing();
	}
}
