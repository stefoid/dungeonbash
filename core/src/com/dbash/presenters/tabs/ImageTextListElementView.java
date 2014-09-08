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
	
	protected static float BORDER = 0.05f;
	
	protected ImageView	backgroundImage;
	protected ImageView image;
	protected TextBoxView textBox;
	protected UIDepend gui;
	protected int extraElements;
	protected Rect elementArea;
	float dt = 0;
	boolean drawFlag;
	
	public ImageTextListElementView(UIDepend gui, ImageView image, ImageView backgroundImage, String text, int textSizeDivisor, HAlignment hAlign, Rect nominalArea) {
		this.gui = gui;
		this.elementArea = new Rect(nominalArea);
		this.backgroundImage = backgroundImage;
		
		Rect imageRect = new Rect(nominalArea, (1 - 2*BORDER));
		imageRect.y = 0;
		Rect textRect = new Rect(imageRect);
		float totalHeight = 0;
		extraElements = 0;
		this.image = image;
		
		if (text != null) {
			textRect.height /= textSizeDivisor;
			textBox = new TextBoxView(gui, null, text, textRect, hAlign, Color.BLACK);
			totalHeight += textBox.getTotalHeight();
		}
		
		if (image != null) {
			float aspectRatio = image.getOriginalHeight() / image.getOriginalWidth();
			imageRect.height = imageRect.width * aspectRatio;
			imageRect.y += totalHeight;
			totalHeight += imageRect.height;
		}
		
		Rect totalArea = new Rect(nominalArea);
		extraElements = (int)(totalHeight / nominalArea.height);
		
		float heightOfAllElements = nominalArea.height * (extraElements + 1);
		float centeringOffset = (heightOfAllElements - totalHeight)/2;
		totalArea.height = heightOfAllElements;
		textRect.y += centeringOffset;
		imageRect.y += centeringOffset;
		if (textBox != null) {
			textBox.setArea(textRect);
		}
		if (image != null) {
			image.setArea(imageRect);
		}
		backgroundImage.setArea(totalArea);
	}
	
	@Override
	public void draw(SpriteBatch spriteBatch, float x, float y) {
		backgroundImage.draw(spriteBatch, x, y);
		if (image != null) {
			image.draw(spriteBatch, x, y);
		}
		
		if (textBox != null) {
			textBox.draw(spriteBatch, x, y);
		}
	}
	
	@Override
	public void addToList(ArrayList<IListElement> list) {
		for (int i=0; i<extraElements; i++) {
			float parentYOffset = (i - extraElements)*elementArea.height;
			IListElement.EmptyListElement empty = new IListElement.EmptyListElement(gui, null, elementArea, this, parentYOffset);
			empty.addToList((list));
		}
		list.add(this);
	}

	@Override
	public void gotSelection(float x, float y) {
	}

	@Override
	public void onSelection(ISelectionListener selectionListener) {
	}
	
	@Override
	public void clearDrawFlag() {
		drawFlag = false;
	}
}
