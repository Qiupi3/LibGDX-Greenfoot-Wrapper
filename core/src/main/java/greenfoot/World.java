package greenfoot;

import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.util.List;

public abstract class World implements Screen {
    private static final greenfoot.Color DEFAULT_BACKGROUND_COLOR = greenfoot.Color.WHITE;
    
    private CollisionChecker collisionChecker = new ColManager();
    
    private TreeActorSet objectsDisordered = new TreeActorSet(); 
    private TreeActorSet objectsInPaintOrder;    
    private TreeActorSet objectsInActOrder;
    private final List<TextLabel> textLabels = new ArrayList<>();
    
    private GreenfootImage backgroundImage;
    private boolean backgroundIsClassImage = true;

    private final SpriteBatch batch;
    private final BitmapFont font; // for showText()
    
    private final int width;
    private final int height;
    private final int cellSize;
    private boolean isBounded;
    
    public World(int worldWidth, int worldHeight, int cellSize) {
        this(worldWidth, worldHeight, cellSize, true);
    }
    
    public World(int worldWidth, int worldHeight, int cellSize, boolean bounded) {
        this.width = worldWidth;
        this.height = worldHeight;
        this.cellSize = cellSize;
        collisionChecker.initialize(worldWidth, worldHeight, cellSize, false);
        this.isBounded = bounded;
        this.batch = new SpriteBatch();
        this.font = new BitmapFont();
        
        backgroundIsClassImage = true;
        setBackground(getClassImage());
        
        //Implement collisionChecker
        
        final WorldHandler wHandler = WorldHandler.getInstance();
        if(wHandler != null) { // will be null when running unit tests.
            wHandler.setInitialisingWorld(this);
        }
    }

    // ---------------- Core Greenfoot API ----------------

    final public void setBackground(GreenfootImage image) {
        if (image != null) {
            int imgWidth = image.getWidth();
            int imgHeight = image.getHeight();
            int worldWidth = getWidthInPixels();
            int worldHeight = getHeightInPixels();
            boolean tile = imgWidth < worldWidth || imgHeight < worldHeight;

            if (tile) {
                backgroundIsClassImage = false;

                // Create a new blank image with default background color
                GreenfootImage tiled = new GreenfootImage(worldWidth, worldHeight, DEFAULT_BACKGROUND_COLOR);

                // Tile the image across
                for (int x = 0; x < worldWidth; x += imgWidth) {
                    for (int y = 0; y < worldHeight; y += imgHeight) {
                        tiled.drawImage(image, x, y);
                    }
                }
                backgroundImage = tiled;
            } else {
                // Just use the provided image directly
                backgroundImage = image;
            }
        } else {
            backgroundIsClassImage = false;
            backgroundImage = new GreenfootImage(getWidthInPixels(), getHeightInPixels(), DEFAULT_BACKGROUND_COLOR);
        }
    }

    final public void setBackground(String filename) throws IllegalArgumentException {
        GreenfootImage bg = new GreenfootImage(filename);
        setBackground(bg);
    }

    public Texture getBackground() {
        return background;
    }
    
    // Greenfoot-like lifecycle methods
    public abstract void act();

    public void addObject(Actor actor, int x, int y) {
        actor.setWorld(this);
        actor.setLocation(x, y);
        actors.add(actor);
    }

    public void removeObject(Actor actor) {
        actors.remove(actor);
        actor.setWorld(null);
    }

    public List<Actor> getObjects(Class cls) {
        List<Actor> result = new ArrayList<>();
        for (Actor a : actors) {
            if (cls.isAssignableFrom(a.getClass())) {
                result.add(a);
            }
        }
        return result;
    }

    public List<Actor> getObjectsAt(int x, int y, Class cls) {
        List<Actor> result = new ArrayList<>();
        for (Actor a : actors) {
            if (a.getX() == x && a.getY() == y &&
                cls.isAssignableFrom(a.getClass())) {
                result.add(a);
            }
        }
        return result;
    }

    public List<Actor> getObjectsInRange(int x, int y, int range, Class cls) {
        List<Actor> result = new ArrayList<>();
        for (Actor a : actors) {
            float dx = a.getX() - x;
            float dy = a.getY() - y;
            if (Math.sqrt(dx*dx + dy*dy) <= range &&
                cls.isAssignableFrom(a.getClass())) {
                result.add(a);
            }
        }
        return result;
    }

    // ---------------- LibGDX Screen lifecycle ----------------

    @Override
    public void render(float delta) {
        act(); // Greenfoot world update

        batch.begin();
        if (background != null) {
            batch.draw(background, 0, 0, width * cellSize, height * cellSize);
        }
        for (Actor a : actors) {
            a.draw(batch);
        }
        batch.end();
    }

    @Override public void show() {}
    @Override public void resize(int width, int height) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() {
        batch.dispose();
        if (background != null) background.dispose();
    }

    // ---------------- Getters ----------------

    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getCellSize() { return cellSize; }
}
