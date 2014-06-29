package com.dbash.platform;

import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dbash.util.Rect;

public class ImagePatchView extends ImageView {
	
	NinePatch ninePatch;
	
	public ImagePatchView(UIDepend gui, String imageType, Rect area) {
		super();
		this.spriteManager = gui.spriteManager;
		ninePatch = spriteManager.create9Patch(imageType);
		name = imageType;
		setArea(area);
	}
	
	public void draw(SpriteBatch spriteBatch) {
		ninePatch.draw(spriteBatch, area.x, area.y, area.width, area.height);
	}
	
	public void draw(SpriteBatch spriteBatch, float x, float y) {
		ninePatch.draw(spriteBatch, area.x+x , area.y+y, area.width, area.height);
	}
	
	public void setArea(Rect area) {
		this.area = new Rect(area);
	}
}
