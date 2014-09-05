package com.dbash.platform;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dbash.util.Rect;



public class ImageView {
	// in case you need access to the Sprite
	public Sprite sprite;
	protected SpriteManager spriteManager;
	protected Rect area;
	protected float originalWidth;
	protected float originalHeight;
	public String name;
	
	// for an texture without frame indexes
	public ImageView(UIDepend gui, String imageType, Rect area) {
		this.spriteManager = gui.spriteManager;
		sprite = spriteManager.createSprite(imageType);
		originalWidth = sprite.getWidth();
		originalHeight = sprite.getHeight();
		name = imageType;
		setArea(area);
	}
	
	// For a texture that is a frame in an animation sequence.
	// frames ae numbered in the file system from _1, so  do not use 0.
	public ImageView(UIDepend gui, String imageType, int frame, Rect area) {
		this.spriteManager = gui.spriteManager;
		sprite = spriteManager.createSprite(imageType, frame-1);
		name = imageType;
		setArea(area);
	}
	
	public ImageView(){};
	
	public void draw(SpriteBatch spriteBatch) {
		sprite.draw(spriteBatch);
	}
	
	public void drawTinted(SpriteBatch spriteBatch) {
		sprite.setColor(.5f, .5f, .5f, 1f);
		draw(spriteBatch);
		sprite.setColor(Color.WHITE);
	}
	
	public void draw(SpriteBatch spriteBatch, float alpha) {
		sprite.setColor(1f, 1f, 1f, alpha);
		draw(spriteBatch);
		sprite.setColor(Color.WHITE);
	}
	
	public void drawTinted(SpriteBatch spriteBatch, float tint, float alpha) {
		sprite.setColor(tint, tint, tint, alpha);
		draw(spriteBatch);
		sprite.setColor(Color.WHITE);
	}
	
	public void draw(SpriteBatch spriteBatch, float x, float y) {
		sprite.setBounds(area.x+x , area.y+y, area.width, area.height);
		draw(spriteBatch);
	}

	public void setArea(Rect area) {
		this.area = new Rect(area);
		sprite.setBounds(area.x , area.y, area.width, area.height);
	}
	
	public void setPos(float x, float y) {
		area.x = x;
		area.y = y;
		sprite.setBounds(area.x , area.y, area.width, area.height);
	}
	
	public void setRotation(float rotation) {
		sprite.setOrigin(area.width/2, area.height/2);
		sprite.setRotation(rotation);
	}
	
	public float getOriginalWidth() {
		return originalWidth;
	}
	
	public float getOriginalHeight() {
		return originalHeight;
	}
	
}
