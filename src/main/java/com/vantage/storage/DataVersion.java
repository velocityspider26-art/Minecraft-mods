package com.vantage.storage;

import com.vantage.Vantage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Version of the on-disk LOD data of one dimension. Data written by an incompatible version is
 * deleted (it is only a cache and is rebuilt from the world).
 */
public final class DataVersion {
    /** 2: voxels carry an "unknown" flag, levels go up to 10. */
    public static final int CURRENT = 2;
    private static final String FILE = "format.txt";

    private DataVersion() {
    }

    public static void prepare(Path dir) {
        Path marker = dir.resolve(FILE);
        try {
            if (Files.isDirectory(dir) && !String.valueOf(CURRENT).equals(read(marker))) {
                Vantage.LOGGER.info("Rebuilding LOD data in {} (format changed)", dir);
                List<Path> paths;
                try (Stream<Path> walk = Files.walk(dir)) {
                    paths = walk.sorted(Comparator.reverseOrder()).filter(p -> !p.equals(dir)).toList();
                }
                for (Path p : paths) {
                    Files.deleteIfExists(p);
                }
            }
            Files.createDirectories(dir);
            Files.writeString(marker, String.valueOf(CURRENT), StandardCharsets.UTF_8);
        } catch (IOException e) {
            Vantage.LOGGER.warn("Could not prepare LOD data folder {}: {}", dir, e.toString());
        }
    }

    private static String read(Path marker) throws IOException {
        return Files.exists(marker) ? Files.readString(marker, StandardCharsets.UTF_8).trim() : null;
    }
}
