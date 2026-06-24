package destiny.null_ouroboros.server.block.entity;

import destiny.null_ouroboros.client.network.ClientBoundSirenSoundPacket;
import destiny.null_ouroboros.client.sound.SirenSoundManager;
import destiny.null_ouroboros.server.block.MechanicalSirenBlock;
import destiny.null_ouroboros.server.registry.BlockEntityRegistry;
import destiny.null_ouroboros.server.registry.CapabilityRegistry;
import destiny.null_ouroboros.server.registry.PacketHandlerRegistry;
import destiny.null_ouroboros.server.registry.SoundRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;

public class MechanicalSirenBlockEntity extends BlockEntity {
    private static final float MAX_RPM = 120f;
    public static final float MAX_SPEED = MAX_RPM * 360f / 60f / 20f;
    private static final float ACCELERATION = MAX_SPEED / (3f * 20f);
    private static final float DECELERATION = MAX_SPEED / (8f * 20f);

    private static final String STATE = "state";
    private static final String PHASE_TIMER = "phaseTimer";
    private static final String LOOPS_REMAINING = "loopsRemaining";
    private static final String MANIFOLDING_ACTIVE = "manifoldingActive";
    private static final String MANIFOLDING_DELAY = "manifoldingDelay";

    public enum State {
        IDLE,
        START,
        LOOP,
        END
    }
    private State state = State.IDLE;
    private int phaseTimer = 0;

    private int loopsRemaining = 0;
    private boolean manifoldingActive = false;
    private int manifoldingDelay = 0;

    private float rotationAngle = 0f;
    private float rotationSpeed = 0f;

