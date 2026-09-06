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
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;

/**
 * A LibGDX-based representation of a Font. The Font can be used to write text on the screen.
 * This implementation uses LibGDX BitmapFont while maintaining Greenfoot API compatibility.
 *
 * This class re-implements greenfoot.Font to provide a LibGDX backend,
 * mainly to allow Greenfoot projects to run on LibGDX (especially to export into
 * mobile devices and other platforms).
 * 
 * Inspired by the original Greenfoot project (GPLv2+ with Classpath Exception).
 * Read the original documentation at
 * https://www.greenfoot.org/files/javadoc/greenfoot/Font.html
 * 
 * @author Fabio Heday (Original Greenfoot version's author)
 * @author Amjad Altadmri (Original Greenfoot version's author)
 * 
 * @modified-by Qiupi3 (LibGDX wrapper implementation)
 * @modified-by DavidsonRafaelK
 * @version 1.0
 */
public class Font {
    private final BitmapFont bitmapFont;
    private final String name;
    private final boolean bold;
    private final boolean italic;
    private final int size;

    /**
     * Creates a Greenfoot font based on a LibGDX BitmapFont
     *
     * @param bitmapFont The LibGDX BitmapFont to wrap
     * @param name The font name
     * @param bold Whether the font is bold
     * @param italic Whether the font is italic
     * @param size The font size
     */
    Font(BitmapFont bitmapFont, String name, boolean bold, boolean italic, int size) {
        this.bitmapFont = bitmapFont;
        this.name = name;
        this.bold = bold;
        this.italic = italic;
        this.size = size;
    }

    /**
     * Creates a font from the specified font name, size and style.
     *
     * @param name The font name
     * @param bold True if the font is meant to be bold
     * @param italic True if the font is meant to be italic
     * @param size The size of the font
     */
    public Font(String name, boolean bold, boolean italic, int size) {
        this.name = name;
        this.bold = bold;
        this.italic = italic;
        this.size = size;
        this.bitmapFont = createBitmapFont(name, bold, italic, size);
    }

    /**
     * Creates a sans serif font with the specified size and style.
     *
     * @param bold True if the font is meant to be bold
     * @param italic True if the font is meant to be italic
     * @param size The size of the font
     */
    public Font(boolean bold, boolean italic, int size) {
        this("SansSerif", bold, italic, size);
    }

    /**
     * Creates a font from the specified font name and size.
     *
     * @param name The font name
     * @param size The size of the font
     */
    public Font(String name, int size) {
        this(name, false, false, size);
    }

    /**
     * Creates a sans serif font of a given size.
     *
     * @param size The size of the font
     */
    public Font(int size) {
        this(false, false, size);
    }

    /**
     * Indicates whether or not this Font style is plain.
     *
     * @return true if this font style is plain; false otherwise
     */
    public boolean isPlain() {
        return !bold && !italic;
    }

    /**
     * Indicates whether or not this Font style is bold.
     *
     * @return true if this font style is bold; false otherwise
     */
    public boolean isBold() {
        return bold;
    }

    /**
     * Indicates whether or not this Font style is italic.
     *
     * @return true if this font style is italic; false otherwise
     */
    public boolean isItalic() {
        return italic;
    }

    /**
     * Returns the logical name of this font.
     *
     * @return a <code>String</code> representing the logical name of this font.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the point size of this font, rounded to an integer.
     *
     * @return the point size of this font in 1/72 of an inch units.
     */
    public int getSize() {
        return size;
    }

    /**
     * Creates a new <code>Font</code> object by cloning the current
     * one and then applying a new size to it.
     *
     * @param size the size for the new <code>Font</code>.
     * @return a new <code>Font</code> object.
     */
    public Font deriveFont(int size) {
        return new Font(this.name, this.bold, this.italic, size);
    }

    /**
     * Creates a new <code>Font</code> object by cloning the current
     * one and then applying a new size to it.
     *
     * @param size the size for the new <code>Font</code>.
     * @return a new <code>Font</code> object.
     */
    public Font deriveFont(float size) {
        return new Font(this.name, this.bold, this.italic, (int) size);
    }

