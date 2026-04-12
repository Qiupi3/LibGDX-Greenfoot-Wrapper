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

/**
 * Mouse event priority manager based on original Greenfoot implementation.
 * 
 * Events are prioritized in this order (highest to lowest):
 * 1. dragEnd
 * 2. click  
 * 3. press
 * 4. drag
 * 5. move
 * 
 * @author Poul Henriksen (Original Greenfoot version's author)
 * @modified-by Qiupi3 (LibGDX wrapper implementation)
 * @version 1.0
 */
class MouseEventPriorityManager {
    
    // Event type constants matching original Greenfoot
    public static final int MOUSE_DRAG_ENDED = 502;
    public static final int MOUSE_CLICKED = 500;
    public static final int MOUSE_PRESSED = 501;
    public static final int MOUSE_DRAGGED = 506;
    public static final int MOUSE_MOVED = 503;
    
    /**
     * Get the priority for an event type.
     * Lower numbers = higher priority.
     */
    public static int getPriority(int eventType) {
        switch (eventType) {
            case MOUSE_DRAG_ENDED:
                return 0; // Highest priority
            case MOUSE_CLICKED:
                return 1;
            case MOUSE_PRESSED:
                return 2;
            case MOUSE_DRAGGED:
                return 3;
            case MOUSE_MOVED:
                return 4; // Lowest priority
            default:
                return Integer.MAX_VALUE;
        }
    }
    
    /**
     * Get the priority for the current event data.
     */
    public static int getPriority(MouseEventData data) {
        if (data.isMouseDragEnded()) {
            return getPriority(MOUSE_DRAG_ENDED);
        }
        else if (data.isMouseClicked()) {
            return getPriority(MOUSE_CLICKED);
        }
        else if (data.isMousePressed()) {
            return getPriority(MOUSE_PRESSED);
        }
        else if (data.isMouseDragged()) {
            return getPriority(MOUSE_DRAGGED);
        }
        else if (data.isMouseMoved()) {
            return getPriority(MOUSE_MOVED);
        }
        else {
            return Integer.MAX_VALUE;
        }
    }
    
    /**
     * Check if the given event type has higher priority than the current event data.
     */
    public static boolean isHigherPriority(int eventType, MouseEventData currentData) {
        int newPriority = getPriority(eventType);
        int currentPriority = getPriority(currentData);
        return newPriority < currentPriority;
    }
}