package com.dbash.platform;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.dbash.models.UIInfoListenerBag;


/**
 * I need to load all the sounds into memory, and then just play them.   I dont need to dispose of them as such.
 *
 * For music, we have a collection of Song objects that have their own state and know how to fade their own volume
 * by an amount based on the master volume.
 * The Audio class just tells them to play or stop with a boolean fade parameter.
 * It keeps track of the currentTheme so it knows not to tell that one to stop if it is already playing.
 */
public class Audio {

	// Sounds
	public static final String BAD_EFFECT = "Bestow Bad";
	public static final String GOOD_EFFECT = "Bestow Good";
	public static final String CHEMICAL_BURST = "Chemical Multiple";
	public static final String CHEMICAL_HIT = "Chemical Single";
	public static final String CLICK = "Click";
	public static final String DEATH = "Death";
	public static final String DEFEND = "Defend";
	public static final String ENERGY_BURST = "Energy Multiple";
	public static final String ENERGY_HIT = "Energy Single";
	public static final String HARD_BURST = "Hard Multiple";
	public static final String HARD_HIT = "Hard Single";
	public static final String HIDING = "Hiding";

	public static final String ITEM_DROP = "Item Drop";	
	public static final String ITEM_PICKUP = "Item Pickup";
	public static final String ITEM_SELECT_ACC = "Item Select Accessory";
	public static final String ITEM_SELECT_MELEE = "Item Select Melee"; 
	public static final String ITEM_SELECT_PROJ = "Item Select Projectile"; 
	
	public static final String KNOCK_BURST = "Knock Multiple";
	public static final String KNOCK_HIT = "Knock Single";
	
	public static final String MISS = "Melee Miss";
	public static final String NEGATIVE = "Negative";
	public static final String RESIST = "Resist";
	public static final String RANGED_ATTACK = "Projectile Whoosh";
	
	public static final String SHARP_BURST = "Sharp Multiple";
	public static final String SHARP_HIT = "Sharp Single";
	
	public static final String DROP_IN = "Stairs Arriving";
	public static final String STAIR_DOWN = "Stairs Down";
	public static final String GAME_OVER_TUNE = "Game Over Theme";
	public static final String ZAP = "Zap";

	
	// Music
	public static final String BATTLE_THEME = "Battle Theme";  
	public static final String MOVING_THEME = "Exploring Theme";  
	
	protected HashMap<String, Sound> sounds;
	protected HashMap<String, Song> music;
	
	float fxVolume = 0f;
	float musicVolume = 0f;
	
	protected Song currentTheme = null;
	public UIInfoListenerBag musicVolumeListeners;
	public UIInfoListenerBag fxVolumeListeners;
	
	public Audio() {
		musicVolumeListeners = new UIInfoListenerBag();
		fxVolumeListeners = new UIInfoListenerBag();
		sounds = new HashMap<String, Sound>();
		addSound(BAD_EFFECT);
		addSound(GOOD_EFFECT);
		addSound(CHEMICAL_BURST);
		addSound(CHEMICAL_HIT);
		addSound(CLICK);
		addSound(DEATH);
		addSound(DEFEND);
		addSound(ENERGY_BURST);
		addSound(ENERGY_HIT);
		addSound(HARD_BURST);
		addSound(HARD_HIT);
		addSound(HIDING);
		addSound(ITEM_DROP);	
		addSound(ITEM_PICKUP);
		addSound(ITEM_SELECT_ACC);
		addSound(ITEM_SELECT_MELEE);
		addSound(ITEM_SELECT_PROJ);
		addSound(KNOCK_BURST);
		addSound(KNOCK_HIT);
		addSound(MISS);
		addSound(NEGATIVE);
		addSound(RESIST);
		addSound(RANGED_ATTACK);
		addSound(SHARP_BURST);
		addSound(SHARP_HIT);
		addSound(DROP_IN);
		addSound(STAIR_DOWN);
		addSound(GAME_OVER_TUNE);
		addSound(ZAP);
		
		music = new HashMap<String, Song>();
		addMusic(BATTLE_THEME);
		addMusic(MOVING_THEME);
		
		setMusicVolume(0f);
		setFxVolume(0f);
}
	
	protected void addSound(String name) {
		Sound sound = Gdx.audio.newSound(Gdx.files.internal("res/dbash2/fx/" + name + ".mp3"));
		sounds.put(name,  sound);
	}

	protected void addMusic(String name) {
		Music themeMusic = Gdx.audio.newMusic(Gdx.files.internal("res/dbash2/music/" + name + ".mp3"));
		themeMusic.setLooping(true);
		Song song = new Song(themeMusic);
		music.put(name,  song);
	}
	
	public void playSound(String sound) {
		sounds.get(sound).play(fxVolume*fxVolume);  // squared for better sensitivity at low volumes
	}
	
	/*
	 * Stops all music from playing
	 */
	public void stopMusic() {
		for (Song song : music.values()) {
			song.stop(false);
		}
	}
	
	public void playMusic(String name, boolean fade) {
		Song theme = music.get(name);
		
		for (Song song : music.values()) {
			if (song != theme) {
				song.stop(fade);
			} else {
				song.play(fade);
			}
		}
	}
	
	public void setMusicVolume(float volume) {
		musicVolume = volume;   // square it to give it more sensitivity at low volumes
		for (Song song : music.values()) {
			song.setMasterVolume(volume*volume);
		}
	}
	
	public float getFxVolume() {
		return fxVolume;
	}
	
	public float getMusicVolume( ) {
		return musicVolume;
	}
	
	public void setFxVolume(float volume) {
		fxVolume = volume;
	}
	
	/**
	 * 
	 */
	public void processVolume() {
		for (Song song : music.values()) {
			song.processVolume();
		}
	}
	
	public void defaultVolume() {
		setFxVolume(1f);
		setMusicVolume(1f);
	}
	
	public void persist(ObjectOutputStream out) throws IOException {
		out.writeFloat(fxVolume);
		out.writeFloat(musicVolume);
	}
	
	public void load(ObjectInputStream in) throws IOException, ClassNotFoundException
	{
		fxVolume = in.readFloat();
		musicVolume = in.readFloat();
		setMusicVolume(musicVolume);
		musicVolumeListeners.alertListeners();
		fxVolumeListeners.alertListeners();
	}
}