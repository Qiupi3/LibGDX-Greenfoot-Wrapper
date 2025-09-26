package greenfoot;

/**
 * Greenfoot Color class with compatibility for LibGDX.
 */
public class Color {
    
    // Color components (0-255)
    private final int red;
    private final int green;
    private final int blue;
    private final int transparency;
    
    // Common colors
    public static final Color BLACK = new Color(0, 0, 0);
    public static final Color BLUE = new Color(0, 0, 255);
    public static final Color CYAN = new Color(0, 255, 255);
    public static final Color DARK_GRAY = new Color(64, 64, 64);
    public static final Color GRAY = new Color(128, 128, 128);
    public static final Color GREEN = new Color(0, 255, 0);
    public static final Color LIGHT_GRAY = new Color(192, 192, 192);
    public static final Color MAGENTA = new Color(255, 0, 255);
    public static final Color ORANGE = new Color(255, 200, 0);
    public static final Color PINK = new Color(255, 175, 175);
    public static final Color RED = new Color(255, 0, 0);
    public static final Color WHITE = new Color(255, 255, 255);
    public static final Color YELLOW = new Color(255, 255, 0);
    
    /**
     * Create a color with RGB values (0-255).
     */
    public Color(int red, int green, int blue) {
        this(red, green, blue, 255);
    }
    
    /**
     * Create a color with RGBA values (0-255).
     */
    public Color(int red, int green, int blue, int transparency) {
        this.red = clamp(red);
        this.green = clamp(green);
        this.blue = clamp(blue);
        this.transparency = clamp(transparency);
    }

    public Color brighter() {
        int r = red;
        int g = green;
        int b = blue;
        int t = transparency;

        // Increase brightness by 10%, ensuring we don't exceed 255
        r = Math.min(255, (int)(r * 1.1));
        g = Math.min(255, (int)(g * 1.1));
        b = Math.min(255, (int)(b * 1.1));

        return new Color(r, g, b, t);
    }

    public Color darker() {
        int r = red;
        int g = green;
        int b = blue;
        int t = transparency;

        // Decrease brightness by 10%, ensuring we don't go below 0
        r = Math.max(0, (int)(r * 0.9));
        g = Math.max(0, (int)(g * 0.9));
        b = Math.max(0, (int)(b * 0.9));

        return new Color(r, g, b, t);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Color)) return false;
        Color other = (Color) obj;
        return red == other.red && green == other.green && 
               blue == other.blue && transparency == other.transparency;
    }
    
    /**
     * Get red component (0-255).
     */
    public int getRed() {
        return red;
    }
    
    /**
     * Get green component (0-255).
     */
    public int getGreen() {
        return green;
    }
    
    /**
     * Get blue component (0-255).
     */
    public int getBlue() {
        return blue;
    }
    
    /**
     * Get alpha component (0-255).
     */
    public int getAlpha() {
        return transparency;
    }

    @Override
    public int hashCode() {
        return red << 24 | green << 16 | blue << 8 | transparency;
    }
    
    @Override
    public String toString() {
        return "Color[r=" + red + ",g=" + green + ",b=" + blue + ",t=" + transparency + "]";
    }
    
    /**
     * Convert to LibGDX Color.
     */
    public com.badlogic.gdx.graphics.Color toLibGDXColor() {
        return new com.badlogic.gdx.graphics.Color(
            red / 255f,
            green / 255f,
            blue / 255f,
            transparency / 255f
        );
    }
    
    /**
     * Create from LibGDX Color.
     */
    public static Color fromLibGDXColor(com.badlogic.gdx.graphics.Color libgdxColor) {
        return new Color(
            (int)(libgdxColor.r * 255),
            (int)(libgdxColor.g * 255),
            (int)(libgdxColor.b * 255),
            (int)(libgdxColor.a * 255)
        );
    }
    
    private int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
