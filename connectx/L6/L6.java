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

package connectx.L6;

import connectx.CXPlayer;
import connectx.CXBoard;
import connectx.CXGameState;
import connectx.CXCell;
import connectx.CXCellState;

import java.util.TreeSet;
import java.util.Random;
import java.util.concurrent.TimeoutException;
import java.util.HashMap;

import java.util.Arrays;

/**
 * Software player only a bit smarter than random.
 * <p>
 * It can detect a single-move win or loss. In all the other cases behaves
 * randomly.
 * </p>
 */
public class L6 implements CXPlayer {

    private Random rand;
    private CXGameState myWin;
    private CXGameState yourWin;
    private int TIMEOUT;
    private long START;

    int M; //rows
    int N; //cols
    int K;
    boolean first;

    int depth;

    /* Default empty constructor */
    public L6() {
    }

    public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
        // New random seed for each game
        rand = new Random(System.currentTimeMillis());
        myWin = first ? CXGameState.WINP1 : CXGameState.WINP2;
        yourWin = first ? CXGameState.WINP2 : CXGameState.WINP1;
        TIMEOUT = timeout_in_secs;

        this.M = M;
        this.N = N;
        this.K = K;

        this.first = first;

        this.depth = 7;

        //transTableCapacity = 500;
        //transTable = new HashMap<>(transTableCapacity);
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

        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;
        int player = B.currentPlayer();
        Integer[] L = B.getAvailableColumns();

        try{
            return alphaBeta(B, B.getAvailableColumns(), depth, player,  alpha, beta, depth)[1];
        }catch(TimeoutException e){
            System.err.println("Timeout L" + (depth - 1) + ", random column returned");
            return L[rand.nextInt(L.length)];
        }
    }

    private int[] alphaBeta(CXBoard board, Integer[] L, int depth, int player, int alpha, int beta, int d) throws TimeoutException {

        if (board.gameState() != CXGameState.OPEN) {
            if (board.gameState() == CXGameState.DRAW)
                return new int[] {0, -1};
            else
                return new int[] {((board.gameState() == myWin) ? Integer.MAX_VALUE : Integer.MIN_VALUE), -1};
        } else if (depth == 0)
            //return new int[]{heuristic(board), -1};
            return new int[]{0, -1};

        int bestScore, bestCol = L[rand.nextInt(L.length)];

        // If it's the maximizing player's turn, initialize the best score to the smallest possible value
        if (board.currentPlayer() == player) {
            bestScore = Integer.MIN_VALUE;
            // Iterate over all possible moves and recursively evaluate each one
            for (int col : L) {
                checktime();
                board.markColumn(col);
                int[] eval = alphaBeta(board, board.getAvailableColumns(), depth - 1, player, alpha, beta, depth);
                if (eval[0] > bestScore) {
                    bestScore = eval[0];
                    bestCol = col;
                    //shortestPathToWin = eval[2] + 1;
                }
                board.unmarkColumn();
                alpha = Math.max(alpha, bestScore);
                if (beta <= alpha)
                    break; // Beta cutoff
            }
        }
        // If it's the minimizing player's turn, initialize the best score to the largest possible value
        else {
            bestScore = Integer.MAX_VALUE;
            // Iterate over all possible moves and recursively evaluate each one
            for (int col : L) {
                checktime();
                board.markColumn(col);
                int[] eval = alphaBeta(board, board.getAvailableColumns(), depth - 1, player, alpha, beta, depth);
                board.unmarkColumn();

                if (eval[0] < bestScore) {
                    bestScore = eval[0];
                    bestCol = col;
                }
                beta = Math.min(beta, bestScore);
                if (beta <= alpha)
                    break; // Alpha cutoff
            }
        }
        /*
        if(alpha == Integer.MIN_VALUE || beta == Integer.MAX_VALUE)
            return new int[]{bestScore, bestCol, longestPathToBest};
        else
            return new int[]{bestScore, bestCol, shortestPathToWin};
         */
        return new int[]{bestScore, bestCol};
    }

    private void checktime() throws TimeoutException {
        if ((System.currentTimeMillis() - START) / 1000.0 >= TIMEOUT * (99.0 / 100.0))
            throw new TimeoutException();
    }

    public String playerName() {
        return "L6";
    }
}
