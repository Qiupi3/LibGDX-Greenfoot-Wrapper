/*
 This file is part of the LibGDX-Greenfoot wrapper.
 Provides LibGDX-compatible implementation of java.awt.Rectangle for cross-platform compatibility.
 
 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 GNU General Public License for more details.
*/

package greenfoot.awt;

import com.badlogic.gdx.graphics.Pixmap;

/**
 * LibGDX-compatible implementation of java.awt.Rectangle for cross-platform use.
 * This class provides rectangle shape functionality using LibGDX Pixmap.
 * 
 * This class replaces java.awt.Rectangle to provide LibGDX backend compatibility,
 * mainly to allow Greenfoot projects to run on LibGDX platforms.
 * 
 * @author Qiupi3 (LibGDX wrapper implementation)  
 * @version 1.0
 */
public class Rectangle extends Shape {
    
    /** The X coordinate of the upper-left corner of the Rectangle. */
    public int x;
    
    /** The Y coordinate of the upper-left corner of the Rectangle. */
    public int y;
    
    /** The width of the Rectangle. */
    public int width;
    
    /** The height of the Rectangle. */
    public int height;
    
    /**
     * Constructs a new Rectangle whose upper-left corner is at (0, 0) 
     * in the coordinate space, and whose width and height are both zero.
     */
    public Rectangle() {
        this(0, 0, 0, 0);
    }
    
    /**
     * Constructs a new Rectangle whose upper-left corner is specified 
     * as (x,y) and whose width and height are specified by the arguments 
     * of the same name.
     * 
     * @param x the specified X coordinate
     * @param y the specified Y coordinate
     * @param width the width of the Rectangle
     * @param height the height of the Rectangle
     */
    public Rectangle(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
    
    @Override
    public void draw(Pixmap pixmap) {
        if (pixmap == null) return;
        
        // Draw rectangle outline
        pixmap.drawRectangle(x, y, width, height);
    }
    
    /**
     * Fill this rectangle using LibGDX Pixmap.
     * 
     * @param pixmap The LibGDX Pixmap to draw on (color should already be set)
     */
    public void fill(Pixmap pixmap) {
        if (pixmap == null) return;
        
        // Fill rectangle
        pixmap.fillRectangle(x, y, width, height);
    }
    
    @Override
    public greenfoot.awt.Rectangle getBounds() {
        return new greenfoot.awt.Rectangle(x, y, width, height);
    }
    
    @Override
    public boolean contains(int px, int py) {
        return px >= x && px < (x + width) && py >= y && py < (y + height);
    }
    
    /**
     * Sets the bounding Rectangle of this Rectangle to the specified 
     * x, y, width, and height.
     * 
     * @param x the new X coordinate for the upper-left corner
     * @param y the new Y coordinate for the upper-left corner  
     * @param width the new width for this Rectangle
     * @param height the new height for this Rectangle
     */
    public void setBounds(int x, int y, int width, int height) {
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