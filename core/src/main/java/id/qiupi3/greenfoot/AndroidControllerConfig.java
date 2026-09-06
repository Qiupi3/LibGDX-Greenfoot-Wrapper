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
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration system for Android virtual gamepad layouts and button mappings.
 * Supports multiple gamepad layouts (2, 3, 4, 6, 8, 10 buttons) with customizable
 * button mappings through JSON configuration.
 * 
 * @author Qiupi3
 * @version 1.0
 */
public class AndroidControllerConfig {
    
    // Predefined gamepad layouts
    public enum GamepadLayout {
        TWO_BUTTON(2, "2-Button", "Basic: D-pad + Action"),
        THREE_BUTTON(3, "3-Button", "D-pad + Action + Jump"),
        FOUR_BUTTON(4, "4-Button", "D-pad + Action + Jump + Special"),
        SIX_BUTTON(6, "6-Button", "D-pad + 4 Action Buttons"),
        EIGHT_BUTTON(8, "8-Button", "D-pad + 4 Action + L/R Shoulders"),
        TEN_BUTTON(10, "10-Button", "Full Controller Layout");
        
        private final int buttonCount;
        private final String name;
        private final String description;
        
        GamepadLayout(int buttonCount, String name, String description) {
            this.buttonCount = buttonCount;
            this.name = name;
            this.description = description;
        }
        
        public int getButtonCount() { return buttonCount; }
        public String getName() { return name; }
        public String getDescription() { return description; }
    }
    
    // Button types that can be mapped
    public enum ButtonType {
        // Directional
        UP, DOWN, LEFT, RIGHT,
        
        // Action buttons
        ACTION, JUMP, ATTACK, SPECIAL,
        BUTTON_A, BUTTON_B, BUTTON_X, BUTTON_Y,
        
        // Triggers/Shoulders
        LEFT_SHOULDER, RIGHT_SHOULDER,
        LEFT_TRIGGER, RIGHT_TRIGGER,
        
        // Special buttons
        START, SELECT, MENU, BACK,
        ENTER, SPACE, ESCAPE
    }
    
    // Configuration data class
    public static class ControllerConfiguration {
        public GamepadLayout layout = GamepadLayout.FOUR_BUTTON;
        public Map<ButtonType, String> buttonMappings = new HashMap<>();
        public boolean showOnScreenControls = true;
        public float controllerOpacity = 0.7f;
        public float controllerScale = 1.0f;
        public boolean enableVibration = true;
        
        public ControllerConfiguration() {
            // Set default mappings
            setDefaultMappings();
        }
        
        private void setDefaultMappings() {
            // Default Greenfoot key mappings
            buttonMappings.put(ButtonType.UP, "up");
            buttonMappings.put(ButtonType.DOWN, "down");
            buttonMappings.put(ButtonType.LEFT, "left");
            buttonMappings.put(ButtonType.RIGHT, "right");
            buttonMappings.put(ButtonType.ACTION, "space");
            buttonMappings.put(ButtonType.JUMP, "w");
            buttonMappings.put(ButtonType.ATTACK, "x");
            buttonMappings.put(ButtonType.SPECIAL, "z");
            buttonMappings.put(ButtonType.ENTER, "enter");
            buttonMappings.put(ButtonType.ESCAPE, "escape");
            buttonMappings.put(ButtonType.START, "enter");
            buttonMappings.put(ButtonType.SELECT, "escape");
            buttonMappings.put(ButtonType.BUTTON_A, "space");
            buttonMappings.put(ButtonType.BUTTON_B, "x");
            buttonMappings.put(ButtonType.BUTTON_X, "z");
            buttonMappings.put(ButtonType.BUTTON_Y, "c");
            buttonMappings.put(ButtonType.LEFT_SHOULDER, "q");
            buttonMappings.put(ButtonType.RIGHT_SHOULDER, "e");
        }
    }
    
    private static ControllerConfiguration currentConfig = null;
    private static final String CONFIG_FILE = "controller_config.json";
    private static boolean initialized = false;
    
