package destiny.null_ouroboros.server.structure;

import destiny.null_ouroboros.server.block.BulkheadBlock;
import destiny.null_ouroboros.server.block.BulkheadPart;
import destiny.null_ouroboros.server.block.GarageDoorBlock;
import destiny.null_ouroboros.server.block.GarageDoorPart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class DoorMultiblockStructure {
    private DoorMultiblockStructure() {
    }

    public static void ensureIfController(LevelAccessor level, BlockPos pos, BlockState state) {
        Block block = state.getBlock();
        if (block instanceof BulkheadBlock && state.getValue(BulkheadBlock.PART).isController()) {
            ensureBulkheadSiblings(level, pos, state, Block.UPDATE_CLIENTS);
        } else if (block instanceof GarageDoorBlock && state.getValue(GarageDoorBlock.PART).isController()) {
            ensureGarageDoorSiblings(level, pos, state, Block.UPDATE_CLIENTS);
        }
    }

    public static void ensureBulkheadSiblings(LevelAccessor level, BlockPos controller, BlockState state, int flags) {
        Direction facing = state.getValue(BulkheadBlock.FACING);
        for (BulkheadPart part : BulkheadPart.values()) {
            if (part.isController()) {
                continue;
            }
            BlockPos partPos = BulkheadBlock.partPos(controller, facing, part);
            BlockState desired = state.setValue(BulkheadBlock.PART, part);
            if (!level.getBlockState(partPos).equals(desired)) {
                level.setBlock(partPos, desired, flags);
            }
        }
    }

    public static void ensureGarageDoorSiblings(LevelAccessor level, BlockPos controller, BlockState state, int flags) {
        Direction facing = state.getValue(GarageDoorBlock.FACING);
        for (GarageDoorPart part : GarageDoorPart.values()) {
            if (part.isController()) {
                continue;
            }
            BlockPos partPos = GarageDoorBlock.partPos(controller, facing, part);
            BlockState desired = state.setValue(GarageDoorBlock.PART, part);
            if (!level.getBlockState(partPos).equals(desired)) {
                level.setBlock(partPos, desired, flags);
            }
        }
    }
}
