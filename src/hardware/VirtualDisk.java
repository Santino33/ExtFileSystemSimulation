package hardware;

import java.util.Arrays;

public enum VirtualDisk {
    INSTANCE;

    private byte[][] blocks; // El almacenamiento crudo
    private int blockSize;
    private int totalBlocks;

    public void init(int totalBlocks, int blockSize) {
        this.totalBlocks = totalBlocks;
        this.blockSize = blockSize;
        this.blocks = new byte[totalBlocks][blockSize];
        // Inicializar con ceros
        for (int i = 0; i < totalBlocks; i++) {
            Arrays.fill(blocks[i], (byte) 0);
        }
        System.out.println("[DISK] Inicializado con " + totalBlocks + " bloques de " + blockSize + " bytes.");
    }

    public void writeBlock(int blockId, byte[] data) {
        if (blockId < 0 || blockId >= totalBlocks) throw new IllegalArgumentException("Block out of bounds");
        // Copiamos los datos para simular persistencia, truncando si es necesario
        int length = Math.min(data.length, blockSize);
        System.arraycopy(data, 0, blocks[blockId], 0, length);
    }

    public byte[] readBlock(int blockId) {
        if (blockId < 0 || blockId >= totalBlocks) throw new IllegalArgumentException("Block out of bounds");
        return Arrays.copyOf(blocks[blockId], blockSize);
    }

    public int getBlockSize() { return blockSize; }
}