package com.mvc.allarthropods;

import com.mvc.allarthropods.Filters.StructureFilter;
import com.seedfinding.mccore.rand.ChunkRand;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import nl.jellejurre.seedchecker.SeedChecker;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("Starting seed finding...");
        FileWriter output = new FileWriter("./output.txt");
        ExecutorService customThreadPool = new ThreadPoolExecutor(Config.THREADS, Config.THREADS, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(Config.THREADS * 10), new ThreadPoolExecutor.CallerRunsPolicy());
        int seedsChecked = 0;
        int seedMatches = 0;
        long nextTime = 0;
        long currentTime;

        while (seedMatches < Config.SEED_MATCHES) {
            ThreadOperation thread = new ThreadOperation();
            customThreadPool.submit(thread);
            Long matchedStructureSeed = thread.getMatchedStructureSeed();

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
        customThreadPool.shutdown();

        if (!customThreadPool.awaitTermination(60, TimeUnit.SECONDS)) {
            System.out.println("Thread pool termination timed out.");
        }
        output.close();
        System.out.printf("%,d seeds checked with %,d matches.\r", seedsChecked, seedMatches);
    }
    public static boolean filterStructureSeed(long structureSeed, ChunkRand chunkRand) {
        StructureFilter structureFilter = new StructureFilter(structureSeed, chunkRand);

        return structureFilter.filterStructures();
    }
}

class ThreadOperation implements Runnable {
    private static final Random random = new Random();
    private static final ChunkRand chunkRand = new ChunkRand();
    private Long matchedStructureSeed;

    @Override
    public void run() {
        long structureSeed = random.nextLong() % (1L << 48);
        matchedStructureSeed = Main.filterStructureSeed(structureSeed, chunkRand) ? structureSeed : null;
    }

    public Long getMatchedStructureSeed() {
        return matchedStructureSeed;
    }
}