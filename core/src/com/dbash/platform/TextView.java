package com.dbash.platform;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dbash.util.Rect;
import com.dbash.util.Rect.HAlignment;
import com.dbash.util.Rect.VAlignment;

// Use a TextView or TextBoxView if you care more about displaying crisp text at the fonts 1:1 resolution.  scaled text looks pretty bad....
// TextView is different from TextImageView.  It tries not to scale the scale the text so that it retains a 1:1 scale and looks good.
// The bounding rectangle supplied is for alignment of the text rather than scaling of the text.  i.e.  if the text is left-aligned. centered
// or right aligned -> with respect to the boundary supplied.
// It only does one line of text - if you want wrapping, use a TextBoxView which is based on TextView.
// 
// You can supply the font to use in the longer version of the constructor.  If you dont supply a font, it will use a default font "gui.font"
// If the string does not fit within the horizontal bounds supplied, it will automatically switch to the default small font 'gui.smallfont' instead.
// if that doesnt fit, then too bad. 
// If you supply a different font, rather than switch to the default small font, it will scale the font by 2/3rds as a last resort.  its not that flexible...
//
// Because the font is not scaled to fit exact bounds, this text will take up more or less of the display according to the devices pixel density.
// A good thing to do might be to set the default gui.font and gui.smallfont at startup from a range of fonts supplied with the assets depending on the 
// discovered screen resolution.
public class TextView {

	static final float scale = 1f;
	static float nominalTextHeight = 0;
	public Sprite sprite;
	protected Rect area;
	CharSequence text;
	BitmapFont font;
	Color color;
	float textScale;
	HAlignment hAlign;
	VAlignment vAlign;
	UIDepend gui;
	boolean deafultFontUsed = true;
	
	// these coordinates calculated to draw the text within the specified rectangle, according to the alignment passed in
	float textX;
	float textY;
	
	// defaults to left-aligned
	public TextView(UIDepend gui, String text, Rect area, Color color) {
		
		this(gui, null, text, area, HAlignment.LEFT, VAlignment.BOTTOM, color);
	}
	
	public TextView(UIDepend gui, BitmapFont font, String text, Rect area, HAlignment hAlign, VAlignment vAlign, Color color) {
		this.gui = gui;
		this.hAlign = hAlign;
		this.vAlign = vAlign;
		this.font = font;
		
		if (font != null) {
			deafultFontUsed = false;
		}
		
		setText(text);
		setArea(area);
		setColor(color);
		
		//itest = new ImageView(gui, "UNCHECKED_IMAGE", area); // TODO debug
	}
	
	//ImageView itest; // TODO debug
	
	public void draw(SpriteBatch spriteBatch, float x, float y) {
		if (text.length() == 0) {
			return;
		}
		
		//itest.draw(spriteBatch, x, y); // TODO debug
		
		font.setScale(scale);
		font.setColor(color);
		font.draw(spriteBatch, text , textX+x, textY+y);
	}
	
	
	public void draw(SpriteBatch spriteBatch) {
		draw(spriteBatch, 0, 0);
	}
	
	public void setArea(Rect area) {
		this.area = new Rect(area);
		setScaleAndPosition();
	}
	
	public void setText(String text) {
		this.text = text;
		setScaleAndPosition();
	}
	
	
	public void setColor(Color color) {
		this.color = color;
	}
	
	// using the current text and area, calculate the scale and position of the text to be drawn.
	// Annoyingly, the text Y position indicates the top of the text character images, rather than the bottom like
	// a normal image.
	//
	// if the font is initially set to null, the default font will be used which will then be tested to find
	// the appropraite size that will fit in the area specified.
	protected void setScaleAndPosition()
	{
		// avoids premature calculation!
		if ((area == null) || (text == null) || (text.length() == 0)) {
			return;
		}
		
		BitmapFont.TextBounds bounds = null;
		
		// cycle through all the default font sizes, biggest first.
		if (deafultFontUsed) {
			for (BitmapFont testFont : gui.fonts) {
				font = testFont;
				font.setScale(scale);
				// get the width of the text, for setting the text X and text Y.  Try big font first.
				bounds = font.getBounds(text);
				if (bounds.width < area.width && bounds.height < area.height) {
					break; // This fits, so good enough.
				}
			}
		} 
		
		font.setScale(scale);
		bounds = font.getBounds(text);
		
		// Use Rect constructor to perform alignment calculations to work out x and y pos.
		Rect textArea = new Rect(area, hAlign, vAlign, bounds.width, bounds.height);
		textX = textArea.x;
		textY = textArea.y + bounds.height;
	}
}
