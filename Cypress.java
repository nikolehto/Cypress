import reversi.*;
import java.util.Vector;
import java.util.Stack;
import java.util.Collections;
import java.util.ArrayList;


public class Cypress implements ReversiAlgorithm {

	class NextMove implements Comparable<NextMove>	// Reference http://stackoverflow.com/questions/2568059/sort-a-vector-of-custom-objects
	{
		NextMove(Move move_p, int score_p, ArrayList<NextMove> sortedMoves_p)	// Constructor
		{
		move = move_p;
		score = score_p;
		sortedMoves = sortedMoves_p;
		}
		
		public int compareTo(NextMove other) {	// To make sorting possible. Returns negative integer if score is bigger.
			if ((other.score == Integer.MIN_VALUE) || (score == Integer.MAX_VALUE))	// These goes over limits of integer
				return -1;
			else if ((other.score == Integer.MAX_VALUE) || (score == Integer.MIN_VALUE))
				return 1;
			else
				return (other.score - score);
			}
				
		void setScore(int score_p)
		{
		score = score_p;
		}
		
		void setMove(Move move_p)
		{
		move = move_p;
		}
		
		void setSortedMoves(ArrayList<NextMove> sortedMoves_p)
		{
		sortedMoves = sortedMoves_p;
		}
			
		int getScore()
		{
		return score;
		}
		
		Move getMove()
		{
		return move;
		}
		
		ArrayList<NextMove> getSortedMoves()
		{
		return sortedMoves;
		}
		

		
		ArrayList<NextMove> sortedMoves = new ArrayList<NextMove>();
		Move move = null;
		int score = 0;
	}

    // Constants
	private final static int MY_POSITION_PRIORITY = 1;
	private final static int OPPONENT_POSITION_PRIORITY = 1;	
	
	// Variables
	//int mobility_factor = 1;
	//int mark_factor = 1;
	int stable_mark_factor = 200;
	//int leaves = 0;
	boolean initialized;
	volatile boolean running; // Note: volatile for synchronization issues.
	GameController controller;
    GameState initialState;
    int myIndex;
    Move selectedMove;
	
	// Test Varailabel
	//ArrayList<ArrayList<NextMove>> ddarrays = new ArrayList<ArrayList<NextMove>>();	// ArrayLists of different depths
	
	
	
    public Cypress() {} //the constructor

	int[][] normalBoardValue =    {{ 800,  -25,  25,  50,  50,  25, -25, 800},
								{    -25,  -75,  25, -25, -25,  25, -75,  -25},
								{     25,   25,  50,   0,   0,  50,  25,  25},
								{     50,  -25,   0,   0,   0,   0, -25,  50},
								{     50,  -25,   0,   0,   0,   0, -25,  50},
								{     25,   25,  50,   0,   0,  50,  25,  25},
								{    -25,  -75,  25, -25, -25,  25, -75, -25},
								{    800,  -25,  25,  50,  50,  25, -25, 800}};


							
	int[][] startBoardValue =    {{800,  -50,    0,    0,    0,    0,  -50,  800},
								 { -50, -100,    0,  -25,  -25,    0, -100,  -50},
								 {   0,    0,   10,    0,    0,   10,    0,    0},
								 {   0,  -25,    0,   25,   25,    0,  -25,    0},
								 {   0,  -25,    0,   25,   25,    0,  -25,    0},
								 {   0,    0,   10,    0,    0,   10,    0,    0},
								 { -50, -100,    0,  -25,  -25,    0, -100,  -50},
								 { 800,  -50,    0,    0,    0,    0,  -50,  800}};
	
	int[][] boardValue;

    public void requestMove(GameController requester)
    {
        running = false;
        requester.doMove(selectedMove);
    }

    public void init(GameController game, GameState state, int playerIndex, int turnLength)
    {
		initialState = state;
		myIndex = playerIndex;
		controller = game;
		initialized = true;
	}

	public String getName() { return "Cypress"; }

	public void cleanup() {}

