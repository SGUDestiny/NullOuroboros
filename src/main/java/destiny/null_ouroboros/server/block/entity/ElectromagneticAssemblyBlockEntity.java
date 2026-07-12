package destiny.null_ouroboros.server.block.entity;

import destiny.null_ouroboros.client.network.ClientBoundEmaPlacePacket;
import destiny.null_ouroboros.server.block.ElectromagneticAssemblyBlock;
import destiny.null_ouroboros.server.registry.BlockEntityRegistry;
import destiny.null_ouroboros.server.registry.PacketHandlerRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public class ElectromagneticAssemblyBlockEntity extends BlockEntity {
    public static final String SPINNER_ANGLE = "SpinnerAngle";
    public static final String VANE_BASE_ANGLE = "VaneBaseAngle";
    public static final String INITIALIZED = "Initialized";

    public static final float MAX_SPINNER_SPEED = 1080f / 20f;
    public static final float SPINNER_ACCELERATION = MAX_SPINNER_SPEED / (3f * 20f);
    public static final float SPINNER_DECELERATION = MAX_SPINNER_SPEED / (8f * 20f);

    /*
     * Minecraft horizontal yaw reference (Vec3.directionFromRotation(0, yaw), manifolding windAngle):
     *   0 = south (+Z), 90 = west (-X), 180 = north (-Z), 270 = east (+X)
     *
     * Block HORIZONTAL_FACING uses the same Direction enum; BER applies that as a Y rotation via getFacingYRot().
     * Renderer chain: center -> Y(facing) -> translate up -> Z(180) -> bone -> part yRot.
     *
     * Vane: worldTipYaw = 90 - facingYaw + vaneAngle  =>  vaneAngle = windAngle - VANE_REST_TIP_WORLD_YAW + facingYaw
     * Compass: counter-rotates block facing so N/S/E/W labels stay world-aligned  =>  180 + facingYaw
     *
     * Do not flip the +/- on facingYaw without re-checking all four facings; east/west use the opposite sign from a naive subtract.
     */
    /** Model vane at yRot=0: tip direction constant in the inverse of worldTipYaw = 90 - facingYaw + vaneAngle. */
    public static final float VANE_REST_TIP_WORLD_YAW = 270f;
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

    public ElectromagneticAssemblyBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.ELECTROMAGNETIC_ASSEMBLY_BLOCK_ENTITY.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ElectromagneticAssemblyBlockEntity be) {
        if (level.isClientSide) {
            DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> new DistExecutor.SafeRunnable() {
                @Override
                public void run() {
                    destiny.null_ouroboros.client.render.ElectromagneticAssemblyClientTicker.tick(level, state, be);
                }
            });
        }
    }

    // here lies getVaneRenderAngle
    // getVaneDisplayAngle was moved to ElectromagneticAssemblyClientTicker
    // sorry Vane, your snippets were crashing in servers

    public float getSpinnerAngleForRender(float partialTick) {
        return spinnerAngle + spinnerSpeed * partialTick;
    }

    /** Local vane yRot so the arrow tip points in windAngle (MC yaw, blow direction). */
    public static float computeVaneTargetAngle(float windAngle, Direction facing) {
        return normalizeDegrees(windAngle - VANE_REST_TIP_WORLD_YAW + getFacingYawOffset(facing));
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

    /** BER Y rotation degrees for each block facing (N=0, E=90, S=180, W=270). */
    public static float getFacingYawOffset(Direction facing) {
        return switch (facing) {
            case EAST -> 270f;
            case SOUTH -> 180f;
            case WEST -> 90f;
            default -> 0f; // NORTH
        };
    }

    /** Compass local yRot to cancel block facing and keep cardinals world-aligned. */
    public static float getCompassCounterRotation(Direction facing) {
        return 180f + getFacingYawOffset(facing);
    }

    public static float getFacingYRot(Direction facing) {
        return getFacingYawOffset(facing);
    }

    public static float angleDiff(float from, float to) {
        float diff = (normalizeDegrees(to) - normalizeDegrees(from)) % 360f;
        if (diff > 180f) {
            diff -= 360f;
        }
        if (diff < -180f) {
            diff += 360f;
        }
        return diff;
    }

    public static float normalizeDegrees(float angle) {
        angle %= 360f;
        if (angle < 0f) {
            angle += 360f;
        }
        return angle;
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
        vaneReachedTarget = false;
        initialized = tag.getBoolean(INITIALIZED);
    }
}