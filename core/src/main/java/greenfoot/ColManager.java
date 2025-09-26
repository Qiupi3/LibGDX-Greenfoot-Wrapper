package greenfoot;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectSet;
import com.badlogic.gdx.utils.Pool;

import java.util.ArrayList;
import java.util.List;

/**
 * LibGDX-based collision manager that handles spatial partitioning and collision detection
 * using LibGDX's optimized data structures and math library.
 */
public class ColManager implements CollisionChecker {
    
    // World properties
    private int worldWidth;
    private int worldHeight;
    private int cellSize;
    private boolean wrap;
    
    // Spatial hash grid for efficient collision detection
    private int gridWidth;
    private int gridHeight;
    private int gridCellSize = 64; // Size of each grid cell in pixels
    
    // Storage for all actors
    private Array<Actor> allActors;
    private ObjectMap<Class<? extends Actor>, Array<Actor>> actorsByClass;
    
    // Spatial hash grid - each cell contains a list of actors
    private Array<Actor>[][] spatialGrid;
    
    // Temporary collections for reuse (to avoid garbage collection)
    private Array<Actor> tempActors;
    private Rectangle tempRect1;
    
    // Pool for temporary arrays
    private Pool<Array<Actor>> arrayPool;
    
    public ColManager() {
        allActors = new Array<Actor>();
        actorsByClass = new ObjectMap<Class<? extends Actor>, Array<Actor>>();
        tempActors = new Array<Actor>();
        tempRect1 = new Rectangle();
        
        // Pool for temporary arrays to avoid garbage collection
        arrayPool = new Pool<Array<Actor>>() {
            @Override
            protected Array<Actor> newObject() {
                return new Array<Actor>();
            }
        };
    }

    @Override
    @SuppressWarnings("unchecked")
    public void initialize(int worldWidth, int worldHeight, int cellSize, boolean wrap) {
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
        this.cellSize = cellSize;
        this.wrap = wrap;
        
        // Calculate grid dimensions
        int pixelWorldWidth = worldWidth * cellSize;
        int pixelWorldHeight = worldHeight * cellSize;
        
        gridWidth = (pixelWorldWidth / gridCellSize) + 1;
        gridHeight = (pixelWorldHeight / gridCellSize) + 1;
        
        // Initialize spatial grid
        spatialGrid = new Array[gridWidth][gridHeight];
        for (int x = 0; x < gridWidth; x++) {
            for (int y = 0; y < gridHeight; y++) {
                spatialGrid[x][y] = new Array<Actor>();
            }
        }
    }

    @Override
    public void addObject(Actor actor) {
        if (actor == null) return;
        
        // Add to main list
        if (!allActors.contains(actor, true)) {
            allActors.add(actor);
        }
        
        // Add to class-based storage
        Class<? extends Actor> actorClass = actor.getClass();
        Array<Actor> classActors = actorsByClass.get(actorClass);
        if (classActors == null) {
            classActors = new Array<Actor>();
            actorsByClass.put(actorClass, classActors);
        }
        if (!classActors.contains(actor, true)) {
            classActors.add(actor);
        }
        
        // Add to spatial grid
        addToSpatialGrid(actor);
    }

    @Override
    public void removeObject(Actor actor) {
        if (actor == null) return;
        
        // Remove from main list
        allActors.removeValue(actor, true);
        
        // Remove from class-based storage
        Class<? extends Actor> actorClass = actor.getClass();
        Array<Actor> classActors = actorsByClass.get(actorClass);
        if (classActors != null) {
            classActors.removeValue(actor, true);
            if (classActors.size == 0) {
                actorsByClass.remove(actorClass);
            }
        }
        
        // Remove from spatial grid
        removeFromSpatialGrid(actor);
    }

    @Override
    public void updateObjectLocation(Actor object, int oldX, int oldY) {
        // Remove from old position in spatial grid
        removeFromSpatialGrid(object);
        // Add to new position
        addToSpatialGrid(object);
    }

