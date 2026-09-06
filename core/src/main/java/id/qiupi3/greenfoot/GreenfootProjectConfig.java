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

package id.qiupi3.greenfoot;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

/**
 * Configuration class for dynamically detecting and managing Greenfoot project paths.
 * <p>
 * The primary detection mechanism reads {@code project_cache.txt} from the assets
 * root — this file is written by the {@code copyGreenfootAssets} Gradle task at
 * build time. If that file is missing (e.g. during first-time development), we
 * fall back to scanning the assets directory for any subfolder that contains
 * {@code project.greenfoot}, then to parsing {@code assets.txt}.
 * 
 * @author Qiupi3
 * @version 1.0
 */
public class GreenfootProjectConfig {
    private static String userProjectFolder = null;
    private static boolean initialized = false;
    private static final String CACHE_FILE = "project_cache.txt";

    /**
     * Get the user project folder name (e.g., "Clash Perpetuation", "MyGame", etc.)
     * This method will automatically detect the project folder if not already initialized.
     *
     * @return The user project folder name, or null if not found
     */
    public static String getUserProjectFolder() {
        if (!initialized) {
            detectUserProjectFolder();
        }
        return userProjectFolder;
    }

    /**
     * Manually set the user project folder (for testing or specific cases).
     * This can be useful when automatic detection fails or for custom project structures.
     *
     * @param folder The folder name to use as the user project folder
     */
    public static void setUserProjectFolder(String folder) {
        userProjectFolder = folder;
        initialized = true;
        if (Gdx.app != null) {
            Gdx.app.log("GreenfootConfig", "User project folder manually set to: " + folder);
        } else {
            System.out.println("GreenfootConfig: User project folder manually set to: " + folder);
        }
    }

    /**
     * Reset the configuration to allow re-detection of the project folder.
     * Useful for testing or when the project structure changes.
     */
    public static void reset() {
        userProjectFolder = null;
        initialized = false;
    }

    // ------------------------------------------------------------------
    //  Detection pipeline
    // ------------------------------------------------------------------

    /**
     * Runs the detection pipeline in priority order:
     * <ol>
     *   <li>Read {@code project_cache.txt} (written by the Gradle build)</li>
     *   <li>Scan the assets directory for a subfolder with {@code project.greenfoot}</li>
     *   <li>Parse {@code assets.txt} for folder names referencing {@code project.greenfoot}</li>
     * </ol>
     */
    private static void detectUserProjectFolder() {
        initialized = true;

        try {
            // 1. Try the build-time cache first (fastest, most reliable)
            userProjectFolder = loadFromCache();
            if (userProjectFolder != null) {
                log("Loaded project folder from cache: " + userProjectFolder);
                return;
            }

            // 2. Scan via LibGDX file handles (works on all platforms)
            userProjectFolder = scanUsingLibGDX();
            if (userProjectFolder != null) {
                log("Detected project folder via LibGDX scan: " + userProjectFolder);
                return;
            }

            // 3. Parse assets.txt as last resort
            userProjectFolder = scanAssetsTextFile();
            if (userProjectFolder != null) {
                log("Detected project folder from assets.txt: " + userProjectFolder);
                return;
            }

            // Nothing found
            log("WARNING: No project folder found. Make sure your Greenfoot project" +
                " was copied to the assets directory by the copyGreenfootAssets Gradle task.");
            userProjectFolder = null;

        } catch (Exception e) {
            System.err.println("GreenfootConfig: Error detecting project folder: " + e.getMessage());
            userProjectFolder = null;
        }
    }

    // ------------------------------------------------------------------
    //  Strategy 1: project_cache.txt (written by Gradle at build time)
    // ------------------------------------------------------------------

    /**
     * Load the cached project folder name from {@code project_cache.txt}.
     * The Gradle {@code copyGreenfootAssets} task writes this file into the
     * assets root every time it runs, so it should always be up-to-date.
     */
    private static String loadFromCache() {
        try {
            if (Gdx.files == null) return null;

            FileHandle cacheFile = Gdx.files.internal(CACHE_FILE);
            if (!cacheFile.exists()) return null;

            String cached = cacheFile.readString().trim();
            if (cached.isEmpty()) return null;

            // Verify the cached folder still has project.greenfoot
            FileHandle projectFile = Gdx.files.internal(cached + "/project.greenfoot");
            if (projectFile.exists()) {
                return cached;
            }

            log("Cache value '" + cached + "' is stale (project.greenfoot not found)");
        } catch (Exception e) {
            System.err.println("GreenfootConfig: Error loading cache: " + e.getMessage());
        }
        return null;
    }

    // ------------------------------------------------------------------
    //  Strategy 2: Scan with LibGDX FileHandle (Android-safe)
    // ------------------------------------------------------------------

