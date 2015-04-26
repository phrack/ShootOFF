/* Copyright (c) 2015 phrack. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package com.shootoff.targets.animation;

import java.awt.image.BufferedImage;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

public class ImageFrame {
    private final int delay;
    private final BufferedImage bufferedImage;
    private final Image image;
    private final String disposal;

    public ImageFrame (BufferedImage image, int delay, String disposal){
        this.bufferedImage = image;
        this.image = SwingFXUtils.toFXImage(image, null);
        this.delay = delay;
        this.disposal = disposal;
    }

    public ImageFrame (BufferedImage image){
        this.bufferedImage = image;
        this.image = SwingFXUtils.toFXImage(image, null);
        this.delay = -1;
        this.disposal = null;
    }

    public BufferedImage getBufferedImage() {
        return bufferedImage;
    }
    
    public Image getImage() {
        return image;
    }

    public int getDelay() {
        return delay;
    }

    public String getDisposal() {
        return disposal;
    }
}
