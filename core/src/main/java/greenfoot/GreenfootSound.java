package greenfoot;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;

/**
 * LibGDX-based representation of audio that can be played in Greenfoot. 
 * This implementation uses LibGDX Sound and Music classes.
 * The sound cannot be played several times simultaneously, but can be played several times sequentially. 
 * 
 * <p>Most files of the following formats are supported: AIFF, AU, WAV, MP3 and MIDI.
 * 
 * @author Poul Henriksen
 * @version 2.4
 */
public class GreenfootSound
{
    private Sound sound;
    private Music music;
    private boolean isMusic = false;
    private float volume = 1.0f;
    private String filename;
    private long soundId = -1;

    /**
     * Creates a new sound from the given file. 
     * 
     * @param filename Typically the name of a file in the sounds directory in
     *            the project directory. 
     */
    public GreenfootSound(String filename)
    {
        this.filename = filename;
        loadSound(filename);
    }

    /**
     * Load sound file using LibGDX audio system.
     */
    private void loadSound(String filename) {
        try {
            FileHandle file = Gdx.files.internal("sounds/" + filename);
            if (!file.exists()) {
                file = Gdx.files.internal(filename);
            }
            
            if (file.exists()) {
                // Use Sound for short audio clips, Music for longer files
                if (filename.toLowerCase().endsWith(".mp3") || filename.toLowerCase().endsWith(".ogg")) {
                    // For longer audio files, use Music
                    this.music = Gdx.audio.newMusic(file);
                    this.isMusic = true;
                } else {
                    // For shorter audio files, use Sound
                    this.sound = Gdx.audio.newSound(file);
                    this.isMusic = false;
                }
            }
        } catch (Exception e) {
            Gdx.app.log("GreenfootSound", "Could not load sound: " + filename, e);
        }
    }

    /**
     * Start playing this sound. If it is playing already, it will do
     * nothing. If the sound is currently looping, it will finish the current
     * loop and stop. If the sound is currently paused, it will resume playback
     * from the point where it was paused. The sound will be played once.
     */
    public void play()
    {
        if (isMusic && music != null) {
            if (!music.isPlaying()) {
                music.setLooping(false);
                music.play();
            }
        } else if (sound != null) {
            soundId = sound.play(volume);
        }
    }

    /**
     * Play this sound repeatedly in a loop. If called on an already looping
     * sound, it will do nothing. If the sound is already playing once, it will
     * start looping instead. If the sound is currently paused, it will resume
     * playing from the point where it was paused.
     */
    public void playLoop()
    {
        if (isMusic && music != null) {
            if (!music.isPlaying()) {
                music.setLooping(true);
                music.play();
            } else {
                music.setLooping(true);
            }
        } else if (sound != null) {
            soundId = sound.loop(volume);
        }
    }

    /**
     * Stop playing this sound if it is currently playing. If the sound is
     * played again later, it will start playing from the beginning. If the
     * sound is currently paused it will now be stopped instead.
     */
    public void stop()
    {
        if (isMusic && music != null) {
            music.stop();
        } else if (sound != null && soundId != -1) {
            sound.stop(soundId);
            soundId = -1;
        }
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
    public void pause()
    {
        if (isMusic && music != null) {
            music.pause();
        } else if (sound != null && soundId != -1) {
            sound.pause(soundId);
        }
    }

    /**
     * True if the sound is currently playing.
     *
     * @return Whether the sound is currently playing.
     */
    public boolean isPlaying()
    {
        if (isMusic && music != null) {
            return music.isPlaying();
        } else if (sound != null) {
            // For Sound objects, LibGDX doesn't provide isPlaying method directly
            // This is a simplified implementation
            return soundId != -1;
        }
        return false;
    }

    /**
     * Get the current volume of the sound, between 0 (off) and 100 (loudest.)
     * 
     * @return A number between 0-100 represents the current sound volume.
     */
    public int getVolume()
    {
        return (int) (volume * 100);
    }

    /**
     * Set the current volume of the sound between 0 (off) and 100 (loudest.)
     * @param level the level to set the sound volume to.
     */
    public void setVolume(int level)
    {
        this.volume = level / 100.0f;
        
        if (isMusic && music != null) {
            music.setVolume(volume);
        } else if (sound != null && soundId != -1) {
            sound.setVolume(soundId, volume);
        }
    }

    /**
     * Returns a string representation of this sound containing the name of the
     * file and whether it is currently playing or not.
     */
    public String toString()
    {
        String s = super.toString() + " file: " + filename + " ";
        if (sound != null) {
            s += ". Is playing: " + isPlaying();
        }
        else {
            s += ". Not found.";
        }
        return s;
    }
}
