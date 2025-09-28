/*
 This file is part of the LibGDX-Greenfoot wrapper.
 Provides LibGDX-compatible implementation of java.awt.image.BufferedImage for cross-platform compatibility.
 
 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 GNU General Public License for more details.
*/

package greenfoot.awt.image;

import com.badlogic.gdx.graphics.Pixmap;
import java.nio.ByteBuffer;

/**
 * LibGDX-compatible implementation of java.awt.image.BufferedImage for cross-platform use.
 * This class provides image data access and manipulation using LibGDX Pixmap.
 * 
 * This class replaces java.awt.image.BufferedImage to provide LibGDX backend compatibility,
 * mainly to allow Greenfoot projects to run on LibGDX platforms.
 * 
 * @author Qiupi3 (LibGDX wrapper implementation)
 * @version 1.0
 */
public class BufferedImage {
    
    /** Represents an image with 8-bit RGBA color components packed into integer pixels. */
    public static final int TYPE_INT_ARGB = 2;
    
    /** Represents an image with 8-bit RGB color components packed into integer pixels. */
    public static final int TYPE_INT_RGB = 1;
    
    private Pixmap pixmap;
    private int imageType;
    
    /**
     * Constructs a BufferedImage of one of the predefined image types.
     * 
     * @param width width of the created image
     * @param height height of the created image  
     * @param imageType type of the created image
     */
    public BufferedImage(int width, int height, int imageType) {
        this.imageType = imageType;
        
        // Create LibGDX Pixmap with appropriate format
        Pixmap.Format format = (imageType == TYPE_INT_RGB) ? 
            Pixmap.Format.RGB888 : Pixmap.Format.RGBA8888;
        
        this.pixmap = new Pixmap(width, height, format);
        
        // Initialize with transparent/white background
        pixmap.setColor(0, 0, 0, imageType == TYPE_INT_RGB ? 1 : 0);
        pixmap.fill();
    }
    
    /**
     * Constructs a BufferedImage from a LibGDX Pixmap.
     * Used internally by GreenfootImage.getAwtImage().
     * 
     * @param pixmap The source LibGDX Pixmap
     */
    public BufferedImage(Pixmap pixmap) {
        this.pixmap = new Pixmap(pixmap.getWidth(), pixmap.getHeight(), pixmap.getFormat());
        this.pixmap.drawPixmap(pixmap, 0, 0);
        
        // Determine image type based on format
        this.imageType = (pixmap.getFormat() == Pixmap.Format.RGB888) ? 
            TYPE_INT_RGB : TYPE_INT_ARGB;
    }
    
    /**
     * Returns the width of the BufferedImage.
     * 
     * @return the width of this BufferedImage
     */
    public int getWidth() {
        return pixmap != null ? pixmap.getWidth() : 0;
    }
    
    /**
     * Returns the height of the BufferedImage.
     * 
     * @return the height of this BufferedImage
     */
    public int getHeight() {
        return pixmap != null ? pixmap.getHeight() : 0;
    }
    
    /**
     * Returns the image type.
     * 
     * @return the image type of this BufferedImage
     */
    public int getType() {
        return imageType;
    }
    
    /**
     * Returns an integer pixel in the default RGB color model and default sRGB colorspace.
     * 
     * @param x the X coordinate of the pixel from which to get the pixel
     * @param y the Y coordinate of the pixel from which to get the pixel
     * @return the RGB value of the pixel at the specified coordinate
     */
    public int getRGB(int x, int y) {
        if (pixmap == null) return 0;
        
        int pixel = pixmap.getPixel(x, y);
        
        // Convert from LibGDX RGBA format to standard ARGB format
        int r = (pixel >>> 24) & 0xFF;
        int g = (pixel >>> 16) & 0xFF; 
        int b = (pixel >>> 8) & 0xFF;
        int a = pixel & 0xFF;
        
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
    
    /**
     * Sets a pixel to the specified RGB value.
     * 
     * @param x the X coordinate of the pixel to set
     * @param y the Y coordinate of the pixel to set  
     * @param rgb the RGB value
     */
    public void setRGB(int x, int y, int rgb) {
        if (pixmap == null) return;
        
        // Convert from standard ARGB format to LibGDX RGBA format
        int a = (rgb >>> 24) & 0xFF;
        int r = (rgb >>> 16) & 0xFF;
        int g = (rgb >>> 8) & 0xFF;
        int b = rgb & 0xFF;
        
        // Set color and draw pixel
        pixmap.setColor(r / 255f, g / 255f, b / 255f, a / 255f);
        pixmap.drawPixel(x, y);
    }
    
    /**
     * Get direct access to the underlying LibGDX Pixmap.
     * This method is specific to the LibGDX implementation.
     * 
     * @return the underlying LibGDX Pixmap
     */
    public Pixmap getPixmap() {
        return pixmap;
    }
    
    /**
     * Get the pixel data as a ByteBuffer.
     * This method is specific to the LibGDX implementation.
     * 
     * @return ByteBuffer containing the pixel data
     */
    public ByteBuffer getPixelData() {
        return pixmap != null ? pixmap.getPixels() : null;
    }
    
    /**
     * Dispose of this BufferedImage and free its resources.
     * This method is specific to the LibGDX implementation.
     */
    public void dispose() {
        if (pixmap != null) {
            pixmap.dispose();
            pixmap = null;
        }
    }
    
    @Override
    public String toString() {
        return "BufferedImage[" + getWidth() + "x" + getHeight() + ", type=" + imageType + "]";
    }
}