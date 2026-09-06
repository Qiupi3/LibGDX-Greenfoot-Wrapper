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
import com.badlogic.gdx.math.Vector3;

/**
 * Mouse manager that polls the mouse state and collects all mouse events that
 * happen in a frame. At the end of the frame the collected events are made
 * available to the simulation.
 * 
 * This is a recreation of the original Greenfoot MousePollingManager but using
 * LibGDX as the backend instead of Swing.
 * 
 * @author Poul Henriksen (Original Greenfoot version's author)
 * @modified-by Qiupi3 (LibGDX wrapper implementation)
 */
public class MousePollingManager
{
    /**
     * The current mouse data that is exposed to the simulation. This is
     * populated when newActStarted() is called. It copies data from
     * futureData and makes it available to the simulation.
     */
    private MouseEventData currentData = new MouseEventData();
    
    /**
     * Mouse data for the next act round. This is what we populate as we
     * get mouse events from LibGDX.
     */
    private MouseEventData futureData = new MouseEventData();
    
    /**
     * Used to collect data if we already have a highest priority dragEnded
     * collected. We need this in order to collect data for a potential new
     * dragEnd since we want to report the latest dragEnd in case there are more
     * than one.
     */
    private MouseEventData potentialNewDragData = new MouseEventData();
    
    /**
     * Keeps track of where a drag started. This should never be explicitly set
     * to null, because it might result in exceptions when doing undefined
     * things like dragging with two buttons down at the same time.
     */
    private MouseEventData dragStartData = new MouseEventData();

    /**
     * Track whether the mouse is currently being dragged.
     */
    private boolean isDragging;

    /**
     * Whether we have received more mouse data since we last gave data to the simulation.
     */
    private boolean gotNewEvent;
    private boolean gotNewDragStartEvent;

    // LibGDX state tracking
    private boolean wasLeftPressed = false;
    private boolean wasRightPressed = false;
    private boolean wasMiddlePressed = false;
    
    // Safety mechanism for preventing mid-air freezing
    private long dragStartTime = 0;
    private static final long MAX_DRAG_TIME_MS = 30000; // 30 seconds max drag time
    private int lastDragFrameCount = 0;
    private static final int MAX_FRAMES_WITHOUT_DRAG_EVENT = 300; // ~5 seconds at 60fps (increased for quick movements)
    
    /**
     * Creates a new mouse manager. The mouse manager should be notified
     * whenever a new act round starts by calling {@link #newActStarted()}.
     */
    public MousePollingManager()
    {
    }

    /**
     * This method should be called when a new act-loop is started.
     */
    public synchronized void newActStarted()
    {
        // Process LibGDX mouse input for this frame
        processLibGDXInput();
        
        if (gotNewEvent) {
            // Move future data to current data
            currentData = futureData;
            futureData = new MouseEventData();
            
            // Handle drag start actor tracking
            if (gotNewDragStartEvent) {
                // If a dragEnd happened this frame, copy the drag start actor to the drag
                // ended this frame (check inside setDragStartActor):
                currentData.setDragStartActor(dragStartData);
                gotNewDragStartEvent = false;
            }
            
            // Indicate that we have processed all current events.
            gotNewEvent = false;
        }
        else {
            currentData.init();
        }
    }

