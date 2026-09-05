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

import java.util.List;
import java.util.Random;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.StringBuilder;
import id.qiupi3.greenfoot.GreenfootGame;

/**
 * LibGDX-based utility class that provides methods to control the simulation
 * and interact with the system using LibGDX's input and audio systems.
 * 
 * This class re-implements greenfoot.Greenfoot to provide a LibGDX backend,
 * mainly to allow Greenfoot projects to run on LibGDX (especially to export into
 * mobile devices and other platforms).
 * 
 * Inspired by the original Greenfoot project (GPLv2+ with Classpath Exception).
 * Read the original documentation at
 * https://www.greenfoot.org/files/javadoc/greenfoot/Greenfoot.html
 * 
 * @author Davin McCall (Original Greenfoot version's author)
 * 
 * @modified-by Qiupi3 (LibGDX wrapper implementation)
 * @version 1.0
 */
public class Greenfoot {
    private static Random randomGenerator = new Random();
    private static String lastKey = null;
    private static int speed = 50;
    private static boolean isPaused = false;
    
    // Frame timing control for drag & drop performance
    private static int targetFPS = 30; // Default 30 FPS for better drag interaction
    private static boolean dragSlowMode = true; // Slow down during drag operations
    
    // Frame-based mouse button tracking
    private static boolean wasLeftButtonPressed = false;
    private static boolean wasRightButtonPressed = false;
    private static boolean wasMiddleButtonPressed = false;
    private static boolean wasAnyButtonPressed = false;

    // Text input management for ask() method
    private static boolean isWaitingForInput = false;
    private static String inputPrompt = "";
    private static StringBuilder currentInput = new StringBuilder();
    private static String inputResult = null;

    /**
     * Sets the World to run to the one given.
     * This World will now be the main World that Greenfoot runs with on the
     * next act.
     *
     * @param world The World to switch running to, cannot be null.
     */
    public static void setWorld(World world) {
        if ( world == null ) {
            throw new NullPointerException("The given world cannot be null.");
        }

        WorldHandler.getInstance().setWorld(world);
    }

    /**
     * Get the most recently pressed key, since the last time this method was
     * called. If no key was pressed since this method was last called, it
     * will return null. If more than one key was pressed, this returns only
     * the most recently pressed key.
     * 
     * @return  The name of the most recently pressed key
     */
    public static String getKey() {
        // Handle key polling using LibGDX input
        String key = pollKeyInput();
        if (key != null) {
            String result = lastKey;
            lastKey = null;
            return result != null ? result : key;
        }
        return null;
    }
    
    /**
     * Check whether a given key is currently pressed down.
     * 
     * @param keyName  The name of the key to check
     * @return         True if the key is down
     */
    public static boolean isKeyDown(String keyName) {
        return isLibGDXKeyPressed(keyName);
    }
    
