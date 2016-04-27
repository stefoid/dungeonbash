package com.me.dbash;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Scanner;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
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
import com.dbash.presenters.root.GameStatePresenter;
import com.dbash.presenters.root.OverlayQueues;
import com.dbash.presenters.root.RootPresenter;
import com.dbash.util.EventBus;
import com.dbash.util.L;
import com.dbash.util.Rect;

@SuppressWarnings("unused")

public class Dbash implements ApplicationListener {
	public static final boolean LOG = false && L.DEBUG;
	
	public static String SAVE_FILE_VERISON = "A24I23";
	
	enum GameState {
		SPLASH,
		INIT,
		PLAYING
	};
	
	public static CachedSpriteManager theSpriteManager = null; 
	
	Dungeon dungeon;
	TurnProcessor turnProcessor;
	private SpriteBatch spriteBatch;
	CachedSpriteManager spriteManager;
	RootPresenter rootPresenter;
	Audio audio; 
	GameState gameState = GameState.SPLASH;
	Texture splash;
	Thread.UncaughtExceptionHandler previousHandler;
	
	public Dbash(int iosVersion, JsonValue json) {
		SizeCalculator.setIosVersion(iosVersion);
		L.json = json;
		
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
		
		int[] fontSizes = {82, 64, 48, 40, 32, 26, 21, 17};
		gui.defaultFonts = new ArrayList<SmoothBitmapFont>();
		gui.numericalFonts = new ArrayList<SmoothBitmapFont>();
		for (int fontSize : fontSizes) {
			gui.defaultFonts.add(new SmoothBitmapFont("toontime", fontSize));
			gui.numericalFonts.add(new SmoothBitmapFont("actionman", fontSize));
		}
		
		gui.spriteManager = spriteManager;
		theSpriteManager = spriteManager;
		gui.numberFont = new FatOutlineFont();
		gui.sizeCalculator = new SizeCalculator();
		gui.audio = new Audio();
		audio = gui.audio;
		
		makeObjects(gui);

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
				EventBus.getDefault().reset();
				makeObjects(gui);
			} 
		} 
		
		if (newGame) {
			audio.defaultVolume();
			EventBus.getDefault().event(GameStatePresenter.NO_SAVED_GAME_EVENT,  turnProcessor);
		} else {
			turnProcessor.resume();
		}
		quitted = false;
	}
	
	private void makeObjects(UIDepend gui) {
		dungeon = new Dungeon(true);
		turnProcessor = new TurnProcessor(dungeon, dungeon, dungeon, this);
		PresenterDepend model = new PresenterDepend();
		model.presenterDungeon = dungeon;
		model.presenterTurnState = turnProcessor;
		rootPresenter = new RootPresenter(gui, model);
		Gdx.input.setInputProcessor(rootPresenter);
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
		
		// only explicitly save tutorial mode games.
		if (turnProcessor.getTutorialMode()) {
			return;
		}
		
		saveGame();
	}
	
	public void saveGame() {
		if (L.NO_SAVE == false && turnProcessor.allCreatures != null) {
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
			boolean drawDungeon = true;
			if (turnProcessor.getGameState() == TurnProcessor.GameState.NO_SAVED_GAME) {
				drawDungeon = false;
			}
			rootPresenter.draw(spriteBatch, drawDungeon);
			break;
			
		case SPLASH:
			drawSplash();
			gameState = GameState.INIT;
			break;
		case INIT:
			readConfig();
			initEverything();
			gameState = GameState.PLAYING;
			break;
		}
	}
	
	protected void readConfig() {
		String MONSTER = "monster";
		if (Gdx.files.isExternalStorageAvailable() == false) {
			return;
		}
		
		FileHandle handle = Gdx.files.external("monster.txt");
		File fl = handle.file();
		String jsonString = null;
		JsonValue json = null;
	
		if (fl.exists() == true) {
			try {
				Scanner scanner = new Scanner(fl).useDelimiter("\\Z");
				jsonString = scanner.next();
				json = new JsonReader().parse(jsonString);
				if (json.has(MONSTER)) {
					L.c1 = json.getString(MONSTER);
				}
			} catch (Exception e) {
				System.out.printf("Exception %s", e.toString());
			}
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



