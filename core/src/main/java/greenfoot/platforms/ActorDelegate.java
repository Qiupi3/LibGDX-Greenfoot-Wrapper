/*
 This file is part of the LibGDX-Greenfoot wrapper.
 Copyright (C) 2026 Qiupi3
 
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

package greenfoot.platforms;

import greenfoot.GreenfootImage;

/**
 * Interface for platform-specific actor operations.
 * This is a minimal implementation for compatibility.
 * 
 * @author Qiupi3
 * @author DavidsonRafaelK
 * @version 1.0
 */
public interface ActorDelegate {
    
    /**
     * Get image for the given class name.
     */
    GreenfootImage getImage(String className) throws Exception;
}