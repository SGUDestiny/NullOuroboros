package destiny.null_ouroboros.server.item;

import destiny.null_ouroboros.server.registry.BlockRegistry;
import destiny.null_ouroboros.server.registry.FluidRegistry;
import destiny.null_ouroboros.server.registry.ItemRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class JerrycanItem extends Item {
    public static final int CAPACITY_MB = 20000;
    public static final int BLOCK_MB = 1000;
    private static final String FUEL_TAG = "FuelMilliBuckets";

    public JerrycanItem(Properties properties) {
        super(properties);
    }

    public static ItemStack createFilled() {
        ItemStack stack = new ItemStack(ItemRegistry.JERRYCAN.get());
        setFuel(stack, CAPACITY_MB);
        return stack;
    }

    public static int getFuel(ItemStack stack) {
        return stack.getOrCreateTag().getInt(FUEL_TAG);
    }

    public static void setFuel(ItemStack stack, int amount) {
        stack.getOrCreateTag().putInt(FUEL_TAG, Math.max(0, Math.min(CAPACITY_MB, amount)));
    }

    public static int addFuel(ItemStack stack, int amount) {
        int accepted = Math.min(Math.max(0, amount), CAPACITY_MB - getFuel(stack));
        setFuel(stack, getFuel(stack) + accepted);
        return accepted;
    }

    public static int removeFuel(ItemStack stack, int amount) {
        int removed = Math.min(Math.max(0, amount), getFuel(stack));
        setFuel(stack, getFuel(stack) - removed);
        return removed;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult fluidHit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (fluidHit.getType() == HitResult.Type.BLOCK) {
            BlockPos fluidPos = fluidHit.getBlockPos();
            FluidState fluidState = level.getFluidState(fluidPos);
            if (fluidState.is(FluidRegistry.SOURCE_BLOOD.get()) && fluidState.isSource()
                    && CAPACITY_MB - getFuel(stack) >= BLOCK_MB) {
                if (!level.mayInteract(player, fluidPos)) {
                    return InteractionResultHolder.fail(stack);
                }
                if (!level.isClientSide) {
                    BlockState state = level.getBlockState(fluidPos);
                    if (state.getBlock() instanceof BucketPickup pickup) {
                        ItemStack taken = pickup.pickupBlock(level, fluidPos, state);
                        if (!taken.isEmpty()) {
                            addFuel(stack, BLOCK_MB);
                            player.awardStat(Stats.ITEM_USED.get(this));
                            level.playSound(null, fluidPos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                            level.gameEvent(player, GameEvent.FLUID_PICKUP, fluidPos);
                        }
                    } else {
                        level.setBlock(fluidPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 11);
                        addFuel(stack, BLOCK_MB);
                        player.awardStat(Stats.ITEM_USED.get(this));
                        level.playSound(null, fluidPos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                        level.gameEvent(player, GameEvent.FLUID_PICKUP, fluidPos);
                    }
                }
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
            }
        }

        if (getFuel(stack) < BLOCK_MB) {
            return InteractionResultHolder.pass(stack);
        }

        BlockHitResult blockHit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        if (blockHit.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(stack);
        }

        BlockPos hitPos = blockHit.getBlockPos();
        Direction face = blockHit.getDirection();
        BlockPos placePos = hitPos.relative(face);
        if (!level.mayInteract(player, hitPos) || !player.mayUseItemAt(placePos, face, stack)) {
            return InteractionResultHolder.fail(stack);
        }

        BlockState hitState = level.getBlockState(hitPos);
        BlockPos targetPos = canPlaceAt(level, hitPos) ? hitPos : placePos;
        if (!canPlaceAt(level, targetPos)) {
            return InteractionResultHolder.fail(stack);
        }

        if (!level.isClientSide) {
            BlockState targetState = level.getBlockState(targetPos);
            if (targetState.getBlock() instanceof LiquidBlockContainer container
                    && container.canPlaceLiquid(level, targetPos, targetState, FluidRegistry.SOURCE_BLOOD.get())) {
                container.placeLiquid(level, targetPos, targetState, FluidRegistry.SOURCE_BLOOD.get().getSource(false));
            } else {
                if (!targetState.isAir()) {
                    level.destroyBlock(targetPos, true);
                }
                level.setBlock(targetPos, BlockRegistry.BLOOD.get().defaultBlockState(), 11);
            }
            removeFuel(stack, BLOCK_MB);
            player.awardStat(Stats.ITEM_USED.get(this));
            level.playSound(null, targetPos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent(player, GameEvent.FLUID_PLACE, targetPos);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private static boolean canPlaceAt(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        FluidState fluidState = state.getFluidState();
        if (fluidState.is(FluidRegistry.SOURCE_BLOOD.get()) && fluidState.isSource()) {
            return false;
        }
        if (state.getBlock() instanceof LiquidBlockContainer container
                && container.canPlaceLiquid(level, pos, state, FluidRegistry.SOURCE_BLOOD.get())) {
            return true;
        }
        return state.canBeReplaced(FluidRegistry.SOURCE_BLOOD.get()) || state.canBeReplaced();
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * getFuel(stack) / CAPACITY_MB);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x8B1A1A;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Blood: " + getFuel(stack) + " mB").withStyle(ChatFormatting.GRAY));
    }
}
