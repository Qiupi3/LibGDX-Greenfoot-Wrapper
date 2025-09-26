package greenfoot;

/**
 * Utility class for managing UserInfo objects in the LibGDX implementation.
 * Provides helper methods for allocating and managing user data.
 */
public class UserInfoVisitor
{
    private static UserInfo myInfo;
    
    /**
     * Allocate a UserInfo object, reusing singleton if appropriate.
     * 
     * @param userName The username for the UserInfo
     * @param rank The rank for the user  
     * @param singletonUserName The name of the singleton user (current user)
     * @return UserInfo object
     */
    public static UserInfo allocate(String userName, int rank, String singletonUserName)
    {
        if (singletonUserName != null && singletonUserName.equals(userName))
        {
            if (myInfo != null && myInfo.getUserName().equals(singletonUserName))
            {
                myInfo.setRank(rank);
            }
            else
            {
                myInfo = new UserInfo(userName, rank);
            }
            return myInfo;
        }
        else
        {
            return new UserInfo(userName, rank);
        }
    }
    
    /**
     * Create a GreenfootImage from byte array data.
     * In LibGDX implementation, this creates a simple placeholder image.
     * 
     * @param imageFileContents Byte array containing image data (ignored in this implementation)
     * @return A simple GreenfootImage placeholder
     */
    public static GreenfootImage readImage(byte[] imageFileContents)
    {
        // Create a simple 50x50 placeholder image
        // In a full implementation, you could decode the byte array using LibGDX's Pixmap
        return new GreenfootImage(50, 50);
    }
    
    /**
     * Get or create the singleton UserInfo for the current user using LibGDX preferences.
     * 
     * @return The current user's UserInfo
     */
    public static UserInfo getCurrentUserInfo()
    {
        if (myInfo == null) {
            myInfo = UserInfo.getMyInfo();
        }
        return myInfo;
    }
    
    /**
     * Clear the cached user info (useful for logout scenarios).
     */
    public static void clearCache()
    {
        myInfo = null;
    }
}
