package com.dbash.platform;

import java.util.ArrayList;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFont.TextBounds;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dbash.util.Rect;
import com.dbash.util.Rect.HAlignment;

// You supply a rectangle for a single line of text.
// And it does line wrapping within the box and horizontal and vertical alignment.
// It works out the size of font that will fit best height-wise for the single line,
// then does line-wrapping across the width using that size font.
public class TextBoxView {
	
	UIDepend gui;
	String text;
	BitmapFont font;
	Rect area;
	HAlignment hAlign;
	Color color;
	float totalHeight;
	
	public TextBoxView(UIDepend gui, ArrayList<SmoothBitmapFont> fontList, String text, Rect singleLineArea, HAlignment hAlign, Color color) {
		
		if (fontList == null) {
			fontList = gui.defaultFonts;
		}
		
		// cycle through all the default font sizes, biggest first.
		for (int i = fontList.size() - 1; i >= 0; i--) {
			this.font = fontList.get(i);

			// get the width of the text, for setting the text X and text Y.  Try big font first.
			if (singleLineArea.height <= font.getCapHeight()) {
				break;
			}
		}
		
		this.text = text;
		this.gui = gui;
		this.area = new Rect(singleLineArea);
		this.hAlign = hAlign;
		this.color = color;
		TextBounds textBounds = font.getWrappedBounds(text, singleLineArea.width);
		totalHeight = textBounds.height;
	}
	
	public float getTotalHeight() {
		return totalHeight;
	}
	
	public void setArea(Rect area) {
		this.area = new Rect(area);
	}
	
	public void draw(SpriteBatch spriteBatch, float x, float y, float alpha) {
		if (text.length() == 0) {
			return;
		}
		
		font.setScale(1f);
		float oldAlpha = color.a;
		color.a = alpha;
		font.setColor(color);
		
		BitmapFont.HAlignment h = BitmapFont.HAlignment.LEFT;
		
		switch (hAlign)
		{
			case CENTER:
				h = BitmapFont.HAlignment.CENTER;
				break;
			case RIGHT:
				h = BitmapFont.HAlignment.RIGHT;
				break;
			case LEFT:
				break;
		}
		
		// text x,y defined from bottom left of text box area
		font.drawWrapped(spriteBatch, text, area.x + x, area.y + y + totalHeight, area.width, h);
		color.a = oldAlpha;
	}
	
	public void draw(SpriteBatch spriteBatch, float x, float y) {
		draw (spriteBatch, x, y, 1f);
	}
}
