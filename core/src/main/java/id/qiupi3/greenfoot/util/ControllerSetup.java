package id.qiupi3.greenfoot.util;

import id.qiupi3.greenfoot.AndroidControllerConfig;
import id.qiupi3.greenfoot.AndroidControllerConfig.ButtonType;
import id.qiupi3.greenfoot.AndroidControllerConfig.GamepadLayout;
import id.qiupi3.greenfoot.AndroidControllerConfig.ControllerConfiguration;
import id.qiupi3.greenfoot.GreenfootGame;
import com.badlogic.gdx.Gdx;

/**
 * Utility class for easy controller configuration setup.
 * Provides helper methods to quickly configure controllers for common game types.
 */
public class ControllerSetup {
    
    /**
     * Create a basic 2-button configuration (movement + action)
     * Perfect for simple games with minimal controls
     */
    public static ControllerConfiguration createBasicConfig() {
        ControllerConfiguration config = new ControllerConfiguration();
        config.layout = GamepadLayout.TWO_BUTTON;
        
        // Keep only essential mappings
        config.buttonMappings.clear();
        config.buttonMappings.put(ButtonType.UP, "up");
        config.buttonMappings.put(ButtonType.DOWN, "down");
        config.buttonMappings.put(ButtonType.LEFT, "left");
        config.buttonMappings.put(ButtonType.RIGHT, "right");
        config.buttonMappings.put(ButtonType.ACTION, "space");
        config.buttonMappings.put(ButtonType.ENTER, "enter");
        
        return config;
    }
    
    /**
     * Create a platformer configuration (movement + jump + attack)
     * Perfect for platform games
     */
    public static ControllerConfiguration createPlatformerConfig() {
        ControllerConfiguration config = new ControllerConfiguration();
        config.layout = GamepadLayout.FOUR_BUTTON;
        
        config.buttonMappings.clear();
        config.buttonMappings.put(ButtonType.UP, "up");
        config.buttonMappings.put(ButtonType.DOWN, "down");
        config.buttonMappings.put(ButtonType.LEFT, "left");
        config.buttonMappings.put(ButtonType.RIGHT, "right");
        config.buttonMappings.put(ButtonType.JUMP, "space");
        config.buttonMappings.put(ButtonType.ATTACK, "x");
        config.buttonMappings.put(ButtonType.ACTION, "space");
        config.buttonMappings.put(ButtonType.SPECIAL, "z");
        config.buttonMappings.put(ButtonType.ENTER, "enter");
        config.buttonMappings.put(ButtonType.ESCAPE, "escape");
        
        return config;
    }
    
    /**
     * Create an RPG/Adventure configuration (movement + multiple actions + menu)
     * Perfect for RPGs, adventure games, or complex games
     */
    public static ControllerConfiguration createRPGConfig() {
        ControllerConfiguration config = new ControllerConfiguration();
        config.layout = GamepadLayout.SIX_BUTTON;
        
        config.buttonMappings.clear();
        // Movement
        config.buttonMappings.put(ButtonType.UP, "up");
        config.buttonMappings.put(ButtonType.DOWN, "down");
        config.buttonMappings.put(ButtonType.LEFT, "left");
        config.buttonMappings.put(ButtonType.RIGHT, "right");
        
        // Actions
        config.buttonMappings.put(ButtonType.ACTION, "space");
        config.buttonMappings.put(ButtonType.ATTACK, "x");
        config.buttonMappings.put(ButtonType.SPECIAL, "z");
        config.buttonMappings.put(ButtonType.JUMP, "w");
        
        // Menu and system
        config.buttonMappings.put(ButtonType.ENTER, "enter");
        config.buttonMappings.put(ButtonType.ESCAPE, "escape");
        config.buttonMappings.put(ButtonType.START, "enter");
        config.buttonMappings.put(ButtonType.SELECT, "escape");
        
        // Gamepad style
        config.buttonMappings.put(ButtonType.BUTTON_A, "space");
        config.buttonMappings.put(ButtonType.BUTTON_B, "escape");
        config.buttonMappings.put(ButtonType.BUTTON_X, "x");
        config.buttonMappings.put(ButtonType.BUTTON_Y, "z");
        
        return config;
    }
    
