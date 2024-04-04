package com.mvc.allarthropods.Filters;

import com.mvc.allarthropods.Config;
import com.seedfinding.mccore.rand.ChunkRand;
import com.seedfinding.mccore.util.pos.CPos;
import com.seedfinding.mccore.util.pos.RPos;
import util.mansionSim.Mansion;
import util.mansionSim.MansionGenerator;
import util.mansionSim.MansionPiece;

public class StructureFilter {
    private final long structureSeed;
    private final ChunkRand chunkRand;
    public Mansion wm = new Mansion(Config.VERSION);
    public MansionGenerator wmGenerator = new MansionGenerator(Config.VERSION);

    public StructureFilter(long structureSeed, ChunkRand chunkRand) {
        this.structureSeed = structureSeed;
        this.chunkRand = chunkRand;
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

                if ((wmLocation == null) || (wmLocation.getMagnitudeSq() >= Config.WM_MAX_DIST)) {
                    continue;
                }
                wmGenerator.fastGenerate(structureSeed, wmLocation.getX(), wmLocation.getZ(), chunkRand);

                for (MansionPiece wmPiece : wmGenerator.getPieces()) {
                    if (wmPiece.getTemplate().contains("1x2_s2")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}