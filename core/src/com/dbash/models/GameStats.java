package com.dbash.models;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;


public class GameStats {
	public int monstersKilled;
	public int xp;
	public int level;
	public int delay;
	
	public GameStats() {
		delay = 2;
	}
	
	public void monsterKilled() {
		monstersKilled++;
		logstats();
	}
	
	public void xpGiven(int xp) {
		this.xp += xp;
		logstats();
	}
	
	public void newLevel(int level) {
		this.level = level;
		logstats();
	}
	
	public GameStats(ObjectInputStream in) throws IOException, ClassNotFoundException
	{
		monstersKilled = in.readInt();
		xp = in.readInt();
		level = in.readInt();
		logstats();
	}
	
	public void persist(ObjectOutputStream out) throws IOException {
		out.writeInt(monstersKilled);
		out.writeInt(xp);
		out.writeInt(level);
	}
	
	public void logstats() {
		//if (LOG) Logger.log("monstersKilled "+monstersKilled+",xp "+xp+",level "+level);
	}
}
