/* Copyright (c) 2015 phrack. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package com.shootoff.targets.animation;

import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class SpriteAnimation extends Transition {
	public static final int DEFAULT_DELAY = 100;
	
    private final ImageView imageView;
    private final ImageFrame[] frames;
    private final int count;

    private int lastIndex;

    public SpriteAnimation(ImageView imageView, ImageFrame[] frames) {
    	
        this.imageView = imageView;
        this.frames = frames;
        this.count = frames.length;
        setInterpolator(Interpolator.LINEAR);
    }
    
    public Image getFirstFrame() {
    	return frames[0].getImage();
    }
    
    public int getFrameCout() {
    	return frames.length;
    }

    public void reset() {
    	imageView.setImage(getFirstFrame());
    }
    
    protected void interpolate(double k) {
        final int index = Math.min((int) Math.floor(k * count), count - 1);
        if (index != lastIndex) {
            imageView.setImage(frames[index].getImage());
            lastIndex = index;
        }
    }
}