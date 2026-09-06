/*
 This file is part of the LibGDX-Greenfoot wrapper.
 Provides LibGDX-compatible implementation of java.awt.geom.Ellipse2D for cross-platform compatibility.
 Copyright (C) 2026 Qiupi3
 
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

package greenfoot.awt;

import com.badlogic.gdx.graphics.Pixmap;

/**
 * LibGDX-compatible implementation of java.awt.geom.Ellipse2D for cross-platform use.
 * This class provides ellipse/oval shape functionality using LibGDX Pixmap.
 * 
 * This class replaces java.awt.geom.Ellipse2D to provide LibGDX backend compatibility,
 * mainly to allow Greenfoot projects to run on LibGDX platforms.
 * 
 * @author Qiupi3
 * @version 1.0
 */
public class Ellipse extends Shape {
    
    /** The X coordinate of the upper-left corner of the framing rectangle. */
    public int x;
    
    /** The Y coordinate of the upper-left corner of the framing rectangle. */
    public int y;
    
    /** The width of the framing rectangle. */
    public int width;
    
    /** The height of the framing rectangle. */
    public int height;
    
    /**
     * Constructs a new Ellipse, initialized to location (0, 0) and size (0, 0).
     */
    public Ellipse() {
        this(0, 0, 0, 0);
    }
    
    /**
     * Constructs and initializes an Ellipse from the specified coordinates.
     * 
     * @param x the X coordinate of the upper-left corner of the framing rectangle
     * @param y the Y coordinate of the upper-left corner of the framing rectangle
     * @param width the width of the framing rectangle
     * @param height the height of the framing rectangle
     */
    public Ellipse(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
    
    @Override
    public void draw(Pixmap pixmap) {
        if (pixmap == null) return;
        
        // Draw ellipse outline using LibGDX circle drawing
        int centerX = x + width / 2;
        int centerY = y + height / 2;
        int radius = Math.min(width, height) / 2;
        
        pixmap.drawCircle(centerX, centerY, radius);
    }
    
    /**
     * Fill this ellipse using LibGDX Pixmap.
     * 
     * @param pixmap The LibGDX Pixmap to draw on (color should already be set)
     */
    public void fill(Pixmap pixmap) {
        if (pixmap == null) return;
        
        // Fill ellipse using LibGDX circle filling
        int centerX = x + width / 2;
        int centerY = y + height / 2;
        int radius = Math.min(width, height) / 2;
        
        pixmap.fillCircle(centerX, centerY, radius);
    }
    
    @Override
    public greenfoot.awt.Rectangle getBounds() {
        return new greenfoot.awt.Rectangle(x, y, width, height);
    }
    
    @Override
    public boolean contains(int px, int py) {
        // Simple ellipse containment test (treating as circle for simplicity)
        int centerX = x + width / 2;
        int centerY = y + height / 2;
        int radius = Math.min(width, height) / 2;
        
        int dx = px - centerX;
        int dy = py - centerY;
        
        return (dx * dx + dy * dy) <= (radius * radius);
    }
    
    /**
     * Sets the location and size of the framing rectangle of this Shape.
     * 
     * @param x the X coordinate of the upper-left corner
     * @param y the Y coordinate of the upper-left corner
     * @param width the width of the framing rectangle
     * @param height the height of the framing rectangle
     */
    public void setFrame(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
    
    @Override
    public String toString() {
        return getClass().getName() + "[x=" + x + ",y=" + y + ",width=" + width + ",height=" + height + "]";
    }
}