package model;

import java.util.BitSet;

public class Bitmap {
    private final BitSet bitSet;
    private final int size;

    public Bitmap(int size) {
        this.size = size;
        this.bitSet = new BitSet(size); // Por defecto false (0/libre)
    }

    public int allocateFirstFree() {
        int index = bitSet.nextClearBit(0);
        if (index >= size) return -1; // Lleno
        bitSet.set(index);
        return index;
    }

    /**
     * Busca una secuencia de 'length' bits libres contiguos.
     * Retorna el índice de inicio o -1 si no encuentra.
     */
    public int findContiguousFree(int length) {
        int count = 0;
        for (int i = 0; i < size; i++) {
            if (!bitSet.get(i)) {
                count++;
                if (count == length) return i - length + 1;
            } else {
                count = 0;
            }
        }
        return -1;
    }

    // Busca cualquier espacio libre, aunque sea de 1 bloque
    public int findFirstFree() {
        return bitSet.nextClearBit(0) < size ? bitSet.nextClearBit(0) : -1;
    }

    public void set(int index) { bitSet.set(index); }
    public void clear(int index) { bitSet.clear(index); }
    public boolean isSet(int index) { return bitSet.get(index); }
}