    /**
     * * Determines whether another object is equal to this font.
     *
     * @param obj the object to test for equality with this font
     * @return true if the fonts are the same; false otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) return false;
        Font font = (Font) obj;
        return bold == font.bold && italic == font.italic && size == font.size && name.equals(font.name);
    }

    /**
     * Returns a hashcode for this font.
     *
     * @return a hashcode value for this font.
     */
    @Override
    public int hashCode() {
        return name.hashCode() ^ size ^ (bold ? 1 : 0) ^ (italic ? 2 : 0);
    }

    /**
     * Return a text representation of the font.
     * @return Details of the font
     */
    @Override
    public String toString() {
        return "Font{name='" + name + "', size=" + size + ", bold=" + bold + ", italic=" + italic + '}';
    }

    /**
     * Return the internal Font object representing the Greenfoot.Font.
     *
     * @return the BitmapFont object for LibGDX compatibility
     */
    public BitmapFont getFontObject() {
        return this.bitmapFont;
    }

    /**
     * Get the LibGDX BitmapFont instance for rendering.
     * This method is for internal use by the LibGDX wrapper.
     */
    public BitmapFont getBitmapFont() {
        return bitmapFont;
    }

    /**
     * Creates a BitmapFont for the given style and size.
     *
     * <p>Preference order:</p>
     * <ol>
     *   <li>A real TrueType/OpenType font found in the assets (see {@link #findFontFile}),
     *       rendered by FreeType at exactly the requested size. This is the only path
     *       that gives crisp text at large sizes.</li>
     *   <li>LibGDX's built-in 15px Arial bitmap font, scaled up. Readable, but every
     *       size above ~15px is an upscale of a small bitmap, so edges stay soft.</li>
     * </ol>
     *
     * <p><b>To get crisp text, drop a .ttf into the assets folder.</b> The simplest
     * choice is <code>assets/fonts/default.ttf</code>; see {@link #fontDirectories()}
     * and {@link #fontCandidatePaths} for every location and file name that is picked
     * up automatically (including per-project <code>&lt;project&gt;/fonts/</code> and
     * style variants such as <code>default-Bold.ttf</code>). No font is shipped with
     * the wrapper, so this is a deliberate, per-project choice.</p>
     */
    private static BitmapFont createBitmapFont(String name, boolean bold, boolean italic, int size) {
        FontFile source = findFontFile(name, bold, italic);
        if (source != null) {
            BitmapFont font = generateFreeTypeFont(source, bold, size);
            if (font != null) {
                return font;
            }
        }

        // Fall back to LibGDX's built-in bitmap font, scaled so its capital letters
        // are as tall as AWT's sans-serif at this point size (cap height is about
        // 0.75 em there), which is what the original Greenfoot draws with.
        BitmapFont defaultFont = new BitmapFont();
        defaultFont.getData().setScale(capHeightScale(defaultFont, size));
        return defaultFont;
    }

    /**
     * AWT's sans-serif draws capital letters about this fraction of the point size tall.
     * Every backend here is matched to it so that text keeps the size a Greenfoot
     * project asked for, whatever font file happens to be available.
     */
    private static final float AWT_CAP_HEIGHT_RATIO = 0.75f;

    /**
     * Scale factor that makes the font's capital letters {@code 0.75 * size} pixels tall.
     */
    private static float capHeightScale(BitmapFont font, int size) {
        // BitmapFontData.capHeight is the inked height of a capital letter, with the
        // atlas padding already taken off, so it is what has to line up with AWT.
        float capHeight = font.getData().capHeight;
        if (capHeight > 0) {
            return AWT_CAP_HEIGHT_RATIO * size / capHeight;
        }
        return size / 15f;
    }

