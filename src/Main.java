import allocation.SmartFitAllocator;
import filesystem.Ext4FileSystem;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        // 1. Configuración: Disco de 10 MB, Bloques de 4KB
        int totalSize = 10 * 1024 * 1024;
        int blockSize = 4096;

        System.out.println(">>> INICIANDO SIMULADOR EXT4 <<<");

        // Inyección de dependencia (Strategy)
        Ext4FileSystem fs = new Ext4FileSystem(totalSize, blockSize, new SmartFitAllocator());

        // 2. Prueba de Estrés (100 Operaciones)
        Random random = new Random();
        List<String> activeFiles = new ArrayList<>();

        System.out.println("\n>>> INICIANDO PRUEBA DE ESTRÉS (100 OPS) <<<");
        long start = System.currentTimeMillis();

        for (int i = 0; i < 100; i++) {
            int op = random.nextInt(10); // 0-5: Crear, 6-8: Leer, 9: Borrar

            try {
                if (op < 6) { // CREAR (60% prob)
                    String name = "file_" + i + ".txt";
                    // Tamaños aleatorios: desde 1KB hasta 200KB (para forzar multi-bloques)
                    int size = 1024 + random.nextInt(200 * 1024);
                    String content = "X".repeat(size); // Contenido dummy

                    fs.createFile(name, content);
                    activeFiles.add(name);
                    System.out.println("[OP " + i + "] CREAR " + name + " (" + size/1024 + "KB) -> " + fs.getUsageStatus());

                } else if (op < 9) { // LEER (30% prob)
                    if (!activeFiles.isEmpty()) {
                        String name = activeFiles.get(random.nextInt(activeFiles.size()));
                        fs.readFile(name);
                        System.out.println("[OP " + i + "] LEER  " + name);
                    }
                } else { // BORRAR (10% prob)
                    if (!activeFiles.isEmpty()) {
                        String name = activeFiles.get(random.nextInt(activeFiles.size()));
                        fs.deleteFile(name);
                        activeFiles.remove(name);
                        System.out.println("[OP " + i + "] BORRAR " + name + " -> " + fs.getUsageStatus());
                    }
                }
            } catch (Exception e) {
                System.err.println("\nError en op " + i + ": " + e.getMessage());
            }
        }

        long end = System.currentTimeMillis();
        System.out.println("\n\n>>> PRUEBA TERMINADA en " + (end - start) + "ms <<<");

        // 3. Verificación final
        fs.printStats();

        // Prueba de consistencia: Crear un archivo final para ver fragmentación
        System.out.println("\n>>> PRUEBA DE FRAGMENTACIÓN <<<");
        try {
            fs.createFile("final_big_file.dat", "F".repeat(1024 * 1024)); // 1MB
            System.out.println("Archivo grande final creado exitosamente.");
        } catch (Exception e) {
            System.out.println("Disco lleno al final.");
        }
        fs.printStats();
    }
}