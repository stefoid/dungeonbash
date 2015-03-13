package com.dbash.models;

import com.dbash.presenters.widgets.AnimQueue;



// This is a rough first draft of a bucket that allows Presenter dependencies to be passed down the heirarchy
// without parent presenters knowing exactly which objects child presenters rely on.
public class PresenterDepend {

	public IPresenterDungeon presenterDungeon;
	public IPresenterTurnState presenterTurnState;
	public AnimQueue animQueue;
}
