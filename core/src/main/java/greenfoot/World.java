package greenfoot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import com.badlogic.gdx.utils.Array;


/**
 * LibGDX-based World implementation that serves as a Screen and container for Actors.
 * Uses LibGDX's rendering system and our custom collision management.
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
    
    // LibGDX rendering components
    private SpriteBatch batch;
    private BitmapFont font;
    private ShapeRenderer shapeRenderer;
    
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
        
        // Initialize collections
        this.allActors = new Array<Actor>();
        this.textLabels = new Array<TextLabel>();
        
        // Initialize collision system
        this.collisionChecker = new ColManager();
        this.collisionChecker.initialize(worldWidth, worldHeight, cellSize, !bounded);
        
        // Set default background
        this.backgroundColor = DEFAULT_BACKGROUND_COLOR.toLibGDXColor();
        
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
    }
    
    /**
     * Get the background color.
     */
    public Color getBackground() {
        // Convert LibGDX Color back to Greenfoot Color
        if (backgroundColor != null) {
            return Color.fromLibGDXColor(backgroundColor);
        }
        return Color.WHITE;
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
    
    // ================ World Lifecycle Methods ================
    
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
     * Act method called each simulation step.
     */
    public void act() {
        // Override in subclasses
    }
    
    /**
     * Request a repaint of the world.
     */
    public void repaint() {
        // In LibGDX, rendering happens automatically
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
        // Clear screen with background color
        Gdx.gl.glClearColor(backgroundColor.r, backgroundColor.g, backgroundColor.b, backgroundColor.a);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        
        // Begin batch rendering
        batch.begin();
        
        // Draw background texture if available
        if (hasBackgroundTexture && backgroundTexture != null) {
            batch.draw(backgroundTexture, 0, 0, getWidthInPixels(), getHeightInPixels());
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
        
        batch.end();
        
        // Update actors (act step)
        if (delta > 0) {
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
        // Handle screen resize if needed
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
        String className = getClass().getSimpleName().toLowerCase();
        try {
            if (Gdx.files.internal("images/" + className + ".png").exists()) {
                setBackground("images/" + className + ".png");
            }
        } catch (Exception e) {
            // Ignore and use default background
        }
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
     */
    private static class TextLabel {
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