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

package greenfoot.collision.ibsp;

/**
 * Simple rectangle class for collision detection compatibility.
 * This is a minimal implementation to support ActorVisitor requirements.
 * 
 * @author Qiupi3
 * @version 1.0
 */
public class Rect {
    private final int x, y, width, height;
    
    public Rect(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
    
    public int getX() {
        return x;
    }
    
    public int getY() {
        return y;
    }
    
    public int getWidth() {
        return width;
    }
    
    public int getHeight() {
        return height;
    }
    
    public int getRight() {
        return x + width;
    }
    
    public int getTop() {
        return y + height;
    }
    
    public boolean contains(int px, int py) {
        return px >= x && px <= getRight() && py >= y && py <= getTop();
    }
    
    @Override
    public String toString() {
        return "Rect[x=" + x + ",y=" + y + ",w=" + width + ",h=" + height + "]";
    }
}