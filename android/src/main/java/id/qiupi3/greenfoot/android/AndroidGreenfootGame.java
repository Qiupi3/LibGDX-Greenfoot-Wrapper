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
import id.qiupi3.greenfoot.AndroidControllerInterface;
import id.qiupi3.greenfoot.GreenfootGame;

/**
 * Android-specific version of GreenfootGame with virtual controls.
 */
public class AndroidGreenfootGame extends GreenfootGame implements AndroidControllerInterface {
    
    private Stage uiStage;
    private ImageButton upButton, downButton, leftButton, rightButton, actionButton;
    private boolean upPressed, downPressed, leftPressed, rightPressed, actionPressed;
    private boolean controllerVisible = true;
    
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
        
        int buttonTextureSize = 96;  // Increased from 64 for sharper graphics
        int buttonDisplaySize = 120; // Increased from 80 for bigger buttons
        int spacingSize = 80;        // Increased from 64 for better spacing
        
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
        
        // Layout the controls with the new sizes
        layoutControls(buttonDisplaySize, spacingSize);
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
        
        // Right side - Action button
        Table actionTable = new Table();
        actionTable.add(actionButton).size(buttonSize, buttonSize); // ACTION button
        
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
    
    // Public getters for game logic to access button states
    public boolean isUpPressed() { return upPressed; }
    public boolean isDownPressed() { return downPressed; }
    public boolean isLeftPressed() { return leftPressed; }
    public boolean isRightPressed() { return rightPressed; }
    public boolean isActionPressed() { return actionPressed; }
}