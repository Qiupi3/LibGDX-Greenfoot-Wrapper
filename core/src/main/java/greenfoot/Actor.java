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

import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.MathUtils;
import greenfoot.platforms.ActorDelegate;

/**
 * Actor class implementation using LibGDX methods.
 * This class represents game objects that can be placed in a World.
 * 
 * This class re-implements greenfoot.Actor to provide a LibGDX backend,
 * mainly to allow Greenfoot projects to run on LibGDX (especially to export into
 * mobile devices and other platforms).
 * 
 * Inspired by the original Greenfoot project (GPLv2+ with Classpath Exception).
 * Read the original documentation at https://www.greenfoot.org/files/javadoc/greenfoot/Actor.html
 * 
 * @author Poul Henriksen (Original Greenfoot version's author)
 * 
 * @modified-by Qiupi3 (LibGDX wrapper implementation)
 * @modified-by DavidsonRafaelK
 * @version 1.0
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

    // The GreenfootImage last passed to setImage(), kept so getImage() returns
    // the same instance and so render() can read its transparency setting.
    private GreenfootImage currentImage;
    
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
    
    // Drag visual feedback
    private boolean isBeingDragged = false;
    private float dragAlpha = 1.0f;
    private float originalAlpha = 1.0f;
    
    // Delegate for platform-specific operations
    private static ActorDelegate delegate;
    
    /**
     * Constructor - creates a new Actor with default settings
     */
    public Actor() {
        mySequenceNumber = sequenceNumber++;
        position = new Vector2();
        velocity = new Vector2();
        bounds = new Rectangle();
        
        // Try to load class-specific image
        Texture classTexture = getTexture();
        if (classTexture != null) {
            setTexture(classTexture);
        } else {
            // Use default texture if available
            if (defaultTexture != null) {
                setTexture(defaultTexture);
            }
        }
    }
    
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
        if (currentImage != null) {
            return currentImage;
        }
        if (texture != null) {
            // Create a GreenfootImage from the texture using the internal constructor
            currentImage = new GreenfootImage(texture);
            return currentImage;
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
        // Route through the GreenfootImage(filename) path so image caching,
        // project-relative path resolution, and transparency tracking all apply.
        setImage(new GreenfootImage(filename));
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
        // Keep the reference so getImage() returns the same instance and
        // render() can read its transparency.
        currentImage = image;
    }

    /**
     * Set the texture of this actor.
     */
    public void setTexture(Texture texture) {
        // IMPORTANT: Don't dispose textures here!
        // Textures should be managed by GreenfootImage, not by individual actors
        // Multiple actors might share the same texture, so disposing here causes
        // the OpenGL "texture unloadable" issue and black sprite rendering

        this.texture = texture;
        // A texture set directly (not via setImage(GreenfootImage)) is no longer
        // tracked against any GreenfootImage; setImage(GreenfootImage) restores
        // currentImage right after calling this method.
        this.currentImage = null;
        
        if (texture != null) {
            try {
                if (sprite == null) {
                    sprite = new Sprite(texture);
                } else {
                    // Sprite.setTexture() swaps only the texture: the UV region and the
                    // sprite's width/height stay at the previous image's values, so a
                    // differently sized image would keep drawing (and hit-testing) at the
                    // old size. Re-region and re-size the sprite to match.
                    sprite.setTexture(texture);
                    sprite.setRegion(texture);
                    sprite.setSize(texture.getWidth(), texture.getHeight());
                    sprite.setOriginCenter();
                }
                
                // Update sprite properties
                sprite.setRotation(rotation);
                updatePixelPosition();
            } catch (Exception e) {
                Gdx.app.log("Actor", "Failed to set sprite texture", e);
                sprite = null;
            }
        } else {
            sprite = null;
        }
        
        updateBounds();
    }

    
    /**
     * Check if this actor intersects with another actor.
     */
    public boolean intersects(Actor other) {
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
     * Note: dx and dy might be in pixel units (from image calculations) and need conversion to cell units.
     */
    protected Actor getOneObjectAtOffset(int dx, int dy, Class<?> cls) {
        failIfNotInWorld();
        
        int finalDx = dx;
        int finalDy = dy;
        
        if (world != null) {
            int cellSize = world.getCellSize();
            
            // When cellSize is NOT 1, we need to convert pixel offsets to cell offsets
            if (cellSize != 1) {
                // Normal Greenfoot case: convert pixel offsets to cell offsets
                if (Math.abs(dx) > cellSize || Math.abs(dy) > cellSize) {
                    finalDx = (int) Math.round((double) dx / cellSize);
                    finalDy = (int) Math.round((double) dy / cellSize);
                    
                    // Ensure we don't lose the direction for small offsets
                    if (finalDx == 0 && dx != 0) finalDx = dx > 0 ? 1 : -1;
                    if (finalDy == 0 && dy != 0) finalDy = dy > 0 ? 1 : -1;
                }
            }
            // When cellSize=1, use offsets directly as pixels (no conversion needed)
        }
        
        // Pass absolute target coordinates to getOneObjectAt
        return world.getOneObjectAt(this, x + finalDx, y + finalDy, cls);
    }
    
    /**
     * Get one object at a specific offset from this actor using a fixed range.
     * This is more reliable than getOneObjectAtOffset for combat systems where
     * the attack range should be consistent regardless of current animation frame size.
     * 
     * @param direction Direction multiplier (-1, 0, or 1) for each axis
     * @param range Fixed range in pixels for the attack
     * @param cls Class type to search for
     * @return First object found within range, or null
     */
    protected Actor getOneObjectAtOffsetWithRange(int directionX, int directionY, int range, Class<?> cls) {
        failIfNotInWorld();
        int dx = directionX * range;
        int dy = directionY * range;
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
    
    String dbgImageAlpha() {
        return currentImage == null ? "no-image" : String.valueOf(currentImage.getTransparency());
    }

    String dbgSpriteInfo() {
        if (sprite == null) return "null";
        return (int) sprite.getX() + "," + (int) sprite.getY() + " "
                + (int) sprite.getWidth() + "x" + (int) sprite.getHeight();
    }

    /**
     * Render this actor using the provided SpriteBatch.
     */
    public void render(SpriteBatch batch) {
        if (sprite != null && world != null) {
            try {
                // Pick up edits made in place through getImage(), and any texture the
                // image had to regenerate, so the sprite never draws a stale texture.
                if (currentImage != null) {
                    Texture current = currentImage.getTexture();
                    if (current != null && current != sprite.getTexture()) {
                        texture = current;
                        sprite.setTexture(current);
                        sprite.setRegion(current);
                        sprite.setSize(current.getWidth(), current.getHeight());
                        updatePixelPosition();
                    }
                }

                // Ensure texture is valid before drawing
                if (sprite.getTexture() != null) {
                    // Check if this actor is being dragged for visual feedback
                    updateDragVisuals();

                    // GreenfootImage.setTransparency() only stores a value; it must be
                    // applied here as the sprite's draw alpha or it has no visual effect.
                    float imageAlpha = currentImage != null ? currentImage.getTransparency() / 255f : 1f;

                    // Sprite.draw() carries its own vertex colour and ignores the
                    // batch colour, so the alpha has to go on the sprite itself.
                    float alpha = isBeingDragged ? dragAlpha * imageAlpha : imageAlpha;
                    float oldAlpha = sprite.getColor().a;
                    sprite.setAlpha(alpha);

                    if (isBeingDragged) {
                        // Draw slightly larger for drag effect
                        float oldScaleX = sprite.getScaleX();
                        float oldScaleY = sprite.getScaleY();
                        sprite.setScale(oldScaleX * 1.1f, oldScaleY * 1.1f);

                        sprite.draw(batch);

                        sprite.setScale(oldScaleX, oldScaleY);
                    } else {
                        sprite.draw(batch);
                    }

                    sprite.setAlpha(oldAlpha);
                }
            } catch (Exception e) {
                // Log the error but don't crash the rendering
                Gdx.app.log("Actor", "Error rendering actor: " + e.getMessage(), e);
            }
        }
    }
    
    /**
     * Update drag visual state based on current drag status.
     * Modified to avoid interfering with quick mouse clicks for attacks.
     */
    private void updateDragVisuals() {
        // Check if this actor is currently being dragged
        boolean wasDragged = isBeingDragged;
        Actor currentDraggedObject = greenfoot.MouseInfo.getDraggedObject();
        
        // Only consider this actor as being dragged if:
        // 1. It's actually the dragged object
        // 2. And the mouse has been pressed for a reasonable drag duration (avoid quick clicks)
        boolean actuallyDragging = (currentDraggedObject == this) && greenfoot.MouseInfo.isDragging();
        
        isBeingDragged = actuallyDragging;
        
        if (isBeingDragged && !wasDragged) {
            // Just started being dragged
            originalAlpha = 1.0f;
            dragAlpha = 0.7f; // Semi-transparent
        } else if (!isBeingDragged && wasDragged) {
            // Just stopped being dragged
            dragAlpha = originalAlpha;
        }
    }
    
    /**
     * Set whether this actor is being dragged.
     */
    public void setBeingDragged(boolean dragged) {
        this.isBeingDragged = dragged;
        if (dragged) {
            this.originalAlpha = 1.0f;
            this.dragAlpha = 0.7f; // 70% opacity when dragging
        } else {
            this.dragAlpha = this.originalAlpha;
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
     * Fallback hit box (in pixels) for an actor that genuinely has no image at all.
     * Kept small so an image-less actor is not clickable over a large invisible area.
     */
    private static final float DEFAULT_HIT_BOX = 8f;

    /**
     * The size of this actor's image in pixels, as the bounding rectangle of the
     * image once the actor's rotation is applied - which is what Greenfoot uses
     * both for mouse hit testing and for getObjectsAt().
     *
     * @return a two element array: {width, height}
     */
    float[] getHitBoxSize() {
        float width = 0f;
        float height = 0f;

        // setTexture()/render() keep the texture field pointing at the texture of the
        // actor's current GreenfootImage, so its size is the drawn size of the actor.
        // (getImage() is avoided here: it fabricates a 32x32 image, and can build a
        // whole Pixmap, for an actor that has none.)
        if (texture != null) {
            width = texture.getWidth();
            height = texture.getHeight();
        } else if (sprite != null) {
            width = Math.abs(sprite.getWidth());
            height = Math.abs(sprite.getHeight());
        }
        if (sprite != null && width > 0f && height > 0f) {
            width *= Math.abs(sprite.getScaleX());
            height *= Math.abs(sprite.getScaleY());
        }
        if (width <= 0f || height <= 0f) {
            width = DEFAULT_HIT_BOX;
            height = DEFAULT_HIT_BOX;
        }

        if (rotation % 180 != 0) {
            double radians = Math.toRadians(rotation);
            float cos = (float) Math.abs(Math.cos(radians));
            float sin = (float) Math.abs(Math.sin(radians));
            float rotatedWidth = width * cos + height * sin;
            float rotatedHeight = width * sin + height * cos;
            width = rotatedWidth;
            height = rotatedHeight;
        }

        return new float[] { width, height };
    }

    /**
     * Whether the given point falls inside this actor's image. The point is in
     * Greenfoot pixel coordinates: origin top-left, y growing downwards, which is
     * what both the unprojected mouse position and World.getObjectsAt() work in.
     *
     * @param pixelX x pixel coordinate, Greenfoot space
     * @param pixelY y pixel coordinate, Greenfoot space
     * @return true if the point is on this actor's image
     */
    boolean containsWorldPixel(float pixelX, float pixelY) {
        if (world == null) {
            return false;
        }

        int cellSize = world.getCellSize();
        float centerX = x * cellSize + cellSize / 2f;
        float centerY = y * cellSize + cellSize / 2f;

        float[] hitBox = getHitBoxSize();
        return Math.abs(pixelX - centerX) <= hitBox[0] / 2f
            && Math.abs(pixelY - centerY) <= hitBox[1] / 2f;
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
     * Note: Expects pixel coordinates in Greenfoot coordinate system (Y increases downward).
     */
    protected void setLocationInPixels(int x, int y) {
        if (world != null) {
            int cellSize = world.getCellSize();
            int cellX = x / cellSize;
            int cellY = y / cellSize;
            
            // Update cell coordinates directly (bypass setLocation to avoid updatePixelPosition override)
            int oldX = this.x;
            int oldY = this.y;
            
            // Apply world bounds if bounded
            if (world.isBounded()) {
                this.x = MathUtils.clamp(cellX, 0, world.getWidth() - 1);
                this.y = MathUtils.clamp(cellY, 0, world.getHeight() - 1);
            } else {
                this.x = cellX;
                this.y = cellY;
            }
            
            // Store LibGDX coordinates in position field for rendering (with Y-flip)
            float libgdxPixelX = x;
            float libgdxPixelY = world.getHeightInPixels() - y;
            position.set(libgdxPixelX, libgdxPixelY);
            
            // Update sprite position
            if (sprite != null) {
                sprite.setCenter(position.x, position.y);
            }
            
            // Notify world of location change if needed
            if (this.x != oldX || this.y != oldY) {
                locationChanged(oldX, oldY);
            }
        } else {
            position.set(x, y);
        }
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
        // Do NOT dispose the texture here. It belongs to the GreenfootImage that
        // produced it, and that image is regularly shared - static image fields and
        // the GreenfootImage cache hand the same texture to other actors and worlds.
        // Disposing it left those images pointing at a dead GL texture, which draws
        // as a black rectangle. Same reasoning as setTexture().
        texture = null;
        sprite = null;
        currentImage = null;
    }
}