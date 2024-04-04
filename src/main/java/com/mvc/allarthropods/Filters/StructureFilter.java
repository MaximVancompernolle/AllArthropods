package com.mvc.allarthropods.Filters;

import com.mvc.allarthropods.Config;
import com.seedfinding.mcbiome.source.OverworldBiomeSource;
import com.seedfinding.mccore.rand.ChunkRand;
import com.seedfinding.mccore.util.pos.CPos;
import com.seedfinding.mccore.util.pos.RPos;
import com.seedfinding.mcfeature.structure.Mansion;
import com.seedfinding.mcterrain.terrain.OverworldTerrainGenerator;

public class StructureFilter {
    private final long structureSeed;
    private final ChunkRand chunkRand;
    public Mansion wm = new Mansion(Config.VERSION);
    public OverworldBiomeSource owBiomeSource;
    public OverworldTerrainGenerator owTerrainGen;

    public StructureFilter(long structureSeed, ChunkRand chunkRand) {
        this.structureSeed = structureSeed;
        this.chunkRand = chunkRand;
        this.owBiomeSource = new OverworldBiomeSource(Config.VERSION, structureSeed);
        this.owTerrainGen = new OverworldTerrainGenerator(this.owBiomeSource);
    }

    public boolean filterStructures() {
        RPos[][] wmRegions = new RPos[2][2];

        for (int x = -1; x <= 0; x++) {
            for (int z = -1; z < 0; z++) {
                wmRegions[x + 1][z + 1] = new RPos(x, z, 80 << 4);
            }
        }

        for (RPos[] rowOfWmRegions : wmRegions) {
            for (RPos wmRegion : rowOfWmRegions) {
                CPos wmLocation = wm.getInRegion(structureSeed, wmRegion.getX(), wmRegion.getZ(), chunkRand);

                if (wmLocation == null) {
                    continue;
                }
                if (wmLocation.getMagnitudeSq() <= Config.WM_MAX_DIST) {
                    return true;
                }
            }
        }
        return false;
    }
}