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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import greenfoot.collision.ColManager;
import greenfoot.collision.CollisionChecker;

import com.badlogic.gdx.utils.Array;

/**
 * LibGDX-based World implementation that serves as a Screen and container for Actors.
 * Uses LibGDX's rendering system and our custom collision management.
 * 
 * This class is the superclass of all worlds. A world is the area in which
 * actors live. It is a two-dimensional grid of cells, each of which can hold
 * one or more actors. The size of the cells is specified when the world is created.
 * 
 * This class re-implements greenfoot.World to provide a LibGDX backend,
 * mainly to allow Greenfoot projects to run on LibGDX (especially to export into
 * mobile devices and other platforms).
 * 
 * Inspired by the original Greenfoot project (GPLv2+ with Classpath Exception).
 * Read the original documentation at
 * https://www.greenfoot.org/files/javadoc/greenfoot/World.html
 * 
 * @see greenfoot.Actor
 * @author Poul Henriksen (Original Greenfoot version's author)
 * @author Michael Kolling (Original Greenfoot version's author)
 * 
 * @modified-by Qiupi3 (LibGDX wrapper implementation)
 * @version 1.0
 */

public abstract class World implements Screen {
    // Default background color (white)
    private static final greenfoot.Color DEFAULT_BACKGROUND_COLOR = greenfoot.Color.WHITE;
    
    // Collision detection system
    private CollisionChecker collisionChecker;
    
    // Storage for all actors
    private Array<Actor> allActors;
    private Array<Actor> actorsInPaintOrder;
    private Array<Actor> actorsInActOrder;
    
    // Text labels for showText functionality
    private Array<TextLabel> textLabels;
    
    // Background handling
    private Texture backgroundTexture;
    private com.badlogic.gdx.graphics.Color backgroundColor;
    private boolean hasBackgroundTexture = false;
    private Texture whitePixel; // 1x1 white texture for rendering backgrounds

    private GreenfootImage backgroundImage;
    private boolean backgroundIsClassImage;
    
    // LibGDX rendering components
    private SpriteBatch batch;
    private BitmapFont font;
    private ShapeRenderer shapeRenderer;
    private OrthographicCamera camera;
    private Viewport viewport;
    
    // World properties
    protected final int width;
    protected final int height;
    protected final int cellSize;
    private boolean bounded;
    
    // Paint order classes
    private Class<?>[] paintOrderClasses;
    private Class<?>[] actOrderClasses;

    /**
     * Create a new world with specified dimensions and cell size.
     * Default is bounded world.
     */
    public World(int worldWidth, int worldHeight, int cellSize) {
        this(worldWidth, worldHeight, cellSize, true);
    }
    
    /**
     * Create a new world with specified dimensions, cell size, and boundary setting.
     */
    public World(int worldWidth, int worldHeight, int cellSize, boolean bounded) {
        this.width = worldWidth;
        this.height = worldHeight;
        this.cellSize = cellSize;
        this.bounded = bounded;
        
        // Initialize LibGDX components
        this.batch = new SpriteBatch();
        this.font = new BitmapFont();
        this.shapeRenderer = new ShapeRenderer();
        
        // Initialize camera and viewport for proper scaling
        this.camera = new OrthographicCamera();
        this.viewport = new StretchViewport(worldWidth * cellSize, worldHeight * cellSize, camera);
        this.camera.position.set(viewport.getWorldWidth() / 2f, viewport.getWorldHeight() / 2f, 0);
        this.camera.update();
        
        // Initialize collections
        this.allActors = new Array<Actor>();
        this.textLabels = new Array<TextLabel>();
        
        // Create 1x1 white texture for backgrounds
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(1, 1, 1, 1); // White color
        pixmap.fill();
        this.whitePixel = new Texture(pixmap);
        pixmap.dispose();
        
        // Initialize collision system
        this.collisionChecker = new ColManager();
        this.collisionChecker.initialize(worldWidth, worldHeight, cellSize, !bounded);
        
        // Set default background
        this.backgroundColor = DEFAULT_BACKGROUND_COLOR.toLibGDXColor();
        backgroundIsClassImage = true;
        
        // Try to load class-specific background
        loadClassBackground();
    }
    
