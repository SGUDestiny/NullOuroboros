package destiny.null_ouroboros.server.block.entity;

import destiny.null_ouroboros.server.block.GarageDoorBlock;
import destiny.null_ouroboros.server.block.GarageDoorPart;
import destiny.null_ouroboros.server.registry.BlockEntityRegistry;
import destiny.null_ouroboros.server.registry.CapabilityRegistry;
import destiny.null_ouroboros.server.registry.SoundRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
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

public class GarageDoorBlockEntity extends BlockEntity implements GeoBlockEntity {
    public static final int OPEN_MOVE_TICKS = 250;
    public static final int CLOSE_MOVE_TICKS = 250;

    private static final int PHASE_IDLE = 0;
    private static final int PHASE_MOVING = 1;

    private static final int OPEN_BOTTOM_TICK = 60;
    private static final int OPEN_MIDDLE_TICK = 120;
    private static final int OPEN_TOP_TICK = 160;
    private static final int CLOSE_TOP_TICK = 130;
    private static final int CLOSE_MIDDLE_TICK = 190;
    private static final int CLOSE_BOTTOM_TICK = 250;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private int phase = PHASE_IDLE;
    private int phaseTicks;
    private float openProgress;
    private boolean opening = true;
    private boolean lastRedstone;
    private boolean snapToAnimEnd;
    private String currentAnimName = "";

    public GarageDoorBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.GARAGE_DOOR_BLOCK_ENTITY.get(), pos, state);
    }

    public boolean isCycling() {
        return phase != PHASE_IDLE;
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
        playSound(SoundRegistry.GARAGE_DOOR.get());
        setChangedAndSync();
    }

    private void finishMove() {
        openProgress = opening ? 1f : 0f;
        applyOpenStage(opening ? 3 : 0);
        phase = PHASE_IDLE;
        phaseTicks = 0;
        if (opening) {
            snapToAnimEnd = true;
        } else {
            snapToAnimEnd = false;
            currentAnimName = "";
        }

        if (level != null) {
            Direction facing = getBlockState().getValue(GarageDoorBlock.FACING);
            lastRedstone = GarageDoorBlock.isStructurePowered(level, worldPosition, facing);
        }
        setChangedAndSync();
    }

    private int moveTicks() {
        return opening ? OPEN_MOVE_TICKS : CLOSE_MOVE_TICKS;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, GarageDoorBlockEntity garageDoor) {
        if (garageDoor.phase != PHASE_MOVING) {
            return;
        }

        garageDoor.phaseTicks++;
        int moveTicks = garageDoor.moveTicks();

        if (!level.isClientSide) {
            garageDoor.syncOpenStageFromTicks();
            if (garageDoor.phaseTicks >= moveTicks) {
                garageDoor.finishMove();
            }
        } else if (garageDoor.phaseTicks >= moveTicks) {
            garageDoor.openProgress = garageDoor.opening ? 1f : 0f;
            garageDoor.phase = PHASE_IDLE;
            garageDoor.phaseTicks = 0;
            if (garageDoor.opening) {
                garageDoor.snapToAnimEnd = true;
            } else {
                garageDoor.snapToAnimEnd = false;
                garageDoor.currentAnimName = "";
            }
        }
    }

    private void syncOpenStageFromTicks() {
        int stage;
        if (opening) {
            if (phaseTicks >= OPEN_TOP_TICK) {
                stage = 3;
            } else if (phaseTicks >= OPEN_MIDDLE_TICK) {
                stage = 2;
            } else if (phaseTicks >= OPEN_BOTTOM_TICK) {
                stage = 1;
            } else {
                stage = 0;
            }
        } else {
            if (phaseTicks >= CLOSE_BOTTOM_TICK) {
                stage = 0;
            } else if (phaseTicks >= CLOSE_MIDDLE_TICK) {
                stage = 1;
            } else if (phaseTicks >= CLOSE_TOP_TICK) {
                stage = 2;
            } else {
                stage = 3;
            }
        }
        applyOpenStage(stage);
    }

    private void applyOpenStage(int stage) {
        if (level == null || level.isClientSide) {
            return;
        }

        Direction facing = getBlockState().getValue(GarageDoorBlock.FACING);
        for (GarageDoorPart part : GarageDoorPart.values()) {
            BlockPos partPos = GarageDoorBlock.partPos(worldPosition, facing, part);
            BlockState partState = level.getBlockState(partPos);
            if (partState.is(getBlockState().getBlock()) && partState.getValue(GarageDoorBlock.OPEN_STAGE) != stage) {
                level.setBlock(partPos, partState.setValue(GarageDoorBlock.OPEN_STAGE, stage), Block.UPDATE_ALL);
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
        return new AABB(worldPosition).inflate(4.0D, 3.0D, 4.0D);
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

    private PlayState predicate(AnimationState<GarageDoorBlockEntity> state) {
        AnimationController<GarageDoorBlockEntity> controller = state.getController();

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
