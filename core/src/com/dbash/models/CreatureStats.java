package com.dbash.models;

import com.dbash.models.Ability.AbilityEffectType;


public class CreatureStats {

	public int health;
	public int maxHealth;
	public int magic;
	public int experience;
	public int maxMagic;
	public String name;
	public boolean isCharacter;
	
	// The ability effect is to highlight to the player the most important ability effect information
	// for the current character without them having to visit the effects tab
	public Ability.AbilityEffectType abilityEffectType = AbilityEffectType.NONE_REALLY;
	public int abilityCountdown = 0;
	
	// If these are 0, there are no such effects currently on the character, otherwise they are the number
	// of turns until those effects disapear.
	public int positiveEffectTurnsLeft;
	public int negativeEffectTurnsLeft;
	
}
