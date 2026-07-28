package destiny.null_ouroboros.server.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;

public enum BulkheadPart implements StringRepresentable {
    LEFT_BOTTOM(-1, 0, "left_bottom"),
    CENTER_BOTTOM(0, 0, "center_bottom"),
    RIGHT_BOTTOM(1, 0, "right_bottom"),
    LEFT_MIDDLE(-1, 1, "left_middle"),
    CENTER_MIDDLE(0, 1, "center_middle"),
    RIGHT_MIDDLE(1, 1, "right_middle"),
    LEFT_TOP(-1, 2, "left_top"),
    CENTER_TOP(0, 2, "center_top"),
    RIGHT_TOP(1, 2, "right_top");

    private final int lateral;
    private final int up;
    private final String name;

    BulkheadPart(int lateral, int up, String name) {
        this.lateral = lateral;
        this.up = up;
        this.name = name;
    }

    public BlockPos offset(Direction facing) {
        BlockPos pos = BlockPos.ZERO.above(up);
        if (lateral < 0) {
            return pos.relative(facing.getCounterClockWise());
        }
        if (lateral > 0) {
            return pos.relative(facing.getClockWise());
        }
        return pos;
    }

    public boolean isController() {
        return this == CENTER_BOTTOM;
    }

    public boolean isCenterColumn() {
        return lateral == 0;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
