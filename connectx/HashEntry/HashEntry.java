package connectx.HashEntry;
public class HashEntry {
    public int eval;
    public int bestCol;

    public int player;

    public HashEntry(int eval, int bestCol, int player){
        this.eval = eval;
        this.bestCol = bestCol;
        this.player = player;
    }
}