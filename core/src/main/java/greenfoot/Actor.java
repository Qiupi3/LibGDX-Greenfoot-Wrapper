package greenfoot;

import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.MathUtils;

/**
 * Actor class implementation using LibGDX methods.
 * This class represents game objects that can be placed in a World.
 */
public class Actor {
    private static final String ACTOR_NEVER_IN_WORLD = "Actor not in world. You must add it to a world before you can call this method.";
    private static final String ACTOR_LEFT_WORLD = "Actor has been removed from the world.";

    private static int sequenceNumber = 0;

    // Position in world grid coordinates
    protected int x, y;
    
    // LibGDX sprite for rendering and positioning
    protected Sprite sprite;
    protected Texture texture;
    
    // Position in pixel coordinates
    protected Vector2 position;
    protected Vector2 velocity;
    
    private int mySequenceNumber;
    
    // Rotation in degrees (0-359)
    protected int rotation = 0;

    // Reference to the world this actor belongs to
    protected World world;
    
    // Collision detection rectangle
    protected Rectangle bounds;
    
    // Tracking for world removal
    private Throwable lastWorldRemovalTrace = null;
    
    // Sleep mechanism
    private int sleepingFor = 0;
    
    // Default texture for actors without specific images
    private static Texture defaultTexture;
    
    // Additional data storage for ActorVisitor compatibility
    private Object data;
    
    // Paint sequence tracking
    private int lastPaintSeqNum = 0;
    
    // Delegate for platform-specific operations
    private static greenfoot.platforms.ActorDelegate delegate;
    
    /**
     * Constructor - creates a new Actor with default settings
     */
    public Actor() {
        mySequenceNumber = sequenceNumber++;
        position = new Vector2();
        velocity = new Vector2();
        bounds = new Rectangle();
        
        // Try to load class-specific image
        Texture classTexture = getClassTexture();
        if (classTexture != null) {
            setTexture(classTexture);
        } else {
            // Use default texture if available
            if (defaultTexture != null) {
                setTexture(defaultTexture);
            }
        }
    }
        // original.
    
    /**
     * Act method - called each simulation step. Override in subclasses.
     */
    public void act() {
        // Override in subclasses
    }
    
    /**
     * Get the X coordinate of this actor in the world grid.
     */
    public int getX() throws IllegalStateException {
        failIfNotInWorld();
        return x;
    }
    
    /**
     * Get the Y coordinate of this actor in the world grid.
     */
    public int getY() throws IllegalStateException {
        failIfNotInWorld();
        return y;
    }
    
    /**
     * Get the rotation of this actor in degrees (0-359).
     */
    public int getRotation() {
        return rotation;
    }
    
    /**
     * Set the rotation of this actor. Automatically normalizes to 0-359 range.
     */
    public void setRotation(int rotation) {
        // Normalize rotation to 0-359 range
        rotation = rotation % 360;
        if (rotation < 0) {
            rotation += 360;
        }
        
        if (this.rotation != rotation) {
            this.rotation = rotation;
            
            // Update sprite rotation if available
            if (sprite != null) {
                sprite.setRotation(rotation);
            }
            
            updateBounds();
        }
    }
    
    /**
     * Turn to face towards a specific point in the world.
     */
    public void turnTowards(int x, int y) {
        double angle = Math.atan2(y - this.y, x - this.x);
        setRotation((int) Math.toDegrees(angle));
    }
    
    /**
     * Check if this actor is at the edge of the world.
     */
    public boolean isAtEdge() {
        failIfNotInWorld();
        return (x <= 0 || y <= 0 || x >= world.getWidth() - 1 || y >= world.getHeight() - 1);
    }
    
    /**
     * Set the location of this actor in the world grid.
     */
    public void setLocation(int x, int y) {
        if (world != null) {
            int oldX = this.x;
            int oldY = this.y;
            
            // Apply world bounds if bounded
            if (world.isBounded()) {
                this.x = MathUtils.clamp(x, 0, world.getWidth() - 1);
                this.y = MathUtils.clamp(y, 0, world.getHeight() - 1);
            } else {
                this.x = x;
                this.y = y;
            }
            
            // Update pixel position
            updatePixelPosition();
            
            // Update bounds
            updateBounds();
            
            // Notify world of location change
            if (this.x != oldX || this.y != oldY) {
                locationChanged(oldX, oldY);
            }
        } else {
            this.x = x;
            this.y = y;
        }
    }
    
