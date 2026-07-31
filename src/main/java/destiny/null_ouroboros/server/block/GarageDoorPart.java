package destiny.null_ouroboros.server.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;

public enum GarageDoorPart implements StringRepresentable {
    L2_BOT(-2, 0, "l2_bot"),
    L1_BOT(-1, 0, "l1_bot"),
    CENTER_BOT(0, 0, "center_bot"),
    R1_BOT(1, 0, "r1_bot"),
    R2_BOT(2, 0, "r2_bot"),
    L2_MID(-2, 1, "l2_mid"),
    L1_MID(-1, 1, "l1_mid"),
    CENTER_MID(0, 1, "center_mid"),
    R1_MID(1, 1, "r1_mid"),
    R2_MID(2, 1, "r2_mid"),
    L2_TOP(-2, 2, "l2_top"),
    L1_TOP(-1, 2, "l1_top"),
    CENTER_TOP(0, 2, "center_top"),
    R1_TOP(1, 2, "r1_top"),
    R2_TOP(2, 2, "r2_top");

    private final int lateral;
    private final int up;
    private final String name;

    GarageDoorPart(int lateral, int up, String name) {
        this.lateral = lateral;
        this.up = up;
        this.name = name;
    }

    public BlockPos offset(Direction facing) {
        BlockPos pos = BlockPos.ZERO.above(up);
        if (lateral == 0) {
            return pos;
        }
        Direction side = lateral < 0 ? facing.getCounterClockWise() : facing.getClockWise();
        return pos.relative(side, Math.abs(lateral));
    }

    public boolean isController() {
        return this == CENTER_BOT;
    }

    public int row() {
        return up;
    }

    public boolean isBottomRow() {
        return up == 0;
    }

    public boolean isMiddleRow() {
        return up == 1;
    }

    public boolean isTopRow() {
        return up == 2;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
