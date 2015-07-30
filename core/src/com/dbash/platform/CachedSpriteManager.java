package com.dbash.platform;

import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.Array;
import com.dbash.util.L;

public class CachedSpriteManager implements SpriteManager {

	private TextureAtlas textureAtlas;
	
	// for single images
	private Map<String, Sprite>	cache	= new HashMap<String, Sprite>();
	
	// for animation frames stored as sequences in an array
	private Map<String, Array<Sprite>>	cacheFrames	= new HashMap<String, Array<Sprite>>();


	public CachedSpriteManager() {
		String packedFile;
		if (L.JARFILE) {
			packedFile = "pngs-packed/pack.atlas";
		} else {
			packedFile = "res/dbash2/pngs-packed/pack.atlas";
		}
		textureAtlas = new TextureAtlas(Gdx.files.internal(packedFile));
	}
	
	// returns a ref to the 'master' sprite.  
	@Override
	public Sprite fetchSprite(String imageType) {
		Sprite sprite = cache.get(imageType);
		if (sprite == null) {
			sprite = textureAtlas.createSprite(imageType);
			cache.put(imageType, sprite);
		}
		return sprite;
	}
	
	// returns a new instance of a sprite
	@Override
	public Sprite createSprite(String imageType) {
		return new Sprite(fetchSprite(imageType));
	}

	@Override
	public Sprite createSprite(String imageType, int frame) {
		return new Sprite(fetchSprite(imageType, frame));
	}

	@Override
	public Sprite fetchSprite(String imageType, int frame) {
		Array<Sprite> frames = cacheFrames.get(imageType);
		if (frames == null) {
			frames = textureAtlas.createSprites(imageType);
			cacheFrames.put(imageType, frames);
		}
		return frames.get(frame);
	}

	@Override
	public int maxFrame(String imageType) {
		// force a fetch.
		Array<Sprite> frames = cacheFrames.get(imageType);
		if (frames == null) {
			frames = textureAtlas.createSprites(imageType);
			cacheFrames.put(imageType, frames);
		}
		return frames.size;
	}

	@Override
	public NinePatch create9Patch(String imageType) {
		return textureAtlas.createPatch(imageType);
	}

	

}
