/*
 This file is part of the LibGDX-Greenfoot wrapper.
 Provides LibGDX-compatible implementation of java.awt.Shape for cross-platform compatibility.
 
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
 * LibGDX-compatible implementation of java.awt.Shape for cross-platform use.
 * This abstract class provides the foundation for drawing shapes using LibGDX Pixmap.
 * 
 * This class replaces java.awt.Shape to provide LibGDX backend compatibility,
 * mainly to allow Greenfoot projects to run on LibGDX platforms.
 * 
 * @author Qiupi3 (LibGDX wrapper implementation)
 * @version 1.0
 */
public abstract class Shape {
    
    /**
     * Draw this shape using LibGDX Pixmap.
     * Subclasses must implement this method to define how the shape is drawn.
     * 
     * @param pixmap The LibGDX Pixmap to draw on (color should already be set)
     */
    public abstract void draw(Pixmap pixmap);
    
    /**
     * Get the bounding rectangle of this shape.
     * 
     * @return Rectangle containing the bounds of this shape
     */
    public abstract greenfoot.awt.Rectangle getBounds();
    
    /**
     * Test if the specified coordinates are inside the boundary of the shape.
     * 
     * @param x the specified X coordinate to be tested
     * @param y the specified Y coordinate to be tested
     * @return true if the coordinates are inside the shape; false otherwise
     */
    public abstract boolean contains(int x, int y);
}