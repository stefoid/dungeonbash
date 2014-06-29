package com.dbash.platform;

import java.io.File;

import com.badlogic.gdx.Gdx;


public class TextResourceIdentifier extends ResourceIdentifier<String> {

	public TextResourceIdentifier(String filename) {
		super(filename);
	}

	public String getFileContents() {
		if (Gdx.files == null){
//			try {
//				return FileUtils.readFileToString(new File("/home/ph/ws-games/dbash-android/assets/res/" + filename));
//			} catch (IOException e) {
				return null;
//			}
		}
		else {
			return Gdx.files.internal(RESOURCE_DIRECTORY + File.separator + filename).readString();
		}
	}
	
}