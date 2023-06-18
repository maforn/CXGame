package connectx.HashEntry;
public class HashEntry {
    public int eval;
    public int depth;
    public int bestCol;

    public HashEntry(int eval, int depth, int bestCol){
        this.eval = eval;
        this.depth = depth;
        this.bestCol = bestCol;
    }
}