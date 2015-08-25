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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.shootoff.camera.Camera;
import com.shootoff.config.Configuration;
import com.shootoff.config.ConfigurationException;
import com.shootoff.gui.controller.ShootOFFController;
import com.shootoff.plugins.TextToSpeech;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class Main extends Application {
	private final String RESOURCES_JAR_NAME = "shootoff-writable-resources.jar";
	private File resourcesFile;
	private Stage primaryStage;
	
	/**
	 * Writable resources (e.g. shootoff.properties, sounds, targets, etc.) cannot be included in 
	 * JAR files for a Webstart applications, thus we download them from a remote URL and extract 
	 * them locally if necessary.
	 * 
	 * Downloads the file at fileAddress with the assumption that it is a JAR containing
	 * writable resources. If there is an existing JAR with writable resources we
	 * only do the download if the file sizes are different. 
	 * 
	 * @param fileAddress	the url (e.g. http://example.com/file.jar) that contains ShootOFF's writable resources
	 */
	private void downloadWebstartResources(String fileAddress) {
		HttpURLConnection connection = null;
		InputStream stream = null;
		
		try {
			connection = (HttpURLConnection)new URL(fileAddress).openConnection();
			stream = connection.getInputStream();
		} catch (UnknownHostException e) {
			System.err.println("Could not connect to remote host " + e.getMessage() + " to download writable resources.");
			tryRunningShootOFF();
			return;
		} catch (IOException e) {
			if (connection != null) connection.disconnect();

			System.err.println("Failed to get stream to download writable resources file");
			e.printStackTrace();
			tryRunningShootOFF();
			return;
		}
		
		long remoteFileLength = connection.getContentLength();

		if (remoteFileLength == 0) {
			System.err.println("Remote writable resources file query returned 0 len");
		}
		
		if (resourcesFile.exists() && remoteFileLength == resourcesFile.length()) {
			connection.disconnect();
			runShootOFF();
			return;
		}
		
        final InputStream remoteStream = stream;
        Task<Boolean> task = new Task<Boolean>() {
            @Override
            public Boolean call() throws InterruptedException {
    			BufferedInputStream bufferedInputStream = new BufferedInputStream(remoteStream);
    			FileOutputStream fileOutputStream = null;
    			
    			try {
    				fileOutputStream = new FileOutputStream(resourcesFile);
	    	
    				long totalDownloaded = 0;
	    			int count;
	    			byte buffer[] = new byte[1024];
	    	
	    			while ((count = bufferedInputStream.read(buffer, 0, buffer.length)) != -1) {
	    				fileOutputStream.write(buffer, 0, count);
	    				totalDownloaded += count;
	    				updateProgress(((double)totalDownloaded / (double)remoteFileLength) * 100, 100);
	    			}
	    			
	    			fileOutputStream.close();
	    			
	                updateProgress(100, 100);
    			} catch (IOException e) {
    				if (fileOutputStream != null) {
    					try {
    						fileOutputStream.close();
    					} catch (IOException e1) {
    						e1.printStackTrace();
    					}
    				}
    				
    				System.err.println("Failed to download writable resources file");
    				e.printStackTrace();
    				return false;
    			}
    			
                return true;
            }
        };
        
        final ProgressDialog progressDialog = new ProgressDialog("Downloading Resources...", 
        		"Download required resources (targets, sounds, etc.)...", task);
        final HttpURLConnection con = connection;
        task.setOnSucceeded((value) -> {
        		progressDialog.close();
        		con.disconnect();
        		if (task.getValue()) {
        			extractWebstartResources();
        		}
        	});
        
        new Thread(task).start();    
	}
	
	/**
	 * If we could not acquire writable resources for Webstart, see if we have enough
	 * to run anyway.
	 */
	private void tryRunningShootOFF() {
		if (!new File(System.getProperty("shootoff.home") + File.separator + "shootoff.properties").exists()) {
			Alert resourcesAlert = new Alert(AlertType.ERROR);
			resourcesAlert.setTitle("Missing Resources");
			resourcesAlert.setHeaderText("Missing Required Resources!");
			resourcesAlert.setResizable(true);
			resourcesAlert.setContentText("ShootOFF could not acquire the necessary resources to run. Please ensure "
					+ "you have a connection to the Internet and can connect to http://shootoffapp.com and try again.");
			resourcesAlert.showAndWait();
		} else {
			runShootOFF();
		}
	}

	private void extractWebstartResources() {	
		Task<Boolean> task = new Task<Boolean>() {
			@Override
			protected Boolean call() throws Exception {
				JarFile jar = null;
				
				try {
					jar = new JarFile(resourcesFile);
					
					Enumeration<JarEntry> enumEntries = jar.entries();
					int fileCount = 0;
					while (enumEntries.hasMoreElements()) {
						JarEntry entry = (JarEntry)enumEntries.nextElement();
						if (!entry.getName().startsWith("META-INF") && !entry.isDirectory()) fileCount++;
					}
					
					enumEntries = jar.entries();
					int currentCount = 0;
					while (enumEntries.hasMoreElements()) {
					    JarEntry entry = (JarEntry)enumEntries.nextElement();
					    
					    if (entry.getName().startsWith("META-INF")) continue;
					    
					    File f = new File(System.getProperty("shootoff.home") + File.separator + entry.getName());
					    if (entry.isDirectory()) {
					        if (!f.mkdir()) 
					        	throw new IOException("Failed to make directory while extracting JAR: " + entry.getName());
					    } else {			    	
						    InputStream is = jar.getInputStream(entry);
						    FileOutputStream fos = new FileOutputStream(f);
						    while (is.available() > 0) {
						        fos.write(is.read());
						    }
						    fos.close();
						    is.close();
						    
						    currentCount++;
						    updateProgress(((double)currentCount / (double)fileCount) * 100, 100);
					    }
					}
					
					updateProgress(100, 100);
				} catch (IOException e) {
					e.printStackTrace();
					return false;
				} finally {
					try {
						if (jar != null) jar.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				
				return true;
			}
		};
		
        final ProgressDialog progressDialog = new ProgressDialog("Extracting Resources...", 
        		"Extracting required resources (targets, sounds, etc.)...", task);
        task.setOnSucceeded((value) -> {
        		progressDialog.close();
        		if (task.getValue()) {
        			runShootOFF();
        		}
        	});
        
        new Thread(task).start();    
	}
	
    public static class ProgressDialog {
        private final Stage stage = new Stage();
        private final Label messageLabel = new Label();
        private final ProgressBar pb = new ProgressBar();
        private final ProgressIndicator pin = new ProgressIndicator();

        public ProgressDialog(String dialogTitle, String dialogMessage, final Task<?> task) {
            stage.setTitle(dialogTitle);
            stage.initModality(Modality.APPLICATION_MODAL);

            pb.setProgress(-1F);
            pin.setProgress(-1F);
            
            messageLabel.setText(dialogMessage);

            final HBox hb = new HBox();
            hb.setSpacing(5);
            hb.setAlignment(Pos.CENTER);
            hb.getChildren().addAll(pb, pin);

            pb.prefWidthProperty().bind(hb.widthProperty().subtract(hb.getSpacing() * 6));
            
            BorderPane bp = new BorderPane();
            bp.setTop(messageLabel);
            bp.setBottom(hb);
            
            Scene scene = new Scene(bp);
            
            stage.setScene(scene);
            stage.show();
            
            pb.progressProperty().bind(task.progressProperty());
            pin.progressProperty().bind(task.progressProperty());
        }
        
        public void close() {
        	stage.close();
        }
    }
	
    public void runShootOFF() {
		String[] args = getParameters().getRaw().toArray(new String[getParameters().getRaw().size()]);
		Configuration config;
		try {
			config = new Configuration(System.getProperty("shootoff.home") + File.separator + 
					"shootoff.properties", args);
		} catch (IOException | ConfigurationException e) {
			e.printStackTrace();
			return;
		}
		
		// This initializes the TTS engine
		TextToSpeech.say("");
		
		try {
			FXMLLoader loader = new FXMLLoader(Main.class.getResource("/com/shootoff/gui/ShootOFF.fxml"));
		    loader.load();   
			
			Scene scene = new Scene(loader.getRoot());
			
			primaryStage.setTitle("ShootOFF");
			primaryStage.setScene(scene);
			((ShootOFFController)loader.getController()).init(config);
			primaryStage.show();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
    }
    
	@Override
	public void start(Stage primaryStage) {
		this.primaryStage = primaryStage;
		
		if (System.getProperty("javawebstart.version", null) != null) {
			File shootoffHome = new File(System.getProperty("user.home") + File.separator + ".shootoff");
			
			if (!shootoffHome.exists()) {
				if (!shootoffHome.mkdirs()) {
					Alert homeAlert = new Alert(AlertType.ERROR);
					homeAlert.setTitle("No ShootOFF Home");
					homeAlert.setHeaderText("Missing ShootOFF's Home Directory!");
					homeAlert.setResizable(true);
					homeAlert.setContentText("ShootOFF's home directory " + shootoffHome.getPath() + " "
							+ "does not exist and could not be created. Now closing...");
					homeAlert.showAndWait();
					return;
				}
			}
			
			System.setProperty("shootoff.home", shootoffHome.getAbsolutePath());
			
			resourcesFile = new File(System.getProperty("shootoff.home") + File.separator + RESOURCES_JAR_NAME);
			downloadWebstartResources("http://shootoffapp.com/jws/" + RESOURCES_JAR_NAME);
		} else {
			System.setProperty("shootoff.home", System.getProperty("user.dir"));
			runShootOFF();
		}
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