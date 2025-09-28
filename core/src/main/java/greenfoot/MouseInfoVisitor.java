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
 * To get access to package private methods in MouseInfo and integrate with LibGDX input.
 * 
 * This class re-implements greenfoot.MouseInfoVisitor to provide a LibGDX backend,
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
public class MouseInfoVisitor {
    public static void setActor(MouseInfo info, Actor actor) {
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
    public static void setLoc(MouseInfo info, int x, int y, int px, int py) {
        info.setLoc(x, y, px, py);
    }

    public static void setButton(MouseInfo info, int button) {
        info.setButton(button);
    }

    public static MouseInfo newMouseInfo() {
        return new MouseInfo();
    }

    public static void setClickCount(MouseInfo mouseInfo, int clickCount) {
        mouseInfo.setClickCount(clickCount);
    }
    
    /**
     * Get the x-coordinate in pixels from a MouseInfo object.
     */
    public static int getPx(MouseInfo info) {
        return info.getPx();
    }
    
    /**
     * Get the y-coordinate in pixels from a MouseInfo object.
     */
    public static int getPy(MouseInfo info) {
        return info.getPy();
    }
    
    /**
     * Create a new MouseInfo with current LibGDX mouse state.
     * 
     * @return A new MouseInfo object with current mouse position and button state
     */
    public static MouseInfo newMouseInfoFromLibGDX() {
        MouseInfo info = new MouseInfo();
        updateMouseInfoFromLibGDX(info);
        return info;
    }
    
    /**
     * Update an existing MouseInfo object with current LibGDX mouse state.
     * 
     * @param info The MouseInfo object to update
     */
    public static void updateMouseInfoFromLibGDX(MouseInfo info) {
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
    public static boolean isMouseJustClicked() {
        return Gdx.input.justTouched();
    }
}
