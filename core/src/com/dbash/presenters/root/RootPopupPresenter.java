package com.dbash.presenters.root;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dbash.models.IPopupController;
import com.dbash.models.PresenterDepend;
import com.dbash.models.TouchEventProvider;
import com.dbash.platform.UIDepend;
import com.dbash.util.Rect;


public class RootPopupPresenter implements IPopupController {

	private HashMap<String, PopupPresenter> popups;
	private HashMap<String, Integer> hidePopups;
	final String filename = "popups";
	final String hideAllString = "HIDE_ALL_POPUPS";
	
	protected UIDepend gui;
	protected PresenterDepend model;
	protected Rect area;
	TouchEventProvider touchEventProvider;
	protected Rect screenArea;
	
	
	public RootPopupPresenter(UIDepend gui, TouchEventProvider touchEventProvider, Rect screenArea) {
		this.gui = new UIDepend(gui);
		this.touchEventProvider = touchEventProvider;
		this.screenArea = screenArea;
		this.area = new Rect(screenArea, 0.2f, 0.2f, 0.2f, 0.2f);
		PopupPresenter.setPopupController(this);
		popups = new HashMap<String, PopupPresenter>();
		hidePopups = new HashMap<String, Integer>();
		
		if (Gdx.files.local(filename).exists() == false) {
			writeHidePopups();  // defaults to all popups will be displayed
		} else {
			readHidePopups();
		}
	}
	
	// uses ObjectOutputStream wrapped around a byte array
	private void writeHidePopups()
	{
		ByteArrayOutputStream dataBucket = new ByteArrayOutputStream();
		try {
			ObjectOutputStream serializer = new ObjectOutputStream(dataBucket);
			serializer.writeObject(hidePopups);  // serialize the hashmap to a byte array
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		FileHandle file = Gdx.files.local(filename);
		file.writeBytes(dataBucket.toByteArray(), false);  // write the byte array to teh file
	}
	
	private void readHidePopups()
	{
		FileHandle file = Gdx.files.local(filename);
		byte[] data = file.readBytes();
		
		ByteArrayInputStream dataBucket = new ByteArrayInputStream(data);
		ObjectInputStream deserializer = null;
		try {
			deserializer = new ObjectInputStream(dataBucket);
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			hidePopups = (HashMap<String, Integer>) deserializer.readObject();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	// save the state of this popup so the next time it is called, should we show it again?
	public void popupDismissed(String popupId, boolean showNextTime) {
		popups.remove(popupId);
		
		// if the user has marked this as 'dont show next time' then we add it to the dontShowPopups list.
		// which means it wont be showed next time.
		if (showNextTime == false) {
			hidePopups.put(popupId, 0);
			writeHidePopups();  // persist this change.
		}
	}

	@Override
	public void draw(SpriteBatch spriteBatch, float x, float y) {
		spriteBatch.begin();
		for (PopupPresenter popup : popups.values()) {
			popup.draw(spriteBatch, x, y);
		}
		spriteBatch.end();
	}


	@Override
	public void popupCreated(PopupPresenter popup, String popupId) {
		popups.put(popupId,  popup);
	}



	@Override
	public Rect getPopupArea() {
		return area;
	}



	@Override
	public TouchEventProvider getTouchEventProvider() {
		return touchEventProvider;
	}



	@Override
	public UIDepend getGuiDependencies() {
		return gui;
	}



	@Override
	public boolean popupShowing() {
		if (popups.size() > 0)
			return true;
		else
			return false;
	}

	@Override
	// Any popup that has its id in this list need not be shown again.
	// if the user toggles off showing all popups, the HIDE_ALL_POPUPS entry will suffice.
	// if the list is empty then all popups will be shown.
	public void setAllPopups(boolean showAllPopups) {
		hidePopups = new HashMap<String, Integer>();  // create an empty list 
		if (showAllPopups == false) {
			hidePopups.put(hideAllString, 1);
		}
		writeHidePopups();
	}

	@Override
	// if this id exists in the dontShowPopups list then return false
	// also same if the special key 'dont show any popups' exists.
	public boolean shouldShowPopup(String popupId) {
		if (hidePopups.containsKey(hideAllString) || hidePopups.containsKey(popupId)) {
			return false;
		} else {
			return true;
		}
	}

}
