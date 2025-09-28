package id.qiupi3.greenfoot;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

/**
 * Configuration class for dynamically detecting and managing Greenfoot project paths.
 * This class scans the actual file system to find project folders and caches the results.
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
    
    /**
     * Simple detection: scan the assets directory for project.greenfoot files and cache the result.
     */
    private static void detectUserProjectFolder() {
        initialized = true;
        
        try {
            // First, try to load from cache
            userProjectFolder = loadFromCache();
            if (userProjectFolder != null) {
                System.out.println("GreenfootConfig: Loaded project folder from cache: " + userProjectFolder);
                return;
            }
            
            // If no cache, scan the file system directly
            userProjectFolder = scanFileSystemForProject();
            
            if (userProjectFolder != null) {
                System.out.println("GreenfootConfig: Found project folder: " + userProjectFolder);
                saveToCache(userProjectFolder);
            } else {
                System.out.println("GreenfootConfig: No project folder found, using default");
                userProjectFolder = "MyGame";
            }
            
        } catch (Exception e) {
            System.err.println("GreenfootConfig: Error detecting project folder: " + e.getMessage());
            userProjectFolder = "MyGame";
        }
    }
    
    /**
     * Scan the actual file system (not through LibGDX) to find project folders.
     * This bypasses all the LibGDX asset limitations.
     */
    private static String scanFileSystemForProject() {
        try {
            // First try using LibGDX FileHandle (works on Android)
            if (Gdx.files != null) {
                String result = scanUsingLibGDX();
                if (result != null) {
                    return result;
                }
            }
            
            // Fall back to java.io.File for desktop platforms
            File assetsDir = new File("assets");
            if (!assetsDir.exists()) {
                // Try alternative locations
                assetsDir = new File("../assets");
                if (!assetsDir.exists()) {
                    assetsDir = new File("../../assets");
                }
            }
            
            if (assetsDir.exists() && assetsDir.isDirectory()) {
                System.out.println("GreenfootConfig: Scanning directory: " + assetsDir.getAbsolutePath());
                File[] subdirs = assetsDir.listFiles(File::isDirectory);
                
                if (subdirs != null) {
                    for (File subdir : subdirs) {
                        File projectFile = new File(subdir, "project.greenfoot");
                        if (projectFile.exists()) {
                            System.out.println("GreenfootConfig: Found project.greenfoot in: " + subdir.getName());
                            return subdir.getName();
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("GreenfootConfig: Error scanning file system: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Scan for project folders using LibGDX FileHandle API (works on all platforms including Android).
     */
    private static String scanUsingLibGDX() {
        try {
            // Try some common project folder names first
            String[] commonFolders = {
                "MyGame",
                "MyWorld",
                "Game"
            };
            
            for (String folderName : commonFolders) {
                FileHandle projectFile = Gdx.files.internal(folderName + "/project.greenfoot");
                if (projectFile.exists()) {
                    System.out.println("GreenfootConfig: Found project.greenfoot in: " + folderName);
                    return folderName;
                }
            }
            
            // If common names don't work, we can't easily list directories on Android
            // So we'll just try reading from cache or using a hardcoded value
            System.out.println("GreenfootConfig: Could not auto-detect project folder using LibGDX");
        } catch (Exception e) {
            System.err.println("GreenfootConfig: Error scanning with LibGDX: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Load the cached project folder name from file.
     */
    private static String loadFromCache() {
        try {
            // Try LibGDX FileHandle first (works on Android)
            if (Gdx.files != null) {
                FileHandle cacheFile = Gdx.files.internal(CACHE_FILE);
                if (cacheFile.exists()) {
                    String cached = cacheFile.readString().trim();
                    
                    // Verify the cached folder still exists and has project.greenfoot
                    FileHandle projectFile = Gdx.files.internal(cached + "/project.greenfoot");
                    if (projectFile.exists()) {
                        return cached;
                    }
                }
            }
            
            // Fall back to java.io.File for desktop
            File cacheFile = new File(CACHE_FILE);
            if (cacheFile.exists()) {
                Scanner scanner = new Scanner(cacheFile);
                if (scanner.hasNextLine()) {
                    String cached = scanner.nextLine().trim();
                    scanner.close();
                    
                    // Verify the cached folder still exists and has project.greenfoot
                    if (Gdx.files != null) {
                        FileHandle projectFile = Gdx.files.internal(cached + "/project.greenfoot");
                        if (projectFile.exists()) {
                            return cached;
                        }
                    } else {
                        File projectFile = new File("assets/" + cached + "/project.greenfoot");
                        if (projectFile.exists()) {
                            return cached;
                        }
                    }
                    
                    // Cache is stale, delete it
                    cacheFile.delete();
                }
                scanner.close();
            }
        } catch (Exception e) {
            System.err.println("GreenfootConfig: Error loading cache: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Save the project folder name to cache file.
     */
    private static void saveToCache(String folderName) {
        try {
            FileWriter writer = new FileWriter(CACHE_FILE);
            writer.write(folderName);
            writer.close();
            System.out.println("GreenfootConfig: Saved project folder to cache: " + folderName);
        } catch (IOException e) {
            System.err.println("GreenfootConfig: Error saving to cache: " + e.getMessage());
        }
    }
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
        // Delete cache file
        try {
            File cacheFile = new File(CACHE_FILE);
            if (cacheFile.exists()) {
                cacheFile.delete();
                System.out.println("GreenfootConfig: Cache cleared");
            }
        } catch (Exception e) {
            System.err.println("GreenfootConfig: Error clearing cache: " + e.getMessage());
        }
        
        // Reset and re-detect
        reset();
        getUserProjectFolder(); // This will trigger detection
    }
}