    /**
     * Render a font file with FreeType at the requested size, or null if that fails.
     *
     * <p>FreeType's {@code size} is the em size in pixels, but Greenfoot sizes are
     * matched on cap height (see {@link #AWT_CAP_HEIGHT_RATIO}), and the ratio between
     * the two differs per typeface. So the font is generated once, measured, and
     * re-generated at a corrected em size. Correcting the em size rather than calling
     * {@code setScale} matters: a scaled font is resampled when it is drawn, which is
     * exactly the blur this path exists to avoid.</p>
     */
    private static BitmapFont generateFreeTypeFont(FontFile source, boolean bold, int size) {
        FreeTypeFontGenerator generator = null;
        try {
            generator = new FreeTypeFontGenerator(source.handle);
            // Only synthesise bold when the file itself is not already a bold face.
            boolean synthesiseBold = bold && !source.styled;

            BitmapFont font = generateAt(generator, size, synthesiseBold);
            float capHeight = font.getData().capHeight;
            if (capHeight > 0) {
                int corrected = Math.round(size * (AWT_CAP_HEIGHT_RATIO * size) / capHeight);
                if (corrected > 0 && corrected != size) {
                    BitmapFont resized = generateAt(generator, corrected, synthesiseBold);
                    font.dispose();
                    font = resized;
                }
            }
            return font;
        } catch (Throwable t) {
            // Missing FreeType natives, an unreadable file, an atlas that will not fit:
            // any of these just means the built-in bitmap font has to do.
            log("could not render " + source.handle.path() + " (" + t + "), using the built-in font");
            return null;
        } finally {
            if (generator != null) {
                generator.dispose();
            }
        }
    }

    private static BitmapFont generateAt(FreeTypeFontGenerator generator, int pixelSize, boolean synthesiseBold) {
        FreeTypeFontParameter parameter = new FreeTypeFontParameter();
        parameter.size = Math.max(1, pixelSize);
        if (synthesiseBold) {
            // FreeType exposes no "make this face bold" switch, but an outline drawn in
            // the glyph's own colour thickens the strokes, which is the usual faux bold.
            // A real bold file (default-Bold.ttf, Arial Bold.ttf, ...) is always better
            // and is preferred by findFontFile() when one exists.
            parameter.borderWidth = Math.max(0.5f, parameter.size * 0.03f);
            parameter.borderColor = parameter.color;
        }
        // Note on italic: neither FreeTypeFontParameter nor BitmapFont can shear a face,
        // so italic is honoured only by finding an italic font file. A plain face asked
        // for in italic renders upright rather than not at all.
        return generator.generateFont(parameter);
    }

    /** A font file plus whether its own name already carries the requested bold/italic style. */
    private static final class FontFile {
        final com.badlogic.gdx.files.FileHandle handle;
        final boolean styled;

        FontFile(com.badlogic.gdx.files.FileHandle handle, boolean styled) {
            this.handle = handle;
            this.styled = styled;
        }
    }

    /**
     * Resolved font files, keyed by style, so the candidate scan below runs once per
     * style rather than once per Font instance. A null value is a remembered miss.
     */
    private static final java.util.Map<String, FontFile> FONT_FILE_CACHE = new java.util.HashMap<>();

    /**
     * Look for a font file the project has supplied. Nothing is bundled with the
     * wrapper, so this returns null until a user drops one into the assets.
     */
    private static FontFile findFontFile(String name, boolean bold, boolean italic) {
        String key = name + "|" + bold + "|" + italic;
        synchronized (FONT_FILE_CACHE) {
            if (FONT_FILE_CACHE.containsKey(key)) {
                return FONT_FILE_CACHE.get(key);
            }
            FontFile found = searchFontFile(name, bold, italic);
            FONT_FILE_CACHE.put(key, found);
            if (found != null) {
                log("using font file " + found.handle.path() + " for " + name
                        + (bold ? " bold" : "") + (italic ? " italic" : ""));
            } else {
                log("no .ttf/.otf found for " + name + "; falling back to the built-in bitmap font."
                        + " Drop one at assets/fonts/default.ttf for crisp text.");
            }
            return found;
        }
    }

    private static FontFile searchFontFile(String name, boolean bold, boolean italic) {
        if (Gdx.files == null) {
            return null;
        }

        // First pass: exact file names, style variants before plain ones.
        for (String path : fontCandidatePaths(name, bold, italic)) {
            com.badlogic.gdx.files.FileHandle handle = internalIfExists(path);
            if (handle != null) {
                return new FontFile(handle, matchesStyle(handle.name(), bold, italic));
            }
        }

        // Second pass: whatever font happens to sit in one of the font folders, so a
        // project can just drop in "MyCoolFont.ttf" without renaming it.
        for (String directory : fontDirectories()) {
            FontFile scanned = firstFontIn(directory, bold, italic);
            if (scanned != null) {
                return scanned;
            }
        }
        return null;
    }

