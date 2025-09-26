package greenfoot;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * Class that makes it possible for classes outside the greenfoot package to get
 * access to Image methods that are package protected. We need some
 * package-protected methods in the Image, because we don't want them to show up
 * in the public interface visible to users.
 * 
 * Converted to work with LibGDX instead of AWT.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: ImageVisitor.java 6256 2009-04-16 11:55:51Z polle $
 */
public class ImageVisitor
{
    /**
     * Draw a GreenfootImage using LibGDX SpriteBatch instead of AWT Graphics2D.
     * 
     * @param image The image to draw
     * @param batch The LibGDX SpriteBatch to draw with
     * @param x The x coordinate
     * @param y The y coordinate  
     * @param useTransparency Whether to use transparency
     */
    public static void drawImage(GreenfootImage image, SpriteBatch batch, int x, int y, boolean useTransparency)
    {
        if (image == null || batch == null) {
            return;
        }
        
        // Get the LibGDX texture from the GreenfootImage
        com.badlogic.gdx.graphics.Texture texture = image.getTexture();
        if (texture != null) {
            if (useTransparency) {
                // Apply transparency from image
                float alpha = image.getTransparency() / 255.0f;
                batch.setColor(1f, 1f, 1f, alpha);
            }
            
            // Draw the texture
            batch.draw(texture, x, y);
            
            if (useTransparency) {
                // Reset color to normal
                batch.setColor(1f, 1f, 1f, 1f);
            }
        }
    }
    
    /**
     * Compare two GreenfootImages for equality.
     * 
     * @param image1 First image to compare
     * @param image2 Second image to compare
     * @return true if the images are equal
     */
    public static boolean equal(GreenfootImage image1, GreenfootImage image2)
    {
        // Use the static method from GreenfootImage class
        return GreenfootImage.equal(image1, image2);
    }
    
    /**
     * Get the width of a GreenfootImage.
     * 
     * @param image The image to get width of
     * @return Width in pixels
     */
    public static int getWidth(GreenfootImage image)
    {
        return image != null ? image.getWidth() : 0;
    }
    
    /**
     * Get the height of a GreenfootImage.
     * 
     * @param image The image to get height of  
     * @return Height in pixels
     */
    public static int getHeight(GreenfootImage image)
    {
        return image != null ? image.getHeight() : 0;
    }
}
