package greenfoot.collision.ibsp;

/**
 * Simple rectangle class for collision detection compatibility.
 * This is a minimal implementation to support ActorVisitor requirements.
 */
public class Rect {
    private final int x, y, width, height;
    
    public Rect(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
    
    public int getX() {
        return x;
    }
    
    public int getY() {
        return y;
    }
    
    public int getWidth() {
        return width;
    }
    
    public int getHeight() {
        return height;
    }
    
    public int getRight() {
        return x + width;
    }
    
    public int getTop() {
        return y + height;
    }
    
    public boolean contains(int px, int py) {
        return px >= x && px <= getRight() && py >= y && py <= getTop();
    }
    
    @Override
    public String toString() {
        return "Rect[x=" + x + ",y=" + y + ",w=" + width + ",h=" + height + "]";
    }
}