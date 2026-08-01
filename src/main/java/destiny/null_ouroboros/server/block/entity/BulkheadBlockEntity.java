package destiny.null_ouroboros.server.block.entity;

import destiny.null_ouroboros.server.block.BulkheadBlock;
import destiny.null_ouroboros.server.block.BulkheadPart;
import destiny.null_ouroboros.server.registry.BlockEntityRegistry;
import destiny.null_ouroboros.server.registry.CapabilityRegistry;
import destiny.null_ouroboros.server.registry.DamageTypeRegistry;
import destiny.null_ouroboros.server.registry.SoundRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class BulkheadBlockEntity extends BlockEntity implements GeoBlockEntity {
    public static final int OPEN_MOVE_TICKS = 80;
    public static final int CLOSE_MOVE_TICKS = 95;

    private static final int PHASE_IDLE = 0;
    private static final int PHASE_MOVING = 1;

    private static final int OPEN_STAGE1_TICK = 40;
    private static final int CLOSE_STAGE1_TICK = 40;
    private static final int CLOSE_STAGE0_TICK = 60;
    private static final int DOOR_SOUND_TICK = 20;
    private static final int UNCLAMP_TICK_A = 10;
    private static final int UNCLAMP_TICK_B = 15;
    private static final int CLAMP_TICK_A = 90;
    private static final int CLAMP_TICK_B = 95;
    private static final int CRUSH_CLOSE_TICK = 4 * 20;
    private static final int CRUSH_WARNING_TICK = CRUSH_CLOSE_TICK - 10;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private int phase = PHASE_IDLE;
    private int phaseTicks;
    private float openProgress;
    private boolean opening = true;
    private boolean lastRedstone;
    private boolean redstonePrimed;
    private boolean snapToAnimEnd;
    private String currentAnimName = "";

    public BulkheadBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.BULKHEAD_BLOCK_ENTITY.get(), pos, state);
    }

    public boolean isCycling() {
        return phase != PHASE_IDLE;
    }

    public boolean isRedstonePrimed() {
        return redstonePrimed;
    }

    public boolean getLastRedstone() {
        return lastRedstone;
    }

    public void setLastRedstone(boolean lastRedstone) {
        this.lastRedstone = lastRedstone;
    }

    public boolean tryStartCycle() {
        if (level == null || level.isClientSide || isCycling()) {
            return false;
        }
        beginMove();
        return true;
    }

    private void beginMove() {
        opening = openProgress < 0.5f;
        snapToAnimEnd = false;
        currentAnimName = "";
        phase = PHASE_MOVING;
        phaseTicks = 0;
        playSound(SoundRegistry.BULKHEAD_BUZZER.get());
        setChangedAndSync();
    }

    private void finishMove() {
        openProgress = opening ? 1f : 0f;
        applyOpenStage(opening ? 2 : 0);
        phase = PHASE_IDLE;
        phaseTicks = 0;
        if (opening) {
            snapToAnimEnd = true;
        } else {
            snapToAnimEnd = false;
            currentAnimName = "";
        }

        if (level != null) {
            Direction facing = getBlockState().getValue(BulkheadBlock.FACING);
            lastRedstone = BulkheadBlock.isStructurePowered(level, worldPosition, facing);
        }
        setChangedAndSync();
    }

    private int moveTicks() {
        return opening ? OPEN_MOVE_TICKS : CLOSE_MOVE_TICKS;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BulkheadBlockEntity bulkhead) {
        if (!level.isClientSide && !bulkhead.redstonePrimed) {
            bulkhead.primeAfterLoad(state);
        }

        if (bulkhead.phase != PHASE_MOVING) {
            return;
        }

        bulkhead.phaseTicks++;
        int moveTicks = bulkhead.moveTicks();

        if (!level.isClientSide) {
            bulkhead.playMoveCueSounds();
            bulkhead.syncOpenStageFromTicks();
            if (bulkhead.phaseTicks >= moveTicks) {
                bulkhead.finishMove();
            }
        } else if (bulkhead.phaseTicks >= moveTicks) {
            bulkhead.openProgress = bulkhead.opening ? 1f : 0f;
            bulkhead.phase = PHASE_IDLE;
            bulkhead.phaseTicks = 0;
            if (bulkhead.opening) {
                bulkhead.snapToAnimEnd = true;
            } else {
                bulkhead.snapToAnimEnd = false;
                bulkhead.currentAnimName = "";
            }
        }
    }

    private void primeAfterLoad(BlockState state) {
        Direction facing = state.getValue(BulkheadBlock.FACING);
        lastRedstone = BulkheadBlock.isStructurePowered(level, worldPosition, facing);

        if (phase == PHASE_MOVING) {
            finishMove();
        } else {
            float reconciled = state.getValue(BulkheadBlock.OPEN_STAGE) >= 2 ? 1f : 0f;
            if (openProgress != reconciled) {
                openProgress = reconciled;
                refreshSnapFromState();
                setChangedAndSync();
            }
        }

        redstonePrimed = true;
    }

    private void playMoveCueSounds() {
        if (phaseTicks == DOOR_SOUND_TICK) {
            playSound(opening ? SoundRegistry.BULKHEAD_OPEN.get() : SoundRegistry.BULKHEAD_CLOSE.get());
        }
        if (opening) {
            if (phaseTicks == UNCLAMP_TICK_A || phaseTicks == UNCLAMP_TICK_B) {
                playSound(SoundRegistry.BULKHEAD_UNCLAMP.get());
            }
        } else {
            if (phaseTicks == CLAMP_TICK_A || phaseTicks == CLAMP_TICK_B) {
                playSound(SoundRegistry.BULKHEAD_CLAMP.get());
            }
            if (phaseTicks == CRUSH_WARNING_TICK && hasPlayerInDoor()) {
                playSound(SoundRegistry.BULKHEAD_CRUSH_MINOS.get());
            }
            if (phaseTicks == CRUSH_CLOSE_TICK && hasPlayerInDoor()) {
                crushPlayersInDoor();
            }
        }
    }

    private AABB doorBounds() {
        Direction facing = getBlockState().getValue(BulkheadBlock.FACING);
        AABB bounds = null;
        for (BulkheadPart part : BulkheadPart.values()) {
            AABB partBox = new AABB(BulkheadBlock.partPos(worldPosition, facing, part));
            bounds = bounds == null ? partBox : bounds.minmax(partBox);
        }
        return bounds;
    }

    private boolean hasPlayerInDoor() {
        if (level == null) {
            return false;
        }
        return !level.getEntitiesOfClass(Player.class, doorBounds()).isEmpty();
    }

    private void crushPlayersInDoor() {
        if (level == null || level.isClientSide) {
            return;
        }
        playSound(SoundRegistry.BULKHEAD_CRUSH_IMPACT.get());
        DamageSource source = DamageTypeRegistry.getSimpleDamageSource(level, DamageTypeRegistry.BULKHEAD_CRUSH);
        for (Player player : level.getEntitiesOfClass(Player.class, doorBounds())) {
            player.hurt(source, Float.MAX_VALUE);
        }
    }

    private void syncOpenStageFromTicks() {
        int stage;
        if (opening) {
            if (phaseTicks >= OPEN_MOVE_TICKS) {
                stage = 2;
            } else if (phaseTicks >= OPEN_STAGE1_TICK) {
                stage = 1;
            } else {
                stage = 0;
            }
        } else {
            if (phaseTicks >= CLOSE_STAGE0_TICK) {
                stage = 0;
            } else if (phaseTicks >= CLOSE_STAGE1_TICK) {
                stage = 1;
            } else {
                stage = 2;
            }
        }
        applyOpenStage(stage);
    }

    private void applyOpenStage(int stage) {
        if (level == null || level.isClientSide) {
            return;
        }

        Direction facing = getBlockState().getValue(BulkheadBlock.FACING);
        for (BulkheadPart part : BulkheadPart.values()) {
            BlockPos partPos = BulkheadBlock.partPos(worldPosition, facing, part);
            BlockState partState = level.getBlockState(partPos);
            if (partState.is(getBlockState().getBlock()) && partState.getValue(BulkheadBlock.OPEN_STAGE) != stage) {
                level.setBlock(partPos, partState.setValue(BulkheadBlock.OPEN_STAGE, stage), Block.UPDATE_ALL);
                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.getCapability(CapabilityRegistry.ASH_ATMOSPHERE_CAPABILITY).ifPresent(ash ->
                            ash.seedAshAtBreach(serverLevel, partPos));
                }
            }
        }
    }

    private void playSound(net.minecraft.sounds.SoundEvent sound) {
        if (level == null || level.isClientSide) {
            return;
        }
        level.playSound(null, worldPosition, sound, SoundSource.BLOCKS, 1f, 1f);
    }

    private void setChangedAndSync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    private void refreshSnapFromState() {
        snapToAnimEnd = phase == PHASE_IDLE && openProgress >= 0.5f;
        if (phase != PHASE_MOVING) {
            currentAnimName = "";
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Phase", phase);
        tag.putInt("PhaseTicks", phaseTicks);
        tag.putFloat("OpenProgress", openProgress);
        tag.putBoolean("Opening", opening);
        tag.putBoolean("LastRedstone", lastRedstone);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        phase = tag.getInt("Phase");
        phaseTicks = tag.getInt("PhaseTicks");
        openProgress = tag.getFloat("OpenProgress");
        opening = tag.getBoolean("Opening");
        lastRedstone = tag.getBoolean("LastRedstone");
        refreshSnapFromState();
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            handleUpdateTag(tag);
        }
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(2.0D, 2.0D, 2.0D);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "doors", 0, this::predicate) {
            @Override
            protected double adjustTick(double tick) {
                if (snapToAnimEnd) {
                    return OPEN_MOVE_TICKS;
                }
                return super.adjustTick(tick);
            }
        });
    }

    private PlayState predicate(AnimationState<BulkheadBlockEntity> state) {
        AnimationController<BulkheadBlockEntity> controller = state.getController();

        if (phase == PHASE_MOVING) {
            String want = opening ? "open" : "close";
            if (!want.equals(currentAnimName)) {
                currentAnimName = want;
                controller.setAnimation(RawAnimation.begin().thenPlayAndHold(want));
            }
            return PlayState.CONTINUE;
        }

        if (openProgress >= 0.5f) {
            if (!"open".equals(currentAnimName)) {
                currentAnimName = "open";
                snapToAnimEnd = true;
                controller.setAnimation(RawAnimation.begin().thenPlayAndHold("open"));
            }
            return PlayState.CONTINUE;
        }

        currentAnimName = "";
        return PlayState.STOP;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}