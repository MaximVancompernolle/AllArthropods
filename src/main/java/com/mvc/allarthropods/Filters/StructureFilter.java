package com.mvc.allarthropods.Filters;

import com.mvc.allarthropods.Config;
import com.seedfinding.mccore.rand.ChunkRand;
import com.seedfinding.mccore.state.Dimension;
import com.seedfinding.mccore.util.math.DistanceMetric;
import com.seedfinding.mccore.util.pos.CPos;
import com.seedfinding.mcfeature.loot.LootContext;
import com.seedfinding.mcfeature.loot.LootTable;
import com.seedfinding.mcfeature.loot.MCLootTables;
import com.seedfinding.mcfeature.loot.item.Item;
import com.seedfinding.mcfeature.loot.item.ItemStack;
import com.seedfinding.mcfeature.structure.RuinedPortal;
import kludwisz.mineshafts.Corridor;
import kludwisz.mineshafts.MineshaftGenerator;
import util.mansionSim.Mansion;
import util.mansionSim.MansionGenerator;
import util.mansionSim.MansionPiece;

import java.util.ArrayList;
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
        chunkRand.setCarverSeed(structureSeed, rpLocation.getX(), rpLocation.getZ(), Config.VERSION);

        if (chunkRand.nextFloat() < 0.50F) {
            return false;
        }

        if (rpLocation.getMagnitudeSq() > Config.RP_MAX_DIST) {
            return false;
        }

        Mansion wm = new Mansion(Config.VERSION);
        CPos wmLocation = wm.getInRegion(structureSeed, 0, 0, chunkRand);

        MansionGenerator wmGenerator = new MansionGenerator(Config.VERSION);
        wmGenerator.fastGenerate(structureSeed, wmLocation.getX(), wmLocation.getZ(), chunkRand);
        boolean hasFakeEndPortalRoom = false;
        CPos fakeEndPortalRoomPos = null;

        for (MansionPiece wmPiece : wmGenerator.getPieces()) {
            if (!wmPiece.getTemplate().contains("1x2_s2")) {
                continue;
            }
            fakeEndPortalRoomPos = wmPiece.getPos().toChunkPos();

            if (fakeEndPortalRoomPos.distanceTo(rpLocation, DistanceMetric.EUCLIDEAN_SQ) < Config.WM_MAX_DIST) {
                hasFakeEndPortalRoom = true;
                break;
            }
        }

        if (!hasFakeEndPortalRoom) {
            return false;
        }

        ArrayList<Corridor> corridorArrayList;
        boolean hasSpawner = false;

        mineshaftLoop:
        for (int x = -8; x < 8; x++) {
            for (int z = -8; z < 8; z++) {
                corridorArrayList = new ArrayList<>();
                MineshaftGenerator.generateForChunk(structureSeed, fakeEndPortalRoomPos.getX() + x, fakeEndPortalRoomPos.getZ() + z, false, corridorArrayList);

                if (corridorArrayList.isEmpty()) {
                    continue;
                }
                for (Corridor corridor : corridorArrayList) {
                    if (!corridor.hasCobwebs) {
                        continue;
                    }

                    if (corridor.bb.minY < 20) {
                        continue;
                    }
                    CPos corridorPos = new CPos(corridor.bb.maxX >> 4, corridor.bb.maxZ >> 4);

                    if (corridorPos.distanceTo(fakeEndPortalRoomPos, DistanceMetric.EUCLIDEAN_SQ) < Config.MS_MAX_DIST) {
                        hasSpawner = true;
                        break mineshaftLoop;
                    }
                }
            }
        }
        return hasSpawner && hasRpLoot(rpLocation);
    }

    public boolean hasRpLoot(CPos rpLocation) {
        chunkRand.setDecoratorSeed(structureSeed, rpLocation.getX() << 4, rpLocation.getZ() << 4, 40005, Config.VERSION);
        LootContext lootContext = new LootContext(chunkRand.nextLong(), Config.VERSION);
        LootTable lootTable = MCLootTables.RUINED_PORTAL_CHEST.get();
        lootTable.apply(Config.VERSION);
        List<ItemStack> items = lootTable.generate(lootContext);

        if (items.isEmpty()) {
            return false;
        }
        boolean hasPickaxe = false;
        boolean hasAxe = false;

        for (ItemStack stack : items) {
            Item item = stack.getItem();

            if (!hasPickaxe && item.getName().equals("golden_pickaxe")) {
                hasPickaxe = true;
                continue;
            }
            if (!hasAxe && item.getName().equals("golden_axe")) {
                hasAxe = true;
            }
        }
        return hasPickaxe && hasAxe;
    }
}