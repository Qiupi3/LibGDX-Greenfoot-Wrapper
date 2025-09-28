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

/**
 * Utility class for managing UserInfo objects in the LibGDX implementation.
 * Provides helper methods for allocating and managing user data.
 * 
 * This class re-implements greenfoot.UserInfoVisitor to provide a LibGDX backend,
 * mainly to allow Greenfoot projects to run on LibGDX (especially to export into
 * mobile devices and other platforms).
 * 
 * Inspired by the original Greenfoot project (GPLv2+ with Classpath Exception).
 * Read the original documentation at
 * https://www.greenfoot.org/files/javadoc/greenfoot/package-summary.html
 * 
 * @modified-by Qiupi3 (LibGDX wrapper implementation)
 * @version 1.0
 */
public class UserInfoVisitor {
    private static UserInfo myInfo;
    
    /**
     * Allocate a UserInfo object, reusing singleton if appropriate.
     * 
     * @param userName The username for the UserInfo
     * @param rank The rank for the user  
     * @param singletonUserName The name of the singleton user (current user)
     * @return UserInfo object
     */
    public static UserInfo allocate(String userName, int rank, String singletonUserName) {
        if (singletonUserName != null && singletonUserName.equals(userName)) {
            if (myInfo != null && myInfo.getUserName().equals(singletonUserName)) {
                myInfo.setRank(rank);
            } else {
                myInfo = new UserInfo(userName, rank);
            }
            return myInfo;
        } else {
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
    public static GreenfootImage readImage(byte[] imageFileContents) {
        // Create a simple 50x50 placeholder image
        // In a full implementation, you could decode the byte array using LibGDX's Pixmap
        return new GreenfootImage(50, 50);
    }
    
    /**
     * Get or create the singleton UserInfo for the current user using LibGDX preferences.
     * 
     * @return The current user's UserInfo
     */
    public static UserInfo getCurrentUserInfo() {
        if (myInfo == null) {
            myInfo = UserInfo.getMyInfo();
        }
        return myInfo;
    }
    
    /**
     * Clear the cached user info (useful for logout scenarios).
     */
    public static void clearCache() {
        myInfo = null;
    }
}
