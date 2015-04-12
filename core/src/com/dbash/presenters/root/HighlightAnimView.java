package com.dbash.presenters.root;

import com.dbash.platform.AnimationView;
import com.dbash.platform.UIDepend;
import com.dbash.util.Rect;

public class HighlightAnimView extends AnimationView {

	public static final String BUTTON_HIGHLIGHT = "RANGED_DOT";
	public static final float startOp = 0.8f;
	public static final float endOp = 0.2f;
	
	public HighlightAnimView(UIDepend gui, Rect startRect, Rect endRect) {
		super(gui, BUTTON_HIGHLIGHT, startRect, endRect, startOp, endOp, 0.5f, 0, null);
	}

}