    /**
     * Move forward by the specified distance in the current direction.
     */
    public void move(int distance) {
        double radians = Math.toRadians(rotation);
        
        // Calculate movement delta
        int dx = (int) Math.round(Math.cos(radians) * distance);
        int dy = (int) Math.round(Math.sin(radians) * distance);
        
        setLocation(x + dx, y + dy);
    }
    
    /**
     * Turn by the specified amount (in degrees).
     */
    public void turn(int amount) {
        setRotation(rotation + amount);
    }
    
    /**
     * Set this actor to sleep for the specified number of steps.
     */
    public void sleepFor(int sleepFor) {
        this.sleepingFor = Math.max(0, sleepFor);
    }
    
    /**
     * Get the world that contains this actor.
     */
    public World getWorld() {
        return world;
    }
    
    /**
     * Get the world cast to a specific type.
     */
    public <W> W getWorldOfType(Class<W> worldClass) {
        return worldClass.cast(world);
    }
    
    /**
     * Called when this actor is added to a world.
     */
    protected void addedToWorld(World world) {
        // Override in subclasses if needed
    }
    
    /**
     * Internal method to add this actor to a world at specific coordinates.
     */
    protected void addToWorld(int x, int y, World world) {
        this.world = world;
        setLocation(x, y);
        addedToWorld(world);
    }
    /**
     * Get the current image of this actor.
     */
    public GreenfootImage getImage() {
        if (texture != null) {
            // Create a GreenfootImage from the texture
            GreenfootImage image = new GreenfootImage(texture.getWidth(), texture.getHeight());
            // TODO: Convert texture data to GreenfootImage properly
            return image;
        }
        return new GreenfootImage(32, 32); // Default size
    }
    
    /**
     * Get the current texture/image of this actor.
     */
    public Texture getTexture() {
        return texture;
    }
    
    /**
     * Set the image of this actor from a filename.
     */
    public void setImage(String filename) throws IllegalArgumentException {
        try {
            Texture newTexture = new Texture(Gdx.files.internal(filename));
            setTexture(newTexture);
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not load image: " + filename, e);
        }
    }

    /**
     * Set the image of this actor from a GreenfootImage.
     */
    public void setImage(GreenfootImage image) {
        if (image == null) {
            setTexture(null);
            return;
        }
        // Get the LibGDX texture from the GreenfootImage
        setTexture(image.getTexture());
    }
    
    /**
     * Set the texture of this actor.
     */
    public void setTexture(Texture texture) {
        // Dispose old texture if we created it
        if (this.texture != null && this.texture != defaultTexture) {
            this.texture.dispose();
        }
        
        this.texture = texture;
        
        if (texture != null) {
            if (sprite == null) {
                sprite = new Sprite(texture);
            } else {
                sprite.setTexture(texture);
            }
            
            // Update sprite properties
            sprite.setRotation(rotation);
            updatePixelPosition();
        } else {
            sprite = null;
        }
        
        updateBounds();
    }

    
    /**
     * Check if this actor intersects with another actor.
     */
    protected boolean intersects(Actor other) {
        failIfNotInWorld();
        
        if (other == null || other.world == null) {
            return false;
        }
        
        // Use LibGDX Rectangle.overlaps for collision detection
        return bounds.overlaps(other.bounds);
    }
    
    /**
     * Get all actors of the specified class within a certain distance.
     */
    protected <A> List<A> getNeighbours(int distance, boolean diagonal, Class<A> cls) {
        failIfNotInWorld();
        return world.getNeighbours(this, distance, diagonal, cls);
    }
    
    /**
     * Get all objects at a specific offset from this actor.
     */
    protected <A> List<A> getObjectsAtOffset(int dx, int dy, Class<A> cls) {
        failIfNotInWorld();
        return world.getObjectsAt(x + dx, y + dy, cls);
    }
    
