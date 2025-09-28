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
 * LibGDX-based WorldHandler implementation.
 * 
 * This class re-implements greenfoot.core.WorldHandler to provide a LibGDX backend,
 * mainly to allow Greenfoot projects to run on LibGDX (especially to export into
 * mobile devices and other platforms).
 * 
 * Inspired by the original Greenfoot project (GPLv2+ with Classpath Exception).
 * Read the original documentation at
 * https://www.greenfoot.org/files/javadoc/greenfoot/package-summary.html
 * 
 * @author Poul Henriksen (Original Greenfoot version's author)\
 * 
 * @modified-by Qiupi3 (LibGDX wrapper implementation)
 * @version 1.0
 */

public class WorldHandler {
    private static final WorldHandler instance = new WorldHandler();

    private World currentWorld;

    private WorldHandler() {
        // private constructor: singleton
    }

    public static WorldHandler getInstance() {
        return instance;
    }

    public World getWorld() {
        return currentWorld;
    }

    public void setWorld(World world) {
        this.currentWorld = world;
    }

    public void objectAddedToWorld(Actor actor) {
        // For now, no-op. In real Greenfoot this triggers repaint/update
    }

    public void objectRemovedFromWorld(Actor actor) {
        // For now, no-op
    }

    public void setInitialisingWorld(World world) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setInitialisingWorld'");
    }

    public void repaintAndWait() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'repaintAndWait'");
    }
}
