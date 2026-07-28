package destiny.null_ouroboros.server.camouflage;

import destiny.null_ouroboros.server.registry.CapabilityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public final class Camouflage {
    public static final BooleanProperty CAMOUFLAGED = BooleanProperty.create("camouflaged");
    public static final String NBT_KEY = "Camouflage";

    private Camouflage() {
    }

    public static boolean isFullCube(BlockGetter level, BlockPos pos, BlockState state) {
        return !state.isAir()
                && state.getFluidState().isEmpty()
                && Block.isShapeFullBlock(state.getCollisionShape(level, pos));
    }

    public static boolean isFullCube(BlockState state) {
        return isFullCube(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, state);
    }

    public static boolean canApply(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return false;
        }
        Block block = blockItem.getBlock();
        if (block instanceof EntityBlock) {
            return false;
        }
        BlockState state = block.defaultBlockState();
        return canApply(state);
    }

    public static boolean canApply(BlockState state) {
        return isFullCube(state);
    }

    public static VoxelShape shapeOrCamouflage(VoxelShape intrinsic, BlockGetter level, BlockPos pos) {
        if (isSealingCamouflage(level, pos)) {
            return Shapes.block();
        }
        return intrinsic;
    }

    public static boolean isSealingCamouflage(BlockGetter level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof Camouflageable camouflageable)) {
            return false;
        }
        BlockState camouflage = camouflageable.getCamouflage();
        return camouflage != null && isFullCube(camouflage);
    }

    public static InteractionResult tryApply(Level level, BlockPos pos, Player player, ItemStack stack) {
        if (!canApply(stack)) {
            return InteractionResult.PASS;
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof Camouflageable camouflageable)) {
            return InteractionResult.PASS;
        }
        if (camouflageable.hasCamouflage()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockState camouflage = ((BlockItem) stack.getItem()).getBlock().defaultBlockState();
        camouflageable.setCamouflage(camouflage);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        level.playSound(null, pos, camouflage.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0F, 0.8F);
        notifyAtmosphere(level, pos);
        return InteractionResult.CONSUME;
    }

    public static InteractionResult tryRemove(Level level, BlockPos pos, Player player) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof Camouflageable camouflageable) || !camouflageable.hasCamouflage()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockState camouflage = camouflageable.getCamouflage();
        camouflageable.clearCamouflage();
        ItemStack drop = new ItemStack(camouflage.getBlock());
        if (!player.getInventory().add(drop)) {
            player.drop(drop, false);
        }
        level.playSound(null, pos, camouflage.getSoundType().getBreakSound(), SoundSource.BLOCKS, 1.0F, 0.8F);
        notifyAtmosphere(level, pos);
        return InteractionResult.CONSUME;
    }

    public static void dropCamouflage(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof Camouflageable camouflageable) || !camouflageable.hasCamouflage()) {
            return;
        }
        Block.popResource(level, pos, new ItemStack(camouflageable.getCamouflage().getBlock()));
    }

    public static void writeNbt(CompoundTag tag, @Nullable BlockState camouflage) {
        tag.putBoolean("HasCamouflage", camouflage != null);
        if (camouflage != null) {
            tag.put(NBT_KEY, NbtUtils.writeBlockState(camouflage));
        }
    }

    @Nullable
    public static BlockState readNbt(CompoundTag tag) {
        if (tag.contains("HasCamouflage")) {
            if (!tag.getBoolean("HasCamouflage") || !tag.contains(NBT_KEY)) {
                return null;
            }
            return NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound(NBT_KEY));
        }
        if (!tag.contains(NBT_KEY)) {
            return null;
        }
        return NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound(NBT_KEY));
    }

    public static void syncCamouflagedProperty(Level level, BlockPos pos, BlockState state, boolean camouflaged) {
        if (!state.hasProperty(CAMOUFLAGED) || state.getValue(CAMOUFLAGED) == camouflaged) {
            return;
        }
        level.setBlock(pos, state.setValue(CAMOUFLAGED, camouflaged), Block.UPDATE_ALL);
    }

    private static void notifyAtmosphere(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        serverLevel.getCapability(CapabilityRegistry.ASH_ATMOSPHERE_CAPABILITY).ifPresent(ash ->
                ash.seedAshAtBreach(serverLevel, pos));
    }
}
