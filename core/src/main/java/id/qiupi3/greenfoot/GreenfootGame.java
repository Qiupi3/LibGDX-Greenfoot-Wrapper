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

package id.qiupi3.greenfoot;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.ReflectionException;

import greenfoot.World;

/**
 * {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms.
 * Manages game lifecycle, screen transitions, and virtual controller initialization.
 * 
 * @author Qiupi3
 * @version 1.0
 */
public class GreenfootGame extends Game {
    private static GreenfootGame instance;

    /** The on-screen pad, or null when this platform does not show one. */
    private VirtualController virtualController;

    /**
     * Get the singleton instance of GreenfootGame
     */
    public static GreenfootGame getInstance() {
        return instance;
    }

    @Override
    public void create() {
        instance = this; // Store static reference for controller access

        // The pad is for touch screens, but -Dgreenfoot.virtualController=true turns it
        // on anywhere so it can be exercised on the desktop without a device.
        if (isAndroidPlatform() || Boolean.getBoolean("greenfoot.virtualController")) {
            virtualController = new VirtualController();
            // A project configures the pad by shipping controller.properties; without
            // one, the built-in 4-2 defaults apply.
            virtualController.loadConfigFromAssets();
        }
        
        // Try to find the main World subclass
        Class<?> mainWorldClass = findMainWorld();

        if (mainWorldClass == null) {
            Gdx.app.error("GreenfootWrapper", "No World subclass found!");
            return;
        }

        try {
            World world = (World) ClassReflection.newInstance(mainWorldClass);
            // Registering the world sets the LibGDX screen too (through WorldHandler,
            // which wraps the world in a WorldScreen), so there is nothing else to do.
            greenfoot.Greenfoot.setWorld(world);
        } catch (ReflectionException e) {
            Gdx.app.error("GreenfootWrapper", "Failed to create World: " + e.getMessage());
        }
    }

