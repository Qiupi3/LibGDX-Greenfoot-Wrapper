package id.qiupi3.greenfoot;

/**
 * Interface for Android-specific controller functionality.
 * This allows the core game to communicate with Android UI controls.
 */
public interface AndroidControllerInterface {
    
    /**
     * Called when up direction is pressed/released
     */
    void onUpPressed(boolean pressed);
    
    /**
     * Called when down direction is pressed/released
     */
    void onDownPressed(boolean pressed);
    
    /**
     * Called when left direction is pressed/released
     */
    void onLeftPressed(boolean pressed);
    
    /**
     * Called when right direction is pressed/released
     */
    void onRightPressed(boolean pressed);
    
    /**
     * Called when action button is pressed/released
     */
    void onActionPressed(boolean pressed);
    
    /**
     * Check if the current platform is Android
     */
    boolean isAndroid();
    
    /**
     * Show/hide the controller UI
     */
    void setControllerVisible(boolean visible);
    
    // State getter methods for checking button states
    boolean isUpPressed();
    boolean isDownPressed();
    boolean isLeftPressed();
    boolean isRightPressed();
    boolean isActionPressed();
}