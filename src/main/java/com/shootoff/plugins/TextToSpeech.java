/*
 * Copyright (c) 2015 phrack. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package com.shootoff.plugins;

import java.util.Set;

import javax.sound.sampled.AudioInputStream;

import marytts.LocalMaryInterface;
import marytts.MaryInterface;
import marytts.exceptions.MaryConfigurationException;
import marytts.exceptions.SynthesisException;
import marytts.util.data.audio.AudioPlayer;

public final class TextToSpeech {
	private static boolean inited = false;
	private static boolean isSilenced = false;
	private static MaryInterface marytts = null;
	
	protected TextToSpeech() {}
	
	private static void init() throws MaryConfigurationException {
		marytts = new LocalMaryInterface();
		Set<String> voices = marytts.getAvailableVoices();
		marytts.setVoice(voices.iterator().next());
		inited = true;
	}
	
	public static void say(String comment) {		
		if (isSilenced) {
			System.out.println(comment);
			return;
		}
		
		try {
			if(!inited) {
				init();
			}
		
			if (comment.isEmpty()) return;
			
			AudioInputStream audio = marytts.generateAudio(comment);
			AudioPlayer player = new AudioPlayer(audio);
			player.start();
		} catch (MaryConfigurationException | 
				SynthesisException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Allows TTS to be silenced or on. If silenced, instead of 
	 * saying a comment the desired comment will be printed to
	 * stdout. This exists so that components can be easily tested
	 * even if they are reliant on TTS. 
	 * 
	 * @param isSilenced set to <tt>true</tt> if spoken phrases
	 * 					 should instead be printed to stdout,
	 * 					 <tt>false</tt> for normal operation.
	 */
	public static void silence(boolean isSilenced) {
		TextToSpeech.isSilenced = isSilenced;
	}
}