package greenfoot;

import java.util.Random;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Sound;

/**
 * LibGDX-based utility class that provides methods to control the simulation
 * and interact with the system using LibGDX's input and audio systems.
 * 
 * <h2>Key names</h2>
 * 
 * <p>Part of the functionality provided by this class is the ability to
 * retrieve keyboard input. The methods getKey() and isKeyDown() are used
 * for this and they return/understand the following key names:
 * 
 * <ul>
 * <li>"a", "b", .., "z" (alphabetical keys), "0".."9" (digits), most
 *     punctuation marks. getKey() also returns uppercase characters when
 *     appropriate.
 * <li>"up", "down", "left", "right" (the cursor keys)
 * <li>"enter", "space", "tab", "escape", "backspace", "shift", "control"
 * <li>"F1", "F2", .., "F12" (the function keys)
 * </ul>
 * 
 * @author Davin McCall
 * @version 2.7
 */
public class Greenfoot
{
    private static Random randomGenerator = new Random();
    private static String lastKey = null;
    private static int speed = 50;

    /**
     * Sets the World to run to the one given.
     * This World will now be the main World that Greenfoot runs with on the
     * next act.
     *
     * @param world The World to switch running to, cannot be null.
     */
    public static void setWorld(World world)
    {
        if ( world == null ) {
            throw new NullPointerException("The given world cannot be null.");
        }

        WorldHandler.getInstance().setWorld(world);
    }

    /**
     * Get the most recently pressed key, since the last time this method was
     * called. If no key was pressed since this method was last called, it
     * will return null. If more than one key was pressed, this returns only
     * the most recently pressed key.
     * 
     * @return  The name of the most recently pressed key
     */
    public static String getKey()
    {
        // Handle key polling using LibGDX input
        String key = pollKeyInput();
        if (key != null) {
            String result = lastKey;
            lastKey = null;
            return result != null ? result : key;
        }
        return null;
    }
    
    /**
     * Check whether a given key is currently pressed down.
     * 
     * @param keyName  The name of the key to check
     * @return         True if the key is down
     */
    public static boolean isKeyDown(String keyName)
    {
        return isLibGDXKeyPressed(keyName);
    }
    
