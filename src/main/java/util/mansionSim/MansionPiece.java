package util.mansionSim;

import com.seedfinding.mccore.util.block.BlockMirror;
import com.seedfinding.mccore.util.block.BlockRotation;
import com.seedfinding.mccore.util.pos.BPos;

public class MansionPiece {
    private BPos pos;
    private final String template;
    private final BlockRotation rotation;
    private final BlockMirror mirror;
    private int floorNumber;

    public MansionPiece(String template, BPos bPos, BlockRotation rotation, BlockMirror mirror, int floorNumber) {
        this.template = template;
        this.pos = bPos;
        this.rotation = rotation;
        this.mirror = mirror;
        this.floorNumber = floorNumber;
    }

    public static BPos getZeroPositionWithTransform(BPos bPos, BlockMirror mirror, BlockRotation rotation, int pivotX, int pivotZ) {
        --pivotX;
        --pivotZ;
        int transformX = mirror == BlockMirror.FRONT_BACK ? pivotX : 0;
        int transformZ = mirror == BlockMirror.LEFT_RIGHT ? pivotZ : 0;
        BPos transformBPos = bPos;
        switch (rotation) {
            case COUNTERCLOCKWISE_90:
                transformBPos = bPos.add(transformZ, 0, pivotX - transformX);
                break;
            case CLOCKWISE_90:
                transformBPos = bPos.add(pivotZ - transformZ, 0, transformX);
                break;
            case CLOCKWISE_180:
                transformBPos = bPos.add(pivotX - transformX, 0, pivotZ - transformZ);
                break;
            case NONE:
                transformBPos = bPos.add(transformX, 0, transformZ);
        }

        return transformBPos;
    }

    public BPos getPos() {
        return pos;
    }

    public String getTemplate() {
        return template;
    }

    public BlockRotation getRotation() {
        return rotation;
    }

    public BlockMirror getMirror() {
        return mirror;
    }

    public int getFloorNumber() {
        return floorNumber;
    }
}