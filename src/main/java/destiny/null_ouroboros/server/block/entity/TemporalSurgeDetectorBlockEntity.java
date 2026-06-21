package destiny.null_ouroboros.server.block.entity;

import destiny.null_ouroboros.client.network.ClientBoundDetectorPlacePacket;
import destiny.null_ouroboros.client.network.ClientBoundDetectorPulsePacket;
import destiny.null_ouroboros.server.block.TemporalSurgeDetectorBlock;
import destiny.null_ouroboros.server.capability.ClientManifoldingHolder;
import destiny.null_ouroboros.server.capability.ManifoldingPhase;
import destiny.null_ouroboros.server.registry.BlockEntityRegistry;
import destiny.null_ouroboros.server.registry.CapabilityRegistry;
import destiny.null_ouroboros.server.registry.PacketHandlerRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.PacketDistributor;

public class TemporalSurgeDetectorBlockEntity extends BlockEntity {
    public static final String RING_1_ANGLE = "Ring1Angle";
    public static final String RING_2_ANGLE = "Ring2Angle";
    public static final String INITIALIZED = "Initialized";

    public float ring1Angle = 0f;
    public float ring2Angle = 0f;

    public float burstBoost = 0f;
    public static final float BURST_SPEED = 30f;
    public static final float BURST_DECAY = 1f / 100f;

    public boolean initialized = false;

    public TemporalSurgeDetectorBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.TEMPORAL_SURGE_DETECTOR_BLOCK_ENTITY.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TemporalSurgeDetectorBlockEntity be) {
        if (!level.isClientSide) return;

        float baseSpeed = getManifoldingBaseSpeed(level);
        float speed = baseSpeed + be.burstBoost * BURST_SPEED;

        be.ring1Angle = (be.ring1Angle + speed) % 360f;
        be.ring2Angle = (be.ring2Angle + speed) % 360f;
        be.burstBoost = Math.max(0f, be.burstBoost - BURST_DECAY);
    }

    private static float getManifoldingBaseSpeed(Level level) {
        ManifoldingPhase phase = ClientManifoldingHolder.getPhase();
        long startTime = ClientManifoldingHolder.getPhaseStartTime();
        long now = level.getGameTime();
        long elapsed = now - startTime;

        switch (phase) {
            case PRE_EVENT:
                int preDur = ClientManifoldingHolder.getPreDuration();
                if (preDur <= 0) return 0;
                float progress = Math.min(1f, (float) elapsed / preDur);
                return progress * 3f;

            case ACTIVE:
                return 6f;

            case POST_EVENT:
                int postDur = ClientManifoldingHolder.getPostDuration();
                if (postDur <= 0) return 0;
                float postProgress = Math.min(1f, (float) elapsed / postDur);
                return (1f - postProgress) * 6f;

            default:
                return 0f;
        }
    }

    public void pulse() {
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            if (state.getBlock() instanceof TemporalSurgeDetectorBlock) {
                level.setBlock(worldPosition, state.setValue(TemporalSurgeDetectorBlock.POWERED, true), 3);
                level.scheduleTick(worldPosition, state.getBlock(), 2);
            }

            PacketHandlerRegistry.INSTANCE.send(
                    PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(worldPosition)),
                    new ClientBoundDetectorPulsePacket(worldPosition)
            );
        }
    }

    public void onPlace() {
        if (!initialized && level != null && !level.isClientSide) {
            ring1Angle = level.random.nextFloat() * 360f;
            ring2Angle = level.random.nextFloat() * 360f;
            initialized = true;
            setChanged();

            PacketHandlerRegistry.INSTANCE.send(PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(worldPosition)),
                    new ClientBoundDetectorPlacePacket(worldPosition, ring1Angle, ring2Angle)
            );
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            level.getCapability(CapabilityRegistry.MANIFOLDING_CAPABILITY).ifPresent(cap -> cap.addDetector(worldPosition));

            if (!initialized) {
                onPlace();
            }
        }
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide) {
            level.getCapability(CapabilityRegistry.MANIFOLDING_CAPABILITY).ifPresent(cap -> {
                cap.removeDetector(worldPosition);
            });
        }
        super.setRemoved();
    }

    @Override
    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        this.load(tag);
    }

    public float getRing1Angle() {
        return ring1Angle;
    }

    public float getRing2Angle() {
        return ring2Angle;
    }

    public float getBurstFactor() {
        return burstBoost;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        tag.putFloat(RING_1_ANGLE, ring1Angle);
        tag.putFloat(RING_2_ANGLE, ring2Angle);
        tag.putBoolean(INITIALIZED, initialized);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        ring1Angle = tag.getFloat(RING_1_ANGLE);
        ring2Angle = tag.getFloat(RING_2_ANGLE);
        initialized = tag.getBoolean(INITIALIZED);
    }
}