    /**
     * Get the current controller configuration
     */
    public static ControllerConfiguration getConfig() {
        if (!initialized) {
            loadConfiguration();
        }
        return currentConfig;
    }
    
    /**
     * Set a new controller configuration
     */
    public static void setConfig(ControllerConfiguration config) {
        currentConfig = config;
        initialized = true;
        saveConfiguration();
    }
    
    /**
     * Load configuration from JSON file
     */
    private static void loadConfiguration() {
        initialized = true;
        
        try {
            // Try to load from assets first (user-provided config)
            String configPath = GreenfootProjectConfig.getAssetPath(CONFIG_FILE);
            FileHandle configFile = Gdx.files.internal(configPath);
            
            if (!configFile.exists()) {
                // Fallback to root assets directory
                configFile = Gdx.files.internal(CONFIG_FILE);
            }
            
            if (configFile.exists()) {
                String jsonString = configFile.readString();
                currentConfig = parseConfiguration(jsonString);
                Gdx.app.log("AndroidControllerConfig", "Loaded configuration from: " + configFile.path());
            } else {
                // Use default configuration
                currentConfig = new ControllerConfiguration();
                Gdx.app.log("AndroidControllerConfig", "Using default controller configuration");
            }
            
        } catch (Exception e) {
            Gdx.app.error("AndroidControllerConfig", "Error loading configuration: " + e.getMessage());
            currentConfig = new ControllerConfiguration();
        }
    }
    
    /**
     * Save configuration to external storage (if available)
     */
    private static void saveConfiguration() {
        try {
            FileHandle configFile = Gdx.files.external(".greenfoot/" + CONFIG_FILE);
            String jsonString = serializeConfiguration(currentConfig);
            configFile.writeString(jsonString, false);
            Gdx.app.log("AndroidControllerConfig", "Saved configuration to: " + configFile.path());
        } catch (Exception e) {
            Gdx.app.debug("AndroidControllerConfig", "Could not save configuration: " + e.getMessage());
        }
    }
    
    /**
     * Parse JSON configuration
     */
    private static ControllerConfiguration parseConfiguration(String jsonString) {
        ControllerConfiguration config = new ControllerConfiguration();
        
        try {
            Json json = new Json();
            JsonValue root = new JsonValue(JsonValue.ValueType.object);
            root = json.fromJson(null, jsonString);
            
            // Parse layout
            if (root.has("layout")) {
                String layoutName = root.getString("layout");
                for (GamepadLayout layout : GamepadLayout.values()) {
                    if (layout.name().equalsIgnoreCase(layoutName)) {
                        config.layout = layout;
                        break;
                    }
                }
            }
            
            // Parse button mappings
            if (root.has("buttonMappings")) {
                JsonValue mappings = root.get("buttonMappings");
                config.buttonMappings.clear();
                
                for (JsonValue entry = mappings.child; entry != null; entry = entry.next) {
                    try {
                        ButtonType buttonType = ButtonType.valueOf(entry.name.toUpperCase());
                        String keyMapping = entry.asString();
                        config.buttonMappings.put(buttonType, keyMapping);
                    } catch (IllegalArgumentException e) {
                        Gdx.app.debug("AndroidControllerConfig", "Unknown button type: " + entry.name);
                    }
                }
            }
            
            // Parse display settings
            if (root.has("showOnScreenControls")) {
                config.showOnScreenControls = root.getBoolean("showOnScreenControls");
            }
            if (root.has("controllerOpacity")) {
                config.controllerOpacity = root.getFloat("controllerOpacity");
            }
            if (root.has("controllerScale")) {
                config.controllerScale = root.getFloat("controllerScale");
            }
            if (root.has("enableVibration")) {
                config.enableVibration = root.getBoolean("enableVibration");
            }
            
        } catch (Exception e) {
            Gdx.app.error("AndroidControllerConfig", "Error parsing configuration: " + e.getMessage());
            // Keep default values in config
        }
        
        return config;
    }
    
