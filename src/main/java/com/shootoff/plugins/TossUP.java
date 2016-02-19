/*TossUP by ifly53e*/

package com.shootoff.plugins;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.HPos;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import com.shootoff.camera.Shot;
import com.shootoff.gui.LocatedImage;
import com.shootoff.gui.Target;
import com.shootoff.targets.ImageRegion;
import com.shootoff.targets.RegionType;
import com.shootoff.targets.TargetRegion;
import java.util.concurrent.Callable;

public class TossUP extends ProjectorTrainingExerciseBase implements TrainingExercise {
	private static int penalty = 5;
	private boolean troubleshootingReset = false;
	final int MAX_TARGETS = 20;
	final int DEFAULT_TARGET_COUNT = 2;//4 - 1;
	final int DEFAULT_TIME_BETWEEN_TARGET_MOVEMENT = 6;//1 - 1;
	final int DEFAULT_MAX_ROUNDS = 19;
	final String DEFAULT_TARGET_STRING = "ISSF";
	final String DEFAULT_SCALE = "three-quarter";
	private int default_target_count_reset = DEFAULT_TARGET_COUNT;
	private int default_time_between_target_movement_reset = DEFAULT_TIME_BETWEEN_TARGET_MOVEMENT;
	private int default_max_rounds_reset = DEFAULT_MAX_ROUNDS;
	private String addTargetString_reset = DEFAULT_TARGET_STRING;
	private String theScale_reset = DEFAULT_SCALE;
	private final static String POINTS_COL_NAME = "Score";
	private final static int POINTS_COL_WIDTH = 60;
	private boolean fromReset = false;
	private String addTargetString = "nothing here";
	private String theScale = "nothing here";
	private int shootCount = 3;
	private int roundCount = 5;
	private int timeBetweenTargetMovement = 9;
	private int misses = 0;
	private int hits = 0;
	private double newScale = 0;
	private int decRoundCount = 0;
	private static int edgeProtection = 50;
	public double targetX = 0;
	public double targetY = 0;
	public double consolidatedX = 0;
	public double consolidatedY = 0;
	public static long beepTimeStatic = 0;
	public List<Point2D> consolidatedList = new ArrayList<Point2D> ();
	public Map<String, List<Point2D>> theMap = new HashMap<String, List<Point2D>>();
	public Map<String, Point2D> targetLocationMap = new HashMap<String, Point2D>();
	public Map<String, TossUPTarget> cttMap = new HashMap<String, TossUPTarget>();
	private double shotX = 0;
	private double shotY = 0;
	private long beepTime = 0;
	private long finishTime = 0;
	private static final List<TossUPTarget> shootTargets = new ArrayList<TossUPTarget>();
	private static final List<TossUPTarget> dontShootTargets = new ArrayList<TossUPTarget>();
	private static final List<Timeline> myTimelineList = new ArrayList<Timeline>();

	private static ProjectorTrainingExerciseBase thisSuper;
	private Timeline targetAnimation;
	private int score = 0;
	private int possiblePoints = 0;

	private boolean testing = false;

	private static Label myLabel = new Label("hello");

	private static int canvasGroupSize = 0;

	public TossUP() {}

	public TossUP(List<Group> targets) {
		super(targets);
		setThisSuper(super.getInstance());
	}

	// For testing
	protected void init(int shootCount, int timeBetweenTargetMovement, int maxVelocity) {

		testing = true;
		this.shootCount = shootCount;
		this.timeBetweenTargetMovement = timeBetweenTargetMovement;
		shootTargets.clear();
		startExercise();

	}

	private static void setThisSuper(ProjectorTrainingExerciseBase thisSuper) {
		TossUP.thisSuper = thisSuper;
	}

	private void initColumn(){
		if (!fromReset){
		super.addShotTimerColumn(POINTS_COL_NAME, POINTS_COL_WIDTH);
		}
	}

	@Override
	public void init() {
		thisSuper.getProjArenaController().setBackground(new LocatedImage("file:\\ShootOFF-master\\src\\main\\resources\\arena\\backgrounds\\hickok45_autumn.gif"));
		initColumn();
		collectSettings();
		startExercise();
	}//end init

