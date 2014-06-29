package com.dbash.platform;

public abstract class ResourceIdentifier<T> {
	
	protected static final String RESOURCE_DIRECTORY = "res";
	
	protected String filename;

	public ResourceIdentifier(String filename) {
		this.setFilename(filename);
	}

	public abstract T getFileContents();

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

}