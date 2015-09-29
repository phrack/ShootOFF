package com.shootoff.gui;

import java.io.InputStream;

import javafx.scene.image.Image;

public class LocatedImage extends Image {
	private final String url;
	private final boolean isResource;
	
	public LocatedImage(String url) {
		super(url);
		this.url = url;
		isResource = false;
	}
	
	public LocatedImage(InputStream is, String resourceName) {
		super(is);
		this.url = resourceName;
		this.isResource = true;
	}
	
	public String getURL() {
		return url;
	}
	
	public boolean isResource() {
		return isResource;
	}
}
