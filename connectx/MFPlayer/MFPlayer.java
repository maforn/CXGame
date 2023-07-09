/*
 *  Copyright (C) 2022 Lamberto Colazzo
 *
 *  This file is part of the ConnectX software developed for the
 *  Intern ship of the course "Information technology", University of Bologna
 *  A.Y. 2021-2022.
 *
 *  ConnectX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This  is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details; see <https://www.gnu.org/licenses/>.
 */

package connectx.MFPlayer;

import connectx.CXPlayer;
import connectx.CXBoard;
import connectx.CXGameState;
import connectx.CXCell;
import connectx.CXCellState;

import java.util.TreeSet;
import java.util.Random;
import java.util.concurrent.TimeoutException;
import java.util.LinkedHashMap;
import java.util.Arrays;

/**
 * Software player only a bit smarter than random.
 * <p>
 * It can detect a single-move win or loss. In all the other cases behaves
 * randomly.
 * </p>
 */
public class MFPlayer implements CXPlayer {

	private Random rand;
	private CXGameState myWin;
	private CXGameState yourWin;
	private int TIMEOUT;
	private long START;

	// Array containing the column visit order. Priority is assigned to central columns
	int[] moveOrder;
	// Table of values used to calculate hash key
	long[][][] zobristTable;
	// Transposition table to save evaluation of calculated configurations
	LinkedHashMap<Long, int[]> transTable;
	// The maximum allowed size for the transposition table
	int maxTTSize = 20000000;
	// The size of the transposition table used (the dimension it is initialized to, not the actual number of entries)
	int transTableSize;
	// Key identifying the current board configuration. Used to save values in transposition table
	long hashKey;

	// Game settings
	int numOfRows; //rows
	int numOfCols; //cols
	int K; // coins to aligne in order to win
	boolean first; // whether the agent is the first player or not


	/* Default empty constructor */
	public MFPlayer() {
	}

	public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
		// New random seed for each game
		rand = new Random(System.currentTimeMillis());
		myWin = first ? CXGameState.WINP1 : CXGameState.WINP2;
		yourWin = first ? CXGameState.WINP2 : CXGameState.WINP1;
		TIMEOUT = timeout_in_secs;

		this.numOfRows = M;
		this.numOfCols = N;
		this.K = K;
		this.first = first;

