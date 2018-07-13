 package battleship.players;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import battleship.handling.Settings;
import battleship.ships.Ship;
import battleship.tiles.ShipTile;
import battleship.utilities.Coordinates;

/**
 * This class lets you make your own AI for a computer-controlled player.
 * Read the notes below to find out what you should rewrite.
 */
public class OpponentByBirte extends ComputerOpponent {
    
    // in sink mode we try to sink a ship based on some initial hit
    // otherwise we are in (parity) search mode
    private boolean sinkMode = false;

    // coordinates we select from when (parity) searching a ship
    private List<Coordinates> coordinatesToTry = new LinkedList<Coordinates>();
    
    // we find the direction of a hit ship by `incrementing' the last hit 
    // coordinate
    public static final Coordinates LEFT = new Coordinates(-1, 0);
    public static final Coordinates RIGHT = new Coordinates(1, 0);
    public static final Coordinates UP = new Coordinates(0, -1);
    public static final Coordinates DOWN = new Coordinates(0, 1);
    
    // after sinking a ship, the list of untried directions is 
    // re-initialized using the directions list above 
    private List<Coordinates> untriedDirections = new LinkedList<Coordinates>();
    private Coordinates currentlyTriedDirection;
    private Coordinates initialHit; // to be able to switch direction
    private Coordinates lastHit;
    
	/**
	 * This is this class' constructor.
	 * @param name The player's name.
	 */
	public OpponentByBirte(String name) {
		super(name); // This calls the base class' constructor, which calls its base class' constructor. You should keep this line.
		initializeCoordinatesToTry();
		initializeUntriedDirections();
	}
	
	/**
	 * We do a parity search: we see the field as b/w chess board and 
	 * only shoot at tiles of one color, other tiles are marked as 
	 * 'rejected'  
	 */
	private void initializeCoordinatesToTry() {
	    for (int x = 0; x < Settings.PLAYING_FIELD_HORIZONTAL_SIZE; x++) {
	        for (int y = 0; y < Settings.PLAYING_FIELD_VERTICAL_SIZE; y++) {
	            if ((x + y) % 2 == 0)
	                coordinatesToTry.add(new Coordinates(x, y));
	        }
	    }
	}
	
    /**
     * (re)fill the list with possible direction increments
     */
    private void initializeUntriedDirections() {
        untriedDirections.clear();
        untriedDirections.add(LEFT);
        untriedDirections.add(RIGHT);
        untriedDirections.add(UP);
        untriedDirections.add(DOWN);
    }

	/**
	 toString() overrides an object's behavior when it is cast to or used as a string.
	 */
	@Override
	public String toString() {
		return name + " (Birtes AI)";
	}

	@Override
	public Coordinates promptToFireShot() throws Exception {
	    if (sinkMode) {
	        // hunt the ship down, initialHit contains the coordinate 
	        // where the ship was first hit, we then try following the 
	        // ship in the possible directions
	        Coordinates candidate = new Coordinates(lastHit.x + currentlyTriedDirection.x, lastHit.y + currentlyTriedDirection.y);
    	        
	        // check if we are in the field's bound
            // the last check should always be false as we keep 
            // track of what we have done
	        while (candidate.x >= Settings.PLAYING_FIELD_HORIZONTAL_SIZE  || 
                    candidate.x < 0 ||
                    candidate.y >= Settings.PLAYING_FIELD_VERTICAL_SIZE ||
                    candidate.y < 0 ||
                    resultObservations[candidate.x][candidate.y] != null) {
	            // if we are out of the field's bound, we change to 
                // the next possible direction and start from the 
                // initial hit 
                currentlyTriedDirection = untriedDirections.remove(0);
                this.lastHit = initialHit;
                candidate = new Coordinates(lastHit.x + currentlyTriedDirection.x, lastHit.y + currentlyTriedDirection.y);;
	        }
	        
    	        // we have a candidate coordinate within the field's bound
	        // do not shot on that field later in (parity) search mode
	        coordinatesToTry.remove(candidate);
            return candidate;
	    } else {
	        // parity search mode
	        int bound = coordinatesToTry.size();
	        int randomIndex = new Random().nextInt(bound);
	        Coordinates candidate = coordinatesToTry.remove(randomIndex);
	        return candidate;
	    }
	}

	@Override
	public void youHaveMissed(Coordinates coordinates) {
		super.youHaveMissed(coordinates);
		if (sinkMode) {
		    currentlyTriedDirection = untriedDirections.remove(0); // trigger trying another direction
        	this.lastHit = initialHit;
		}
	}

	@Override
	public void youHaveHitYourTarget(Coordinates coordinates) {
		super.youHaveHitYourTarget(coordinates);
		this.lastHit = coordinates;
		if (!sinkMode) {
	        this.sinkMode = true;
            this.initialHit = coordinates;
            this.currentlyTriedDirection = untriedDirections.remove(0);
		}
	}

	@Override
	public void youHaveSunkAnEnemyShip(Coordinates lastHit, Ship ship) {
		super.youHaveSunkAnEnemyShip(lastHit, ship);
        avoidSurrounding(ship);
        this.sinkMode = false;
        initializeUntriedDirections();
        this.lastHit = null;
        this.initialHit = null;
    }
	
	public void avoidSurrounding(Ship ship) {
       for (ShipTile tile : ship.getTiles()) {
            coordinatesToTry.remove(new Coordinates(tile.horizontalIndex() + 1, tile.verticalIndex()));
            coordinatesToTry.remove(new Coordinates(tile.horizontalIndex() - 1, tile.verticalIndex()));
            coordinatesToTry.remove(new Coordinates(tile.horizontalIndex(), tile.verticalIndex() + 1));
            coordinatesToTry.remove(new Coordinates(tile.horizontalIndex(), tile.verticalIndex() - 1));
        }
	}

}
