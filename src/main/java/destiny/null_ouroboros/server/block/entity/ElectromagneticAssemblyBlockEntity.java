package destiny.null_ouroboros.server.block.entity;

import destiny.null_ouroboros.client.network.ClientBoundEmaPlacePacket;
import destiny.null_ouroboros.client.render.dimension.VergeOfRealityDimensionEffects;
import destiny.null_ouroboros.server.block.ElectromagneticAssemblyBlock;
import destiny.null_ouroboros.server.capability.ClientManifoldingHolder;
import destiny.null_ouroboros.server.capability.ManifoldingPhase;
import destiny.null_ouroboros.server.registry.BlockEntityRegistry;
import destiny.null_ouroboros.server.registry.PacketHandlerRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.PacketDistributor;

public class ElectromagneticAssemblyBlockEntity extends BlockEntity {
    public static final String SPINNER_ANGLE = "SpinnerAngle";
    public static final String VANE_BASE_ANGLE = "VaneBaseAngle";
    public static final String INITIALIZED = "Initialized";

    public static final float MAX_SPINNER_SPEED = 1080f / 20f;
    public static final float SPINNER_ACCELERATION = MAX_SPINNER_SPEED / (3f * 20f);
    public static final float SPINNER_DECELERATION = MAX_SPINNER_SPEED / (8f * 20f);

    public static final float VANE_REST_TIP_WORLD_YAW = 90f;
    public static final float APPROACH_THRESHOLD = 2f;
    public static final float VANE_APPROACH_SPEED = 3f;
    public static final float SWAY_LEEWAY_MIN = 5f;
    public static final float SWAY_LEEWAY_MAX = 60f;
    public static final float SWAY_BASE_HZ = 0.25f;
    public static final float SWAY_FREQ_SCALE_HZ = 2.75f;
    public static final float WIND_ANGLE_RESET_THRESHOLD = 15f;

    public float spinnerAngle = 0f;
    public float spinnerSpeed = 0f;
    public float vaneBaseAngle = 0f;
    public float vaneOscPhase = 0f;
    public boolean vaneReachedTarget = false;
    public boolean initialized = false;

    private float lastWindAngle = 0f;

    public ElectromagneticAssemblyBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.ELECTROMAGNETIC_ASSEMBLY_BLOCK_ENTITY.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ElectromagneticAssemblyBlockEntity be) {
        if (!level.isClientSide) {
            return;
        }

        if (!(level instanceof ClientLevel clientLevel) || !VergeOfRealityDimensionEffects.isVergeOfReality(clientLevel)) {
            if (be.spinnerSpeed > 0f) {
                be.spinnerSpeed = Math.max(0f, be.spinnerSpeed - SPINNER_DECELERATION);
                be.spinnerAngle = (be.spinnerAngle + be.spinnerSpeed) % 360f;
            }
            be.vaneReachedTarget = false;
            return;
        }

        float windStrength = ClientManifoldingHolder.getWindStrength();
        ManifoldingPhase phase = ClientManifoldingHolder.getPhase();
        float windAngle = ClientManifoldingHolder.getWindAngle();
        Direction facing = state.getValue(ElectromagneticAssemblyBlock.HORIZONTAL_FACING);

        float targetSpinnerSpeed = windStrength * MAX_SPINNER_SPEED;
        if (be.spinnerSpeed < targetSpinnerSpeed) {
            be.spinnerSpeed = Math.min(be.spinnerSpeed + SPINNER_ACCELERATION, targetSpinnerSpeed);
        } else if (be.spinnerSpeed > targetSpinnerSpeed) {
            be.spinnerSpeed = Math.max(be.spinnerSpeed - SPINNER_DECELERATION, targetSpinnerSpeed);
        }
        be.spinnerAngle = (be.spinnerAngle + be.spinnerSpeed) % 360f;

        if (Math.abs(angleDiff(be.lastWindAngle, windAngle)) > WIND_ANGLE_RESET_THRESHOLD) {
            be.vaneReachedTarget = false;
            be.lastWindAngle = windAngle;
        }

        float targetLocal = computeVaneTargetAngle(windAngle, facing);

        if (phase != ManifoldingPhase.CLEAR && windStrength > 0f) {
            if (!be.vaneReachedTarget) {
                float diff = angleDiff(be.vaneBaseAngle, targetLocal);
                float approachSpeed = phase == ManifoldingPhase.PRE_EVENT ? VANE_APPROACH_SPEED * 0.5f : VANE_APPROACH_SPEED;
                if (Math.abs(diff) <= APPROACH_THRESHOLD) {
                    be.vaneBaseAngle = targetLocal;
                    be.vaneReachedTarget = true;
                } else {
                    float step = Math.min(approachSpeed, Math.abs(diff));
                    be.vaneBaseAngle += Math.signum(diff) * step;
                }
            }
        }

        if (be.vaneReachedTarget && windStrength > 0f) {
            float freqHz = SWAY_BASE_HZ + windStrength * SWAY_FREQ_SCALE_HZ;
            be.vaneOscPhase += freqHz * 2f * (float) Math.PI / 20f;
        } else if (windStrength <= 0f) {
            be.vaneReachedTarget = false;
        }
    }

    public float getVaneDisplayAngle() {
        float windStrength = ClientManifoldingHolder.getWindStrength();
        if (vaneReachedTarget && windStrength > 0f) {
            float leeway = Mth.clamp(SWAY_LEEWAY_MAX - windStrength * (SWAY_LEEWAY_MAX - SWAY_LEEWAY_MIN), SWAY_LEEWAY_MIN, SWAY_LEEWAY_MAX);
            if (ClientManifoldingHolder.getPhase() == ManifoldingPhase.POST_EVENT) {
                leeway *= windStrength;
            }
            return vaneBaseAngle + leeway * (float) Math.sin(vaneOscPhase);
        }
        return vaneBaseAngle;
    }

    public float getSpinnerAngleForRender(float partialTick) {
        return spinnerAngle + spinnerSpeed * partialTick;
    }

    public static float computeVaneTargetAngle(float windAngle, Direction facing) {
        return windAngle - VANE_REST_TIP_WORLD_YAW - getFacingYawOffset(facing);
    }

    public float getVaneRenderAngle() {
        return getVaneDisplayAngle();
    }

    public void onPlace() {
        if (!initialized && level != null && !level.isClientSide) {
            spinnerAngle = level.random.nextFloat() * 360f;
            vaneBaseAngle = level.random.nextFloat() * 360f;
            initialized = true;
            setChanged();

            PacketHandlerRegistry.INSTANCE.send(
                    PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(worldPosition)),
                    new ClientBoundEmaPlacePacket(worldPosition, spinnerAngle, vaneBaseAngle)
            );
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide && !initialized) {
            onPlace();
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }

    public static float getFacingYawOffset(Direction facing) {
        return switch (facing) {
            case EAST -> 90f;
            case SOUTH -> 180f;
            case WEST -> 270f;
            default -> 0f;
        };
    }

    public static float getCompassCounterRotation(Direction facing) {
        return 180f - getFacingYawOffset(facing);
    }

    public static float getFacingYRot(Direction facing) {
        return getFacingYawOffset(facing);
    }

    private static float angleDiff(float from, float to) {
        float diff = (to - from) % 360f;
        if (diff > 180f) {
            diff -= 360f;
        }
        if (diff < -180f) {
            diff += 360f;
        }
        return diff;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putFloat(SPINNER_ANGLE, spinnerAngle);
        tag.putFloat(VANE_BASE_ANGLE, vaneBaseAngle);
        tag.putBoolean(INITIALIZED, initialized);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        spinnerAngle = tag.getFloat(SPINNER_ANGLE);
        vaneBaseAngle = tag.getFloat(VANE_BASE_ANGLE);
        initialized = tag.getBoolean(INITIALIZED);
    }
}
