package com.mvc.allarthropods;

import com.mvc.allarthropods.Filters.StructureFilter;
import com.seedfinding.mccore.rand.ChunkRand;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

public class Main {
    public static void main(String[] args) throws IOException {
        final Random random = new Random();
        final ChunkRand chunkRand = new ChunkRand();
        System.out.println("Starting seed finding...");
        FileWriter output = new FileWriter("./output.txt");
        long seedsChecked = 0;
        int seedMatches = 0;
        long nextTime = 0;
        long currentTime;

        while (seedMatches < Config.SEED_MATCHES) {
            long structureSeed = random.nextLong() % (1L << 48);
            Long matchedStructureSeed = filterStructureSeed(structureSeed, chunkRand) ? structureSeed : null;

            if (matchedStructureSeed != null) {
                output.write(matchedStructureSeed + "\n");
                seedMatches++;
            }
            seedsChecked++;

            currentTime = System.currentTimeMillis();
            if (currentTime > nextTime) {
                System.out.printf("%,d seeds checked with %,d matches\r", seedsChecked, seedMatches);
                nextTime = currentTime + Config.LOG_DELAY;
            }
        }
        output.close();
        System.out.printf("%,d seeds checked with %,d matches.\r", seedsChecked, seedMatches);
    }
    public static boolean filterStructureSeed(long structureSeed, ChunkRand chunkRand) {
        StructureFilter structureFilter = new StructureFilter(structureSeed, chunkRand);

        return structureFilter.filterStructures();
    }
}