	public void run()
	{
		//implementation of the actual algorithm
		while(!initialized);
		initialized = false;
		running = true;
		
		selectedMove = null;
		
		NextMove nextmove = null;
		Move checkMove = null;
		Move bestMoveOfDepth = null;
		int currentDepth = 0;		
		int score = 0;
        int alpha = Integer.MIN_VALUE; // # Integer.MIN_VALUE = smallest possible integer, basically same as minus infinity	
        int beta = Integer.MAX_VALUE;
		
		int movesLeft = initialState.getMarkCount(-1);
		
		int depthlimit = movesLeft;
		
		//// This could be chosen in evaluation function
		if (movesLeft > 56)
		{
			boardValue = startBoardValue;
			//mobility_factor = 1;
			//mark_factor = 1;
		}
		
		else
		{
			boardValue = normalBoardValue;
			//mobility_factor = 1;
			//mark_factor = 1;
		}
		//////////////////////////////
		// PRINT SCORES OF THIS STATE
		//int currentscore = evaluate_state(initialState, myIndex, false);
		//System.out.println("Scores of initialState :" + currentscore);
		
		
		////////////////////////////
		// ADD MOVES TO CONTAINER //
		Vector tempmoves = initialState.getPossibleMoves(myIndex);  // Get possible moves for current gamestate
		ArrayList<NextMove> moves = new ArrayList<NextMove>();	// Size and capasity increment should be checked?

		ArrayList<NextMove> nextDepthMoves = null;
		
		for(int i = 0; i < tempmoves.size(); i++)
		{
			checkMove = (Move)tempmoves.elementAt(i);
			moves.add(new NextMove(checkMove, 0, new ArrayList<NextMove>()));
		}
		
		
		///////////////
		// MAIN LOOP //
        for(int j=1; j <= depthlimit; j++)	// Iterative deeping
        {
            currentDepth = j;
			//leaves = 0;
            for(int i=0; i < moves.size(); i++)	// Check all moves
            {
				nextmove = (NextMove)moves.get(i);
				checkMove = nextmove.getMove();
				nextDepthMoves = nextmove.getSortedMoves();
				
				nextmove = searchToDepth(initialState.getNewInstance(checkMove), nextDepthMoves, currentDepth - 1, alpha, beta, myIndex ^ 1);  // Alpha-beta search starting from checkMove. Because this is root, we do not need max(score, sTD)
		
				if (!(running))
					break;
					
				nextmove.setMove(checkMove);	// nextmove.move is empty because it is replaced with new nextmove
				
				if (nextmove.getScore() > alpha) // always maximimum at our turn. 
				{
					alpha = nextmove.getScore();
					bestMoveOfDepth = checkMove;
				}

				moves.set(i, nextmove);	// store object to arrayList
            }
			if (!(running))
			{	
				moves.clear();	// Must be cleared
				//ddarrays.clear();
				break;
			}
			
			Collections.sort(moves);
			
			alpha = Integer.MIN_VALUE;	// Revalue alpha of currentDepth before going next depth
			selectedMove = bestMoveOfDepth; 
			
						// does not occur!
			//if (moves.size() != initialState.getPossibleMoveCount(myIndex))
			//	System.out.println("Error in mainloop, Possible move count does not match with arraylist size");
			
			
			///////////////////////
			// Debug information //
			//System.out.println("Empty marks : " + initialState.getMarkCount(-1));
			//System.out.println("Best move of depth " + currentDepth + " : " + bestMoveOfDepth + "Score : " + alpha);
			//System.out.println("Depth : " + currentDepth);
			//System.out.println("Evaluated tables :" + leaves);
			
			
			/*	this prints previous depths placements of the current depth's best move
			ddarrays.add(new ArrayList<NextMove>(moves));

			if(j > 3)
			{
				for(int l = 0; l < j ; l++)
				{
					nextDepthMoves = (ArrayList<NextMove>)ddarrays.get(l);	// just save to somewhere
					for(int m = 0; m < nextDepthMoves.size(); m++)
					{
						checkMove = (Move)nextDepthMoves.get(m).getMove();
						//System.out.print(checkMove);
						
						if (checkMove == bestMoveOfDepth)
						{
							System.out.println("BestMove index: " + m + "/" + nextDepthMoves.size() + " in depth: " + (l+1));
						}
						//System.out.print(checkMove);
					}
				}
			}
			*/
		}

		
		if (running) // Make a move if there is still time left.
		{
			controller.doMove(selectedMove);
		}
	}
	
