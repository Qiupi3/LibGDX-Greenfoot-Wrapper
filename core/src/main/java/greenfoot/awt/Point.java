/*
 This file is part of the LibGDX-Greenfoot wrapper.
 Provides LibGDX-compatible implementation of java.awt.Point for cross-platform compatibility.
 
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

/**
 * LibGDX-compatible implementation of java.awt.Point for cross-platform use.
 * This class represents a location in (x, y) integer coordinate space.
 * 
 * This class replaces java.awt.Point to provide LibGDX backend compatibility,
 * mainly to allow Greenfoot projects to run on LibGDX platforms. The Android
 * runtime does not ship java.awt, so the AWT class cannot be used here.
 * 
 * @author Qiupi3 (LibGDX wrapper implementation)
 * @version 1.0
 */
public class Point {
    
    /** The X coordinate of this Point. */
    public int x;
    
    /** The Y coordinate of this Point. */
    public int y;
    
    /**
     * Constructs a Point at the origin (0, 0).
     */
    public Point() {
        this(0, 0);
    }
    
    /**
     * Constructs a Point at the specified (x, y) location.
     * 
     * @param x the X coordinate
     * @param y the Y coordinate
     */
    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    /**
     * Constructs a Point at the same location as the specified Point.
     * 
     * @param p the Point to copy
     */
    public Point(Point p) {
        this(p.x, p.y);
    }
    
    /**
     * Returns the X coordinate of this Point.
     * 
     * @return the X coordinate
     */
    public int getX() {
        return x;
    }
    
    /**
     * Returns the Y coordinate of this Point.
     * 
     * @return the Y coordinate
     */
    public int getY() {
        return y;
    }
    
    /**
     * Returns this Point. Provided for compatibility with the original AWT API.
     * 
     * @return this Point
     */
    public Point getLocation() {
        return new Point(x, y);
    }
    
    /**
     * Moves this Point to the specified location.
     * 
     * @param x the new X coordinate
     * @param y the new Y coordinate
     */
    public void move(int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    /**
     * Sets the location of this Point to the specified coordinates.
     * 
     * @param x the new X coordinate
     * @param y the new Y coordinate
     */
    public void setLocation(int x, int y) {
        move(x, y);
    }
    
    /**
     * Sets the location of this Point to the same location as the specified Point.
     * 
     * @param p the Point to copy the location from
     */
    public void setLocation(Point p) {
        move(p.x, p.y);
    }
    
    /**
     * Translates this Point by the specified offsets.
     * 
     * @param dx the offset to add to the X coordinate
     * @param dy the offset to add to the Y coordinate
     */
    public void translate(int dx, int dy) {
        this.x += dx;
        this.y += dy;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Point) {
            Point other = (Point) obj;
            return x == other.x && y == other.y;
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return x * 31 + y;
    }
    
    @Override
    public String toString() {
        return getClass().getName() + "[x=" + x + ",y=" + y + "]";
    }
}
