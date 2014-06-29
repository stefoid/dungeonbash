package com.dbash.platform;

import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.Sprite;

public interface SpriteManager {

	// Creates a new instance of a sprite
	public Sprite createSprite(String imageType);
	public Sprite createSprite(String imageType, int frame);
	
	// returns a reference to the 'master' sprite
	public Sprite fetchSprite(String imageType);
	public Sprite fetchSprite(String imageType, int frame);

	// info
	public int maxFrame(String imageType);
	
	public NinePatch create9Patch(String imageType);
}
