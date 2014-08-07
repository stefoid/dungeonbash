package com.dbash.presenters.tabs;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dbash.models.CreatureStats;
import com.dbash.platform.ImagePatchView;
import com.dbash.platform.ImageView;
import com.dbash.platform.TextView;
import com.dbash.platform.UIDepend;
import com.dbash.presenters.widgets.IListElement;
import com.dbash.presenters.widgets.ISelectionListener;
import com.dbash.util.Rect;
import com.dbash.util.Rect.HAlignment;
import com.dbash.util.Rect.VAlignment;

public class CreatureListElementView implements IListElement {

	ISelectionListener selectionListener;
	
	// Ability background
	protected Rect area;
	protected ImageView	elementBackground;
	
	// Ability text
	protected BitmapFont	font;
	protected TextView	creatureName;

	// portrait pic
	protected ImageView	portraitImage;
	
	// stats
	private TextView healthText;
	private TextView magicText;
	private ImageView healthIcon;
	private ImageView magicIcon;
	private ImageView xpIcon;
	private TextView experience;
	private ImagePatchView border;
	
	protected CreatureStats stats;
	
	public CreatureListElementView(UIDepend gui, CreatureStats stats, Rect area) {
		this.area = new Rect(area);
		this.stats = stats;
		String imageName = stats.name.replace(" ","_");
		
		elementBackground = new ImageView(gui, "PORTRAIT_IMAGE", this.area);
		this.border = new ImagePatchView(gui, "9patchportrait", this.area);
		
		Rect portraitArea = new Rect(this.area, .2f, .2f, .1f, .3f); 
		if (portraitArea.height < portraitArea.width) {
			float dif = portraitArea.width - portraitArea.height;
			portraitArea.width = portraitArea.height;
			portraitArea.x += dif/2;
		} else {
			float dif = portraitArea.height - portraitArea.width;
			portraitArea.height = portraitArea.width;
			portraitArea.y += dif/2;			
		}
		
		portraitImage = new ImageView(gui, imageName, portraitArea);
		
		Rect nameArea = new Rect(this.area, 0f, 0f, .01f, .91f);
		creatureName = new TextView(gui,null, stats.name, nameArea, HAlignment.CENTER, VAlignment.BOTTOM, Color.WHITE);

		// stats
		Rect heartArea = new Rect(area, 0.05f, 0.8f, 0.7f, 0.17f);
		heartArea.width = heartArea.height; // square it up
		Rect expArea = new Rect(heartArea);
		healthIcon = new ImageView(gui, "HEART_IMAGE", heartArea);
	
		//Rect textArea = new Rect (heartArea, 0f, 0f, BOTTOM, BOTTOM - .2f);
		Rect textArea = new Rect(heartArea, 0, 0, .10f, .10f);
		textArea.x += heartArea.width * 1f;
		textArea.width = area.width * .35f;
		healthText = new TextView(gui,gui.numericalFonts, new String(stats.health+"/"+stats.maxHealth), textArea, HAlignment.CENTER, VAlignment.CENTER, Color.RED);
		
		Rect xpArea = new Rect(heartArea);
		heartArea.x += area.width*.46f;
		magicIcon = new ImageView(gui, "MAGIC_STAR_IMAGE", heartArea);
		textArea.x += area.width*.45f;
		magicText = new TextView(gui,gui.numericalFonts, new String(stats.magic+"/"+stats.maxMagic), textArea, HAlignment.CENTER, VAlignment.CENTER, new Color(0f, 0f, 1f, 1));
	
		xpArea.y -= heartArea.height * 1.05f;
		xpIcon = new ImageView(gui, "XP_IMAGE", xpArea);
		
		textArea = new Rect(expArea, 0, -5f, .15f, .05f);
		textArea.x = xpArea.x + xpArea.width * 1.1f; 
		textArea.y -= textArea.height * 1.2f;
		textArea.width = area.width * .67f;
		experience = new TextView(gui,gui.numericalFonts, new String(""+stats.experience), textArea, HAlignment.LEFT, VAlignment.CENTER, new Color(0, .8f, 0, 1));
		
	}
	
	@Override
	public void draw(SpriteBatch spriteBatch, float x, float y) {
		
		// Draw background
		elementBackground.draw(spriteBatch, x, y);

		portraitImage.draw(spriteBatch, x, y);
		
		creatureName.draw(spriteBatch, x, y);
		healthIcon.draw(spriteBatch, x, y);
		healthText.draw(spriteBatch, x, y);
		
		if (stats.isCharacter) {
			magicIcon.draw(spriteBatch, x, y);
			magicText.draw(spriteBatch, x, y);
			xpIcon.draw(spriteBatch, x, y);
			experience.draw(spriteBatch, x, y);
		}
		
		border.draw(spriteBatch, x, y);
	}

	@Override
	public void gotSelection(float x, float y) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSelection(ISelectionListener selectionListener) {
		// TODO Auto-generated method stub
		
	}
}
