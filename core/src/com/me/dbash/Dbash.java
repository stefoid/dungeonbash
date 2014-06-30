package com.me.dbash;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dbash.models.AllCreatures;
import com.dbash.models.Dungeon;
import com.dbash.models.PresenterDepend;
import com.dbash.models.TurnProcessor;
import com.dbash.platform.Audio;
import com.dbash.platform.CachedSpriteManager;
import com.dbash.platform.FatOutlineFont;
import com.dbash.platform.SizeCalculator;
import com.dbash.platform.SmoothBitmapFont;
import com.dbash.platform.UIDepend;
import com.dbash.presenters.root.RootPresenter;

public class Dbash implements ApplicationListener {
	
	Dungeon dungeon;
	TurnProcessor turnProcessor;
	private SpriteBatch spriteBatch;
	CachedSpriteManager spriteManager;
	RootPresenter rootPresenter;
	Audio audio;
	public Dbash() {
	}

	@Override
	public void create() {
		boolean newGame = true;
		Gdx.app.setLogLevel(Application.LOG_INFO);
		
		dungeon = new Dungeon(true);
		turnProcessor = new TurnProcessor(dungeon, dungeon, dungeon);
		
		// Make the dependency buckets to pass to the rootPresenter
		spriteManager = new CachedSpriteManager();
		spriteBatch = new SpriteBatch();
		UIDepend gui = new UIDepend();
		
		int[] fontSizes = {82, 64, 48, 40, 32, 26, 21, 17};
		gui.fonts = new ArrayList<SmoothBitmapFont>();
		for (int fontSize : fontSizes) {
			gui.fonts.add(new SmoothBitmapFont(fontSize));
		}
		
		gui.spriteManager = spriteManager;
		gui.numberFont = new FatOutlineFont();
		gui.sizeCalculator = new SizeCalculator();
		gui.audio = new Audio();
		audio = gui.audio;
		
		PresenterDepend model = new PresenterDepend();
		model.presenterDungeon = dungeon;
		model.presenterTurnState = turnProcessor;
		
		this.rootPresenter = new RootPresenter(gui, model);
		Gdx.input.setInputProcessor(rootPresenter);

		// load previously saved game, if it exists.
		FileHandle fl = Gdx.files.local("gamedata.dat");
		if (fl.exists() == true) {
			newGame = false;
			ObjectInputStream in = null;
			try {
				in = new ObjectInputStream(fl.read());
				
				// load the entire list of creatures, so we have them available for future reference via their
				// uniqueIds. (minus falling characters which dont appear on the map)
				AllCreatures allCreatures = new AllCreatures(in, dungeon, dungeon, turnProcessor);
				
				// Now tell the turn processor and dungeon to load, and any references to creatures
				// they have in their saved files can point into the list of allCreatures.
				dungeon.load(in, allCreatures);
				turnProcessor.load(in, dungeon, dungeon, dungeon, allCreatures);
				gui.audio.load(in);
				in.close();
				newGame = false;
			} catch (Exception e) {
				newGame = true;
			} 
		} 

		if (newGame) {
			turnProcessor.startNewGame();
		} else {
			turnProcessor.resume();
		}
	}
	
	@Override
	public void pause() {
		System.out.println("SAVING GAMEDATA");
		FileHandle fl = Gdx.files.local("gamedata.dat");
		ObjectOutputStream out = null;
		try {
			out = new ObjectOutputStream(fl.write(false));
			turnProcessor.allCreatures.persist(out);
			dungeon.persist(out);
			turnProcessor.persist(out);
			audio.persist(out);
		} catch (IOException e) {
		} finally {
			try {
				out.close();
			} catch (IOException e) {
			}
		}
	}
	
	@Override
	public void dispose() {
		spriteBatch.dispose();
		
	}

	@Override
	public void render() {
		
		Gdx.gl.glClearColor(0f,0f,0f,1f);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		
		// Give the TurnProcessor a tick - it processes one creature or character per call.
		turnProcessor.gameLogicLoop();

		// draw everything
		rootPresenter.draw(spriteBatch);
	}

	@Override
	public void resize(int width, int height) {

	}

	@Override
	public void resume() {
	
	}
}
