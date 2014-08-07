package com.dbash.platform;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFont.TextBounds;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dbash.util.Rect;
import com.dbash.util.Rect.HAlignment;
import com.dbash.util.Rect.VAlignment;

// You supply a rectangle for the text to appear in.  
// And it does line wrapping within the box and horizontal and vertical alignment.
// its based on TextView so the font is not scaled - depending on the font used and the size of the box, it may 
// exceed the boundaries.
public class TextBoxView extends TextView {
	
	public TextBoxView(UIDepend gui, String text, Rect area, HAlignment hAlign, VAlignment vAlign, Color color) {
		super(gui, null, text, area, hAlign, vAlign, color);
	}
	
	public void draw(SpriteBatch spriteBatch, float x, float y) {
		if (text.length() == 0) {
			return;
		}
		
		font.setScale(scale);
		font.setColor(color);
		
		BitmapFont.HAlignment  h = BitmapFont.HAlignment.LEFT;
		
		switch (hAlign)
		{
			case CENTER:
				h = BitmapFont.HAlignment.CENTER;
				break;
			case RIGHT:
				h = BitmapFont.HAlignment.RIGHT;
				break;
		}
		
		// text x,y defined frm bottom left of text box area
		font.drawWrapped(spriteBatch, text, area.x + x, area.y + y + area.height, area.width, h);
	}
	
	protected void setScaleAndPosition()
	{
		// avoids premature calculation!
		if ((area == null) || (text == null) || (text.length() == 0)) {
			return;
		}
		
		font = gui.defaultFonts.get(2);  // TODO random font for now
		
		// work out vertical alignment w.r.t the bounding box passed in.
		if (vAlign == Rect.VAlignment.CENTER) {
			TextBounds bounds = font.getWrappedBounds(text, area.width);
			area.y -= (area.height - bounds.height)/2;
		} else if (vAlign == Rect.VAlignment.CENTER) {
			area.y -= area.height;
		}
	}
}
