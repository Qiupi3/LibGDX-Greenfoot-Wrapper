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
 * Class to hold data collected from mouse events during a single frame.
 * Based on the original Greenfoot MouseEventData but adapted for LibGDX.
 * 
 * This stores different types of mouse events that can happen in a frame:
 * press, click, drag, dragEnd, move - and allows prioritized access to them.
 * 
 * @author Poul Henriksen (Original Greenfoot version's author)
 * 
 * @modified-by Qiupi3 (LibGDX wrapper implementation)
 * @version 1.0
 */
class MouseEventData {
    private MouseInfo mouseInfo;
    
    // Different types of mouse events that can happen in a frame
    private MouseInfo mousePressedInfo;
    private MouseInfo mouseClickedInfo;
    private MouseInfo mouseDraggedInfo;
    private MouseInfo mouseDragEndedInfo;
    private MouseInfo mouseMovedInfo;
    
    // Reference to the event data that started a drag sequence
    private MouseEventData dragStartedBy;

    /**
     * Initialize/reset all event data for a new frame.
     */
    public void init() {
        mousePressedInfo = null;
        mouseClickedInfo = null;
        mouseDraggedInfo = null;
        mouseDragEndedInfo = null;
        mouseMovedInfo = null;
        
        if (mouseInfo != null) {
            // Retain location info and actor but clear event flags
            MouseInfo blankedMouseInfo = MouseInfoVisitor.newMouseInfo();
            MouseInfoVisitor.setLoc(blankedMouseInfo, mouseInfo.getX(), mouseInfo.getY(),
                    MouseInfoVisitor.getPx(mouseInfo), MouseInfoVisitor.getPy(mouseInfo));
            // IMPORTANT: Preserve the actor information!
            MouseInfoVisitor.setActor(blankedMouseInfo, mouseInfo.getActor());
            mouseInfo = blankedMouseInfo;
        }
    }
    
    /**
     * Get the mouse info for this frame.
     */
    public MouseInfo getMouseInfo() {
        return mouseInfo;
    }
    
    // === MOUSE PRESSED ===
    
    public boolean isMousePressed() {
        return mousePressedInfo != null;
    }
    
    public boolean isMousePressedOn(Object obj) {
        return checkObject(obj, mousePressedInfo);
    }
    
    public void mousePressed(int x, int y, int px, int py, int button) {
        init();
        mousePressedInfo = MouseInfoVisitor.newMouseInfo();
        mouseInfo = mousePressedInfo;
        MouseInfoVisitor.setButton(mouseInfo, button);
        MouseInfoVisitor.setLoc(mouseInfo, x, y, px, py);
        setActorAtLocation(x, y);
    }
    
    /**
     * Special version of mousePressed for drag start data that preserves state.
     * This should be used for dragStartData to avoid clearing existing information.
     */
    public void mousePressedForDragStart(int x, int y, int px, int py, int button) {
        // DON'T call init() - we want to preserve any existing state
        mousePressedInfo = MouseInfoVisitor.newMouseInfo();
        mouseInfo = mousePressedInfo;
        MouseInfoVisitor.setButton(mouseInfo, button);
        MouseInfoVisitor.setLoc(mouseInfo, x, y, px, py);
        setActorAtLocation(x, y);
    }
    
    // === MOUSE CLICKED ===
    
    public boolean isMouseClicked() {
        return mouseClickedInfo != null;
    }
    
    public boolean isMouseClickedOn(Object obj) {
        return checkObject(obj, mouseClickedInfo);
    }
    
    public void mouseClicked(int x, int y, int px, int py, int button, int clickCount) {
        init();
        mouseClickedInfo = MouseInfoVisitor.newMouseInfo();
        mouseInfo = mouseClickedInfo;
        MouseInfoVisitor.setButton(mouseInfo, button);
        MouseInfoVisitor.setClickCount(mouseInfo, clickCount);
        MouseInfoVisitor.setLoc(mouseInfo, x, y, px, py);
        setActorAtLocation(x, y);
    }
    
    // === MOUSE DRAGGED ===
    
    public boolean isMouseDragged() {
        return mouseDraggedInfo != null;
    }
    
    public boolean isMouseDraggedOn(Object obj) {
        return checkObject(obj, mouseDraggedInfo);
    }
    
    public void mouseDragged(int x, int y, int px, int py, int button, Actor actor) {
        init();
        mouseDraggedInfo = MouseInfoVisitor.newMouseInfo();
        mouseInfo = mouseDraggedInfo;
        MouseInfoVisitor.setButton(mouseInfo, button);
        MouseInfoVisitor.setLoc(mouseInfo, x, y, px, py);
        MouseInfoVisitor.setActor(mouseInfo, actor);
    }
    
    // === MOUSE DRAG ENDED ===
    
    public boolean isMouseDragEnded() {
        return mouseDragEndedInfo != null;
    }
    
    public boolean isMouseDragEndedOn(Object obj) {
        return checkObject(obj, mouseDragEndedInfo);
    }
    
    public void mouseDragEnded(int x, int y, int px, int py, int button, MouseEventData dragStartData) {
        // Preserve press and click info from drag end
        MouseInfo tempPressedInfo = mousePressedInfo;
        MouseInfo tempClickedInfo = mouseClickedInfo;
        init();
        mousePressedInfo = tempPressedInfo;
        mouseClickedInfo = tempClickedInfo;
        
        mouseDragEndedInfo = MouseInfoVisitor.newMouseInfo();
        mouseInfo = mouseDragEndedInfo;
        MouseInfoVisitor.setButton(mouseInfo, button);
        MouseInfoVisitor.setLoc(mouseInfo, x, y, px, py);
        MouseInfoVisitor.setActor(mouseInfo, dragStartData.getActor());
        this.dragStartedBy = dragStartData;
        
        Actor draggedActor = dragStartData.getActor();
        if (draggedActor != null) {
            // PERFORM DROP VALIDATION AND MOVEMENT
            performDropValidation(draggedActor, x, y);
        } else {
            System.out.println("[MouseEventData] ERROR: No dragged actor - user drop validation will not be called");
        }
        if (mouseInfo != null) {
            System.out.println("[MouseEventData] MouseInfo.getX/getY(): (" + mouseInfo.getX() + ", " + mouseInfo.getY() + ")");
        }
    }
    
    /**
     * Perform drop validation and movement for the dragged actor.
     * This implements the Greenfoot drag and drop logic.
     */
    private void performDropValidation(Actor draggedActor, int targetX, int targetY) {
        World world = WorldHandler.getInstance().getWorld();
        if (world == null) {
            return;
        }
        
        boolean dropValidated = true; // Default to allowing the drop
        
        // In real Greenfoot, this would call user validation methods on the target actors
        // For now, we'll implement basic validation and move the actor
        if (dropValidated) {
            draggedActor.setLocation(targetX, targetY);
        } else {
            // Could implement snap-back logic here if needed
        }
    }
    
    // === MOUSE MOVED ===
    
    public boolean isMouseMoved() {
        return mouseMovedInfo != null;
    }
    
    public boolean isMouseMovedOn(Object obj) {
        return checkObject(obj, mouseMovedInfo);
    }
    
    public void mouseMoved(int x, int y, int px, int py) {
        init();
        mouseMovedInfo = MouseInfoVisitor.newMouseInfo();
        mouseInfo = mouseMovedInfo;
        MouseInfoVisitor.setLoc(mouseInfo, x, y, px, py);
        setActorAtLocation(x, y);
    }
    
    // === UTILITY METHODS ===
    
    /**
     * Get the actor associated with the mouse event.
     */
    public Actor getActor() {
        if (mouseInfo != null) {
            return mouseInfo.getActor();
        }
        return null;
    }
    
    /**
     * Get the button associated with the mouse event.
     */
    public int getButton() {
        if (mouseInfo != null) {
            return mouseInfo.getButton();
        }
        return 0;
    }
    
    /**
     * Get the pixel X coordinate from mouse info.
     */
    public int getPixelX() {
        if (mouseInfo != null) {
            return MouseInfoVisitor.getPx(mouseInfo);
        }
        return 0;
    }
    
    /**
     * Get the pixel Y coordinate from mouse info.
     */
    public int getPixelY() {
        if (mouseInfo != null) {
            return MouseInfoVisitor.getPy(mouseInfo);
        }
        return 0;
    }
    
    /**
     * Check if an object matches the object associated with a specific mouse event.
     */
    private boolean checkObject(Object obj, MouseInfo eventInfo) {
        if (eventInfo == null) {
            return false;
        }
        
        // If obj is null, mouse action is valid anywhere
        if (obj == null) {
            return true;
        }
        
        // Get mouse coordinates from the event
        int px = eventInfo.getPx();
        int py = eventInfo.getPy();
        
        // Get current world
        World currentWorld = WorldHandler.getInstance().getWorld();
        if (currentWorld == null) {
            return false;
        }
        
        // Convert screen coordinates to world coordinates using camera
        Vector3 worldCoords = new Vector3(px, py, 0);
        currentWorld.getCamera().unproject(worldCoords);
        float worldX = worldCoords.x;
        float worldY = worldCoords.y;
        
        float correctedWorldY = currentWorld.getHeightInPixels() - worldY;
        
        if (obj instanceof Actor) {
            Actor actor = (Actor) obj;
            
            // Get actor position
            float actorX = actor.getX();
            float actorY = actor.getY();
            
            // Actor collision box
            float actorWidth = 80f;
            float actorHeight = 80f;
            
            float boxLeft = actorX - actorWidth/2;
            float boxRight = actorX + actorWidth/2;
            float boxBottom = actorY - actorHeight/2;
            float boxTop = actorY + actorHeight/2;
            
            return worldX >= boxLeft && worldX <= boxRight &&
                   correctedWorldY >= boxBottom && correctedWorldY <= boxTop;
                   
        } else if (obj instanceof World) {
            // Mouse is on world background if it's within world bounds
            World world = (World) obj;
            float worldWidth = world.getWidthInPixels();
            float worldHeight = world.getHeightInPixels();
            
            return worldX >= 0 && worldX <= worldWidth &&
                   correctedWorldY >= 0 && correctedWorldY <= worldHeight;
        }
        
        return false;
    }
    
    /**
     * Set the actor at the given location in the mouse info.
     */
    public void setActorAtLocation(int x, int y) {
        if (mouseInfo != null) {
            World world = WorldHandler.getInstance().getWorld();
            if (world != null) {
                java.util.List<Actor> actorsAtPosition = world.getObjectsAt(x, y, Actor.class);
                if (!actorsAtPosition.isEmpty()) {
                    // Get the topmost actor (first in the list for LibGDX)
                    Actor topActor = actorsAtPosition.get(0);
                    MouseInfoVisitor.setActor(mouseInfo, topActor);
                } else {
                    MouseInfoVisitor.setActor(mouseInfo, null);
                }
            }
        }
    }
    
    /**
     * If drag ended, and was started by the given MouseEventData, copy the drag-start
     * actor to the drag-end info.
     */
    public void setDragStartActor(MouseEventData dragStartData) {
        if (mouseDragEndedInfo != null && dragStartedBy == dragStartData) {
            MouseInfoVisitor.setActor(mouseDragEndedInfo, dragStartData.getActor());
        }
    }
}