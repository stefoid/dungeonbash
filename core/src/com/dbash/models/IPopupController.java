package com.dbash.models;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dbash.platform.UIDepend;
import com.dbash.presenters.root.PopupPresenter;
import com.dbash.util.Rect;

public interface IPopupController {

	public boolean shouldShowPopup(String popupId);
	
	public void popupCreated(PopupPresenter popup, String popupId);
	
	public void popupDismissed(String popupId, boolean showNextTime);
	
	// draw popups at the given offset form their location.
	public void draw(SpriteBatch spriteBatch, float x, float y);
	public Rect getPopupArea();
	public TouchEventProvider getTouchEventProvider();
	public UIDepend getGuiDependencies();
	
	public boolean popupShowing();
	
	public void setAllPopups(boolean showPopups);
}
