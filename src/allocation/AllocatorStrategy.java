package allocation;

import model.BlockGroup;
import model.Extent;

import java.util.List;

public interface AllocatorStrategy {
    /**
     * Determina qué bloques físicos usar para almacenar N bloques lógicos.
     * Retorna una lista de Extents (puede haber fragmentación).
     */
    List<Extent> allocate(List<BlockGroup> groups, int blocksNeeded);
}
