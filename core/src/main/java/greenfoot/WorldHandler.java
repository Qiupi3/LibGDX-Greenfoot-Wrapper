package greenfoot;

public class WorldHandler {
    private static final WorldHandler instance = new WorldHandler();

    private World currentWorld;

    private WorldHandler() {
        // private constructor: singleton
    }

    public static WorldHandler getInstance() {
        return instance;
    }

    public World getWorld() {
        return currentWorld;
    }

    public void setWorld(World world) {
        this.currentWorld = world;
    }

    public void objectAddedToWorld(Actor actor) {
        // For now, no-op. In real Greenfoot this triggers repaint/update
    }

    public void objectRemovedFromWorld(Actor actor) {
        // For now, no-op
    }

    public void setInitialisingWorld(World world) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setInitialisingWorld'");
    }

    public void repaintAndWait() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'repaintAndWait'");
    }
}
