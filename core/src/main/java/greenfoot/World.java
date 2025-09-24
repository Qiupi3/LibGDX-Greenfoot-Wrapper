package greenfoot;

import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.util.List;

public abstract class World implements Screen {
    protected int width;
    protected int height;
    protected int cellSize;

    protected SpriteBatch batch;
    protected Texture background;

    protected List<Actor> actors = new ArrayList<>();

    public World(int width, int height, int cellSize) {
        this.width = width;
        this.height = height;
        this.cellSize = cellSize;
        this.batch = new SpriteBatch();
    }

    // ---------------- Core Greenfoot API ----------------

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

    public void setBackground(String filename) {
        if (background != null) background.dispose();
        background = new Texture(Gdx.files.internal(filename));
    }

    public Texture getBackground() {
        return background;
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
