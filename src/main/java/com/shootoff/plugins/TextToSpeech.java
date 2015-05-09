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
	private static MaryInterface marytts = null;
	
	protected TextToSpeech() {}
	
	private static void init() throws MaryConfigurationException {
		marytts = new LocalMaryInterface();
		Set<String> voices = marytts.getAvailableVoices();
		marytts.setVoice(voices.iterator().next());
		inited = true;
	}
	
	   
	public static void say(String comment) {
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
}
