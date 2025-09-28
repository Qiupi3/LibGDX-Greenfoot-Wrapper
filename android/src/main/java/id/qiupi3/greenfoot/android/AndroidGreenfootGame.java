package id.qiupi3.greenfoot.android;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import id.qiupi3.greenfoot.AndroidControllerConfig;
import id.qiupi3.greenfoot.AndroidControllerConfig.ButtonType;
import id.qiupi3.greenfoot.AndroidControllerInterface;
import id.qiupi3.greenfoot.GreenfootGame;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Android-specific version of GreenfootGame with virtual controls.
 * 
 * @author Qiupi3
 * @version 1.0
 */
public class AndroidGreenfootGame extends GreenfootGame implements AndroidControllerInterface {
    
    private Stage uiStage;
    private ImageButton upButton, downButton, leftButton, rightButton, actionButton, enterButton;
    private boolean upPressed, downPressed, leftPressed, rightPressed, actionPressed;
    private boolean controllerVisible = true;

    /** On-screen buttons by id ("btn_up", "btn_action", "btn_enter", ...). */
    private final Map<String, ImageButton> buttonsById = new HashMap<>();
    /** Press state for the flexible button system. */
    private final Map<ButtonType, Boolean> buttonStates = new EnumMap<>(ButtonType.class);

    private float controllerOpacity = 1.0f;
    private float controllerScale = 1.0f;
    
    @Override
    public void create() {
        super.create();
        
        // Initialize UI stage for controls
        uiStage = new Stage(new ScreenViewport());
        
        // Create virtual controls
        createVirtualControls();
        
        // Set input processor to handle both game and UI input
        Gdx.input.setInputProcessor(uiStage);
    }
    
    @Override
    public void render() {
        // Render the main game first
        super.render();
        
        // Clear depth buffer for UI rendering
        Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);
        
