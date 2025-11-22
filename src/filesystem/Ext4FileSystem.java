package filesystem;

import allocation.AllocatorStrategy;
import hardware.VirtualDisk;
import model.BlockGroup;
import model.Extent;
import model.Inode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public class Ext4FileSystem {
    private final List<BlockGroup> blockGroups;
    private final Map<String, Inode> directory; // Nombre -> Inode
    private final AllocatorStrategy allocator;
    private final int blockSize;

    public Ext4FileSystem(int totalSize, int blockSize, AllocatorStrategy allocator) {
        this.allocator = allocator;
        this.directory = new HashMap<>();
        this.blockSize = blockSize;
        this.blockGroups = new ArrayList<>();

        // Inicializar Disco Físico
        int totalBlocks = totalSize / blockSize;
        VirtualDisk.INSTANCE.init(totalBlocks, blockSize);

        // Crear Block Groups (Simulamos particionar en 4 grupos)
        int groupsCount = 4;
        int blocksPerGroup = totalBlocks / groupsCount;

        for (int i = 0; i < groupsCount; i++) {
            int start = i * blocksPerGroup;
            // 1024 inodos por grupo arbitrario
            blockGroups.add(new BlockGroup(i, start, blocksPerGroup, 1024));
        }
        System.out.println("[FS] Formateado con " + groupsCount + " Block Groups.");
    }

    // --- Operación de Localización (Allocation) ---
    public void createFile(String name, String content) {
        byte[] data = content.getBytes();
        int size = data.length;
        int blocksNeeded = (int) Math.ceil((double) size / blockSize);

        // 1. Buscar Inodo libre (Round robin simple entre grupos)
        BlockGroup selectedGroup = null;
        int inodeIdRel = -1;
        for (BlockGroup bg : blockGroups) {
            inodeIdRel = bg.inodeBitmap.allocateFirstFree();
            if (inodeIdRel != -1) {
                selectedGroup = bg;
                break;
            }
        }
        if (selectedGroup == null) throw new RuntimeException("Max files reached (No inodes).");

        // Crear ID único global para el inodo (solo para referencia)
        int globalInodeId = (selectedGroup.id * 10000) + inodeIdRel;

        // 2. Allocation Strategy (Bitmaps & Extents)
        List<Extent> extents;
        try {
            extents = allocator.allocate(blockGroups, blocksNeeded);
        } catch (RuntimeException e) {
            // Rollback inodo si falla disco
            selectedGroup.inodeBitmap.clear(inodeIdRel);
            throw e;
        }

        // 3. Escribir datos en VirtualDisk (Simulación)
        int bytesWritten = 0;
        for (Extent ext : extents) {
            for (int i = 0; i < ext.length(); i++) {
                int physicalBlock = ext.physicalBlock() + i;
                int offset = bytesWritten;
                int lengthToWrite = Math.min(blockSize, size - bytesWritten);

                byte[] chunk = new byte[blockSize]; // Buffer lleno de ceros
                if (lengthToWrite > 0) {
                    System.arraycopy(data, offset, chunk, 0, lengthToWrite);
                }
                VirtualDisk.INSTANCE.writeBlock(physicalBlock, chunk);
                bytesWritten += lengthToWrite;
            }
        }

        // 4. Guardar Inodo
        Inode inode = new Inode(globalInodeId, size);
        inode.extents.addAll(extents);
        directory.put(name, inode);

        // System.out.println("Archivo creado: " + name + " | Extents: " + extents.size());
    }

    // --- Lectura ---
    public String readFile(String name) {
        Inode inode = directory.get(name);
        if (inode == null) return null;

        StringBuilder sb = new StringBuilder();
        for (Extent ext : inode.extents) {
            for (int i = 0; i < ext.length(); i++) {
                byte[] blockData = VirtualDisk.INSTANCE.readBlock(ext.physicalBlock() + i);
                // En una impl real controlariamos el EOF exacto por byte, aquí simplificamos
                sb.append(new String(blockData).trim());
            }
        }
        // Recortar al tamaño real exacto
        String raw = sb.toString();
        return raw.substring(0, Math.min(raw.length(), inode.sizeBytes));
    }

    // --- Operación de Deslocalización (Deallocation) ---
    public void deleteFile(String name) {
        Inode inode = directory.get(name);
        if (inode == null) return;

        // 1. Liberar Bloques en BlockBitmaps (Usando Extents)
        for (Extent ext : inode.extents) {
            // Determinar a qué grupo pertenece este rango físico
            for (BlockGroup bg : blockGroups) {
                // Verificar intersección simple
                if (ext.physicalBlock() >= bg.startBlock &&
                        ext.physicalBlock() < bg.startBlock + bg.totalBlocks) {

                    int relativeStart = ext.physicalBlock() - bg.startBlock;
                    for (int i = 0; i < ext.length(); i++) {
                        bg.blockBitmap.clear(relativeStart + i);
                    }
                }
            }
        }

        // 2. Liberar Inodo
        // (Simplificación: Requeriría mapeo inverso ID->Grupo, aquí asumimos búsqueda lineal o ID decodificado)
        int groupId = inode.id / 10000;
        int relativeInodeId = inode.id % 10000;
        if (groupId < blockGroups.size()) {
            blockGroups.get(groupId).inodeBitmap.clear(relativeInodeId);
        }

        // 3. Borrar de directorio
        directory.remove(name);
        // System.out.println("Archivo borrado: " + name);
    }

    public void printStats() {
        System.out.println("\n--- ESTADO DEL SISTEMA ---");
        System.out.println("Archivos: " + directory.size());
        for(BlockGroup bg : blockGroups) {
            // Calcular ocupación
            long used = IntStream.range(0, bg.totalBlocks).filter(bg.blockBitmap::isSet).count();
            System.out.printf("Group %d: Usados %d/%d bloques.\n", bg.id, used, bg.totalBlocks);
        }
    }

    public String getUsageStatus() {
        long totalUsed = 0;
        long totalCap = 0;
        for (BlockGroup bg : blockGroups) {
            // Contamos cuántos bits están en true
            long usedInGroup = IntStream.range(0, bg.totalBlocks)
                    .filter(bg.blockBitmap::isSet)
                    .count();
            totalUsed += usedInGroup;
            totalCap += bg.totalBlocks;
        }
        double percent = (double) totalUsed / totalCap * 100.0;
        return String.format("Uso Global: %.2f%%", percent);
    }
}
