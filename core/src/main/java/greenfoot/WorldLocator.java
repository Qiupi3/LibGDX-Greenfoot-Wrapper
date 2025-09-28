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

import com.badlogic.gdx.math.Vector3;

/**
 * World locator for converting screen coordinates to world coordinates.
 * Based on original Greenfoot coordinate conversion logic.
 * 
 * @author Poul Henriksen (Original Greenfoot version's author)
 * @modified-by Qiupi3 (LibGDX wrapper implementation)
 * @version 1.0
 */
class WorldLocator {
    private World world;
    
    /**
     * Set the world for coordinate conversion.
     */
    public void setWorld(World world) {
        this.world = world;
    }
    
    /**
     * Convert screen X coordinate to world cell coordinate.
     */
    public int getTranslatedX(int screenX) {
        if (world == null) {
            return screenX;
        }
        
        // Convert screen coordinates to world coordinates using camera
        Vector3 worldCoords = new Vector3(screenX, 0, 0);
        world.getCamera().unproject(worldCoords);
        
        // Convert to cell coordinates
        return (int)(worldCoords.x / world.getCellSize());
    }
    
    /**
     * Convert screen Y coordinate to world cell coordinate.
     */
    public int getTranslatedY(int screenY) {
        if (world == null) {
            return screenY;
        }
        
        // Convert screen coordinates to world coordinates using camera
        Vector3 worldCoords = new Vector3(0, screenY, 0);
        world.getCamera().unproject(worldCoords);
        
        // Apply Y-axis flip for Greenfoot coordinate system and convert to cells
        float greenfootPixelY = world.getHeightInPixels() - worldCoords.y;
        return (int)(greenfootPixelY / world.getCellSize());
    }
}