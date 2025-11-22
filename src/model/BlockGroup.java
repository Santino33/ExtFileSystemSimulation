package model;

public class BlockGroup {
    public final int id;
    public final int startBlock; // Bloque físico inicial
    public final int totalBlocks;
    public final Bitmap blockBitmap;
    public final Bitmap inodeBitmap;

    public BlockGroup(int id, int startBlock, int numBlocks, int numInodes) {
        this.id = id;
        this.startBlock = startBlock;
        this.totalBlocks = numBlocks;
        this.blockBitmap = new Bitmap(numBlocks);
        this.inodeBitmap = new Bitmap(numInodes);
    }

    // Convierte índice relativo (bitmap) a físico (disco)
    public int getPhysicalBlock(int relativeIndex) {
        return startBlock + relativeIndex;
    }
}