		initMoveOrder();
		initZobristTable();
		initTransTable();
	}

	// Save the desired move order in array. Columns closer to the centre of the table are examined first
	private void initMoveOrder(){
		moveOrder = new int[numOfCols];
		for(int i = 0; i < numOfCols; i++){
			if(i % 2 == 0)
				moveOrder[i] = numOfCols/2 + i/2;
			else
				moveOrder[i] = numOfCols/2 - i/2 - 1;
		}
	}

	// Initialize the table used for Zobrist hashing and set the hash key to zero
	private void initZobristTable(){
		zobristTable = new long[numOfRows][numOfCols][2];
		for (int r = 0; r < numOfRows; r++) {
			for (int c = 0; c < numOfCols; c++) {
				for (int p = 0; p < 2; p++) {
					zobristTable[r][c][p] = rand.nextLong();
				}
			}
		}
		hashKey = 0;
	}

	// Initialize the transposition table
	private void initTransTable(){
		int desiredMemoryDepth = 8; // parameter to calculate the desired capacity of the table

		if(Math.round(Math.pow(maxTTSize * 3 / 4, 1.0 / 8.0)) < numOfCols)
			transTableSize = maxTTSize;
		else{
			int desiredCapacity = (int)Math.pow(numOfCols, desiredMemoryDepth); // how many entries we'd like to have at most in the table
			transTableSize = desiredCapacity * 4 / 3; // the necessary size to have desiredCapacity entries and load factor <= 0.75
		}

		// load factor set to 1 because
		// 1) no rehashing will be done
		// 2) the load factor won't be controlled the table itself
		transTable = new LinkedHashMap<Long, int[]>(transTableSize, 1);
	}

	/**
	 * Selects a free colum on game board.
	 * <p>
	 * Selects a winning column (if any), otherwise selects a column (if any)
	 * that prevents the adversary to win with his next move. If both previous
	 * cases do not apply, selects a random column.
	 * </p>
	 */
	public int selectColumn(CXBoard B) {
		START = System.currentTimeMillis(); // Save starting time

		CXBoard copyOfBoard = B.copy(); //save original board
		hashKey = updateHashKey(copyOfBoard, hashKey); //update hashKey to include opposer move

		// parametres for Iterative Deepening
		int alpha = Integer.MIN_VALUE;
		int beta = Integer.MAX_VALUE;
		int player = B.currentPlayer();

		// compute best choice for current configuration
		int choice = ID(B, player, alpha, beta, hashKey);

		// update hashKey to include player move
		copyOfBoard.markColumn(choice);
		hashKey = updateHashKey(copyOfBoard, hashKey);

		return choice;
	}

	private int ID(CXBoard board, int player, int alpha, int beta, long hashKey) {
		int bestSavedScore = Integer.MIN_VALUE; // initialize bestScore to loss
		int bestSavedCol = board.getAvailableColumns()[0]; // save random column
		int freeCells = board.numOfFreeCells(); //maximum depth that can be searched

		try{
			for (int depth = 1; depth <= freeCells; depth++) {
				int[] eval = alphaBeta(board, depth, player, alpha, beta, hashKey);
				if(eval[0] == Integer.MIN_VALUE) // if inevitable loss is discovered,
					break; // maintain previous best choice so to positicipate the loss
				else{
					bestSavedScore = eval[0];
					bestSavedCol = eval[1];
					if(bestSavedScore >= beta) // if winning situation is discovered, break
						break;
				}
			}
		} catch (TimeoutException e) { }

		return bestSavedCol;
	}

	private int[] alphaBeta(CXBoard board, int depth, int player, int alpha, int beta, long hashKey) throws TimeoutException {

		// Evaluate leaf / open configuration reached at depth = 0
		if (board.gameState() != CXGameState.OPEN) {
			if (board.gameState() == CXGameState.DRAW)
				return new int[] {0, -1}; // -1 is a placeholder
			else
				return new int[] {((board.gameState() == myWin) ? Integer.MAX_VALUE : Integer.MIN_VALUE), -1};
		} else if (depth == 0)
			return new int[]{0, -1}; // treat open configuration as a draw

		// Check transposition table and return saved data if found
		int[] saved = checkTransTable(hashKey);
		if(saved != null)
			return saved;

		int bestScore, bestCol = -1, colsToCheck;

		// Symmetry check
		boolean isSymmetric = isSymmetric(board);

		// If it's the player's turn, initialize the best score to the smallest possible value
		if (board.currentPlayer() == player) {
			bestScore = Integer.MIN_VALUE;
			for (int i = 0; i < numOfCols; i = isSymmetric ? i+2 : i+1) {
				int col = moveOrder[i];
				if(board.fullColumn(col))
					continue;
				checktime();
				board.markColumn(col);
				hashKey = updateHashKey(board, hashKey);
				int[] eval = alphaBeta(board, depth - 1, player, alpha, beta, hashKey);
				if (eval[0] > bestScore) {
					bestScore = eval[0];
					bestCol = col;
				}
				hashKey = updateHashKey(board, hashKey); // Undo changes to hashKey by xoring again with same value
				board.unmarkColumn();
				alpha = Math.max(alpha, bestScore);
				if (beta <= alpha)
					break; // Beta cutoff
			}
		}
		// If it's the opposing player's turn, initialize the best score to the largest possible value
		else {
			bestScore = Integer.MAX_VALUE;
			for (int i = 0; i < numOfCols; i = isSymmetric ? i+2 : i+1) {
				int col = moveOrder[i];
				if(board.fullColumn(col))
					continue;
				checktime();
				board.markColumn(col);
				hashKey = updateHashKey(board, hashKey);
				int[] eval = alphaBeta(board,depth - 1, player, alpha, beta, hashKey);
				if (eval[0] < bestScore) {
					bestScore = eval[0];
					bestCol = col;
				}
				hashKey = updateHashKey(board, hashKey); // Undo changes to hashKey by xoring again with same value
				board.unmarkColumn();
				beta = Math.min(beta, bestScore);
				if (beta <= alpha)
					break; // Alpha cutoff
			}
		}

		// Save data in transposition table if winning / losing configuration is found
		if(bestScore == Integer.MIN_VALUE || bestScore == Integer.MAX_VALUE)
			updateTransTable(hashKey, new int[]{bestScore, bestCol});

		return new int[]{bestScore, bestCol};
	}

	// Returns true if the board configuration is symmetric
	boolean isSymmetric(CXBoard board)throws TimeoutException{
		checktime();
		CXCell[] MC = board.getMarkedCells();
		for(CXCell cell : MC){
			if(board.cellState(cell.i, numOfCols - cell.j - 1) != cell.state)
				return false;
		}
		return true;
	}

	// Update hashKey to include last move. Returns the updated hashKey
	long updateHashKey(CXBoard board, long hashKey){
		CXCell lastMove = board.getLastMove();
		if(lastMove != null) {
			int lastPlayer = (lastMove.state == CXCellState.P1) ? 0 : 1;
			hashKey ^= zobristTable[lastMove.i][lastMove.j][lastPlayer];
		}
		return hashKey;
	}

	// Checks if the transposition table contains an entry with hash as key. If so, it makes it the newest element of the
	// table and it returns it. Otherwise, it returns null.
	int[] checkTransTable(Long hash)throws TimeoutException {
		checktime();
		int[] saved = transTable.get(hash);
		if(saved != null)
			transTable.put(hash, saved); // make the entry the newest in the table
		return saved;
	}

	// Add new data to the transposition table. If the table capacity exceeds the maximum allowed,
	// the oldest entry in the table is removed.
	void updateTransTable(Long hash, int[] newRes) throws TimeoutException{
		checktime();
		// remove oldest entry if load factor exceeds optimal value
		if (transTable.size() > transTableSize*0.75) {
			Long firstKey = transTable.keySet().iterator().next();
			transTable.remove(firstKey);
		}
		transTable.put(hash, newRes);
	}

	private int heuristic(CXBoard board) throws TimeoutException {
		checktime();
		return isNotColour(board.getBoard(), board.getLastMove().i, board.getLastMove().j);
	}

	private int isNotColour(CXCellState[][] B, int i, int j) {
		int n, max = 1, free;

		CXCellState me = first ? CXCellState.P1 : CXCellState.P2;
		CXCellState enemy = (me == CXCellState.P1) ? CXCellState.P2 : CXCellState.P1;

		// Useless pedantic check
		if (me == CXCellState.FREE)
			return max;

		// Horizontal check
		free = 1;
		n = 1;
		for (int k = 1; j - k >= 0 && B[i][j - k] != enemy; k++) {
			free++;
			if (B[i][j - k] == me) n++;
		} // backward check
		for (int k = 1; j + k < numOfCols && B[i][j + k] != enemy; k++) {
			free++;
			if (B[i][j + k] == me) n++;
		} // forward check
		if (free >= K) max = n;

		// Vertical check
		free = 1;
		n = 1;
		for (int k = 1; i + k < numOfRows && B[i + k][j] != enemy; k++) {
			free++;
			if (B[i + k][j] == me) n++;
		}
		if (free >= K) max = Math.max(max, n);

		// Diagonal check
		free = 1;
		n = 1;
		for (int k = 1; i - k >= 0 && j - k >= 0 && B[i - k][j - k] != enemy; k++) {
			free++;
			if (B[i - k][j - k] == me) n++;
		} // backward check
		for (int k = 1; i + k < numOfRows && j + k < numOfCols && B[i + k][j + k] != enemy; k++) {
			free++;
			if (B[i + k][j + k] == me) n++;
		} // forward check
		if (free >= K) max = Math.max(n, max);

		// Anti-diagonal check
		free = 1;
		n = 1;
		for (int k = 1; i - k >= 0 && j + k < numOfCols && B[i - k][j + k] != enemy; k++) {
			free++; // backward check
			if (B[i - k][j + k] == me) n++; // backward check
		}
		for (int k = 1; i + k < numOfRows && j - k >= 0 && B[i + k][j - k] != enemy; k++) {
			free++; // forward check
			if (B[i + k][j - k] == me) n++; // forward check
		}
		if (free >= K) max = Math.max(max, n);

		return max * 1000 + (this.numOfCols / 2) - Math.abs(j - (this.numOfCols / 2));
	}

	private void checktime() throws TimeoutException {
		if ((System.currentTimeMillis() - START) / 1000.0 >= TIMEOUT * (99.0 / 100.0))
			throw new TimeoutException();
	}

	public String playerName() {
		return "MFPlayer";
	}
}
