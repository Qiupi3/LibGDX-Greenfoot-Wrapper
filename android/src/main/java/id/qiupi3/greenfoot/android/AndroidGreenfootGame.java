package id.qiupi3.greenfoot.android;

import com.badlogic.gdx.Gdx;
import id.qiupi3.greenfoot.AndroidControllerConfig;
import id.qiupi3.greenfoot.AndroidControllerConfig.ButtonType;
import id.qiupi3.greenfoot.AndroidControllerInterface;
import id.qiupi3.greenfoot.GreenfootGame;
import id.qiupi3.greenfoot.VirtualController;

/**
 * Android-specific version of GreenfootGame.
 *
 * The on-screen pad itself lives in core ({@link VirtualController}) and is created
 * and driven by {@link GreenfootGame}, because it is plain LibGDX drawing and touch
 * polling with nothing Android-specific in it. What is left here is the Android
 * flavour of the {@link AndroidControllerInterface} contract: identifying the
 * platform, vibration, and translating the older {@link ButtonType} vocabulary into
 * the key names the pad now speaks.
 *
 * @author Qiupi3
 * @version 1.0
 */
public class AndroidGreenfootGame extends GreenfootGame implements AndroidControllerInterface {

    private boolean upPressed, downPressed, leftPressed, rightPressed, actionPressed;
    private float controllerOpacity = 0.55f;
    private float controllerScale = 1.0f;

    @Override
    public void create() {
        super.create();

        // Appearance only. Visibility is deliberately NOT applied here: super.create()
        // has already built the first world, and a world's constructor commonly hides
        // the pad (a menu, say). Re-applying the saved "showOnScreenControls" at this
        // point would undo that, which is a difference only Android would have shown.
        AndroidControllerConfig.ControllerConfiguration config = AndroidControllerConfig.getConfig();
        if (config != null) {
            setControllerOpacity(config.controllerOpacity);
            setControllerScale(config.controllerScale);
        }
    }

    /** The shared pad, or null if this run has none. */
    private VirtualController pad() {
        return GreenfootGame.getVirtualController();
    }

    // ================ platform ================

    @Override
    public boolean isAndroid() {
        return true;
    }

    @Override
    public void vibrate(long duration, float strength) {
        try {
            Gdx.input.vibrate((int) duration);
        } catch (Exception e) {
            Gdx.app.log("AndroidGreenfootGame", "Vibration not available: " + e.getMessage());
        }
    }

    // ================ pad visibility and appearance ================

    @Override
    public void setControllerVisible(boolean visible) {
        VirtualController pad = pad();
        if (pad != null) {
            pad.setVisible(visible);
        }
    }

    @Override
    public void setButtonVisible(String buttonId, boolean visible) {
        VirtualController pad = pad();
        if (pad != null) {
            pad.setSlotVisible(VirtualController.Slot.fromId(buttonId), visible);
        }
    }

    @Override
    public void setAllButtonsVisible(boolean visible) {
        VirtualController pad = pad();
        if (pad != null) {
            pad.setAllSlotsVisible(visible);
        }
    }

    @Override
    public boolean isButtonVisible(String buttonId) {
        VirtualController pad = pad();
        return pad != null && pad.isSlotVisible(VirtualController.Slot.fromId(buttonId));
    }

    @Override
    public void updateControllerLayout() {
        VirtualController pad = pad();
        if (pad != null) {
            pad.resize();
        }
    }

    @Override
    public void setControllerOpacity(float opacity) {
        this.controllerOpacity = opacity;
        VirtualController pad = pad();
        if (pad != null) {
            pad.setOpacity(opacity);
        }
    }

    @Override
    public void setControllerScale(float scale) {
        this.controllerScale = scale;
        VirtualController pad = pad();
        if (pad != null) {
            pad.setScale(scale);
        }
    }

    // ================ button state ================

    @Override
    public void onUpPressed(boolean pressed) {
        this.upPressed = pressed;
    }

    @Override
    public void onDownPressed(boolean pressed) {
        this.downPressed = pressed;
    }

    @Override
    public void onLeftPressed(boolean pressed) {
        this.leftPressed = pressed;
    }

    @Override
    public void onRightPressed(boolean pressed) {
        this.rightPressed = pressed;
    }

    @Override
    public void onActionPressed(boolean pressed) {
        this.actionPressed = pressed;
    }

    @Override
    public void onButtonPressed(ButtonType buttonType, boolean pressed) {
        if (buttonType == null) {
            return;
        }
        switch (buttonType) {
            case UP: upPressed = pressed; break;
            case DOWN: downPressed = pressed; break;
            case LEFT: leftPressed = pressed; break;
            case RIGHT: rightPressed = pressed; break;
            case ACTION: actionPressed = pressed; break;
            default: break;
        }
    }

    /**
     * Ask the pad whether the key this button type is mapped to is held. The mapping
     * comes from {@link AndroidControllerConfig}, so a project that renamed a button's
     * key keeps working.
     */
    @Override
    public boolean isButtonPressed(ButtonType buttonType) {
        if (buttonType == null) {
            return false;
        }
        VirtualController pad = pad();
        if (pad == null) {
            return false;
        }

        AndroidControllerConfig.ControllerConfiguration config = AndroidControllerConfig.getConfig();
        String keyName = config != null ? config.buttonMappings.get(buttonType) : null;
        if (keyName == null) {
            keyName = buttonType.name().toLowerCase();
        }
        return pad.isKeyDown(keyName);
    }

    @Override
    public boolean isUpPressed() {
        return upPressed || isVirtualKeyDown("up") || isVirtualKeyDown("w");
    }

    @Override
    public boolean isDownPressed() {
        return downPressed || isVirtualKeyDown("down") || isVirtualKeyDown("s");
    }

    @Override
    public boolean isLeftPressed() {
        return leftPressed || isVirtualKeyDown("left") || isVirtualKeyDown("a");
    }

    @Override
    public boolean isRightPressed() {
        return rightPressed || isVirtualKeyDown("right") || isVirtualKeyDown("d");
    }

    @Override
    public boolean isActionPressed() {
        return actionPressed || isVirtualKeyDown("space");
    }

    // ================ configuration ================

    @Override
    public AndroidControllerConfig.ControllerConfiguration getControllerConfig() {
        return AndroidControllerConfig.getConfig();
    }

    @Override
    public void setControllerConfig(AndroidControllerConfig.ControllerConfiguration config) {
        if (config == null) {
            return;
        }
        AndroidControllerConfig.setConfig(config);
        setControllerOpacity(config.controllerOpacity);
        setControllerScale(config.controllerScale);
        setControllerVisible(config.showOnScreenControls);
    }
}
