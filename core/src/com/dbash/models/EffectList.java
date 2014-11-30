package com.dbash.models;
import java.util.ArrayList;

import com.badlogic.gdx.graphics.Color;


//each Character has a AbilitySelectionList that has the following responsibilities.
//
//.  as the list is specifically designed to be consumed by a Presenter, the info it contains is geared towards player consumption. 'AbilityInfo'.
//.  maintains a specifically ordered list of AbilityInfo meant for the player to interact with.
//.  encapsulates the logic to order the list.
//.  registers observers who want to know when the list content changes
//.  modifies the list according to events from the character such as an ability being acquired, dropped, used, equipped, etc�


// This list is of the effects currently affecting the character.
@SuppressWarnings("serial")
public class EffectList extends ArrayList<AbilityInfo>{
	
	public Creature owner;
	
	public EffectList(Creature owner) {
		
		super();
		
		this.owner = owner;
		
		for (Ability ability : owner.abilities) {
			// Effects are anything that that currently has an effect on the character, such as a non-physical, non-selectable
			// ability, or a currently-equipped item that has intrinsic effects of its own, such as armor.
			AbilityInfo info = new AbilityInfo(ability, owner);

			// 'Effects' are non-physical, non-selectable abilities.
			if (!ability.isSelectable() && (!ability.isPhysical())) {
				
				// effects are currently not sorted into other order than what they come in.
				
				add(info);
				
			} else if (info.equipped) {
				if (info.abilityEffects.size() > 0) {
					add(info);
				}
			}
		}
		
		if (owner.getNameUnderscore().equals("nobody") == false) {

			// We add character stats
			int as = owner.calculateAttackSkill();
			String[] tas = {"ATTACK", " : "};
			AbilityInfo attack = new AbilityInfo(tas, as, Color.WHITE);
			add(attack);
			
			int ds = owner.calculateDefenceSkill();
			String[] tds = {"DEFENCE", " : "};
			AbilityInfo defence = new AbilityInfo(tds, ds, Color.WHITE);
			add(defence);
			
			int ss = owner.calculateSpeed();
			String[] tss = {"SPEED", " : "};
			AbilityInfo speed = new AbilityInfo(tss, ss, Color.WHITE);
			add(speed);
			
			int wd = owner.calculateDamage();
			String[] twd = {"MELEE DAMAGE", " : "};
			AbilityInfo dam = new AbilityInfo(twd, wd, Color.WHITE);
			add(dam);
			
			int st = owner.calculateStealth();
			String[] tst = {"STEALTH", " : "};
			AbilityInfo stealth = new AbilityInfo(tst, st, Color.WHITE);
			add(stealth);
			
			int de = owner.calculateDetect();
			String[] tde = {"DETECT", " : "};
			AbilityInfo detect = new AbilityInfo(tde, de, Color.WHITE);
			add(detect);
			
			
			int hpv = owner.calcProtection(AbilityCommand.RESIST_HARD);
			String[] thpv = {"HARD DEF", " : ", "%"};
			AbilityInfo hp = new AbilityInfo(thpv, hpv , new Color(1, .3f, 0.3f, 1));
			add(hp);
			
			int spv = owner.calcProtection(AbilityCommand.RESIST_SHARP);
			String[] tspv = {"SHARP DEF", " : ", "%"};
			AbilityInfo sp = new AbilityInfo(tspv, spv , new Color(0, 0, .7f, 1));
			add(sp);
			
			int epv = owner.calcProtection(AbilityCommand.RESIST_ENERGY);
			String[] tepv = {"ENERGY DEF", " : ", "%"};
			AbilityInfo ep = new AbilityInfo(tepv, epv , Color.YELLOW);
			add(ep);
			
			int cpv = owner.calcProtection(AbilityCommand.RESIST_CHEMICAL);
			String[] tcpv = {"CHEMICAL DEF", " : ", "%"};
			AbilityInfo cp = new AbilityInfo(tcpv, cpv , Color.GREEN);
			add(cp);
			
			if (owner.hasHead() == false) {
				add(new AbilityInfo("NO HEAD", Color.BLACK));
			}
			
			if (owner.hasHands() == false) {
				add(new AbilityInfo("NO HANDS", Color.BLACK));
			}
			
			if (owner.isHumanid() == false) {
				add(new AbilityInfo("NOT NORMAL BODY", Color.BLACK));
			}
		}
		
		// Now sort the list according to usageCount and that is the order presented to the player.
		//Collections.sort(this);
	}
	
	public int difference(AbilityInfo abilityInfo) {
		if (abilityInfo.isStat == false || abilityInfo.statText == null) {
			return 0;
		}
		
		for (AbilityInfo info : this) {
		
			if (info.isStat && abilityInfo.statText.equals(info.statText)) {
				return info.statValue - abilityInfo.statValue;
			}
		}
		return 0;
	}
}