	private void startExercise()  {

		long scoreTime = finishTime-beepTime;
		if (scoreTime < 0)scoreTime = 0;
		//super.showTextOnFeed(String.format("Score: %d  Misses: %d Hits: %d Points Possible: %d Deduction: %d", score-((roundCount*shootCount-hits)*penalty),misses,hits,possiblePoints,((roundCount*shootCount-hits)*penalty) ));
		
		if(shootCount == 6){ //possible 45 points
			possiblePoints = 45;
			addTargets(shootTargets,"targets/TossUP_chicken.target",1);
			addTargets(shootTargets,"targets/TossUP_Turkey_Silhouette.target",1);
			addTargets(shootTargets,"targets/TossUP_pig_silhouette.target",1);
			addTargets(shootTargets,"targets/TossUP_ram_silhouette.target",1);
			addTargets(shootTargets,"targets/TossUP_silhouette-plate.target",1);
			addTargets(shootTargets,"targets/TossUP_Pepper_Popper.target",1);
		}
		if(shootCount == 5){ //35 points
			possiblePoints = 35;
			addTargets(shootTargets,"targets/TossUP_Turkey_Silhouette.target",1);
			addTargets(shootTargets,"targets/TossUP_pig_silhouette.target",1);
			addTargets(shootTargets,"targets/TossUP_ram_silhouette.target",1);
			addTargets(shootTargets,"targets/TossUP_silhouette-plate.target",1);
			addTargets(shootTargets,"targets/TossUP_Pepper_Popper.target",1);
		}
		if(shootCount == 4){ //26
			possiblePoints = 26;
			addTargets(shootTargets,"targets/TossUP_pig_silhouette.target",1);
			addTargets(shootTargets,"targets/TossUP_ram_silhouette.target",1);
			addTargets(shootTargets,"targets/TossUP_silhouette-plate.target",1);
			addTargets(shootTargets,"targets/TossUP_Pepper_Popper.target",1);
		}
		if(shootCount == 3){//18
			possiblePoints = 18;
			addTargets(shootTargets,"targets/TossUP_ram_silhouette.target",1);
			addTargets(shootTargets,"targets/TossUP_silhouette-plate.target",1);
			addTargets(shootTargets,"targets/TossUP_Pepper_Popper.target",1);
		}
		if(shootCount == 2){//11
			possiblePoints = 11;
			addTargets(shootTargets,"targets/TossUP_silhouette-plate.target",1);
			addTargets(shootTargets,"targets/TossUP_Pepper_Popper.target",1);
		}
		if(shootCount == 1){//5
			possiblePoints = 5;
			addTargets(shootTargets,"targets/TossUP_Pepper_Popper.target",1);
		}

		myLabel = thisSuper.getProjArenaController().getCanvasManager().addProjectorMessage(String.format("Score: %d", score), Color.YELLOW);

		thisSuper.getProjArenaController().getCanvasManager().hideProjectorMessage(myLabel);

		targetAnimation = new Timeline(new KeyFrame(Duration.millis(timeBetweenTargetMovement * 1000), e -> updateTargets()));
		targetAnimation.setCycleCount(roundCount);

		playSound("sounds/voice/shootoff-makeready.wav");
		try {
			Thread.sleep((long) 3500.);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		playSound("sounds/beep.wav");

		targetAnimation.play();
		updateTargets();

		pauseShotDetection(false);
		beepTime = System.currentTimeMillis();
		beepTimeStatic = beepTime;

	}//end start exercise


//	protected class PlayTheTimeline implements Callable<Void> {
//		Timeline theTimeline = new Timeline();
//
//		public PlayTheTimeline(Timeline t){
//			this.theTimeline.equals(t);
//		}
//		@Override
//		public Void call(){
//			theTimeline.play();//playTheTimeline(theTimeline);
//			return null;
//		}
//
//		public Void call(Timeline timelineToPlay) throws Exception {
//			timelineToPlay.play();
//			return null;
//		}
//	}

	private void collectSettings() {
		super.pauseShotDetection(true);

		final Stage TossUPTargetsStage = new Stage();
		final GridPane TossUPTargetsPane = new GridPane();
		final ColumnConstraints cc = new ColumnConstraints(200);
		final ObservableList<String> targetList = FXCollections.observableArrayList();
		final ComboBox<String> targetListComboBox = new ComboBox<String>(targetList);
		final ObservableList<String> shootCountList = FXCollections.observableArrayList();
		final ObservableList<String> roundCountList = FXCollections.observableArrayList();
		final ComboBox<String> shootTargetsComboBox = new ComboBox<String>(shootCountList);
		final ObservableList<String> maxScale = FXCollections.observableArrayList();
		final ComboBox<String> maxScaleComboBox = new ComboBox<String>(maxScale);
		final ComboBox<String> numberOfRoundsComboBox = new ComboBox<String>(roundCountList);//ok to use between 1 and 10
		final Scene scene = new Scene(TossUPTargetsPane);
		final Button okButton = new Button("OK");

			cc.setHalignment(HPos.LEFT);
			TossUPTargetsPane.getColumnConstraints().addAll(new ColumnConstraints(), cc);

			for (int i = 1; i <= 6; i++){
				shootCountList.add(Integer.toString(i));
			}

			for (int i = 1; i <= MAX_TARGETS; i++){
				roundCountList.add(Integer.toString(i));
			}

			shootTargetsComboBox.getSelectionModel().select(default_target_count_reset);
			TossUPTargetsPane.add(new Label("Number of Targets:"), 0, 0);
			TossUPTargetsPane.add(shootTargetsComboBox, 1, 0);
			//shootTargetsComboBox.setVisible(false);

			maxScale.add("one-quarter");
			maxScale.add("one-half");
			maxScale.add("three-quarter");
			maxScale.add("original");
			maxScale.add("double");

			maxScaleComboBox.getSelectionModel().select(theScale_reset);
			TossUPTargetsPane.add(new Label("Target Scale:"), 0, 2);
			TossUPTargetsPane.add(maxScaleComboBox, 1, 2);

			numberOfRoundsComboBox.getSelectionModel().select(default_max_rounds_reset);
			TossUPTargetsPane.add(new Label("Target Rounds:"), 0, 3);
			TossUPTargetsPane.add(numberOfRoundsComboBox, 1, 3);

			okButton.setDefaultButton(true);
			TossUPTargetsPane.add(okButton, 1, 6);

		okButton.setOnAction((e) -> {
			addTargetString =  targetListComboBox.getSelectionModel().getSelectedItem();
			shootCount = Integer.parseInt(shootTargetsComboBox.getSelectionModel().getSelectedItem());
			//timeBetweenTargetMovement = Integer.parseInt(targetTimeComboBox.getSelectionModel().getSelectedItem());
			theScale = maxScaleComboBox.getSelectionModel().getSelectedItem();
			roundCount = Integer.parseInt(numberOfRoundsComboBox.getSelectionModel().getSelectedItem());

			TossUPTargetsStage.close();
		});//end OKButton

		TossUPTargetsStage.initOwner(super.getShootOFFStage());
		TossUPTargetsStage.initModality(Modality.WINDOW_MODAL);
		TossUPTargetsStage.setTitle("TossUP Target Settings");
		TossUPTargetsStage.setScene(scene);
		TossUPTargetsStage.showAndWait();

		addTargetString_reset = addTargetString;
		default_target_count_reset = shootCount -1;
		default_time_between_target_movement_reset = timeBetweenTargetMovement -1;
		theScale_reset = theScale;
		default_max_rounds_reset = roundCount -1;
		decRoundCount = roundCount;
		score = 0;

	}//end collectSettings

	protected List<TossUPTarget> getShootTargets() {
		return shootTargets;
	}

	//kept in so I did not break TestBouncingTargets...
	protected List<TossUPTarget> getDontShootTargets() {
		return dontShootTargets;
	}

	private void moveTargetUp(TossUPTarget ctt){
		ctt.moveTargetUp();

	}

	private void updateTargets() {
		decRoundCount--;

		//stop the shooting and present user with consolidated target
		if(decRoundCount < 0) {
			finishTime = System.currentTimeMillis();

			targetAnimation.stop();
			for(TossUPTarget ctt2 : shootTargets){
				ctt2.cttTimeline.stop();
			}
			super.pauseShotDetection(true);
			long scoreTime = finishTime-beepTime;
			if (scoreTime < 0)scoreTime = 0;
			double hitPercentage = 0;
			if (hits+misses !=0) hitPercentage = hits/(double)(hits+misses)*100;
			super.showTextOnFeed(String.format("Total Shots Fired: %d%n Hits: %d%n Misses: %d%n Hit Percentage: %f%n Points Possible: %d%n Total Targets: %d%n Targets Not Hit: %d%n Targets Not Hit Deduction: %d%n  Shots Missed Deduction %d%n Score: %d ",misses+hits ,hits,misses,hitPercentage,possiblePoints*roundCount,roundCount*shootCount,roundCount*shootCount-hits,((roundCount*shootCount-hits)*penalty),misses*5,score-((roundCount*shootCount-hits)*penalty)-misses*5 ));
			
			thisSuper.getProjArenaController().getCanvasManager().setShowShots(true);

			//make the shots go away for a cleaner picture
			for (Shot shot : thisSuper.getProjArenaController().getCanvasManager().getShots()) {
				shot.getMarker().setVisible(false);
			}

			//reset the target animations so the targets pop back up
			for (TossUPTarget target : shootTargets) {
				for (Node node : target.getTarget().getTargetGroup().getChildren()) {
					TargetRegion r = (TargetRegion) node;
					if(r.getType()==RegionType.IMAGE){
						ImageRegion myIR = (ImageRegion) r;
						myIR.getAnimation().get().reset();
					}
				}
			}

			//so we can remove the shots we added later
			canvasGroupSize = thisSuper.getProjArenaController().getCanvasManager().getCanvasGroup().getChildren().size();

			int incX=100;
			//line up the targets
			for (TossUPTarget b : shootTargets){
				File theFile = b.getTarget().getTargetFile();
				String theFileName = theFile.getName();//  getAbsolutePath();//  getName();
				b.getTarget().setPosition(incX,400);
				for(Map.Entry<String, List<Point2D>> e: theMap.entrySet()){
					if(theFileName.compareToIgnoreCase(e.getKey())==0 ){
						//super.showTextOnFeed(String.format("theFileName was found inside update: %s",theFileName));
							for (Point2D p2d : e.getValue()){
								thisSuper.getProjArenaController().getCanvasManager().getCanvasGroup().getChildren().add(new javafx.scene.shape.Circle(p2d.getX()+incX, p2d.getY()+400,10,Color.YELLOW));
							}//end for
						//}//end for
					}//end if
				}//end for

				incX=incX+225;
			}//end for

			//total shots fired = hits plus misses
			//total hits
			//total misses
			//hit percentage  = hits/shots fired
			
			//possible points = possiblePoints * roundCount
			//possible targets to hit = roundcount * shootcount
			//total targets not hit = (roundCount * shootCount) - hits
			//penalty for not hitting targets = ((roundCount * shootCount) - hits)*5
			//penalty for misses = misses*5
			//score
			
			myLabel.setText(String.format("Total Shots Fired: %d%n Hits: %d%n Misses: %d%n Hit Percentage: %f%n Points Possible: %d%n Total Targets: %d%n Targets Not Hit: %d%n Targets Not Hit Deduction: %d%n  Shots Missed Deduction %d%n Score: %d ",misses+hits ,hits,misses,hitPercentage,possiblePoints*roundCount,roundCount*shootCount,roundCount*shootCount-hits,((roundCount*shootCount-hits)*penalty),misses*5,score-((roundCount*shootCount-hits)*penalty)-misses*5 ));
			thisSuper.getProjArenaController().getCanvasManager().showProjectorMessage(myLabel);
			return;
		}//end if

		for (TossUPTarget b : shootTargets){
			b.moveTarget();
		}

		//to reset the round time
		beepTimeStatic = System.currentTimeMillis();
	}//end updateTargets

	public static int getRandom(int from, int to) {
		Random theRandom = new Random();
	    if (from < to)
	        return from + theRandom.nextInt(Math.abs(to - from));
	    return from - theRandom.nextInt(Math.abs(to - from));
	}

	private enum CollisionType {
		NONE, COLLISION_X, COLLISION_Y, COLLISION_BOTH;
	}

	private static CollisionType checkCollision(Optional<Target> newTarget) {
		final Bounds targetBounds = newTarget.get().getTargetGroup().getBoundsInParent();

		List<TossUPTarget> collisionList;
		collisionList = shootTargets;

		for (TossUPTarget b : collisionList) {
			if (b.getTarget().equals(newTarget)) continue;

			if(b.getTarget().getPosition().equals(newTarget.get().getPosition()))continue;

			final Bounds bBounds = b.getTarget().getTargetGroup().getBoundsInParent();

			//thisSuper.showTextOnFeed(String.format("bounds newTarget, bounds listTarget %s, %s", targetBounds.toString(),bBounds.toString()));

			if (targetBounds.intersects(bBounds)) {
				final boolean atRight = targetBounds.getMaxX() > bBounds.getMinX();
				final boolean atLeft = bBounds.getMaxX() > bBounds.getMinX();
				final boolean atBottom = targetBounds.getMaxY() > bBounds.getMinY();
				final boolean atTop = bBounds.getMaxY() > targetBounds.getMinY();

				if ((atRight || atLeft) && (atBottom || atTop)) {
					return CollisionType.COLLISION_BOTH;
				} else if (atRight || atLeft) {
					return CollisionType.COLLISION_X;
				} else if (atBottom || atTop) {
					return CollisionType.COLLISION_Y;
				}
			}
		}

		return CollisionType.NONE;
	}

	protected static class TossUPTarget {
		private final Target target;
		private int launchVelocity = 150;//seems to control the launch angle better than launch degrees
		private double angleOfLaunchDegrees = 90;
		private double gravity = 25;//13,800 top, 17,800 .8, 21,800 .6 30
		private double projectileY = 0;
		private double moveTargetTime = 0;
		private double angleInRads = 0;
		private double targetSpeed = 150.0;//between 100 and 200 works best
		private double yMove = 1;
		Timeline cttTimeline = new Timeline(new KeyFrame(Duration.millis(20), e -> moveTargetUp() ));
		private double delayTime = 0;
		private double startTime = 0;
		private boolean targetWasHit = false;
		
		public void setTargetWasHit(boolean theFlag)
		{
			this.targetWasHit = theFlag;
		}

		public boolean getTargetWasHit()
		{
			return this.targetWasHit;
		}

		public void setGravity(double theGravity)
		{
			this.gravity = theGravity;
		}

		public double getGravity()
		{
			return this.gravity;
		}

		public TossUPTarget(Target target) {
			this.target = target;
		}

		public Target getTarget() {
			return this.target;
		}

		//called by b.moveTarget()
		public void moveTarget() {

			CollisionType ct = CollisionType.COLLISION_X;
			while(ct ==CollisionType.COLLISION_BOTH || ct == CollisionType.COLLISION_X  ){
				int maxX = (int) (thisSuper.getArenaWidth() - this.getTarget().getDimension().getWidth() - edgeProtection);

				Random myRandom1 = new Random();
				int x = myRandom1.nextInt(maxX + 1) + 1;
				this.getTarget().setPosition(x, 1200);
				
				this.setGravity(getRandom(9,15));

				ct = checkCollision(Optional.of(this.getTarget()));

				//thisSuper.showTextOnFeed("inside moveTarget collisionLoop: "+x+" ,"+ct.toString());
			}

		if (this.getTargetWasHit()){
				for (Node node : this.getTarget().getTargetGroup().getChildren()) {
					TargetRegion r = (TargetRegion) node;
					if(r.getType()==RegionType.IMAGE){
						ImageRegion myIR = (ImageRegion) r;
						myIR.getAnimation().get().reset();
					}//end if
				}//end for
				this.setTargetWasHit(false);
			}//end if targetWasHIt

			this.cttTimeline.stop();
			int randomDelay = getRandom(10,30);
			this.delayTime = randomDelay;
			//thisSuper.showTextOnFeed("the delay is: "+randomDelay);
			this.cttTimeline.setDelay(new Duration( (double)randomDelay*100));
			this.cttTimeline.play();
			this.startTime = System.currentTimeMillis();

		}//end moveTarget
		
		//called by update
		public void moveTargetUp() {

			angleInRads = angleOfLaunchDegrees * 3.14159/180;

			moveTargetTime = (System.currentTimeMillis() - delayTime*100 - startTime)/targetSpeed;//beepTimeStatic)/targetSpeed;

			projectileY =  ( (launchVelocity*Math.sin(angleInRads)*moveTargetTime - getGravity()*moveTargetTime*moveTargetTime/2.0)*-1 ) / yMove;

			target.setPosition(target.getPosition().getX(), 1200+projectileY);

		}//end moveTarget
	}//end class TossUPTargets

	private void addTargets(List<TossUPTarget> targets, String target, int count) {
		for (int i = 0; i < count; i++) {
			Optional<Target> newTarget = super.addTarget(new File(target), 0, 0);

			if (newTarget.isPresent()) {

				TossUPTarget theNewCtt = new TossUPTarget(newTarget.get());

				theNewCtt.cttTimeline = new Timeline(new KeyFrame(Duration.millis(20), e -> moveTargetUp(theNewCtt) ));
				theNewCtt.cttTimeline.setCycleCount(Timeline.INDEFINITE);
				myTimelineList.add(theNewCtt.cttTimeline);
				cttMap.put(target.substring(target.indexOf("/")+1), theNewCtt);

				targets.add(theNewCtt);
				targets.get(targets.size()-1).setGravity(getRandom(9,15));
				super.showTextOnFeed("gravity is: "+targets.get(targets.size()-1).getGravity());

				//scale the target
				if(theScale == "one-half")newScale = 0.5;
				if(theScale == "one-quarter")newScale = 0.25;
				if(theScale == "three-quarter")newScale = 0.75;
				if(theScale == "original")newScale = 1.0;
				if(theScale == "double")newScale = 2.0;

				//newTarget.get().setDimensions(newTarget.get().getDimension().getWidth() * newScale, newTarget.get().getDimension().getHeight() * newScale);

				newTarget.get().getTargetGroup().setScaleX(newScale);
				newTarget.get().getTargetGroup().setScaleY(newScale);

				// Randomly place the target with no overlap
				CollisionType myCT = CollisionType.COLLISION_X;
				int maxX,x=0,maxY,y = 0;
				while(myCT ==CollisionType.COLLISION_BOTH || myCT == CollisionType.COLLISION_X ){
					maxX = (int) (super.getArenaWidth() - newTarget.get().getDimension().getWidth() - edgeProtection);
					x = new Random().nextInt(maxX + 1) + 1;
					maxY = (int) (super.getArenaHeight() - newTarget.get().getDimension().getHeight() - edgeProtection);
					y = new Random().nextInt(maxY + 1) + 1;
					newTarget.get().setPosition(x, 1200);
					myCT = checkCollision(newTarget);
					//thisSuper.showTextOnFeed("inside addTargets collisionLoop");
				}

				targetLocationMap.put(target.substring(target.indexOf("/")+1), new Point2D(x,y));


				//using the filename as the key
				theMap.put(target.substring(target.indexOf("/")+1), new ArrayList<Point2D>());

			}//end if
		}//end for
	}//end function

	@Override
	public ExerciseMetadata getInfo() {
		return new ExerciseMetadata("TossUP", "1.0", "ifly53e",
				"This exercise randomly moves six silhouette targets at a user specified interval around the projector arena."
					+ "Hit the targets as quickly as possible to move to the next round of randomly placed targets."
					+ "The targets will disappear after the target face time expires."
					+ "The rounds are the number of cycles in which targets will be presented."
					+ "You can scale the targets to be bigger or smaller "
					+ "Scoring:  Each target is worth 10 points."
					+ "A target that is not hit is minus 10 points.  All misses are minus five points."
					+ "A time bonus is added to your score based on how quickly all targets are hit.");
		//TODO:
		//add a screen count down to start...currently stopping thread for a few seconds
		//figure out how to reset and start over the exercise from hitting a reset target
	}

	@Override
	public void shotListener(Shot shot, Optional<TargetRegion> hitRegion) {
		//targetWasHit = true;

//		if(hitRegion.isPresent()){
//			if(hitRegion.get().getType() == RegionType.ELLIPSE)super.showTextOnFeed("ELLIPSE");
//			if(hitRegion.get().getType() == RegionType.IMAGE)super.showTextOnFeed("IMAGE");
//			if(hitRegion.get().tagExists("name"))super.showTextOnFeed("Name is present:"+hitRegion.get().getTag("name"));
//			if(hitRegion.get().tagExists("subtarget"))super.showTextOnFeed("Subtarget is present:"+hitRegion.get().getTag("subtarget"));
//		}

		if (hitRegion.isPresent()) {
			if (shot.getColor().equals(Color.RED)) {
				hits++;
				if (hitRegion.get().tagExists("points")) {

					super.setShotTimerColumnText(POINTS_COL_NAME, hitRegion.get().getTag("points"));
					score += Integer.parseInt(hitRegion.get().getTag("points"));

				}//end if tagexists points
				else{
					score = score + 10;
				}//end else<tag name="points" value="2" />
				long scoreTime = finishTime-beepTime;
				if (scoreTime < 0)scoreTime = 0;
				//super.showTextOnFeed(String.format("Score: %d  Misses: %d Hits: %d Points Possible: %d Deduction: %d", score-((roundCount*shootCount-hits)*penalty),misses,hits,possiblePoints,((roundCount*shootCount-hits)*penalty) ));
				
				if(hitRegion.get().tagExists("name")){
					TossUPTarget myCtt;
					switch(hitRegion.get().getTag("name")){

						case "TossUP_silhouette-plate.target":
							myCtt = cttMap.get(hitRegion.get().getTag("name"));
							myCtt.setTargetWasHit(true);
						break;
						case "TossUP_ram_silhouette.target":
							myCtt = cttMap.get(hitRegion.get().getTag("name"));
							myCtt.setTargetWasHit(true);
						break;
						case "TossUP_pig_silhouette.target":
							myCtt = cttMap.get(hitRegion.get().getTag("name"));
							myCtt.setTargetWasHit(true);
						break;
						case "TossUP_chicken.target":
							myCtt = cttMap.get(hitRegion.get().getTag("name"));
							myCtt.setTargetWasHit(true);
						break;
						case "TossUP_Turkey_Silhouette.target":
							myCtt = cttMap.get(hitRegion.get().getTag("name"));
							myCtt.setTargetWasHit(true);
						break;
						case "TossUP_Pepper_Popper.target":
							myCtt = cttMap.get(hitRegion.get().getTag("name"));
							myCtt.setTargetWasHit(true);
						break;
					}//end switch

					List<Point2D> myTempList = new ArrayList<Point2D>();
					myTempList = theMap.get( hitRegion.get().getTag("name"));//get the list of points from the map for the target that was hit
					//get the location of that target as soon as you can
					if(myTempList !=null){
						myTempList.add(new Point2D(hitRegion.get().getRegionImpactX(), hitRegion.get().getRegionImpactY() ));
						theMap.put( hitRegion.get().getTag("name"), myTempList);
					}
					super.showTextOnFeed(String.format("NAME hitx: %d, shotX: %f, hity: %d, shoty: %f",hitRegion.get().getRegionImpactX(),shotX,hitRegion.get().getRegionImpactY(),shotY));
				}
				if(hitRegion.get().tagExists("subtarget")){
					TossUPTarget myCtt;
					switch(hitRegion.get().getTag("subtarget")){

						case "TossUP_silhouette-plate.target":
							myCtt = cttMap.get(hitRegion.get().getTag("subtarget"));
							myCtt.setTargetWasHit(true);
						break;
						case "TossUP_ram_silhouette.target":
							myCtt = cttMap.get(hitRegion.get().getTag("subtarget"));
							myCtt.setTargetWasHit(true);
						break;
						case "TossUP_pig_silhouette.target":
							myCtt = cttMap.get(hitRegion.get().getTag("subtarget"));
							myCtt.setTargetWasHit(true);
						break;
						case "TossUP_chicken.target":
							myCtt = cttMap.get(hitRegion.get().getTag("subtarget"));
							myCtt.setTargetWasHit(true);
						break;
						case "TossUP_Turkey_Silhouette.target":
							myCtt = cttMap.get(hitRegion.get().getTag("subtarget"));
							myCtt.setTargetWasHit(true);
						break;
						case "TossUP_Pepper_Popper.target":
							myCtt = cttMap.get(hitRegion.get().getTag("subtarget"));
							myCtt.setTargetWasHit(true);
						break;
					}//end switch

					List<Point2D> myTempList1 = new ArrayList<Point2D>();
					myTempList1 = theMap.get( hitRegion.get().getTag("subtarget"));//get the list of points from the map for the target that was hit
					//get the location of that target as soon as you can
					myTempList1.add(new Point2D(hitRegion.get().getRegionImpactX(), hitRegion.get().getRegionImpactY() ));
					theMap.put( hitRegion.get().getTag("subtarget"), myTempList1);
					super.showTextOnFeed(String.format("SUB  hitx: %d, shotX: %f, hity: %d, shoty: %f",hitRegion.get().getRegionImpactX(),shotX,hitRegion.get().getRegionImpactY(),shotY));
				}

				return;
			}//end if

		}//end if hitRegion.isPresent
		else{
			//its a miss
			if (shot.getColor().equals(Color.RED)) {
				score = score - 5;
				misses++;
				long scoreTime = finishTime-beepTime;
				if (scoreTime < 0)scoreTime = 0;
				//super.showTextOnFeed(String.format("Score: %d  Misses: %d Hits: %d Points Possible: %d Deduction: %d", score-((roundCount*shootCount-hits)*penalty),misses,hits,possiblePoints,((roundCount*shootCount-hits)*penalty) ));
			}//end if

		}//end else
	}//end function

	@Override
	public void reset(List<Group> targets) {
		reset();
	}
	@Override
	public void reset() {
		targetAnimation.stop();

		//clean up the shots we added
		//super.showTextOnFeed("canvasGroupSize before: "+canvasGroupSize +" after:"+thisSuper.getProjArenaController().getCanvasManager().getCanvasGroup().getChildren().size());
		for (int i = thisSuper.getProjArenaController().getCanvasManager().getCanvasGroup().getChildren().size(); i>canvasGroupSize;i-- ) {
			//super.showTextOnFeed("removed node in reset"+i);
			thisSuper.getProjArenaController().getCanvasManager().getCanvasGroup().getChildren().remove(thisSuper.getProjArenaController().getCanvasManager().getCanvasGroup().getChildren().size()-1);
		}

		for (TossUPTarget b : shootTargets)
			super.removeTarget(b.getTarget());
		shootTargets.clear();

		consolidatedList.clear();

		//shots.clear wont work for the projector arena so do it manually

		if(!troubleshootingReset){
			for (Shot shot : thisSuper.getProjArenaController().getCanvasManager().getShots() ) {
				if(thisSuper.getProjArenaController().getCanvasManager().getCanvasGroup().getChildren().contains(shot.getMarker()) ){
					thisSuper.getProjArenaController().getCanvasManager().getCanvasGroup().getChildren().remove(shot.getMarker());
				}//endif
			}//end for
		}//end iff

		this.getProjArenaController().getCanvasManager().getShots().clear();
		thisSuper.getProjArenaController().getCanvasManager().removeProjectorMessage(myLabel);
		//myLabel = thisSuper.getProjArenaController().getCanvasManager().addProjectorMessage(String.format("Score: %d", score), Color.YELLOW); //getCanvasGroup().getChildren().set(1,myLabel);

		fromReset = true;
		getProjArenaController().getCanvasManager().setShowShots(false);
		hits = 0;
		misses = 0;
		init();

	}//end reset

}//end public class TossUP
