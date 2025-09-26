package id.qiupi3.greenfoot;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.ReflectionException;

import greenfoot.World;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class GreenfootGame extends Game {

    @Override
    public void create() {
        // Try to find the main World subclass
        Class<?> mainWorldClass = findMainWorld();

        if (mainWorldClass == null) {
            Gdx.app.error("GreenfootWrapper", "No World subclass found!");
            return;
        }

        try {
            World world = (World) ClassReflection.newInstance(mainWorldClass);
            setScreen(world); // World extends Screen in your wrapper
        } catch (ReflectionException e) {
            Gdx.app.error("GreenfootWrapper", "Failed to create World: " + e.getMessage());
        }
    }

    private Class<?> findMainWorld() {
        try {
            // Use LibGDX internal file system to read project.greenfoot
            com.badlogic.gdx.files.FileHandle projectFile = Gdx.files.internal("tes/project.greenfoot");
            if (projectFile.exists()) {
                String content = projectFile.readString();
                String[] lines = content.split("\n");
                
                for (String line : lines) {
                    if (line.startsWith("world.lastInstantiated=")) {
                        String entryWorld = line.split("=")[1].trim();
                        // Try to load class - first without package, then with package prefix
                        try {
                            return Class.forName(entryWorld);
                        } catch (ClassNotFoundException e1) {
                            // If that fails, try with the full package path (assume it's in user-project.tes package)
                            return Class.forName("user-project.tes." + entryWorld);
                        }
                    }
                }
            }

            // Try default MyWorld class - first without package, then with package prefix
            try {
                return Class.forName("MyWorld");
            } catch (ClassNotFoundException e1) {
                return Class.forName("user-project.tes.MyWorld");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return null;
    }
}
