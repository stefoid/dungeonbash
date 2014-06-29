package com.dbash.platform;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dbash.util.Rect;

// This class looks like an ImageView to things that use image views, but the string is actually text that 
// is explicitly scaled to exactly fit the area passed in.
// yes I know, Im incredibly lazy not to make an interface out of ImageView, but what the fuck.
// A much preferable solution is to draw the bitmap font text directly to a texture, make it into an actual sprite
// that can be rotated and whatnot, but I havent worked out how to do that with libgdx yet.
//
// Anyway, unlike TextView, you would use TextImageView where you want precise control over the dimensions of the text and dont mind it being scaled
// in both dimensions to do that.  i.e. if you want animated text, or text that always takes up the same proportion of the screen on any device, 
// use a TextImageView.
public class TextImageView extends ImageView {

	protected String text;
	protected BitmapFont font;
	protected float scaleX;
	protected float scaleY;
	protected float boundsW;
	protected float boundsH;
	
	public TextImageView(UIDepend gui, BitmapFont font, String text, Rect area) {
		super();
		this.area = area;
		this.text = text;
		this.font = font;
		
		font.setScale(1f);  // set the scale to 1:1 with pixels in order to do the calculations
		boundsW = font.getBounds(text).width;
		boundsH = font.getBounds(text).height;
		setArea(area);
	}
	
	public void draw(SpriteBatch spriteBatch) {
		font.setScale(scaleX, scaleY);
		font.draw(spriteBatch, text, area.x, area.y);
	}
	
	public void draw(SpriteBatch spriteBatch, float alpha) {
		font.setScale(scaleX, scaleY);
		font.setColor(1f, 1f, 1f, alpha);
		font.draw(spriteBatch, text, area.x, area.y);
	}
	
	public void draw(SpriteBatch spriteBatch, float x, float y) {
		font.setScale(scaleX, scaleY);
		font.draw(spriteBatch, text, area.x+x, area.y+y);
	}

	public void setArea(Rect area) {
		this.area = new Rect(area);

		// work out the X and Y scale to use in order that the text fit exactly into the rectangle provided.
		scaleX = area.width/boundsW;
		scaleY = area.height/boundsH;
		setPos(area.x, area.y);
	}
	
	// fonts y pos is from the top left or something.
	public void setPos(float x, float y) {
		area.x = x;
		area.y = y+area.height;
	}
	
	
	// these dont make any sense for bitmap font drawn stuff.
	public void setRotation(float rotation) {

	}

	public void drawTinted(SpriteBatch spriteBatch) {

	}
	
	public void drawTinted(SpriteBatch spriteBatch, float alpha) {

	}
}
