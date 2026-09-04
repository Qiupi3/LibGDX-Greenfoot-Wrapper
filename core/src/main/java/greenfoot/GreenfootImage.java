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
        greenfoot.Font textFont = new greenfoot.Font(size);
        com.badlogic.gdx.graphics.g2d.BitmapFont bitmapFont = textFont.getBitmapFont();
        com.badlogic.gdx.graphics.g2d.BitmapFont.BitmapFontData data = bitmapFont.getData();

        String[] lines = string.split("\n", -1);
        com.badlogic.gdx.graphics.g2d.GlyphLayout layout = new com.badlogic.gdx.graphics.g2d.GlyphLayout(bitmapFont, string);

        // Distance from the top of a line to its baseline, as in drawGlyphLine().
        float baseLineFromTop = data.ascent + data.capHeight;

        int width = Math.max((int) Math.ceil(layout.width), 1);
        // The last baseline plus room below it for descenders (g, y, p, ...).
        int height = Math.max((int) Math.ceil(baseLineFromTop + (lines.length - 1) * data.lineHeight
                                              + Math.abs(data.descent)), 1);
        createPixmap(width, height);

        if (background != null) {
            pixmap.setColor(background.getRed() / 255f, background.getGreen() / 255f,
                          background.getBlue() / 255f, background.getAlpha() / 255f);
            pixmap.fill();
        }

        setFont(textFont);
        greenfoot.Color textColor = foreground != null ? foreground : greenfoot.Color.BLACK;
        setColor(textColor);

        for (int i = 0; i < lines.length; i++) {
            drawString(lines[i], 0, Math.round(baseLineFromTop + i * data.lineHeight));
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
        Pixmap sourcePixmap = getPixmapFromTexture(texture);

        // Copy the actual pixel data (including alpha/transparency) into our own
        // pixmap. Filling with opaque white here would destroy the real image.
        pixmap = new Pixmap(texture.getWidth(), texture.getHeight(), Pixmap.Format.RGBA8888);
        if (sourcePixmap != null) {
            pixmap.drawPixmap(sourcePixmap, 0, 0);
        }

        copyOnWrite = true;
    }

    /**
     * Extract the CPU-side Pixmap backing a Texture, preparing its TextureData if needed.
     * Returns null if the pixel data cannot be read back (e.g. a compressed/GPU-only format).
     */
    private static Pixmap getPixmapFromTexture(Texture texture) {
        try {
            com.badlogic.gdx.graphics.TextureData data = texture.getTextureData();
            if (!data.isPrepared()) {
                data.prepare();
            }
            return data.consumePixmap();
        } catch (Exception e) {
            return null;
        }
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

        com.badlogic.gdx.graphics.g2d.BitmapFont bitmapFont = getFont().getBitmapFont();
        com.badlogic.gdx.graphics.g2d.BitmapFont.BitmapFontData data = bitmapFont.getData();

        String[] lines = string.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            drawGlyphLine(bitmapFont, lines[i], x, y + (int) Math.round(i * data.lineHeight));
        }

        invalidateTexture();
    }

    /**
     * Draw one line of text by blitting each character's glyph bitmap from the
     * font's own texture, tinted with the current color. y is the text baseline.
     */
    private void drawGlyphLine(com.badlogic.gdx.graphics.g2d.BitmapFont bitmapFont, String line, int x, int y) {
        com.badlogic.gdx.graphics.g2d.BitmapFont.BitmapFontData data = bitmapFont.getData();
        com.badlogic.gdx.utils.Array<com.badlogic.gdx.graphics.g2d.TextureRegion> regions = bitmapFont.getRegions();

        float cr = currentColor.getRed() / 255f;
        float cg = currentColor.getGreen() / 255f;
        float cb = currentColor.getBlue() / 255f;
        float ca = currentColor.getAlpha() / 255f;

        // Per-glyph metrics are texture pixels; the requested point size lives in
        // the font data's scale (BitmapFontData.setScale scales only the aggregate
        // metrics), so every glyph metric has to be scaled here as well.
        float scaleX = data.scaleX;
        float scaleY = data.scaleY;

        // Distance from the top of a line to its baseline: ascent = baseLine - capHeight,
        // so baseLine = ascent + capHeight. Both are already scaled.
        float baseLineFromTop = data.ascent + data.capHeight;

        float penX = x;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            com.badlogic.gdx.graphics.g2d.BitmapFont.Glyph glyph = data.getGlyph(ch);
            if (glyph == null || glyph.width <= 0 || glyph.height <= 0) {
                penX += glyph != null ? glyph.xadvance * scaleX : 0;
                continue;
            }

            com.badlogic.gdx.graphics.g2d.TextureRegion region = regions.get(glyph.page);
            Pixmap glyphSource = glyphAtlasPixmap(region.getTexture());
            if (glyphSource != null) {
                int srcX = region.getRegionX() + glyph.srcX;
                int srcY = region.getRegionY() + glyph.srcY;

                // glyph.yoffset is y-up and negative: -(glyph.height + offsetFromLineTop).
                // The pixmap is y-down, so the top of the glyph sits this far above the baseline.
                float aboveBaseline = baseLineFromTop + (glyph.yoffset + glyph.height) * scaleY;

                int dstX = Math.round(penX + glyph.xoffset * scaleX);
                int dstY = Math.round(y - aboveBaseline);
                int dstWidth = Math.max(1, Math.round(glyph.width * scaleX));
                int dstHeight = Math.max(1, Math.round(glyph.height * scaleY));

                blitGlyph(glyphSource, srcX, srcY, glyph.width, glyph.height,
                          dstX, dstY, dstWidth, dstHeight, cr, cg, cb, ca);
            }

            penX += glyph.xadvance * scaleX;
        }
    }

    /**
     * Alpha-blend one glyph's coverage (its alpha channel) as a solid tinted
     * color onto this image's pixmap, using the pixmap's own SourceOver blending.
     */
    private void blitGlyph(Pixmap source, int srcX, int srcY, int width, int height,
                            int dstX, int dstY, int dstWidth, int dstHeight,
                            float r, float g, float b, float a) {
        float contrast = coverageContrast(Math.max((float) dstWidth / width,
                                                   (float) dstHeight / height));

        for (int gy = 0; gy < dstHeight; gy++) {
            int py = dstY + gy;
            if (py < 0 || py >= pixmap.getHeight()) continue;

            // Sample the middle of the destination pixel so up- and down-scaling stay centred.
            float sourceY = srcY + (gy + 0.5f) * height / dstHeight - 0.5f;

            for (int gx = 0; gx < dstWidth; gx++) {
                int px = dstX + gx;
                if (px < 0 || px >= pixmap.getWidth()) continue;

                float sourceX = srcX + (gx + 0.5f) * width / dstWidth - 0.5f;

                int coverage = sampleCoverage(source, sourceX, sourceY,
                                              srcX, srcY, srcX + width - 1, srcY + height - 1,
                                              contrast);
                if (coverage == 0) continue;

                pixmap.setColor(r, g, b, a * (coverage / 255f));
                pixmap.drawPixel(px, py);
            }
        }
    }

    /**
     * Font atlases read back for glyph blitting, kept per texture. A file-backed
     * texture decodes its whole image on every consumePixmap() call, so without this
     * a single drawString() would decode (and leak) the atlas once per character.
     * Weak keys let the entry go as soon as the font itself is unreachable.
     */
    private static final Map<Texture, Pixmap> glyphAtlases = new java.util.WeakHashMap<>();

    private static Pixmap glyphAtlasPixmap(Texture texture) {
        Pixmap cached = glyphAtlases.get(texture);
        if (cached == null) {
            cached = getPixmapFromTexture(texture);
            if (cached != null) {
                glyphAtlases.put(texture, cached);
            }
        }
        return cached;
    }

    /**
     * Strongest contrast applied to upscaled glyph coverage. Past this the edge is
     * effectively hard and the glyph starts to show the stair-steps of the small
     * source bitmap, which looks worse than the halo it replaces.
     */
    private static final float MAX_COVERAGE_CONTRAST = 8f;

    /**
     * How wide, in destination pixels, an upscaled glyph edge is allowed to stay.
     * Two pixels was picked by rendering "BETUL" at size 160 (a 10.9x upscale of the
     * built-in 15px font) across a range of values: with no sharpening not even the
     * middle of a stem reached full colour (a scanline through the B read 60,60,60...
     * instead of 0), which is the "shadow" this fixes; at one pixel the edge is a
     * single-pixel step and the source bitmap's stair-steps start to show; two pixels
     * gives a solid interior with a 2-3 pixel roll-off and no visible stepping.
     */
    private static final float TARGET_EDGE_PIXELS = 2f;

    /**
     * How hard to push a glyph's antialiased edge back together after upscaling.
     *
     * <p>Bilinear sampling spreads one source edge pixel over roughly {@code upscale}
     * destination pixels, which is the "shadow" seen around big text drawn with the
     * built-in 15px bitmap font. Running the sampled coverage through a contrast curve
     * centred on 0.5 narrows that ramp to about {@code upscale / contrast} pixels, so
     * aiming for a ramp of ~2 destination pixels keeps a soft, non-blocky edge at any
     * scale. At scale 1 (a real TTF rendered by FreeType at the requested size) the
     * curve is the identity and the font's own antialiasing is left untouched -- which
     * is the normal case now that a real TTF ships in assets/fonts, since FreeType
     * renders each glyph at the size it is asked for.</p>
     */
    private static float coverageContrast(float upscale) {
        if (upscale <= TARGET_EDGE_PIXELS) return 1f;
        return Math.min(upscale / TARGET_EDGE_PIXELS, MAX_COVERAGE_CONTRAST);
    }

    /**
     * Bilinear read of a glyph's coverage (its alpha channel), clamped to the glyph's
     * own rectangle so neighbouring glyphs in the font atlas never bleed in, then
     * sharpened by {@code contrast} (1 leaves the bilinear result alone).
     */
    private static int sampleCoverage(Pixmap source, float x, float y,
                                      int minX, int minY, int maxX, int maxY,
                                      float contrast) {
        int x0 = (int) Math.floor(x);
        int y0 = (int) Math.floor(y);
        float fx = x - x0;
        float fy = y - y0;

        int c00 = coverageAt(source, x0, y0, minX, minY, maxX, maxY);
        int c10 = coverageAt(source, x0 + 1, y0, minX, minY, maxX, maxY);
        int c01 = coverageAt(source, x0, y0 + 1, minX, minY, maxX, maxY);
        int c11 = coverageAt(source, x0 + 1, y0 + 1, minX, minY, maxX, maxY);

        float top = c00 + (c10 - c00) * fx;
        float bottom = c01 + (c11 - c01) * fx;
        float coverage = top + (bottom - top) * fy;

        if (contrast != 1f) {
            // a' = clamp((a - 0.5) * k + 0.5, 0, 1), on the 0..255 coverage scale.
            coverage = (coverage - 127.5f) * contrast + 127.5f;
            if (coverage <= 0f) return 0;
            if (coverage >= 255f) return 255;
        }
        return Math.round(coverage);
    }

    private static int coverageAt(Pixmap source, int x, int y,
                                  int minX, int minY, int maxX, int maxY) {
        int cx = Math.max(minX, Math.min(maxX, x));
        int cy = Math.max(minY, Math.min(maxY, y));
        if (cx < 0 || cy < 0 || cx >= source.getWidth() || cy >= source.getHeight()) return 0;
        return source.getPixel(cx, cy) & 0xFF; // alpha channel = glyph coverage
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
            FileHandle fileHandle = null;
            
            // Use dynamic path resolution from GreenfootProjectConfig
            String[] pathsToTry = id.qiupi3.greenfoot.GreenfootProjectConfig.getAllPossiblePaths(filename);
            
            for (String path : pathsToTry) {
                fileHandle = Gdx.files.internal(path);
                if (fileHandle.exists()) {
                    break;
                }
            }
            
            if (fileHandle != null && fileHandle.exists()) {
                Pixmap loadedPixmap = new Pixmap(fileHandle);
                
                // Ensure consistent format (RGBA8888) for all loaded images
                if (loadedPixmap.getFormat() != Pixmap.Format.RGBA8888) {
                    pixmap = new Pixmap(loadedPixmap.getWidth(), loadedPixmap.getHeight(), Pixmap.Format.RGBA8888);
                    pixmap.drawPixmap(loadedPixmap, 0, 0);
                    loadedPixmap.dispose();
                } else {
                    pixmap = loadedPixmap;
                }
                
                // Ensure any existing texture is invalidated
                invalidateTexture();
            } else {
                // Create a detailed error message showing all attempted paths
                StringBuilder pathList = new StringBuilder();
                for (int i = 0; i < pathsToTry.length; i++) {
                    if (i > 0) pathList.append(", ");
                    pathList.append(pathsToTry[i]);
                }
                
                throw new IllegalArgumentException("Could not find image file: " + filename + 
                    " (tried paths: " + pathList.toString() + ")");
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
        
        // Always use RGBA8888 format for consistency
        pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        pixmap.setColor(0, 0, 0, 0); // Transparent background
        pixmap.fill();
        copyOnWrite = false;
        
        // Ensure any existing texture is invalidated
        invalidateTexture();
    }

    private static void copyStates(GreenfootImage src, GreenfootImage dst) {
        dst.imageFileName = src.imageFileName;
        dst.currentColor = src.currentColor;
        dst.currentFont = src.currentFont;
        dst.transparency = src.transparency;
    }
    
    private boolean textureNeedsUpdate = false; // Flag to track if texture needs to be regenerated
    
    public Texture getTexture() {
        // A disposed texture keeps a non-null reference but its GL handle is 0 and it
        // draws as a black rectangle. Rebuild it from the pixmap instead of handing
        // back a dead texture.
        if (texture != null && texture.getTextureObjectHandle() == 0) {
            texture = null;
            textureNeedsUpdate = true;
        }

        if (texture == null || textureNeedsUpdate) {
            if (pixmap != null) {
                try {
                    // Dispose old texture if it exists
                    if (texture != null) {
                        texture.dispose();
                    }
                    
                    // Ensure pixmap is in correct format before creating texture
                    if (pixmap.getFormat() != Pixmap.Format.RGBA8888) {
                        // Convert to RGBA8888 if needed
                        Pixmap convertedPixmap = new Pixmap(pixmap.getWidth(), pixmap.getHeight(), Pixmap.Format.RGBA8888);
                        convertedPixmap.drawPixmap(pixmap, 0, 0);
                        texture = new Texture(convertedPixmap);
                        convertedPixmap.dispose();
                    } else {
                        texture = new Texture(pixmap);
                    }
                    
                    // Set texture filtering to prevent sampling issues
                    if (texture != null) {
                        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
                        // Ensure texture wrapping is set properly to avoid sampling issues
                        texture.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
                    }
                    
                    textureNeedsUpdate = false; // Reset the flag
                } catch (Exception e) {
                    System.err.println("Failed to create texture from pixmap: " + e.getMessage());
                    // Create a fallback 1x1 white texture to prevent crashes
                    Pixmap fallbackPixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
                    fallbackPixmap.setColor(1, 1, 1, 1);
                    fallbackPixmap.fill();
                    texture = new Texture(fallbackPixmap);
                    texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
                    texture.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
                    fallbackPixmap.dispose();
                    textureNeedsUpdate = false;
                }
            }
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
        textureNeedsUpdate = false;
    }
    
    private void ensureWritableImage() {
        if (copyOnWrite && pixmap != null) {
            Pixmap newPixmap = new Pixmap(pixmap.getWidth(), pixmap.getHeight(), pixmap.getFormat());
            newPixmap.drawPixmap(pixmap, 0, 0);
            pixmap = newPixmap;
            copyOnWrite = false;
            
            // Mark texture for update instead of immediate disposal
            textureNeedsUpdate = true;
        }
    }

    Pixmap dbgPixmap() {
        return pixmap;
    }

    private void invalidateTexture() {
        // Instead of immediately disposing, just mark texture as needing update
        // This prevents excessive dispose-recreate cycles
        textureNeedsUpdate = true;
    }
}
