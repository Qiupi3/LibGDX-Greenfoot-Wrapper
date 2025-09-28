/*
 This file is part of the Greenfoot program.
 Copyright (C) 2005-2009,2010,2011,2013,2014,2015,2016,2021 Poul Henriksen and Michael Kolling

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

 This file is subject to the Classpath exception as provided in the
 LICENSE file that accompanied this code.
*/

package greenfoot;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * LibGDX-based implementation of the ImageVisitor class.
 * Converted to work with LibGDX instead of AWT.
 * 
 * This class re-implements greenfoot.ImageVisitor to provide a LibGDX backend,
 * mainly to allow Greenfoot projects to run on LibGDX (especially to export into
 * mobile devices and other platforms).
 * 
 * Inspired by the original Greenfoot project (GPLv2+ with Classpath Exception).
 * Read the original documentation at
 * https://www.greenfoot.org/files/javadoc/greenfoot/package-summary.html
 * 
 * @author Poul Henriksen (Original Greenfoot version's author)
 * 
 * @modified-by Qiupi3 (LibGDX wrapper implementation)
 * @version 1.0
 */
public class ImageVisitor {
    /**
     * Draw a GreenfootImage using LibGDX SpriteBatch instead of AWT Graphics2D.
     * 
     * @param image The image to draw
     * @param batch The LibGDX SpriteBatch to draw with
     * @param x The x coordinate
     * @param y The y coordinate  
     * @param useTransparency Whether to use transparency
     */
    public static void drawImage(GreenfootImage image, SpriteBatch batch, int x, int y, boolean useTransparency) {
        if (image == null || batch == null) {
            return;
        }
        
        // Get the LibGDX texture from the GreenfootImage
        com.badlogic.gdx.graphics.Texture texture = image.getTexture();
        if (texture != null) {
            if (useTransparency) {
                // Apply transparency from image
                float alpha = image.getTransparency() / 255.0f;
                batch.setColor(1f, 1f, 1f, alpha);
            }
            
            // Draw the texture
            batch.draw(texture, x, y);
            
            if (useTransparency) {
                // Reset color to normal
                batch.setColor(1f, 1f, 1f, 1f);
            }
        }
    }
    
    /**
     * Compare two GreenfootImages for equality.
     * 
     * @param image1 First image to compare
     * @param image2 Second image to compare
     * @return true if the images are equal
     */
    public static boolean equal(GreenfootImage image1, GreenfootImage image2) {
        // Use the static method from GreenfootImage class
        return GreenfootImage.equal(image1, image2);
    }
}