    /**
     * Delay the current execution by a number of time steps. 
     * The size of one time step is defined by the Greenfoot environment (the speed slider).
     * 
     * @param time  The number of steps the delay will last.
     * @see #setSpeed(int)
     */
    public static void delay(int time) {
        // In LibGDX, we simulate delay by sleeping the current thread
        try {
            Thread.sleep(time * (100 - speed)); // Faster speed = less delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Set the speed of the execution.
     *  
     * @param speed  The new speed. the value must be in the range (1..100)
     */
    public static void setSpeed(int speed) {
        if (speed < 1 || speed > 100) {
            throw new IllegalArgumentException("Speed must be between 1 and 100");
        }
        Greenfoot.speed = speed;
    }
    
    /**
     * Pause the execution.
     */
    public static void stop() {
        isPaused = true;
        Gdx.app.log("Greenfoot", "Simulation paused");
    }
    
    /**
     * Run (or resume) the execution.
     */
    public static void start() {
        isPaused = false;
        Gdx.app.log("Greenfoot", "Simulation resumed");
    }
    
    /**
     * Get current target FPS setting.
     * 
     * @return Current target frames per second
     */
    public static int getTargetFPS() {
        return targetFPS;
    }
    
    /**
     * Set target FPS for the application.
     * Lower FPS provides better timing for drag & drop operations.
     * 
     * @param fps Target frames per second (recommended: 30 for drag operations, 60 for normal gameplay)
     */
    public static void setTargetFPS(int fps) {
        targetFPS = Math.max(10, Math.min(fps, 120)); // Clamp between 10-120 FPS
    }
    
    /**
     * Check if drag slow mode is enabled.
     * 
     * @return true if drag operations use slower timing for better precision
     */
    public static boolean isDragSlowMode() {
        return dragSlowMode;
    }
    
    /**
     * Enable or disable drag slow mode.
     * When enabled, frame rate is reduced during drag operations for better precision.
     * 
     * @param enabled true to enable drag slow mode
     */
    public static void setDragSlowMode(boolean enabled) {
        dragSlowMode = enabled;
    }
    
    /**
     * Return a random number between 0 (inclusive) and limit (exclusive).
     *
     * @param limit  An upper limit which the returned random number will be smaller than.
     * @return A random number within 0 to (limit-1) range.
     */
    public static int getRandomNumber(int limit) {
        return randomGenerator.nextInt(limit);
    }

    // ================ Virtual Controller (touch platforms) ================

    /**
     * Show or hide the on-screen virtual controller. This is an addition to the
     * original Greenfoot API, for touch platforms such as Android. On platforms
     * without an on-screen controller (desktop, HTML) the call does nothing.
     *
     * @param visible true to show the controller, false to hide it
     */
    public static void setVirtualControllerVisible(boolean visible) {
        GreenfootGame.setVirtualControllerVisible(visible);
    }

    /**
     * Hide every on-screen button except the named one. Useful for menus or
     * dialogs where only a confirm button should stay reachable.
     *
     * @param buttonId Button id such as "btn_enter", "btn_action", "btn_up".
     *                 The "btn_" prefix is optional.
     */
    public static void hideAllButtonsExcept(String buttonId) {
        GreenfootGame.setAllVirtualButtonsVisible(false);
        GreenfootGame.setVirtualControllerVisible(true);
        GreenfootGame.setVirtualButtonVisible(buttonId, true);
    }

    /**
     * Show every on-screen button again, undoing {@link #hideAllButtonsExcept(String)}.
     */
    public static void showAllButtons() {
        GreenfootGame.setAllVirtualButtonsVisible(true);
        GreenfootGame.setVirtualControllerVisible(true);
    }

    /**
     * Show or hide a single on-screen button.
     *
     * @param buttonId Button id such as "btn_enter" ("btn_" prefix optional)
     * @param visible true to show that button
     */
    public static void setButtonVisible(String buttonId, boolean visible) {
        GreenfootGame.setVirtualButtonVisible(buttonId, visible);
    }

    /**
     * Choose the on-screen pad layout. This is an addition to the original Greenfoot
     * API, for touch platforms.
     *
     * @param layoutId "4-4" for four buttons each side, "4-2" for four on the left
     *                 and two on the right
     */
    public static void setVirtualControllerLayout(String layoutId) {
        GreenfootGame.setVirtualControllerLayout(layoutId);
    }

    /**
     * Bind a key to one on-screen button. The button then reports that key through
     * {@link #isKeyDown(String)} and labels itself with it.
     *
     * @param buttonId slot id: "left_up", "left_down", "left_left", "left_right",
     *                 "right_1".."right_4", or "special" for a tap anywhere on screen
     * @param keyName the Greenfoot key name to send, such as "w" or "space";
     *                null clears the binding and hides the button
     */
    public static void mapVirtualButton(String buttonId, String keyName) {
        GreenfootGame.mapVirtualButton(buttonId, keyName);
    }

    /**
     * Hide the on-screen pad automatically whenever one of these classes is on screen,
     * instead of calling {@link #setVirtualControllerVisible(boolean)} by hand.
     *
     * A World subclass hides the pad while that world is showing; an Actor subclass
     * hides it while at least one actor of that class is in the world - which is the
     * easy way to drop the pad during a dialogue, a menu overlay or a modal panel.
     *
     * <pre>
     * Greenfoot.hideVirtualControllerFor(MainMenu.class, Credit.class, Dialog.class);
     * </pre>
     *
     * @param classes the World and Actor classes that hide the pad; pass none to clear
     */
    public static void hideVirtualControllerFor(Class<?>... classes) {
        GreenfootGame.setVirtualControllerHiddenFor(classes);
    }

    /**
     * Play sound from the given file. The file format must be one of: aiff, au,
     * wav, mp3. The file should be located in the sounds directory in the project.
     * This method returns once the sound has started playing - it does not wait
     * for the sound to complete. If the sound file cannot be found or loaded, no 
     * exception is thrown - the method simply returns without playing a sound.
     *
     * @param soundFile Name of the sound file to play
     */
    public static void playSound(String soundFile) {
        try {
            Sound sound = Gdx.audio.newSound(Gdx.files.internal("sounds/" + soundFile));
            sound.play();
        } catch (Exception e) {
            // Fail silently as per Greenfoot behavior
            Gdx.app.log("Greenfoot", "Could not play sound: " + soundFile);
        }
    }

    /**
     * True if a mouse button has been pressed (clicked) on the given object.
     * 
     * @param obj  The object to check for mouse press. If obj is the world, 
     *             it checks whether the background has been clicked. If obj 
     *             is null, this method checks for a mouse click anywhere.
     * @return     True if the mouse has been pressed on the given object, 
     *             false otherwise.
     */
    public static boolean mousePressed(Object obj) {
        // Check if any mouse button was just pressed this frame
        boolean justPressed = isAnyButtonJustPressed();
        
        if (!justPressed) {
            return false;
        }
        
        boolean result = isMouseOnObject(obj);
        
        return result;
    }

    /**
     * True if a mouse button has been clicked (pressed and released) on the given object.
     * 
     * @param obj  The object to check for mouse click. If obj is the world, 
     *             it checks whether the background has been clicked. If obj 
     *             is null, this method checks for a mouse click anywhere.
     * @return     True if the mouse has been clicked on the given object, 
     *             false otherwise.
     */
    public static boolean mouseClicked(Object obj) {
        // Check if there was a click event
        if (!Gdx.input.justTouched()) {
            return false;
        }
        
        return isMouseOnObject(obj);
    }

    /**
     * True if the mouse has been dragged on the given object. Dragging is when
     * the mouse is moved while the button is held down.
     * 
     * @param obj  The object to check for mouse dragging. If obj is the world, 
     *             it checks whether the mouse is being dragged on the background. 
     *             If obj is null, this method checks for mouse dragging anywhere.
     * @return     True if the mouse has been dragged on the given object, 
     *             false otherwise.
     */
    public static boolean mouseDragged(Object obj) {
        // Check if any mouse button is pressed while moving
        boolean anyButtonPressed = Gdx.input.isButtonPressed(Input.Buttons.LEFT) ||
                                  Gdx.input.isButtonPressed(Input.Buttons.RIGHT) ||
                                  Gdx.input.isButtonPressed(Input.Buttons.MIDDLE);
        
        boolean isMoving = Gdx.input.getDeltaX() != 0 || Gdx.input.getDeltaY() != 0;
        
        if (!anyButtonPressed || !isMoving) {
            return false;
        }
        
        return isMouseOnObject(obj);
    }

    /**
     * True if a mouse drag has ended. This happens when the mouse button is 
     * released after a dragging.
     * 
     * @param obj  The object to check for end of mouse dragging. If obj is the 
     *             world, it checks whether a mouse drag on the background ended. 
     *             If obj is null, this method checks for mouse drag ending anywhere.
     * @return     True if a mouse drag has ended on the given object, false otherwise.
     */
    public static boolean mouseDragEnded(Object obj) {
        // Check if any mouse button was just released this frame
        if (!isAnyButtonJustReleased()) {
            return false;
        }
        
        return isMouseOnObject(obj);
    }

    /**
     * True if the mouse has been moved on the given object.
     * 
     * @param obj  The object to check for mouse move. If obj is the world, 
     *             it checks whether the mouse has been moved over the background. 
     *             If obj is null, this method checks for mouse moves anywhere.
     * @return     True if the mouse has been moved on the given object, 
     *             false otherwise.
     */
    public static boolean mouseMoved(Object obj) {
        // Check for mouse movement
        boolean isMoving = Gdx.input.getDeltaX() != 0 || Gdx.input.getDeltaY() != 0;
        
        if (!isMoving) {
            return false;
        }
        
        return isMouseOnObject(obj);
    }

    // Mouse state caching for performance optimization
    private static MouseInfo cachedMouseInfo = null;
    private static long lastMouseInfoUpdate = 0;
    private static final long MOUSE_INFO_CACHE_TIME = 16666666L; // ~60 FPS (16.67ms in nanoseconds)
    
    /**
     * Return a mouse info object with information about the state of the mouse.
     * Performance optimized: caches results and reduces coordinate calculation frequency during drag.
     * 
     * @return A mouse info object if the mouse has been clicked, pressed, moved, etc.
     *         null, otherwise.
     */
    public static MouseInfo getMouseInfo() {
        // Check for any mouse button activity or movement
        boolean anyButtonPressed = Gdx.input.isButtonPressed(Input.Buttons.LEFT) ||
                                  Gdx.input.isButtonPressed(Input.Buttons.RIGHT) ||
                                  Gdx.input.isButtonPressed(Input.Buttons.MIDDLE);
        
        // Presses that belong to the on-screen pad are not mouse activity.
        if (GreenfootGame.isTouchOnVirtualController()) {
            return null;
        }

        boolean hasActivity = Gdx.input.justTouched() || anyButtonPressed || mouseMoved(null);
        
        if (hasActivity || anyButtonPressed) {
            long currentTime = System.nanoTime();
            
            // Use cached result if we're in drag slow mode and recent update exists
            if (dragSlowMode && cachedMouseInfo != null && 
                (currentTime - lastMouseInfoUpdate) < MOUSE_INFO_CACHE_TIME) {
                return cachedMouseInfo;
            }
            
            MouseInfo info = new MouseInfo();
            
            // Get current mouse position and convert to world coordinates
            int screenX = Gdx.input.getX();
            int screenY = Gdx.input.getY();
            
            World currentWorld = WorldHandler.getInstance().getWorld();
            if (currentWorld != null) {
                // Convert screen coordinates to world coordinates using camera unproject
                Vector3 worldCoords = new Vector3(screenX, screenY, 0);
                currentWorld.getCamera().unproject(worldCoords);
                
                // Convert to world cell coordinates using consistent method
                int cellX = (int)(worldCoords.x / currentWorld.getCellSize());
                int cellY = (int)((currentWorld.getHeightInPixels() - worldCoords.y) / currentWorld.getCellSize());
                
                // Calculate pixel coordinates in world space (for drag positioning)
                int worldPixelX = (int)worldCoords.x;
                int worldPixelY = (int)(currentWorld.getHeightInPixels() - worldCoords.y);
                
                // Set coordinates using MouseInfoVisitor (cell coordinates and world pixel coordinates)
                MouseInfoVisitor.setLoc(info, cellX, cellY, worldPixelX, worldPixelY);
                
                // Set button information
                int button = 0;
                if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
                    button = 1;
                } else if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT)) {
                    button = 3;
                } else if (Gdx.input.isButtonPressed(Input.Buttons.MIDDLE)) {
                    button = 2;
                }
                MouseInfoVisitor.setButton(info, button);
                MouseInfoVisitor.setClickCount(info, button != 0 ? 1 : 0);
                
                // Only detect actor when not dragging or on button press for performance
                if (!isDragging() || Gdx.input.justTouched()) {
                    List<Actor> actorsAtPosition = currentWorld.getObjectsAt(cellX, cellY, Actor.class);
                    if (!actorsAtPosition.isEmpty()) {
                        // Get the topmost actor (last in the list, as it was added most recently)
                        Actor topActor = actorsAtPosition.get(actorsAtPosition.size() - 1);
                        MouseInfoVisitor.setActor(info, topActor);
                    } else {
                        MouseInfoVisitor.setActor(info, null);
                    }
                } else {
                    // Don't set actor during drag for performance
                    MouseInfoVisitor.setActor(info, null);
                }
            }
            
            // Cache the result
            cachedMouseInfo = info;
            lastMouseInfoUpdate = currentTime;
            
            return info;
        }
        return null;
    }
    
    /**
     * Check if currently dragging for performance optimization.
     */
    private static boolean isDragging() {
        return WorldHandler.getInstance().isDragging();
    }
    /**
     * Get the current microphone level (volume). This can be used to react to
     * the sound level of the default microphone device.
     * 
     * TODO: LibGDX doesn't have built-in microphone support.
     * Implementing microphone functionality requires platform-specific code.
     * For now, this method returns 0.
     * @return The sound level (0-100), where 0 is no sound at all.
     */
    public static int getMicLevel() {
        return 0;
    }

    /**
     * Get input from the user (and freeze the scenario while we are waiting).
     * The prompt String parameter will be shown to the user (e.g. "How many players?"), and the answer will be returned as a String.
     * If you want to ask for a number, you can use methods like <code>Integer.parseInt</code> to turn
     * the returned String into a number.
     * <p>
     * This method can only be used when a world is in place and the scenario is running.
     * It returns null if that is not the case, or if the scenario is reset while the prompt
     * is being shown.
     *
     * @param prompt The prompt to show to the user.
     * @return The string that the user typed in.
     */
    public static String ask(String prompt) {
        World currentWorld = WorldHandler.getInstance().getWorld();
        if (currentWorld == null) {
            return null;
        }
        
        // Initialize input state
        isWaitingForInput = true;
        inputPrompt = prompt != null ? prompt : "Enter text:";
        currentInput.clear();
        inputResult = null;
        
        Gdx.app.log("Greenfoot", "Ask prompt: " + inputPrompt);
        
        // Pause the simulation while waiting for input
        boolean wasRunning = !isPaused;
        if (wasRunning) {
            stop();
        }
        
        // Main input loop - this will block until user presses Enter
        while (isWaitingForInput && currentWorld == WorldHandler.getInstance().getWorld()) {
            // Process input events
            processTextInput();
            
            // Render the input dialog
            renderInputDialog(currentWorld);
            
            try {
                Thread.sleep(16); // ~60 FPS
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        // Resume simulation if it was running before
        if (wasRunning) {
            start();
        }
        
        return inputResult;
    }

    /**
     * Check if the simulation is currently paused.
     * 
     * @return true if the simulation is paused, false if running
     */
    public static boolean isPaused() {
        return isPaused;
    }
    
    /**
     * Toggle between paused and running state.
     */
    public static void togglePause() {
        if (isPaused) {
            start();
        } else {
            stop();
        }
    }
    
    /**
     * Update mouse state tracking - should be called once per frame.
     * This tracks button state changes for proper just-pressed/just-released detection.
     */
    public static void updateMouseState() {
        boolean currentLeft = Gdx.input.isButtonPressed(Input.Buttons.LEFT);
        boolean currentRight = Gdx.input.isButtonPressed(Input.Buttons.RIGHT);
        boolean currentMiddle = Gdx.input.isButtonPressed(Input.Buttons.MIDDLE);
        boolean currentAny = currentLeft || currentRight || currentMiddle;
        
        wasLeftButtonPressed = currentLeft;
        wasRightButtonPressed = currentRight;
        wasMiddleButtonPressed = currentMiddle;
        wasAnyButtonPressed = currentAny;
    }
    
    /**
     * Check if left mouse button was just pressed this frame.
     */
    public static boolean isLeftButtonJustPressed() {
        boolean currentLeft = Gdx.input.isButtonPressed(Input.Buttons.LEFT);
        return currentLeft && !wasLeftButtonPressed;
    }
    
    /**
     * Check if left mouse button was just released this frame.
     */
    public static boolean isLeftButtonJustReleased() {
        boolean currentLeft = Gdx.input.isButtonPressed(Input.Buttons.LEFT);
        return !currentLeft && wasLeftButtonPressed;
    }
    
    /**
     * Check if any mouse button was just pressed this frame.
     */
    public static boolean isAnyButtonJustPressed() {
        boolean currentAny = Gdx.input.isButtonPressed(Input.Buttons.LEFT) ||
                           Gdx.input.isButtonPressed(Input.Buttons.RIGHT) ||
                           Gdx.input.isButtonPressed(Input.Buttons.MIDDLE);
        boolean result = currentAny && !wasAnyButtonPressed;
        return result;
    }
    
    /**
     * Check if any mouse button was just released this frame.
     */
    public static boolean isAnyButtonJustReleased() {
        boolean currentAny = Gdx.input.isButtonPressed(Input.Buttons.LEFT) ||
                           Gdx.input.isButtonPressed(Input.Buttons.RIGHT) ||
                           Gdx.input.isButtonPressed(Input.Buttons.MIDDLE);
        return !currentAny && wasAnyButtonPressed;
    }
    
    // ================ LibGDX Helper Methods ================
    
    /**
     * Check if the mouse cursor is currently over the given object.
     * 
     * @param obj The object to check (Actor, World, or null for anywhere)
     * @return true if mouse is over the object, false otherwise
     */
    private static boolean isMouseOnObject(Object obj) {        
        // If obj is null, mouse action is valid anywhere
        if (obj == null) {
            return true;
        }
        
        // A touch that landed on an on-screen pad button belongs to the pad, not to the
        // world: without this, every button press also registered as a click wherever
        // the button happens to sit over the world.
        if (GreenfootGame.isTouchOnVirtualController()) {
            return false;
        }

        // Get mouse coordinates in screen space
        int mouseX = Gdx.input.getX();
        int mouseY = Gdx.input.getY();
        
        World currentWorld = WorldHandler.getInstance().getWorld();

        if (obj instanceof Actor) {
            Actor actor = (Actor) obj;

            // An actor that has been removed from its world - possibly by another
            // actor earlier in this very act cycle - is never under the mouse.
            // Checking the world explicitly (rather than catching the exception)
            // keeps a plain Greenfoot.mouseClicked(this) from blowing up with
            // "Actor has been removed from the world." out of getX()/getY().
            World actorWorld = actor.getWorld();
            if (actorWorld == null) {
                return false;
            }

            // Unproject through the active world when there is one; an actor that
            // still belongs to a world can be queried before WorldHandler has been
            // told about it, so fall back to the actor's own world.
            World viewWorld = currentWorld != null ? currentWorld : actorWorld;

            Vector3 actorCoords = new Vector3(mouseX, mouseY, 0);
            viewWorld.getCamera().unproject(actorCoords);
            // Greenfoot pixel space: origin top-left, y growing downwards.
            float pointerX = actorCoords.x;
            float pointerY = viewWorld.getHeightInPixels() - actorCoords.y;

            // The actor's image, in Greenfoot pixel space, is the hit area - the same
            // test World.getObjectsAt() uses, so both agree on what is under the mouse.
            return actor.containsWorldPixel(pointerX, pointerY);
        }

        if (currentWorld == null) {
            // For non-Actor objects when world is null, return false
            return false;
        }

        // Convert screen coordinates to world coordinates using camera
        Vector3 worldCoords = new Vector3(mouseX, mouseY, 0);
        currentWorld.getCamera().unproject(worldCoords);
        float worldX = worldCoords.x;
        float worldY = worldCoords.y;

        float correctedWorldY = currentWorld.getHeightInPixels() - worldY;

        if (obj instanceof World) {
            // Mouse is on world background if it's within world bounds
            World world = (World) obj;
            float worldWidth = world.getWidthInPixels();
            float worldHeight = world.getHeightInPixels();
            
            return worldX >= 0 && worldX <= worldWidth &&
                   correctedWorldY >= 0 && correctedWorldY <= worldHeight;
        }
        
        // Unknown object type, return false
        return false;
    }


    /**
     * Poll for key input using LibGDX input system.
     */
    private static String pollKeyInput() {
        // Letters (A-Z)
        if (Gdx.input.isKeyJustPressed(Input.Keys.A)) {
            lastKey = "a";
            return "a";
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.B)) {
            lastKey = "b";
            return "b";
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.C)) {
            lastKey = "c";
            return "c";
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.D)) {
            lastKey = "d";
            return "d";
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            lastKey = "e";
            return "e";
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F)) {
            lastKey = "f";
            return "f";
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.G)) {
            lastKey = "g";
            return "g";
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.H)) {
            lastKey = "h";
            return "h";
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.I)) {
            lastKey = "i";
            return "i";
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.J)) {
            lastKey = "j";
            return "j";
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.K)) {
            lastKey = "k";
            return "k";
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.L)) {
            lastKey = "l";
            return "l";
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.M)) {
            lastKey = "m";
            return "m";
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.N)) {
            lastKey = "n";
            return "n";
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.O)) {
            lastKey = "o";
            return "o";
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.P)) {
            lastKey = "p";
            return "p";
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) {
            lastKey = "q";
            return "q";
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            lastKey = "r";
            return "r";
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            lastKey = "s";
            return "s";
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.T)) {
            lastKey = "t";
            return "t";
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.U)) {
            lastKey = "u";
            return "u";
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.V)) {
            lastKey = "v";
            return "v";
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.W)) {
            lastKey = "w";
            return "w";
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.X)) {
            lastKey = "x";
            return "x";
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.Y)) {
            lastKey = "y";
            return "y";
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.Z)) {
            lastKey = "z";
            return "z";
        }
        
        // Numbers (0-9)
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_0)) {
            lastKey = "0";
            return "0";
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
            lastKey = "1";
            return "1";
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
            lastKey = "2";
            return "2";
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) {
            lastKey = "3";
            return "3";
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_4)) {
            lastKey = "4";
            return "4";
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_5)) {
            lastKey = "5";
            return "5";
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_6)) {
            lastKey = "6";
            return "6";
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_7)) {
            lastKey = "7";
            return "7";
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_8)) {
            lastKey = "8";
            return "8";
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_9)) {
            lastKey = "9";
            return "9";
        }
        
        // Arrow keys
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            lastKey = "up";
            return "up";
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            lastKey = "down";
            return "down";
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) {
            lastKey = "left";
            return "left";
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) {
            lastKey = "right";
            return "right";
        }
        
        // Common action keys
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            lastKey = "space";
            return "space";
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            lastKey = "enter";
            return "enter";
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            lastKey = "escape";
            return "escape";
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.TAB)) {
            lastKey = "tab";
            return "tab";
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE)) {
            lastKey = "backspace";
            return "backspace";
        }
        
        // Modifier keys
        if (Gdx.input.isKeyJustPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyJustPressed(Input.Keys.SHIFT_RIGHT)) {
            lastKey = "shift";
            return "shift";
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.CONTROL_LEFT) || Gdx.input.isKeyJustPressed(Input.Keys.CONTROL_RIGHT)) {
            lastKey = "control";
            return "control";
        }
        
        return null;
    }
    
    /**
     * Check if a LibGDX key is currently pressed.
     * 
     * VIRTUAL CONTROLLER INTEGRATION:
     * This method automatically checks both physical keyboard input and virtual controller input.
     * No user code modification needed - existing Greenfoot.isKeyDown() calls work seamlessly!
     * 
     * KEY MAPPING (Physical Keyboard → Virtual Controller):
     * - "up"    → Virtual D-pad UP button
     * - "down"  → Virtual D-pad DOWN button  
     * - "left"  → Virtual D-pad LEFT button
     * - "right" → Virtual D-pad RIGHT button
     * - "space" → Virtual ACTION button (right side)
     * - All other keys → Physical keyboard only
     */
    private static boolean isLibGDXKeyPressed(String keyName) {
        String key = keyName.toLowerCase();
        boolean keyPressed = false;
        
        // Check physical keyboard input first
        switch (key) {
            // Letters (A-Z)
            case "a":
                keyPressed = Gdx.input.isKeyPressed(Input.Keys.A);
                break;
            case "b":
                keyPressed = Gdx.input.isKeyPressed(Input.Keys.B);
                break;
            case "c":
                keyPressed = Gdx.input.isKeyPressed(Input.Keys.C);
                break;
            case "d":
                keyPressed = Gdx.input.isKeyPressed(Input.Keys.D);
                break;
            case "e":
                keyPressed = Gdx.input.isKeyPressed(Input.Keys.E);
                break;
            case "f":
                keyPressed = Gdx.input.isKeyPressed(Input.Keys.F);
                break;
            case "g":
                keyPressed = Gdx.input.isKeyPressed(Input.Keys.G);
                break;
            case "h":
                keyPressed = Gdx.input.isKeyPressed(Input.Keys.H);
                break;
            case "i":
                keyPressed = Gdx.input.isKeyPressed(Input.Keys.I);
                break;
            case "j":
                keyPressed = Gdx.input.isKeyPressed(Input.Keys.J);
                break;
            case "k":
                keyPressed = Gdx.input.isKeyPressed(Input.Keys.K);
                break;
            case "l":
                keyPressed = Gdx.input.isKeyPressed(Input.Keys.L);
                break;
            case "m":
                keyPressed = Gdx.input.isKeyPressed(Input.Keys.M);
                break;
            case "n":
                keyPressed = Gdx.input.isKeyPressed(Input.Keys.N);
                break;
            case "o":
                keyPressed = Gdx.input.isKeyPressed(Input.Keys.O);
                break;
            case "p":
                keyPressed = Gdx.input.isKeyPressed(Input.Keys.P);
                break;
            case "q":
                keyPressed = Gdx.input.isKeyPressed(Input.Keys.Q);
                break;
            case "r":
                keyPressed = Gdx.input.isKeyPressed(Input.Keys.R);
                break;
            case "s":
                keyPressed = Gdx.input.isKeyPressed(Input.Keys.S);
                break;
            case "t":
                keyPressed = Gdx.input.isKeyPressed(Input.Keys.T);
                break;
            case "u":
                keyPressed = Gdx.input.isKeyPressed(Input.Keys.U);
                break;
            case "v":
                keyPressed = Gdx.input.isKeyPressed(Input.Keys.V);
                break;
            case "w":
                keyPressed = Gdx.input.isKeyPressed(Input.Keys.W);
                break;
            case "x":
                keyPressed = Gdx.input.isKeyPressed(Input.Keys.X);
                break;
            case "y":
                keyPressed = Gdx.input.isKeyPressed(Input.Keys.Y);
                break;
            case "z":
                keyPressed = Gdx.input.isKeyPressed(Input.Keys.Z);
                break;
            
            // Numbers (0-9)
            case "0":
                keyPressed = Gdx.input.isKeyPressed(Input.Keys.NUM_0);
                break;
            case "1":
                keyPressed = Gdx.input.isKeyPressed(Input.Keys.NUM_1);
                break;
            case "2":
                keyPressed = Gdx.input.isKeyPressed(Input.Keys.NUM_2);
                break;
            case "3":
                keyPressed = Gdx.input.isKeyPressed(Input.Keys.NUM_3);
                break;
            case "4":
                keyPressed = Gdx.input.isKeyPressed(Input.Keys.NUM_4);
                break;
            case "5":
                keyPressed = Gdx.input.isKeyPressed(Input.Keys.NUM_5);
                break;
            case "6":
                keyPressed = Gdx.input.isKeyPressed(Input.Keys.NUM_6);
                break;
            case "7":
                keyPressed = Gdx.input.isKeyPressed(Input.Keys.NUM_7);
                break;
            case "8":
                keyPressed = Gdx.input.isKeyPressed(Input.Keys.NUM_8);
                break;
            case "9":
                keyPressed = Gdx.input.isKeyPressed(Input.Keys.NUM_9);
                break;
            
            // Arrow keys
            case "up":
                keyPressed = Gdx.input.isKeyPressed(Input.Keys.UP);
                break;
            case "down":
                keyPressed = Gdx.input.isKeyPressed(Input.Keys.DOWN);
                break;
            case "left":
                keyPressed = Gdx.input.isKeyPressed(Input.Keys.LEFT);
                break;
            case "right":
                keyPressed = Gdx.input.isKeyPressed(Input.Keys.RIGHT);
                break;
            
            // Special keys
            case "space":
                // Only the actual space bar. This used to accept a left mouse button
                // press as well, which on a touch screen means EVERY touch: tapping any
                // pad button made isKeyDown("space") true at the same time, so pressing
                // "w" turned the player and then fired the space action instead of
                // walking. (Only a button bound to "f" escaped it, because "f" has no
                // such alias.)
                keyPressed = Gdx.input.isKeyPressed(Input.Keys.SPACE);
                break;
            case "enter":
                keyPressed = Gdx.input.isKeyPressed(Input.Keys.ENTER);
                break;
            case "tab":
                keyPressed = Gdx.input.isKeyPressed(Input.Keys.TAB);
                break;
            case "escape":
                keyPressed = Gdx.input.isKeyPressed(Input.Keys.ESCAPE);
                break;
            case "backspace":
                keyPressed = Gdx.input.isKeyPressed(Input.Keys.BACKSPACE);
                break;
            
            // Modifier keys
            case "shift":
                keyPressed = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) ||
                           Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);
                break;
            case "control":
                keyPressed = Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) ||
                           Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT);
                break;
            
            default:
                keyPressed = false;
        }
        
        // If physical key is pressed, return true immediately
        if (keyPressed) {
            return true;
        }
        
        // VIRTUAL CONTROLLER INTEGRATION:
        // The on-screen pad binds each button to a Greenfoot key name, so any key the
        // project asks about - not just the directions - can be produced by a button.
        return GreenfootGame.isVirtualKeyDown(key);
    }
    
    // ================ Text Input Helper Methods ================
    
    /**
     * Process text input for the ask() method.
     * Handles character input, backspace, and enter key.
     */
    private static void processTextInput() {
        // Handle backspace
        if (Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE)) {
            if (currentInput.length() > 0) {
                currentInput.deleteCharAt(currentInput.length() - 1);
            }
        }
        
        // Handle enter - finish input
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            inputResult = currentInput.toString();
            isWaitingForInput = false;
            return;
        }
        
        // Handle escape - cancel input
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            inputResult = "";
            isWaitingForInput = false;
            return;
        }
        
        // Handle character input (letters with shift support)
        if (Gdx.input.isKeyJustPressed(Input.Keys.A)) {
            char c = (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)) ? 'A' : 'a';
            currentInput.append(c);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.B)) {
            char c = (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)) ? 'B' : 'b';
            currentInput.append(c);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.C)) {
            char c = (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)) ? 'C' : 'c';
            currentInput.append(c);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.D)) {
            char c = (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)) ? 'D' : 'd';
            currentInput.append(c);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            char c = (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)) ? 'E' : 'e';
            currentInput.append(c);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F)) {
            char c = (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)) ? 'F' : 'f';
            currentInput.append(c);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.G)) {
            char c = (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)) ? 'G' : 'g';
            currentInput.append(c);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.H)) {
            char c = (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)) ? 'H' : 'h';
            currentInput.append(c);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.I)) {
            char c = (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)) ? 'I' : 'i';
            currentInput.append(c);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.J)) {
            char c = (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)) ? 'J' : 'j';
            currentInput.append(c);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.K)) {
            char c = (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)) ? 'K' : 'k';
            currentInput.append(c);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.L)) {
            char c = (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)) ? 'L' : 'l';
            currentInput.append(c);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.M)) {
            char c = (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)) ? 'M' : 'm';
            currentInput.append(c);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.N)) {
            char c = (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)) ? 'N' : 'n';
            currentInput.append(c);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.O)) {
            char c = (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)) ? 'O' : 'o';
            currentInput.append(c);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.P)) {
            char c = (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)) ? 'P' : 'p';
            currentInput.append(c);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) {
            char c = (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)) ? 'Q' : 'q';
            currentInput.append(c);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            char c = (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)) ? 'R' : 'r';
            currentInput.append(c);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            char c = (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)) ? 'S' : 's';
            currentInput.append(c);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.T)) {
            char c = (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)) ? 'T' : 't';
            currentInput.append(c);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.U)) {
            char c = (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)) ? 'U' : 'u';
            currentInput.append(c);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.V)) {
            char c = (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)) ? 'V' : 'v';
            currentInput.append(c);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.W)) {
            char c = (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)) ? 'W' : 'w';
            currentInput.append(c);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.X)) {
            char c = (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)) ? 'X' : 'x';
            currentInput.append(c);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.Y)) {
            char c = (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)) ? 'Y' : 'y';
            currentInput.append(c);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.Z)) {
            char c = (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)) ? 'Z' : 'z';
            currentInput.append(c);
        }
        
        // Handle numbers
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_0)) {
            currentInput.append('0');
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
            currentInput.append('1');
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
            currentInput.append('2');
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) {
            currentInput.append('3');
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_4)) {
            currentInput.append('4');
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_5)) {
            currentInput.append('5');
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_6)) {
            currentInput.append('6');
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_7)) {
            currentInput.append('7');
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_8)) {
            currentInput.append('8');
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_9)) {
            currentInput.append('9');
        }
        
        // Handle space
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            currentInput.append(' ');
        }
        
        // Handle common punctuation
        if (Gdx.input.isKeyJustPressed(Input.Keys.PERIOD)) {
            currentInput.append('.');
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.COMMA)) {
            currentInput.append(',');
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.MINUS)) {
            currentInput.append('-');
        }
    }
    
    /**
     * Render the input dialog overlay using the current world's rendering system.
     * Creates a semi-transparent background with the prompt and current input text.
     */
    private static void renderInputDialog(World world) {
        if (world == null) return;
        
        try {
            // Use Greenfoot's showText functionality for simple text display
            int centerX = world.getWidth() / 2;
            int centerY = world.getHeight() / 2;
            
            // Display prompt text
            world.showText(inputPrompt, centerX, centerY - 2);
            
            // Display current input with blinking cursor
            String displayText = currentInput.toString();
            if (System.currentTimeMillis() % 1000 < 500) { // Blinking cursor
                displayText += "|"; 
            }
            world.showText("Input: " + displayText, centerX, centerY);
            
            // Display instructions
            world.showText("Press ENTER to confirm, ESC to cancel", centerX, centerY + 2);
            
        } catch (Exception e) {
            // If rendering fails, fall back to console logging only
            Gdx.app.log("Greenfoot", "Prompt: " + inputPrompt + " | Input: " + currentInput.toString());
        }
    }

    public static boolean wasRightButtonPressed() {
        return wasRightButtonPressed;
    }

    public static boolean wasMiddleButtonPressed() {
        return wasMiddleButtonPressed;
    }
}
