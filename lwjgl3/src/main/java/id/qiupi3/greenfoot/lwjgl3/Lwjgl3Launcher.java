/*
 This file is part of the LibGDX-Greenfoot wrapper.
 Copyright (C) 2026 Qiupi3

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

package id.qiupi3.greenfoot.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import id.qiupi3.greenfoot.GreenfootGame;

/**
 * Launches the desktop (LWJGL3) application.
 * 
 * @author Qiupi3
 * @version 1.0
 */
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