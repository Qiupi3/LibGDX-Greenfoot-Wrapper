package greenfoot;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;

/**
 * To get access to package private methods in MouseInfo and integrate with LibGDX input.
 * 
 * @author Poul Henriksen
 *
 */
public class MouseInfoVisitor
{
    public static void setActor(MouseInfo info, Actor actor)
    {
        info.setActor(actor);
    }    

    /**
     * Set the event location for a MouseInfo.
     * @param info   the mouseinfo object
     * @param x      the x-coordinate (in world cells)
     * @param y      the y-coordinate (in world cells)
     * @param px     the x-coordinate (in pixels)
     * @param py     the y-coordinate (in pixels)
     */
    public static void setLoc(MouseInfo info, int x, int y, int px, int py)
    {
        info.setLoc(x, y, px, py);
    }

    public static void setButton(MouseInfo info, int button)
    {
        info.setButton(button);
    }    
    
    public static MouseInfo newMouseInfo()
    {
        return new MouseInfo();
    }

    public static void setClickCount(MouseInfo mouseInfo, int clickCount)
    {
        mouseInfo.setClickCount(clickCount);
    }
    
    /**
     * Get the x-coordinate in pixels from a MouseInfo object.
     */
    public static int getPx(MouseInfo info)
    {
        return info.getPx();
    }
    
    /**
     * Get the y-coordinate in pixels from a MouseInfo object.
     */
    public static int getPy(MouseInfo info)
    {
        return info.getPy();
    }
    
    /**
     * Create a new MouseInfo with current LibGDX mouse state.
     * 
     * @return A new MouseInfo object with current mouse position and button state
     */
    public static MouseInfo newMouseInfoFromLibGDX()
    {
        MouseInfo info = new MouseInfo();
        updateMouseInfoFromLibGDX(info);
        return info;
    }
    
    /**
     * Update an existing MouseInfo object with current LibGDX mouse state.
     * 
     * @param info The MouseInfo object to update
     */
    public static void updateMouseInfoFromLibGDX(MouseInfo info)
    {
        int mouseX = Gdx.input.getX();
        int mouseY = Gdx.input.getY();
        
        // Convert LibGDX screen coordinates (top-left origin) to Greenfoot coordinates
        int convertedY = Gdx.graphics.getHeight() - mouseY;
        
        // Set both world coordinates and pixel coordinates
        info.setLoc(mouseX, convertedY, mouseX, convertedY);
        
        // Set button state based on LibGDX input
        int button = 0;
        if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
            button = 1;
        } else if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT)) {
            button = 3;
        } else if (Gdx.input.isButtonPressed(Input.Buttons.MIDDLE)) {
            button = 2;
        }
        
        info.setButton(button);
        info.setClickCount(button != 0 ? 1 : 0);
    }
    
    /**
     * Check if the mouse just clicked using LibGDX input.
     * 
     * @return true if mouse was just clicked
     */
    public static boolean isMouseJustClicked()
    {
        return Gdx.input.justTouched();
    }
}
