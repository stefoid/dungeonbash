package com.me.dbash;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
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
import com.dbash.presenters.root.OverlayQueues;
import com.dbash.presenters.root.RootPresenter;
import com.dbash.util.EventBus;
import com.dbash.util.L;
import com.dbash.util.Rect;

@SuppressWarnings("unused")

public class Dbash implements ApplicationListener {
	public static final boolean LOG = false && L.DEBUG;
	
	public static String SAVE_FILE_VERISON = "V13";
	
	enum GameState {
		SPLASH,
		INIT,
		PLAYING
	};
	
	Dungeon dungeon;
	TurnProcessor turnProcessor;
	private SpriteBatch spriteBatch;
	CachedSpriteManager spriteManager;
	RootPresenter rootPresenter;
	Audio audio; 
	GameState gameState = GameState.SPLASH;
	Texture splash;
	Thread.UncaughtExceptionHandler previousHandler;
	
	public Dbash(int iosVersion) {
		SizeCalculator.setIosVersion(iosVersion);
//		previousHandler = Thread.getDefaultUncaughtExceptionHandler();
//        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
//            @Override
//            public void uncaughtException(Thread thread, Throwable throwable) {
//                pause();
//                if (previousHandler != null) {
//                	previousHandler.uncaughtException(thread,  throwable);
//                }
//            }
//        });
	}

	
	protected void initEverything() {
		boolean newGame = true;
		//Gdx.app.setLogLevel(Application.LOG_INFO);
		// Make the dependency buckets to pass to the rootPresenter
		spriteManager = new CachedSpriteManager();
		UIDepend gui = new UIDepend();
		
		gui.assetManager = new AssetManager();
		dungeon = new Dungeon(true);
		turnProcessor = new TurnProcessor(dungeon, dungeon, dungeon, this);
		
		int[] fontSizes = {82, 64, 48, 40, 32, 26, 21, 17};
		gui.defaultFonts = new ArrayList<SmoothBitmapFont>();
		gui.numericalFonts = new ArrayList<SmoothBitmapFont>();
		for (int fontSize : fontSizes) {
			gui.defaultFonts.add(new SmoothBitmapFont("toontime", fontSize));
			gui.numericalFonts.add(new SmoothBitmapFont("actionman", fontSize));
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
		if (!L.NO_LOAD && fl.exists() == true) {
			if (LOG) L.log("LOADING GAMEDATA");
			newGame = true;
			ObjectInputStream in = null;
			try {
				in = new ObjectInputStream(fl.read());
				
				String version = in.readUTF();
				if (version.equals(SAVE_FILE_VERISON)) {
					// load the entire list of creatures, so we have them available for future reference via their
					// uniqueIds. (minus falling characters which dont appear on the map)
					AllCreatures allCreatures = new AllCreatures(in, dungeon, dungeon, turnProcessor);
					
					// Now tell the turn processor and dungeon to load, and any references to creatures
					// they have in their saved files can point into the list of allCreatures.
					dungeon.load(in, allCreatures);
					turnProcessor.load(in, dungeon, dungeon, dungeon, allCreatures);
					gui.audio.load(in);
					newGame = false;
				} 
			} catch (Exception e) {
				newGame = true;
			} 
		} 

		if (newGame) {
			audio.defaultVolume();
			turnProcessor.startNewGame();
		} else {
			turnProcessor.resume();
		}
		quitted = false;
	}
	
	@Override
	public void create() {
		spriteBatch = new SpriteBatch();
		splash = new Texture(Gdx.files.internal("splash.png"));
	}
	
	@Override
	public void pause() {
		if (quitted) {
			return;
		}
		
		if (L.NO_SAVE == false) {
			if (LOG) L.log("SAVING GAMEDATA");
			FileHandle fl = Gdx.files.local("gamedata.dat");
			ObjectOutputStream out = null;
			try {
				out = new ObjectOutputStream(fl.write(false));
				out.writeUTF(SAVE_FILE_VERISON);
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
	}
	
	@Override
	public void dispose() {
		spriteBatch.dispose();
		if (LOG) L.log("");
	}

	@Override
	public void render() {
		
		switch (gameState) {
		case PLAYING:
		default:
			Gdx.gl.glClearColor(0f,0f,0f,1f);
			Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
			// Give the TurnProcessor a tick - it processes one creature or character per call.
			turnProcessor.gameLogicLoop();

			// draw everything
			rootPresenter.draw(spriteBatch);
			break;
			
		case SPLASH:
			drawSplash();
			gameState = GameState.INIT;
			break;
		case INIT:
			initEverything();
			gameState = GameState.PLAYING;
			break;
		}
	}
	
	private void drawSplash() {
		Gdx.gl.glClearColor(0f,0f,0f,1f);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		
		float aspect = (float)splash.getWidth()/(float)splash.getHeight();
		
		Rect splashRect = SizeCalculator.getScreenSize();
		float splashW = splashRect.width;
		float splashH = splashW/aspect;
		float splashY = (float) ((Gdx.graphics.getHeight() - splashH) / 2);
		float splashX = 0;
		
		spriteBatch.begin();
		spriteBatch.draw(splash, splashX, splashY, splashW, splashH, 0, 0, splash.getWidth(), splash.getHeight(), false, false);
		spriteBatch.end();
	}

	@Override
	public void resize(int width, int height) {

	}

	@Override
	public void resume() {
		if (LOG) L.log("");
	}
	
	boolean quitted = true;
	public void quit() {
		pause();
		quitted = true;
		Gdx.app.exit();
	}
	
}



