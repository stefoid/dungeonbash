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
		//this.area.y -= area.height * 2;
		//this.area.height *= 3;
		this.stats = stats;
		String imageName = stats.name.replace(" ","_");
		float BOTTOM = .5f;
		
		elementBackground = new ImageView(gui, "PORTRAIT_IMAGE", this.area);
		this.border = new ImagePatchView(gui, "9patchportrait", this.area);
		
		Rect portraitArea = new Rect(this.area, .2f, .2f, .1f, .3f); // TODO must maintain aspect ratio of portrait
		// adjust to square
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
		float iconSize = area.height * .36f;
		float iconY = area.y + area.height * BOTTOM;
		float iconX = area.x + area.width * .06f;
		Rect heartArea = new Rect(iconX, iconY, iconSize, iconSize);
		Rect expArea = new Rect(heartArea);
		healthIcon = new ImageView(gui, "HEART_IMAGE", heartArea);

		//Rect textArea = new Rect (heartArea, 0f, 0f, BOTTOM, BOTTOM - .2f);
		Rect textArea = new Rect(heartArea, 0, 0, .15f, .15f);
		textArea.x += heartArea.width * 1f;
		textArea.width *= 3f;
		healthText = new TextView(gui,null, new String(stats.health+"/"+stats.maxHealth), textArea, HAlignment.LEFT, VAlignment.CENTER, Color.RED);
		
		Rect xpArea = new Rect(heartArea);
		heartArea.x += area.width*.46f;
		magicIcon = new ImageView(gui, "MAGIC_STAR_IMAGE", heartArea);
		textArea.x += area.width*.45f;
		magicText = new TextView(gui,null, new String(stats.magic+"/"+stats.maxMagic), textArea, HAlignment.LEFT, VAlignment.CENTER, new Color(0f, 0f, 1f, 1));
	
		xpArea.y -= heartArea.height * 1.05f;
		xpIcon = new ImageView(gui, "XP_IMAGE", xpArea);
		
		textArea = new Rect(expArea, 0, -3f, .15f, .05f);
		textArea.x = xpArea.x + xpArea.width * 1.1f; 
		textArea.y -= textArea.height * 1.2f;
		experience = new TextView(gui,null, new String(""+stats.experience), textArea, HAlignment.LEFT, VAlignment.CENTER, new Color(0, .8f, 0, 1));
		
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
