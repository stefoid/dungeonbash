package com.dbash.models;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import com.dbash.util.L;


// A shadowMap belongs to a character, representing the characters view of the map (which Locations the character can see).  
// Every time the character moves, it gets an update shadowmap message.
// Monsters will also use it so they see characters, based on the principle if the character can see them, they can see the character.
// Therefore to identify if a particular location is visible within a shadowmap, you just need to find out if the Location exists with
// the map.
public class ShadowMap {
	public static final boolean LOG = false && L.DEBUG;
	// so a RayPos is just a DungeonPosition with one extra parameter - distance from the center point.
	// the idea being that the 'rays' are generated only once at startup, and each point can store how far it is from the
	// center, and then a shadowmap can use that info to efficiently generate an *ordered set* of Locations sorted according to proximity.
	// therefore to find the closest creature, or all creatures in a certain range is just a linear search.
	@SuppressWarnings("serial")
	protected class RayPos extends DungeonPosition {
		public int distance;  // square radus
		public float trueDistance;  // circular radius
		public RayPos(int x, int y) {
			super(x,y);
		}

		// 'distance' a true distance, but a tile-based count (i.e. radius is a square, not a circle)
		public void setDistance(DungeonPosition centrePos) {
			distance = distanceTo(centrePos);
		}	
	}
	
	DungeonPosition centerPos;
	public Character owner;
	HashSet<Location> locations;
	int range;
	int totalRange = 4 * Map.RANGE;  // we use a much larger map than the 5 tile radius we can see, because we want the extra rays for
						  // for added resolution to enable us to see into corners and all the wall tiles along long, thin corridors.
	Map map;
	
	static Vector<RayPos[]> rays = null;
	
	// characters shadowmap constructor
	public ShadowMap(Character owner) {
		this.owner = owner;
		locations = new HashSet<Location>();
	}
	
	// burst effect shadowmap constructor.
	public ShadowMap() {
		this.owner = null;
		locations = new HashSet<Location>();
	}
	
	public ShadowMap(ShadowMap shadowMap) {
		this.centerPos = new DungeonPosition(shadowMap.centerPos);
		this.range = shadowMap.range;
		this.map = shadowMap.map;
		this.owner = shadowMap.owner;
		this.locations = new HashSet<Location>(shadowMap.locations);
	}
	
	// Make a shadowmap with a certain range form the centrePos.
	public void setMap(Map map, DungeonPosition centerPos, int range)
	{	
		this.centerPos = centerPos;
		this.range = range;
		this.map = map;
		
		// we only have to do this once per game.  calculate the rays for the normal LOS viewport.
		if (rays == null) {
			generateRays();
		}
		
		generateShadowMap();
	}
	
	// rather than create a new shadowmap every time, we just update this one, and that way we can inform locations that they
	// are no longer visible
	public void updateCenterPos(DungeonPosition centerPos)
	{
		this.centerPos = centerPos;
		generateShadowMap();
	}
	
	public boolean locationIsVisible(Location location)
	{
		if (locations.contains(location)) {
			return true;
		} else {
			return false;
		}
	}
	
	public boolean positionIsVisible(DungeonPosition position) {
		return locationIsVisible(map.location(position));
	}
	
	public List<Creature> getVisibleCreatures() {
		LinkedList<Creature> result = new LinkedList<Creature>();
		for (Location location : locations) {
			if (location.creature != null) {
				result.add(location.creature);
			}
		}
		return result;
	}
	
	public void refresh() {
		if (LOG) L.log("");
		for (Location location : locations) {
			location.updatePresenter();
		}
	}
	
	/*------------------------------------------------*/

	// So this will generate a shadow map which is a collection of positions that are visible from 
	// a certain position within a certain range.
	// The theory is we trace 'rays' from that point to every point on the permimeter of the square surrounding it.
	// A 'ray' is an array of points starting from that teh centre and ending on the perimeter
	// To generate the shadow map, you follow each and every ray from its start until it hits a solid object.
	// Because the 'rays' are always the same points (realtive to the centre), you only have to calculate them once.  
	// Its just the tracing of them from a certain point that you have to do when you generate a new shadow map.
	// The result of this is a Set of Locations that are visible form the focus of the shadowmap.  
	// As a shadowmap is generated, it updates each Location that is in it that it can be seen from this shadowmap.
	// So when drawing locations, you can easilly tell how to draw them depending on which shadowmap(s) are currently active.
    private void generateShadowMap()
    {
    	if (LOG) L.log("");
    	// The algorythm is to follow each ray in the vector until it hits solid rock, or the range limit, whichever comes first
    	// Then add that location to the set
    	HashSet<Location> newLocations = new HashSet<Location>();
    	
    	for (RayPos[] ray : rays) {
    		for (RayPos rayPos : ray) {
    			if (rayPos.distance > range) {
    				break;
    			}
    			
    			// work out the actual location by adding the centerPos to to the ray offset
    			Location loc = map.safeLocation(rayPos.x + centerPos.x, rayPos.y + centerPos.y);
				newLocations.add(loc);
				// if this is a character-ownned shadowmap, tell this location it is visible from this shadow map
				if (owner != null) {
					loc.setDiscovered();
				}
				
				// if that was not see-through, then this ray is stopped
    			if (loc.isOpaque()) {
    				break;
    			}
    		}
    	} 
        
    	// now make the newly calculated shadowmap the official one
    	locations = newLocations; 
    } 
    