    @Override
    public void render() {
        // Read the pad before the world acts, so a button held this frame is already
        // visible to isKeyDown() during act().
        if (virtualController != null) {
            virtualController.update();
        }

        super.render();

        // Drawn last so it sits above the world.
        if (virtualController != null) {
            virtualController.render();
        }
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        if (virtualController != null) {
            virtualController.resize();
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        if (virtualController != null) {
            virtualController.dispose();
            virtualController = null;
        }
    }

    /**
     * The on-screen pad for this run.
     *
     * @return the controller, or null on a platform that shows none
     */
    public static VirtualController getVirtualController() {
        return instance != null ? instance.virtualController : null;
    }

    /**
     * Whether a key is currently held on the on-screen pad.
     *
     * @param keyName a Greenfoot key name such as "w" or "space"
     * @return true if the pad is showing and a button bound to that key is held
     */
    public static boolean isVirtualKeyDown(String keyName) {
        VirtualController controller = getVirtualController();
        return controller != null && controller.isKeyDown(keyName);
    }

    /**
     * Whether the touch that is happening belongs to the pad. Greenfoot's mouse
     * polling uses this to ignore taps that landed on a button, which is what keeps
     * a button press from also clicking the world underneath it.
     *
     * @return true if any pointer is currently down on a pad button
     */
    public static boolean isTouchOnVirtualController() {
        VirtualController controller = getVirtualController();
        return controller != null && controller.isAnyPointerConsumed();
    }

    /**
     * Choose the pad layout.
     *
     * @param layoutId "4-4" or "4-2"
     */
    public static void setVirtualControllerLayout(String layoutId) {
        VirtualController controller = getVirtualController();
        VirtualController.Layout parsed = VirtualController.Layout.fromId(layoutId);
        if (controller != null && parsed != null) {
            controller.setLayout(parsed);
        }
    }

    /**
     * Bind a key to one pad slot. The button relabels itself to the key.
     *
     * @param slotId a slot id such as "left_up", "right_1" or "special"
     * @param keyName the Greenfoot key name to send, or null to clear the slot
     */
    public static void mapVirtualButton(String slotId, String keyName) {
        VirtualController controller = getVirtualController();
        if (controller != null) {
            controller.bind(VirtualController.Slot.fromId(slotId), keyName);
        }
    }

    /**
     * Hide the pad automatically whenever one of these World or Actor classes is on
     * screen.
     *
     * @param classes World subclasses (hide while that world is showing) and Actor
     *                subclasses (hide while such an actor is in the world)
     */
    public static void setVirtualControllerHiddenFor(Class<?>... classes) {
        VirtualController controller = getVirtualController();
        if (controller != null) {
            controller.setHideFor(classes);
        }
    }

    private Class<?> findMainWorld() {
        try {
            // Use dynamic project folder detection
            String projectFolder = GreenfootProjectConfig.getUserProjectFolder();
            String projectFilePath = projectFolder + "/project.greenfoot";
            
            com.badlogic.gdx.files.FileHandle projectFile = Gdx.files.internal(projectFilePath);
            System.out.println("Looking for project.greenfoot at: " + projectFile.path());
            System.out.println("Project folder detected as: " + projectFolder);
            
            if (projectFile.exists()) {
                String content = projectFile.readString();
                String[] lines = content.split("\n");
                
                for (String line : lines) {
                    if (line.startsWith("world.lastInstantiated=")) {
                        System.out.println("Found entry world line: " + line);
                        String entryWorld = line.split("=")[1].trim();
                        System.out.println("Trying to load class: " + entryWorld);
                        
                        // Since the classes are in the default package, try loading directly
                        try {
                            return Class.forName(entryWorld);
                        } catch (ClassNotFoundException e1) {
                            System.out.println("Class not found in default package: " + e1.getMessage());
                            // If direct loading fails, the class might not be compiled or accessible
                            // This could happen if the class files are not in the classpath
                        }
                    }
                }
            } else {
                System.out.println("project.greenfoot file not found at: " + projectFilePath);
                // Print debug info to help with troubleshooting
                System.out.println(GreenfootProjectConfig.getDebugInfo());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Static method to check virtual controller input.
     * This allows actors to check controller input regardless of platform.
     * Now supports the flexible button system with enhanced mappings.
     */
    public static boolean isVirtualControllerPressed(String direction) {
        // The pad works in Greenfoot key names now, so a "direction" is just a key.
        VirtualController pad = getVirtualController();
        if (pad != null && pad.isKeyDown(direction)) {
            return true;
        }

        if (instance != null && instance instanceof AndroidControllerInterface) {
            AndroidControllerInterface controller = (AndroidControllerInterface) instance;
            
            // First try the traditional direct mappings for backward compatibility
            switch (direction.toLowerCase()) {
                case "up": return controller.isUpPressed();
                case "down": return controller.isDownPressed();
                case "left": return controller.isLeftPressed();
                case "right": return controller.isRightPressed();
                case "action": return controller.isActionPressed();
                
                // Enhanced support for additional keys
                case "enter": 
                case "start":
                    return controller.isButtonPressed(AndroidControllerConfig.ButtonType.ENTER) ||
                           controller.isButtonPressed(AndroidControllerConfig.ButtonType.START);
                           
                case "space":
                    return controller.isButtonPressed(AndroidControllerConfig.ButtonType.ACTION) ||
                           controller.isButtonPressed(AndroidControllerConfig.ButtonType.SPACE);
                           
                case "escape":
                case "back":
                    return controller.isButtonPressed(AndroidControllerConfig.ButtonType.ESCAPE) ||
                           controller.isButtonPressed(AndroidControllerConfig.ButtonType.BACK);
                           
                // Support for common action buttons
                case "jump":
                case "w":
                    return controller.isButtonPressed(AndroidControllerConfig.ButtonType.JUMP);
                    
                case "attack":
                case "x":
                    return controller.isButtonPressed(AndroidControllerConfig.ButtonType.ATTACK);
                    
                case "special":
                case "z":
                    return controller.isButtonPressed(AndroidControllerConfig.ButtonType.SPECIAL);
                    
                // Gamepad style buttons
                case "a":
                case "button_a":
                    return controller.isButtonPressed(AndroidControllerConfig.ButtonType.BUTTON_A);
                    
                case "b":
                case "button_b":
                    return controller.isButtonPressed(AndroidControllerConfig.ButtonType.BUTTON_B);
                    
                case "y":
                case "button_y":
                    return controller.isButtonPressed(AndroidControllerConfig.ButtonType.BUTTON_Y);
                    
                case "button_x":
                    return controller.isButtonPressed(AndroidControllerConfig.ButtonType.BUTTON_X);
                    
                // Shoulder buttons
                case "q":
                case "left_shoulder":
                    return controller.isButtonPressed(AndroidControllerConfig.ButtonType.LEFT_SHOULDER);
                    
                case "e":
                case "right_shoulder":
                    return controller.isButtonPressed(AndroidControllerConfig.ButtonType.RIGHT_SHOULDER);
            }
        }
        return false;
    }
    
    /**
     * Enhanced method to check controller input by button type
     * This provides direct access to the flexible button system
     */
    public static boolean isVirtualButtonPressed(AndroidControllerConfig.ButtonType buttonType) {
        if (instance != null && instance instanceof AndroidControllerInterface) {
            AndroidControllerInterface controller = (AndroidControllerInterface) instance;
            return controller.isButtonPressed(buttonType);
        }
        return false;
    }
    
    /**
     * Show or hide the whole on-screen virtual controller.
     * Does nothing on platforms without one (e.g. desktop).
     */
    public static void setVirtualControllerVisible(boolean visible) {
        VirtualController pad = getVirtualController();
        if (pad != null) {
            pad.setVisible(visible);
        }
        AndroidControllerInterface controller = getController();
        if (controller != null) {
            controller.setControllerVisible(visible);
        }
    }

    /**
     * Show or hide one on-screen button, identified by id ("btn_enter", "enter", ...).
     */
    public static void setVirtualButtonVisible(String buttonId, boolean visible) {
        VirtualController pad = getVirtualController();
        if (pad != null) {
            pad.setSlotVisible(VirtualController.Slot.fromId(buttonId), visible);
        }
        AndroidControllerInterface controller = getController();
        if (controller != null) {
            controller.setButtonVisible(buttonId, visible);
        }
    }

    /**
     * Show or hide every on-screen button.
     */
    public static void setAllVirtualButtonsVisible(boolean visible) {
        VirtualController pad = getVirtualController();
        if (pad != null) {
            pad.setAllSlotsVisible(visible);
        }
        AndroidControllerInterface controller = getController();
        if (controller != null) {
            controller.setAllButtonsVisible(visible);
        }
    }

    private static AndroidControllerInterface getController() {
        if (instance instanceof AndroidControllerInterface) {
            return (AndroidControllerInterface) instance;
        }
        return null;
    }

    /**
     * Check if running on Android platform
     */
    public static boolean isAndroidPlatform() {
        return instance != null && instance instanceof AndroidControllerInterface && 
               ((AndroidControllerInterface) instance).isAndroid();
    }
}
