package com.dbash.platform;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture.TextureFilter;

public class FatOutlineFont extends EfficientScalingBitmapFont {
	public FatOutlineFont() {
		super(Gdx.files.internal("res/dbash2/fatoutline.fnt"), false);
		getRegion().getTexture().setFilter(TextureFilter.Linear, TextureFilter.Linear);
	}
}