    /**
     * Process LibGDX mouse input and generate appropriate mouse events.
     */
    private void processLibGDXInput()
    {
        // Get current mouse state
        boolean leftPressed = Gdx.input.isButtonPressed(Input.Buttons.LEFT);
        boolean rightPressed = Gdx.input.isButtonPressed(Input.Buttons.RIGHT);
        boolean middlePressed = Gdx.input.isButtonPressed(Input.Buttons.MIDDLE);
        boolean anyPressed = leftPressed || rightPressed || middlePressed;
        
        // Get mouse position
        int screenX = Gdx.input.getX();
        int screenY = Gdx.input.getY();
        
        // Convert to world coordinates
        World world = WorldHandler.getInstance().getWorld();
        if (world == null) {
            return;
        }
        
        Vector3 worldCoords = new Vector3(screenX, screenY, 0);
        world.getCamera().unproject(worldCoords);
        
        int worldPixelX = (int)worldCoords.x;
        int worldPixelY = (int)(world.getHeightInPixels() - worldCoords.y);
        // Use proper floor division for consistent coordinate conversion
        int cellX = (int) Math.floor((double) worldPixelX / world.getCellSize());
        int cellY = (int) Math.floor((double) worldPixelY / world.getCellSize());
        
        // Detect state changes
        boolean leftJustPressed = leftPressed && !wasLeftPressed;
        boolean leftJustReleased = !leftPressed && wasLeftPressed;
        boolean rightJustPressed = rightPressed && !wasRightPressed;
        boolean rightJustReleased = !rightPressed && wasRightPressed;
        boolean middleJustPressed = middlePressed && !wasMiddlePressed;
        boolean middleJustReleased = !middlePressed && wasMiddlePressed;
        
        boolean justPressed = leftJustPressed || rightJustPressed || middleJustPressed;
        boolean justReleased = leftJustReleased || rightJustReleased || middleJustReleased;
        
        // Get button number
        int button = 0;
        if (leftPressed || leftJustPressed || leftJustReleased) {
            button = 1;
        } else if (rightPressed || rightJustPressed || rightJustReleased) {
            button = 3;
        } else if (middlePressed || middleJustPressed || middleJustReleased) {
            button = 2;
        }
        
        // Handle mouse pressed events
        if (justPressed) {
            mousePressed(cellX, cellY, worldPixelX, worldPixelY, button);
        }
        
        // Handle mouse dragged events - continue dragging if already started
        if (anyPressed && isDragging) {
            mouseDragged(cellX, cellY, worldPixelX, worldPixelY, button);
        }
        
        // Handle mouse released events
        if (justReleased) {
            mouseReleased(cellX, cellY, worldPixelX, worldPixelY, button);
        }
        
        // Handle mouse moved events (no buttons down)
        if (!anyPressed && (Gdx.input.getDeltaX() != 0 || Gdx.input.getDeltaY() != 0)) {
            mouseMoved(cellX, cellY, worldPixelX, worldPixelY);
        }
        
        // Check if dragging should start - use position-based detection for better reliability
        if (anyPressed && !isDragging) {
            // Start dragging on any mouse movement while button is pressed
            // This is more reliable than delta-based detection for quick movements
            boolean hasMovedFromPress = false;
            if (dragStartData != null && dragStartData.getActor() != null) {
                // Check if we've moved from the initial press position
                int deltaFromStart = Math.abs(worldPixelX - dragStartData.getPixelX()) + Math.abs(worldPixelY - dragStartData.getPixelY());
                hasMovedFromPress = deltaFromStart > 8; // Moderate threshold to allow UI clicks but prevent most attack drags
            }
            
            // Only start drag if significant movement OR continuous movement
            int currentDelta = Math.abs(Gdx.input.getDeltaX()) + Math.abs(Gdx.input.getDeltaY());
            if (hasMovedFromPress || currentDelta > 6) {
                isDragging = true;
                dragStartTime = System.currentTimeMillis();
                lastDragFrameCount = 0;
                mouseDragged(cellX, cellY, worldPixelX, worldPixelY, button);
            }
        }
        
        // SAFETY CHECK: Detect and fix stuck drag states
        if (isDragging) {
            lastDragFrameCount++;
            long currentTime = System.currentTimeMillis();
            boolean dragTimeout = (currentTime - dragStartTime) > MAX_DRAG_TIME_MS;
            boolean noButtonsPressed = !anyPressed;
            boolean tooManyFramesWithoutDrag = lastDragFrameCount > MAX_FRAMES_WITHOUT_DRAG_EVENT;
            
            // Force end drag if we detect a stuck state
            if (dragTimeout || noButtonsPressed || tooManyFramesWithoutDrag) {
                // Force cleanup of both systems
                forceDragCleanup(cellX, cellY, worldPixelX, worldPixelY, button);
            }
        }
        
        // Validate drag state consistency between systems
        boolean worldHandlerDragging = WorldHandler.getInstance().isDragging();
        if (isDragging != worldHandlerDragging) {
            // Sync states - prefer the more conservative approach (end drag)
            if (!isDragging && worldHandlerDragging) {
                WorldHandler.getInstance().endDrag();
            } else if (isDragging && !worldHandlerDragging) {
                forceDragCleanup(cellX, cellY, worldPixelX, worldPixelY, button);
            }
        }
        
        // Update state tracking
        wasLeftPressed = leftPressed;
        wasRightPressed = rightPressed;
        wasMiddlePressed = middlePressed;
    }