    /**
     * Serialize configuration to JSON
     */
    private static String serializeConfiguration(ControllerConfiguration config) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"layout\": \"").append(config.layout.name()).append("\",\n");
        json.append("  \"showOnScreenControls\": ").append(config.showOnScreenControls).append(",\n");
        json.append("  \"controllerOpacity\": ").append(config.controllerOpacity).append(",\n");
        json.append("  \"controllerScale\": ").append(config.controllerScale).append(",\n");
        json.append("  \"enableVibration\": ").append(config.enableVibration).append(",\n");
        json.append("  \"buttonMappings\": {\n");
        
        boolean first = true;
        for (Map.Entry<ButtonType, String> entry : config.buttonMappings.entrySet()) {
            if (!first) json.append(",\n");
            json.append("    \"").append(entry.getKey().name().toLowerCase()).append("\": \"")
                .append(entry.getValue()).append("\"");
            first = false;
        }
        
        json.append("\n  }\n");
        json.append("}");
        
        return json.toString();
    }
    
    /**
     * Get key mapping for a button type
     */
    public static String getKeyMapping(ButtonType buttonType) {
        ControllerConfiguration config = getConfig();
        return config.buttonMappings.getOrDefault(buttonType, "");
    }
    
    /**
     * Get all available layouts
     */
    public static GamepadLayout[] getAvailableLayouts() {
        return GamepadLayout.values();
    }
    
    /**
     * Create a sample configuration file
     */
    public static void createSampleConfig() {
        ControllerConfiguration sampleConfig = new ControllerConfiguration();
        sampleConfig.layout = GamepadLayout.SIX_BUTTON;
        sampleConfig.showOnScreenControls = true;
        sampleConfig.controllerOpacity = 0.8f;
        sampleConfig.controllerScale = 1.2f;
        sampleConfig.enableVibration = true;
        
        // Custom mappings example
        sampleConfig.buttonMappings.put(ButtonType.ATTACK, "z");
        sampleConfig.buttonMappings.put(ButtonType.JUMP, "space");
        sampleConfig.buttonMappings.put(ButtonType.SPECIAL, "x");
        
        try {
            String projectFolder = GreenfootProjectConfig.getUserProjectFolder();
            FileHandle sampleFile = Gdx.files.local(projectFolder + "/sample_" + CONFIG_FILE);
            String jsonString = serializeConfiguration(sampleConfig);
            sampleFile.writeString(jsonString, false);
            Gdx.app.log("AndroidControllerConfig", "Created sample config at: " + sampleFile.path());
        } catch (Exception e) {
            Gdx.app.error("AndroidControllerConfig", "Could not create sample config: " + e.getMessage());
        }
    }
    
    /**
     * Reset to default configuration
     */
    public static void resetToDefault() {
        currentConfig = new ControllerConfiguration();
        initialized = true;
        saveConfiguration();
    }
    
    /**
     * Get debug information about current configuration
     */
    public static String getDebugInfo() {
        ControllerConfiguration config = getConfig();
        StringBuilder sb = new StringBuilder();
        sb.append("Android Controller Configuration:\n");
        sb.append("  Layout: ").append(config.layout.getName()).append(" (").append(config.layout.getButtonCount()).append(" buttons)\n");
        sb.append("  Description: ").append(config.layout.getDescription()).append("\n");
        sb.append("  Show Controls: ").append(config.showOnScreenControls).append("\n");
        sb.append("  Opacity: ").append(config.controllerOpacity).append("\n");
        sb.append("  Scale: ").append(config.controllerScale).append("\n");
        sb.append("  Vibration: ").append(config.enableVibration).append("\n");
        sb.append("  Button Mappings:\n");
        
        for (Map.Entry<ButtonType, String> entry : config.buttonMappings.entrySet()) {
            sb.append("    ").append(entry.getKey()).append(" -> \"").append(entry.getValue()).append("\"\n");
        }
        
        return sb.toString();
    }
}