    // ================ Core World API ================
    
    /**
     * Set the background from a texture file.
     */
    public final void setBackground(String filename) throws IllegalArgumentException {
        try {
            if (backgroundTexture != null) {
                backgroundTexture.dispose();
            }
            backgroundTexture = new Texture(Gdx.files.internal(filename));
            hasBackgroundTexture = true;
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not load background image: " + filename, e);
        }
    }
    
    /**
     * Set the background texture directly.
     */
    public final void setBackground(Texture texture) {
        if (backgroundTexture != null && backgroundTexture != texture) {
            backgroundTexture.dispose();
        }
        backgroundTexture = texture;
        hasBackgroundTexture = (texture != null);
    }
    
    /**
     * Set the background to a solid color.
     */
    public void setBackground(Color color) {
        hasBackgroundTexture = false;
        if (backgroundTexture != null) {
            backgroundTexture.dispose();
            backgroundTexture = null;
        }
        // Convert Greenfoot Color to LibGDX Color
        if (color != null) {
            backgroundColor = color.toLibGDXColor();
        } else {
            backgroundColor = DEFAULT_BACKGROUND_COLOR.toLibGDXColor();
        }
        // Clear GreenfootImage background
        backgroundImage = null;
        backgroundIsClassImage = false;
    }
    
    /**
     * Set the background from a GreenfootImage.
     * This method handles tiling if the image is smaller than the world.
     */
    public final void setBackground(GreenfootImage image)
    {
        if (image != null) {
            int imgWidth = image.getWidth();
            int imgHeight = image.getHeight();
            int worldWidth = getWidthInPixels();
            int worldHeight = getHeightInPixels();
            boolean tile = imgWidth < worldWidth || imgHeight < worldHeight;

            if (tile) {
                backgroundIsClassImage = false;
                backgroundImage = new GreenfootImage(worldWidth, worldHeight);
                backgroundImage.setColor(DEFAULT_BACKGROUND_COLOR);
                backgroundImage.fill();

                for (int x = 0; x < worldWidth; x += imgWidth) {
                    for (int y = 0; y < worldHeight; y += imgHeight) {
                        backgroundImage.drawImage(image, x, y);
                    }
                }
            }
            else {
                // To make it behave exactly the same way as when tiling we
                // should make a clone here. But it performs better when not cloning.
                // Performance will be an issue for people changing the
                // background image all the time for animated backgrounds
                backgroundImage = image;
                backgroundIsClassImage = false;
            }
            
            // Update texture for rendering
            if (backgroundTexture != null) {
                backgroundTexture.dispose();
            }
            backgroundTexture = backgroundImage.getTexture();
            hasBackgroundTexture = true;
        }
        else {
            backgroundIsClassImage = false;
            backgroundImage = null;
            if (backgroundTexture != null) {
                backgroundTexture.dispose();
                backgroundTexture = null;
            }
            hasBackgroundTexture = false;
        }
    }

    /**
     * Get the background image (if set).
     * Returns null if background is a solid color or no background is set.
     */
    public GreenfootImage getBackground() {
        if (backgroundImage == null) {
            backgroundImage = new GreenfootImage(getWidthInPixels(), getHeightInPixels());
            backgroundImage.setColor(DEFAULT_BACKGROUND_COLOR);
            backgroundImage.fill();
            backgroundIsClassImage = false;
        }
        else if (backgroundIsClassImage) {
            // Make the image a copy of the original to avoid modifications
            // to the original.
            backgroundImage = backgroundImage.getCopyOnWriteClone();
            backgroundIsClassImage = false;
        }
        return backgroundImage;
    }

    /**
     * Get the background color at a specific cell location.
     */
    public Color getColorAt(int cellX, int cellY) {
        ensureWithinXBounds(cellX);
        ensureWithinYBounds(cellY);
        
        // For now, return the background color
        // In a more advanced implementation, we could sample the background texture
        return Color.fromLibGDXColor(backgroundColor);
    }
    
