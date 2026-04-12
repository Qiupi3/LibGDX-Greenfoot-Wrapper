package id.qiupi3.greenfoot.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import id.qiupi3.greenfoot.GreenfootGame;

/** Launches the desktop (LWJGL3) application. */
public class Lwjgl3Launcher {
    public static void main(String[] args) {
        if (StartupHelper.startNewJvmIfRequired()) return; // This handles macOS support and helps on Windows.
        createApplication();
    }

    private static Lwjgl3Application createApplication() {
        return new Lwjgl3Application(new GreenfootGame(), getDefaultConfiguration());
    }

    private static Lwjgl3ApplicationConfiguration getDefaultConfiguration() {
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("greenfoot-wrapper");
        
        // Disable VSync for better control over frame timing during drag operations
        // configuration.useVsync(false);
        configuration.useVsync(true);
        
        // Set a lower FPS for better drag & drop interaction timing
        // 30 FPS gives ~33ms per frame, providing more time for mouse event processing
        // This can be increased to 60 if drag performance is satisfactory
        // configuration.setForegroundFPS(30);
        configuration.setForegroundFPS(Lwjgl3ApplicationConfiguration.getDisplayMode().refreshRate + 1);
        
        //// Original high-FPS settings (commented out for better drag & drop):
        //// configuration.useVsync(true);
        //// configuration.setForegroundFPS(Lwjgl3ApplicationConfiguration.getDisplayMode().refreshRate + 1);
        //// Uncomment above and set FPS to monitor refresh rate for maximum performance

        configuration.setWindowedMode(640, 480);
        //// You can change these files; they are in lwjgl3/src/main/resources/ .
        //// They can also be loaded from the root of assets/ .
        configuration.setWindowIcon("libgdx128.png", "libgdx64.png", "libgdx32.png", "libgdx16.png");
        return configuration;
    }
}