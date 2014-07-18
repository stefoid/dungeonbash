package com.dbash.models;

import java.util.Vector;

import com.dbash.models.Ability.AbilityEffectType;
import com.dbash.models.Ability.AbilityType;


// Presentation layer
public class AbilityInfo implements Comparable<AbilityInfo>{
	
	public Ability ability;
	
	// Displayable ability attributes.
	public String 		name;
	public AbilityType 	abilityType = AbilityType.DE_NADA;
	public boolean 		equipped = false;
	public int			magicCost = 0;
	public boolean 		targetable;
	public boolean		enoughMagic = false;	
	public int			sortValue = 0;
	public boolean 		currentySelected = false;
	public int 			creates = -1;  // Does this ability crete another?
	public boolean oneShot;
	public boolean isStat = false;
	public int	statValue = 0;
	
	public Vector<AbilityEffectType> abilityEffects;
	public int expireTime;
	public boolean isUsableByOwner;
	public boolean isCarried;
	
	
	// Sets the attributes accordingly.  Items may be unowned, if lying around the dungeon, so set owner to null in that case.
	public AbilityInfo(Ability ability, Creature owner)
	{
		Data details = ability.ability;
		
		this.ability = ability;  // A pointer to the actual ability this presentation data belongs to.
		
		name = new String(details.name);
		abilityType = ability.getAbilityType();
		equipped = ability.set;
		magicCost = details.magicCost;
		abilityEffects = ability.getEffectType();
		oneShot = ability.isInstant();
		creates = ability.findAbilityAddedWhenEquipped();
		
		if (owner != null) {
			enoughMagic = owner.hasEnoughMagic(ability);
			expireTime = ability.getAbilityDurationLeft();
			isUsableByOwner = owner.canUseAbility(ability);
			isCarried = true;
			
			targetable = ability.isTargetable();
			
			if (owner instanceof Character) {
				Character c = (Character) owner;
				// being disabled for not enough magic overrides selection status
				if ((c.currentSelectedAbility == ability) && (enoughMagic)) {  
					currentySelected = true;
				}
			}
		} else {
			isCarried = false;
		}
	}

	public AbilityInfo(String text) {
		name = text;
	}

	// constructor for stat listing
	public AbilityInfo(String text, int value) {
		name = text;
		statValue = value;
		isStat = true;
	}
	
	@Override
	public int compareTo(AbilityInfo o) {
		return (this.sortValue - o.sortValue);
	}
	
	public String getAbilityTypeImageName()
	{
		String image;
		
		switch (abilityType) {
			case WEAPON:
				if (equipped) {
					image = "WEAPON_EQUIPPED_IMAGE";
				} else {
					image = "WEAPON_IMAGE";
				}
				break;
			case ARMOR:
				if (equipped) {
					image = "ARMOR_EQUIPPED_IMAGE";
				} else {
					image = "ARMOR_IMAGE";
				}
				break;
			case AMULET:
				if (equipped) {
					image = "AMULET_EQUIPPED_IMAGE";
				} else {
					image = "AMULET_IMAGE";
				}
				break;
			case RANGED:
				image = "RANGED_IMAGE";
				break;
			case WAND:
				image = "WAND_IMAGE";
				break;
			case MAGIC_ITEM:
				image = "MAGIC_ITEM_IMAGE";
				break;
			case POTION:
				image = "POTION_IMAGE";
				break;
			case ORB:
				image = "ORB_IMAGE";
				break;
			case ABILITY:
			default:
				image = "ABILITY_IMAGE";
				break;
		}
		return image;
	}
	
	public static String getImageForEffectType(Ability.AbilityEffectType effectType)
	{
		String image;
		
		if (effectType == null) {
			return null;
		}
		
		switch (effectType) {
			case POISON:
				image = "POISON_IMAGE";
				break;
			case BLESSING:
				image = "BLESSING_IMAGE";
				break;
			case HEALING:
				image = "HEALING_IMAGE";
				break;
			case PROTECTION:
				image = "PROTECTION_IMAGE";
				break;
			case ATTACK:
				image = "ATTACK_IMAGE";
				break;
			case CURSE:
				image = "CURSE_IMAGE";
				break;
			case SPEED:
				image = "SPEED_IMAGE";
				break;
			case SLOW:
				image = "SLOW_IMAGE";
				break;
			case HOLD:
				image = "HELD_IMAGE";
				break;
			case STUNNED:
				image = "STUNNED_IMAGE";
				break;
			case RESIST_HELD:
				image = "RESIST_HOLD_IMAGE";
				break;
			case RESIST_POISON:
				image = "RESIST_POISON_IMAGE";
				break;
			case RESIST_HARD:
				image = "RESIST_HARD_IMAGE";
				break;
			case RESIST_SHARP:
				image = "RESIST_SHARP_IMAGE";
				break;
			case RESIST_CHEMICAL:
				image = "RESIST_CHEMICAL_IMAGE";
				break;
			case RESIST_ENERGY:
				image = "RESIST_ENERGY_IMAGE";
				break;
			case RESIST_ALL:
				image = "RESIST_ALL_IMAGE";
				break;
			case RESIST_STUN:
				image = "RESIST_STUN_IMAGE";
				break;
			default:
				image = "MAGIC_ITEM_IMAGE";
				break;
		}
		
		return image;
	}
}