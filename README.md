# CXGame
CXGame is a generalized version of the Connect Four game where the grid has an arbitrary size and the players need to align an arbitrary number of pieces to win.

## The agent
This project develops a software agent capable of playing CXGame with great proficiency.
The final agent is ``IDPlayer.java``. It employs:

- **Iterative deepening** to gradually increase the depth of the game-tree search;
- **Alpha-beta pruning** to reduce the number of explored nodes;
- **Symmetry check** to further reduce the number of explored configurations;
- **Transposition table based on Zobrist hashing** to save the evaluations of previously analysed configurations.

A heuristic for the evaluation of open positions is also provided, although not used by the agent. 
The computational cost of the heuristic limits the number of configurations the agent can explore in a turn, ultimately reducing its performance.

## Commands

### Building
``compile.bat`` compiles the project.

### Playing
- **Human vs Computer**:  In the project directory run:

  	java -cp . connectx.CXGame 6 7 4 connectx.IDPlayer.IDPlayer

This will run a game against IDPlayer in a 6x7 matrix where the players have to align 4 pieces to win.

- **Computer vs Computer**: In the project directory run:

    	java -cp . connectx.CXGame 6 7 4 connectx.IDPlayer.IDPlayer connectx.L1.L1

This will run a game between IDPlayer and L1 in a 6x7 matrix where the players have to align 4 pieces to win.

**Note**: You need to click on the display in order to have the agents make a move in their turn.

### Testing
``autoTest.cmd`` runs several games in multiple configurations between IDPlayer and the agents L0-L6.
The results are saved in ``results.txt``.

``autoTestIDHvsID.bat`` runs several games in multiple configurations between two versions of IDPlayer: one with heuristic (IDPlayerHeur) and one without (IDPlayer).  
The results are saved in ``results.txt``.