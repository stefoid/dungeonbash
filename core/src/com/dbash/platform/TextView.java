package com.dbash.platform;

import java.util.ArrayList;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dbash.util.L;
import com.dbash.util.Rect;
import com.dbash.util.Rect.HAlignment;
import com.dbash.util.Rect.VAlignment;

// Use a TextView or TextBoxView if you care more about displaying crisp text at the fonts 1:1 resolution.  scaled text looks pretty bad....
// TextView is different from TextImageView.  It tries not to scale the scale the text so that it retains a 1:1 scale and looks good.
// The bounding rectangle supplied is for alignment of the text AND scaling of the text.  i.e.  if the text is left-aligned. centered
// or right aligned -> with respect to the boundary supplied.
// It only does one line of text - if you want wrapping, use a TextBoxView which is based on TextView.
// 
// You supply an array of fonts, in order of biggest to smallest, and it will check which is the biggest font from the list that will 
// fit inside the bounding box, and use that to draw with.  setting the list to null means use the default list of fonts.
//
public class TextView {

	static final float scale = 1f;
	static float nominalTextHeight = 0;
	public Sprite sprite;
	public Rect area;
	CharSequence text;
	BitmapFont font;
	ArrayList<SmoothBitmapFont> fontList;
	Color color;
	HAlignment hAlign;
	VAlignment vAlign;
	UIDepend gui;
	boolean deafultFontUsed = true;
	
	// these coordinates calculated to draw the text within the specified rectangle, according to the alignment passed in
	float textX;
	float textY;
	ImageView itest; 

	
	// defaults to left-aligned
	public TextView(UIDepend gui, String text, Rect area, Color color) {
		this(gui, gui.defaultFonts, text, area, HAlignment.LEFT, VAlignment.BOTTOM, color);
	}
	
	public TextView(UIDepend gui, ArrayList<SmoothBitmapFont> fontList, String text, Rect area, HAlignment hAlign, VAlignment vAlign, Color color) {
		this.gui = gui;
		this.hAlign = hAlign;
		this.vAlign = vAlign;
		this.fontList = fontList;
		
		if (fontList == null) {
			this.fontList = gui.defaultFonts;
		}
		
		setText(text);
		setArea(area);
		setColor(color);
		
		if (L.SHOWTEXTBOXES) {
			itest = new ImageView(gui, "UNCHECKED_IMAGE", area); 
		}
	}
	
	public void draw(SpriteBatch spriteBatch, float x, float y) {
		if (text.length() == 0) {
			return;
		}
		
		if (L.SHOWTEXTBOXES) {
			itest.draw(spriteBatch, x, y); 
		}
		
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
	
	public BitmapFont getFont() {
		return font;
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
		for (BitmapFont testFont : fontList) {
			font = testFont;
			font.setScale(scale);
			// get the width of the text, for setting the text X and text Y.  Try big font first.
			bounds = font.getBounds(text);
			if (bounds.width < area.width && bounds.height < area.height) {
				break; // This fits, so good enough.
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
