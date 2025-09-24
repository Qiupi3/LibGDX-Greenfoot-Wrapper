package id.qiupi3.greenfoot;

import com.badlogic.gdx.Gdx;

import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
            // Locate project.greenfoot
            Path basePath = Paths.get("core/src/main/java/user-project");
            Path projectName = Files.list(basePath).filter(Files::isDirectory).findFirst().orElse(null);
            String projectPath = projectName.toString();
            List<String> lines = Files.readAllLines(
                Files.list(projectName)
                .filter(
                    p -> p.getFileName().toString()
                            .equals("project.greenfoot")
                )
                .findFirst()
                .orElse(null));
            
            for (String line : lines) {
                if (line.startsWith("world.lastInstantiated=")) {
                    String entryWorld = line.split("=")[1].trim();
                    // E.g. "MyWorld" → user-project.MyWorld
                    return Class.forName(projectPath.split("main/java/")[1]
                            .trim()
                            .replace("/", ".") + "." + entryWorld);
                }
            }

            return Class.forName(projectPath.split("main/java/")[1]
                            .trim()
                            .replace("/", ".") + ".MyWorld");
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return null;
    }
}
