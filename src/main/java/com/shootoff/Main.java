/*
 * ShootOFF - Software for Laser Dry Fire Training
 * Copyright (C) 2015 phrack
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

package com.shootoff;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.shootoff.camera.Camera;
import com.shootoff.config.Configuration;
import com.shootoff.config.ConfigurationException;
import com.shootoff.gui.controller.ShootOFFController;
import com.shootoff.plugins.TextToSpeech;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
	// For Java Web Start we include a JAR that has our writable resources (shootoff.properties,
	// sounds folder, and targets folder). We need to extract it into the shootoff 
	// home directory if we are missing those resources or the resources in the JWS cache
	// are newer.
	private void extractWebstartResources() {
		final String resourcesPath = "shootoff-writable-resources.jar";
		final InputStream resources = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcesPath);

		if (resources != null) {
			String shootoffHome = System.getProperty("shootoff.home");
			
			File writableResources = new File(shootoffHome + File.separator + "shootoff-writable-resources.jar");
			
			long oldLength = 0;
			if (writableResources.exists()) oldLength = writableResources.length();
			
			// Update the writable resources jar in ShootOFF home and extract it
			long newLength = 0;
			try {
				newLength = Files.copy(resources, writableResources.toPath(), StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
			
			// If there is not a newer version of the writable resources and we already
			// have the configuration file at a minimum, nothing to do
			if (oldLength == newLength && new File(shootoffHome + File.separator + "shootoff.properties").exists()) 
				return;
		
			JarFile jar = null;
			
			try {
				jar = new JarFile(writableResources);
				
				Enumeration<JarEntry> enumEntries = jar.entries();
				while (enumEntries.hasMoreElements()) {
				    JarEntry entry = (java.util.jar.JarEntry) enumEntries.nextElement();
				    
				    if (entry.getName().startsWith("META-INF")) continue;
				    
				    File f = new File(shootoffHome + File.separator + entry.getName());
				    if (entry.isDirectory()) {
				        f.mkdir();
				    } else {				    
					    InputStream is = jar.getInputStream(entry);
					    FileOutputStream fos = new FileOutputStream(f);
					    while (is.available() > 0) {
					        fos.write(is.read());
					    }
					    fos.close();
					    is.close();
				    }
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					if (jar != null) jar.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} else {
			System.err.println("Can't find writable resources JAR");
		}
	}
	
	@Override
	public void start(Stage primaryStage) throws IOException, ConfigurationException {
		if (System.getProperty("javawebstart.version", null) != null) {
			File shootoffHome = new File(System.getProperty("user.home") + File.separator + ".shootoff");
			if (!shootoffHome.exists()) shootoffHome.mkdirs();
			System.setProperty("shootoff.home", shootoffHome.getAbsolutePath());
			extractWebstartResources();
		} else {
			System.setProperty("shootoff.home", System.getProperty("user.dir"));
		}
		
		String[] args = getParameters().getRaw().toArray(new String[getParameters().getRaw().size()]);
		Configuration config = new Configuration(System.getProperty("shootoff.home") + File.separator + 
				"shootoff.properties", args);
		
		// This initializes the TTS engine
		TextToSpeech.say("");
		
		FXMLLoader loader = new FXMLLoader(Main.class.getResource("/com/shootoff/gui/ShootOFF.fxml"));
	    loader.load();   
		
		Scene scene = new Scene(loader.getRoot());
		
		primaryStage.setTitle("ShootOFF");
		primaryStage.setScene(scene);
		((ShootOFFController)loader.getController()).init(config);
		primaryStage.show();
	}
	
	public static void main(String[] args) {
		// Check the comment at the top of the Camera class
		// for more information about this hack
		String os = System.getProperty("os.name"); 
		if (os != null && os.equals("Mac OS X")) {
			Camera.getDefault();
		}
		
		launch(args);
	}
}