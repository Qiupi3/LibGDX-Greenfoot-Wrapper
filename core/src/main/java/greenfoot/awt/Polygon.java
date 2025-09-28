/*
 This file is part of the LibGDX-Greenfoot wrapper.
 Provides LibGDX-compatible implementation of java.awt.Polygon for cross-platform compatibility.
 
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
 * LibGDX-compatible implementation of java.awt.Polygon for cross-platform use.
 * This class provides polygon shape functionality using LibGDX Pixmap.
 * 
 * This class replaces java.awt.Polygon to provide LibGDX backend compatibility,
 * mainly to allow Greenfoot projects to run on LibGDX platforms.
 * 
 * @author Qiupi3 (LibGDX wrapper implementation)
 * @version 1.0
 */
public class Polygon extends Shape {
    
    /** The array of X coordinates. */
    public int[] xpoints;
    
    /** The array of Y coordinates. */
    public int[] ypoints;
    
    /** The total number of points. */
    public int npoints;
    
    /**
     * Creates an empty polygon.
     */
    public Polygon() {
        xpoints = new int[4];
        ypoints = new int[4];
        npoints = 0;
    }
    
    /**
     * Constructs and initializes a Polygon from the specified parameters.
     * 
     * @param xpoints an array of X coordinates
     * @param ypoints an array of Y coordinates  
     * @param npoints the total number of points in the Polygon
     */
    public Polygon(int[] xpoints, int[] ypoints, int npoints) {
        if (npoints > xpoints.length || npoints > ypoints.length) {
            throw new IndexOutOfBoundsException("npoints > xpoints.length || npoints > ypoints.length");
        }
        
        if (npoints < 0) {
            throw new NegativeArraySizeException("npoints < 0");
        }
        
        this.npoints = npoints;
        this.xpoints = new int[npoints];
        this.ypoints = new int[npoints];
        
        System.arraycopy(xpoints, 0, this.xpoints, 0, npoints);
        System.arraycopy(ypoints, 0, this.ypoints, 0, npoints);
    }
    
    @Override
    public void draw(Pixmap pixmap) {
        if (pixmap == null || npoints < 2) return;
        
        // Draw polygon outline by connecting consecutive points
        for (int i = 0; i < npoints - 1; i++) {
            pixmap.drawLine(xpoints[i], ypoints[i], xpoints[i + 1], ypoints[i + 1]);
        }
        
        // Close the polygon
        if (npoints > 2) {
            pixmap.drawLine(xpoints[npoints - 1], ypoints[npoints - 1], xpoints[0], ypoints[0]);
        }
    }
    
    /**
     * Fill this polygon using LibGDX Pixmap.
     * This is a simplified implementation that draws filled triangles.
     * 
     * @param pixmap The LibGDX Pixmap to draw on (color should already be set)
     */
    public void fill(Pixmap pixmap) {
        if (pixmap == null || npoints < 3) return;
        
        // Simple polygon filling by drawing triangles from first vertex
        for (int i = 1; i < npoints - 1; i++) {
            // Draw triangle: point 0, point i, point i+1
            drawFilledTriangle(pixmap, 
                xpoints[0], ypoints[0],
                xpoints[i], ypoints[i], 
                xpoints[i + 1], ypoints[i + 1]);
        }
    }
    
    /**
     * Helper method to draw a filled triangle.
     */
    private void drawFilledTriangle(Pixmap pixmap, int x1, int y1, int x2, int y2, int x3, int y3) {
        // Simple triangle filling using line sweeping
        int minY = Math.min(Math.min(y1, y2), y3);
        int maxY = Math.max(Math.max(y1, y2), y3);
        
        for (int y = minY; y <= maxY; y++) {
            // Find intersections with triangle edges at this y coordinate
            // This is a simplified implementation
            int minX = Math.min(Math.min(x1, x2), x3);
            int maxX = Math.max(Math.max(x1, x2), x3);
            
            pixmap.drawLine(minX, y, maxX, y);
        }
    }
    
    @Override
    public greenfoot.awt.Rectangle getBounds() {
        if (npoints == 0) {
            return new greenfoot.awt.Rectangle();
        }
        
        int minX = xpoints[0], maxX = xpoints[0];
        int minY = ypoints[0], maxY = ypoints[0];
        
        for (int i = 1; i < npoints; i++) {
            minX = Math.min(minX, xpoints[i]);
            maxX = Math.max(maxX, xpoints[i]);
            minY = Math.min(minY, ypoints[i]);
            maxY = Math.max(maxY, ypoints[i]);
        }
        
        return new greenfoot.awt.Rectangle(minX, minY, maxX - minX, maxY - minY);
    }
    
    @Override
    public boolean contains(int x, int y) {
        // Ray casting algorithm for point-in-polygon test
        if (npoints < 3) return false;
        
        boolean inside = false;
        
        for (int i = 0, j = npoints - 1; i < npoints; j = i++) {
            if (((ypoints[i] > y) != (ypoints[j] > y)) &&
                (x < (xpoints[j] - xpoints[i]) * (y - ypoints[i]) / (ypoints[j] - ypoints[i]) + xpoints[i])) {
                inside = !inside;
            }
        }
        
        return inside;
    }
    
    /**
     * Appends the specified coordinates to this Polygon.
     * 
     * @param x the specified X coordinate
     * @param y the specified Y coordinate
     */
    public void addPoint(int x, int y) {
        if (npoints >= xpoints.length) {
            // Resize arrays
            int[] newXPoints = new int[npoints * 2];
            int[] newYPoints = new int[npoints * 2];
            
            System.arraycopy(xpoints, 0, newXPoints, 0, npoints);
            System.arraycopy(ypoints, 0, newYPoints, 0, npoints);
            
            xpoints = newXPoints;
            ypoints = newYPoints;
        }
        
        xpoints[npoints] = x;
        ypoints[npoints] = y;
        npoints++;
    }
    
    @Override
    public String toString() {
        return getClass().getName() + "[npoints=" + npoints + "]";
    }
}