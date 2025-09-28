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
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.Color;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;

/**
 * LibGDX-based WorldVisitor implementation.
 * 
 * This class re-implements greenfoot.WorldVisitor to provide a LibGDX backend,
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
public class WorldVisitor {
    public static int getWidthInCells(World w) {
        return w.width;
    }
    
    public static int getHeightInCells(World w) {
        return w.height;
    }
    
    public static int getWidthInPixels(World w) {
        // Assume each cell is cellSize pixels wide
        return w.width * w.cellSize;
    }

    public static int getHeightInPixels(World w) {
        // Assume each cell is cellSize pixels tall
        return w.height * w.cellSize;
    }

    public static int getCellSize(World w) {
        return w.cellSize;
    }
    
    public static Collection<Actor> getObjectsAtPixel(World w, int x, int y) {
        // Convert pixel coordinates to cell coordinates
        int cellX = toCellFloor(w, x);
        int cellY = toCellFloor(w, y);
        
        // Check if coordinates are within bounds
        if (cellX < 0 || cellX >= w.width || cellY < 0 || cellY >= w.height) {
            return java.util.Collections.emptyList();
        }
        
        // Get objects at the cell location
        return w.getObjectsAt(cellX, cellY, Actor.class);
    }

    /**
     * Used to indicate the start of an animation sequence. For use in the collision checker.
     */
    public static void startSequence(World w) {
        w.startSequence();
    }

    /**
     * Paint debug information using LibGDX ShapeRenderer for proper debug visualization.
     */
    public static void paintDebug(World world, SpriteBatch batch) {
        paintDebug(world, batch, null);
    }
    
    /**
     * Paint debug information using LibGDX ShapeRenderer with custom renderer.
     */
    public static void paintDebug(World world, SpriteBatch batch, ShapeRenderer shapeRenderer) {
        if (world == null) return;
        
        // If no shape renderer provided, create a temporary one
        ShapeRenderer renderer = shapeRenderer;
        boolean shouldDisposeRenderer = false;
        
        if (renderer == null) {
            renderer = new ShapeRenderer();
            shouldDisposeRenderer = true;
        }
        
        // End SpriteBatch before starting ShapeRenderer
        boolean batchWasDrawing = batch.isDrawing();
        if (batchWasDrawing) {
            batch.end();
        }
        
        try {
            renderer.setProjectionMatrix(batch.getProjectionMatrix());
            renderer.begin(ShapeRenderer.ShapeType.Line);
            
            // Set debug color (green for grid lines)
            renderer.setColor(Color.GREEN);
            
            // Draw grid lines
            int cellSize = world.getCellSize();
            int worldWidth = world.getWidthInPixels();
            int worldHeight = world.getHeightInPixels();
            
            // Vertical grid lines
            for (int x = 0; x <= worldWidth; x += cellSize) {
                renderer.line(x, 0, x, worldHeight);
            }
            
            // Horizontal grid lines  
            for (int y = 0; y <= worldHeight; y += cellSize) {
                renderer.line(0, y, worldWidth, y);
            }
            
            // Draw world boundary (red color)
            renderer.setColor(Color.RED);
            renderer.rect(0, 0, worldWidth, worldHeight);
            
            // Draw actor bounds (blue color)
            renderer.setColor(Color.BLUE);
            List<Actor> actors = world.getObjects(Actor.class);
            for (Actor actor : actors) {
                if (actor.getImage() != null) {
                    int actorX = actor.getX() - actor.getImage().getWidth() / 2;
                    int actorY = actor.getY() - actor.getImage().getHeight() / 2;
                    renderer.rect(actorX, actorY, actor.getImage().getWidth(), actor.getImage().getHeight());
                }
            }
            
            renderer.end();
        } finally {
            // Restart SpriteBatch if it was drawing before
            if (batchWasDrawing) {
                batch.begin();
            }
            
            // Dispose temporary renderer
            if (shouldDisposeRenderer) {
                renderer.dispose();
            }
        }
    }
    
    /**
     * Convert a location in pixels into a cell location
     */
    public static int toCellFloor(World world, int x) {
        // Simple implementation: divide by cell size and floor
        return (int) Math.floor((double) x / world.getCellSize());
    }
    
    /**
     * Returns the center of the cell. It should be rounded down with Math.floor() if the integer version is needed.
     * @param l Cell location.
     * @return Absolute location of the cell center in pixels.
     */
    public static double getCellCenter(World w, int c) {
        // Calculate center of cell: (cell * cellSize) + (cellSize / 2)
        return (c * w.getCellSize()) + (w.getCellSize() / 2.0);
    }
    
    /**
     * Get the list of all objects in the world. This returns a live list which
     * should not be modified by the caller. If iterating over this list, it
     * should be synchronized on the world lock.
     */
    public static TreeActorSet getObjectsListInPaintOrder(World world) {
        TreeActorSet actorSet = new TreeActorSet();
        List<Actor> actors = world.getObjects(Actor.class);
        actorSet.addAll(actors);
        
        // If paint order is set, arrange actors accordingly
        // The TreeActorSet will handle the ordering based on class hierarchy
        actorSet.setClassOrder(true, Actor.class); // Default paint order
        
        return actorSet;
    }
    
    /**
     * Get the list of all objects in the world. This returns a live list which
     * should not be modified by the caller. While iterating over this list, the
     * world lock should be held.
     */
    public static TreeActorSet getObjectsListInActOrder(World world) {
        TreeActorSet actorSet = new TreeActorSet();
        List<Actor> actors = world.getObjects(Actor.class);
        actorSet.addAll(actors);
        
        // If act order is set, arrange actors accordingly
        // The TreeActorSet will handle the ordering based on class hierarchy
        actorSet.setClassOrder(false, Actor.class); // Default act order
        
        return actorSet;
    }

    /**
     * Get the background image for the world, but without initialising it if it is not yet created.
     * 
     * @return Background of the world or null if not create yet.
     */
    public static GreenfootImage getBackgroundImage(World world) {
        // Return the world's background image directly
        if (world != null) {
            return world.getBackground();
        }
        return null;
    }
    
    /**
     * Get the list of text labels to be displayed on the world.
     * Returns a list of formatted strings with text and position information.
     */
    public static List<String> getTextLabels(World world) {
        List<String> labels = new ArrayList<>();
        
        if (world == null) {
            return labels;
        }
        
        // Use the package-private accessor method instead of reflection
        com.badlogic.gdx.utils.Array<World.TextLabel> textLabels = world.getTextLabels();
        
        if (textLabels != null) {
            for (World.TextLabel label : textLabels) {
                // Format as "text@(x,y)" for easy parsing
                labels.add(label.getText() + "@(" + label.getX() + "," + label.getY() + ")");
            }
        }
        
        return labels;
    }
    
    /**
     * Get detailed text label information including position data.
     * Returns a list of TextLabelInfo objects with separate text and position fields.
     */
    public static List<TextLabelInfo> getDetailedTextLabels(World world) {
        List<TextLabelInfo> labels = new ArrayList<>();
        
        if (world == null) {
            return labels;
        }
        
        // Use the package-private accessor method instead of reflection
        com.badlogic.gdx.utils.Array<World.TextLabel> textLabels = world.getTextLabels();
        
        if (textLabels != null) {
            for (World.TextLabel label : textLabels) {
                labels.add(new TextLabelInfo(label.getText(), label.getX(), label.getY()));
            }
        }
        
        return labels;
    }
    
    /**
     * Helper class to hold text label information.
     */
    public static class TextLabelInfo {
        public final String text;
        public final int x, y;
        
        public TextLabelInfo(String text, int x, int y) {
            this.text = text;
            this.x = x;
            this.y = y;
        }
        
        public String getText() { return text; }
        public int getX() { return x; }
        public int getY() { return y; }
        
        @Override
        public String toString() {
            return text + "@(" + x + "," + y + ")";
        }
    }
}