    /**
     * Folders searched for fonts, nearest-to-the-project first. The "assets/" variants
     * cover desktop runs whose working directory is the repository root rather than the
     * assets folder; on Android they simply do not exist.
     */
    private static String[] fontDirectories() {
        String project = null;
        try {
            project = id.qiupi3.greenfoot.GreenfootProjectConfig.getUserProjectFolder();
        } catch (Throwable ignored) {
            // Project detection is best-effort; the shared folders below still work.
        }

        java.util.List<String> directories = new java.util.ArrayList<>();
        if (project != null && !project.isEmpty()) {
            directories.add(project + "/fonts");
            directories.add(project);
        }
        directories.add("fonts");
        directories.add("");

        java.util.List<String> withAssetsPrefix = new java.util.ArrayList<>(directories);
        for (String directory : directories) {
            withAssetsPrefix.add(directory.isEmpty() ? "assets" : "assets/" + directory);
        }
        return withAssetsPrefix.toArray(new String[0]);
    }

    /** Every exact file name that is accepted for this style, most specific first. */
    private static java.util.List<String> fontCandidatePaths(String name, boolean bold, boolean italic) {
        java.util.List<String> bases = new java.util.ArrayList<>();
        if (name != null && !name.trim().isEmpty()) {
            bases.add(name.trim());
        }
        bases.add("default");

        java.util.List<String> paths = new java.util.ArrayList<>();
        for (String suffix : styleSuffixes(bold, italic)) {
            for (String directory : fontDirectories()) {
                String prefix = directory.isEmpty() ? "" : directory + "/";
                for (String base : bases) {
                    paths.add(prefix + base + suffix + ".ttf");
                    paths.add(prefix + base + suffix + ".otf");
                }
            }
        }
        return paths;
    }

    private static String[] styleSuffixes(boolean bold, boolean italic) {
        if (bold && italic) {
            return new String[]{"-BoldItalic", " Bold Italic", "-bolditalic", "_BoldItalic", ""};
        }
        if (bold) {
            return new String[]{"-Bold", " Bold", "-bold", "_Bold", ""};
        }
        if (italic) {
            return new String[]{"-Italic", " Italic", "-italic", "_Italic", ""};
        }
        return new String[]{"", "-Regular", " Regular", "-regular"};
    }

    private static com.badlogic.gdx.files.FileHandle internalIfExists(String path) {
        try {
            com.badlogic.gdx.files.FileHandle handle = Gdx.files.internal(path);
            if (handle.exists() && !handle.isDirectory()) {
                return handle;
            }
        } catch (Throwable ignored) {
            // An unreadable path is simply not a candidate.
        }
        return null;
    }

    /** First font file in a directory, preferring one whose name matches the style. */
    private static FontFile firstFontIn(String directory, boolean bold, boolean italic) {
        try {
            com.badlogic.gdx.files.FileHandle handle = Gdx.files.internal(directory);
            if (!handle.exists()) {
                return null;
            }
            com.badlogic.gdx.files.FileHandle fallback = null;
            for (com.badlogic.gdx.files.FileHandle child : handle.list()) {
                String extension = child.extension().toLowerCase(java.util.Locale.ROOT);
                if (!extension.equals("ttf") && !extension.equals("otf")) {
                    continue;
                }
                if (matchesStyle(child.name(), bold, italic)) {
                    return new FontFile(child, true);
                }
                if (fallback == null && !hasStyleWord(child.name())) {
                    fallback = child;
                }
            }
            if (fallback != null) {
                return new FontFile(fallback, false);
            }
        } catch (Throwable ignored) {
            // Directory listing is not available everywhere; that is not an error.
        }
        return null;
    }

    private static boolean matchesStyle(String fileName, boolean bold, boolean italic) {
        String lower = fileName.toLowerCase(java.util.Locale.ROOT);
        boolean isBold = lower.contains("bold");
        boolean isItalic = lower.contains("italic") || lower.contains("oblique");
        return isBold == bold && isItalic == italic && (bold || italic);
    }

    private static boolean hasStyleWord(String fileName) {
        String lower = fileName.toLowerCase(java.util.Locale.ROOT);
        return lower.contains("bold") || lower.contains("italic") || lower.contains("oblique");
    }

    private static void log(String message) {
        if (Gdx.app != null) {
            Gdx.app.log("GreenfootFont", message);
        } else {
            System.out.println("GreenfootFont: " + message);
        }
    }
}
