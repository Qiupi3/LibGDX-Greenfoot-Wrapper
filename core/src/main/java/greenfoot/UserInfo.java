package greenfoot;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

import java.util.List;
import java.util.ArrayList;

/**
 * <p>The UserInfo class can be used to store data permanently on a server, and
 * to share this data between different users, when the scenario runs on the
 * Greenfoot web site. This can be used to implement shared high score tables
 * or other examples of shared data.</p>
 *
 * <p>Storage is only available when the current user is logged in on the Greenfoot
 * site, so for some users storage will not be available. Always use
 * UserInfo.isStorageAvailable() to check before accessing the user data.</p>
 *
 * <p>A typical code snippet for storing a high score is as follows:</p>
 *
 * <pre>
 *     if (UserInfo.isStorageAvailable()) {
 *         UserInfo myInfo = UserInfo.getMyInfo();
 *         if (newScore &gt; myInfo.getScore()) {
 *             myInfo.setScore(newScore);
 *             myInfo.store();  // write back to server
 *         }
 *     }
 * </pre>
 * <p>Methods to retrieve user data include getting data for the current user
 * (getMyInfo()), the top scorers (e.g. getTop(10) for the top 10), and data
 * for users with scores near my own score (e.g. getNearby(10)).</p>
 *
 * <p>The data that can be stored for each user consists of a score, 10
 * additional general purpose integers, and 5 strings (limited to 50 characters
 * in length). In addition, the user name and user's image can be retrieved from
 * the user data.</p>
 *
 * <p>For testing purposes, while running within Greenfoot (not on the web site),
 * the user name can be set in the preferences (CTRL-SHIFT-P / CMD-SHIFT-P).
 * This allows to simulate different users during development. When running
 * on the web site, the user name is the name used to log in to the site.</p>
 * 
 * @author Neil Brown
 * @version 2.4
 */
public class UserInfo
{
    // These may enlarge in future:
    
    /** The number of integers that can be stored */
    public static final int NUM_INTS = 10;
    /** The number of Strings that can be stored */
    public static final int NUM_STRINGS = 5;
    /** The maximum number of characters that can be stored in each String */    
    public static final int STRING_LENGTH_LIMIT = 50;
    // NB the above limit matches the database schema in the gallery storage
    // so don't alter it!
    private int[] ints;
    private String[] strings;
    private String userName;
    private int score;
    private int rank;
    
    //package-visible:
    UserInfo(String userName, int rank)
    {
        this.userName = userName;
        this.rank = rank;
        score = 0;
        ints = new int[NUM_INTS];
        strings = new String[NUM_STRINGS];
    }
    
    //package-visible:
    void setRank(int n)
    {
        rank = n;
    }
    
    /**
     * Get the username of the user that this storage belongs to.
     * 
     * @return The username as a String.
     */
    public String getUserName()
    {
        return userName;
    }
    
    /**
     * Get the value of the int at the given index (0 to NUM_INTS-1, inclusive).
     * <p>
     * The default value is zero.
     * 
     * @param index  The index of the array where the needed int is positioned.
     * @return The value of the number in the given index of the array.
     */
    public int getInt(int index)
    {
        return ints[index];
    }
    
    /**
     * Get the value of the String at the given index (0 to NUM_STRINGS-1, inclusive).
     *
     * The default value is the empty String.
     *
     * @param index The index at which to fetch a string
     * @return The String at that index, or the empty String if none has been stored.
     */
    public String getString(int index)
    {
        return strings[index] == null ? "" : strings[index];
    }
    
    /**
     * Set the value of the int at the given index (0 to NUM_INTS-1, inclusive).
     * 
     * Note that to store this value permanently, you must later call store().
     *
     * @param index The index at which to store the integer
     * @param value The integer value to store
     */
    public void setInt(int index, int value)
    {
        ints[index] = value;
    }

    /**
     * Get the value of the String at the given index (0 to NUM_STRINGS-1, inclusive).
     * Passing null is treated as a blank string.  The given String must be of STRING_LENGTH_LIMIT
     * characters or less (or else the method will fail).
     * 
     * Note that to store this value permanently, you must later call store().
     *
     * @param index The index at which to store the String
     * @param value The String value to store.
     */
    public void setString(int index, String value)
    {
        if (value != null && value.length() > STRING_LENGTH_LIMIT)
        {
            System.err.println("Error: tried to store a String of length " + value.length() + " in UserInfo, which is longer than UserInfo.STRING_LENGTH_LIMIT (" + STRING_LENGTH_LIMIT + ")");
        }
        else
        {
            strings[index] = value;
        }
    }
    
    /**
     * Get the user's score.  By default, this is zero.
     *
     * @return The user's score.
     */
    public int getScore()
    {
        return score;
    }
    
    /**
     * Set the user's score.
     * <p>
     * Note that this really does set the user's score.  If you want to record only the user's highest
     * score, you must code that yourself, using something like:
     * <pre>
     *   if (latestScore &gt; userData.getScore())
     *   {
     *     userData.setScore(latestScore);
     *   }
     * </pre>
     * Without some code like this, you'll always overwrite the user's previous score.
     * 
     * <p>Note that to store this value permanently, you must later call store().
     *
     * @param score The score to set
     */
    public void setScore(int score)
    {
        this.score = score;
    }
    