    @Override
    public void updateObjectSize(Actor object) {
        // Size change might affect spatial grid placement
        updateObjectLocation(object, object.getX(), object.getY());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Actor> List<T> getObjectsAt(int x, int y, Class<T> cls) {
        List<T> result = new ArrayList<T>();
        
        // Convert world coordinates to pixel coordinates
        float pixelX = x * cellSize + cellSize / 2f;
        float pixelY = y * cellSize + cellSize / 2f;
        
        // Get grid cell
        int gridX = (int) (pixelX / gridCellSize);
        int gridY = (int) (pixelY / gridCellSize);
        
        if (gridX >= 0 && gridX < gridWidth && gridY >= 0 && gridY < gridHeight) {
            Array<Actor> cellActors = spatialGrid[gridX][gridY];
            
            for (Actor actor : cellActors) {
                if (cls.isAssignableFrom(actor.getClass())) {
                    // Check if actor is actually at this position
                    if (actor.getX() == x && actor.getY() == y) {
                        result.add((T) actor);
                    }
                }
            }
        }
        
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Actor> List<T> getIntersectingObjects(Actor actor, Class<T> cls) {
        List<T> result = new ArrayList<T>();
        
        if (actor == null || actor.getBounds() == null) {
            return result;
        }
        
        Rectangle actorBounds = actor.getBounds();
        Array<Actor> nearbyActors = getActorsInBounds(actorBounds);
        
        for (Actor other : nearbyActors) {
            if (other != actor && cls.isAssignableFrom(other.getClass())) {
                if (actor.intersects(other)) {
                    result.add((T) other);
                }
            }
        }
        
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Actor> List<T> getObjectsInRange(int x, int y, int radius, Class<T> cls) {
        List<T> result = new ArrayList<T>();
        
        // Convert to pixel coordinates
        float pixelX = x * cellSize + cellSize / 2f;
        float pixelY = y * cellSize + cellSize / 2f;
        float pixelRadius = radius * cellSize;
        
        // Create bounding rectangle for the range
        tempRect1.set(pixelX - pixelRadius, pixelY - pixelRadius, 
                     pixelRadius * 2, pixelRadius * 2);
        
        Array<Actor> nearbyActors = getActorsInBounds(tempRect1);
        
        for (Actor actor : nearbyActors) {
            if (cls.isAssignableFrom(actor.getClass())) {
                // Check distance
                float dx = actor.getPixelX() - pixelX;
                float dy = actor.getPixelY() - pixelY;
                float distanceSquared = dx * dx + dy * dy;
                
                if (distanceSquared <= pixelRadius * pixelRadius) {
                    result.add((T) actor);
                }
            }
        }
        
        return result;
    }

    @Override
    public <T extends Actor> List<T> getNeighbours(Actor actor, int distance, boolean diagonal, Class<T> cls) {
        List<T> result = new ArrayList<T>();
        
        if (actor == null) return result;
        
        int centerX = actor.getX();
        int centerY = actor.getY();
        
        for (int dx = -distance; dx <= distance; dx++) {
            for (int dy = -distance; dy <= distance; dy++) {
                if (dx == 0 && dy == 0) continue; // Skip center
                
                if (!diagonal && dx != 0 && dy != 0) continue; // Skip diagonals if not allowed
                
                int targetX = centerX + dx;
                int targetY = centerY + dy;
                
                List<T> actorsAtPosition = getObjectsAt(targetX, targetY, cls);
                result.addAll(actorsAtPosition);
            }
        }
        
        return result;
    }

    @Override
    public <T extends Actor> List<T> getObjectsInDirection(int x, int y, int angle, int length, Class<T> cls) {
        List<T> result = new ArrayList<T>();
        
        // Calculate end point
        double radians = Math.toRadians(angle);
        int endX = x + (int) Math.round(Math.cos(radians) * length);
        int endY = y + (int) Math.round(Math.sin(radians) * length);
        
        // Check all points along the line using Bresenham's algorithm
        int dx = Math.abs(endX - x);
        int dy = Math.abs(endY - y);
        int sx = x < endX ? 1 : -1;
        int sy = y < endY ? 1 : -1;
        int err = dx - dy;
        
        int currentX = x, currentY = y;
        
        while (true) {
            List<T> actorsAtPosition = getObjectsAt(currentX, currentY, cls);
            result.addAll(actorsAtPosition);
            
            if (currentX == endX && currentY == endY) break;
            
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                currentX += sx;
            }
            if (e2 < dx) {
                err += dx;
                currentY += sy;
            }
        }
        
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Actor> List<T> getObjects(Class<T> cls) {
        List<T> result = new ArrayList<T>();
        
        for (Actor actor : allActors) {
            if (cls.isAssignableFrom(actor.getClass())) {
                result.add((T) actor);
            }
        }
        
        return result;
    }

    @Override
    public List<Actor> getObjectsList() {
        List<Actor> result = new ArrayList<Actor>();
        for (int i = 0; i < allActors.size; i++) {
            result.add(allActors.get(i));
        }
        return result;
    }

    @Override
    public void startSequence() {
        // Called at the start of each simulation step
        // Could be used for optimization or cleanup if needed
    }

    @Override
    public <T extends Actor> T getOneObjectAt(Actor object, int dx, int dy, Class<T> cls) {
        if (object == null) return null;
        
        int targetX = object.getX() + dx;
        int targetY = object.getY() + dy;
        
        List<T> actors = getObjectsAt(targetX, targetY, cls);
        return actors.isEmpty() ? null : actors.get(0);
    }

    @Override
    public <T extends Actor> T getOneIntersectingObject(Actor object, Class<T> cls) {
        if (object == null) return null;
        
        List<T> intersecting = getIntersectingObjects(object, cls);
        return intersecting.isEmpty() ? null : intersecting.get(0);
    }

    @Override
    public void renderDebug(SpriteBatch batch) {
        // Optional: render debug information
        // Could draw grid lines, actor bounds, etc. using LibGDX's shape renderer
    }
    
    // Helper methods
    
    /**
     * Add actor to spatial grid based on its bounds.
     */
    private void addToSpatialGrid(Actor actor) {
        if (actor.getBounds() == null) return;
        
        Rectangle bounds = actor.getBounds();
        int minGridX = MathUtils.clamp((int) (bounds.x / gridCellSize), 0, gridWidth - 1);
        int maxGridX = MathUtils.clamp((int) ((bounds.x + bounds.width) / gridCellSize), 0, gridWidth - 1);
        int minGridY = MathUtils.clamp((int) (bounds.y / gridCellSize), 0, gridHeight - 1);
        int maxGridY = MathUtils.clamp((int) ((bounds.y + bounds.height) / gridCellSize), 0, gridHeight - 1);
        
        for (int gx = minGridX; gx <= maxGridX; gx++) {
            for (int gy = minGridY; gy <= maxGridY; gy++) {
                if (!spatialGrid[gx][gy].contains(actor, true)) {
                    spatialGrid[gx][gy].add(actor);
                }
            }
        }
    }
    
    /**
     * Remove actor from spatial grid.
     */
    private void removeFromSpatialGrid(Actor actor) {
        // Remove from all grid cells (inefficient but simple)
        for (int gx = 0; gx < gridWidth; gx++) {
            for (int gy = 0; gy < gridHeight; gy++) {
                spatialGrid[gx][gy].removeValue(actor, true);
            }
        }
    }
    
    /**
     * Get all actors that might intersect with the given bounds.
     */
    private Array<Actor> getActorsInBounds(Rectangle bounds) {
        tempActors.clear();
        
        int minGridX = MathUtils.clamp((int) (bounds.x / gridCellSize), 0, gridWidth - 1);
        int maxGridX = MathUtils.clamp((int) ((bounds.x + bounds.width) / gridCellSize), 0, gridWidth - 1);
        int minGridY = MathUtils.clamp((int) (bounds.y / gridCellSize), 0, gridHeight - 1);
        int maxGridY = MathUtils.clamp((int) ((bounds.y + bounds.height) / gridCellSize), 0, gridHeight - 1);
        
        ObjectSet<Actor> uniqueActors = new ObjectSet<Actor>();
        
        for (int gx = minGridX; gx <= maxGridX; gx++) {
            for (int gy = minGridY; gy <= maxGridY; gy++) {
                for (Actor actor : spatialGrid[gx][gy]) {
                    uniqueActors.add(actor);
                }
            }
        }
        
        for (Actor actor : uniqueActors) {
            tempActors.add(actor);
        }
        
        return tempActors;
    }

    public int getWorldWidth() {
        return worldWidth;
    }

    public int getWorldHeight() {
        return worldHeight;
    }

    public boolean isWrap() {
        return wrap;
    }

    public Pool<Array<Actor>> getArrayPool() {
        return arrayPool;
    }
}
