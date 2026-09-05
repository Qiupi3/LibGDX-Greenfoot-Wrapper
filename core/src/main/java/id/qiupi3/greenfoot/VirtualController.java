package id.qiupi3.greenfoot;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The on-screen touch pad.
 *
 * This lives in core rather than in the Android module on purpose: the pad is plain
 * LibGDX drawing and touch polling, so the same code runs on a phone and on the
 * desktop. On desktop it stays off unless {@code -Dgreenfoot.virtualController=true}
 * is set, which makes the pad testable without a device.
 *
 * <h3>What a button is</h3>
 * A button is a {@link Slot} - a position in the layout - bound to a Greenfoot key
 * name such as {@code "w"} or {@code "space"}. Pressing it makes
 * {@link greenfoot.Greenfoot#isKeyDown(String)} report that key as held, and the
 * button draws its own binding as its label, so remapping a slot relabels it.
 *
 * <h3>Touch ownership</h3>
 * A pointer that goes down on a button belongs to the pad for as long as it is held,
 * and {@link #isPointerConsumed(int)} reports it. Greenfoot's mouse polling skips
 * those pointers, which is what stops a tap on the pad from also clicking whatever
 * the game happens to be drawing underneath it. The special full-screen tap
 * deliberately does NOT consume, so tapping the world still works normally.
 *
 * @author Qiupi3
 * @version 1.0
 */
public class VirtualController {

    /** How many buttons the pad shows, and where. */
    public enum Layout {
        /** Four on the left (a d-pad) and four on the right. */
        FOUR_FOUR("4-4"),
        /** Four on the left (a d-pad) and two stacked on the right. */
        FOUR_TWO("4-2"),
        /** Four on the left (a d-pad) and a single button on the right. */
        FOUR_ONE("4-1"),
        /** Two side by side on each side: left/right only, no up or down. */
        TWO_TWO("2-2");

        private final String id;

        Layout(String id) {
            this.id = id;
        }

        /** The short name accepted by {@link #fromId(String)}, e.g. "4-2". */
        public String getId() {
            return id;
        }

        /**
         * Parse a layout from "4-4"/"44"/"four_four" style text.
         *
         * @param text the layout name; null or unrecognised yields null
         * @return the layout, or null if the text names none
         */
        public static Layout fromId(String text) {
            if (text == null) {
                return null;
            }
            String key = text.trim().toLowerCase().replace("_", "").replace("-", "").replace(" ", "");
            if (key.equals("44") || key.equals("fourfour")) {
                return FOUR_FOUR;
            }
            if (key.equals("42") || key.equals("fourtwo")) {
                return FOUR_TWO;
            }
            if (key.equals("41") || key.equals("fourone")) {
                return FOUR_ONE;
            }
            if (key.equals("22") || key.equals("twotwo")) {
                return TWO_TWO;
            }
            return null;
        }
    }

    /** A position on the pad that a key can be bound to. */
    public enum Slot {
        LEFT_UP("left_up"),
        LEFT_LEFT("left_left"),
        LEFT_DOWN("left_down"),
        LEFT_RIGHT("left_right"),
        RIGHT_1("right_1"),
        RIGHT_2("right_2"),
        RIGHT_3("right_3"),
        RIGHT_4("right_4"),
        /** Not drawn: a tap anywhere that is not on a button. */
        SPECIAL("special");

        private final String id;

        Slot(String id) {
            this.id = id;
        }

        /** The id used by the string-based API, e.g. "left_up". */
        public String getId() {
            return id;
        }

        /**
         * Parse a slot id. Accepts the plain id ("right_1"), a "btn_" prefix
         * ("btn_right_1"), and the d-pad aliases "up"/"down"/"left"/"right".
         *
         * @param text the slot id; null or unrecognised yields null
         * @return the slot, or null if the text names none
         */
        public static Slot fromId(String text) {
            if (text == null) {
                return null;
            }
            String key = text.trim().toLowerCase().replace("-", "_").replace(" ", "");
            if (key.startsWith("btn_")) {
                key = key.substring(4);
            }
            switch (key) {
                case "up": return LEFT_UP;
                case "down": return LEFT_DOWN;
                case "left": return LEFT_LEFT;
                case "right": return LEFT_RIGHT;
                default: break;
            }
            for (Slot slot : values()) {
                if (slot.id.equals(key)) {
                    return slot;
                }
            }
            return null;
        }
    }

    /** Slots drawn on the left, in every layout. */
    private static final Slot[] LEFT_SLOTS = {
        Slot.LEFT_UP, Slot.LEFT_LEFT, Slot.LEFT_DOWN, Slot.LEFT_RIGHT
    };

    /** Slots drawn on the right; the last two are used by the 4-4 layout only. */
    private static final Slot[] RIGHT_SLOTS = {
        Slot.RIGHT_1, Slot.RIGHT_2, Slot.RIGHT_3, Slot.RIGHT_4
    };

    private static final int MAX_POINTERS = 10;

    /**
     * Half the gap between the two stacked right-hand buttons, in radii. Kept above
     * {@link #TOUCH_RADIUS_FACTOR} so their touch areas cannot overlap.
     */
    private static final float STACK_GAP = 1.2f;

    /**
     * Half the gap between two side-by-side buttons, in radii. Same rule as
     * {@link #STACK_GAP}: wide enough that the touch areas stay apart.
     */
    private static final float PAIR_GAP = 1.2f;

    /**
     * Touch areas are slightly larger than the drawn circle, which is what makes a pad
     * feel reliable under a thumb. Kept below 1.2 so that neighbouring buttons - whose
     * centres are 1.6 radii apart on the diagonal - never both match a touch.
     */
    private static final float TOUCH_RADIUS_FACTOR = 1.12f;

    // ---- configuration ----
    private Layout layout = Layout.FOUR_TWO;
    private final Map<Slot, String> bindings = new EnumMap<>(Slot.class);
    private final Map<Slot, Boolean> slotVisible = new EnumMap<>(Slot.class);
    private boolean manuallyVisible = true;
    private boolean autoHidden;
    private final java.util.List<Class<?>> hideFor = new java.util.ArrayList<>();
    private final java.util.List<String> hideForNames = new java.util.ArrayList<>();
    private boolean resolvedNames = true;
    private float opacity = 0.72f;
    private float scale = 1.0f;
    /** When true, every press is logged - set "debug = true" in controller.properties. */
    private boolean debug;

    // ---- per-frame state ----
    private final Map<Slot, Boolean> pressed = new EnumMap<>(Slot.class);
    private final Map<Slot, float[]> geometry = new LinkedHashMap<>();
    /** Bounding areas of each button cluster: {minX, minY, maxX, maxY}, y-up. */
    private final java.util.List<float[]> clusterAreas = new java.util.ArrayList<>();
    private final boolean[] pointerConsumed = new boolean[MAX_POINTERS];
    /** Pointers that were already down on the previous frame. */
    private final boolean[] pointerWasDown = new boolean[MAX_POINTERS];
    /**
     * Pointers the pad is allowed to act on. A pointer is adopted only if it went down
     * while the pad was visible, so a finger that was already touching the screen when
     * the pad appeared is ignored until it is lifted.
     */
    private final boolean[] pointerAdopted = new boolean[MAX_POINTERS];
    private boolean specialPressed;
    private float radius;

    // ---- drawing ----
    private SpriteBatch batch;
    private Texture buttonTexture;
    private Texture buttonPressedTexture;
    private BitmapFont font;
    private int fontSize = -1;
    private final GlyphLayout glyphLayout = new GlyphLayout();
    private final Matrix4 projection = new Matrix4();
    private boolean resourcesReady;

    /**
     * Create a pad with the default 4+2 layout: w/a/s/d on the left, f and enter
     * stacked on the right, and space on the full-screen special tap.
     */
    public VirtualController() {
        bindings.put(Slot.LEFT_UP, "w");
        bindings.put(Slot.LEFT_LEFT, "a");
        bindings.put(Slot.LEFT_DOWN, "s");
        bindings.put(Slot.LEFT_RIGHT, "d");
        bindings.put(Slot.RIGHT_1, "f");
        bindings.put(Slot.RIGHT_2, "enter");
        bindings.put(Slot.RIGHT_3, "space");
        bindings.put(Slot.RIGHT_4, "escape");
        bindings.put(Slot.SPECIAL, "space");

        for (Slot slot : Slot.values()) {
            slotVisible.put(slot, true);
            pressed.put(slot, false);
        }
    }

    // ================ configuration ================

    /** The layout currently drawn. */
    public Layout getLayout() {
        return layout;
    }

    /**
     * Choose how many buttons to show and where.
     *
     * @param layout the layout; null is ignored
     */
    public void setLayout(Layout layout) {
        if (layout != null && layout != this.layout) {
            this.layout = layout;
            releaseAll();
        }
    }

    /**
     * Bind a key to a slot. The button relabels itself to the new key.
     *
     * @param slot the slot to bind; null is ignored
     * @param keyName a Greenfoot key name such as "w", "space" or "enter";
     *                null or empty unbinds the slot, which also hides it
     */
    public void bind(Slot slot, String keyName) {
        if (slot == null) {
            return;
        }
        if (keyName == null || keyName.trim().isEmpty()) {
            bindings.remove(slot);
        } else {
            bindings.put(slot, keyName.trim().toLowerCase());
        }
        pressed.put(slot, false);
    }

    /**
     * The key bound to a slot.
     *
     * @param slot the slot to look up
     * @return the key name, or null when the slot is unbound
     */
    public String getBinding(Slot slot) {
        return slot == null ? null : bindings.get(slot);
    }

    /**
     * Debug switch: {@code -Dgreenfoot.virtualController.preview=true} keeps the pad on
     * screen even where the project hides it, so a layout can be looked at without
     * playing through to a world that shows it.
     */
    private static final boolean PREVIEW_ALWAYS_VISIBLE =
            Boolean.getBoolean("greenfoot.virtualController.preview");

    /**
     * Whether the pad is drawn and accepts touches at all. False when it was hidden
     * with {@link #setVisible(boolean)} or when the current world matches
     * {@link #setHideFor(Class[])}.
     */
    public boolean isVisible() {
        return PREVIEW_ALWAYS_VISIBLE || (manuallyVisible && !autoHidden);
    }

    /**
     * Show or hide the whole pad. A hidden pad is not drawn, takes no touches and
     * consumes nothing, so taps go straight through to the game.
     *
     * @param visible true to show the pad
     */
    public void setVisible(boolean visible) {
        if (this.manuallyVisible != visible) {
            this.manuallyVisible = visible;
            releaseAll();
        }
    }

    /**
     * Hide the pad automatically whenever one of these classes is on screen.
     *
     * Each entry is either a World subclass - the pad hides while that world is the
     * current one - or an Actor subclass, in which case the pad hides while at least
     * one actor of that class is in the world. This is the declarative alternative to
     * sprinkling {@code setVirtualControllerVisible(false)} through a project: a menu
     * world, a dialogue actor or a modal panel can simply be listed here.
     *
     * @param classes the World and Actor classes that hide the pad; passing none, or
     *                null, clears the rule
     */
    public void setHideFor(Class<?>... classes) {
        hideFor.clear();
        hideForNames.clear();
        if (classes != null) {
            for (Class<?> type : classes) {
                if (type != null) {
                    hideFor.add(type);
                }
            }
        }
        refreshAutoHidden();
    }

    /**
     * Hide the pad automatically whenever one of these classes, named as text, is on
     * screen.
     *
     * A Greenfoot project's classes live in the default package, which Java cannot
     * import from a named package, so names are the only way for wrapper-side
     * configuration to refer to them. Names are resolved when first needed and cached;
     * a name that matches no class is ignored.
     *
     * @param classNames simple class names such as "MainMenu" or "Dialog"
     */
    public void setHideForNames(String... classNames) {
        hideFor.clear();
        hideForNames.clear();
        if (classNames != null) {
            for (String name : classNames) {
                if (name != null && !name.trim().isEmpty()) {
                    hideForNames.add(name.trim());
                }
            }
        }
        resolvedNames = false;
        refreshAutoHidden();
    }

    /** Resolve any names given to {@link #setHideForNames(String...)} into classes. */
    private void resolveHideForNames() {
        if (resolvedNames || hideForNames.isEmpty()) {
            return;
        }
        for (String name : hideForNames) {
            try {
                hideFor.add(Class.forName(name));
            } catch (ClassNotFoundException e) {
                Gdx.app.log("VirtualController", "hideFor: no class named " + name);
            }
        }
        resolvedNames = true;
    }

    /**
     * The classes that currently hide the pad.
     *
     * @return a copy of the list, never null
     */
    public java.util.List<Class<?>> getHideFor() {
        return new java.util.ArrayList<>(hideFor);
    }

    /**
     * Whether the pad is hidden right now because of {@link #setHideFor(Class[])}
     * rather than because someone called {@link #setVisible(boolean)}.
     *
     * @return true if a listed class is on screen
     */
    public boolean isAutoHidden() {
        return autoHidden;
    }

    /**
     * Re-evaluate the auto-hide rule against the world showing right now.
     *
     * A World entry matches when it is the current world's class (or a superclass of
     * it); an Actor entry matches when the current world holds at least one such actor.
     */
    private void refreshAutoHidden() {
        resolveHideForNames();

        if (hideFor.isEmpty()) {
            autoHidden = false;
            return;
        }

        greenfoot.World world = greenfoot.WorldHandler.getInstance().getWorld();
        if (world == null) {
            autoHidden = false;
            return;
        }

        for (Class<?> type : hideFor) {
            if (greenfoot.World.class.isAssignableFrom(type)) {
                if (type.isInstance(world)) {
                    autoHidden = true;
                    return;
                }
            } else if (greenfoot.Actor.class.isAssignableFrom(type)) {
                @SuppressWarnings("unchecked")
                Class<? extends greenfoot.Actor> actorType = (Class<? extends greenfoot.Actor>) type;
                if (!world.getObjects(actorType).isEmpty()) {
                    autoHidden = true;
                    return;
                }
            }
        }
        autoHidden = false;
    }

    /**
     * Show or hide one slot. A hidden button is not drawn and takes no touches.
     *
     * @param slot the slot; null is ignored
     * @param slotIsVisible true to show it
     */
    public void setSlotVisible(Slot slot, boolean slotIsVisible) {
        if (slot == null) {
            return;
        }
        slotVisible.put(slot, slotIsVisible);
        if (!slotIsVisible) {
            pressed.put(slot, false);
        }
    }

    /**
     * Whether a slot is shown. A slot in a position the current layout does not use
     * is never shown, whatever this was set to.
     *
     * @param slot the slot to test
     * @return true if the slot is currently on screen
     */
    public boolean isSlotVisible(Slot slot) {
        if (slot == null || !isSlotInLayout(slot)) {
            return false;
        }
        Boolean value = slotVisible.get(slot);
        return value == null || value;
    }

    /** Show every slot the layout uses. */
    public void setAllSlotsVisible(boolean slotsVisible) {
        for (Slot slot : Slot.values()) {
            setSlotVisible(slot, slotsVisible);
        }
    }

    /** Idle opacity of the buttons, 0 to 1. */
    public void setOpacity(float opacity) {
        this.opacity = Math.max(0f, Math.min(1f, opacity));
    }

    /** Size multiplier applied to the whole pad. */
    public void setScale(float scale) {
        if (scale > 0f) {
            this.scale = scale;
        }
    }

    // ================ file configuration ================

    /**
     * The file a project drops next to its assets to configure the pad without
     * touching any Java. Looked for in the project folder first, then at the assets
     * root.
     */
    public static final String CONFIG_FILE = "controller.properties";

    /**
     * Apply {@code controller.properties} if the project ships one.
     *
     * The file is plain {@code key=value} lines, {@code #} starts a comment:
     *
     * <pre>
     * layout    = 4-2
     * left_up   = w
     * left_left = a
     * left_down = s
     * left_right= d
     * right_1   = f
     * right_2   = enter
     * special   = space
     * hide_for  = MainMenu, Credit, Controls
     * opacity   = 0.72
     * scale     = 1.0
     * </pre>
     *
     * Any key left out keeps its default. {@code hide_for} takes the class names -
     * worlds or actors - that hide the pad while they are on screen.
     */
    public void loadConfigFromAssets() {
        String text = readConfigText();
        if (text == null) {
            return;
        }

        for (String rawLine : text.split("\n")) {
            String line = rawLine.trim();
            int comment = line.indexOf('#');
            if (comment >= 0) {
                line = line.substring(0, comment).trim();
            }
            int equals = line.indexOf('=');
            if (line.isEmpty() || equals <= 0) {
                continue;
            }

            String key = line.substring(0, equals).trim().toLowerCase();
            String value = line.substring(equals + 1).trim();

            switch (key) {
                case "layout": {
                    Layout parsed = Layout.fromId(value);
                    if (parsed != null) {
                        setLayout(parsed);
                    }
                    break;
                }
                case "opacity":
                    setOpacity(parseFloat(value, opacity));
                    break;
                case "scale":
                    setScale(parseFloat(value, scale));
                    break;
                case "hide_for":
                case "hidefor":
                    setHideForNames(value.split(","));
                    break;
                case "visible":
                    setVisible(Boolean.parseBoolean(value));
                    break;
                case "debug":
                    debug = Boolean.parseBoolean(value);
                    break;
                default: {
                    Slot slot = Slot.fromId(key);
                    if (slot != null) {
                        bind(slot, value.isEmpty() || "none".equalsIgnoreCase(value) ? null : value);
                    }
                    break;
                }
            }
        }
        Gdx.app.log("VirtualController", "loaded " + CONFIG_FILE);
    }

    private String readConfigText() {
        String[] candidates = {
            GreenfootProjectConfig.getUserProjectFolder() + "/" + CONFIG_FILE,
            CONFIG_FILE
        };
        for (String path : candidates) {
            try {
                com.badlogic.gdx.files.FileHandle handle = Gdx.files.internal(path);
                if (handle.exists()) {
                    return handle.readString();
                }
            } catch (Exception e) {
                // Try the next candidate.
            }
        }
        return null;
    }

    private static float parseFloat(String value, float fallback) {
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    // ================ per-frame work ================

    /**
     * Read the touch screen and update which buttons are held. Call once per frame,
     * before the game reads its input.
     */
    public void update() {
        java.util.Arrays.fill(pointerConsumed, false);
        specialPressed = false;
        refreshAutoHidden();

        boolean padVisible = isVisible();

        // Decide which pointers the pad may act on, before anything else looks at them.
        // A pointer counts only if it went DOWN while the pad was visible. Without this,
        // a finger that is still on the screen when the pad reappears - the tap that
        // just dismissed a dialogue, say - is adopted mid-gesture: it landed off the
        // buttons, so it fired the special tap the instant the pad came back, before the
        // player had touched any button.
        for (int pointer = 0; pointer < MAX_POINTERS; pointer++) {
            boolean down = Gdx.input.isTouched(pointer);
            if (!down) {
                pointerAdopted[pointer] = false;
            } else if (!pointerWasDown[pointer]) {
                pointerAdopted[pointer] = padVisible;
            }
            pointerWasDown[pointer] = down;
        }

        if (!padVisible) {
            releaseAll();
            return;
        }

        layoutButtons();

        for (Slot slot : geometry.keySet()) {
            pressed.put(slot, false);
        }

        boolean anyTouchOffButtons = false;

        for (int pointer = 0; pointer < MAX_POINTERS; pointer++) {
            if (!Gdx.input.isTouched(pointer) || !pointerAdopted[pointer]) {
                continue;
            }

            float touchX = Gdx.input.getX(pointer);
            // Touch coordinates are y-down; the pad's geometry is y-up like the screen.
            float touchY = Gdx.graphics.getHeight() - Gdx.input.getY(pointer);

            Slot hit = slotAt(touchX, touchY);
            if (hit != null) {
                pressed.put(hit, true);
                if (pointer < pointerConsumed.length) {
                    pointerConsumed[pointer] = true;
                }
            } else if (isInsidePad(touchX, touchY)) {
                // In the gap between a cluster's buttons: the pad still owns the touch,
                // so it neither fires the special tap nor reaches the world.
                if (pointer < pointerConsumed.length) {
                    pointerConsumed[pointer] = true;
                }
            } else {
                anyTouchOffButtons = true;
            }
        }

        // The special tap is everything that is not a button. It does not consume the
        // pointer, so the same tap still reaches the world as a normal mouse click.
        specialPressed = anyTouchOffButtons && bindings.get(Slot.SPECIAL) != null;

        if (debug) {
            logState();
        }

    }

    /**
     * Log what the pad currently sees. Enabled with "debug = true" in
     * controller.properties; on Android the output shows up in logcat.
     */
    private void logState() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Slot, Boolean> entry : pressed.entrySet()) {
            if (Boolean.TRUE.equals(entry.getValue())) {
                sb.append(entry.getKey().getId()).append("=").append(bindings.get(entry.getKey())).append(" ");
            }
        }
        if (specialPressed) {
            sb.append("special=").append(bindings.get(Slot.SPECIAL)).append(" ");
        }

        boolean touching = false;
        for (int pointer = 0; pointer < MAX_POINTERS; pointer++) {
            if (Gdx.input.isTouched(pointer)) {
                touching = true;
                sb.append("| p").append(pointer).append("=")
                  .append(Gdx.input.getX(pointer)).append(",").append(Gdx.input.getY(pointer))
                  .append(" consumed=").append(pointerConsumed[pointer]);
            }
        }

        if (touching || sb.length() > 0) {
            sb.append(" | screen=").append(Gdx.graphics.getWidth()).append("x").append(Gdx.graphics.getHeight())
              .append(" back=").append(Gdx.graphics.getBackBufferWidth()).append("x")
              .append(Gdx.graphics.getBackBufferHeight())
              .append(" r=").append(radius);
            for (Map.Entry<Slot, float[]> entry : geometry.entrySet()) {
                float[] c = entry.getValue();
                sb.append(" ").append(entry.getKey().getId()).append("@")
                  .append(Math.round(c[0])).append(",").append(Math.round(c[1]));
            }
            Gdx.app.log("PADDBG", sb.toString());
        }
    }

    /**
     * Draw the pad. Call after the world has been drawn, so the pad sits on top.
     */
    public void render() {
        if (!isVisible()) {
            return;
        }

        ensureResources();
        if (!resourcesReady) {
            return;
        }

        layoutButtons();

        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();
        projection.setToOrtho2D(0, 0, screenWidth, screenHeight);
        batch.setProjectionMatrix(projection);

        batch.begin();
        for (Map.Entry<Slot, float[]> entry : geometry.entrySet()) {
            Slot slot = entry.getKey();
            float[] circle = entry.getValue();
            boolean isDown = Boolean.TRUE.equals(pressed.get(slot));

            Texture texture = isDown ? buttonPressedTexture : buttonTexture;
            float alpha = isDown ? Math.min(1f, opacity + 0.35f) : opacity;
            float diameter = circle[2] * 2f;

            batch.setColor(1f, 1f, 1f, alpha);
            batch.draw(texture, circle[0] - circle[2], circle[1] - circle[2], diameter, diameter);

            String label = labelFor(slot);
            if (label != null) {
                drawLabel(label, circle[0], circle[1], isDown, Math.min(1f, alpha + 0.25f));
            }
        }
        batch.setColor(Color.WHITE);
        batch.end();
    }

    /**
     * Whether a key is currently held down on the pad.
     *
     * @param keyName a Greenfoot key name, case-insensitive
     * @return true if a pressed button, or the special tap, is bound to that key
     */
    public boolean isKeyDown(String keyName) {
        if (!isVisible() || keyName == null) {
            return false;
        }
        String wanted = keyName.trim().toLowerCase();

        for (Map.Entry<Slot, Boolean> entry : pressed.entrySet()) {
            if (Boolean.TRUE.equals(entry.getValue())
                    && wanted.equals(bindings.get(entry.getKey()))) {
                return true;
            }
        }
        return specialPressed && wanted.equals(bindings.get(Slot.SPECIAL));
    }

    /**
     * Whether a touch belongs to the pad and should not reach the game as a click.
     *
     * @param pointer the pointer index
     * @return true if that pointer is currently down on a button
     */
    public boolean isPointerConsumed(int pointer) {
        return pointer >= 0 && pointer < pointerConsumed.length && pointerConsumed[pointer];
    }

    /** Whether any pointer is currently down on a button. */
    public boolean isAnyPointerConsumed() {
        for (boolean consumed : pointerConsumed) {
            if (consumed) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether a screen point is on a visible button.
     *
     * @param screenX x in screen pixels
     * @param screenY y in screen pixels, y-down as reported by Gdx.input
     * @return true if a button is under that point
     */
    public boolean isPointOnPad(float screenX, float screenY) {
        if (!isVisible()) {
            return false;
        }
        layoutButtons();
        float y = Gdx.graphics.getHeight() - screenY;
        return slotAt(screenX, y) != null || isInsidePad(screenX, y);
    }

    /** Free the pad's own GPU resources. */
    public void dispose() {
        if (batch != null) {
            batch.dispose();
            batch = null;
        }
        if (buttonTexture != null) {
            buttonTexture.dispose();
            buttonTexture = null;
        }
        if (buttonPressedTexture != null) {
            buttonPressedTexture.dispose();
            buttonPressedTexture = null;
        }
        disposeFont();
        resourcesReady = false;
    }

    /** Drop cached geometry and font so the next frame rebuilds them at the new size. */
    public void resize() {
        geometry.clear();
        disposeFont();
    }

    // ================ internals ================

    private void releaseAll() {
        for (Slot slot : Slot.values()) {
            pressed.put(slot, false);
        }
        specialPressed = false;
        java.util.Arrays.fill(pointerConsumed, false);
        java.util.Arrays.fill(pointerAdopted, false);
    }

    private boolean isSlotInLayout(Slot slot) {
        if (slot == Slot.SPECIAL) {
            return true;
        }

        switch (layout) {
            case FOUR_FOUR:
                return true;
            case FOUR_TWO:
                return slot != Slot.RIGHT_3 && slot != Slot.RIGHT_4;
            case FOUR_ONE:
                return slot == Slot.RIGHT_1
                    || slot == Slot.LEFT_UP || slot == Slot.LEFT_DOWN
                    || slot == Slot.LEFT_LEFT || slot == Slot.LEFT_RIGHT;
            case TWO_TWO:
                // Left and right only, on both sides: no up or down anywhere.
                return slot == Slot.LEFT_LEFT || slot == Slot.LEFT_RIGHT
                    || slot == Slot.RIGHT_1 || slot == Slot.RIGHT_2;
            default:
                return true;
        }
    }

    /**
     * Work out where the buttons sit for the current screen size. Each entry is
     * {centreX, centreY, radius} in y-up screen pixels.
     */
    private void layoutButtons() {
        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();
        float shortSide = Math.min(screenWidth, screenHeight);

        float newRadius = shortSide * 0.085f * scale;
        if (!geometry.isEmpty() && Math.abs(newRadius - radius) < 0.5f
                && geometry.containsKey(Slot.LEFT_UP)) {
            // Same size as last frame: the cached geometry still holds. Visibility can
            // change between frames, so only the membership below needs refreshing.
            if (geometryMatchesVisibility()) {
                return;
            }
        }
        radius = newRadius;
        geometry.clear();

        float margin = shortSide * 0.05f;
        float spread = radius * 1.6f;

        float leftCentreX = margin + spread + radius;
        float leftCentreY = margin + spread + radius;

        if (layout == Layout.TWO_TWO) {
            // A side-by-side pair rather than a d-pad: only left and right.
            putIfVisible(Slot.LEFT_LEFT, leftCentreX - PAIR_GAP * radius, leftCentreY);
            putIfVisible(Slot.LEFT_RIGHT, leftCentreX + PAIR_GAP * radius, leftCentreY);
        } else {
            putIfVisible(Slot.LEFT_UP, leftCentreX, leftCentreY + spread);
            putIfVisible(Slot.LEFT_DOWN, leftCentreX, leftCentreY - spread);
            putIfVisible(Slot.LEFT_LEFT, leftCentreX - spread, leftCentreY);
            putIfVisible(Slot.LEFT_RIGHT, leftCentreX + spread, leftCentreY);
        }

        float rightCentreX = screenWidth - margin - spread - radius;
        float rightCentreY = margin + spread + radius;
        // Flush with the screen edge, so a thumb reaches the buttons without covering
        // the middle of the screen.
        float edgeX = screenWidth - margin - radius;

        switch (layout) {
            case FOUR_FOUR:
                putIfVisible(Slot.RIGHT_1, rightCentreX, rightCentreY - spread);
                putIfVisible(Slot.RIGHT_2, rightCentreX + spread, rightCentreY);
                putIfVisible(Slot.RIGHT_3, rightCentreX, rightCentreY + spread);
                putIfVisible(Slot.RIGHT_4, rightCentreX - spread, rightCentreY);
                break;
            case FOUR_ONE:
                putIfVisible(Slot.RIGHT_1, edgeX, rightCentreY);
                break;
            case TWO_TWO:
                // Mirror of the left pair: side by side, not stacked.
                putIfVisible(Slot.RIGHT_1, edgeX - 2f * PAIR_GAP * radius, rightCentreY);
                putIfVisible(Slot.RIGHT_2, edgeX, rightCentreY);
                break;
            case FOUR_TWO:
            default:
                // Two buttons stacked like the d-pad's up/down pair.
                putIfVisible(Slot.RIGHT_1, edgeX, rightCentreY + STACK_GAP * radius);
                putIfVisible(Slot.RIGHT_2, edgeX, rightCentreY - STACK_GAP * radius);
                break;
        }

        buildClusterAreas();
    }

    private boolean geometryMatchesVisibility() {
        for (Slot slot : Slot.values()) {
            if (slot == Slot.SPECIAL) {
                continue;
            }
            boolean shouldBeThere = isSlotVisible(slot) && bindings.get(slot) != null;
            if (shouldBeThere != geometry.containsKey(slot)) {
                return false;
            }
        }
        return true;
    }

    private void putIfVisible(Slot slot, float centreX, float centreY) {
        if (isSlotVisible(slot) && bindings.get(slot) != null) {
            geometry.put(slot, new float[] { centreX, centreY, radius });
        }
    }


    /**
     * Work out the area each cluster covers, so a touch that lands between two buttons
     * still belongs to the pad.
     *
     * Without this, the hole in the middle of the d-pad counted as "off the pad": it
     * fired the special tap and passed a click through to the world, which is exactly
     * where a thumb sliding between buttons ends up.
     */
    private void buildClusterAreas() {
        clusterAreas.clear();
        addClusterArea(LEFT_SLOTS);
        addClusterArea(RIGHT_SLOTS);
    }

    private void addClusterArea(Slot[] slots) {
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        boolean any = false;

        for (Slot slot : slots) {
            float[] circle = geometry.get(slot);
            if (circle == null) {
                continue;
            }
            any = true;
            float pad = circle[2] * TOUCH_RADIUS_FACTOR;
            minX = Math.min(minX, circle[0] - pad);
            minY = Math.min(minY, circle[1] - pad);
            maxX = Math.max(maxX, circle[0] + pad);
            maxY = Math.max(maxY, circle[1] + pad);
        }

        if (any) {
            clusterAreas.add(new float[] { minX, minY, maxX, maxY });
        }
    }

    /**
     * Whether a y-up screen point is inside a cluster - on a button or in the gap
     * between its buttons. Such a touch belongs to the pad even when no button fires.
     */
    private boolean isInsidePad(float x, float y) {
        for (float[] area : clusterAreas) {
            if (x >= area[0] && x <= area[2] && y >= area[1] && y <= area[3]) {
                return true;
            }
        }
        return false;
    }

    /**
     * The slot under a y-up screen point, or null.
     *
     * The nearest centre wins rather than the first one that matches: touch areas are
     * grown past the drawn circle so a thumb lands reliably, and with the d-pad's
     * diagonal neighbours that close, taking the first match let one touch press two
     * buttons - pressing "s" also fired "a".
     */
    private Slot slotAt(float x, float y) {
        Slot best = null;
        float bestDistanceSquared = Float.MAX_VALUE;

        for (Map.Entry<Slot, float[]> entry : geometry.entrySet()) {
            float[] circle = entry.getValue();
            float dx = x - circle[0];
            float dy = y - circle[1];
            float distanceSquared = dx * dx + dy * dy;
            float touchRadius = circle[2] * TOUCH_RADIUS_FACTOR;

            if (distanceSquared <= touchRadius * touchRadius && distanceSquared < bestDistanceSquared) {
                bestDistanceSquared = distanceSquared;
                best = entry.getKey();
            }
        }
        return best;
    }

    /** The text drawn on a button: the key it is bound to. */
    private String labelFor(Slot slot) {
        String binding = bindings.get(slot);
        if (binding == null) {
            return null;
        }
        // Plain ASCII only: FreeType generates the font's default Latin character set,
        // so an arrow glyph such as U+21B5 comes out as a missing-glyph box.
        switch (binding) {
            case "space": return "SPC";
            case "enter": return "ENT";
            case "escape": return "ESC";
            case "up": return "UP";
            case "down": return "DN";
            case "left": return "LT";
            case "right": return "RT";
            default: break;
        }
        return binding.length() <= 4 ? binding.toUpperCase() : binding.substring(0, 4).toUpperCase();
    }

    private void drawLabel(String label, float centreX, float centreY, boolean isDown, float alpha) {
        // Measure only. GlyphLayout bakes the font's colour in at setText() time, so the
        // colour has to be set before each setText() - drawing a laid-out label after
        // calling setColor() paints it in whatever colour the PREVIOUS label used, which
        // made the next button in draw order blink along with the one being pressed.
        if (isDown) {
            // Dark text on the pressed (near-white) disc.
            font.setColor(0.05f, 0.06f, 0.10f, alpha);
            glyphLayout.setText(font, label);
            font.draw(batch, glyphLayout,
                      centreX - glyphLayout.width / 2f, centreY + glyphLayout.height / 2f);
            return;
        }

        // White text on the dark disc, with a shadow so it survives a bright background
        // showing through the disc's own translucency.
        font.setColor(0f, 0f, 0f, alpha * 0.55f);
        glyphLayout.setText(font, label);
        float textX = centreX - glyphLayout.width / 2f;
        float textY = centreY + glyphLayout.height / 2f;
        font.draw(batch, glyphLayout, textX + 1.5f, textY - 1.5f);

        font.setColor(1f, 1f, 1f, alpha);
        glyphLayout.setText(font, label);
        font.draw(batch, glyphLayout, textX, textY);
    }

    private void ensureResources() {
        if (batch == null) {
            batch = new SpriteBatch();
        }
        if (buttonTexture == null) {
            buttonTexture = createButtonTexture(false);
        }
        if (buttonPressedTexture == null) {
            buttonPressedTexture = createButtonTexture(true);
        }

        int wantedFontSize = Math.max(12, Math.round(radius * 0.62f));
        if (font == null || wantedFontSize != fontSize) {
            disposeFont();
            fontSize = wantedFontSize;
            font = new greenfoot.Font(fontSize).getBitmapFont();
        }

        resourcesReady = batch != null && buttonTexture != null
                && buttonPressedTexture != null && font != null;
    }

    private void disposeFont() {
        if (font != null) {
            font.dispose();
            font = null;
        }
        fontSize = -1;
    }

    /**
     * A round button: a filled disc with a brighter rim, drawn once into a texture.
     * Supersampled 4x so the edges stay smooth when it is scaled to the button size.
     */
    private Texture createButtonTexture(boolean isPressed) {
        int size = 256;
        int supersample = 4;
        int hiRes = size * supersample;

        Pixmap hi = new Pixmap(hiRes, hiRes, Pixmap.Format.RGBA8888);
        hi.setBlending(Pixmap.Blending.SourceOver);

        int centre = hiRes / 2;
        int outer = centre - supersample;
        int rim = Math.max(1, (int) (hiRes * 0.045f));

        // A dark disc keeps a white label readable over bright artwork; the pressed
        // state inverts to a near-white disc, which reads at a glance under a thumb.
        if (isPressed) {
            hi.setColor(0.93f, 0.96f, 1f, 0.98f);
        } else {
            hi.setColor(0.05f, 0.06f, 0.10f, 0.92f);
        }
        hi.fillCircle(centre, centre, outer - rim);

        // Rim: a bright ring reads as a raised edge without needing a gradient.
        hi.setColor(1f, 1f, 1f, isPressed ? 1f : 0.85f);
        for (int i = 0; i < rim; i++) {
            hi.drawCircle(centre, centre, outer - i);
        }

        // A soft halo just outside the rim separates the button from busy artwork.
        hi.setColor(0f, 0f, 0f, 0.28f);
        int halo = Math.max(1, rim / 2);
        for (int i = 1; i <= halo; i++) {
            hi.drawCircle(centre, centre, outer + i - halo);
        }

        Pixmap scaled = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        scaled.setFilter(Pixmap.Filter.BiLinear);
        scaled.drawPixmap(hi, 0, 0, hiRes, hiRes, 0, 0, size, size);
        hi.dispose();

        Texture texture = new Texture(scaled);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        scaled.dispose();
        return texture;
    }
}