    NextMove searchToDepth(GameState currentState, ArrayList<NextMove> sortedMoves, int depth, int alpha, int beta, int index)	// Recursive sorting alphabeta algorithm	Reference: http://en.wikipedia.org/wiki/Alpha%E2%80%93beta_pruning
    {
		NextMove nextmove = null;
		ArrayList<NextMove> checkArray = null;
		Vector tempmoves = null;
		Move checkMove = null;
		GameState nextState = null;		
		int value = 0;
		
		///////////////////////////////
		// First part, special cases //
		if (!(running)) // If not running, return something fast
		{
			return new NextMove(null, 0, null);	
		}
		
		else if (depth == 0)  // If at depth limit, evaluate gamestate.
        {
            value = evaluate_state(currentState, index, false);	
			return new NextMove(null, value, sortedMoves);
        }
		
		else if (currentState.getPossibleMoveCount(index) != sortedMoves.size()) // If sortedMoves are empty, we have to add moves to arrayList. it is empty when depth =1 or when we haven't been here before because pruning.
		{
				// does not occur
			//if (sortedMoves.size() != 0)  // If it's not empty there is error.
			//	System.out.println("Error: sortedMoves is empty, time is wasted but program continues " + sortedMoves.size());
			
			tempmoves = currentState.getPossibleMoves(index);  // Get possible moves for current gamestate
			sortedMoves = new ArrayList<NextMove>();
			
			for(int i = 0; i < tempmoves.size(); i++)
			{
				sortedMoves.add(new NextMove((Move)tempmoves.elementAt(i), 0, new ArrayList<NextMove>()));
			}
		}
		
		/////////////////
		// Second part //
		if (currentState.getPossibleMoveCount(index) == 0)	  // If current player have no moves
		{
			if (currentState.getPossibleMoveCount(index ^ 1) > 0)	// and if next player will have game continues..
			{
				nextmove = searchToDepth(currentState, sortedMoves, depth, alpha, beta, index ^ 1); // depthlimit is not reduced, because there is still same count of emptymarks, and depthlimit = emptymarks
				value = nextmove.getScore();
			}
			
			else	// but if neither player have no moves, game end is true
			{
				value = evaluate_state(currentState, index, true);
			}
		}
		
        else if (index == myIndex)  // MAXIMIZE if our turn
        {
			value = Integer.MIN_VALUE;
			
            for (int i=0; i < sortedMoves.size(); i++)    // Loop through possible moves
            {
				nextmove = (NextMove)sortedMoves.get(i);
				checkMove = nextmove.getMove();
				checkArray = nextmove.getSortedMoves();
				nextState = currentState.getNewInstance(checkMove);
				
                nextmove = searchToDepth(nextState, checkArray, depth - 1, alpha, beta, index ^ 1);  // Going down the tree

				nextmove.setMove(checkMove);
				
				value = Math.max(value, nextmove.getScore());
				alpha = Math.max(alpha, value);
				
				sortedMoves.set(i, nextmove);
                
				if (beta <= alpha) // Cutoff point
                {
					
					for (int j = (i + 1); j < sortedMoves.size(); j++)	// save moves of unknown scores to the container
					{
						nextmove = (NextMove)sortedMoves.get(j);
						nextmove.setScore(Integer.MIN_VALUE);
						sortedMoves.set(j, nextmove);
					}					
                    break;
                }			
            }	
			Collections.sort(sortedMoves);	
        }
		
        else    // MINIMIZE  if not our turn
        {
			value = Integer.MAX_VALUE;
			
            for (int i=0; i < sortedMoves.size(); i++)    // Loop through possible moves
            {
				nextmove = (NextMove)sortedMoves.get(i);
				checkMove = nextmove.getMove();
				checkArray = nextmove.getSortedMoves();
				nextState = currentState.getNewInstance(checkMove);
				
                nextmove = searchToDepth(nextState, checkArray, depth - 1, alpha, beta, index ^ 1);  // Going down the tree
				
				nextmove.setMove(checkMove);
				
				value = Math.min(value, nextmove.getScore());
				beta = Math.min(beta, value);
				
				sortedMoves.set(i, nextmove);
                
				if (beta <= alpha) // Cutoff point
                {
					for (int j = (i + 1); j < sortedMoves.size(); j++)
						{
						nextmove = (NextMove)sortedMoves.get(j);
						nextmove.setScore(Integer.MAX_VALUE);
						sortedMoves.set(j, nextmove);
						}
                    break;
                }
            }
			Collections.sort(sortedMoves, Collections.reverseOrder());			
		}
        return new NextMove(null, value, sortedMoves);
    }
	
	
    int evaluate_state(GameState state, int index, boolean gameEnded)
    {
        int score = 0;
		int myMarks = 0;
		int my_tiles = 0;
		int opp_tiles = 0;
		int player = myIndex;
        int opponent = myIndex^1;
		int playerMoves = 0;        // Amount of moves player has next turn
        int opponentMoves = 0;      // Amount of moves opponent has next turn
        int m = 0;
		int l = 0;
		int c = 0;
		
		//leaves++;
		
		//((state.getPossibleMoveCount(myIndex) == 0) && (state.getPossibleMoveCount(myIndex ^ 1) == 0)) this is checked already
		
		if (state.getMarkCount(-1) == 0)	// just in case, basically there shouldn't be moves and so gameEnded is true already
		{
			gameEnded = true;
		}
		
        if (gameEnded) // Maximize amount of own marks here.
        {
            myMarks = state.getMarkCount(myIndex);
			score = 10 * myMarks;
			
			if (myMarks > state.getMarkCount(myIndex ^ 1))
				score += (100000 * (state.getMarkCount(-1) + 1));	// earlier win is better
			else
				score -= (100000 * (state.getMarkCount(-1) + 1));	// earlier lose is worse.
        }
		
        else 
        {
		
			playerMoves = state.getPossibleMoveCount(player);		//mobility	  MOBILITY AND CORNER CLOSENESS REFERENCE: https://kartikkukreja.wordpress.com/2013/03/30/heuristic-function-for-reversiothello/
			opponentMoves = state.getPossibleMoveCount(opponent);
			

			if(playerMoves > opponentMoves)
			{
				m = (100 * playerMoves)/(playerMoves + opponentMoves);
			}   
			else if(playerMoves < opponentMoves)
			{   
				m = -(100 * opponentMoves)/(playerMoves + opponentMoves);
			}
			else
			{   
				m = 0;
			}
			
			
			my_tiles = opp_tiles = 0;		//corner closeness
			
			if(state.getMarkAt(0, 0) == -1)   
			{
				if(state.getMarkAt(0, 1) == player)
				{
					my_tiles++;
				}
				else if(state.getMarkAt(0, 1) == opponent)
				{
					opp_tiles++;
				}
				if(state.getMarkAt(1, 1) == player)
				{
					my_tiles++;
				}
				else if(state.getMarkAt(1, 1) == opponent)
				{
					opp_tiles++;
				}
				if(state.getMarkAt(1, 0) == player)
				{
					my_tiles++;
				}
				else if(state.getMarkAt(1, 0) == opponent)
				{ 
					opp_tiles++;
				}
			}
			if(state.getMarkAt(0, 7) == -1)   
			{
				if(state.getMarkAt(0, 6) == player)
				{
					my_tiles++;
				}
				else if(state.getMarkAt(0, 6) == opponent) 
				{
					opp_tiles++;
				}
				if(state.getMarkAt(1, 6) == player) 
				{
					my_tiles++;
				}
				else if(state.getMarkAt(1, 6) == opponent) 
				{
					opp_tiles++;
				}
				if(state.getMarkAt(1, 7) == player) 
				{
					my_tiles++;
				}
				else if(state.getMarkAt(1, 7) == opponent) 
				{
					opp_tiles++;
				}
			}
			if(state.getMarkAt(7, 0) == -1)   
			{
				if(state.getMarkAt(7, 1) == player) 
				{
					my_tiles++;
				}
				else if(state.getMarkAt(7, 1) == opponent) 
				{
					opp_tiles++;
				}
				if(state.getMarkAt(6, 1) == player) 
				{
					my_tiles++;
				}
				else if(state.getMarkAt(6, 1) == opponent) 
				{
					opp_tiles++;
				}
				if(state.getMarkAt(6, 0) == player) 
				{
					my_tiles++;
				}
				else if(state.getMarkAt(6, 0) == opponent) 
				{
					opp_tiles++;
				}
			}
			if(state.getMarkAt(7, 7) == -1)   
			{
				if(state.getMarkAt(6, 7) == player) 
				{
					my_tiles++;
				}
				else if(state.getMarkAt(6, 7) == opponent) 
				{
					opp_tiles++;
				}
				if(state.getMarkAt(6, 6) == player) 
				{
					my_tiles++;
				}
				else if(state.getMarkAt(6, 6) == opponent) 
				{
					opp_tiles++;
				}
				if(state.getMarkAt(7, 6) == player) 
				{
					my_tiles++;
				}
				else if(state.getMarkAt(7, 6) == opponent) 
				{
					opp_tiles++;
				}
			}
			l = -600 * (my_tiles - opp_tiles);
			
			
			int[][] stableMarks = refreshStableMarks(state, null, 0, 0);
			stableMarks = refreshStableMarks(state, stableMarks, 0, 7);
			stableMarks = refreshStableMarks(state, stableMarks, 7, 0);
			stableMarks = refreshStableMarks(state, stableMarks, 7, 7);
			
				for(int i=0; i < 8; i++)
				{
					for(int j=0; j < 8; j++)
					{
						if (state.getMarkAt(j, i) == myIndex)
						{
							score += (MY_POSITION_PRIORITY * boardValue[j][i]);
						}
						
						else if (state.getMarkAt(j, i) == (myIndex ^ 1))
						{
							score -= (OPPONENT_POSITION_PRIORITY * boardValue[j][i]);
						}
						
						score += (stable_mark_factor * stableMarks[j][i]);
					}
				}				
				
			score += m * 80;
			score += l;
			
		}
		
        return score;
		
			/*
			if (index == myIndex)
				{
				score = score + (mobility_factor * state.getPossibleMoveCount(index)) + (mark_factor * state.getMarkCount(index));
				}
				
			else
				{
				score = score - (mobility_factor * state.getPossibleMoveCount(index)) - (mark_factor * state.getMarkCount(index));
				}
			*/


	}
	
