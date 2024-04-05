package com.mvc.allarthropods.Filters;

import com.mvc.allarthropods.Config;
import com.seedfinding.mcbiome.source.OverworldBiomeSource;
import com.seedfinding.mccore.rand.ChunkRand;
import com.seedfinding.mccore.util.pos.CPos;
import com.seedfinding.mcterrain.terrain.OverworldTerrainGenerator;
import util.mansionSim.Mansion;
import util.mansionSim.MansionGenerator;
import util.mansionSim.MansionPiece;

public class StructureFilter {
    private final long structureSeed;
    private final ChunkRand chunkRand;
    public Mansion wm = new Mansion(Config.VERSION);
    public MansionGenerator wmGenerator = new MansionGenerator(Config.VERSION);
    public OverworldBiomeSource owBiomeSource;
    public OverworldTerrainGenerator owTerrainGen;

    public StructureFilter(long structureSeed, ChunkRand chunkRand) {
        this.structureSeed = structureSeed;
        this.chunkRand = chunkRand;
        this.owBiomeSource = new OverworldBiomeSource(Config.VERSION, structureSeed);
        this.owTerrainGen = new OverworldTerrainGenerator(owBiomeSource);
    }

    public boolean filterStructures() {
        CPos wmLocation = wm.getInRegion(structureSeed, 0, 0, chunkRand);

        if (wmLocation.getX() > Config.WM_MAX_DIST || wmLocation.getZ() > Config.WM_MAX_DIST) {
            return false;
        }

        wmGenerator.fastGenerate(structureSeed, wmLocation.getX(), wmLocation.getZ(), chunkRand);
        boolean hasFakeEndPortalRoom = false;

        for (MansionPiece wmPiece : wmGenerator.getPieces()) {
            if (wmPiece.getTemplate().contains("1x2_s2")) {
                hasFakeEndPortalRoom = true;
                break;
            }
        }
        return hasFakeEndPortalRoom;
    }
}