package com.dbash.models;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Vector;

import com.dbash.models.Creature.CreatureType;

public class AllCreatures extends Vector<Creature> {

	/**
	 * I know, right .  should be a map god Im lazy
	 */
	public Creature getCreatureByUniqueId(int uniqueId) {
		for (Creature c : this) {
			if (c.uniqueId == uniqueId) {
				return c;
			}
		}
		return null;
	}
	
	public int getIndex(Creature current) {
		for (int i=0; i<size(); i++) {
			if (current == get(i)) {
				return i;
			}
		}
		return 0;
	}
	
	public AllCreatures() {
		super();
	}
	
	public void persist(ObjectOutputStream out) throws IOException {
		// persist all creatures on map and falling in
		out.writeInt(Creature.uniqueIdCounter);
		out.writeInt(size());
		for (Creature creature : this) {
			creature.persist(out);
		}
	}
	
	public AllCreatures(ObjectInputStream in, IDungeonEvents dungeonEvents, IDungeonQuery dungeonQuery, TurnProcessor turnProcessor) throws IOException, ClassNotFoundException {
		Creature.uniqueIdCounter = in.readInt();
		int total = (int) in.readInt();
		for (int i=0; i<total; i++) {
			Creature.CreatureType t = (CreatureType) in.readObject();
			if (t == Creature.CreatureType.CHARACTER) {
				add(new Character(in, dungeonEvents, dungeonQuery, turnProcessor));
			} else {
				add(new Monster(in, dungeonEvents, dungeonQuery, turnProcessor));
			}
		}
	}
}
