package com.shootoff.camera;

import java.awt.image.BufferedImage;
import java.io.File;

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
	private long timeOffset = 0;
	private File relativeVideoFile;
	private File videoFile;
	private IMediaWriter videoWriter;
	private boolean isFirstShotFrame = true;
	
	private boolean forking = false;
	
	private boolean haveForked = false;
	
	public RollingRecorder(ICodec.ID codec, String extension, String sessionName, String cameraName) {
		this.codec = codec;
		this.extension = extension;
		this.sessionName = sessionName;
		this.cameraName = cameraName;
		
		startTime = System.currentTimeMillis();
		relativeVideoFile =  new File(sessionName + File.separator + String.valueOf(System.nanoTime()) + extension);
		videoFile = new File(System.getProperty("shootoff.home") + File.separator + "sessions" + File.separator + 
				relativeVideoFile.getPath());
		
		videoWriter = ToolFactory.makeWriter(videoFile.getPath());
		videoWriter.addVideoStream(0, 0, codec, CameraManager.FEED_WIDTH, CameraManager.FEED_HEIGHT);
		
		logger.debug("Started recording new rolling video: {}", videoFile.getName());
	}
	
	public void recordFrame(BufferedImage frame) {
		if (forking) return;
		
		BufferedImage image = ConverterFactory.convertToType(frame, BufferedImage.TYPE_3BYTE_BGR);
		IConverter converter = ConverterFactory.createConverter(image, IPixelFormat.Type.YUV420P);

		long timestamp = (System.currentTimeMillis() - startTime) + timeOffset;
		
		if (haveForked) System.out.println(timestamp);
		
		IVideoPicture f = converter.toPicture(image, timestamp * 1000);
		f.setKeyFrame(isFirstShotFrame);
		f.setQuality(0);
		isFirstShotFrame = false;

		videoWriter.encodeVideo(0, f);
		
		if (!haveForked && timestamp >= ShotRecorder.RECORD_LENGTH * 3) {
			fork(false);
			haveForked = true;
		}
	}
	
	private ForkContext fork(boolean keepOld) {
		forking = true;
		
		videoWriter.close();
		
		File relativeVideoFile =  new File(sessionName + File.separator + String.valueOf(System.nanoTime()) + extension);
		File videoFile = new File(System.getProperty("shootoff.home") + File.separator + "sessions" + File.separator + 
				relativeVideoFile.getPath());
		
		logger.debug("Forking video file {} to {}, keepOld = {}", this.relativeVideoFile.getPath(), 
				relativeVideoFile.getPath(), keepOld);
		
		IMediaReader reader = ToolFactory.makeReader(this.videoFile.getPath());
		reader.open();
		reader.setCloseOnEofOnly(false);
		Cutter cutter = new Cutter(videoFile, codec, (reader.getContainer().getDuration() / 1000) - ShotRecorder.RECORD_LENGTH);
		reader.addListener(cutter);
		
		while (reader.readPacket() == null);
		
		ForkContext context = new ForkContext(relativeVideoFile, videoFile, cutter.getMediaWriter());
		
		if (keepOld) {
			// We aren't rolling this file because it got too big,
			// the video is being forked (probably because there was
			// a shot)
			// TODO: Why isn't this appending to the end?			
			videoWriter = ToolFactory.makeWriter(this.videoFile.getPath(), reader);
			
			IMediaReader r = ToolFactory.makeReader(this.videoFile.getPath());
			r.open();
			
			startTime = System.currentTimeMillis();
			timeOffset = r.getContainer().getDuration();
			
			r.close();
			
			// TODO: Remove debug code
			System.out.println(startTime + " " + timeOffset);
		} else {
			// Start adding new frames to the new video as it's the new
			// canonical video file to peel end frames off of.
			this.videoFile.delete();
			this.relativeVideoFile = relativeVideoFile;
			this.videoFile = videoFile;
			videoWriter = cutter.getMediaWriter();
			startTime = System.currentTimeMillis();
			timeOffset = cutter.getLastTimestamp();
		}
		
		forking = false;
		
		return context;
	}
	
	public ShotRecorder fork() {
		ForkContext context = fork(true);
		return new ShotRecorder(context.getRelativeVideoFile(), context.getVideoFile(), context.getVideoWriter(), cameraName);
	}
	
	private static class ForkContext {
		private final File relativeVideoFile;
		private final File videoFile;
		private final IMediaWriter videoWriter;
	
		public ForkContext(File relativeVideoFile, File videoFile, IMediaWriter videoWriter) {
			this.relativeVideoFile = relativeVideoFile;
			this.videoFile = videoFile;
			this.videoWriter = videoWriter;
		}
		
		public File getRelativeVideoFile() {
			return relativeVideoFile;
		}

		public File getVideoFile() {
			return videoFile;
		}

		public IMediaWriter getVideoWriter() {
			return videoWriter;
		}
	}
	
	private static class Cutter extends MediaListenerAdapter {
		private final IMediaWriter writer;
		private final long startingTimestamp;
		private long startTimestamp = -1;
		private long lastTimestamp;
		
		public Cutter(File newVideoFile, ICodec.ID codec, long startingTimestamp) {
			this.startingTimestamp = startingTimestamp;
			writer = ToolFactory.makeWriter(newVideoFile.getPath());
			writer.addVideoStream(0, 0, codec, CameraManager.FEED_WIDTH, CameraManager.FEED_HEIGHT);
		}
		
		public void onVideoPicture(IVideoPictureEvent event)
		{
			// < 0 means the file we are rolling off of has < RECORD_LENGTH seconds of footage
			if (event.getTimeStamp() / 1000 >= startingTimestamp || startingTimestamp < 0) {
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
		videoWriter.close();
		videoFile.delete();
	}
}
