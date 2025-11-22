package allocation;

import model.BlockGroup;
import model.Extent;

import java.util.ArrayList;
import java.util.List;

public class SmartFitAllocator implements AllocatorStrategy {
    @Override
    public List<Extent> allocate(List<BlockGroup> groups, int blocksNeeded) {
        List<Extent> allocations = new ArrayList<>();
        int blocksRemaining = blocksNeeded;
        int logicalOffsetCurrent = 0;

        // Intentamos llenar lo que falta iterando grupos
        for (BlockGroup bg : groups) {
            if (blocksRemaining == 0) break;

            // 1. Intento Ideal: Buscar todo el hueco contiguo en este grupo
            int startRel = bg.blockBitmap.findContiguousFree(blocksRemaining);

            if (startRel != -1) {
                // ¡Éxito total! Cabe todo junto aquí
                marcarYCrearExtent(bg, startRel, blocksRemaining, logicalOffsetCurrent, allocations);
                blocksRemaining = 0;
            } else {
                // 2. Fallback: El grupo está fragmentado o lleno.
                // Llenamos huecos disponibles en este grupo antes de saltar al siguiente.
                // (Simplificación: Buscamos el primer hueco libre y llenamos 1 a 1 o en pequeños grupos)
                // Para simular extents, intentaremos buscar el hueco más grande posible loop

                while (blocksRemaining > 0) {
                    int freeIdx = bg.blockBitmap.findFirstFree();
                    if (freeIdx == -1) break; // Grupo lleno totalmente

                    // Ver cuánto espacio contiguo hay desde freeIdx
                    int available = 0;
                    while (freeIdx + available < bg.totalBlocks &&
                            !bg.blockBitmap.isSet(freeIdx + available) &&
                            available < blocksRemaining) {
                        available++;
                    }

                    marcarYCrearExtent(bg, freeIdx, available, logicalOffsetCurrent, allocations);
                    blocksRemaining -= available;
                    logicalOffsetCurrent += available;
                }
            }
        }

        if (blocksRemaining > 0) {
            throw new RuntimeException("DISK FULL: No se pudo asignar espacio para el archivo.");
        }

        return allocations;
    }

    private void marcarYCrearExtent(BlockGroup bg, int startRel, int length, int logicalOff, List<Extent> list) {
        for (int i = 0; i < length; i++) {
            bg.blockBitmap.set(startRel + i);
        }
        int physicalStart = bg.getPhysicalBlock(startRel);
        list.add(new Extent(logicalOff, physicalStart, length));
    }
}
