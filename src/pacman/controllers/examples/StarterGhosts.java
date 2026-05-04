/**
 * Based on:
 * Novikov, A., Yakovlev, S., Gushchin, I. (2025)
 * Exploring the Possibilities of MADDPG for UAV Swarm Control
 * by Simulating in Pac-Man Environment
 *
 * Base code from:
 * Ms Pac-Man vs Ghosts AI Framework
 */
package pacman.controllers.examples;

import java.util.EnumMap;
import java.util.Random;
import java.util.*;
import pacman.controllers.Controller;
import pacman.game.Game;

import static pacman.game.Constants.*;

/*
 * Ghost team controller as part of the starter package - simply upload this file as a zip called
 * MyGhosts.zip and you will be entered into the rankings - as simple as that! Feel free to modify 
 * it or to start from scratch, using the classes supplied with the original software. Best of luck!
 * 
 * This ghost controller does the following:
 * 1. If edible or Ms Pac-Man is close to power pill, run away from Ms Pac-Man
 * 2. If non-edible, attack Ms Pac-Man with certain probability, else choose random direction
 */

public final class StarterGhosts extends Controller<EnumMap<GHOST,MOVE>>
{	
	private final static float CONSISTENCY=0.9f;	//attack Ms Pac-Man with this probability
	private final static int PILL_PROXIMITY=15;		//if Ms Pac-Man is this close to a power pill, back away
	
