package com.dbash.presenters.widgets;

import java.util.ArrayList;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dbash.platform.ImageView;
import com.dbash.platform.UIDepend;
import com.dbash.util.Rect;


// ScrollingLists are made up of a number of ListElements that get told to draw themselves.
//
// I was going to get fancy with generic lists and ListElements having multiple touchable hotspots and being able
// to take generic things and lay them out in the list element area, yada yada.
// But in the end, we only have to lists in the game, and only one of those lists has selectable items, with the whole
// area of the list element being one selection hotspot, so why bother?  This will just be an interface for two specific
// concrete implementations of ListElement.
public interface IListElement {
	
	public class EmptyListElement implements IListElement
	{
		protected UIDepend gui;
		protected Rect area;
		protected ImageView background;

		public EmptyListElement(UIDepend gui,String background, Rect area) {
			this.area = new Rect(area);
			this.gui = gui;
			if (background != null) {
				this.background = new ImageView(gui, background, area);
			} else {
				this.background = null;
			}
		}
		
		@Override
		public void draw(SpriteBatch spriteBatch, float x, float y) {
			if (background != null) {
				background.setPos(x, y);
				background.draw(spriteBatch);
			}
		}

		@Override
		public void gotSelection(float x, float y) {
			// do nothing
		}

		@Override
		public void onSelection(ISelectionListener selectionListener) {
			// do nothing
		}

		@Override
		public void addToList(ArrayList<IListElement> list) {
			list.add(this);
		}
	}
	
	// Just draw yourself to the screen at the coords given - the ScrollList will clip bits thatt shouldnt show.
	void draw(SpriteBatch spriteBatch, float x, float y);
	
	// These selection coordinates are relative to the top-left of the list element area.
	void gotSelection(float x, float y);

	// 
	void onSelection(ISelectionListener selectionListener);
	
	// Usually this is just a single add, but some list elements take up morethan one'slot' so have to add
	// empty list elements to a number of slots first.
	void addToList(ArrayList<IListElement> list);

}