    /**
     * Delay the current execution by a number of time steps. 
     * The size of one time step is defined by the Greenfoot environment (the speed slider).
     * 
     * @param time  The number of steps the delay will last.
     * @see #setSpeed(int)
     */
    public static void delay(int time)
    {
        // In LibGDX, we simulate delay by sleeping the current thread
        try {
            Thread.sleep(time * (100 - speed)); // Faster speed = less delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Set the speed of the execution.
     *  
     * @param speed  The new speed. the value must be in the range (1..100)
     */
    public static void setSpeed(int speed)
    {
        if (speed < 1 || speed > 100) {
            throw new IllegalArgumentException("Speed must be between 1 and 100");
        }
        Greenfoot.speed = speed;
    }
    
    /**
     * Pause the execution.
     */
    public static void stop()
    {
        // In LibGDX, we don't have a built-in pause mechanism, so we use a flag
        // This would need to be integrated with the main game loop
        Gdx.app.log("Greenfoot", "Stop requested");
    }
    
    /**
     * Run (or resume) the execution.
     */
    public static void start()
    {
        // In LibGDX, we don't have a built-in pause mechanism, so we use a flag
        // This would need to be integrated with the main game loop
        Gdx.app.log("Greenfoot", "Start requested");
    }
    
    /**
     * Return a random number between 0 (inclusive) and limit (exclusive).
     * 
     * @param limit  An upper limit which the returned random number will be smaller than.
     * @return A random number within 0 to (limit-1) range.
     */
    public static int getRandomNumber(int limit)
    {
        return randomGenerator.nextInt(limit);
    }

        /**
     * Play sound from the given file. The file format must be one of: aiff, au,
     * wav, mp3. The file should be located in the sounds directory in the project.
     * This method returns once the sound has started playing - it does not wait
     * for the sound to complete. If the sound file cannot be found or loaded, no 
     * exception is thrown - the method simply returns without playing a sound.
     *
     * @param soundFile Name of the sound file to play
     */
    public static void playSound(String soundFile)
    {
        try {
            Sound sound = Gdx.audio.newSound(Gdx.files.internal("sounds/" + soundFile));
            sound.play();
        } catch (Exception e) {
            // Fail silently as per Greenfoot behavior
            Gdx.app.log("Greenfoot", "Could not play sound: " + soundFile);
        }
    }


        /**
     * True if a mouse button has been pressed (clicked) on the given object.
     * 
     * @param obj  The object to check for mouse press. If obj is the world, 
     *             it checks whether the background has been clicked. If obj 
     *             is null, this method checks for a mouse click anywhere.
     * @return     True if the mouse has been pressed on the given object, 
     *             false otherwise.
     */
    public static boolean mousePressed(Object obj)
    {
        // For LibGDX implementation, we need to integrate with the mouse handling
        // This is a simplified implementation
        return Gdx.input.isButtonPressed(Input.Buttons.LEFT);
    }

    /**
     * True if a mouse button has been clicked (pressed and released) on the given object.
     * 
     * @param obj  The object to check for mouse click. If obj is the world, 
     *             it checks whether the background has been clicked. If obj 
     *             is null, this method checks for a mouse click anywhere.
     * @return     True if the mouse has been clicked on the given object, 
     *             false otherwise.
     */
    public static boolean mouseClicked(Object obj)
    {
        // For LibGDX implementation, we need to track button release after press
        // This is a simplified implementation
        return Gdx.input.justTouched();
    }

    /**
     * True if the mouse has been dragged on the given object. Dragging is when
     * the mouse is moved while the button is held down.
     * 
     * @param obj  The object to check for mouse dragging. If obj is the world, 
     *             it checks whether the mouse is being dragged on the background. 
     *             If obj is null, this method checks for mouse dragging anywhere.
     * @return     True if the mouse has been dragged on the given object, 
     *             false otherwise.
     */
    public static boolean mouseDragged(Object obj)
    {
        // For LibGDX implementation, we need to track mouse movement while button is down
        // This is a simplified implementation
        return Gdx.input.isButtonPressed(Input.Buttons.LEFT) && 
               (Gdx.input.getDeltaX() != 0 || Gdx.input.getDeltaY() != 0);
    }

    /**
     * True if a mouse drag has ended. This happens when the mouse button is 
     * released after a dragging.
     * 
     * @param obj  The object to check for end of mouse dragging. If obj is the 
     *             world, it checks whether a mouse drag on the background ended. 
     *             If obj is null, this method checks for mouse drag ending anywhere.
     * @return     True if a mouse drag has ended on the given object, false otherwise.
     */
    public static boolean mouseDragEnded(Object obj)
    {
        // For LibGDX implementation, we need to track button release after dragging
        // This is a simplified implementation - would need proper state tracking
        return !Gdx.input.isButtonPressed(Input.Buttons.LEFT);
    }

    /**
     * True if the mouse has been moved on the given object.
     * 
     * @param obj  The object to check for mouse move. If obj is the world, 
     *             it checks whether the mouse has been moved over the background. 
     *             If obj is null, this method checks for mouse moves anywhere.
     * @return     True if the mouse has been moved on the given object, 
     *             false otherwise.
     */
    public static boolean mouseMoved(Object obj)
    {
        // For LibGDX implementation, check for mouse movement
        return Gdx.input.getDeltaX() != 0 || Gdx.input.getDeltaY() != 0;
    }

    /**
     * Return a mouse info object with information about the state of the mouse.
     * 
     * @return A mouse info object if the mouse has been clicked, pressed, moved, etc.
     *         null, otherwise.
     */
    public static MouseInfo getMouseInfo()
    {
        // For LibGDX implementation, create MouseInfo from current input state
        // This is a simplified implementation
        if (Gdx.input.justTouched() || Gdx.input.isButtonPressed(Input.Buttons.LEFT) || mouseMoved(null)) {
            MouseInfo info = new MouseInfo();
            // Would need proper coordinate conversion and actor detection
            return info;
        }
        return null;
    }

    /**
     * Get the current microphone level (volume). This can be used to react to
     * the sound level of the default microphone device.
     * 
     * @return The sound level (0-100), where 0 is no sound at all.
     */
    public static int getMicLevel()
    {
        // LibGDX doesn't have built-in microphone support, return 0
        return 0;
    }

    /**
     * True if the mouse has been released (changed from pressed to non-pressed) on the given
     * object. If the parameter is an Actor the method will only return true if
     * the mouse has been clicked on the given actor. If there are several
     * actors at the same place, only the top most actor will receive the click.
     * If the parameter is a World then true will be returned if the mouse was
     * clicked on the world background. If the parameter is null,
     * then true will be returned for any click, independent of the target 
    /**
     * Get input from the user (and freeze the scenario while we are waiting).
     * The prompt String parameter will be shown to the user (e.g. "How many players?"), and the answer will be returned as a String.
     * If you want to ask for a number, you can use methods like <code>Integer.parseInt</code> to turn
     * the returned String into a number.
     * <p>
     * This method can only be used when a world is in place and the scenario is running.
     * It returns null if that is not the case, or if the scenario is reset while the prompt
     * is being shown.
     *
     * @param prompt The prompt to show to the user.
    /**
     * @return The string that the user typed in.
     */
    public static String ask(String prompt)
    {
        // For LibGDX, we can't show dialog boxes easily, so return empty string
        // In a full implementation, this could use a custom UI overlay
        Gdx.app.log("Greenfoot", "Ask prompt: " + prompt);
        return "";
    }

    // ================ LibGDX Helper Methods ================
    
    /**
     * Poll for key input using LibGDX input system.
     */
    private static String pollKeyInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.A)) { lastKey = "a"; return "a"; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.B)) { lastKey = "b"; return "b"; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.C)) { lastKey = "c"; return "c"; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.D)) { lastKey = "d"; return "d"; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) { lastKey = "e"; return "e"; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F)) { lastKey = "f"; return "f"; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.G)) { lastKey = "g"; return "g"; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.H)) { lastKey = "h"; return "h"; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.I)) { lastKey = "i"; return "i"; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.J)) { lastKey = "j"; return "j"; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.K)) { lastKey = "k"; return "k"; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.L)) { lastKey = "l"; return "l"; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.M)) { lastKey = "m"; return "m"; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.N)) { lastKey = "n"; return "n"; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.O)) { lastKey = "o"; return "o"; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.P)) { lastKey = "p"; return "p"; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) { lastKey = "q"; return "q"; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) { lastKey = "r"; return "r"; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.S)) { lastKey = "s"; return "s"; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.T)) { lastKey = "t"; return "t"; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.U)) { lastKey = "u"; return "u"; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.V)) { lastKey = "v"; return "v"; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.W)) { lastKey = "w"; return "w"; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.X)) { lastKey = "x"; return "x"; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.Y)) { lastKey = "y"; return "y"; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.Z)) { lastKey = "z"; return "z"; }
        
        // Numbers
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_0)) { lastKey = "0"; return "0"; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) { lastKey = "1"; return "1"; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) { lastKey = "2"; return "2"; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) { lastKey = "3"; return "3"; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_4)) { lastKey = "4"; return "4"; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_5)) { lastKey = "5"; return "5"; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_6)) { lastKey = "6"; return "6"; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_7)) { lastKey = "7"; return "7"; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_8)) { lastKey = "8"; return "8"; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_9)) { lastKey = "9"; return "9"; }
        
        // Special keys
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) { lastKey = "up"; return "up"; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) { lastKey = "down"; return "down"; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) { lastKey = "left"; return "left"; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) { lastKey = "right"; return "right"; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) { lastKey = "space"; return "space"; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) { lastKey = "enter"; return "enter"; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.TAB)) { lastKey = "tab"; return "tab"; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) { lastKey = "escape"; return "escape"; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE)) { lastKey = "backspace"; return "backspace"; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyJustPressed(Input.Keys.SHIFT_RIGHT)) { 
            lastKey = "shift"; return "shift"; 
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.CONTROL_LEFT) || Gdx.input.isKeyJustPressed(Input.Keys.CONTROL_RIGHT)) { 
            lastKey = "control"; return "control"; 
        }
        
        return null;
    }
    
    /**
     * Check if a LibGDX key is currently pressed.
     */
    private static boolean isLibGDXKeyPressed(String keyName) {
        switch (keyName.toLowerCase()) {
            case "a": return Gdx.input.isKeyPressed(Input.Keys.A);
            case "b": return Gdx.input.isKeyPressed(Input.Keys.B);
            case "c": return Gdx.input.isKeyPressed(Input.Keys.C);
            case "d": return Gdx.input.isKeyPressed(Input.Keys.D);
            case "e": return Gdx.input.isKeyPressed(Input.Keys.E);
            case "f": return Gdx.input.isKeyPressed(Input.Keys.F);
            case "g": return Gdx.input.isKeyPressed(Input.Keys.G);
            case "h": return Gdx.input.isKeyPressed(Input.Keys.H);
            case "i": return Gdx.input.isKeyPressed(Input.Keys.I);
            case "j": return Gdx.input.isKeyPressed(Input.Keys.J);
            case "k": return Gdx.input.isKeyPressed(Input.Keys.K);
            case "l": return Gdx.input.isKeyPressed(Input.Keys.L);
            case "m": return Gdx.input.isKeyPressed(Input.Keys.M);
            case "n": return Gdx.input.isKeyPressed(Input.Keys.N);
            case "o": return Gdx.input.isKeyPressed(Input.Keys.O);
            case "p": return Gdx.input.isKeyPressed(Input.Keys.P);
            case "q": return Gdx.input.isKeyPressed(Input.Keys.Q);
            case "r": return Gdx.input.isKeyPressed(Input.Keys.R);
            case "s": return Gdx.input.isKeyPressed(Input.Keys.S);
            case "t": return Gdx.input.isKeyPressed(Input.Keys.T);
            case "u": return Gdx.input.isKeyPressed(Input.Keys.U);
            case "v": return Gdx.input.isKeyPressed(Input.Keys.V);
            case "w": return Gdx.input.isKeyPressed(Input.Keys.W);
            case "x": return Gdx.input.isKeyPressed(Input.Keys.X);
            case "y": return Gdx.input.isKeyPressed(Input.Keys.Y);
            case "z": return Gdx.input.isKeyPressed(Input.Keys.Z);
            case "0": return Gdx.input.isKeyPressed(Input.Keys.NUM_0);
            case "1": return Gdx.input.isKeyPressed(Input.Keys.NUM_1);
            case "2": return Gdx.input.isKeyPressed(Input.Keys.NUM_2);
            case "3": return Gdx.input.isKeyPressed(Input.Keys.NUM_3);
            case "4": return Gdx.input.isKeyPressed(Input.Keys.NUM_4);
            case "5": return Gdx.input.isKeyPressed(Input.Keys.NUM_5);
            case "6": return Gdx.input.isKeyPressed(Input.Keys.NUM_6);
            case "7": return Gdx.input.isKeyPressed(Input.Keys.NUM_7);
            case "8": return Gdx.input.isKeyPressed(Input.Keys.NUM_8);
            case "9": return Gdx.input.isKeyPressed(Input.Keys.NUM_9);
            case "up": return Gdx.input.isKeyPressed(Input.Keys.UP);
            case "down": return Gdx.input.isKeyPressed(Input.Keys.DOWN);
            case "left": return Gdx.input.isKeyPressed(Input.Keys.LEFT);
            case "right": return Gdx.input.isKeyPressed(Input.Keys.RIGHT);
            case "space": return Gdx.input.isKeyPressed(Input.Keys.SPACE);
            case "enter": return Gdx.input.isKeyPressed(Input.Keys.ENTER);
            case "tab": return Gdx.input.isKeyPressed(Input.Keys.TAB);
            case "escape": return Gdx.input.isKeyPressed(Input.Keys.ESCAPE);
            case "backspace": return Gdx.input.isKeyPressed(Input.Keys.BACKSPACE);
            case "shift": return Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);
            case "control": return Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT);
            default: return false;
        }
    }
}
