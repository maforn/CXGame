package connectx.HashEntry;
public class HashEntry {
    public int eval;
    public int depth;
    public int bestCol;

    public HashEntry(int eval, int bestCol, int depth){
        this.eval = eval;
        this.depth = depth;
        this.bestCol = bestCol;
    }
}