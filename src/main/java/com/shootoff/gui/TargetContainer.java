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

package com.shootoff.gui;

import com.shootoff.config.Configuration;

import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
 
/**
 * This class wraps a group that represents a target so that the target
 * can be moved and resized using the mouse.
 * 
 * @author phrack
 */
public class TargetContainer {
    private static final int RESIZE_MARGIN = 5;
    private final Group target;
    private final Configuration config;
    private boolean move;
    private boolean resize;
    private boolean top;
    private boolean bottom;
    private boolean left;
    private boolean right;
    
    private double x;
    private double y;
    
    public TargetContainer(Group target, Configuration config) {
        this.target = target;
        this.config = config;
        
        mousePressed();
        mouseDragged();
        mouseMoved();
        mouseReleased();
    }
    
    private void mousePressed() {
        target.setOnMousePressed((event) -> {
	        if(!isInResizeZone(event)) {
	        	move = true;
	        	
	        	x = event.getX() - target.getLayoutX();
	        	y = event.getY() - target.getLayoutY();
	            return;
	        }
	        
	        resize = true;
	        top = isTopZone(event);
	        bottom = isBottomZone(event);
	        left = isLeftZone(event);
	        right = isRightZone(event);
        });
    }
 
    private void mouseDragged() {
    	target.setOnMouseDragged((event) -> {
    		if (!resize && !move) return;
    		
	        if(move) {
	        	if (config.inDebugMode() && (event.isControlDown() || event.isShiftDown())) return;
	        	
	        	for (Node node : target.getChildren()) {
		            node.setLayoutX(target.getLayoutX() + event.getX() - x);
		            node.setLayoutY(target.getLayoutY() + event.getY() - y);
	        	}
	            
	            return;
	        }
	 
	        if (left || right) {
		        double gap; // The gap between the mouse and nearest target edge
		        
		        if (right) {
		        	gap = event.getX() - target.getLayoutBounds().getMaxX();
		        } else {
		        	gap = event.getX() - target.getLayoutBounds().getMinX();
		        }
		        
		        double currentWidth = target.getBoundsInParent().getWidth(); 
		        double newWidth = currentWidth + gap;
		        double scaleDelta = (newWidth - currentWidth) / currentWidth;
		       
		        double currentOriginX = target.getBoundsInParent().getMinX();
		        double newOriginX;
		        
		        if (right) {
		        	scaleDelta *= -1.0;
		        	newOriginX = currentOriginX - ((newWidth - currentWidth) / 2);
		        } else {
		        	newOriginX = currentOriginX + ((newWidth - currentWidth) / 2);
		        }
		        
		        double originXDelta = newOriginX - currentOriginX;
		        
		        if (right) originXDelta *= -1.0;
		        
		        for (Node node : target.getChildren()) {
		        	node.setLayoutX(node.getLayoutX() + originXDelta);
		        	node.setScaleX(node.getScaleX() * (1.0 - scaleDelta));
		        }
    		} else if (top || bottom) {
		        double gap;
		        
		        if (bottom) {
		        	gap = event.getY() - target.getLayoutBounds().getMaxY();
		        } else {
		        	gap = event.getY() - target.getLayoutBounds().getMinY();
		        }    
		        
		        double currentHeight = target.getBoundsInParent().getHeight(); 
		        double newHeight = currentHeight + gap;
		        double scaleDelta = (newHeight - currentHeight) / currentHeight;
		       
		        double currentOriginY = target.getBoundsInParent().getMinY();
		        double newOriginY;
		        
		        if (bottom) {
		        	scaleDelta *= -1.0;
		        	newOriginY = currentOriginY - ((newHeight - currentHeight) / 2);
		        } else {
		        	newOriginY = currentOriginY + ((newHeight - currentHeight) / 2);
		        }
		        
		        double originYDelta = newOriginY - currentOriginY;
		        
		        if (bottom) originYDelta *= -1.0;
		        
		        for (Node node : target.getChildren()) {
		        	node.setLayoutY(node.getLayoutY() + originYDelta); 
		        	node.setScaleY(node.getScaleY() * (1.0 - scaleDelta));
		        }
	        }
    	});
    }
    
    private void mouseMoved() {
    	target.setOnMouseMoved((event) -> {
    		if (isTopZone(event)) {
    			target.setCursor(Cursor.N_RESIZE);
    		} else if (isBottomZone(event)) {
	       		target.setCursor(Cursor.S_RESIZE);
	       	} else if (isLeftZone(event)) {
	       		target.setCursor(Cursor.W_RESIZE);
	       	} else if (isRightZone(event)) {
	       		target.setCursor(Cursor.E_RESIZE);
	       	} else {
	       		target.setCursor(Cursor.DEFAULT);
	        }
    	});
    }
    
    private void mouseReleased() {
    	target.setOnMouseReleased((event) -> {
    		resize = false;
    		move = false;
        	target.setCursor(Cursor.DEFAULT);
    	});
    }
    
    private boolean isTopZone(MouseEvent event) {
    	return event.getY() < (target.getLayoutBounds().getMinY() + RESIZE_MARGIN);	
    }
    
    private boolean isBottomZone(MouseEvent event) {
    	return event.getY() > (target.getLayoutBounds().getMaxY() - RESIZE_MARGIN);
    }
    
    private boolean isLeftZone(MouseEvent event) {
    	return event.getX() < (target.getLayoutBounds().getMinX() + RESIZE_MARGIN);	
    }
    
    private boolean isRightZone(MouseEvent event) {
    	return event.getX() > (target.getLayoutBounds().getMaxX() - RESIZE_MARGIN) ;
    }
 
    private boolean isInResizeZone(MouseEvent event) { 
        return isTopZone(event) || isBottomZone(event) || isLeftZone(event) ||
        		isRightZone(event);
    }   
}