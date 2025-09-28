/*
 This file is part of the Greenfoot program.
 Copyright (C) 2005-2009,2010,2011,2013,2014,2015,2016,2021 Poul Henriksen and Michael Kolling

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

package greenfoot;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.files.FileHandle;
import java.util.HashMap;
import java.util.Map;

// LibGDX-compatible implementations of AWT classes
import greenfoot.awt.Shape;
import greenfoot.awt.image.BufferedImage;

/**
 * LibGDX-based GreenfootImage implementation. This class encapsulates a LibGDX Pixmap and provides
 * methods for manipulating images in a way that is compatible with the Greenfoot API.
 *
 * This class re-implements greenfoot.GreenfootImage to provide a LibGDX backend,
 * mainly to allow Greenfoot projects to run on LibGDX (especially to export into
 * mobile devices and other platforms).
 * 
 * Inspired by the original Greenfoot project (GPLv2+ with Classpath Exception).
 * Read the original documentation at
 * https://www.greenfoot.org/files/javadoc/greenfoot/GreenfootImage.html
 * 
 * @author Poul Henriksen (Original Greenfoot version's author)
 * 
 * @modified-by Qiupi3 (LibGDX wrapper implementation)
 * @version 1.0
 */
public class GreenfootImage {
    private Texture texture;
    private Pixmap pixmap;
    private String imageFileName;
    private greenfoot.Color currentColor = greenfoot.Color.BLACK;
    private greenfoot.Font currentFont;
    private boolean copyOnWrite = false;
    private int transparency = 255;
    
    private static Map<String, GreenfootImage> cachedImages = new HashMap<>();

    public GreenfootImage(String filename) {
        GreenfootImage gImage = getCachedImage(filename);
        if (gImage != null)
        {
            createClone(gImage);
        }
        else 
        {
            try{
                loadFile(filename);
            }
            catch(IllegalArgumentException ile){
                addCachedImage(filename, null);
                throw ile;
            }
        }
        //if the image was successfully cached, ensure that the image is copyOnWrite
        boolean success = addCachedImage(filename, new GreenfootImage(this));
        if (success){
            copyOnWrite = true;
        }
    }
       
    public GreenfootImage(int width, int height) {
        createPixmap(width, height);
    }

    public GreenfootImage(GreenfootImage image) {
        if (image == null) {
            throw new IllegalArgumentException("Source image cannot be null");
        }
        
        if (image.pixmap != null) {
            createPixmap(image.getWidth(), image.getHeight());
            pixmap.drawPixmap(image.pixmap, 0, 0);
        }
        copyStates(image, this);
    }
    
    public GreenfootImage(String string, int size, greenfoot.Color foreground, greenfoot.Color background) {
        this(string, size, foreground, background, null);
    }
    
    public GreenfootImage(String string, int size, greenfoot.Color foreground, greenfoot.Color background, greenfoot.Color outline) {
        String[] lines = string.split("\n");
        
        int maxWidth = 0;
        for (String line : lines) {
            int width = line.length() * (size * 3 / 4); 
            if (width > maxWidth) {
                maxWidth = width;
            }
        }
        
        int totalHeight = lines.length * size;
        createPixmap(Math.max(maxWidth, 1), Math.max(totalHeight, 1));
        
        if (background != null) {
            pixmap.setColor(background.getRed() / 255f, background.getGreen() / 255f, 
                          background.getBlue() / 255f, background.getAlpha() / 255f);
            pixmap.fill();
        }
        
        greenfoot.Color textColor = foreground != null ? foreground : greenfoot.Color.BLACK;
        setColor(textColor);
        
        for (int i = 0; i < lines.length; i++) {
            drawString(lines[i], 0, size * (i + 1));
        }
    }
    
    GreenfootImage(byte[] imageData) {
        try {
            pixmap = new Pixmap(imageData, 0, imageData.length);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Could not load image" + (imageFileName != null ? (" from: " + imageFileName) : ""));
        }
    }  

    /**
     * Package-visible constructor to create GreenfootImage from LibGDX Texture.
     * Used internally by Actor.getImage() method.
     */
    GreenfootImage(Texture texture) {
        if (texture == null) {
            throw new IllegalArgumentException("Texture must not be null.");
        }
        
        this.texture = texture;
        
        // Create pixmap from texture for editing operations
        // Note: This is expensive but necessary for pixel-level editing
        if (!texture.getTextureData().isPrepared()) {
            texture.getTextureData().prepare();
        }
        
        // Create pixmap with same dimensions as texture
        pixmap = new Pixmap(texture.getWidth(), texture.getHeight(), Pixmap.Format.RGBA8888);
        
        // For now, create empty pixmap - in a full implementation we'd copy texture data
        // This allows the GreenfootImage to be used for drawing operations
        pixmap.setColor(1, 1, 1, 1);
        pixmap.fill();
        
        copyOnWrite = true; // Mark as copy-on-write to defer expensive operations
    }
    