    // Make a vector of 'rays' which are themselves arrays of RayPos.
    // startTile and endStile stuff
    private void generateRays()
    {
    	if (LOG) L.log("");
    	RayPos[] losPoints;
        int tileX;
        int tileY;
        Vector<RayPos[]> rays = new Vector<RayPos[]>();
        RayPos point = new RayPos(0, 0); 

        for(tileY = -totalRange; tileY <= totalRange; tileY++)
        {
            losPoints = getLinePositions(point, new DungeonPosition(-totalRange, tileY));
            rays.addElement(losPoints);
            losPoints = getLinePositions(point, new DungeonPosition(totalRange, tileY));
            rays.addElement(losPoints);
        }

        for(tileX = -totalRange-1; tileX < totalRange; tileX++)
        {
            losPoints = getLinePositions(point, new DungeonPosition(tileX, -totalRange));
            rays.addElement(losPoints);
            losPoints = getLinePositions(point, new DungeonPosition(tileX, totalRange));
            rays.addElement(losPoints);
        }

        ShadowMap.rays = rays;
    }
    
    // Make a single 'ray' - generate an array of RayPos that is a straight line from the start position (i.e. the center of the shadow map) to 
    // an endPosition (a tile on the permiter of the shadowmap).  
    private RayPos[] getLinePositions(DungeonPosition a, DungeonPosition b)
    {
        int deltax = Math.abs(b.x - a.x); // The difference between the x's
        int deltay = Math.abs(b.y - a.y); // The difference between the y's
        int x = a.x; // Start x off at the first pixel
        int y = a.y; // Start y off at the first pixel
        int xinc1;
        int yinc1;
        int xinc2;
        int yinc2;
        int curpixel;
        int numpixels;
        int numadd;
        int den;
        int num;

        if(b.x >= a.x) // The x-values are increasing
        {
            xinc1 = 1;
            xinc2 = 1;
        }
        else // The x-values are decreasing
        {
            xinc1 = -1;
            xinc2 = -1;
        }

        if(b.y >= a.y) // The y-values are increasing
        {
            yinc1 = 1;
            yinc2 = 1;
        }
        else // The y-values are decreasing
        {
            yinc1 = -1;
            yinc2 = -1;
        }

        if(deltax >= deltay) // There is at least one x-value for every y-value
        {
            xinc1 = 0; // Don't change the x when numerator >= denominator
            yinc2 = 0; // Don't change the y for every iteration
            den = deltax;
            num = deltax >> 1;
            numadd = deltay;
            numpixels = deltax + 1; // There are more x-values than y-values			
        }
        else // There is at least one y-value for every x-value
        {
            xinc2 = 0; // Don't change the x for every iteration
            yinc1 = 0; // Don't change the y when numerator >= denominator
            den = deltay;
            num = deltay >> 1;
            numadd = deltax;
            numpixels = deltay + 1; // There are more y-values than x-values
        }

        RayPos[] losPoints = new RayPos[numpixels];

        for(curpixel = 0; curpixel < numpixels; curpixel++)
        {
            losPoints[curpixel] = new RayPos(x, y);
            losPoints[curpixel].setDistance(a);
            
            num += numadd; // Increase the numerator by the top of the fraction

            if(num >= den) // Check if numerator >= denominator
            {
                num -= den; // Calculate the new numerator value
                x += xinc1; // Change the x as appropriate
                y += yinc1; // Change the y as appropriate
            }

            x += xinc2; // Change the x as appropriate
            y += yinc2; // Change the y as appropriate
        }

        return losPoints;
    }
 
    public String toString() {
    	return "(pos "+centerPos+" - owner "+owner+")";
    }
}