    /**
     * Get world width in cells.
     */
    public int getWidth() { 
        return width; 
    }
    
    /**
     * Get world height in cells.
     */
    public int getHeight() { 
        return height; 
    }
    
    /**
     * Get cell size in pixels.
     */
    public int getCellSize() { 
        return cellSize; 
    }
    
    /**
     * Get world width in pixels.
     */
    public int getWidthInPixels() {
        return width * cellSize;
    }
    
    /**
     * Get world height in pixels.
     */
    public int getHeightInPixels() {
        return height * cellSize;
    }
    
    /**
     * Check if world is bounded.
     */
    public boolean isBounded() {
        return bounded;
    }
    
    /**
     * Set the paint order for actors.
     */
    @SuppressWarnings("unchecked")
    public final void setPaintOrder(Class<? extends Actor>... classes) {
        this.paintOrderClasses = classes;
        updatePaintOrder();
    }
    
    /**
     * Set the act order for actors.
     */
    @SuppressWarnings("unchecked") 
    public final void setActOrder(Class<? extends Actor>... classes) {
        this.actOrderClasses = classes;
        updateActOrder();
    }
    
    /**
     * Add an actor to the world at specified coordinates.
     */
    public final void addObject(Actor object, int x, int y) {
        if (object == null) return;
        
        // Remove from previous world if necessary
        if (object.getWorld() != null) {
            if (object.getWorld() == this) {
                return; // Already in this world
            }
            object.getWorld().removeObject(object);
        }
        
        // Add to collections
        allActors.add(object);
        updatePaintOrder();
        updateActOrder();
        
        // Add to world and collision system
        object.addToWorld(x, y, this);
        collisionChecker.addObject(object);
        
        // Notify actor
        object.addedToWorld(this);
    }
    
    /**
     * Remove an actor from the world.
     */
    public final void removeObject(Actor object) {
        if (object == null || object.getWorld() != this) {
            return;
        }
        
        // Remove from collections
        allActors.removeValue(object, true);
        if (actorsInPaintOrder != null) {
            actorsInPaintOrder.removeValue(object, true);
        }
        if (actorsInActOrder != null) {
            actorsInActOrder.removeValue(object, true);
        }
        
        // Remove from collision system
        collisionChecker.removeObject(object);
        
        // Remove from world
        object.setRemovedFromWorld(new RuntimeException("Actor removed from world"));
    }
    
    /**
     * Remove multiple actors from the world.
     */
    public final void removeObjects(Collection<? extends Actor> objects) {
        if (objects == null) return;
        
        for (Actor actor : objects) {
            removeObject(actor);
        }
    }
    
    /**
     * Get all objects of a specific class.
     */
    @SuppressWarnings("unchecked")
    public final <A> List<A> getObjects(Class<A> cls) {
        List<A> result = new ArrayList<A>();
        
        for (Actor actor : allActors) {
            if (cls == null || cls.isAssignableFrom(actor.getClass())) {
                result.add((A) actor);
            }
        }
        
        return result;
    }
    
    /**
     * Get the number of objects in the world.
     */
    public int numberOfObjects() {
        return allActors.size;
    }

    /**
     * Request a repaint of the world.
     */
    public void repaint() {
        // In LibGDX, rendering happens automatically
    }

    /**
     * Act method called each simulation step.
     */
    public void act() {
        // Override in subclasses
    }

    /**
     * Called when the simulation is started.
     */
    public void started() {
        // Override in subclasses
    }
    
    /**
     * Called when the simulation is stopped.
     */
    public void stopped() {
        // Override in subclasses
    }
    
    /**
     * Get objects at a specific grid location.
     */
    @SuppressWarnings("unchecked")
    public <A> List<A> getObjectsAt(int x, int y, Class<A> cls) {
        return (List<A>) collisionChecker.getObjectsAt(x, y, (Class<Actor>) cls);
    }
    
    /**
     * Display text at a specific location.
     */
    public final void showText(String text, int x, int y) {
        // Remove existing text at this location
        for (int i = textLabels.size - 1; i >= 0; i--) {
            TextLabel label = textLabels.get(i);
            if (label.getX() == x && label.getY() == y) {
                textLabels.removeIndex(i);
                break;
            }
        }
        
        // Add new text if not empty
        if (text != null && !text.trim().isEmpty()) {
            textLabels.add(new TextLabel(text, x, y));
        }
    }
    
