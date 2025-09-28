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
 * This class resolves the priorities of mouse events for the MousePollingManager.
 * 
 * Priorities with highest priority first:
 * <ul>
 * <li> dragEnd </li>
 * <li> click </li>
 * <li> press </li>
 * <li> drag </li>
 * <li> move </li>
 * </ul>
 * 
 * If several of the same type of event happens, then the last one is used.
 * 
 * @author Poul Henriksen (Original Greenfoot version's author)
 * @modified-by Assistant (LibGDX wrapper implementation)
 */
public class PriorityManager
{
    public static final int MOUSE_RELEASED = 0;  // Drag end / click
    public static final int MOUSE_CLICKED = 1;
    public static final int MOUSE_PRESSED = 2;
    public static final int MOUSE_DRAGGED = 3;
    public static final int MOUSE_MOVED = 4;

    /**
     * Returns true if the event has higher priority than what is currently
     * stored in the MouseEventData.
     */
    public static boolean isHigherPriority(int event, MouseEventData data)
    {
        return getPriority(event) <= getPriority(data);
    }

    /**
     * Priority 0 is highest.
     * @param event The event type constant
     * @return The mapped priority, to enable a comparison.
     */
    private static int getPriority(int event)
    {
        switch(event) {
            case MOUSE_RELEASED:
                return 0;
            case MOUSE_CLICKED:
                return 1;
            case MOUSE_PRESSED:
                return 2;
            case MOUSE_DRAGGED:
                return 3;
            case MOUSE_MOVED:
                return 4;
            default:
                return Integer.MAX_VALUE;
        }
    }

    /**
     * Get priority of current data.
     */
    private static int getPriority(MouseEventData data)
    {
        if(data.isMouseDragEnded()) {
            return 0;
        }
        else if(data.isMouseClicked()) {
            return 1;
        }
        else if(data.isMousePressed()) {
            return 2;
        }
        else if(data.isMouseDragged()) {
            return 3;
        }
        else if(data.isMouseMoved()) {
            return 4;
        }
        else {
            return Integer.MAX_VALUE;
        }
    }
}