    GreenfootImage() { }

    public void clear() {
        if (pixmap == null) return;
        
        ensureWritableImage();
        pixmap.setColor(0, 0, 0, 0);
        pixmap.fill();
        invalidateTexture();
    }

    public void drawImage(GreenfootImage image, int x, int y) {
        if (pixmap == null || image == null || image.pixmap == null) return;
        
        ensureWritableImage();
        pixmap.drawPixmap(image.pixmap, x, y);
        invalidateTexture();
    }

    public void drawLine(int x1, int y1, int x2, int y2) {
        if (pixmap == null) return;
        
        ensureWritableImage();
        pixmap.setColor(currentColor.getRed() / 255f, currentColor.getGreen() / 255f, 
                       currentColor.getBlue() / 255f, currentColor.getAlpha() / 255f);
        pixmap.drawLine(x1, y1, x2, y2);
        invalidateTexture();
    }

    public void drawOval(int x, int y, int width, int height) {
        if (pixmap == null) return;
        
        ensureWritableImage();
        pixmap.setColor(currentColor.getRed() / 255f, currentColor.getGreen() / 255f, 
                       currentColor.getBlue() / 255f, currentColor.getAlpha() / 255f);
        pixmap.drawCircle(x + width/2, y + height/2, Math.min(width, height) / 2);
        invalidateTexture();
    }

