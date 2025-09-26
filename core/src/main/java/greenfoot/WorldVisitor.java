package greenfoot;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import java.util.Collection;
import java.util.List;

/**
 * Class that makes it possible for classes outside the greenfoot package to get
 * access to world methods that are package protected. We need some
 * package-protected methods in the world, because we don't want them to show up
 * in the public interface visible to users.
 * 
 * Converted to work with LibGDX instead of AWT.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 */
public class WorldVisitor
{
    public static int getWidthInCells(World w)
    {
        return w.width;
    }
    
    public static int getHeightInCells(World w)
    {
        return w.height;
    }
    
    public static int getWidthInPixels(World w)
    {
        // Assume each cell is cellSize pixels wide
        return w.width * w.cellSize;
    }

    public static int getHeightInPixels(World w)
    {
        // Assume each cell is cellSize pixels tall
        return w.height * w.cellSize;
    }

    public static int getCellSize(World w)
    {
        return w.cellSize;
    }
    
    public static Collection<Actor> getObjectsAtPixel(World w, int x, int y)
    {
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
    public static void startSequence(World w)
    {
        w.startSequence();
    }

    /**
     * Paint debug information using LibGDX SpriteBatch instead of AWT Graphics.
     */
    public static void paintDebug(World world, SpriteBatch batch)
    {
        // Basic debug painting - could be enhanced with collision bounds, grid lines, etc.
        // For now, this is a placeholder that doesn't paint anything
        // In a full implementation, this would draw collision boundaries, actor bounds, etc.
        
        // Example of what could be drawn:
        // - Grid lines showing cell boundaries
        // - Actor collision rectangles  
        // - World boundaries
        // - Actor coordinates and other debug info
    }
    
    /**
     * Convert a location in pixels into a cell location
     */
    public static int toCellFloor(World world, int x)
    {
        // Simple implementation: divide by cell size and floor
        return (int) Math.floor((double) x / world.cellSize);
    }
    
    /**
     * Returns the center of the cell. It should be rounded down with Math.floor() if the integer version is needed.
     * @param l Cell location.
     * @return Absolute location of the cell center in pixels.
     */
    public static double getCellCenter(World w, int c)
    {
        // Calculate center of cell: (cell * cellSize) + (cellSize / 2)
        return (c * w.cellSize) + (w.cellSize / 2.0);
    }
    
    /**
     * Get the list of all objects in the world. This returns a live list which
     * should not be modified by the caller. If iterating over this list, it
     * should be synchronized on the world lock.
     */
    public static TreeActorSet getObjectsListInPaintOrder(World world)
    {
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
    public static TreeActorSet getObjectsListInActOrder(World world)
    {
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
    public static GreenfootImage getBackgroundImage(World world)
    {
        // Check if the world has a background texture
        if (world != null) {
            // Try to get the background color and create a GreenfootImage from it
            greenfoot.Color bgColor = world.getBackground();
            if (bgColor != null) {
                // Create a GreenfootImage with the world's dimensions and background color
                int width = world.getWidthInPixels();
                int height = world.getHeightInPixels();
                GreenfootImage bgImage = new GreenfootImage(width, height);
                bgImage.setColor(bgColor);
                bgImage.fill();
                return bgImage;
            }
        }
        return null;
    }
    
    /**
     * Get the list of text labels to be displayed on the world.
     */
    public static List<String> getTextLabels(World world)
    {
        // The World class stores text labels internally
        // We need to extract the text from the TextLabel objects
        // Since TextLabel is private in World, we'll return an empty list for now
        // In a full implementation, World would provide access to text labels
        
        // For now, return empty list as text labels are handled internally by World
        return new java.util.ArrayList<String>();
    }
}