    /**
     * Create a fighting game configuration (movement + 6 attack buttons)
     * Perfect for fighting games or action games with many moves
     */
    public static ControllerConfiguration createFightingConfig() {
        ControllerConfiguration config = new ControllerConfiguration();
        config.layout = GamepadLayout.EIGHT_BUTTON;
        
        config.buttonMappings.clear();
        // Movement
        config.buttonMappings.put(ButtonType.UP, "up");
        config.buttonMappings.put(ButtonType.DOWN, "down");
        config.buttonMappings.put(ButtonType.LEFT, "left");
        config.buttonMappings.put(ButtonType.RIGHT, "right");
        
        // Attack buttons
        config.buttonMappings.put(ButtonType.BUTTON_A, "z"); // Light punch
        config.buttonMappings.put(ButtonType.BUTTON_B, "x"); // Heavy punch
        config.buttonMappings.put(ButtonType.BUTTON_X, "c"); // Light kick
        config.buttonMappings.put(ButtonType.BUTTON_Y, "v"); // Heavy kick
        
        // Shoulder buttons for special moves
        config.buttonMappings.put(ButtonType.LEFT_SHOULDER, "q");
        config.buttonMappings.put(ButtonType.RIGHT_SHOULDER, "e");
        
        // System
        config.buttonMappings.put(ButtonType.START, "enter");
        config.buttonMappings.put(ButtonType.SELECT, "escape");
        
        return config;
    }
    
    /**
     * Create a full controller configuration with all buttons
     * For complex games that need maximum input options
     */
    public static ControllerConfiguration createFullConfig() {
        ControllerConfiguration config = new ControllerConfiguration();
        config.layout = GamepadLayout.TEN_BUTTON;
        
        // Use all default mappings - this gives access to everything
        return config; // Default constructor sets up all mappings
    }
    
    /**
     * Apply a configuration and log the result
     */
    public static void applyConfiguration(ControllerConfiguration config) {
        AndroidControllerConfig.setConfig(config);
        
        if (Gdx.app != null) {
            Gdx.app.log("ControllerSetup", "Applied " + config.layout.getName() + " configuration");
            Gdx.app.debug("ControllerSetup", AndroidControllerConfig.getDebugInfo());
        }
        
        // If we're on Android, update the controller layout
        if (GreenfootGame.isAndroidPlatform()) {
            // This would be implemented in the Android launcher
            Gdx.app.debug("ControllerSetup", "Android platform detected - controller layout will be updated");
        }
    }
    
    /**
     * Quick setup method for common game types
     */
    public static void quickSetup(String gameType) {
        ControllerConfiguration config;
        
        switch (gameType.toLowerCase()) {
            case "basic":
            case "simple":
                config = createBasicConfig();
                break;
                
            case "platformer":
            case "platform":
                config = createPlatformerConfig();
                break;
                
            case "rpg":
            case "adventure":
                config = createRPGConfig();
                break;
                
            case "fighting":
            case "action":
                config = createFightingConfig();
                break;
                
            case "full":
            case "complete":
                config = createFullConfig();
                break;
                
            default:
                config = createRPGConfig(); // Default to RPG config
                if (Gdx.app != null) {
                    Gdx.app.log("ControllerSetup", "Unknown game type '" + gameType + "', using RPG configuration");
                }
                break;
        }
        
        applyConfiguration(config);
    }
    
    /**
     * Get a user-friendly description of available configurations
     */
    public static String getAvailableConfigurations() {
        StringBuilder sb = new StringBuilder();
        sb.append("Available Controller Configurations:\n\n");
        
        sb.append("\"basic\" - 2-Button Layout\n");
        sb.append("  Perfect for simple games\n");
        sb.append("  Controls: D-pad + Action\n\n");
        
        sb.append("\"platformer\" - 4-Button Layout\n");
        sb.append("  Perfect for platform games\n");
        sb.append("  Controls: D-pad + Jump + Attack + Special\n\n");
        
        sb.append("\"rpg\" - 6-Button Layout (Default)\n");
        sb.append("  Perfect for RPGs and adventure games\n");
        sb.append("  Controls: D-pad + 4 Action buttons + Menu\n\n");
        
        sb.append("\"fighting\" - 8-Button Layout\n");
        sb.append("  Perfect for fighting games\n");
        sb.append("  Controls: D-pad + 4 Attack + 2 Shoulder buttons\n\n");
        
        sb.append("\"full\" - 10-Button Layout\n");
        sb.append("  Maximum control options\n");
        sb.append("  Controls: All available buttons\n\n");
        
        sb.append("Usage: ControllerSetup.quickSetup(\"rpg\");\n");
        
        return sb.toString();
    }
}