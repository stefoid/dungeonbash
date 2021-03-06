package com.dbash.platform;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dbash.util.Rect;
import com.dbash.util.Rect.HAlignment;

public class FadeBox extends TextBoxView
{
	ImageView backgroundImage;
	float yOffset;
	
	public FadeBox(UIDepend gui, String text, Rect area) {
		super(gui, gui.defaultFonts, text, 
				new Rect(area.x, area.y, area.width, gui.sizeCalculator.dungeonArea.height/30),
				HAlignment.LEFT , Color.WHITE);
		
		Rect backgroundArea = new Rect(area);
		yOffset = (area.height - totalHeight)/2;
		this.backgroundImage = new ImageView(gui, "GAME_OVER_BACKGROUND", backgroundArea);
	}

	@Override
	public void draw(SpriteBatch spriteBatch, float x, float y) {
		backgroundImage.draw(spriteBatch, x, y);
		super.draw(spriteBatch, x, y+yOffset);
	}
	
	public void draw(SpriteBatch spriteBatch, float x, float y, float alpha) {
		backgroundImage.draw(spriteBatch, x, y, 1f);
		super.draw(spriteBatch, x, y+yOffset, alpha);
	}
}
