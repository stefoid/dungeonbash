package com.dbash.presenters.root;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dbash.models.IPopupController;
import com.dbash.models.TouchEvent;
import com.dbash.models.TouchEventListener;
import com.dbash.models.TouchEventProvider;
import com.dbash.platform.ImageView;
import com.dbash.platform.TextBoxView;
import com.dbash.platform.UIDepend;
import com.dbash.presenters.widgets.ButtonView;
import com.dbash.presenters.widgets.CheckBoxView;
import com.dbash.presenters.widgets.IClickListener;
import com.dbash.util.Rect;



// Takes a list of AbilityInfo derived from the owners abilities and 
// displays it as a ScrollingList of ListElements.
public class PopupPresenter implements TouchEventListener {
	
	protected UIDepend gui;
	static protected IPopupController popupController = null;

	protected ImageView backgroundImage;
	protected String popupId;
	protected TextBoxView textBox;
	protected ButtonView okButton;
	protected CheckBoxView checkBox;
	protected Rect area;
	
	public PopupPresenter(String popupId, String backgroundImage, String text) { 
		this.popupId = popupId;
		
		// Should this popup even be shown?
		if (popupController.shouldShowPopup(popupId)) {
			// grab dependencies from the controller so the popup initiator doesnt have to deal with them.
			Rect area = popupController.getPopupArea();
			TouchEventProvider touchEventProvider = popupController.getTouchEventProvider();
			this.gui = popupController.getGuiDependencies();
			
			// Needs to swallow all touches to the screen to be modal
			touchEventProvider.addTouchEventListener(this, null, gui.cameraViewPort.viewPort);  //null area means entire screen
			
			this.backgroundImage = new ImageView(gui, backgroundImage, area);
			
			// This is a 'show this popup next time' checkbox, which is always marked OK if the popup actually gets shown.
			Rect checkBoxRect = new Rect(area, 0.1f, 0.75f, 0.75f, 0.1f);
			checkBoxRect.height = checkBoxRect.width; // square it up
			checkBox = new CheckBoxView(gui, touchEventProvider, checkBoxRect, true);
			
			Rect okButtonRect = new Rect(area, 0.75f, 0.1f, 0.75f, 0.1f);
			okButtonRect.height = okButtonRect.width; // square it up
			okButton = new ButtonView(gui, touchEventProvider, okButtonRect, "CONFIRM_SELECTED_IMAGE", "CONFIRM_BUTTON_IMAGE", "CONFIRM_BUTTON_IMAGE");
			final IPopupController controller = popupController;
			final String id = popupId;
			final TouchEventProvider touchEP = touchEventProvider;
			final TouchEventListener me = this;
			final CheckBoxView box = checkBox;
			okButton.onClick( new IClickListener() {
				public void processClick() {
					touchEP.removeTouchEventListener(me);
					controller.popupDismissed(id, box.getState());
				}
			});
			
			//Rect  = new Rect(area, 0.05f, 0.05f, 0.03f, 0.35f);
			//textBox = new TextBoxView(gui, text, textBoxRect, Rect.HAlignment.CENTER, Rect.VAlignment.CENTER, Color.BLACK);
			
			popupController.popupCreated(this, popupId);  // tell the popup controller about me.		
		}
	}

	
	public void draw(SpriteBatch spriteBatch, float x, float y)
	{
		backgroundImage.draw(spriteBatch, x, y);
		textBox.draw(spriteBatch, x, y);
		okButton.draw(spriteBatch, x, y);
		checkBox.draw(spriteBatch, x, y);
	}

	static public void setPopupController(IPopupController controller)
	{
		popupController = controller;
	}
	
	static public void showAllPopups()
	{
		popupController.setAllPopups(true);
	}
	
	static public void hideAllPopups()
	{
		popupController.setAllPopups(false);
	}

	@Override
	public boolean touchEvent(TouchEvent event) {
		// TODO Auto-generated method stub
		return true;
	}

}