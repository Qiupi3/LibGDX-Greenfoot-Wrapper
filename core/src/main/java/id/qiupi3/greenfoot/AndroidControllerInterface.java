package id.qiupi3.greenfoot;

import id.qiupi3.greenfoot.AndroidControllerConfig.ButtonType;

/**
 * Interface for Android-specific controller functionality.
 * This allows the core game to communicate with Android UI controls.
 * Now supports flexible button configurations and multiple gamepad layouts.
 */
public interface AndroidControllerInterface {
    
    // Traditional directional methods (maintained for backward compatibility)
    void onUpPressed(boolean pressed);
    void onDownPressed(boolean pressed);
    void onLeftPressed(boolean pressed);
    void onRightPressed(boolean pressed);
    void onActionPressed(boolean pressed);
    
    // New flexible button system
    /**
     * Called when any configured button is pressed/released
     * @param buttonType The type of button that was pressed
     * @param pressed Whether the button is pressed (true) or released (false)
     */
    void onButtonPressed(ButtonType buttonType, boolean pressed);
    
    /**
     * Check if the current platform is Android
     */
    boolean isAndroid();
    
    /**
     * Show/hide the controller UI
     */
    void setControllerVisible(boolean visible);

    /**
     * Show/hide a single on-screen button.
     *
     * @param buttonId Button id, e.g. "btn_up", "btn_action", "btn_enter".
     *                 The "btn_" prefix is optional.
     * @param visible  Whether that button should be drawn and accept touches
     */
    default void setButtonVisible(String buttonId, boolean visible) {
        // Platforms without individual on-screen buttons ignore this.
    }

    /**
     * Show/hide every on-screen button at once.
     */
    default void setAllButtonsVisible(boolean visible) {
        setControllerVisible(visible);
    }

    /**
     * Check whether a single on-screen button is currently visible.
     */
    default boolean isButtonVisible(String buttonId) {
        return false;
    }
    
    /**
     * Update the controller layout based on current configuration
     */
    void updateControllerLayout();
    
    /**
     * Set controller transparency (0.0 = invisible, 1.0 = fully visible)
     */
    void setControllerOpacity(float opacity);
    
    /**
     * Set controller scale (1.0 = normal size)
     */
    void setControllerScale(float scale);
    
    /**
     * Trigger vibration if supported and enabled
     * @param duration Duration in milliseconds
     * @param strength Vibration strength (0.0 to 1.0)
     */
    void vibrate(long duration, float strength);
    
    // State getter methods for checking button states (backward compatibility)
    boolean isUpPressed();
    boolean isDownPressed();
    boolean isLeftPressed();
    boolean isRightPressed();
    boolean isActionPressed();
    
    // New flexible button state checking
    /**
     * Check if a specific button type is currently pressed
     * @param buttonType The button type to check
     * @return true if the button is currently pressed
     */
    boolean isButtonPressed(ButtonType buttonType);
    
    /**
     * Get the current controller configuration being used
     */
    AndroidControllerConfig.ControllerConfiguration getControllerConfig();
    
    /**
     * Apply a new controller configuration
     */
    void setControllerConfig(AndroidControllerConfig.ControllerConfiguration config);
}