    /**
     * Get the users overall rank for this scenario.
     * <p>
     * The user with the highest score will return 1, the user with the second highest score
     * will return 2, and so on.  Players with equal scores will get equal ranks,
     * so rank will not necessarily be unique.  To find the rank, scores are sorted
     * in descending order (highest score first).  If your scores need to be lowest-first,
     * one trick is to store them as negative numbers.
     * <p>
     * If the rank is unavailable (e.g. because the data hasn't been stored yet), this function will return -1.
     *
     * @return The user's overall rank for this scenario.
     */
    public int getRank()
    {
        return rank;
    }
    
    /**
     * Indicate whether storage is available.
     * <p>
     * With LibGDX, local storage is always available through Preferences.
     * Server-based storage is not supported in this implementation.
     *
     * @return Whether storage is available.
     */
    public static boolean isStorageAvailable()
    {
        // LibGDX Preferences are always available for local storage
        return true;
    }
    
    /**
     * Get the data stored for the current user using LibGDX Preferences.
     * 
     * This method returns null if there was a problem accessing local storage.
     * Since this uses local storage, it doesn't support multiple users on a server.
     * 
     * @return the user's data, or null if there was a problem.
     */
    public static UserInfo getMyInfo()
    {
        try {
            Preferences prefs = Gdx.app.getPreferences("greenfoot-userinfo");
            
            // Get the username (default to "Player" if not set)
            String username = prefs.getString("username", "Player");
            
            // Create UserInfo object
            UserInfo info = new UserInfo(username, 1); // Rank 1 for local user
            
            // Load stored data
            info.score = prefs.getInteger("score", 0);
            
            // Load integers array
            for (int i = 0; i < NUM_INTS; i++) {
                info.ints[i] = prefs.getInteger("int_" + i, 0);
            }
            
            // Load strings array
            for (int i = 0; i < NUM_STRINGS; i++) {
                info.strings[i] = prefs.getString("string_" + i, "");
            }
            
            return info;
        } catch (Exception e) {
            // Return null if there was any problem
            return null;
        }
    }
    
    /**
     * Store the data using LibGDX Preferences.
     * <p>
     * This implementation stores data locally using LibGDX Preferences.
     * Server-based storage is not supported.
     * 
     * @return true if stored successfully, false if there was a problem.
     */
    public boolean store()
    {
        try {
            Preferences prefs = Gdx.app.getPreferences("greenfoot-userinfo");
            
            // Store basic data
            prefs.putString("username", userName);
            prefs.putInteger("score", score);
            
            // Store integers array
            for (int i = 0; i < NUM_INTS; i++) {
                prefs.putInteger("int_" + i, ints[i]);
            }
            
            // Store strings array  
            for (int i = 0; i < NUM_STRINGS; i++) {
                prefs.putString("string_" + i, strings[i] != null ? strings[i] : "");
            }
            
            // Flush changes to persistent storage
            prefs.flush();
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get a sorted list of the UserInfo items for this scenario, starting at the top.
     * 
     * <p>In this LibGDX implementation, since we only support local storage,
     * this will return a list containing only the current user's data.</p>
     * 
     * <p>Returns null if there was a problem accessing local storage.</p>
     * 
     * @param maxAmount The maximum number of data items to retrieve (ignored in local implementation).
     * @return A list with the current user's data, or null if there was a problem
     */
    public static List<UserInfo> getTop(int maxAmount)
    {
        UserInfo myInfo = getMyInfo();
        if (myInfo != null) {
            List<UserInfo> result = new ArrayList<UserInfo>();
            result.add(myInfo);
            return result;
        }
        return null;
    }
    
    /**
     * Get a sorted list of the UserInfo items for this scenario surrounding the current user.
     * 
     * <p>In this LibGDX implementation, since we only support local storage,
     * this will return a list containing only the current user's data.</p>
     * 
     * <p>Returns null if there was a problem accessing local storage.</p>
     *
     * @param maxAmount The maximum number of data items to retrieve (ignored in local implementation).  
     * @return A list with the current user's data, or null if there was a problem
     */
    public static List<UserInfo> getNearby(int maxAmount)
    {
        // Same as getTop for local implementation
        return getTop(maxAmount);
    }
    
    /**
     * Return an image of the user. The image size is 50x50 pixels.
     * <p>
     * In this LibGDX implementation, this creates a simple 50x50 pixel image
     * representing the user.
     *
     * @return A 50x50 pixel GreenfootImage
     */
    public GreenfootImage getUserImage()
    {
        // Create a simple placeholder image
        // The actual implementation would depend on the GreenfootImage class
        GreenfootImage image = new GreenfootImage(50, 50);
        
        // Note: Additional customization could be added here based on username
        // This would depend on the specific GreenfootImage drawing methods available
        
        return image;
    }
}
