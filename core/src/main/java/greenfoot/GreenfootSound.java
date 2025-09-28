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
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;

/**
 * LibGDX-based representation of audio that can be played in Greenfoot. 
 * This implementation uses LibGDX Sound and Music classes.
 * 
 * This class re-implements greenfoot.GreenfootSound to provide a LibGDX backend,
 * mainly to allow Greenfoot projects to run on LibGDX (especially to export into
 * mobile devices and other platforms).
 * 
 * Inspired by the original Greenfoot project (GPLv2+ with Classpath Exception).
 * Read the original documentation at
 * https://www.greenfoot.org/files/javadoc/greenfoot/GreenfootSound.html
 * 
 * @author Poul Henriksen (Original Greenfoot version's author)
 * 
 * @modified-by Qiupi3 (LibGDX wrapper implementation)
 * @version 1.0
 */
public class GreenfootSound {
    private Sound sound;
    private Music music;
    private boolean isMusic = false;
    private float volume = 1.0f;
    private String filename;
    private long soundId = -1;
    private boolean isCurrentlyPlaying = false;
    private boolean isPaused = false;
    private boolean isLooping = false;

    /**
     * Creates a new sound from the given file. 
     * 
     * @param filename Typically the name of a file in the sounds directory in
     *            the project directory. 
     */
    public GreenfootSound(String filename) {
        this.filename = filename;
        loadSound(filename);
    }

    /**
     * Load sound file using LibGDX audio system.
     * Tries multiple locations and handles various audio formats.
     */
    private void loadSound(String filename) {
        try {
            FileHandle file = Gdx.files.internal("sounds/" + filename);
            if (!file.exists()) {
                file = Gdx.files.internal(filename);
            }
            
            if (file != null && file.exists()) {
                String lowerName = filename.toLowerCase();
                // Use Music for longer audio files (typically mp3, ogg, m4a)
                // Use Sound for shorter audio files (wav, short ogg/mp3)
                if (lowerName.endsWith(".mp3") || lowerName.endsWith(".ogg") || 
                    lowerName.endsWith(".m4a") || lowerName.endsWith(".flac")) {
                    
                    // For very short files, still use Sound even if they're mp3/ogg
                    if (file.length() < 1024 * 1024) { // Less than 1MB, use Sound
                        this.sound = Gdx.audio.newSound(file);
                        this.isMusic = false;
                    } else {
                        this.music = Gdx.audio.newMusic(file);
                        this.isMusic = true;
                    }
                } else {
                    // WAV and other formats typically use Sound
                    this.sound = Gdx.audio.newSound(file);
                    this.isMusic = false;
                }
            } else {
                Gdx.app.error("GreenfootSound", "Sound file not found: " + filename);
            }
        } catch (Exception e) {
            Gdx.app.error("GreenfootSound", "Could not load sound: " + filename, e);
        }
    }

    /**
     * Start playing this sound. If it is playing already, it will do
     * nothing. If the sound is currently looping, it will finish the current
     * loop and stop. If the sound is currently paused, it will resume playback
     * from the point where it was paused. The sound will be played once.
     */
    public void play() {
        if (isMusic && music != null) {
            if (isPaused) {
                music.play(); // Resume from pause
                isPaused = false;
            } else if (!music.isPlaying()) {
                music.setLooping(false);
                music.play();
                isLooping = false;
            }
            isCurrentlyPlaying = true;
        } else if (sound != null) {
            if (!isCurrentlyPlaying || soundId == -1) {
                soundId = sound.play(volume);
                isCurrentlyPlaying = true;
                isLooping = false;
            }
            isPaused = false;
        }
    }

