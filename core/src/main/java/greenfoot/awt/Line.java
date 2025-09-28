/*
 This file is part of the LibGDX-Greenfoot wrapper.
 Provides LibGDX-compatible implementation of java.awt.geom.Line2D for cross-platform compatibility.
 
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
 * LibGDX-compatible implementation of java.awt.geom.Line2D for cross-platform use.
 * This class provides line shape functionality using LibGDX Pixmap.
 * 
 * This class replaces java.awt.geom.Line2D to provide LibGDX backend compatibility,
 * mainly to allow Greenfoot projects to run on LibGDX platforms.
 * 
 * @author Qiupi3 (LibGDX wrapper implementation)
 * @version 1.0
 */
public class Line extends Shape {
    
    /** The X coordinate of the start point. */
    public int x1;
    
    /** The Y coordinate of the start point. */
    public int y1;
    
    /** The X coordinate of the end point. */
    public int x2;
    
    /** The Y coordinate of the end point. */
    public int y2;
    
    /**
     * Constructs a new Line, initialized to location (0,0) -> (0,0).
     */
    public Line() {
        this(0, 0, 0, 0);
    }
    
    /**
     * Constructs and initializes a Line from the specified coordinates.
     * 
     * @param x1 the X coordinate of the start point
     * @param y1 the Y coordinate of the start point
     * @param x2 the X coordinate of the end point
     * @param y2 the Y coordinate of the end point
     */
    public Line(int x1, int y1, int x2, int y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }
    
    @Override
    public void draw(Pixmap pixmap) {
        if (pixmap == null) return;
        
        // Draw line using LibGDX
        pixmap.drawLine(x1, y1, x2, y2);
    }
    
    @Override
    public greenfoot.awt.Rectangle getBounds() {
        int minX = Math.min(x1, x2);
        int minY = Math.min(y1, y2);
        int maxX = Math.max(x1, x2);
        int maxY = Math.max(y1, y2);
        
        return new greenfoot.awt.Rectangle(minX, minY, maxX - minX, maxY - minY);
    }
    
    @Override
    public boolean contains(int x, int y) {
        // For a line, containment is usually defined as being very close to the line
        // This is a simplified implementation using distance to line
        
        // Calculate distance from point to line using cross product
        double A = y2 - y1;
        double B = x1 - x2;
        double C = x2 * y1 - x1 * y2;
        
        double distance = Math.abs(A * x + B * y + C) / Math.sqrt(A * A + B * B);
        
        // Consider the point contained if it's within 2 pixels of the line
        return distance <= 2.0;
    }
    
    /**
     * Sets the location of the endpoints of this Line.
     * 
     * @param x1 the X coordinate of the start point
     * @param y1 the Y coordinate of the start point  
     * @param x2 the X coordinate of the end point
     * @param y2 the Y coordinate of the end point
     */
    public void setLine(int x1, int y1, int x2, int y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }
    
    @Override
    public String toString() {
        return getClass().getName() + "[(" + x1 + "," + y1 + ") -> (" + x2 + "," + y2 + ")]";
    }
}