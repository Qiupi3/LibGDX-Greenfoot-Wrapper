package greenfoot;

import java.util.List;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * LibGDX-based collision checker interface for managing actor collisions and spatial queries.
 */
public interface CollisionChecker {
    
    /**
     * Initialize the collision checker with world parameters.
     */
    public void initialize(int worldWidth, int worldHeight, int cellSize, boolean wrap);

    /**
     * Add an actor to the collision system.
     */
    public void addObject(Actor actor);
    
    /**
     * Remove an actor from the collision system.
     */
    public void removeObject(Actor object);

    /**
     * Update an actor's location in the collision system.
     */
    public void updateObjectLocation(Actor object, int oldX, int oldY);

    /**
     * Update an actor's size in the collision system.
     */
    public void updateObjectSize(Actor object);

    /**
     * Get all objects of the specified type at a grid location.
     */
    public <T extends Actor> List<T> getObjectsAt(int x, int y, Class<T> cls);

    /**
     * Get all objects of the specified type that intersect with the given actor.
     */
    public <T extends Actor> List<T> getIntersectingObjects(Actor actor, Class<T> cls);
    
    /**
     * Get all objects of the specified type within a radius of a point.
     */
    public <T extends Actor> List<T> getObjectsInRange(int x, int y, int r, Class<T> cls);
    
    /**
     * Get all neighboring objects of the specified type within a distance.
     */
    public <T extends Actor> List<T> getNeighbours(Actor actor, int distance, boolean diag, Class<T> cls);
    
    /**
     * Get all objects of the specified type in a direction from a point.
     */
    public <T extends Actor> List<T> getObjectsInDirection(int x, int y, int angle, int length, Class<T> cls);
    
    /**
     * Get all objects of the specified type in the world.
     */
    public <T extends Actor> List<T> getObjects(Class<T> cls);

    /**
     * Get all actors in the world as a list.
     */
    public List<Actor> getObjectsList();

    /**
     * Start a new sequence (called each simulation step).
     */
    public void startSequence();

    /**
     * Get one object of the specified type at an offset from the given actor.
     */
    public <T extends Actor> T getOneObjectAt(Actor object, int dx, int dy, Class<T> cls);

    /**
     * Get one object of the specified type that intersects with the given actor.
     */
    public <T extends Actor> T getOneIntersectingObject(Actor object, Class<T> cls);

    /**
     * Render debug information using LibGDX SpriteBatch (optional).
     */
    public void renderDebug(SpriteBatch batch);
}