    /**
     * The mouse got pressed on the given world location
     * @param x The cell location in the world
     * @param y The cell location in the world
     * @param px The pixel location in the world
     * @param py The pixel location in the world
     * @param button The button that was pressed.
     */
    private synchronized void mousePressed(int x, int y, int px, int py, int button)
    {
        MouseEventData mouseData = futureData;
        // In case we already have a dragEnded and we get another
        // dragEnded, we need to start collection data for that.
        if (futureData.isMouseDragEnded())
        {
            mouseData = potentialNewDragData;
        }
    
        // This might be the beginning of a drag so we store it
        dragStartData = new MouseEventData();
        dragStartData.mousePressedForDragStart(x, y, px, py, button);
        gotNewDragStartEvent = true;
        
        // We only really want to register this event as a press if there is no higher priorities
        if (!PriorityManager.isHigherPriority(PriorityManager.MOUSE_PRESSED, mouseData))
        {
            return;
        }
        registerEventReceived();
        mouseData.mousePressed(x, y, px, py, button);
        isDragging = false;
        
        // AUTOMATIC DRAG INITIATION: If there's an actor at this location, prepare for potential drag
        World world = WorldHandler.getInstance().getWorld();
        if (world != null && dragStartData.getActor() != null) {
            // Clear any previous drag state first
            MouseInfo.setDraggedObject(null);
        }
    }

    /**
     * The mouse got released at the given world location
     * @param x The cell location in the world
     * @param y The cell location in the world
     * @param px The pixel location in the world
     * @param py The pixel location in the world
     * @param button The button that was released.
     */
    private synchronized void mouseReleased(int x, int y, int px, int py, int button)
    {
        // SAFETY: Always clean up WorldHandler drag state on any mouse release
        boolean wasDragging = WorldHandler.getInstance().isDragging();
        if (wasDragging) {
            WorldHandler.getInstance().endDrag();
            MouseInfo.setDraggedObject(null);
        }
        
        // This might be the end of a drag
        if(isDragging)
        {
            // In case we already have a dragEnded and we get another
            // dragEnded, should use the new one
            if (futureData.isMouseDragEnded())
            {
                futureData = potentialNewDragData;
            }
            
            if (!PriorityManager.isHigherPriority(PriorityManager.MOUSE_RELEASED, futureData))
            {
                return;
            }
            registerEventReceived();
            
            futureData.mouseClicked(x, y, px, py, button, 1);
            futureData.mouseDragEnded(x, y, px, py, button, dragStartData);
            
            // Reset drag state
            isDragging = false;
            dragStartTime = 0;
            lastDragFrameCount = 0;
            potentialNewDragData = new MouseEventData();
        }
        else
        {
            // Just a normal click
            if (!PriorityManager.isHigherPriority(PriorityManager.MOUSE_RELEASED, futureData))
            {
                return;
            }
            registerEventReceived();
            
            futureData.mouseClicked(x, y, px, py, button, 1);
        }
    }