    /**
     * Play this sound repeatedly in a loop. If called on an already looping
     * sound, it will do nothing. If the sound is already playing once, it will
     * start looping instead. If the sound is currently paused, it will resume
     * playing from the point where it was paused.
     */
    public void playLoop() {
        if (isMusic && music != null) {
            if (isPaused) {
                music.play(); // Resume from pause
                music.setLooping(true);
                isPaused = false;
            } else if (!music.isPlaying() || !isLooping) {
                if (music.isPlaying() && !isLooping) {
                    // If playing but not looping, make it loop
                    music.setLooping(true);
                } else {
                    // Start playing with loop
                    music.setLooping(true);
                    music.play();
                }
                isLooping = true;
            }
            isCurrentlyPlaying = true;
        } else if (sound != null) {
            if (!isLooping || soundId == -1) {
                if (soundId != -1) {
                    sound.stop(soundId); // Stop current playback
                }
                soundId = sound.loop(volume);
                isLooping = true;
            }
            isCurrentlyPlaying = true;
            isPaused = false;
        }
    }

    /**
     * Stop playing this sound if it is currently playing. If the sound is
     * played again later, it will start playing from the beginning. If the
     * sound is currently paused it will now be stopped instead.
     */
    public void stop() {
        if (isMusic && music != null) {
            music.stop();
        } else if (sound != null && soundId != -1) {
            sound.stop(soundId);
            soundId = -1;
        }
        isCurrentlyPlaying = false;
        isPaused = false;
        isLooping = false;
    }

    /**
     * Pauses the current sound if it is currently playing. If the sound is
     * played again later, it will resume from the point where it was paused.
     * <p>
     * Make sure that this is really the method you want. If possible, you
     * should always use {@link #stop()}, because the resources can be released
     * after calling {@link #stop()}. The resources for the sound will not be
     * released while it is paused.
     * @see #stop()
     */
    public void pause() {
        if (isCurrentlyPlaying && !isPaused) {
            if (isMusic && music != null && music.isPlaying()) {
                music.pause();
                isPaused = true;
            } else if (sound != null && soundId != -1) {
                sound.pause(soundId);
                isPaused = true;
            }
        }
    }

    /**
     * True if the sound is currently playing.
     *
     * @return Whether the sound is currently playing.
     */
    public boolean isPlaying() {
        if (isMusic && music != null) {
            return music.isPlaying() && !isPaused;
        } else if (sound != null) {
            // For Sound objects, LibGDX doesn't provide isPlaying method directly
            // We track the state manually
            return isCurrentlyPlaying && !isPaused && soundId != -1;
        }
        return false;
    }

    /**
     * Get the current volume of the sound, between 0 (off) and 100 (loudest.)
     * 
     * @return A number between 0-100 represents the current sound volume.
     */
    public int getVolume() {
        return (int) (volume * 100);
    }

    /**
     * Set the current volume of the sound between 0 (off) and 100 (loudest.)
     * @param level the level to set the sound volume to.
     */
    public void setVolume(int level) {
        this.volume = level / 100.0f;
        
        if (isMusic && music != null) {
            music.setVolume(volume);
        } else if (sound != null && soundId != -1) {
            sound.setVolume(soundId, volume);
        }
    }

    /**
     * Check if the sound is currently paused.
     * 
     * @return true if the sound is paused, false otherwise
     */
    public boolean isPaused() {
        return isPaused;
    }
    
    /**
     * Check if the sound is currently looping.
     * 
     * @return true if the sound is looping, false otherwise
     */
    public boolean isLooping() {
        return isLooping;
    }
    
    /**
     * Get the filename of this sound.
     * 
     * @return the filename used to create this sound
     */
    public String getFilename() {
        return filename;
    }
    
    /**
     * Dispose of this sound and free its resources.
     * After calling dispose(), this sound should not be used anymore.
     */
    public void dispose() {
        stop();
        if (sound != null) {
            sound.dispose();
            sound = null;
        }
        if (music != null) {
            music.dispose();
            music = null;
        }
        soundId = -1;
        isCurrentlyPlaying = false;
        isPaused = false;
        isLooping = false;
    }

    /**
     * Returns a string representation of this sound containing the name of the
     * file and whether it is currently playing or not.
     */
    public String toString() {
        String s = super.toString() + " file: " + filename + " ";
        if (sound != null || music != null) {
            s += ". Is playing: " + isPlaying();
            if (isPaused) s += " (paused)";
            if (isLooping) s += " (looping)";
        }
        else {
            s += ". Not found.";
        }
        return s;
    }
}