    public MechanicalSirenBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.MECHANICAL_SIREN_BLOCK_ENTITY.get(), pos, state);
    }

    public void triggerManifolding(int loops, int delay) {
        if (level == null || level.isClientSide) return;
        manifoldingActive = true;
        loopsRemaining = loops;
        manifoldingDelay = delay;
        state = State.IDLE;
        phaseTimer = 0;
        setChanged();
        level.blockEntityChanged(worldPosition);
        level.getChunkSource().updateChunkForced(new ChunkPos(worldPosition), true);
    }

    public void checkRedstone(boolean powered) {
        if (level == null || level.isClientSide) return;
        if (manifoldingActive) return;

        if (state == State.IDLE && powered) {
            transitionToStart();
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MechanicalSirenBlockEntity sirenEntity) {
        float targetSpeed = (sirenEntity.state == State.START || sirenEntity.state == State.LOOP) ? MAX_SPEED : 0f;
        if (sirenEntity.rotationSpeed < targetSpeed) {
            sirenEntity.rotationSpeed = Math.min(sirenEntity.rotationSpeed + ACCELERATION, targetSpeed);
        } else if (sirenEntity.rotationSpeed > targetSpeed) {
            sirenEntity.rotationSpeed = Math.max(sirenEntity.rotationSpeed - DECELERATION, targetSpeed);
        }

        sirenEntity.rotationAngle = (sirenEntity.rotationAngle + sirenEntity.rotationSpeed) % 360f;

        if (level.isClientSide) {
            sirenEntity.clientTickSounds();
        } else {
            sirenEntity.serverTick();
            boolean shouldBeActive = sirenEntity.state != State.IDLE;
            BlockState currentState = level.getBlockState(pos);

            if (currentState.getBlock() instanceof MechanicalSirenBlock
                    && currentState.getValue(MechanicalSirenBlock.ACTIVE) != shouldBeActive) {
                level.setBlock(pos, currentState.setValue(MechanicalSirenBlock.ACTIVE, shouldBeActive), 3);
            }
        }
    }

    private void serverTick() {
        if (manifoldingActive && state == State.IDLE) {
            if (manifoldingDelay > 0) {
                manifoldingDelay--;
                setChanged();

                if (manifoldingDelay <= 0) {
                    transitionToStart();
                }
            }
            return;
        }

        if (state == State.IDLE) return;

        phaseTimer++;
        int phaseDuration = getPhaseDuration();

        switch (state) {
            case START -> {
                if (phaseTimer >= phaseDuration) {
                    if (loopsRemaining > 0 || isRedstonePowered()) {
                        transitionToLoop();
                    } else {
                        transitionToEnd();
                    }
                }
            }
            case LOOP -> {
                if (phaseTimer >= phaseDuration) {
                    if (loopsRemaining > 1) {
                        loopsRemaining--;
                        phaseTimer = 0;
                    } else if (loopsRemaining == 1) {
                        transitionToEnd();
                    } else if (isRedstonePowered()) {
                        phaseTimer = 0;
                    } else {
                        transitionToEnd();
                    }
                }
            }
            case END -> {
                if (phaseTimer >= phaseDuration) {
                    transitionToIdle();
                }
            }
        }
        setChanged();
    }

    private boolean isRedstonePowered() {
        return getBlockState().getValue(MechanicalSirenBlock.POWERED);
    }

    private void syncToClient() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private void transitionToStart() {
        state = State.START;
        phaseTimer = 0;

        if (level != null) {
            level.getChunkSource().updateChunkForced(new ChunkPos(worldPosition), true);
            setChanged();
            level.blockEntityChanged(worldPosition);
        }
        sendSoundPacket();
        syncToClient();
    }

    private void transitionToLoop() {
        state = State.LOOP;
        phaseTimer = 0;

        if (level != null) {
            setChanged();
            level.blockEntityChanged(worldPosition);
        }
        sendSoundPacket();
        syncToClient();
    }

    private void transitionToEnd() {
        state = State.END;
        phaseTimer = 0;

        if (level != null) {
            setChanged();
            level.blockEntityChanged(worldPosition);
        }
        sendSoundPacket();
        syncToClient();
    }

    private void transitionToIdle() {
        state = State.IDLE;
        phaseTimer = 0;
        loopsRemaining = 0;
        manifoldingActive = false;

        if (level != null) {
            level.getChunkSource().updateChunkForced(new ChunkPos(worldPosition), false);
            setChanged();
            level.blockEntityChanged(worldPosition);
        }
        sendSoundPacket();
        syncToClient();
    }

    private void sendSoundPacket() {
        if (level == null || level.isClientSide) return;

        SoundEvent normal = getNormalSoundEvent();
        SoundEvent distant = getDistantSoundEvent();
        boolean looping = (state == State.LOOP);

        ClientBoundSirenSoundPacket packet = new ClientBoundSirenSoundPacket(worldPosition, normal, distant, looping);

        PacketHandlerRegistry.INSTANCE.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(worldPosition.getX() + 0.5,
                worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, 300.0, level.dimension())), packet);
    }

    private int getPhaseDuration() {
        return switch (state) {
            case START -> (int) (2.129 * 20);
            case LOOP -> (int) (8.932 * 20);
            case END -> (int) (14.713 * 20);
            default -> 0;
        };
    }

    private void clientTickSounds() {
        State trackedPhase = SirenSoundManager.getTrackedPhase(worldPosition);

        if (state == State.IDLE) {
            if (trackedPhase == State.END || (trackedPhase == null && SirenSoundManager.isActive(worldPosition))) {
                SirenSoundManager.stop(worldPosition);
            }
            return;
        }

        if (trackedPhase == null) {
            if (state != State.END) {
                SirenSoundManager.syncFromBlockEntity(this);
            }
            return;
        }

        if (trackedPhase.ordinal() > state.ordinal()) {
            return;
        }

        if (trackedPhase != state) {
            SirenSoundManager.syncFromBlockEntity(this);
            return;
        }

        if (state == State.LOOP && !SirenSoundManager.isFullyActive(worldPosition)) {
            SirenSoundManager.syncFromBlockEntity(this);
        }
    }

    @Nullable
    public SoundEvent getNormalSoundEvent() {
        return switch (state) {
            case START -> SoundRegistry.MECHANICAL_SIREN_START.get();
            case LOOP -> SoundRegistry.MECHANICAL_SIREN_LOOP.get();
            case END -> SoundRegistry.MECHANICAL_SIREN_END.get();
            default -> null;
        };
    }

    @Nullable
    public SoundEvent getDistantSoundEvent() {
        return switch (state) {
            case START -> SoundRegistry.MECHANICAL_SIREN_START_DISTANT.get();
            case LOOP -> SoundRegistry.MECHANICAL_SIREN_LOOP_DISTANT.get();
            case END -> SoundRegistry.MECHANICAL_SIREN_END_DISTANT.get();
            default -> null;
        };
    }

    public State getState() {
        return state;
    }

    public float getRotationAngle() {
        return rotationAngle;
    }
    public float getRotationSpeed() {
        return rotationSpeed;
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString(STATE, state.name());
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        if (tag.contains(STATE)) {
            State newState = State.valueOf(tag.getString(STATE));

            if (this.state != newState) {
                this.state = newState;
                this.phaseTimer = 0;
                if (level != null && level.isClientSide) {
                    SirenSoundManager.syncFromBlockEntity(this);
                }
            }
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            level.getCapability(CapabilityRegistry.MANIFOLDING_CAPABILITY).ifPresent(cap -> {
                cap.addSiren(worldPosition);
                cap.applyPendingSirenTrigger(this);
            });
        }
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide) {
            level.getCapability(CapabilityRegistry.MANIFOLDING_CAPABILITY).ifPresent(cap -> cap.removeSiren(worldPosition));
        }
        super.setRemoved();
        if (level != null && level.isClientSide) {
            SirenSoundManager.stop(worldPosition);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        tag.putString(STATE, state.name());
        tag.putInt(PHASE_TIMER, phaseTimer);
        tag.putInt(LOOPS_REMAINING, loopsRemaining);
        tag.putBoolean(MANIFOLDING_ACTIVE, manifoldingActive);
        tag.putInt(MANIFOLDING_DELAY, manifoldingDelay);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        state = State.valueOf(tag.getString(STATE));
        phaseTimer = tag.getInt(PHASE_TIMER);
        loopsRemaining = tag.getInt(LOOPS_REMAINING);
        manifoldingActive = tag.getBoolean(MANIFOLDING_ACTIVE);
        manifoldingDelay = tag.getInt(MANIFOLDING_DELAY);
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}