	Random rnd=new Random();
	EnumMap<GHOST,MOVE> myMoves=new EnumMap<GHOST,MOVE>(GHOST.class);
    // BFS algorithm, made with help from ChatGPT
    private MOVE bfsNextMove(Game game, int start, int target) {
        Queue<Integer> q = new LinkedList<>();
        Map<Integer, Integer> parent = new HashMap<>();
        Set<Integer> visited = new HashSet<>();

        q.add(start);
        visited.add(start);

        while (!q.isEmpty()) {
            int cur = q.poll();
            if (cur == target) break;

            for (MOVE m : game.getPossibleMoves(cur)) {
                int next = game.getNeighbour(cur, m);
                if (next != -1 && !visited.contains(next)) {
                    visited.add(next);
                    parent.put(next, cur);
                    q.add(next);
                }
            }
        }

        // backtrack one step
        int step = target;
        while (parent.containsKey(step) && parent.get(step) != start) {
            step = parent.get(step);
        }

        return game.getMoveToMakeToReachDirectNeighbour(start, step);
    }
    // A* Algorithm, made with help from ChatGPT
    private MOVE aStarNextMove(Game game, int start, int target) {
        PriorityQueue<int[]> open = new PriorityQueue<>(Comparator.comparingInt(a -> a[1]));
        Map<Integer, Integer> g = new HashMap<>();
        Map<Integer, Integer> parent = new HashMap<>();

        g.put(start, 0);
        open.add(new int[]{start, 0});

        while (!open.isEmpty()) {
            int cur = open.poll()[0];
            if (cur == target) break;

            for (MOVE m : game.getPossibleMoves(cur)) {
                int next = game.getNeighbour(cur, m);
                if (next == -1) continue;

                int newG = g.get(cur) + 1;

                if (!g.containsKey(next) || newG < g.get(next)) {
                    g.put(next, newG);

                    int h = game.getShortestPathDistance(next, target); // heuristic
                    int f = newG + h;

                    open.add(new int[]{next, f});
                    parent.put(next, cur);
                }
            }
        }

        int step = target;
        while (parent.containsKey(step) && parent.get(step) != start) {
            step = parent.get(step);
        }

        return game.getMoveToMakeToReachDirectNeighbour(start, step);
    }
    //Hybrid, made with help from ChatGPT
    private MOVE hybridNextMove(Game game, int start, int target) {
        int dist = game.getShortestPathDistance(start, target);

        if (dist > 20) {
            return aStarNextMove(game, start, target);
        } else {
            return bfsNextMove(game, start, target);
        }
    }
	public EnumMap<GHOST,MOVE> getMove(Game game,long timeDue)
	{
		for(GHOST ghost : GHOST.values())	//for each ghost
		{			
			if(game.doesGhostRequireAction(ghost))		//if ghost requires an action
			{
				//pinky changes start - Jaidan
				if (ghost == GHOST.PINKY) {

    if(game.getGhostEdibleTime(ghost)>0 || closeToPower(game)) {
        myMoves.put(ghost,game.getApproximateNextMoveAwayFromTarget(game.getGhostCurrentNodeIndex(ghost),game.getPacmanCurrentNodeIndex(),game.getGhostLastMoveMade(ghost),DM.PATH));
    } else {
        //Pinky logic made with help from chatGPT
        int pacmanPos = game.getPacmanCurrentNodeIndex();
        MOVE pacmanMove = game.getPacmanLastMoveMade();

        int target = pacmanPos;

        for (int i = 0; i < 2; i++) {
            int next = game.getNeighbour(target, pacmanMove);
            if (next == -1) break;
            target = next;
        }
        myMoves.put(ghost, aStarNextMove(game, game.getGhostCurrentNodeIndex(ghost), game.getPacmanCurrentNodeIndex()));
    }
}

// Inky changes start - Jaidan
else if (ghost == GHOST.INKY) {

    if(game.getGhostEdibleTime(ghost)>0 || closeToPower(game)) {
		myMoves.put(ghost,game.getApproximateNextMoveAwayFromTarget(game.getGhostCurrentNodeIndex(ghost),game.getPacmanCurrentNodeIndex(),game.getGhostLastMoveMade(ghost),DM.PATH));
    } else {

        myMoves.put(ghost, hybridNextMove(game, game.getGhostCurrentNodeIndex(ghost), game.getPacmanCurrentNodeIndex()));
    }
}
// Blinky changes start - Kevin
else if (ghost == GHOST.BLINKY) {

    if(game.getGhostEdibleTime(ghost)> 0 || closeToPower(game)){
        myMoves.put(ghost,game.getApproximateNextMoveAwayFromTarget(game.getGhostCurrentNodeIndex(ghost),game.getPacmanCurrentNodeIndex(),game.getGhostLastMoveMade(ghost),DM.PATH));
    } else {
        myMoves.put(ghost, bfsNextMove(game, game.getGhostCurrentNodeIndex(ghost), game.getPacmanCurrentNodeIndex()));
    }


}


// Clyde no changes, uses standard formula - Kevin
else if(game.getGhostEdibleTime(ghost)>0 || closeToPower(game))
    myMoves.put(ghost, game.getApproximateNextMoveAwayFromTarget(game.getGhostCurrentNodeIndex(ghost),game.getPacmanCurrentNodeIndex(),game.getGhostLastMoveMade(ghost),DM.PATH));
else 
{
    if(rnd.nextFloat()<CONSISTENCY)myMoves.put(ghost,game.getApproximateNextMoveTowardsTarget(game.getGhostCurrentNodeIndex(ghost),game.getPacmanCurrentNodeIndex(),game.getGhostLastMoveMade(ghost),DM.PATH));
    else
    {
        MOVE[] possibleMoves = game.getPossibleMoves(
            game.getGhostCurrentNodeIndex(ghost),
            game.getGhostLastMoveMade(ghost));
        myMoves.put(ghost, possibleMoves[rnd.nextInt(possibleMoves.length)]);
    }
}
			}
		}
		return myMoves;
	}
	
    //This helper function checks if Ms Pac-Man is close to an available power pill
	private boolean closeToPower(Game game)
    {
    	int[] powerPills=game.getPowerPillIndices();
    	
    	for(int i=0;i<powerPills.length;i++)
    		if(game.isPowerPillStillAvailable(i) && game.getShortestPathDistance(powerPills[i],game.getPacmanCurrentNodeIndex())<PILL_PROXIMITY)
    			return true;

        return false;
    }
}
	
