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

package greenfoot.collision;

import java.util.List;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import greenfoot.Actor;

/**
 * LibGDX-based collision checker interface for managing actor collisions and spatial queries.
 * 
 * This class re-implements greenfoot.collision.CollisionChecker to provide a LibGDX backend,
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
public interface CollisionChecker {
    
    /**
     * Initialize the collision checker with world parameters.
     */
    public void initialize(int worldWidth, int worldHeight, int cellSize, boolean wrap);

    /**
     * Add an actor to the collision system.
     */
    public void addObject(Actor actor);
    
    /**
     * Remove an actor from the collision system.
     */
    public void removeObject(Actor object);

    /**
     * Update an actor's location in the collision system.
     */
    public void updateObjectLocation(Actor object, int oldX, int oldY);

    /**
     * Update an actor's size in the collision system.
     */
    public void updateObjectSize(Actor object);

    /**
     * Get all objects of the specified type at a grid location.
     */
    public <T extends Actor> List<T> getObjectsAt(int x, int y, Class<T> cls);

    /**
     * Get all objects of the specified type that intersect with the given actor.
     */
    public <T extends Actor> List<T> getIntersectingObjects(Actor actor, Class<T> cls);
    
    /**
     * Get all objects of the specified type within a radius of a point.
     */
    public <T extends Actor> List<T> getObjectsInRange(int x, int y, int r, Class<T> cls);
    
    /**
     * Get all neighboring objects of the specified type within a distance.
     */
    public <T extends Actor> List<T> getNeighbours(Actor actor, int distance, boolean diag, Class<T> cls);
    
    /**
     * Get all objects of the specified type in a direction from a point.
     */
    public <T extends Actor> List<T> getObjectsInDirection(int x, int y, int angle, int length, Class<T> cls);
    
    /**
     * Get all objects of the specified type in the world.
     */
    public <T extends Actor> List<T> getObjects(Class<T> cls);

    /**
     * Get all actors in the world as a list.
     */
    public List<Actor> getObjectsList();

    /**
     * Start a new sequence (called each simulation step).
     */
    public void startSequence();

    /**
     * Get one object of the specified type at an offset from the given actor.
     */
    public <T extends Actor> T getOneObjectAt(Actor object, int dx, int dy, Class<T> cls);

    /**
     * Get one object of the specified type that intersects with the given actor.
     */
    public <T extends Actor> T getOneIntersectingObject(Actor object, Class<T> cls);

    /**
     * Render debug information using LibGDX SpriteBatch (optional).
     */
    public void renderDebug(SpriteBatch batch);
}