    /**
     * Get one object at a specific offset from this actor.
     */
    protected Actor getOneObjectAtOffset(int dx, int dy, Class<?> cls) {
        failIfNotInWorld();
        return world.getOneObjectAt(this, x + dx, y + dy, cls);
    }
    
    /**
     * Get all objects within a specified radius of this actor.
     */
    protected <A> List<A> getObjectsInRange(int radius, Class<A> cls) {
        failIfNotInWorld();
        List<A> inRange = world.getObjectsInRange(x, y, radius, cls);
        inRange.remove(this);
        return inRange;
    }
    
    /**
     * Get all objects that intersect with this actor.
     */
    protected <A> List<A> getIntersectingObjects(Class<A> cls) {
        failIfNotInWorld();
        List<A> intersecting = world.getIntersectingObjects(this, cls);
        intersecting.remove(this);
        return intersecting;
    }
    
    /**
     * Get one object that intersects with this actor.
     */
    protected Actor getOneIntersectingObject(Class<?> cls) {
        failIfNotInWorld();
        return world.getOneIntersectingObject(this, cls);
    }
    
    /**
     * Check if this actor is touching any object of the specified class.
     */
    protected boolean isTouching(Class<?> cls) {
        failIfNotInWorld();
        return getOneIntersectingObject(cls) != null;
    }
    
    /**
     * Remove all objects of the specified class that are touching this actor.
     */
    protected void removeTouching(Class<?> cls) {
        failIfNotInWorld();
        Actor touchingActor = (Actor) getOneIntersectingObject(cls);
        if (touchingActor != null) {
            world.removeObject(touchingActor);
        }
    }
    
    /**
     * Render this actor using the provided SpriteBatch.
     */
    public void render(SpriteBatch batch) {
        if (sprite != null && world != null) {
            sprite.draw(batch);
        }
    }
    
    /**
     * Get the bounding rectangle of this actor for collision detection.
     */
    public Rectangle getBounds() {
        return bounds;
    }
    
    /**
     * Check if a point is contained within this actor's bounds.
     */
    public boolean containsPoint(float px, float py) {
        return bounds.contains(px, py);
    }
    
    /**
     * Internal method to check if sleeping time has expired.
     */
    public boolean isSleeping() {
        if (sleepingFor > 0) {
            sleepingFor--;
            return true;
        }
        return false;
    }
    
    /**
     * Get pixel position X coordinate.
     */
    public float getPixelX() {
        return position.x;
    }
    
    /**
     * Get pixel position Y coordinate.
     */
    public float getPixelY() {
        return position.y;
    }
    
    /**
     * Internal method to update pixel position based on grid coordinates.
     */
    private void updatePixelPosition() {
        if (world != null) {
            int cellSize = world.getCellSize();
            // Convert from Greenfoot coordinates (top-left origin, Y down) to LibGDX coordinates (bottom-left origin, Y up)
            float pixelX = x * cellSize + cellSize / 2f;
            float pixelY = world.getHeightInPixels() - (y * cellSize + cellSize / 2f);
            position.set(pixelX, pixelY);
            
            if (sprite != null) {
                sprite.setCenter(position.x, position.y);
            }
        }
    }
    
    /**
     * Internal method to update collision bounds.
     */
    private void updateBounds() {
        if (sprite != null) {
            bounds.set(sprite.getX(), sprite.getY(), sprite.getWidth(), sprite.getHeight());
        } else {
            // Default point collision
            bounds.set(position.x, position.y, 1, 1);
        }
    }
    
    /**
     * Internal method called when location changes.
     */
    private void locationChanged(int oldX, int oldY) {
        if (world != null) {
            world.updateObjectLocation(this, oldX, oldY);
        }
    }
    
    /**
     * Internal method to throw exception if not in world.
     */
    private void failIfNotInWorld() {
        if (world == null) {
            if (lastWorldRemovalTrace == null) {
                throw new IllegalStateException(ACTOR_NEVER_IN_WORLD);
            } else {
                throw new IllegalStateException(ACTOR_LEFT_WORLD, lastWorldRemovalTrace);
            }
        }
    }
    
