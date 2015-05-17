/*
 * Copyright (c) 2015 phrack. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package com.shootoff.plugins;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;

import javafx.scene.Group;

/** 
 * This class implements common training protocol operations. All
 * training protocols should extend it.
 * 
 * @author phrack
 */
public class TrainingProtocolBase {
	@SuppressWarnings("unused")
	private List<Group> targets;

	// Only exists to make it easy to call getInfo without having
	// to do a bunch of unnecessary setup
	public TrainingProtocolBase() {}
	
	public TrainingProtocolBase(List<Group> targets) {
		this.targets = targets;
	}
	
	public static void playSound(String soundFilePath) {
		AudioInputStream audioInputStream = null;

		try {
			audioInputStream = AudioSystem.getAudioInputStream(new File(soundFilePath));
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (audioInputStream != null) {
			AudioFormat format = audioInputStream.getFormat();
			DataLine.Info info = new DataLine.Info(Clip.class, format);
			try {
				Clip clip = (Clip) AudioSystem.getLine(info);
				clip.open(audioInputStream);
				clip.start();
			} catch (LineUnavailableException | IOException e) {
				e.printStackTrace();
			}
		} 
	}
}
