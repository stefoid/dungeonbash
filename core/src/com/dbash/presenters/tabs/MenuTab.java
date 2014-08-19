package com.dbash.presenters.tabs;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.dbash.models.PresenterDepend;
import com.dbash.models.TouchEventProvider;
import com.dbash.models.UIInfoListener;
import com.dbash.platform.Audio;
import com.dbash.platform.ImagePatchView;
import com.dbash.platform.ImageView;
import com.dbash.platform.UIDepend;
import com.dbash.presenters.widgets.ButtonView;
import com.dbash.presenters.widgets.IClickListener;
import com.dbash.presenters.widgets.ISelectionListener;
import com.dbash.presenters.widgets.SliderView;
import com.dbash.presenters.widgets.TabPresenter;
import com.dbash.util.Rect;

	public class MenuTab extends TabPresenter {

		private ImageView menuTabImage;
		private ImageView menuBackground;
		private ButtonView startButton;
		private ButtonView quitButton;
		final private SliderView fxSlider;
		final private SliderView musicSlider;
//		private ButtonView tutorialOnButton;
//		private ButtonView tutorialOffButton;
		private PresenterDepend model;
		private Rect bodyArea;
		UIDepend gui;
		TouchEventProvider touchEventProvider;
		protected ImagePatchView border;
		
		public MenuTab(final PresenterDepend model, UIDepend gui, TouchEventProvider touchEventProvider, Rect tabArea, Rect bodyArea) {
			super(model, gui, touchEventProvider, tabArea, bodyArea);
			this.model = model;
			this.touchEventProvider = touchEventProvider;
			final Audio audio = gui.audio;
			
			menuTabImage = new ImageView(gui, "MENU_TAB_IMAGE", tabArea);
			backImageCurrent = new ImageView(gui, "MENU_TAB_ON_IMAGE", tabArea);
			backImageNotCurrent = new ImageView(gui, "MENU_TAB_OFF_IMAGE", tabArea);
			
			border = new ImagePatchView(gui, "9patchlistsides", bodyArea); 
			
			Rect sliderRect1 = new Rect(bodyArea, .65f, .2f, .05f, .4f);
			fxSlider = new SliderView(gui, touchEventProvider, sliderRect1, 2f, "VOLUME_SLIDER_IMAGE", "FX_SLIDER_BUTTON");
			gui.audio.fxVolumeListeners.add(new UIInfoListener() {
				public void UIInfoChanged() {
					fxSlider.setSliderPosition(audio.getFxVolume()*100f);
				}	
			});
			fxSlider.onSliderChange (new ISelectionListener() {
				public void processSelection() {
					int volume = (int) (fxSlider.getSliderPosition());
					audio.setFxVolume((float) (volume)/100f);
				}
			});
			
			Rect sliderRect2 = new Rect(bodyArea, .2f, .65f, .05f, .4f);
			musicSlider = new SliderView(gui, touchEventProvider, sliderRect2, 2f, "VOLUME_SLIDER_IMAGE", "MUSIC_SLIDER_BUTTON");
			gui.audio.musicVolumeListeners.add(new UIInfoListener() {
				public void UIInfoChanged() {
					musicSlider.setSliderPosition(audio.getMusicVolume()*100f);
				}	
			});
			musicSlider.onSliderChange (new ISelectionListener() {
				public void processSelection() {
					int volume = (int) (musicSlider.getSliderPosition());
					audio.setMusicVolume((float) (volume)/100f);
				}
			});
			
//			Rect tuteModeRect = new Rect(bodyArea, .1f, .55f, .05f, .75f);
//			tutorialOnButton = new ButtonView(gui, touchEventProvider, tuteModeRect, "TUTE_ON_SELECTED_IMAGE", 
//					"TUTE_ON_BUTTON_IMAGE", "TUTE_ON_BUTTON_IMAGE");
//			tutorialOnButton.onClick( new IClickListener() {
//				public void processClick() {
//					PopupPresenter.showAllPopups();
//				}
//			});
//			
//			tuteModeRect = new Rect(bodyArea, .55f, .1f, .05f, .75f);
//			tutorialOffButton = new ButtonView(gui, touchEventProvider, tuteModeRect, "TUTE_OFF_SELECTED_IMAGE", 
//					"TUTE_OFF_BUTTON_IMAGE", "TUTE_OFF_BUTTON_IMAGE");
//			tutorialOffButton.onClick( new IClickListener() {
//				public void processClick() {
//					PopupPresenter.hideAllPopups();
//				}
//			});
			
			Rect quitRect = new Rect(bodyArea, .6f, .15f, .75f, .05f);
			quitButton = new ButtonView(gui, touchEventProvider, quitRect, "INFO_SELECTED_IMAGE", 
					"INFO_BUTTON_IMAGE", "INFO_BUTTON_IMAGE");
			quitButton.onClick( new IClickListener() {
				public void processClick() {
					model.presenterTurnState.infoSelected();
				}
			});
			
			this.bodyArea = new Rect(bodyArea);
			this.gui = gui;
			menuBackground = new ImageView(gui, "MENU_BGROUND_IMAGE", bodyArea);
			
			doStartButton();
			model.presenterTurnState.onChangeToGameInProgress(new UIInfoListener() {
				public void UIInfoChanged() {
					doStartButton();
				}
			});
		}

		private void doStartButton() 
		{
			Rect startRect = new Rect(bodyArea, .15f, .6f, .75f, .05f);
			
			if (model.presenterTurnState.gameInProgress()) {
				startButton = new ButtonView(gui, touchEventProvider, startRect, "RESTART_SELECTED_IMAGE", 
						"RESTART_BUTTON_IMAGE", "RESTART_BUTTON_IMAGE");
			} else {
				startButton = new ButtonView(gui, touchEventProvider, startRect, "START_SELECTED_IMAGE", 
						"START_BUTTON_IMAGE", "START_BUTTON_IMAGE");
			}
			
			startButton.onClick( new IClickListener() {
				public void processClick() {
					model.presenterTurnState.startGameSelected();
				}
			});
		}

		@Override
		public void drawTab(SpriteBatch spriteBatch, float x, float y) {
			// super will draw tab background (different for current/non current)
			super.drawTab(spriteBatch, x, y);
			menuTabImage.draw(spriteBatch);
			
			if (shouldDrawBody) {  // should not be doing View stuff here, but fuck it.
				// Clip the top and bottom of the entire list so the top or bottom elements 
				spriteBatch.flush(); // cause stuff drawn so far to be draw so it wont be clipped.
				Rectangle scissors = new Rectangle();
				Rectangle clipBounds = new Rectangle(bodyArea.x, bodyArea.y, bodyArea.width, bodyArea.height);
				Rectangle vp = ScissorStack.getViewport();
				ScissorStack.calculateScissors(gui.cameraViewPort, vp.x, vp.y, vp.width, vp.height, spriteBatch.getTransformMatrix(), clipBounds, scissors);
				ScissorStack.pushScissors(scissors);
				
				menuBackground.draw(spriteBatch, x, y);
				startButton.draw(spriteBatch, x, y);
				quitButton.draw(spriteBatch, x, y);
//				tutorialOnButton.draw(spriteBatch, x, y);
//				tutorialOffButton.draw(spriteBatch, x, y);
				fxSlider.draw(spriteBatch, x, y);
				musicSlider.draw(spriteBatch, x, y);
				
				// end clipping
				spriteBatch.flush();   // cause the list to be drawn within the clipping window.
				ScissorStack.popScissors();  // remove the clipping window for further drawing.
			}
			border.draw(spriteBatch);
		}
		
		@Override
		public void setCurrent() {
			super.setCurrent();
			quitButton.setEnabled(true);
			startButton.setEnabled(true);
			fxSlider.activate();
			musicSlider.activate();
		}
		
		@Override
		public void unsetCurrent() {
			super.unsetCurrent();
			fxSlider.deactivate();
			musicSlider.deactivate();
			quitButton.setEnabled(false);
			startButton.setEnabled(false);
		}
	}