    /**
     * Get all text labels currently displayed in the world.
     * Package-private method for WorldVisitor access.
     */
    Array<TextLabel> getTextLabels() {
        return textLabels;
    }
    
    // ================ Collision and Object Query Methods ================
    
    /**
     * Get objects that intersect with the given actor.
     */
    @SuppressWarnings("unchecked")
    public <A> List<A> getIntersectingObjects(Actor actor, Class<A> cls) {
        return (List<A>) collisionChecker.getIntersectingObjects(actor, (Class<Actor>) cls);
    }
    
    /**
     * Get objects within a range of a point.
     */
    @SuppressWarnings("unchecked")
    public <A> List<A> getObjectsInRange(int x, int y, int radius, Class<A> cls) {
        return (List<A>) collisionChecker.getObjectsInRange(x, y, radius, (Class<Actor>) cls);
    }
    
    /**
     * Get neighbors of an actor.
     */
    @SuppressWarnings("unchecked")
    public <A> List<A> getNeighbours(Actor actor, int distance, boolean diagonal, Class<A> cls) {
        if (distance < 0) {
            throw new IllegalArgumentException("Distance must not be less than 0. It was: " + distance);
        }
        return (List<A>) collisionChecker.getNeighbours(actor, distance, diagonal, (Class<Actor>) cls);
    }
    
    /**
     * Get objects in a specific direction.
     */
    @SuppressWarnings("unchecked")
    public <A> List<A> getObjectsInDirection(int x0, int y0, int angle, int length, Class<A> cls) {
        return (List<A>) collisionChecker.getObjectsInDirection(x0, y0, angle, length, (Class<Actor>) cls);
    }
    
    /**
     * Get one object at a specific offset from an actor.
     */
    @SuppressWarnings("unchecked")
    public Actor getOneObjectAt(Actor object, int dx, int dy, Class<?> cls) {
        return collisionChecker.getOneObjectAt(object, dx, dy, (Class<Actor>) cls);
    }
    
    /**
     * Get one object that intersects with the given actor.
     */
    @SuppressWarnings("unchecked")
    public Actor getOneIntersectingObject(Actor object, Class<?> cls) {
        return collisionChecker.getOneIntersectingObject(object, (Class<Actor>) cls);
    }
    
    // ================ Internal Update Methods ================
    
    /**
     * Update object location in collision system.
     */
    void updateObjectLocation(Actor object, int oldX, int oldY) {
        collisionChecker.updateObjectLocation(object, oldX, oldY);
    }
    
    /**
     * Update object size in collision system.
     */
    void updateObjectSize(Actor object) {
        collisionChecker.updateObjectSize(object);
    }
    
    /**
     * Start new sequence in collision system.
     */
    void startSequence() {
        collisionChecker.startSequence();
    }
    
    // ================ LibGDX Screen Implementation ================
    
    @Override
    public void show() {
        // Called when this world becomes the active screen
    }
    
