package id.qiupi3.greenfoot;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.ReflectionException;

import greenfoot.World;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class GreenfootGame extends Game {
    private static GreenfootGame instance;

    /**
     * Get the singleton instance of GreenfootGame
     */
    public static GreenfootGame getInstance() {
        return instance;
    }

    @Override
    public void create() {
        instance = this; // Store static reference for controller access
        
        // Try to find the main World subclass
        Class<?> mainWorldClass = findMainWorld();

        if (mainWorldClass == null) {
            Gdx.app.error("GreenfootWrapper", "No World subclass found!");
            return;
        }

        try {
            World world = (World) ClassReflection.newInstance(mainWorldClass);
            // CRITICAL: Register the world with the Greenfoot wrapper system first
            greenfoot.Greenfoot.setWorld(world);
            setScreen(world); // World extends Screen in your wrapper
        } catch (ReflectionException e) {
            Gdx.app.error("GreenfootWrapper", "Failed to create World: " + e.getMessage());
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
     * Check if running on Android platform
     */
    public static boolean isAndroidPlatform() {
        return instance != null && instance instanceof AndroidControllerInterface && 
               ((AndroidControllerInterface) instance).isAndroid();
    }
}
