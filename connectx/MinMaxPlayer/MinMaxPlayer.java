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

package connectx.MinMaxPlayer;

import connectx.CXPlayer;
import connectx.CXBoard;
import connectx.CXGameState;
import connectx.CXCell;
import connectx.CXCellState;
import java.util.TreeSet;
import java.util.Random;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * Software player only a bit smarter than random.
 * <p>
 * It can detect a single-move win or loss. In all the other cases behaves
 * randomly.
 * </p>
 */
public class MinMaxPlayer implements CXPlayer {
	private Random rand;
	private CXGameState myWin;
	private CXGameState yourWin;
	private int  TIMEOUT;
	private long START;

	int M; //rows
	int N; //cols
	int K;
	boolean first;

	/* Default empty constructor */
	public MinMaxPlayer() {
	}

	public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
		// New random seed for each game
		rand    = new Random(System.currentTimeMillis());
		myWin   = first ? CXGameState.WINP1 : CXGameState.WINP2;
		yourWin = first ? CXGameState.WINP2 : CXGameState.WINP1;
		TIMEOUT = timeout_in_secs;

		this.M = M;
		this.N = N;
		this.K = K;

		this.first = first;
	}

	// order columns based on height
	protected Integer[] orderColumns(CXBoard B, Integer[] L) {
		HashMap<Integer, Integer> unsortMap = new HashMap<>(); // create a key value pair for columns:height
		for (int i : L) { // for each free column
			int n = 0; // count not empty cells
			for (int e = 0; e < this.M; e++) {
				if (B.cellState(i, e) != CXCellState.FREE) n++;
			}
			unsortMap.put(i, n); // put in the map column:occupied cells
		}
		// sort by ascending occupied cells, so we first have the emptier columns
		return new ArrayList<Integer>(sortHashMapByValues(unsortMap).keySet()).toArray(Integer[]::new);
	}

	public LinkedHashMap<Integer, Integer> sortHashMapByValues(
			HashMap<Integer, Integer> passedMap) {
		List<Integer> mapKeys = new ArrayList<>(passedMap.keySet());
		List<Integer> mapValues = new ArrayList<>(passedMap.values());
		Collections.sort(mapValues);
		Collections.sort(mapKeys);

		LinkedHashMap<Integer, Integer> sortedMap =
				new LinkedHashMap<>();

		Iterator<Integer> valueIt = mapValues.iterator();
		while (valueIt.hasNext()) {
			Integer val = valueIt.next();
			Iterator<Integer> keyIt = mapKeys.iterator();

			while (keyIt.hasNext()) {
				Integer key = keyIt.next();
                Integer comp1 = passedMap.get(key);
				Integer comp2 = val;

				if (comp1.equals(comp2)) {
					keyIt.remove();
					sortedMap.put(key, val);
					break;
				}
			}
		}
		return sortedMap;
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

		Integer[] L = B.getAvailableColumns();
		int save    = L[rand.nextInt(L.length)]; // Save a random column

		L = orderColumns(B, L);


		int bestValue = Integer.MIN_VALUE; // -1?
		int alpha = Integer.MIN_VALUE; // -1?
		int beta = Integer.MAX_VALUE; // 1?

		int depth = 12;
		int player = B.currentPlayer();
		// minmaxing code here for each column in the avaible ones
		try {
			for (int col : L) { // for each column
				B.markColumn(col);
				int value = minimax(B, B.getAvailableColumns(), depth, player, alpha, beta);
				B.unmarkColumn();
				if (value >= bestValue) {
					bestValue = value;
					save = col;
				}
				alpha = Math.max(alpha, value);
				if (beta <= alpha) {
					break;
				}
			}
			return save;
		} catch (TimeoutException e) {
			System.err.println("Timeout! Random column selected");
			return save;
		}
	}


	private void checktime() throws TimeoutException {
		if ((System.currentTimeMillis() - START) / 1000.0 >= TIMEOUT * (99.0 / 100.0))
			throw new TimeoutException();
	}

	public int minimax(CXBoard board, Integer[] L, int depth, int player, int alpha, int beta) throws TimeoutException {
		// check if the time is enough
		checktime();
		// Check if the game is over or if the depth limit has been reached
		if (board.gameState() != CXGameState.OPEN || depth == 0) {
			if (board.gameState() == CXGameState.DRAW || (depth == 0 && board.gameState() == CXGameState.OPEN)) return 0; // eval: if it's a draw return 0
			return (board.gameState() == myWin) ? 1 : -1; // if it's my win return 1, else return -1
		}


		int bestScore;

		// If it's the maximizing player's turn, initialize the best score to the smallest possible value
		if (board.currentPlayer() == player) {
			bestScore = Integer.MIN_VALUE;

			// Iterate over all possible moves and recursively evaluate each one
			for (int i : L) {
				board.markColumn(i);
				int score = minimax(board, board.getAvailableColumns(), depth - 1, player, alpha, beta);
				board.unmarkColumn();
				bestScore = Math.max(bestScore, score);
				alpha = Math.max(alpha, score); // bestScore??
				if (beta <= alpha) {
					break; // Beta cutoff
				}
			}
		}
		// If it's the minimizing player's turn, initialize the best score to the largest possible value
		else {
			bestScore = Integer.MAX_VALUE;

			// Iterate over all possible moves and recursively evaluate each one
			for (int i : L) {
				board.markColumn(i);
				int score = minimax(board, board.getAvailableColumns(),depth - 1, player, alpha, beta);
				board.unmarkColumn();
				bestScore = Math.min(bestScore, score);
				beta = Math.min(beta, score);
				if (beta <= alpha) {
					break; // Alpha cutoff
				}
			}
		}

		return bestScore;
	}

	/**
	 * Check if we can win in a single move
	 *
	 * Returns the winning column if there is one, otherwise -1
	 */	
	private int singleMoveWin(CXBoard B, Integer[] L) throws TimeoutException {
    for(int i : L) {
			checktime(); // Check timeout at every iteration
      CXGameState state = B.markColumn(i);
      if (state == myWin)
        return i; // Winning column found: return immediately
      B.unmarkColumn();
    }
		return -1;
	}

	/**
   * Check if we can block adversary's victory 
   *
   * Returns a blocking column if there is one, otherwise a random one
   */
	private int singleMoveBlock(CXBoard B, Integer[] L) throws TimeoutException {
		TreeSet<Integer> T = new TreeSet<Integer>(); // We collect here safe column indexes

		for(int i : L) {
			checktime();
			T.add(i); // We consider column i as a possible move
			B.markColumn(i);

			int j;
			boolean stop;

			for(j = 0, stop=false; j < L.length && !stop; j++) {
				//try {Thread.sleep((int)(0.2*1000*TIMEOUT));} catch (Exception e) {} // Uncomment to test timeout
				checktime();
				if(!B.fullColumn(L[j])) {
					CXGameState state = B.markColumn(L[j]);
					if (state == yourWin) {
						T.remove(i); // We ignore the i-th column as a possible move
						stop = true; // We don't need to check more
					}
					B.unmarkColumn(); // 
				}
			}
			B.unmarkColumn();
		}

		if (T.size() > 0) {
			Integer[] X = T.toArray(new Integer[T.size()]);
 			return X[rand.nextInt(X.length)];
		} else {
			return L[rand.nextInt(L.length)];
		}
	}

	public String playerName() {
		return "MinMaxPlayer";
	}
}