        // Render UI controls on top
        if (controllerVisible) {
            uiStage.act(Gdx.graphics.getDeltaTime());
            uiStage.draw();
        }
    }
    
    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        uiStage.getViewport().update(width, height, true);
    }
    
    @Override
    public void dispose() {
        super.dispose();
        if (uiStage != null) {
            uiStage.dispose();
        }
    }
    
    private void createVirtualControls() {
        // CONTROLLER SIZE CONFIGURATION:
        // - buttonTextureSize: Resolution of button graphics (higher = sharper)
        // - buttonDisplaySize: How big buttons appear on screen (higher = bigger buttons)  
        // - spacingSize: Empty space between buttons (higher = more spread out)
        
        int buttonTextureSize = 96;
        int buttonDisplaySize = 120;
        int spacingSize = 80;
        
        // Create simple colored rectangles as button textures
        Texture buttonTexture = createButtonTexture(buttonTextureSize, buttonTextureSize, 0.3f, 0.3f, 0.8f, 0.8f); // Semi-transparent blue
        Texture pressedTexture = createButtonTexture(buttonTextureSize, buttonTextureSize, 0.5f, 0.5f, 1.0f, 0.9f); // Lighter when pressed
        
        TextureRegionDrawable buttonDrawable = new TextureRegionDrawable(new TextureRegion(buttonTexture));
        TextureRegionDrawable pressedDrawable = new TextureRegionDrawable(new TextureRegion(pressedTexture));
        
        // Create directional buttons
        upButton = createDirectionButton(buttonDrawable, pressedDrawable, true, () -> onUpPressed(true), () -> onUpPressed(false));
        downButton = createDirectionButton(buttonDrawable, pressedDrawable, true, () -> onDownPressed(true), () -> onDownPressed(false));
        leftButton = createDirectionButton(buttonDrawable, pressedDrawable, true, () -> onLeftPressed(true), () -> onLeftPressed(false));
        rightButton = createDirectionButton(buttonDrawable, pressedDrawable, true, () -> onRightPressed(true), () -> onRightPressed(false));
        
        // Create action button
        actionButton = createDirectionButton(buttonDrawable, pressedDrawable, false, () -> onActionPressed(true), () -> onActionPressed(false));

        // Create enter/confirm button (used by dialogs and menus)
        enterButton = createDirectionButton(buttonDrawable, pressedDrawable, false,
                () -> onButtonPressed(ButtonType.ENTER, true), () -> onButtonPressed(ButtonType.ENTER, false));

        buttonsById.put("btn_up", upButton);
        buttonsById.put("btn_down", downButton);
        buttonsById.put("btn_left", leftButton);
        buttonsById.put("btn_right", rightButton);
        buttonsById.put("btn_action", actionButton);
        buttonsById.put("btn_enter", enterButton);

        // Layout the controls with the new sizes
        layoutControls(buttonDisplaySize, spacingSize);
    }

    /**
     * Normalize a button id so both "enter" and "btn_enter" resolve to the same button.
     */
    private ImageButton findButton(String buttonId) {
        if (buttonId == null) {
            return null;
        }
        String id = buttonId.trim().toLowerCase();
        if (!id.startsWith("btn_")) {
            id = "btn_" + id;
        }
        return buttonsById.get(id);
    }
    
    private ImageButton createDirectionButton(TextureRegionDrawable normalDrawable, 
                                            TextureRegionDrawable pressedDrawable, 
                                            boolean isDirectional,
                                            Runnable onTouchDown, 
                                            Runnable onTouchUp) {
        
        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.up = normalDrawable;
        style.down = pressedDrawable;
        
        ImageButton button = new ImageButton(style);
        
        button.addListener(new ClickListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                onTouchDown.run();
                return true;
            }
            
            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                onTouchUp.run();
            }
        });
        
        return button;
    }
    
    private void layoutControls(int buttonSize, int spacingSize) {
        // Drop any previous layout so re-layouts don't stack tables on the stage
        uiStage.clear();

        // Create main table for layout
        Table mainTable = new Table();
        mainTable.setFillParent(true);
        
        // Left side - D-pad
        // LAYOUT CONFIGURATION:
        // - buttonSize: Size of the actual clickable buttons
        // - spacingSize: Size of empty spaces between buttons (affects D-pad spread)
        Table dpadTable = new Table();
        dpadTable.add().size(spacingSize, spacingSize); // Empty space (top-left)
        dpadTable.add(upButton).size(buttonSize, buttonSize); // UP button
        dpadTable.add().size(spacingSize, spacingSize); // Empty space (top-right)
        dpadTable.row();
        dpadTable.add(leftButton).size(buttonSize, buttonSize); // LEFT button  
        dpadTable.add().size(spacingSize, spacingSize); // Center space
        dpadTable.add(rightButton).size(buttonSize, buttonSize); // RIGHT button
        dpadTable.row();
        dpadTable.add().size(spacingSize, spacingSize); // Empty space (bottom-left)
        dpadTable.add(downButton).size(buttonSize, buttonSize); // DOWN button
        dpadTable.add().size(spacingSize, spacingSize); // Empty space (bottom-right)
        
        // Right side - Action and enter buttons
        Table actionTable = new Table();
        actionTable.add(enterButton).size(buttonSize, buttonSize); // ENTER button
        actionTable.row();
        actionTable.add(actionButton).size(buttonSize, buttonSize).padTop(spacingSize / 2f); // ACTION button

        // Position controls at bottom corners with padding
        // POSITIONING CONFIGURATION: 
        // - .pad(20): Distance from screen edges (increase for more margin)
        mainTable.add(dpadTable).expand().bottom().left().pad(20);
        mainTable.add(actionTable).expand().bottom().right().pad(20);
        
        uiStage.addActor(mainTable);
    }
    
    private Texture createButtonTexture(int width, int height, float r, float g, float b, float a) {
        // Create a simple colored texture for buttons
        com.badlogic.gdx.graphics.Pixmap pixmap = new com.badlogic.gdx.graphics.Pixmap(width, height, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        pixmap.setColor(r, g, b, a);
        pixmap.fillCircle(width/2, height/2, width/2 - 2);
        pixmap.setColor(1, 1, 1, 0.6f); // White border
        pixmap.drawCircle(width/2, height/2, width/2 - 2);
        
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }
    
    // AndroidControllerInterface implementation
    @Override
    public void onUpPressed(boolean pressed) {
        this.upPressed = pressed;
        // You can inject key events or handle movement directly here
        if (pressed) {
            // Simulate "up" key press for Greenfoot compatibility
            // This can be handled by actors that check for input
        }
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
    public boolean isAndroid() {
        return true;
    }
    
    @Override
    public void setControllerVisible(boolean visible) {
        this.controllerVisible = visible;
    }

    @Override
    public void onButtonPressed(ButtonType buttonType, boolean pressed) {
        if (buttonType == null) {
            return;
        }
        buttonStates.put(buttonType, pressed);

        // Keep the directional/action flags in sync for the legacy getters
        switch (buttonType) {
            case UP: upPressed = pressed; break;
            case DOWN: downPressed = pressed; break;
            case LEFT: leftPressed = pressed; break;
            case RIGHT: rightPressed = pressed; break;
            case ACTION: actionPressed = pressed; break;
            default: break;
        }
    }

    @Override
    public boolean isButtonPressed(ButtonType buttonType) {
        Boolean pressed = buttonStates.get(buttonType);
        return pressed != null && pressed;
    }

    @Override
    public void setButtonVisible(String buttonId, boolean visible) {
        ImageButton button = findButton(buttonId);
        if (button != null) {
            button.setVisible(visible);
            // An invisible button must not swallow touches either
            button.setTouchable(visible
                    ? com.badlogic.gdx.scenes.scene2d.Touchable.enabled
                    : com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
        }
    }

    @Override
    public void setAllButtonsVisible(boolean visible) {
        for (String id : buttonsById.keySet()) {
            setButtonVisible(id, visible);
        }
    }

    @Override
    public boolean isButtonVisible(String buttonId) {
        ImageButton button = findButton(buttonId);
        return button != null && button.isVisible();
    }

    @Override
    public void updateControllerLayout() {
        if (uiStage == null) {
            return;
        }
        int buttonDisplaySize = (int) (120 * controllerScale);
        int spacingSize = (int) (80 * controllerScale);
        layoutControls(buttonDisplaySize, spacingSize);
        setControllerOpacity(controllerOpacity);
    }

    @Override
    public void setControllerOpacity(float opacity) {
        this.controllerOpacity = opacity;
        for (ImageButton button : buttonsById.values()) {
            button.getColor().a = opacity;
        }
    }

    @Override
    public void setControllerScale(float scale) {
        this.controllerScale = scale;
        updateControllerLayout();
    }

    @Override
    public void vibrate(long duration, float strength) {
        try {
            Gdx.input.vibrate((int) duration);
        } catch (Exception e) {
            Gdx.app.log("AndroidGreenfootGame", "Vibration not available: " + e.getMessage());
        }
    }

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
        controllerOpacity = config.controllerOpacity;
        controllerScale = config.controllerScale;
        setControllerVisible(config.showOnScreenControls);
        updateControllerLayout();
    }

    // Public getters for game logic to access button states
    public boolean isUpPressed() { return upPressed; }
    public boolean isDownPressed() { return downPressed; }
    public boolean isLeftPressed() { return leftPressed; }
    public boolean isRightPressed() { return rightPressed; }
    public boolean isActionPressed() { return actionPressed; }
}