    public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        fillPolygon(xPoints, yPoints, nPoints); // Same implementation for now
    }

    public void drawRect(int x, int y, int width, int height) {
        if (pixmap == null) return;
        
        ensureWritableImage();
        pixmap.setColor(currentColor.getRed() / 255f, currentColor.getGreen() / 255f, 
                       currentColor.getBlue() / 255f, currentColor.getAlpha() / 255f);
        pixmap.drawRectangle(x, y, width, height);
        invalidateTexture();
    }

    /**
     * Draw a geometric shape on this image.
     * This implementation provides LibGDX-based shape rendering that's compatible
     * with the original Greenfoot API that used java.awt.Shape.
     * 
     * @param shape The shape to draw. Uses LibGDX-compatible Shape implementation.
     */
    public void drawShape(Shape shape) {
        if (pixmap == null || shape == null) return;
        
        ensureWritableImage();
        pixmap.setColor(currentColor.getRed() / 255f, currentColor.getGreen() / 255f, 
                       currentColor.getBlue() / 255f, currentColor.getAlpha() / 255f);
        
        // Use the LibGDX-compatible Shape's drawing methods
        shape.draw(pixmap);
        
        invalidateTexture();
    }

    public void drawString(String string, int x, int y) {
        if (pixmap == null || string == null) return;
        
        ensureWritableImage();
        
        String[] lines = string.split("\n");
        pixmap.setColor(currentColor.getRed() / 255f, currentColor.getGreen() / 255f, 
                       currentColor.getBlue() / 255f, currentColor.getAlpha() / 255f);
        
        int lineHeight = 12; // Default line height
        for (int i = 0; i < lines.length; i++) {
            // Simplified character drawing - each character as a small rectangle
            for (int j = 0; j < lines[i].length(); j++) {
                char c = lines[i].charAt(j);
                if (c != ' ') {
                    pixmap.fillRectangle(x + j * 8, y + i * lineHeight, 6, 10);
                }
            }
        }
        
        invalidateTexture();
    }

    public void fill() {
        if (pixmap == null) return;
        
        ensureWritableImage();
        pixmap.setColor(currentColor.getRed() / 255f, currentColor.getGreen() / 255f, 
                       currentColor.getBlue() / 255f, currentColor.getAlpha() / 255f);
        pixmap.fill();
        
        invalidateTexture();
    }

    public void fillOval(int x, int y, int width, int height) {
        if (pixmap == null) return;
        
        ensureWritableImage();
        pixmap.setColor(currentColor.getRed() / 255f, currentColor.getGreen() / 255f, 
                       currentColor.getBlue() / 255f, currentColor.getAlpha() / 255f);
        pixmap.fillCircle(x + width/2, y + height/2, Math.min(width, height) / 2);
        invalidateTexture();
    }

    public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        if (pixmap == null || xPoints == null || yPoints == null) return;
        
        ensureWritableImage();
        pixmap.setColor(currentColor.getRed() / 255f, currentColor.getGreen() / 255f, 
                       currentColor.getBlue() / 255f, currentColor.getAlpha() / 255f);
        
        // Simplified polygon fill - connect the points with lines
        for (int i = 0; i < nPoints - 1; i++) {
            pixmap.drawLine(xPoints[i], yPoints[i], xPoints[i + 1], yPoints[i + 1]);
        }
        if (nPoints > 2) {
            pixmap.drawLine(xPoints[nPoints - 1], yPoints[nPoints - 1], xPoints[0], yPoints[0]);
        }
        
        invalidateTexture();
    }

    public void fillRect(int x, int y, int width, int height) {
        if (pixmap == null) return;
        
        ensureWritableImage();
        pixmap.setColor(currentColor.getRed() / 255f, currentColor.getGreen() / 255f, 
                       currentColor.getBlue() / 255f, currentColor.getAlpha() / 255f);
        pixmap.fillRectangle(x, y, width, height);
        invalidateTexture();
    }
    
    /**
     * Get a BufferedImage representation of this image.
     * This method returns a LibGDX-compatible BufferedImage implementation
     * that maintains API compatibility with the original Greenfoot method.
     * 
     * @return A BufferedImage containing the pixel data of this image
     */
    public BufferedImage getAwtImage() {
        if (pixmap == null) {
            return null;
        }
        
        // Create a LibGDX-compatible BufferedImage from our Pixmap
        return new BufferedImage(pixmap);
    }
    
    public greenfoot.Color getColor() {
        return currentColor;
    }

    public greenfoot.Color getColorAt(int x, int y) {
        if (pixmap == null) return greenfoot.Color.BLACK;
        
        int pixel = pixmap.getPixel(x, y);
        
        int r = (pixel >>> 24) & 0xFF;
        int g = (pixel >>> 16) & 0xFF;
        int b = (pixel >>> 8) & 0xFF;
        int a = pixel & 0xFF;
        
        return new greenfoot.Color(r, g, b, a);
    }

    public greenfoot.Font getFont() {
        if (currentFont == null) {
            currentFont = new greenfoot.Font("Arial", false, false, 12);
        }
        return currentFont;
    }

    public int getHeight() {
        return pixmap != null ? pixmap.getHeight() : 0;
    }

    public int getTransparency() {
        return transparency;
    }

    public int getWidth() {
        return pixmap != null ? pixmap.getWidth() : 0;
    }

    public void mirrorHorizontally() {
        if (pixmap == null) return;
        
        ensureWritableImage();
        
        int width = getWidth();
        int height = getHeight();
        Pixmap flippedPixmap = new Pixmap(width, height, pixmap.getFormat());
        
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                flippedPixmap.drawPixel(width - 1 - x, y, pixmap.getPixel(x, y));
            }
        }
        
        pixmap.dispose();
        pixmap = flippedPixmap;
        
        if (texture != null) {
            texture.dispose();
            texture = null;
        }
    }

    public void mirrorVertically() {
        if (pixmap == null) return;
        
        ensureWritableImage();
        
        int width = getWidth();
        int height = getHeight();
        Pixmap flippedPixmap = new Pixmap(width, height, pixmap.getFormat());
        
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                flippedPixmap.drawPixel(x, height - 1 - y, pixmap.getPixel(x, y));
            }
        }
        
        pixmap.dispose();
        pixmap = flippedPixmap;
        
        if (texture != null) {
            texture.dispose();
            texture = null;
        }
    }

    public void rotate(int degrees) {
        if (pixmap == null) return;
        
        ensureWritableImage();
        
        int width = getWidth();
        int height = getHeight();
        
        Pixmap rotatedPixmap = new Pixmap(width, height, pixmap.getFormat());
        
        double radians = Math.toRadians(degrees);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        
        int centerX = width / 2;
        int centerY = height / 2;
        
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int newX = (int)(cos * (x - centerX) - sin * (y - centerY) + centerX);
                int newY = (int)(sin * (x - centerX) + cos * (y - centerY) + centerY);
                
                if (newX >= 0 && newX < width && newY >= 0 && newY < height) {
                    rotatedPixmap.drawPixel(x, y, pixmap.getPixel(newX, newY));
                }
            }
        }
        
        pixmap.dispose();
        pixmap = rotatedPixmap;
        
        if (texture != null) {
            texture.dispose();
            texture = null;
        }
    }

    public void scale(int width, int height) {
        if (pixmap == null) return;
        if (width == getWidth() && height == getHeight()) return;
        
        ensureWritableImage();
        
        Pixmap scaledPixmap = new Pixmap(width, height, pixmap.getFormat());
        scaledPixmap.drawPixmap(pixmap, 0, 0, pixmap.getWidth(), pixmap.getHeight(), 
                               0, 0, width, height);
        
        pixmap.dispose();
        pixmap = scaledPixmap;
        
        if (texture != null) {
            texture.dispose();
            texture = null;
        }
    }

    public void setColor(greenfoot.Color color) {
        if (color == null)
            throw new NullPointerException("Cannot set color of GreenfootImage to null");
        currentColor = color;
    }
    
    public void setColorAt(int x, int y, greenfoot.Color color) {
        if (pixmap == null) return;
        
        ensureWritableImage();
        int pixel = (color.getRed() << 24) | (color.getGreen() << 16) | 
                   (color.getBlue() << 8) | color.getAlpha();
        pixmap.drawPixel(x, y, pixel);
        invalidateTexture();
    }

    public void setFont(greenfoot.Font f) {
        currentFont = f;
    }

    public void setTransparency(int t) {
        if (t < 0 || t > 255) {
            throw new IllegalArgumentException("The transparency value has to be in the range 0 to 255. It was: " + t);
        }

        this.transparency = t;
    }

    public String toString() {
        String superString = super.toString();
        if (imageFileName == null) {
            return superString;
        }
        else {
            return "Image file name: " + imageFileName + "  " + superString;
        }
    }
    
    private void loadFile(String filename) {
        if (filename == null) {
            throw new NullPointerException("Filename must not be null.");
        }
        imageFileName = filename;
        
        try {
            FileHandle fileHandle = Gdx.files.internal("images/" + filename);
            if (!fileHandle.exists()) {
                fileHandle = Gdx.files.internal(filename);
            }
            
            if (fileHandle.exists()) {
                pixmap = new Pixmap(fileHandle);
            } else {
                throw new IllegalArgumentException("Could not find image file: " + filename);
            }
        }
        catch (Exception e) {
            throw new IllegalArgumentException("Could not load image from: " + filename, e);
        }
    }
    
    private void createPixmap(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Width and height must be positive");
        }
        
        pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        pixmap.setColor(0, 0, 0, 0);
        pixmap.fill();
        copyOnWrite = false;
    }

    private static void copyStates(GreenfootImage src, GreenfootImage dst) {
        dst.imageFileName = src.imageFileName;
        dst.currentColor = src.currentColor;
        dst.currentFont = src.currentFont;
        dst.transparency = src.transparency;
    }
    
    public Texture getTexture() {
        if (texture == null && pixmap != null) {
            texture = new Texture(pixmap);
        }
        return texture;
    }
    
    GreenfootImage getCopyOnWriteClone() {
        GreenfootImage clone = new GreenfootImage();
        clone.copyOnWrite = true;
        clone.pixmap = pixmap;
        clone.texture = texture;
        copyStates(this, clone);
        
        return clone;
    }
    
    void createClone(GreenfootImage cachedImage) {
        this.copyOnWrite = true;
        this.pixmap = cachedImage.pixmap;
        this.texture = cachedImage.texture;
        copyStates(cachedImage, this);
    }
    
    private static GreenfootImage getCachedImage(String filename) {
        return cachedImages.get(filename);
    }
    
    private static boolean addCachedImage(String filename, GreenfootImage image) {
        if (image == null) {
            cachedImages.put(filename, null);
            return false;
        }
        cachedImages.put(filename, image.getCopyOnWriteClone());
        return true;
    }
    
    static boolean equal(GreenfootImage image1, GreenfootImage image2) {
        if (image1 == null || image2 == null) {
            return image1 == image2;
        }
        else {
            return (image1.pixmap == image2.pixmap || image1.equals(image2));
        }
    }
    
    public void dispose() {
        if (texture != null) {
            texture.dispose();
            texture = null;
        }
        if (pixmap != null) {
            pixmap.dispose();
            pixmap = null;
        }
    }
    
    private void ensureWritableImage() {
        if (copyOnWrite && pixmap != null) {
            Pixmap newPixmap = new Pixmap(pixmap.getWidth(), pixmap.getHeight(), pixmap.getFormat());
            newPixmap.drawPixmap(pixmap, 0, 0);
            pixmap = newPixmap;
            copyOnWrite = false;
            
            if (texture != null) {
                texture.dispose();
                texture = null;
            }
        }
    }

    private void invalidateTexture() {
        if (texture != null) {
            texture.dispose();
            texture = null;
        }
    }
}
