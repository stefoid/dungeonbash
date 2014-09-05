package com.dbash.presenters.tabs;

import java.util.ArrayList;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dbash.platform.ImageView;
import com.dbash.platform.TextBoxView;
import com.dbash.platform.UIDepend;
import com.dbash.presenters.widgets.IListElement;
import com.dbash.presenters.widgets.ISelectionListener;
import com.dbash.util.Rect;
import com.dbash.util.Rect.HAlignment;


public class ImageTextListElementView implements IListElement {
	
	// Ability background
	protected ImageView	elementBackground;
	protected ImageView image;
	protected TextBoxView textBox;
	UIDepend gui;
	
	public ImageTextListElementView(UIDepend gui, ImageView image, String text, Rect nominalArea) {
		this.gui = gui;
		Rect imageRect = new Rect(nominalArea, .9f);
		Rect textRect = new Rect(imageRect);
		float totalHeight = 0;
		
		if (image != null) {
			float aspectRatio = image.getOriginalHeight() / image.getOriginalWidth();
			imageRect.height = imageRect.width * aspectRatio;
			image.setArea(imageRect);
			totalHeight += imageRect.height;
		}
		
		if (text != null) {
			textRect.height /= 4;
			textBox = new TextBoxView(gui, text, textRect, HAlignment.LEFT, Color.BLACK);
			totalHeight += textBox.getTotalHeight();
		}
		
		// TODO armed with the total height I can add the required elements and create the required background size.
	}
	
	@Override
	public void draw(SpriteBatch spriteBatch, float x, float y) {
		elementBackground.draw(spriteBatch, x, y);
		image.draw(spriteBatch, x, y);
		textBox.draw(spriteBatch, x, y);
	}
	
	@Override
	public void addToList(ArrayList<IListElement> list) {
		list.add(this);
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