    /**
     * The mouse got dragged to the given world location
     * @param x The cell location in the world
     * @param y The cell location in the world
     * @param px The pixel location in the world
     * @param py The pixel location in the world
     * @param button The button that is being dragged.
     */
    private synchronized void mouseDragged(int x, int y, int px, int py, int button)
    {
        if (!PriorityManager.isHigherPriority(PriorityManager.MOUSE_DRAGGED, futureData))
        {
            return;
        }
        registerEventReceived();
        
        // AUTOMATIC DRAG SYSTEM INTEGRATION: Only start visual dragging if this is actually a drag
        // Don't start drag for quick clicks (likely attacks) - require actual mouse movement
        Actor dragActor = dragStartData.getActor();
        if (dragActor != null && !WorldHandler.getInstance().isDragging()) {
            // Calculate movement distance since press
            int pressX = dragStartData.getPixelX();
            int pressY = dragStartData.getPixelY();
            int dragDistance = (int) Math.sqrt(Math.pow(px - pressX, 2) + Math.pow(py - pressY, 2));
            
            // Only start drag if mouse has moved significantly (more than 5 pixels)
            // This prevents attack clicks from triggering drag visuals
            if (dragDistance > 5) {
                // Convert coordinates to Point for WorldHandler
                greenfoot.awt.Point dragPoint = new greenfoot.awt.Point(px, py);
                
                // Start the visual drag system
                WorldHandler.getInstance().startDrag(dragActor, dragPoint, 1);
                
                // Set global drag state
                MouseInfo.setDraggedObject(dragActor);
            } else {
            
            }
        }
        
        // Find and store the actor that relates to this drag.
        futureData.mouseDragged(x, y, px, py, dragStartData.getButton(), dragStartData.getActor());
        
        // Reset frame counter since we're actively dragging
        lastDragFrameCount = 0;
    }

    /**
     * The mouse was moved to the given world location (no buttons down)
     * @param x The cell location in the world
     * @param y The cell location in the world
     * @param px The pixel location in the world
     * @param py The pixel location in the world
     */
    private synchronized void mouseMoved(int x, int y, int px, int py)
    {
        if (!PriorityManager.isHigherPriority(PriorityManager.MOUSE_MOVED, futureData))
        {
            return;
        }
        registerEventReceived();
        
        futureData.mouseMoved(x, y, px, py);
    }

    /**
     * Mark that we have received a new event.
     */
    private void registerEventReceived()
    {
        gotNewEvent = true;
    }
    
    /**
     * Force cleanup of stuck drag states to prevent mid-air freezing.
     * This is a safety mechanism that preserves drop validation functionality.
     */
    private void forceDragCleanup(int x, int y, int px, int py, int button) {
        // Clean up WorldHandler drag state first
        if (WorldHandler.getInstance().isDragging()) {
            WorldHandler.getInstance().endDrag();
        }
        
        // Clear global drag state
        MouseInfo.setDraggedObject(null);
        
        // If we have valid drag start data, attempt proper drop validation
        if (dragStartData != null && dragStartData.getActor() != null) {
            // Create a proper drag end event to trigger drop validation
            if (futureData.isMouseDragEnded()) {
                futureData = potentialNewDragData;
            }
            
            futureData.mouseClicked(x, y, px, py, button, 1);
            futureData.mouseDragEnded(x, y, px, py, button, dragStartData);
            
            registerEventReceived();
        }
        
        // Reset drag state
        isDragging = false;
        dragStartTime = 0;
        lastDragFrameCount = 0;
        
        // Prepare for next potential drag
        potentialNewDragData = new MouseEventData();
    }

    // Public API methods for simulation access
    
    public boolean isMousePressed(Object obj)
    {
        return currentData.isMousePressedOn(obj);
    }

    public boolean isMouseClicked(Object obj)
    {
        return currentData.isMouseClickedOn(obj);
    }

    public boolean isMouseDragged(Object obj)
    {
        return currentData.isMouseDraggedOn(obj);
    }

    public boolean isMouseDragEnded(Object obj)
    {
        return currentData.isMouseDragEndedOn(obj);
    }

    public boolean isMouseMoved(Object obj)
    {
        return currentData.isMouseMovedOn(obj);
    }
    
    // General methods (no specific object)
    public boolean isMousePressed()
    {
        return currentData.isMousePressed();
    }

    public boolean isMouseClicked()
    {
        return currentData.isMouseClicked();
    }

    public boolean isMouseDragged()
    {
        return currentData.isMouseDragged();
    }

    public boolean isMouseDragEnded()
    {
        return currentData.isMouseDragEnded();
    }

    public boolean isMouseMoved()
    {
        return currentData.isMouseMoved();
    }

    /**
     * Gets the mouse info with information about the current state of the
     * mouse. Within the same act-loop it will always return exactly the same
     * MouseInfo object with exactly the same contents.
     * 
     * @return The info about the current state of the mouse; Null if the mouse is outside
     *         the world boundaries (unless being dragged).
     */
    public MouseInfo getMouseInfo()
    {
        return currentData.getMouseInfo();
    }
}