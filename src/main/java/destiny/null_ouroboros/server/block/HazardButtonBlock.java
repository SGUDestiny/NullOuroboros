package destiny.null_ouroboros.server.block;

import destiny.null_ouroboros.server.registry.SoundRegistry;
import destiny.null_ouroboros.server.util.ModUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class HazardButtonBlock extends ButtonBlock {
    public static final BlockSetType HAZARD_BLOCKSET = new BlockSetType("hazard", true, SoundType.LANTERN, SoundEvents.IRON_DOOR_CLOSE, SoundEvents.IRON_DOOR_OPEN, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundEvents.IRON_TRAPDOOR_OPEN, SoundEvents.STONE_PRESSURE_PLATE_CLICK_OFF, SoundEvents.STONE_PRESSURE_PLATE_CLICK_ON, SoundEvents.STONE_BUTTON_CLICK_OFF, SoundEvents.STONE_BUTTON_CLICK_ON);

    public static final VoxelShape CEILING_AABB = ModUtil.buildShape(
            Block.box(3, 13, 3, 13, 16, 13),
            Block.box(4, 11, 4, 12, 13, 12)
    );
    public static final VoxelShape FLOOR_AABB = ModUtil.buildShape(
            Block.box(3, 0, 3, 13, 3, 13),
            Block.box(4, 3, 4, 12, 5, 12)
    );
    public static final VoxelShape NORTH_AABB = ModUtil.buildShape(
            Block.box(3, 3, 13, 13, 13, 16),
            Block.box(4, 4, 11, 12, 12, 13)
    );
    public static final VoxelShape SOUTH_AABB = ModUtil.buildShape(
            Block.box(3, 3, 0, 13, 13, 3),
            Block.box(4, 4, 3, 12, 12, 5)
    );
    public static final VoxelShape WEST_AABB = ModUtil.buildShape(
            Block.box(13, 3, 3, 16, 13, 13),
            Block.box(11, 4, 4, 13, 12, 12)
    );
    public static final VoxelShape EAST_AABB = ModUtil.buildShape(
            Block.box(0, 3, 3, 3, 13, 13),
            Block.box(3, 4, 4, 5, 12, 12)
    );
    public static final VoxelShape PRESSED_CEILING_AABB = ModUtil.buildShape(
            Block.box(3, 13, 3, 13, 16, 13),
            Block.box(4, 12, 4, 12, 14, 12)
    );
    public static final VoxelShape PRESSED_FLOOR_AABB = ModUtil.buildShape(
            Block.box(3, 0, 3, 13, 3, 13),
            Block.box(4, 2, 4, 12, 4, 12)
    );
    public static final VoxelShape PRESSED_NORTH_AABB = ModUtil.buildShape(
            Block.box(3, 3, 13, 13, 13, 16),
            Block.box(4, 4, 12, 12, 12, 14)
    );
    public static final VoxelShape PRESSED_SOUTH_AABB = ModUtil.buildShape(
            Block.box(3, 3, 0, 13, 13, 3),
            Block.box(4, 4, 2, 12, 12, 4)
    );
    public static final VoxelShape PRESSED_WEST_AABB = ModUtil.buildShape(
            Block.box(13, 3, 3, 16, 13, 13),
            Block.box(12, 4, 4, 14, 12, 12)
    );
    public static final VoxelShape PRESSED_EAST_AABB = ModUtil.buildShape(
            Block.box(0, 3, 3, 3, 13, 13),
            Block.box(2, 4, 4, 4, 12, 12)
    );

    public HazardButtonBlock(Properties pProperties, int pTicksToStayPressed, boolean pArrowsCanPress) {
        super(pProperties, HAZARD_BLOCKSET, pTicksToStayPressed, pArrowsCanPress);
    }

    protected SoundEvent getSound(boolean pIsOn) {
        return pIsOn ? SoundRegistry.HAZARD_BUTTON_PRESS.get() : SoundRegistry.HAZARD_BUTTON_RELEASE.get();
    }

    public VoxelShape getShape(BlockState state, BlockGetter getter, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        boolean powered = state.getValue(POWERED);
        switch (state.getValue(FACE)) {
            case FLOOR:
                if (facing.getAxis() == Direction.Axis.X) {
                    return powered ? PRESSED_FLOOR_AABB : FLOOR_AABB;
                }

                return powered ? PRESSED_FLOOR_AABB : FLOOR_AABB;
            case WALL:
                VoxelShape shape;
                switch (facing) {
                    case EAST:
                        shape = powered ? PRESSED_EAST_AABB : EAST_AABB;
                        break;
                    case WEST:
                        shape = powered ? PRESSED_WEST_AABB : WEST_AABB;
                        break;
                    case SOUTH:
                        shape = powered ? PRESSED_SOUTH_AABB : SOUTH_AABB;
                        break;
                    case NORTH:
                    case UP:
                    case DOWN:
                        shape = powered ? PRESSED_NORTH_AABB : NORTH_AABB;
                        break;
                    default:
                        throw new IncompatibleClassChangeError();
                }

                return shape;
            case CEILING:
            default:
                return powered ? PRESSED_CEILING_AABB : CEILING_AABB;
        }
    }
}
