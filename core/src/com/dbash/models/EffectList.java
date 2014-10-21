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
			AbilityInfo attack = new AbilityInfo("ATTACK SKILL : "+as, as, Color.WHITE);
			add(attack);
			
			int ds = owner.calculateDefenceSkill();
			AbilityInfo defence = new AbilityInfo("DEFENCE SKILL : "+ds, ds, Color.WHITE);
			add(defence);
			
			int ss = owner.calculateSpeed();
			AbilityInfo speed = new AbilityInfo("SPEED : "+ss, ss, Color.WHITE);
			add(speed);
			
			int wd = owner.calculateDamage();
			AbilityInfo dam = new AbilityInfo("MELEE DAMAGE : "+wd, wd, Color.WHITE);
			add(dam);
			
			int st = owner.calculateStealth();
			AbilityInfo stealth = new AbilityInfo("STEALTH : "+st, st, Color.WHITE);
			add(stealth);
			
			int de = owner.calculateDetect();
			AbilityInfo detect = new AbilityInfo("DETECT : "+de, de, Color.WHITE);
			add(detect);
			
			
			int hpv = owner.calcProtection(AbilityCommand.RESIST_HARD);
			AbilityInfo hp = new AbilityInfo("HARD DEF : "+hpv+"%", hpv , new Color(1, .3f, 0.3f, 1));
			add(hp);
			
			int spv = owner.calcProtection(AbilityCommand.RESIST_SHARP);
			AbilityInfo sp = new AbilityInfo("SHARP DEF : "+spv+"%", spv , new Color(0, 0, .7f, 1));
			add(sp);
			
			int epv = owner.calcProtection(AbilityCommand.RESIST_ENERGY);
			AbilityInfo ep = new AbilityInfo("ENERGY DEF : "+epv+"%", epv , Color.YELLOW);
			add(ep);
			
			int cpv = owner.calcProtection(AbilityCommand.RESIST_CHEMICAL);
			AbilityInfo cp = new AbilityInfo("CHEMICAL DEF : "+cpv+"%", cpv , Color.GREEN);
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
}