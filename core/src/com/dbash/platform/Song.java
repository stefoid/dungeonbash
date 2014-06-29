package com.dbash.platform;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.dbash.models.IAnimListener;
import com.dbash.util.Tween;



/**
 * A song has a state and responds to setMasterVolume(), Stop() with optional fade and start() with optional fade.
 * If it receives a repeated command to do something that it already is doing or has done, then it ignores it.
 * Otherwise it changes its state accordingly and does that thing, potentially using a Tween to manage a fade.
 * The fade out is a bit quicker than the fade in, so there will be a small period of almost silence when 
 * fade-swapping themes.
 */
public class Song {
	
	private enum State {
		STOPPED,
		FADING_IN,
		FADING_OUT,
		PLAYING
	}
	
	protected State state = State.STOPPED;
	public Music music;
	protected Tween volumeTween;
	protected float masterVolume;
	protected Audio audio;
	
	public Song(Music music) {
		this.music = music;
		state = State.STOPPED;
	}
	
	/** the master volume
	 */
	public void setMasterVolume(float volume) {
		masterVolume = volume;
	}
	
	public void stop(boolean fade) {
		if (fade == false || state == State.STOPPED) {
			music.stop();
			state = State.STOPPED;
			return;
		}
		
		if (state == State.FADING_OUT) {
			return;
		}
		
		// we need to fade out and we are either playing or fading in
		// fade out is 100% to 0% over 3 second.
		volumeTween = new Tween();
		state = State.FADING_OUT;
		volumeTween.init(1f, 0f, 3, new IAnimListener() {
			public void animEvent() {
				music.stop();
				state = State.STOPPED;
			}
		});
	}
	
	public void play(boolean fade) {
		if (fade == false) {
			if (state != State.PLAYING) {
				music.play();
				state = State.PLAYING;
			}

			return;
		}
		
		if (state == State.FADING_IN || state == State.PLAYING) {
			return;
		}
		
		// we need to fade in and we are not either stopped or fading out 
		// fade in is currentVolume to 100% over 10 seconds.
		float startVolume = 0f;
		if (state == State.FADING_OUT) {
			startVolume = volumeTween.getValue();
		} else {
			music.play();
		}
		
		volumeTween = new Tween();
		state = State.FADING_IN;
		volumeTween.init(startVolume, 1f, 10, new IAnimListener() {
			public void animEvent() {
				state = State.PLAYING;
			}
		});
	}
	
	/*
	 * To operate any volumeTween currently in use.
	 */
	public void processVolume() {
		if (state != State.STOPPED) {
			volumeTween.deltaTime(Gdx.graphics.getDeltaTime());
			music.setVolume(masterVolume*volumeTween.getValue());
		}
	}
}
