package com.mvc.allarthropods.Filters;

import com.mvc.allarthropods.Config;
import com.seedfinding.mccore.rand.ChunkRand;
import com.seedfinding.mccore.util.pos.CPos;
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
        CPos wmLocation = wm.getInRegion(structureSeed, 0, 0, chunkRand);

        if (wmLocation.getX() > 2 || wmLocation.getZ() > 2) {
            return false;
        }

        wmGenerator.fastGenerate(structureSeed, wmLocation.getX(), wmLocation.getZ(), chunkRand);


        for (MansionPiece wmPiece : wmGenerator.getPieces()) {
            if (wmPiece.getTemplate().contains("1x2_s2")) {
                return true;
            }
        }
        return false;
    }
}