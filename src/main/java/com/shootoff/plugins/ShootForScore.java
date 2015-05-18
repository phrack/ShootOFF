package com.shootoff.plugins;

import java.util.List;
import java.util.Optional;

import javafx.scene.Group;
import javafx.scene.paint.Color;

import com.shootoff.camera.Shot;
import com.shootoff.targets.TargetRegion;

public class ShootForScore extends TrainingProtocolBase implements TrainingProtocol {
	private final static String POINTS_COL_NAME = "Score";
	private final static int POINTS_COL_WIDTH = 60;
	
	private int redScore = 0;
	private int greenScore = 0;
	
	public ShootForScore() {}
	
	public ShootForScore(List<Group> targets) {
		super(targets);
	}
	
	@Override
	public void init() {
		super.addShotTimerColumn(POINTS_COL_NAME, POINTS_COL_WIDTH);
	}
	
	/**
	 * Returns the score for the red player. This method exists to make this
	 * protocol easier to test.
	 * 
	 * @return	red's score
	 */
	protected int getRedScore() {
		return redScore;
	}
	
	/**
	 * Returns the score for the green player. This method exists to make this
	 * protocol easier to test.
	 * 
	 * @return	green's score
	 */
	protected int getGreenScore() {
		return greenScore;
	}
	
	@Override
	public ProtocolMetadata getInfo() {	    	    
		return new ProtocolMetadata("Shoot for Score", "1.0", "phrack",
	    	    "This protocol works with targets that have score tags "
	    	    		+ "assigned to regions. Any time a target region is hit, "
	    	    		+ "the number of points assigned to that region are added "
	    	    		+ "to your total score.");
	}

	@Override
	public void shotListener(Shot shot, Optional<TargetRegion> hitRegion) {
		if (!hitRegion.isPresent()) return;
		
		if (hitRegion.get().tagExists("points")) {
	        super.setShotTimerColumnText(POINTS_COL_NAME, hitRegion.get().getTag("points"));
	        
			if (shot.getColor().equals(Color.RED)) {
				redScore += Integer.parseInt(hitRegion.get().getTag("points"));
			} else if (shot.getColor().equals(Color.GREEN)) {
				greenScore += Integer.parseInt(hitRegion.get().getTag("points"));
			}
		}
		
        String message = "score: 0";

        if (redScore > 0 && greenScore > 0) {
        	message = String.format("red score: %d\ngreen score: %d", redScore, greenScore);
        }
        
        if (redScore > 0 && greenScore > 0) {
        	message = String.format("red score: %d\ngreen score: %d", redScore, greenScore);
        } else if (redScore > 0) {
        	message = String.format("red score: %d", redScore);
        } else if (greenScore > 0) {
        	message = String.format("green score: %d", greenScore);
        }
        
        super.showTextOnFeed(message);
	}

	@Override
	public void reset(List<Group> targets) {
		redScore = 0;
		greenScore = 0;
		super.showTextOnFeed("score: 0");
	}

	@Override
	public void destroy() {
		super.destroy();
	}

}
