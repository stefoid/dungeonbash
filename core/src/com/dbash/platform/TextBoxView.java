package com.dbash.platform;

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
	
	public TextBoxView(UIDepend gui, String text, Rect singleLineArea, HAlignment hAlign, Color color) {
		TextView testTextView = new TextView(gui, "A", singleLineArea, color);
		this.font = testTextView.getFont();  // This is the size of font to use.
		this.text = text;
		this.gui = gui;
		this.area = singleLineArea;
		this.hAlign = hAlign;
		this.color = color;
		TextBounds textBounds = font.getWrappedBounds(text, singleLineArea.width);
		totalHeight = textBounds.height;
	}
	
	public float getTotalHeight() {
		return totalHeight;
	}
	
	public void draw(SpriteBatch spriteBatch, float x, float y) {
		if (text.length() == 0) {
			return;
		}
		
		font.setScale(1f);
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
	}
}
