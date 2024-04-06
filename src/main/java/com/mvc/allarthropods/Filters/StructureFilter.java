package com.mvc.allarthropods.Filters;

import com.mvc.allarthropods.Config;
import com.seedfinding.mccore.rand.ChunkRand;
import com.seedfinding.mccore.state.Dimension;
import com.seedfinding.mccore.util.math.DistanceMetric;
import com.seedfinding.mccore.util.pos.CPos;
import com.seedfinding.mcfeature.loot.ChestContent;
import com.seedfinding.mcfeature.loot.item.ItemStack;
import com.seedfinding.mcfeature.loot.item.Items;
import com.seedfinding.mcfeature.structure.RuinedPortal;
import com.seedfinding.mcfeature.structure.generator.structure.RuinedPortalGenerator;
import util.mansionSim.Mansion;
import util.mansionSim.MansionGenerator;
import util.mansionSim.MansionPiece;

import java.util.List;

public class StructureFilter {
    private final long structureSeed;
    private final ChunkRand chunkRand;

    public StructureFilter(long structureSeed, ChunkRand chunkRand) {
        this.structureSeed = structureSeed;
        this.chunkRand = chunkRand;
    }

    public boolean filterStructures() {
        RuinedPortal rp = new RuinedPortal(Dimension.OVERWORLD, Config.VERSION);
        CPos rpLocation = rp.getInRegion(structureSeed, 0, 0, chunkRand);

        if (rpLocation.getMagnitudeSq() > Config.RP_MAX_DIST) {
            return false;
        }

        if (!hasRpLoot(rpLocation, rp)) {
            return false;
        }

        Mansion wm = new Mansion(Config.VERSION);
        CPos wmLocation = wm.getInRegion(structureSeed, 0, 0, chunkRand);

        if (wmLocation.distanceTo(rpLocation, DistanceMetric.EUCLIDEAN_SQ) > Config.WM_MAX_DIST) {
            return false;
        }

        MansionGenerator wmGenerator = new MansionGenerator(Config.VERSION);
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

    public boolean hasRpLoot(CPos rpLocation, RuinedPortal rp) {
        RuinedPortalGenerator rpGenerator = new RuinedPortalGenerator(Config.VERSION);
        if(!rpGenerator.generate(structureSeed, Dimension.OVERWORLD, rpLocation.getX(), rpLocation.getZ())) {
            return false;
        }
        List<ChestContent> loot = rp.getLoot(structureSeed, rpGenerator, chunkRand, false);

        if (loot.isEmpty()) {
            return false;
        }

        for (ChestContent chest : loot) {
            if (!chest.contains(Items.GOLDEN_AXE)) {
                return false;
            }

            for (ItemStack stack : chest.getItems()) {
                if (!stack.getItem().equals(Items.GOLDEN_AXE)) {
                    continue;
                }
                return true;
            }
        }
        return false;
    }
}