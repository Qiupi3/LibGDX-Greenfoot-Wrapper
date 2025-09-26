package greenfoot;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;

/**
 * A LibGDX-based representation of a Font. The Font can be used to write text on the screen.
 * This implementation uses LibGDX BitmapFont while maintaining Greenfoot API compatibility.
 *
 * @author Fabio Heday
 * @author Amjad Altadmri
 */
public class Font
{
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
    Font(BitmapFont bitmapFont, String name, boolean bold, boolean italic, int size)
    {
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
    public Font(String name, boolean bold, boolean italic, int size)
    {
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
    public Font(boolean bold, boolean italic, int size)
    {
        this("SansSerif", bold, italic, size);
    }

    /**
     * Creates a font from the specified font name and size.
     *
     * @param name The font name
     * @param size The size of the font
     */
    public Font(String name, int size)
    {
        this(name, false, false, size);
    }

    /**
     * Creates a sans serif font of a given size.
     *
     * @param size The size of the font
     */
    public Font(int size)
    {
        this(false, false, size);
    }

    /**
     * Indicates whether or not this Font style is plain.
     *
     * @return true if this font style is plain; false otherwise
     */
    public boolean isPlain()
    {
        return !bold && !italic;
    }

    /**
     * Indicates whether or not this Font style is bold.
     *
     * @return true if this font style is bold; false otherwise
     */
    public boolean isBold()
    {
        return bold;
    }

    /**
     * Indicates whether or not this Font style is italic.
     *
     * @return true if this font style is italic; false otherwise
     */
    public boolean isItalic()
    {
        return italic;
    }

    /**
     * Returns the logical name of this font.
     *
     * @return a <code>String</code> representing the logical name of this font.
     */
    public String getName()
    {
        return name;
    }

    /**
     * Returns the point size of this font, rounded to an integer.
     *
     * @return the point size of this font in 1/72 of an inch units.
     */
    public int getSize()
    {
        return size;
    }

    /**
     * Creates a new <code>Font</code> object by cloning the current
     * one and then applying a new size to it.
     *
     * @param size the size for the new <code>Font</code>.
     * @return a new <code>Font</code> object.
     */
    public Font deriveFont(int size)
    {
        return new Font(this.name, this.bold, this.italic, size);
    }

    /**
     * Creates a new <code>Font</code> object by cloning the current
     * one and then applying a new size to it.
     *
     * @param size the size for the new <code>Font</code>.
     * @return a new <code>Font</code> object.
     */
    public Font deriveFont(float size)
    {
        return new Font(this.name, this.bold, this.italic, (int) size);
    }

    /**
     * * Determines whether another object is equal to this font.
     *
     * @param obj the object to test for equality with this font
     * @return true if the fonts are the same; false otherwise.
     */
    @Override
    public boolean equals(Object obj)
    {
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
    public int hashCode()
    {
        return name.hashCode() ^ size ^ (bold ? 1 : 0) ^ (italic ? 2 : 0);
    }

    /**
     * Return a text representation of the font.
     * @return Details of the font
     */
    @Override
    public String toString()
    {
        return "Font{name='" + name + "', size=" + size + ", bold=" + bold + ", italic=" + italic + '}';
    }

    /**
     * Return the internal Font object representing the Greenfoot.Font.
     *
     * @return the BitmapFont object for LibGDX compatibility
     */
    public BitmapFont getFontObject()
    {
        return this.bitmapFont;
    }

    /**
     * Get the LibGDX BitmapFont instance for rendering.
     * This method is for internal use by the LibGDX wrapper.
     */
    public BitmapFont getBitmapFont()
    {
        return bitmapFont;
    }

    /**
     * Creates a BitmapFont from the given parameters using LibGDX.
     * Uses default font if FreeType is not available.
     */
    private static BitmapFont createBitmapFont(String name, boolean bold, boolean italic, int size)
    {
        try {
            // Try to use FreeType font generator for better quality
            FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/default.ttf"));
            FreeTypeFontParameter parameter = new FreeTypeFontParameter();
            parameter.size = size;
            BitmapFont font = generator.generateFont(parameter);
            generator.dispose();
            return font;
        } catch (Exception e) {
            // Fall back to LibGDX's default font with scaling
            BitmapFont defaultFont = new BitmapFont();
            defaultFont.getData().setScale(size / 15.0f); // LibGDX default font is about 15px
            return defaultFont;
        }
    }

}
