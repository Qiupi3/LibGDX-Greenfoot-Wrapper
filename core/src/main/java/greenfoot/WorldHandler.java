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
import greenfoot.awt.Point;

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
 * @author Poul Henriksen (Original Greenfoot version's author)
 * 
 * @modified-by Qiupi3 (LibGDX wrapper implementation)
 * @version 1.0
 */
public class WorldHandler {
    /** The singleton instance */
    private static WorldHandler instance;
    
    /** The currently active world */
    private World world;
    
    /** Flag to check whether a world has been set */
    private boolean worldIsSet;
    
    /** Stack trace for debugging world access issues */
    private Exception worldAccessTrace;
    
    // Drag and drop support
    private int dragBeginX;
    private int dragBeginY;
    private Actor dragActor;
    private boolean dragActorMoved;
    private int dragId;
    
    // Movement threshold for repainting during drag (pixels)
    private static final int DRAG_REPAINT_THRESHOLD = 1; // Reduced for better responsiveness during quick movements
    private int lastDragX = -1;
    private int lastDragY = -1;
    
    // Keyboard state tracking for LibGDX
    private boolean[] keyPressed = new boolean[256];
    private boolean[] keyJustPressed = new boolean[256];
    private boolean[] keyJustReleased = new boolean[256];
    
    /**
     * Private constructor to enforce singleton pattern.
     */
    private WorldHandler() {
        Gdx.app.log("WorldHandler", "WorldHandler instance created");
    }
    
    /**
     * Get the singleton instance of WorldHandler.
     * Creates the instance if it doesn't exist.
     * 
     * @return The WorldHandler singleton instance
     */
    public static synchronized WorldHandler getInstance() {
        if (instance == null) {
            instance = new WorldHandler();
        }
        return instance;
    }
    
    /**
     * Check whether a world has been set since the flag was last cleared.
     */
    public synchronized boolean checkWorldSet() {
        return worldIsSet;
    }
    
    /**
     * Clear the "world is set" flag.
     */
    public synchronized void clearWorldSet() {
        worldIsSet = false;
    }
    
    /**
     * Set the current world.
     * This is called when a new world is created or when switching between worlds.
     * 
     * @param newWorld The world to set as current
     * @param byUserCode Whether this was set by user code (Greenfoot.setWorld)
     */
    public synchronized void setWorld(World newWorld, boolean byUserCode) {
        if (newWorld != null) {
            // Store reference to old world for potential cleanup
            World oldWorld = this.world;
            
            // Set new world reference BEFORE calling setScreen to avoid race conditions
            this.world = newWorld;
            this.worldIsSet = true;
            this.worldAccessTrace = new Exception("World set at this location");
            
            // Also update the LibGDX screen - this is crucial for screen switching
            try {
                // Use the GreenfootGame static instance to set the screen
                id.qiupi3.greenfoot.GreenfootGame gameInstance = id.qiupi3.greenfoot.GreenfootGame.getInstance();
                if (gameInstance != null) {
                    // LibGDX's setScreen will automatically dispose the old screen (world)
                    // so we need to be careful about the order of operations
                    // Wrap in the Screen adapter: World is not a Screen, so that a
                    // project's own show()/hide()/dispose() are never called by LibGDX.
                    gameInstance.setScreen(newWorld.getScreen());
                } else {
                    System.err.println("WorldHandler: GreenfootGame instance is null, cannot switch screen");
                }
            } catch (Exception e) {
                System.err.println("WorldHandler: Failed to switch screen: " + e.getMessage());
                e.printStackTrace();
                // If screen switching failed, revert the world reference
                this.world = oldWorld;
            }
        } else {
            Gdx.app.error("WorldHandler", "Attempted to set null world!");
            this.worldAccessTrace = new Exception("Null world set at this location");
        }
    }
    
    /**
     * Set the current world (compatibility method).
     * 
     * @param newWorld The world to set as current
     */
    public synchronized void setWorld(World newWorld) {
        setWorld(newWorld, false);
    }
    
    /**
     * Get the current world.
     * 
     * @return The currently active world, or null if no world is set
     */
    public synchronized World getWorld() {
        if (world == null && worldAccessTrace != null) {
            System.out.println("WorldHandler: getWorld() called but world is null!");
            System.out.println("Last world access trace:");
            worldAccessTrace.printStackTrace();
        }
        return world;
    }
    
    /**
     * Check if a world is currently set.
     * 
     * @return true if a world is set, false otherwise
     */
    public synchronized boolean hasWorld() {
        return world != null;
    }
    
    /**
     * Removes the current world.
     */
    public synchronized void discardWorld() {
        if (world != null) {
            World discardedWorld = world;
            this.world = null;
            this.worldAccessTrace = new Exception("World discarded at this location");
            Gdx.app.log("WorldHandler", "World discarded: " + discardedWorld.getClass().getSimpleName());
        }
    }
    
    /**
     * Clear the current world.
     * This is typically called when ending or restarting a scenario.
     */
    public synchronized void clearWorld() {
        discardWorld();
    }
    
    /**
     * Special method for world initialization tracking.
     * This method exists for compatibility with Greenfoot debugging tools.
     * 
     * @param world The world being initialized
     */
    public void setInitialisingWorld(World world) {
        // This method is referenced by name in debugging tools
        // For LibGDX implementation, we just log this event
        Gdx.app.log("WorldHandler", "Initializing world: " + world.getClass().getSimpleName());
    }
    
    /**
     * Instantiate a new world by class name.
     * 
     * @param className The fully qualified name of the world class
     */
    public void instantiateNewWorld(String className) {
        try {
            Class<?> worldClass = Class.forName(className);
            World newWorld = (World) worldClass.getDeclaredConstructor().newInstance();
            setWorld(newWorld, false);
        } catch (Exception e) {
            Gdx.app.error("WorldHandler", "Failed to instantiate world: " + className, e);
        }
    }
    
    // === Drag and Drop Support ===
    
    /**
     * Process mouse events and automatically handle drag operations.
     * Process mouse events to handle drag operations.
     * Only handles drag continuation and ending - drag starting is controlled by user-project code.
     */
    public void processMouseEvents() {
        if (world == null) return;
        
        // Check if mouse was just pressed this frame
        boolean leftPressed = com.badlogic.gdx.Gdx.input.isButtonPressed(com.badlogic.gdx.Input.Buttons.LEFT);
        boolean wasLeftPressed = wasLeftButtonPressed;
        wasLeftButtonPressed = leftPressed;
        
        boolean justReleased = !leftPressed && wasLeftPressed;
        
        // Handle mouse release - end any active drag
        if (justReleased && isDragging()) {
            endDrag();
            return;
        }
        
        // Handle mouse drag - update drag position (only if already dragging)
        if (leftPressed && isDragging()) {
            updateDragPosition();
        }
    }
    
    // Mouse state tracking for WorldHandler  
    private boolean wasLeftButtonPressed = false;
    
    /**
     * Update drag position during mouse movement.
     * Implements movement threshold to reduce repainting frequency.
     */
    private void updateDragPosition() {
        if (dragActor == null || world == null) return;
        
        // SAFETY CHECK: Verify drag actor is still valid
        if (dragActor.getWorld() == null) {
            System.out.println("[WorldHandler] SAFETY: Drag actor no longer in world - ending drag");
            endDrag();
            return;
        }
        
        // Get current mouse position
        int mouseX = com.badlogic.gdx.Gdx.input.getX();
        int mouseY = com.badlogic.gdx.Gdx.input.getY();
        
        // SAFETY CHECK: If no mouse buttons are pressed, end drag
        if (!com.badlogic.gdx.Gdx.input.isButtonPressed(com.badlogic.gdx.Input.Buttons.LEFT) &&
            !com.badlogic.gdx.Gdx.input.isButtonPressed(com.badlogic.gdx.Input.Buttons.RIGHT) &&
            !com.badlogic.gdx.Gdx.input.isButtonPressed(com.badlogic.gdx.Input.Buttons.MIDDLE)) {
            System.out.println("[WorldHandler] SAFETY: No mouse buttons pressed during drag - ending drag");
            endDrag();
            return;
        }
        
        // SAFETY CHECK: Verify mouse position is within reasonable bounds
        if (mouseX < -100 || mouseX > com.badlogic.gdx.Gdx.graphics.getWidth() + 100 ||
            mouseY < -100 || mouseY > com.badlogic.gdx.Gdx.graphics.getHeight() + 100) {
            System.out.println("[WorldHandler] SAFETY: Mouse position out of reasonable bounds (" + mouseX + ", " + mouseY + ") - ending drag");
            endDrag();
            return;
        }
        
        // Check if we've moved enough to warrant an update
        if (lastDragX != -1 && lastDragY != -1) {
            int deltaX = Math.abs(mouseX - lastDragX);
            int deltaY = Math.abs(mouseY - lastDragY);
            
            if (deltaX < DRAG_REPAINT_THRESHOLD && deltaY < DRAG_REPAINT_THRESHOLD) {
                return; // Not enough movement, skip update
            }
        }
        
        // Update last position
        lastDragX = mouseX;
        lastDragY = mouseY;
        
        // Convert to world coordinates
        com.badlogic.gdx.math.Vector3 worldCoords = new com.badlogic.gdx.math.Vector3(mouseX, mouseY, 0);
        world.getCamera().unproject(worldCoords);
        
        // Apply Y-axis flip for Greenfoot coordinate system (Y increases downward from top-left)
        int pixelX = (int) worldCoords.x;
        int pixelY = (int) (world.getHeightInPixels() - worldCoords.y);
        
        // Update drag position with pixel coordinates (original Greenfoot behavior)
        drag(dragActor, new Point(pixelX, pixelY));
    }
    
    /**
     * Check if currently dragging an actor.
     */
    public boolean isDragging() {
        return dragActor != null;
    }
    
    /**
     * Start dragging an actor.
     */
    public void startDrag(Actor actor, Point p, int dragId) {
        if (world == null || actor == null) return;
        
        dragActor = actor;
        dragActorMoved = false;
        
        // Reset movement tracking
        lastDragX = -1;
        lastDragY = -1;
        
        // Get actor position in pixel coordinates (with Y-axis flip to match mouse coordinates)
        int cellSize = world.getCellSize();
        int actorPixelX = actor.getX() * cellSize + cellSize / 2;
        // Convert actor's cell Y to pixel Y with Y-axis flip to match input coordinate system
        int actorPixelY = world.getHeightInPixels() - (actor.getY() * cellSize + cellSize / 2);
        
        // Store begin position in pixel coordinates for potential snap-back (with Y-axis flip)
        dragBeginX = actorPixelX;
        dragBeginY = actorPixelY;
        
        this.dragId = dragId;
        
        // Set visual feedback for dragging
        actor.setBeingDragged(true);
        
        // Start with initial drag position
        drag(actor, p);
    }
    
    /**
     * Continue dragging an actor.
     */
    public void continueDragging(int dragId, int x, int y) {
        if (this.dragId == dragId && dragActor != null) {
            drag(dragActor, new Point(x, y));
        }
    }
    
    /**
     * Handle drag on actors that are already in the world.
     * This follows the original Greenfoot implementation pattern.
     */
    public boolean drag(Object o, Point p) {
        World world = this.world;
        if (o instanceof Actor && world != null) {
            // Clamp pixel coordinates to world bounds to prevent out-of-bounds issues
            int clampedPixelX = Math.max(0, Math.min((int) p.getX(), WorldVisitor.getWidthInPixels(world) - 1));
            int clampedPixelY = Math.max(0, Math.min((int) p.getY(), WorldVisitor.getHeightInPixels(world) - 1));
            
            int x = WorldVisitor.toCellFloor(world, clampedPixelX);
            int y = WorldVisitor.toCellFloor(world, clampedPixelY);
            final Actor actor = (Actor) o;
            
            try {
                int oldX = ActorVisitor.getX(actor);
                int oldY = ActorVisitor.getY(actor);

                if (oldX != x || oldY != y) {
                    if (x < WorldVisitor.getWidthInCells(world) && y < WorldVisitor.getHeightInCells(world)
                            && x >= 0 && y >= 0) {
                        // Set location using clamped pixel coordinates for smooth movement
                        ActorVisitor.setLocationInPixels(actor, clampedPixelX, clampedPixelY);
                        dragActorMoved = true;
                        repaint();
                    } else {
                        ActorVisitor.setLocationInPixels(actor, dragBeginX, dragBeginY);
                        x = WorldVisitor.toCellFloor(getWorld(), dragBeginX);
                        y = WorldVisitor.toCellFloor(getWorld(), dragBeginY);
                        
                        dragActorMoved = false; // Pinged back to where it was
                        repaint();
                        return false;
                    }
                }
            } catch (IndexOutOfBoundsException e) {
                // Handle bounds exception
                System.out.println("[WorldHandler] IndexOutOfBoundsException during drag: " + e.getMessage());
            } catch (IllegalStateException e) {
                // If World.addObject() has been overridden the actor might not
                // have been added to the world and we will get this exception
                System.out.println("[WorldHandler] IllegalStateException during drag: " + e.getMessage());
            }
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Handle drop operation following original Greenfoot implementation.
     */
    public boolean drop(Object o, Point p) {
        final World world = this.world;
        
        int maxHeight = WorldVisitor.getHeightInPixels(world);
        int maxWidth = WorldVisitor.getWidthInPixels(world);
        final int x = (int) p.getX();
        final int y = (int) p.getY();

        if (x >= maxWidth || y >= maxHeight || x < 0 || y < 0) {
            return false;
        } else if (o instanceof Actor && ActorVisitor.getWorld((Actor) o) == null) {
            // object received from the inspector via the Get button.
            Actor actor = (Actor) o;
            addActorAtPixel(actor, x, y);
            return true;
        } else if (o instanceof Actor) {
            final Actor actor = (Actor) o;
            if (ActorVisitor.getWorld(actor) == null) {
                // Under some strange circumstances the world can be null here.
                // This can happen in the GridWorld scenario because it
                // overrides World.addObject().
                return false;
            }
            // Use direct call instead of Simulation.runLater since we don't have Simulation class
            ActorVisitor.setLocationInPixels(actor, x, y);
            dragActorMoved = true;
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Finish drag operation.
     */
    public void finishDrag(int dragId, int ax, int ay) {
        if (this.dragId == dragId && dragActor != null) {
            if (dragActorMoved) {
                int beginCellX = dragBeginX / world.getCellSize();
                int beginCellY = (world.getHeightInPixels() - dragBeginY) / world.getCellSize();
                dragActor.setLocation(beginCellX, beginCellY);
                dragActor.setLocation(ax, ay);
            }
            dragActor = null;
        }
    }
    
    /**
     * End drag operation immediately.
     * Called when mouse button is released to stop drag operations.
     * Implements proper grid snapping and drop validation like original Greenfoot.
     */
    public void endDrag() {
        if (dragActor != null) {
            System.out.println("[WorldHandler] endDrag() - Ending drag for " + dragActor.getClass().getSimpleName());
            
            // Reset visual feedback
            dragActor.setBeingDragged(false);
            
            // IMPORTANT: Do NOT automatically move the actor here!
            // Let the Greenfoot event system (MouseEventData.mouseDragEnded) handle drop validation
            // and actor movement. This method should only clean up the visual drag state.
            
            // Clear drag state completely
            dragActor = null;
            dragActorMoved = false;
            dragId = 0;
            lastDragX = -1;
            lastDragY = -1;
        } else {
            System.out.println("[WorldHandler] endDrag() - No drag actor to clean up");
        }
        
        // SAFETY: Always clear global drag state when ending drag
        MouseInfo.setDraggedObject(null);
    }
    
    /**
     * Validate if an actor can be dropped at the specified location.
     * This method implements basic Greenfoot drop validation rules.
     * It can be overridden by subclasses to implement custom drop validation.
     * 
     * @param actor The actor being dropped
     * @param cellX The target cell X coordinate
     * @param cellY The target cell Y coordinate
     * @return true if the actor can be dropped at this location, false otherwise
     */
    protected boolean canDropAt(Actor actor, int cellX, int cellY) {
        // Basic bounds check (already done by caller, but defensive programming)
        if (cellX < 0 || cellX >= world.getWidth() || 
            cellY < 0 || cellY >= world.getHeight()) {
            return false;
        }
        
        // Original Greenfoot behavior: Check if there's already an actor of the same type
        // This prevents stacking identical objects (standard Greenfoot collision behavior)
        java.util.List<? extends Actor> actorsAtLocation = world.getObjectsAt(cellX, cellY, actor.getClass());
        if (!actorsAtLocation.isEmpty()) {
            // Allow dropping on self (no movement) but not on other actors of same type
            for (Actor existingActor : actorsAtLocation) {
                if (existingActor != actor) {
                    return false; // Another actor of same type is already here
                }
            }
        }
        
        // Default Greenfoot behavior: allow dropping anywhere within bounds
        // if no collision with same-type actors
        return true;
    }
    
    /**
     * Enhanced drop validation that can be customized for specific game logic.
     * This method provides hooks for implementing parent-child relationships
     * and specific drop zone validation.
     */
    public boolean isValidDropLocation(Actor actor, int cellX, int cellY) {
        // First check basic validation
        if (!canDropAt(actor, cellX, cellY)) {
            return false;
        }
        
        // For a generic implementation, we simply allow all drops that pass basic validation
        // Game-specific logic should be implemented by overriding this method or 
        // using the Actor's own validation methods
        
        return true; // Default: allow drop if basic validation passes
    }
    
    /**
     * Add an actor at pixel coordinates.
     */
    public boolean addActorAtPixel(Actor actor, int xPixel, int yPixel) {
        if (world == null) return false;
        
        int x = xPixel / world.getCellSize();
        int y = yPixel / world.getCellSize();
        
        if (x >= 0 && x < world.getWidth() && y >= 0 && y < world.getHeight()) {
            world.addObject(actor, x, y);
            return true;
        }
        
        return false;
    }
    
    // === Keyboard Management ===
    
    /**
     * Update keyboard state - should be called each frame.
     */
    public void updateKeyboardState() {
        // Update just-pressed and just-released states
        for (int i = 0; i < keyPressed.length; i++) {
            boolean currentPressed = Gdx.input.isKeyPressed(i);
            keyJustPressed[i] = currentPressed && !keyPressed[i];
            keyJustReleased[i] = !currentPressed && keyPressed[i];
            keyPressed[i] = currentPressed;
        }
    }
    
    /**
     * Check if a key is currently pressed.
     */
    public boolean isKeyPressed(int keyCode) {
        if (keyCode >= 0 && keyCode < keyPressed.length) {
            return keyPressed[keyCode];
        }
        return false;
    }
    
    /**
     * Check if a key was just pressed this frame.
     */
    public boolean isKeyJustPressed(int keyCode) {
        if (keyCode >= 0 && keyCode < keyJustPressed.length) {
            return keyJustPressed[keyCode];
        }
        return false;
    }
    
    /**
     * Check if a key was just released this frame.
     */
    public boolean isKeyJustReleased(int keyCode) {
        if (keyCode >= 0 && keyCode < keyJustReleased.length) {
            return keyJustReleased[keyCode];
        }
        return false;
    }
    
    /**
     * Ask user for input (simplified implementation for LibGDX).
     */
    public String ask(String prompt) {
        // For LibGDX, we'll delegate to Greenfoot.ask() which has the actual implementation
        return Greenfoot.ask(prompt);
    }
    
    /**
     * Request a repaint of the world.
     */
    public void repaint() {
        // In LibGDX, repainting happens automatically via the render loop
        // This method exists for compatibility
    }
    
    /**
     * Request a repaint and wait.
     */
    public void repaintAndWait() {
        repaint(); // Same as repaint() in LibGDX
    }
    
    /**
     * Handle focus changes.
     */
    public void worldFocusChanged(boolean focused) {
        // LibGDX handles focus automatically, but we can log this for debugging
        Gdx.app.log("WorldHandler", "World focus changed: " + focused);
    }
    
    /**
     * Notify that world construction has completed.
     */
    public void finishedInitialisingWorld() {
        Gdx.app.log("WorldHandler", "Finished initializing world");
    }
    
    /**
     * Called when simulation is stopped with an error.
     */
    public void notifyStoppedWithError() {
        Gdx.app.error("WorldHandler", "Simulation stopped with error");
    }
    
    /**
     * Called when an object is added to the world.
     */
    public void objectAddedToWorld(Actor actor) {
        // Hook for when actors are added to worlds
        // Can be used for tracking or debugging
    }
    
    /**
     * Called when an object is removed from the world.
     */
    public void objectRemovedFromWorld(Actor actor) {
        // Hook for when actors are removed from worlds
        // Can be used for tracking or debugging
    }
}
