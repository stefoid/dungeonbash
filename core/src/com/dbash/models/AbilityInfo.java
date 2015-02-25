package com.dbash.models;

import java.util.Vector;

import com.badlogic.gdx.graphics.Color;
import com.dbash.models.Ability.AbilityEffectType;
import com.dbash.models.Ability.AbilityType;
import com.dbash.util.L;


// Presentation layer
public class AbilityInfo implements Comparable<AbilityInfo> {
	public static final boolean LOG = false && L.DEBUG;
	
	public Ability ability;
	
	// Displayable ability attributes.
	public String 		name;
	public String 		tag;
	public AbilityType 	abilityType = AbilityType.DE_NADA;
	public boolean 		equipped = false;
	public int			magicCost = 0;
	public boolean 		targetable;
	public boolean 		burstEffect;
	public boolean 		aimed;
	public boolean 		isRoughTerrain;
	public boolean		enoughMagic = false;	
	public int			sortValue = 0;
	public boolean 		currentySelected = false;
	public int 			creates = -1;  // Does this ability crete another?
	public boolean 		oneShot;
	public boolean 		isStat = false;
	public String 		statText;
	public int			statValue = 0;
	public Color color;
	public boolean isDefend;
	public boolean isCover;
	public boolean restrictFromHighlight;
	public boolean isCooldown = false;
	public boolean isCool = true;
	public int cooldownTurnsLeft = 0;
	public int cooldownPeriod = 0;
	public boolean isSetable;
	public boolean isSet;
	
	public Vector<AbilityEffectType> abilityEffects;
	public int expireTime;
	public boolean isUsableByOwner;
	public boolean canBeCarried;
	public boolean isCarried;
	public int damageType;
	public int meleeDamage;
	public int missileDamage;
	
	
	// Sets the attributes accordingly.  Items may be unowned, if lying around the dungeon, so set owner to null in that case.
	public AbilityInfo(Ability ability, Creature owner)
	{
		Data details = ability.ability;
		
		this.ability = ability;  // A pointer to the actual ability this presentation data belongs to.
		
		name = new String(details.name);

		if (ability.isRoughTerrain()) {
			isRoughTerrain  = true;
		}
		
		if (ability.hasTag(Ability.DEFEND_TAG)) {
			isDefend = true;
		}
		
		if (ability.hasTag(Ability.COVER_TAG)) {
			isCover = true;
		}
		
		if (isDefend || isCover) {
			restrictFromHighlight = true;
		}
		
		if (ability.hasTag(Ability.SETABLE_TAG)) {
			isSetable = true;
		}
		
		isSet = ability.set;
		
		abilityType = ability.getAbilityType();
		equipped = ability.set;
		magicCost = details.magicCost;
		abilityEffects = ability.getEffectType();
		oneShot = ability.isInstant();
		creates = ability.findAbilityAddedWhenEquipped();
		burstEffect = ability.isBurstEffect();
		
		isCooldown = ability.isCooldownAbility();
		isCool = ability.isCool();
		cooldownTurnsLeft = ability.getTurnsUntilCooldown();
		cooldownPeriod = ability.getCooldownPeriod();
		
		if (owner != null) {
			canBeCarried = true;
			enoughMagic = owner.hasEnoughMagic(ability);
			expireTime = ability.getAbilityDurationLeft();
			isUsableByOwner = owner.canUseAbility(ability);
			
			isCarried = true;
			targetable = ability.isTargetable();
			aimed = ability.isAimed();
			
			if (abilityType == AbilityType.WEAPON) {
				meleeDamage = owner.calculateMeleeDamageForAbility(ability);
				damageType = ability.getAbilityDamageType();
			}
			
			if (abilityType == AbilityType.RANGED || abilityType == AbilityType.WAND) {
				missileDamage = owner.calculateMissileDamage(ability);
				damageType = ability.getAbilityDamageType();
			}
			
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
		
		if (LOG) L.log("name: %s, aimed: %s, burst: %s",  name, aimed, burstEffect);
	}

	public AbilityInfo(String text) {
		name = text;
		isStat = true;
	}
	
	public AbilityInfo(String text, boolean isStat) {
		name = text;
		this.isStat = isStat;
	}

	public AbilityInfo(String text, Color color) {
		name = text;
		isStat = true;
		this.color = color;
	}
	
	// constructor for stat listing
	public AbilityInfo(String[] textArray, int value, Color color) {
		
		name = textArray[0] + textArray[1] + value;
		
		if (textArray.length > 2) {
			for (int i=2; i<textArray.length; i++) {
				name += textArray[2];
			}
		}
		
		statText = textArray[0];
		statValue = value;
		isStat = true;
		this.color = color;
	}
	
	@Override
	public int compareTo(AbilityInfo o) {
		return (this.sortValue - o.sortValue);
	}
	
	public boolean canBeCarried(Creature creature) {
		if (creature == null) {
			return true;
		}
		if (creature.getCreature().hands > 0) {
			return true;
		} else {
			return false;
		}
	}
	
	public String getAbilityTypeImageName()
	{
		String image = "ABILITY_IMAGE";
		
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
			case DASH:
				image = "DASH_IMAGE";
				break;
			case ABILITY:
			default:
				if (equipped) {
					image = "ABILITY_EQUIPPED_IMAGE";
				} else {
					image = "ABILITY_IMAGE";
				}
				break;
		}
		
		if (isRoughTerrain) {
			image = ability.ability.name;
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
			case DEFENDING:
				image = "PROTECTION_IMAGE";
				break;
			case HIDING:
				image = "SNEAK_IMAGE";
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
			case RESIST_KNOCKBACK:
				image = "RESIST_KNOCKBACK_IMAGE";
				break;
			case FLIGHT:
				image = "FLIGHT_IMAGE";
				break;
			case CHARGE:
				image = "CHARGE_IMAGE";
				break;
			case KNOCKBACK:
				image = "KNOCKBACK_IMAGE";
				break;
			case RESIST_BURST:
				image = "RESIST_BURST";
				break;
			case MISSILE_DEFENCE:
				image = "MISSILE_DEFENCE";
				break;
			case DASH:
				image = "DASH_IMAGE";
				break;
			default:
				image = "MAGIC_ITEM_IMAGE";
				break;
		}
		
		return image;
	}
	
	public String toString() {
		return name + " - " + abilityType;
	}
}
