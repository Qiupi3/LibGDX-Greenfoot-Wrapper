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

import greenfoot.collision.ibsp.Rect;
import greenfoot.platforms.ActorDelegate;

/**
 * Inspired by the original Greenfoot project (GPLv2+ with Classpath Exception).
 * Read the original documentation at
 * https://www.greenfoot.org/files/javadoc/greenfoot/package-summary.html
 * 
 * @author Poul Henriksen (Original Greenfoot version's author)
 */
public class ActorVisitor {
    /**
     * Set the location of an actor in pixel coordinates (for dragging operations).
     */
    public static void setLocationInPixels(Actor actor, int x, int y) {
        actor.setLocationInPixels(x, y);
    }
    
    /**
     * Get the X coordinate of an actor's position, in cells.
     */
    public static int getX(Actor actor) {
        return actor.getX();
    }
    
    /**
     * Get the Y coordinate of an actor's position, in cells.
     */
    public static int getY(Actor actor) {
        return actor.getY();
    }
    
    /**
     * Get the rotation of an actor, in degrees, from 0-359.
     */
    public static int getRotation(Actor actor) {
        return actor.getRotation();
    }
    
    /**
     * Get the world that an actor resides in (null if none).
     */
    public static World getWorld(Actor actor) {
        return actor.getWorld();
    }
   
    /**
     * Checks whether the specified point (in pixel coordinates) is within the area
     * covered by the (rotated) graphical representation of the given actor.
     * 
     * @param actor The relevant actor
     * @param px The (world relative) x pixel coordinate
     * @param py The (world relative) y pixel coordinate
     * @return true if the pixel is within the actor's bounds; false otherwise
     */
    public static boolean containsPoint(Actor actor, int px, int py) {
        return actor.containsPoint(px, py);
    }

    /**
     * Check if two actors intersect.
     */
    public static boolean intersects(Actor actor, Actor other) {
        return actor.intersects(other);
    }
    
    /**
     * Convert cell coordinate to pixel coordinate.
     */
    public static int toPixel(Actor actor, int cellCoordinate) {
        return actor.toPixel(cellCoordinate);
    }
    
    /**
     * Get the bounding rectangle of an actor.
     */
    public static Rect getBoundingRect(Actor actor) {
        return actor.getBoundingRect();
    }
    
    /**
     * Set arbitrary data on an actor.
     */
    public static void setData(Actor actor, Object data) {
        actor.setData(data);
    }
    
    /**
     * Get arbitrary data from an actor.
     */
    public static Object getData(Actor actor) {
        return actor.getData();
    }
    
    /**
     * Get the display image for an actor. This is the last image that was
     * set using setImage(). The returned image should not be modified.
     * 
     * @param actor The actor whose display image to retrieve
     */
    public static GreenfootImage getDisplayImage(Actor actor) {
        return actor.getImage();
    }

    /**
     * Get a drag image for the actor. Normally this delegates to
     * Actor.getImage(), but it will return the Greenfoot logo image
     * if that returns null.
     */
    public static GreenfootImage getDragImage(Actor actor) {
        GreenfootImage image = actor.getImage();
        if (image == null) {
            image = Actor.greenfootImage;
        }
        return image;
    }

    /**
     * Set the actor delegate for platform-specific operations.
     */
    public static void setDelegate(ActorDelegate delegate) {
        Actor.setDelegate(delegate);
    }
    
    /**
     * Get the sequence number of an actor.
     */
    public static int getSequenceNumber(Actor actor) {
        return actor.getSequenceNumber();
    }
    
    /**
     * Get the sequence number of the given actor from the last paint
     * operation on the world.
     */
    public static int getLastPaintSeqNum(Actor actor) {
        return actor.getLastPaintSeqNum();
    }
    
    /**
     * Set the sequence number of the given actor from the last paint
     * operation on the world.
     */
    public static void setLastPaintSeqNum(Actor actor, int seqNum) {
        actor.setLastPaintSeqNum(seqNum);
    }

    /**
     * Decrements the sleep counter if it is positive, and returns whether the actor was awake.
     * @param actor The actor to change the sleep counter for.
     * @return Whether the actor was awake (sleep count == 0) before this method was called.
     */
    public static boolean decrementSleepForIfPositive(Actor actor) {
        int sleepFor = actor.getSleepingFor();
        if (sleepFor > 0) {
            actor.setSleepingFor(sleepFor - 1);
        }
        // Returns true if the actor was awake (exactly zero sleep count)
        return sleepFor == 0;
    }
}