    @Override
    public void render(float delta) {
        // Update viewport and apply camera
        viewport.apply();
        camera.update();
        
        // Clear screen with background color
        Gdx.gl.glClearColor(backgroundColor.r, backgroundColor.g, backgroundColor.b, backgroundColor.a);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        
        // Set batch to use camera's combined matrix for proper scaling
        batch.setProjectionMatrix(camera.combined);
        
        // Begin batch rendering
        batch.begin();
        
        // Draw background texture if available (scaled to fill viewport)
        if (hasBackgroundTexture && backgroundTexture != null) {
            batch.draw(backgroundTexture, 0, 0, viewport.getWorldWidth(), viewport.getWorldHeight());
        }
        
        // Render all actors in paint order
        Array<Actor> actorsToRender = getActorsInPaintOrder();
        for (Actor actor : actorsToRender) {
            if (!actor.isSleeping()) {
                actor.render(batch);
            }
        }
        
        // Render text labels
        for (TextLabel label : textLabels) {
            float pixelX = label.getX() * cellSize;
            float pixelY = label.getY() * cellSize;
            font.draw(batch, label.getText(), pixelX, pixelY);
        }
        
        // Show pause indicator when simulation is paused
        if (Greenfoot.isPaused()) {
            // Draw "PAUSED" text in the center of the screen
            String pauseText = "PAUSED";
            com.badlogic.gdx.graphics.g2d.GlyphLayout layout = new com.badlogic.gdx.graphics.g2d.GlyphLayout(font, pauseText);
            float textX = (viewport.getWorldWidth() - layout.width) / 2;
            float textY = (viewport.getWorldHeight() + layout.height) / 2;
            
            // Draw semi-transparent background for better text visibility
            batch.setColor(0, 0, 0, 0.7f);
            batch.draw(whitePixel, textX - 10, textY - layout.height - 10, layout.width + 20, layout.height + 20);
            batch.setColor(1, 1, 1, 1); // Reset to white
            
            font.draw(batch, pauseText, textX, textY);
        }
        
        batch.end();
        
        // Handle pause/resume keyboard shortcut (P key or ESC key)
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.P) || 
            Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.ESCAPE)) {
            Greenfoot.togglePause();
        }
        
        // Update actors (act step) - only if not paused
        if (delta > 0 && !Greenfoot.isPaused()) {
            startSequence();
            
            // Call world act method
            act();
            
            // Call act on all actors in act order
            Array<Actor> actorsToAct = getActorsInActOrder();
            for (Actor actor : actorsToAct) {
                if (!actor.isSleeping()) {
                    actor.act();
                }
            }
        }
    }
    
    @Override
    public void resize(int width, int height) {
        // Update viewport when screen size changes
        viewport.update(width, height);
        camera.update();
    }
    
    @Override
    public void pause() {
        // Handle pause
    }
    
    @Override
    public void resume() {
        // Handle resume
    }
    
    @Override
    public void hide() {
        // Called when this world is no longer the active screen
    }
    
    @Override
    public void dispose() {
        // Clean up resources
        if (backgroundTexture != null) {
            backgroundTexture.dispose();
        }
        if (batch != null) {
            batch.dispose();
        }
        if (font != null) {
            font.dispose();
        }
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
        }
        if (whitePixel != null) {
            whitePixel.dispose();
        }
        
        // Dispose all actors
        for (Actor actor : allActors) {
            actor.dispose();
        }
    }
    
    // ================ Helper Methods ================
    
    /**
     * Get center pixel coordinate of a cell.
     */
    @SuppressWarnings("unused")
    private double getCellCenter(int cellPosition) {
        return cellPosition * cellSize + cellSize / 2.0;
    }
    
    /**
     * Convert pixel coordinate to cell coordinate (floor).
     */
    @SuppressWarnings("unused")
    private int toCellFloor(int pixel) {
        return (int) Math.floor((double) pixel / cellSize);
    }
    
    /**
     * Convert pixel coordinate to cell coordinate (ceiling).
     */
    @SuppressWarnings("unused")
    private int toCellCeil(int pixel) {
        return (int) Math.ceil((double) pixel / cellSize);
    }
    
    /**
     * Ensure X coordinate is within bounds.
     */
    private void ensureWithinXBounds(int x) throws IndexOutOfBoundsException {
        if (x >= getWidth()) {
            throw new IndexOutOfBoundsException("The x-coordinate is: " + x + ". It must be smaller than: " + getWidth());
        }
        if (x < 0) {
            throw new IndexOutOfBoundsException("The x-coordinate is: " + x + ". It must be larger than: 0");
        }
    }
    
    /**
     * Ensure Y coordinate is within bounds.
     */
    private void ensureWithinYBounds(int y) throws IndexOutOfBoundsException {
        if (y >= getHeight()) {
            throw new IndexOutOfBoundsException("The y-coordinate is: " + y + ". It must be smaller than: " + getHeight());
        }
        if (y < 0) {
            throw new IndexOutOfBoundsException("The y-coordinate is: " + y + ". It must be larger than: 0");
        }
    }
    
    /**
     * Load class-specific background image.
     */
    private void loadClassBackground() {
        String className = getClass().getSimpleName();
        
        // Try to get image from project.greenfoot configuration
        try {
            String imageFileName = getImageFromProjectFile(className);
            if (imageFileName != null) {
                // Try to load the specified image
                if (Gdx.files.internal("tes/images/" + imageFileName).exists()) {
                    setBackground("tes/images/" + imageFileName);
                    return;
                }
            }
        } catch (Exception e) {
            // Continue to fallback method
        }
        
        // Fallback: try to load image with class name
        try {
            String classNameLower = className.toLowerCase();
            if (Gdx.files.internal("tes/images/" + classNameLower + ".png").exists()) {
                setBackground("tes/images/" + classNameLower + ".png");
            }
        } catch (Exception e) {
            // Ignore and use default background
        }
    }
    
    /**
     * Get the image filename for a class from project.greenfoot file.
     */
    private String getImageFromProjectFile(String className) {
        try {
            // Use LibGDX internal file system (works on all platforms including Android)
            com.badlogic.gdx.files.FileHandle projectFile = com.badlogic.gdx.Gdx.files.internal("tes/project.greenfoot");
            if (projectFile.exists()) {
                String content = projectFile.readString();
                String[] lines = content.split("\n");
                String searchPattern = "class." + className + ".image=";
                
                for (String line : lines) {
                    if (line.startsWith(searchPattern)) {
                        return line.substring(searchPattern.length()).trim();
                    }
                }
            }
        } catch (Exception e) {
            // Ignore and return null
        }
        return null;
    }
    
    /**
     * Update paint order based on class order.
     */
    private void updatePaintOrder() {
        if (paintOrderClasses == null) {
            actorsInPaintOrder = null;
            return;
        }
        
        if (actorsInPaintOrder == null) {
            actorsInPaintOrder = new Array<Actor>();
        } else {
            actorsInPaintOrder.clear();
        }
        
        // Add actors in specified class order
        for (Class<?> cls : paintOrderClasses) {
            for (Actor actor : allActors) {
                if (cls.isAssignableFrom(actor.getClass())) {
                    actorsInPaintOrder.add(actor);
                }
            }
        }
        
        // Add any remaining actors not in specified classes
        for (Actor actor : allActors) {
            if (!actorsInPaintOrder.contains(actor, true)) {
                actorsInPaintOrder.add(actor);
            }
        }
    }
    
    /**
     * Update act order based on class order.
     */
    private void updateActOrder() {
        if (actOrderClasses == null) {
            actorsInActOrder = null;
            return;
        }
        
        if (actorsInActOrder == null) {
            actorsInActOrder = new Array<Actor>();
        } else {
            actorsInActOrder.clear();
        }
        
        // Add actors in specified class order
        for (Class<?> cls : actOrderClasses) {
            for (Actor actor : allActors) {
                if (cls.isAssignableFrom(actor.getClass())) {
                    actorsInActOrder.add(actor);
                }
            }
        }
        
        // Add any remaining actors not in specified classes
        for (Actor actor : allActors) {
            if (!actorsInActOrder.contains(actor, true)) {
                actorsInActOrder.add(actor);
            }
        }
    }
    
    /**
     * Get actors in paint order.
     */
    private Array<Actor> getActorsInPaintOrder() {
        return (actorsInPaintOrder != null) ? actorsInPaintOrder : allActors;
    }
    
    /**
     * Get actors in act order.
     */
    private Array<Actor> getActorsInActOrder() {
        return (actorsInActOrder != null) ? actorsInActOrder : allActors;
    }
    
    // ================ Inner Classes ================
    
    /**
     * Text label for showText functionality.
     * Package-private for WorldVisitor access.
     */
    static class TextLabel {
        private final String text;
        private final int x, y;
        
        public TextLabel(String text, int x, int y) {
            this.text = text;
            this.x = x;
            this.y = y;
        }
        
        public String getText() { return text; }
        public int getX() { return x; }
        public int getY() { return y; }
    }
}