    /**
     * On desktop the internal root is a real directory we can list.
     * On Android {@code list()} on an internal directory returns the entries
     * inside the APK assets, so this works cross-platform.
     */
    private static String scanUsingLibGDX() {
        try {
            if (Gdx.files == null) return null;

            // List the assets root
            FileHandle assetsRoot = Gdx.files.internal("");
            FileHandle[] children = assetsRoot.list();

            if (children != null) {
                for (FileHandle child : children) {
                    if (child.isDirectory()) {
                        FileHandle marker = child.child("project.greenfoot");
                        if (marker.exists()) {
                            return child.name();
                        }
                    }
                }
            }
        } catch (Exception e) {
            // list() may throw on some backends; fall through to next strategy
            System.err.println("GreenfootConfig: Error scanning with LibGDX: " + e.getMessage());
        }
        return null;
    }

    // ------------------------------------------------------------------
    //  Strategy 3: Parse assets.txt
    // ------------------------------------------------------------------

    /**
     * The root {@code build.gradle} generates {@code assets.txt} listing every
     * file in the assets directory. We scan it for lines matching
     * {@code <folder>/project.greenfoot} and extract the folder name.
     */
    private static String scanAssetsTextFile() {
        try {
            if (Gdx.files == null) return null;

            FileHandle assetsList = Gdx.files.internal("assets.txt");
            if (!assetsList.exists()) return null;

            String content = assetsList.readString();
            for (String line : content.split("\\n")) {
                String trimmed = line.trim();
                if (trimmed.endsWith("/project.greenfoot") || trimmed.endsWith("\\project.greenfoot")) {
                    // e.g. "MyGame/project.greenfoot" → "MyGame"
                    int sep = trimmed.lastIndexOf('/');
                    if (sep < 0) sep = trimmed.lastIndexOf('\\');
                    if (sep > 0) {
                        String folder = trimmed.substring(0, sep);
                        // Handle nested paths: take only the top-level folder
                        int firstSep = folder.indexOf('/');
                        if (firstSep < 0) firstSep = folder.indexOf('\\');
                        if (firstSep > 0) {
                            folder = folder.substring(0, firstSep);
                        }
                        return folder;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("GreenfootConfig: Error parsing assets.txt: " + e.getMessage());
        }
        return null;
    }

    // ------------------------------------------------------------------
    //  Path helpers (public API)
    // ------------------------------------------------------------------

    /**
     * Get the path for images within the user's project.
     * @param filename The image filename
     * @return The full path to the image
     */
    public static String getImagesPath(String filename) {
        String folder = getUserProjectFolder();
        return folder + "/images/" + filename;
    }

    /**
     * Get the path for sounds within the user's project.
     * @param filename The sound filename
     * @return The full path to the sound
     */
    public static String getSoundsPath(String filename) {
        String folder = getUserProjectFolder();
        return folder + "/sounds/" + filename;
    }

    /**
     * Get the path for any asset within the user's project.
     * @param filename The asset filename
     * @return The full path to the asset
     */
    public static String getAssetPath(String filename) {
        String folder = getUserProjectFolder();
        return folder + "/" + filename;
    }

    /**
     * Get all possible paths for an asset (for fallback loading).
     * @param filename The filename to search for
     * @return Array of possible paths to try
     */
    public static String[] getAllPossiblePaths(String filename) {
        String projectFolder = getUserProjectFolder();
        return new String[] {
            projectFolder + "/images/" + filename,     // Project images folder
            projectFolder + "/sounds/" + filename,     // Project sounds folder
            projectFolder + "/" + filename,            // Project root
            filename                                   // Assets root fallback
        };
    }

    /**
     * Get debug information about the current configuration.
     * @return Debug info string
     */
    public static String getDebugInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("GreenfootProjectConfig Debug Info:\n");
        sb.append("  Initialized: ").append(initialized).append("\n");
        sb.append("  User Project Folder: ").append(userProjectFolder).append("\n");
        sb.append("  Sample paths:\n");
        sb.append("    Images: ").append(getImagesPath("test.png")).append("\n");
        sb.append("    Sounds: ").append(getSoundsPath("test.wav")).append("\n");
        sb.append("    Assets: ").append(getAssetPath("test.txt")).append("\n");

        // Check if project.greenfoot file exists
        String projectFilePath = getAssetPath("project.greenfoot");
        boolean projectExists = false;
        try {
            if (Gdx.files != null) {
                FileHandle projectFile = Gdx.files.internal(projectFilePath);
                projectExists = projectFile.exists();
            }
        } catch (Exception e) {
            // Ignore
        }
        sb.append("  Project file exists: ").append(projectExists);

        return sb.toString();
    }

    /**
     * Force re-detection of the project folder (clears cache).
     */
    public static void forceRedetection() {
        reset();
        getUserProjectFolder(); // This will trigger detection
    }

    // ------------------------------------------------------------------
    //  Internal helpers
    // ------------------------------------------------------------------

    private static void log(String message) {
        if (Gdx.app != null) {
            Gdx.app.log("GreenfootConfig", message);
        } else {
            System.out.println("GreenfootConfig: " + message);
        }
    }
}