	int[][] refreshStableMarks(GameState state, int[][] stableMarks, int cornerX, int cornerY)
	{
		if (stableMarks == null)
		{
			stableMarks = new int[8][8];
		}
		int player = state.getMarkAt(cornerX, cornerY);
		int stepX = 0;
		int stepY = 0;
		int stable = 0;
		
		
		switch(cornerX)
		{
		case 0:	// LEFT
			switch(cornerY)
			{
				case 0:	// UP
					stepX = 1;
					stepY = 1;
					break;
					
				case 7:	// DOWN
					stepX = 1;
					stepY = -1;
					break;								
			}
			break;
		
		case 7:	// RIGHT
			switch(cornerY)
			{
				case 0:	// UP
					stepX = -1;
					stepY = 1;
					break;
					
				case 7:	// DOWN
					stepX = -1;
					stepY = -1;			
					break;						
			}
			break;
		}
   
    if (player == -1)    // is empty
        return stableMarks;

    if (player == myIndex)
        stable = 1;
    else
        stable = -1;


    for(int x = cornerX;(x >= 0) && (x <= 7); x += stepX)   // first row
    {
        if (state.getMarkAt(x, cornerY) == player)
            stableMarks[x][cornerY] = stable;

        else
            break;
    }
        
    for(int y = cornerY + stepY;(y >= 0) && (y <= 7); y += stepY)
    {
        for(int x = cornerX;(x >= 0) && (x <= 7);x += stepX)
        {
		
        if (state.getMarkAt(x , y) == player) 
			{
				if (x + stepX > 7 || x + stepX < 0)	// == WALL
					stableMarks[x][y] = stable;			
				
				else if (stableMarks[x + stepX][y - stepY] == stable)
					stableMarks[x][y] = stable;
				else
					break;
			}
		else
            break;
		
        }
    }
	
	
	for(int y = cornerY; (y >= 0) && (y <= 7); y+= stepY)   // first column
    {
        if (state.getMarkAt(cornerX, y) == player)
            stableMarks[cornerX][y] = stable;

        else
            break;
    }
        
    for(int x = cornerX + stepX;(x >= 0) && (x <= 7); x += stepX)
    {
        for(int y = cornerY;(y >= 0) && (y <= 7);y += stepY)
		{
			if (state.getMarkAt(x , y) == player)
			{
				if (y + stepY > 7 || y + stepY < 0)	// == WALL
					stableMarks[x][y] = stable;
					
				else if (stableMarks[x - stepX][y + stepY] == stable)
					stableMarks[x][y] = stable;
					
				else
					break;
			}
        else
            break;
        }		
	}
	
	return stableMarks;
	}
	
}