    /**
     * Internal method to mark when removed from world.
     */
    protected void setRemovedFromWorld(Throwable trace) {
        world = null;
        lastWorldRemovalTrace = trace;
    }
    
    /**
     * Try to load a texture specific to this class.
     */
    private Texture getClassTexture() {
        String className = getClass().getSimpleName();
        
        // Try to get image from project.greenfoot configuration
        try {
            String imageFileName = getImageFromProjectFile(className);
            if (imageFileName != null) {
                // Try to load the specified image
                if (Gdx.files.internal("tes/images/" + imageFileName).exists()) {
                    return new Texture(Gdx.files.internal("tes/images/" + imageFileName));
                }
            }
        } catch (Exception e) {
            // Continue to fallback method
        }
        
        // Fallback: try to load image with class name
        try {
            String classNameLower = className.toLowerCase();
            if (Gdx.files.internal("tes/images/" + classNameLower + ".png").exists()) {
                return new Texture(Gdx.files.internal("tes/images/" + classNameLower + ".png"));
            }
        } catch (Exception e) {
            // Ignore and continue
        }
        return null;
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
     * Set the default texture for all actors.
     */
    public static void setDefaultTexture(Texture texture) {
        defaultTexture = texture;
    }
    
    /**
     * Get the sequence number of this actor.
     */
    public int getSequenceNumber() {
        return mySequenceNumber;
    }
    
    /**
     * Set location in pixel coordinates (for dragging operations).
     */
    protected void setLocationInPixels(int x, int y) {
        if (world != null) {
            int cellSize = world.getCellSize();
            int cellX = x / cellSize;
            int cellY = y / cellSize;
            setLocation(cellX, cellY);
        }
        position.set(x, y);
        updateBounds();
    }
    
    /**
     * Check if a point is contained within this actor's bounds (int version for ActorVisitor).
     */
    public boolean containsPoint(int px, int py) {
        return containsPoint((float)px, (float)py);
    }
    
    /**
     * Get bounding rectangle for collision detection.
     */
    public greenfoot.collision.ibsp.Rect getBoundingRect() {
        // Convert LibGDX Rectangle to greenfoot Rect
        return new greenfoot.collision.ibsp.Rect((int)bounds.x, (int)bounds.y, (int)bounds.width, (int)bounds.height);
    }
    
    /**
     * Convert cell coordinate to pixel coordinate.
     */
    protected int toPixel(int cellCoordinate) {
        if (world != null) {
            return cellCoordinate * world.getCellSize() + world.getCellSize() / 2;
        }
        return cellCoordinate * 32 + 16; // Default cell size
    }
    
    /**
     * Set arbitrary data on this actor.
     */
    public void setData(Object data) {
        this.data = data;
    }
    
    /**
     * Get arbitrary data from this actor.
     */
    public Object getData() {
        return data;
    }
    
    /**
     * Get the last paint sequence number.
     */
    public int getLastPaintSeqNum() {
        return lastPaintSeqNum;
    }
    
    /**
     * Set the last paint sequence number.
     */
    public void setLastPaintSeqNum(int seqNum) {
        this.lastPaintSeqNum = seqNum;
    }
    
    /**
     * Get how many steps this actor is sleeping for.
     */
    public int getSleepingFor() {
        return sleepingFor;
    }
    
    /**
     * Set how many steps this actor should sleep for.
     */
    public void setSleepingFor(int sleepFor) {
        this.sleepingFor = sleepFor;
    }
    
    /**
     * Static reference to Greenfoot logo image.
     */
    public static GreenfootImage greenfootImage;
    
    /**
     * Set the actor delegate for platform-specific operations.
     */
    public static void setDelegate(greenfoot.platforms.ActorDelegate delegate) {
        // Store delegate for future use
        Actor.delegate = delegate;
    }
    
    /**
     * Get the current delegate.
     */
    public static greenfoot.platforms.ActorDelegate getDelegate() {
        return delegate;
    }
    
    /**
     * Clean up resources when actor is destroyed.
     */
    public void dispose() {
        if (texture != null && texture != defaultTexture) {
            texture.dispose();
        }
    }
}