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

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;

/**
 * LibGDX-based implementation of the MouseInfo class.
 * Uses LibGDX input system for mouse tracking.
 * This class contains information about the current status of the mouse. You
 * can get a MouseInfo object via {@link Greenfoot#getMouseInfo()}.
 * 
 * This class re-implements greenfoot.MouseInfo to provide a LibGDX backend,
 * mainly to allow Greenfoot projects to run on LibGDX (especially to export into
 * mobile devices and other platforms).
 * 
 * Inspired by the original Greenfoot project (GPLv2+ with Classpath Exception).
 * Read the original documentation at
 * https://www.greenfoot.org/files/javadoc/greenfoot/MouseInfo.html
 * 
 * @see Greenfoot#getMouseInfo()
 * @author Poul Henriksen (Original Greenfoot version's author)
 * 
 * @modified-by Qiupi3 (LibGDX wrapper implementation)
 * @version 1.0
 */
public class MouseInfo {
    private Actor actor;
    private int button;
    private int x;
    private int y;
    // px and py are pixel coordinates (x and y are world cell coordinates):
    private int px;
    private int py;
    private int clickCount;
    
    // Static drag state management
    private static Actor draggedObject = null;
    private static boolean isDragging = false;

    /**
     * Do not create your own MouseInfo objects. Use
     * {@link Greenfoot#getMouseInfo() getMouseInfo()}.
     * 
     * @see Greenfoot#getMouseInfo()
     */
    MouseInfo() {  }
    
    /**
     * Return the current x position of the mouse cursor.
     * 
     * @return the x position in grid coordinates
     */
    public int getX() {
        return x;
    }

    /**
     * Return the current y position of the mouse cursor.
     * 
     * @return the y position in grid coordinates
     */
    public int getY() {
        return y;
    }
    
    /**
     * Return the actor (if any) that the current mouse behaviour is related to.
     * If the mouse was clicked or pressed the actor it was clicked on will be
     * returned. If the mouse was dragged or a drag ended, the actor where the
     * drag started will be returned. If the mouse was moved, it will return the
     * actor that the mouse is currently over.
     * 
     * @return Actor that the current mouse behaviour relates to, or null if
     *         there is no actor related to current behaviour. 
     */
    public Actor getActor() {
        return actor;
    }
    
    /**
     * The number of the pressed or clicked button (if any).
     * 
     * @return The button number. Usually 1 is the left button, 2 is the middle
     *         button and 3 is the right button.
     */
    public int getButton() {
        return button;
    }

    /**
     * Return the number of mouse clicks associated with this mouse event.
     * @return The number of times a button has been clicked.
     */
    public int getClickCount() {
        return clickCount;
    }
    
    void setButton(int button) {
        this.button = button;
    }

    void setLoc(int x, int y, int px, int py) {
        this.x = x;
        this.y = y;
        this.px = px;
        this.py = py;
    }

    void setActor(Actor actor) {
        this.actor = actor;
    }

    void setClickCount(int clickCount) {
        this.clickCount = clickCount;
    }
    
    /**
     * Update this MouseInfo with current LibGDX mouse state.
     * This method integrates with LibGDX input system and uses consistent coordinate conversion.
     */
    void updateFromLibGDX() {
        int mouseX = Gdx.input.getX();
        int mouseY = Gdx.input.getY();
        
        // Get current world for proper coordinate conversion
        World currentWorld = WorldHandler.getInstance().getWorld();
        
        if (currentWorld != null) {
            // Convert screen coordinates to world coordinates using camera unproject (same as other systems)
            com.badlogic.gdx.math.Vector3 worldCoords = new com.badlogic.gdx.math.Vector3(mouseX, mouseY, 0);
            currentWorld.getCamera().unproject(worldCoords);
            
            // Set pixel coordinates (world space)
            this.px = (int) worldCoords.x;
            this.py = (int) (currentWorld.getHeightInPixels() - worldCoords.y);
            
            // Convert to cell coordinates using consistent method
            this.x = (int) (worldCoords.x / currentWorld.getCellSize());
            this.y = (int) ((currentWorld.getHeightInPixels() - worldCoords.y) / currentWorld.getCellSize());
        } else {
            // Fallback: Convert LibGDX screen coordinates directly
            this.px = mouseX;
            this.py = Gdx.graphics.getHeight() - mouseY;
            this.x = mouseX;
            this.y = Gdx.graphics.getHeight() - mouseY;
        }
        
        // Determine current button state using LibGDX
        if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
            this.button = 1;
        } else if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT)) {
            this.button = 3;
        } else if (Gdx.input.isButtonPressed(Input.Buttons.MIDDLE)) {
            this.button = 2;
        } else {
            this.button = 0; // No button pressed
        }
        
        // Click count would need to be tracked separately in a mouse listener
        // For now, default to 1 if button is pressed
        this.clickCount = (this.button != 0) ? 1 : 0;
    }
    
    /**
     * Helper method to get current mouse button state from LibGDX.
     * 
     * @return Current mouse button pressed, or 0 if none
     */
    public static int getCurrentButton() {
        if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
            return 1;
        } else if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT)) {
            return 3;
        } else if (Gdx.input.isButtonPressed(Input.Buttons.MIDDLE)) {
            return 2;
        }
        return 0;
    }
    
    /**
     * Get the x position, in pixel coordinates.
     */
    int getPx() {
        return px;
    }
    
    /**
     * Get the y position, in pixel coordinates.
     */
    int getPy() {
        return py;
    }
    
    public String toString() {
        return "MouseInfo. Actor: " + actor + "  Location: (" + x + "," + y + ")  Button: " + button + " Click Count: " + clickCount;
    }
    
    // Drag functionality
    public static Actor getDraggedObject() {
        return draggedObject;
    }
    
    public static boolean isDragging() {
        return isDragging;
    }
    
    public static void setDraggedObject(Actor actor) {
        draggedObject = actor;
        isDragging = actor != null;
    }
}
