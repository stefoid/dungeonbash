package com.dbash.presenters.widgets;

import com.dbash.models.TouchEventProvider;
import com.dbash.platform.UIDepend;
import com.dbash.util.Rect;

public class CheckBoxView extends ButtonView {

	public CheckBoxView(UIDepend gui, TouchEventProvider touchEventProvider, Rect area, boolean checked) {
		
		super(gui, touchEventProvider, area, "CHECKED_IMAGE", "UNCHECKED_IMAGE", "UNCHECKED_IMAGE");
		setState(checked);
	}
	
	// check boxes dont untoggle when the user lifts their finger.
	@Override
	public void unToggleState() {
	}

}
