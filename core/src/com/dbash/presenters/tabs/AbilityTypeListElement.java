package com.dbash.presenters.tabs;

import java.util.Vector;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dbash.models.Ability.AbilityEffectType;
import com.dbash.models.AbilityInfo;
import com.dbash.platform.ImageView;
import com.dbash.platform.TextView;
import com.dbash.platform.UIDepend;
import com.dbash.presenters.widgets.IListElement;
import com.dbash.presenters.widgets.ISelectionListener;
import com.dbash.util.Rect;
import com.dbash.util.Rect.HAlignment;
import com.dbash.util.Rect.VAlignment;


public abstract class AbilityTypeListElement implements IListElement {

	ISelectionListener selectionListener;
	
	protected UIDepend gui;
	boolean drawFlag;
	
	// Ability area
	protected Rect area;
	
	// Ability text
	protected TextView	abilityName;
	protected AbilityInfo abilityInfo;
	
	// Type of item icon
	protected ImageView abilityType;
	
	// Clock timer icon
	protected ImageView clock;
	protected TextView	clockText;
	
	// Effect type(s)
	Vector<ImageView> abilityEffects;
	
	// magic cost
	protected ImageView magicIcon;
	protected TextView	magicCost;
	protected Rect iconArea;
	
	// spacing and position
	final static float iconSpacer = 1.05f;
	final static float leftSide = 0.05f;
	final static float fromBottom = 0.05f;
	final static float effectSpacer = 1.05f;
	final static float textBottom = 0.25f;
	final static float rightSide = 1.9f;
	final static float textSpacer = .9f;
	final static float textHeightFat = 1.1f;
	
	final static float textFromTop = .1f;
	final static float textFromBottom = .65f;
	
	public AbilityTypeListElement(UIDepend gui, AbilityInfo abilityInfo, Rect area) {
		this.area = new Rect(area);
		this.abilityInfo = abilityInfo;
		this.gui = gui;
		
		// name
		HAlignment hAlign = HAlignment.LEFT;
		VAlignment vAlign = VAlignment.BOTTOM;
		Color color = Color.BLACK;
		float add = (gui.sizeCalculator.MAX_ELEMENTS - gui.sizeCalculator.ELEMENTS_PER_SCREEN) * 0.02f;
		Rect textArea = new Rect(area, .05f, 0f, textFromTop, textFromBottom+add);  // 5% from left, 10% from top, 65% from bottom
		
		if (abilityInfo.isStat) {
			hAlign = HAlignment.LEFT;
			textArea.y -= textArea.height*1.2f;
			color = Color.WHITE;
		}

		//String rightPaddedString = new IListElement.PaddedString(abilityInfo.name).string;
		abilityName = new TextView(gui, null, abilityInfo.name, textArea, hAlign, vAlign, color);
		
		// work out icon size
		iconArea = new Rect(0, 0, .55f*area.height, .55f*area.height);

		// type
		setImageType(iconArea);
		
		// clock
		if (abilityInfo.expireTime > 0) {
			iconArea.x = area.width - iconArea.width * rightSide;
			iconArea.y = fromBottom * area.height;
			clock =  new ImageView(gui, "CLOCK_IMAGE", iconArea);
			
			textArea.x = iconArea.x + iconArea.width * textSpacer;
			textArea.y = iconArea.y + iconArea.height * textBottom;
			textArea.width = area.x+area.width - textArea.x - area.width * leftSide;
			textArea.height *= textHeightFat;
			clockText = new TextView(gui, null, ""+abilityInfo.expireTime, textArea, HAlignment.LEFT, VAlignment.BOTTOM, Color.BLACK);
		} else {
			clock = null;
			clockText = null;
		}
		
		// magic cost
		if (abilityInfo.magicCost > 0) {
			iconArea.x = area.width - iconArea.width * rightSide;
			iconArea.y = fromBottom * area.height;
			magicIcon =  new ImageView(gui, "MAGIC_STAR_IMAGE", iconArea);
			
			textArea.x = iconArea.x + iconArea.width * textSpacer;
			textArea.y = iconArea.y + iconArea.height * textBottom;
			textArea.width = area.x+area.width - textArea.x - area.width * leftSide;
			textArea.height *= textHeightFat;
			magicCost = new TextView(gui, null,""+abilityInfo.magicCost, textArea, HAlignment.LEFT, VAlignment.BOTTOM, Color.BLACK);
		} else {
			magicIcon = null;
			magicCost = null;
		}
	}
	
	
	
	private void setImageType(Rect iconArea)
	{
		String image = abilityInfo.getAbilityTypeImageName();
		
		iconArea.x = leftSide * area.width;
		iconArea.y = fromBottom * area.height;
		abilityType = new ImageView(gui, image, iconArea);
	}
	
	protected void setEffects(float posX)
	{
		abilityEffects = new Vector<ImageView>();
		String image;
		
		for (AbilityEffectType effect : abilityInfo.abilityEffects) {
			
			image = AbilityInfo.getImageForEffectType(effect);
			
			if (image != null) {
				iconArea.y = fromBottom * area.height;
				iconArea.x = posX;
				abilityEffects.add(new ImageView(gui, image, iconArea));
				posX += iconArea.width * effectSpacer;
			}
		}
	}
	
	@Override
	public void draw(SpriteBatch spriteBatch, float x, float y)
	{
		// Draw text
		abilityName.draw(spriteBatch, x, y);
		
		// draw type (optional - draw in subclass)
		//abilityType.draw(spriteBatch, x, y);
		
		// clock and time
		if (clock != null) {
			clock.draw(spriteBatch, x, y);
			clockText.draw(spriteBatch, x, y);
		}
	}
	
	// These selection coordinates are relative to the top-left of the list element area.
	@Override
	public void gotSelection(float x, float y) {
		if (selectionListener != null) {
			selectionListener.processSelection();
		}
	}
	
	@Override
	public void onSelection(ISelectionListener selectionListener) {
		this.selectionListener = selectionListener;
	}

}
