package greenfoot.platforms;

import greenfoot.GreenfootImage;

/**
 * Interface for platform-specific actor operations.
 * This is a minimal implementation for compatibility.
 */
public interface ActorDelegate {
    
    /**
     * Get image for the given class name.
     */
    GreenfootImage getImage(String className) throws Exception;
}