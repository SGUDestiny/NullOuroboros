package destiny.null_ouroboros.server.entity;

import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.client.render.DusterbikePistonShakeManager;
import destiny.null_ouroboros.common.DusterbikeEngineSoundConstants;
import destiny.null_ouroboros.common.DusterbikeGear;
import destiny.null_ouroboros.common.DusterbikeRiderAnimation;
import destiny.null_ouroboros.common.DusterbikeTransforms;
import destiny.null_ouroboros.common.dusterbike.DusterbikeEngineState;
import destiny.null_ouroboros.common.dusterbike.DusterbikePartItems;
import destiny.null_ouroboros.common.dusterbike.DusterbikePartTargetType;
import destiny.null_ouroboros.common.dusterbike.DusterbikePartState;
import destiny.null_ouroboros.common.dusterbike.DusterbikePartType;
import destiny.null_ouroboros.server.item.BikeKeyItem;
import destiny.null_ouroboros.server.item.JerrycanItem;
import destiny.null_ouroboros.server.item.SprayCanItem;
import destiny.null_ouroboros.server.network.ServerBoundDusterbikeDrivePacket;
import destiny.null_ouroboros.server.network.ServerBoundDusterbikeImpactPacket;
import destiny.null_ouroboros.server.registry.DamageTypeRegistry;
import destiny.null_ouroboros.server.registry.EntityRegistry;
import destiny.null_ouroboros.server.registry.ItemRegistry;
import destiny.null_ouroboros.server.registry.PacketHandlerRegistry;
import destiny.null_ouroboros.server.registry.SoundRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Optional;
import java.util.UUID;

public class DusterbikeEntity extends Entity implements GeoAnimatable {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final EntityDataAccessor<Integer> FRONT_WHEEL_ID =
            SynchedEntityData.defineId(DusterbikeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> REAR_WHEEL_ID =
            SynchedEntityData.defineId(DusterbikeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> FRONT_WHEEL_UUID =
            SynchedEntityData.defineId(DusterbikeEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Optional<UUID>> REAR_WHEEL_UUID =
            SynchedEntityData.defineId(DusterbikeEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Float> PITCH =
            SynchedEntityData.defineId(DusterbikeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> FORWARD_SPEED =
            SynchedEntityData.defineId(DusterbikeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Byte> GEAR =
            SynchedEntityData.defineId(DusterbikeEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Float> STEER_ANGLE =
            SynchedEntityData.defineId(DusterbikeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> FRONT_WHEEL_ROTATION =
            SynchedEntityData.defineId(DusterbikeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> REAR_WHEEL_ROTATION =
            SynchedEntityData.defineId(DusterbikeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Byte> ENGINE_RUNNING =
            SynchedEntityData.defineId(DusterbikeEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> KEY_CRANKING =
            SynchedEntityData.defineId(DusterbikeEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> KEY_ID =
            SynchedEntityData.defineId(DusterbikeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> KEY_UUID =
            SynchedEntityData.defineId(DusterbikeEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> FUEL_MILLI_BUCKETS =
            SynchedEntityData.defineId(DusterbikeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FRAME_HEALTH =
            SynchedEntityData.defineId(DusterbikeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> HEADLIGHTS_ON =
            SynchedEntityData.defineId(DusterbikeEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> INSTALLED_PARTS_MASK =
            SynchedEntityData.defineId(DusterbikeEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Integer> MAIN_COLOR_FRAME =
            SynchedEntityData.defineId(DusterbikeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> GLOW_COLOR_FRAME =
            SynchedEntityData.defineId(DusterbikeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> MAIN_COLOR_FRONT_WHEEL =
            SynchedEntityData.defineId(DusterbikeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> GLOW_COLOR_FRONT_WHEEL =
            SynchedEntityData.defineId(DusterbikeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> MAIN_COLOR_REAR_WHEEL =
            SynchedEntityData.defineId(DusterbikeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> GLOW_COLOR_REAR_WHEEL =
            SynchedEntityData.defineId(DusterbikeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> MAIN_COLOR_FRONT_LIGHT =
            SynchedEntityData.defineId(DusterbikeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> GLOW_COLOR_FRONT_LIGHT =
            SynchedEntityData.defineId(DusterbikeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> MAIN_COLOR_REAR_LIGHT =
            SynchedEntityData.defineId(DusterbikeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> GLOW_COLOR_REAR_LIGHT =
            SynchedEntityData.defineId(DusterbikeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> MAIN_COLOR_BATTERY =
            SynchedEntityData.defineId(DusterbikeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> GLOW_COLOR_BATTERY =
            SynchedEntityData.defineId(DusterbikeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> MAIN_COLOR_PISTON_FRONT =
            SynchedEntityData.defineId(DusterbikeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> GLOW_COLOR_PISTON_FRONT =
            SynchedEntityData.defineId(DusterbikeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> MAIN_COLOR_PISTON_REAR =
            SynchedEntityData.defineId(DusterbikeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> GLOW_COLOR_PISTON_REAR =
            SynchedEntityData.defineId(DusterbikeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> MAIN_COLOR_KEY =
            SynchedEntityData.defineId(DusterbikeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> GLOW_COLOR_KEY =
            SynchedEntityData.defineId(DusterbikeEntity.class, EntityDataSerializers.INT);

    private static final int NO_LINKED_ENTITY = -1;
    private static final int MISSING_WHEEL_GRACE_TICKS = 100;
    private static final int NO_KEY_HOLDER = -1;

    private enum IgnitionPhase { NONE, STARTING, COOLDOWN }
    public static final float MAX_SPEED = DusterbikePhysics.MAX_FORWARD_SPEED;

    private boolean discarding;
    private UUID savedFrontUuid;
    private UUID savedRearUuid;
    private UUID savedKeyUuid;
    private int missingWheelTicks;

    private float previousRenderPitch;
    private float renderPitch;
    private float previousRenderSteer;
    private float renderSteer;
    private float previousFrontWheelRotation;
    private float previousRearWheelRotation;
    private float renderFrontWheelRotation;
    private float renderRearWheelRotation;

    private boolean inputForward;
    private boolean inputBackward;
    private boolean inputLeft;
    private boolean inputRight;
    private boolean inputHandbrake;

    private float forwardSpeed;
    private float steerAngle;
    private boolean wheelsInitialized;
    private boolean wasLocallyControlled;
    private boolean needsGroundSnap;

    private float savedFrontWheelRotation;
    private float savedRearWheelRotation;
    private double savedFrontWheelAngularVelocity;
    private double savedRearWheelAngularVelocity;
    private boolean pendingWheelSpinRestore;

    private DusterbikeGear driverGear;

    private int ignitionAttempts;
    private IgnitionPhase ignitionPhase = IgnitionPhase.NONE;
    private int ignitionTicksRemaining;
    private int keyHeldByPlayerId = NO_KEY_HOLDER;
    private int fuelConsumptionTicks;
    private int engineWearTicks;
    private int wheelWearTicks;

    private float pendingImpactSpeed;
    private final DusterbikeEngineState engineState = new DusterbikeEngineState();

    public DusterbikeEntity(EntityType<? extends DusterbikeEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(FRONT_WHEEL_ID, NO_LINKED_ENTITY);
        this.entityData.define(REAR_WHEEL_ID, NO_LINKED_ENTITY);
        this.entityData.define(FRONT_WHEEL_UUID, Optional.empty());
        this.entityData.define(REAR_WHEEL_UUID, Optional.empty());
        this.entityData.define(PITCH, 0.0F);
        this.entityData.define(FORWARD_SPEED, 0.0F);
        this.entityData.define(GEAR, (byte) DusterbikeGear.N.ordinal());
        this.entityData.define(STEER_ANGLE, 0.0F);
        this.entityData.define(FRONT_WHEEL_ROTATION, 0.0F);
        this.entityData.define(REAR_WHEEL_ROTATION, 0.0F);
        this.entityData.define(ENGINE_RUNNING, (byte) 0);
        this.entityData.define(KEY_CRANKING, (byte) 0);
        this.entityData.define(KEY_ID, NO_LINKED_ENTITY);
        this.entityData.define(KEY_UUID, Optional.empty());
        this.entityData.define(FUEL_MILLI_BUCKETS, 0);
        this.entityData.define(FRAME_HEALTH, DusterbikeEngineState.FRAME_MAX_HEALTH);
        this.entityData.define(HEADLIGHTS_ON, (byte) 0);
        this.entityData.define(INSTALLED_PARTS_MASK, computeDefaultInstalledMask());

        this.entityData.define(MAIN_COLOR_FRAME, -1);
        this.entityData.define(GLOW_COLOR_FRAME, -1);
        this.entityData.define(MAIN_COLOR_FRONT_WHEEL, -1);
        this.entityData.define(GLOW_COLOR_FRONT_WHEEL, -1);
        this.entityData.define(MAIN_COLOR_REAR_WHEEL, -1);
        this.entityData.define(GLOW_COLOR_REAR_WHEEL, -1);
        this.entityData.define(MAIN_COLOR_FRONT_LIGHT, -1);
        this.entityData.define(GLOW_COLOR_FRONT_LIGHT, -1);
        this.entityData.define(MAIN_COLOR_REAR_LIGHT, -1);
        this.entityData.define(GLOW_COLOR_REAR_LIGHT, -1);
        this.entityData.define(MAIN_COLOR_BATTERY, -1);
        this.entityData.define(GLOW_COLOR_BATTERY, -1);
        this.entityData.define(MAIN_COLOR_PISTON_FRONT, -1);
        this.entityData.define(GLOW_COLOR_PISTON_FRONT, -1);
        this.entityData.define(MAIN_COLOR_PISTON_REAR, -1);
        this.entityData.define(GLOW_COLOR_PISTON_REAR, -1);
        this.entityData.define(MAIN_COLOR_KEY, -1);
        this.entityData.define(GLOW_COLOR_KEY, -1);
    }

    private int computeDefaultInstalledMask() {
        int mask = 0;
        for (DusterbikePartType type : DusterbikePartType.values()) {
            if (type.isRemovable()) {
                mask |= (1 << type.ordinal());
            }
        }
        return mask;
    }

    private void updateInstalledMask() {
        int mask = 0;
        for (DusterbikePartState state : engineState.parts()) {
            if (state.installed()) {
                mask |= (1 << state.type().ordinal());
            }
        }
        this.entityData.set(INSTALLED_PARTS_MASK, mask);
    }

    private EntityDataAccessor<Integer> getMainColorAccessor(DusterbikePartType type) {
        return switch (type) {
            case FRAME -> MAIN_COLOR_FRAME;
            case FRONT_WHEEL -> MAIN_COLOR_FRONT_WHEEL;
            case REAR_WHEEL -> MAIN_COLOR_REAR_WHEEL;
            case FRONT_LIGHT -> MAIN_COLOR_FRONT_LIGHT;
            case REAR_LIGHT -> MAIN_COLOR_REAR_LIGHT;
            case BATTERY -> MAIN_COLOR_BATTERY;
            case PISTON_FRONT -> MAIN_COLOR_PISTON_FRONT;
            case PISTON_REAR -> MAIN_COLOR_PISTON_REAR;
            case KEY -> MAIN_COLOR_KEY;
            default -> null;
        };
    }

    private EntityDataAccessor<Integer> getGlowColorAccessor(DusterbikePartType type) {
        return switch (type) {
            case FRAME -> GLOW_COLOR_FRAME;
            case FRONT_WHEEL -> GLOW_COLOR_FRONT_WHEEL;
            case REAR_WHEEL -> GLOW_COLOR_REAR_WHEEL;
            case FRONT_LIGHT -> GLOW_COLOR_FRONT_LIGHT;
            case REAR_LIGHT -> GLOW_COLOR_REAR_LIGHT;
            case BATTERY -> GLOW_COLOR_BATTERY;
            case PISTON_FRONT -> GLOW_COLOR_PISTON_FRONT;
            case PISTON_REAR -> GLOW_COLOR_PISTON_REAR;
            case KEY -> GLOW_COLOR_KEY;
            default -> null;
        };
    }

    public Integer getPartMainColor(DusterbikePartType type) {
        EntityDataAccessor<Integer> accessor = getMainColorAccessor(type);
        int value = accessor != null ? this.entityData.get(accessor) : -1;
        return value >= 0 ? value : null;
    }

    public Integer getPartGlowColor(DusterbikePartType type) {
        EntityDataAccessor<Integer> accessor = getGlowColorAccessor(type);
        int value = accessor != null ? this.entityData.get(accessor) : -1;
        return value >= 0 ? value : null;
    }

    private void updateSyncedColors() {
        for (DusterbikePartType type : DusterbikePartType.values()) {
            EntityDataAccessor<Integer> mainAcc = getMainColorAccessor(type);
            EntityDataAccessor<Integer> glowAcc = getGlowColorAccessor(type);
            DusterbikePartState state = engineState.part(type);
            if (mainAcc != null) {
                Integer color = state.mainColor();
                this.entityData.set(mainAcc, color != null ? color : -1);
            }
            if (glowAcc != null) {
                Integer color = state.glowColor();
                this.entityData.set(glowAcc, color != null ? color : -1);
            }
        }
    }

    public DusterbikeEngineState getEngineState() { return engineState; }
    public DusterbikePartState getPartState(DusterbikePartType type) { return engineState.part(type); }
    public int getFuelMilliBuckets() { return this.entityData.get(FUEL_MILLI_BUCKETS); }
    public float getFuelRatio() {
        return Mth.clamp(getFuelMilliBuckets() / (float) DusterbikeEngineState.BIKE_FUEL_CAPACITY_MB, 0.0F, 1.0F);
    }
    public void setFuelMilliBuckets(int amount) {
        engineState.setFuelMilliBuckets(amount);
        this.entityData.set(FUEL_MILLI_BUCKETS, engineState.fuelMilliBuckets());
    }
    public int addFuelMilliBuckets(int amount) {
        int accepted = engineState.addFuel(amount);
        this.entityData.set(FUEL_MILLI_BUCKETS, engineState.fuelMilliBuckets());
        return accepted;
    }
    public int removeFuelMilliBuckets(int amount) {
        int removed = engineState.removeFuel(amount);
        this.entityData.set(FUEL_MILLI_BUCKETS, engineState.fuelMilliBuckets());
        return removed;
    }
    public int getFrameHealth() { return this.entityData.get(FRAME_HEALTH); }

    public boolean areHeadlightsOn() {
        return this.entityData.get(HEADLIGHTS_ON) != 0 && hasUsable(DusterbikePartType.FRONT_LIGHT);
    }
    public boolean isLeftBlinkerLit() {
        return hasUsable(DusterbikePartType.FRONT_LIGHT)
                && hasUsable(DusterbikePartType.REAR_LIGHT)
                && inputLeft
                && (tickCount / 10) % 2 == 0;
    }
    public boolean isRightBlinkerLit() {
        return hasUsable(DusterbikePartType.FRONT_LIGHT)
                && hasUsable(DusterbikePartType.REAR_LIGHT)
                && inputRight
                && (tickCount / 10) % 2 == 0;
    }
    public boolean isStopLightLit() {
        return hasUsable(DusterbikePartType.REAR_LIGHT) && (inputBackward || inputHandbrake);
    }

    public boolean isPartInstalled(DusterbikePartType type) {
        if (type == null) return false;
        int mask = this.entityData.get(INSTALLED_PARTS_MASK);
        return (mask & (1 << type.ordinal())) != 0;
    }

    public void toggleHeadlights() {
        if (level().isClientSide || !hasUsable(DusterbikePartType.FRONT_LIGHT)) {
            return;
        }
        boolean next = this.entityData.get(HEADLIGHTS_ON) == 0;
        this.entityData.set(HEADLIGHTS_ON, (byte) (next ? 1 : 0));
        if (next) {
            maybeDamagePart(DusterbikePartType.FRONT_LIGHT, 0.25F);
        }
    }

    private void setFrameHealth(int health) {
        engineState.setFrameHealth(health);
        this.entityData.set(FRAME_HEALTH, engineState.frameHealth());
    }
    private void damageFrame(float amount) {
        engineState.damageFrame(amount);
        this.entityData.set(FRAME_HEALTH, engineState.frameHealth());
    }

    public boolean isEngineRunning() { return this.entityData.get(ENGINE_RUNNING) != 0; }

    private void setEngineRunning(boolean running) {
        this.entityData.set(ENGINE_RUNNING, (byte) (running ? 1 : 0));
    }
    public boolean isKeyCranking() { return this.entityData.get(KEY_CRANKING) != 0; }
    private void setKeyCranking(boolean cranking) {
        this.entityData.set(KEY_CRANKING, (byte) (cranking ? 1 : 0));
    }
    private void syncKeyCrankingFromHoldState() {
        setKeyCranking(keyHeldByPlayerId != NO_KEY_HOLDER);
    }

    public void releaseKeyHoldForPlayer(Player player) {
        if (this.level().isClientSide) return;
        if (keyHeldByPlayerId == player.getId()) {
            handleKeyInteraction(player, false, false);
        }
    }
    public void handleKeyInteraction(Player player, boolean holding, boolean pressed) {
        if (this.level().isClientSide) return;
        if (pressed) return;
        if (!holding) {
            keyHeldByPlayerId = NO_KEY_HOLDER;
            setKeyCranking(false);
            if (!isEngineRunning()) cancelIgnitionSequence();
            return;
        }
        keyHeldByPlayerId = player.getId();
        setKeyCranking(true);
        if (isEngineRunning()) {
            shutOffEngineFromKeyTurn();
            return;
        }
        if (ignitionPhase == IgnitionPhase.NONE) {
            if (canBeginIgnition(player)) beginIgnitionSequence();
        }
    }

    private boolean canBeginIgnition(Player player) {
        if (engineState.insertedKeyBikeUuid() == null) return false;
        if (!hasUsable(DusterbikePartType.ENGINE)) return false;
        if (!hasUsable(DusterbikePartType.BATTERY)) return false;
        if (!hasUsable(DusterbikePartType.PISTON_FRONT) || !hasUsable(DusterbikePartType.PISTON_REAR)) return false;
        if (!hasUsable(DusterbikePartType.SPARK_PLUG_FRONT) || !hasUsable(DusterbikePartType.SPARK_PLUG_REAR)) return false;
        if (getIgnitionAttemptCap() <= 0) return false;
        return getFuelMilliBuckets() > 0;
    }

    private boolean hasUsable(DusterbikePartType type) { return engineState.hasUsable(type); }

    public void handlePartInteraction(Player player, InteractionHand hand, DusterbikePartTargetType targetType, boolean secondaryUse) {
        if (this.level().isClientSide) return;
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(ItemRegistry.WRENCH.get())) {
            handleWrenchInteraction(player, hand, stack, targetType, secondaryUse);
            return;
        }
        if (stack.getItem() instanceof JerrycanItem && targetType == DusterbikePartTargetType.FUEL_INTAKE) {
            handleFuelTransfer(player, stack, secondaryUse);
            return;
        }
        if (stack.getItem() instanceof SprayCanItem) {
            handleSprayInteraction(player, stack, targetType, secondaryUse);
            return;
        }
        if (handlePartInstall(player, hand, stack, targetType)) return;
    }

    private void handleWrenchInteraction(Player player, InteractionHand hand, ItemStack wrench, DusterbikePartTargetType targetType, boolean secondaryUse) {
        DusterbikePartType partType = targetType.partType();
        if (partType == null) return;
        DusterbikePartState state = getPartState(partType);
        if (!state.installed()) return;

        if (!secondaryUse) {
            sendActionBar(player, partType.serializedName() + ": " + state.durability() + " / " + state.maxDurability());
            damageHeldTool(player, hand, wrench);
            return;
        }

        if (partType == DusterbikePartType.ENGINE) {
            EngineHoistEntity hoist = findEmptyEngineHoist();
            if (hoist != null) {
                hoist.setEngineState(engineState);
                for (DusterbikePartType t : new DusterbikePartType[]{
                        DusterbikePartType.ENGINE,
                        DusterbikePartType.PISTON_FRONT,
                        DusterbikePartType.PISTON_REAR,
                        DusterbikePartType.SPARK_PLUG_FRONT,
                        DusterbikePartType.SPARK_PLUG_REAR,
                        DusterbikePartType.KEY
                }) {
                    getPartState(t).setInstalled(false);
                }
                setEngineRunning(false);
                updateInstalledMask();
            }
            damageHeldTool(player, hand, wrench);
            return;
        }

        if (!partType.hasItemForm() || !partType.isRemovable()) return;
        if (isPistonBlockedBySparkPlug(partType)) return;

        ItemStack removed = DusterbikePartItems.createPartStack(state);
        state.setInstalled(false);
        updateInstalledMask();

        if (!player.addItem(removed)) spawnAtLocation(removed);
        damageHeldTool(player, hand, wrench);
    }

    public void handleWheelWrenchInteraction(Player player, InteractionHand hand, ItemStack wrench, DusterbikeWheelEntity.WheelType wheelType, boolean secondaryUse) {
        DusterbikePartType partType = wheelType == DusterbikeWheelEntity.WheelType.FRONT ? DusterbikePartType.FRONT_WHEEL : DusterbikePartType.REAR_WHEEL;
        DusterbikePartState state = getPartState(partType);
        if (!secondaryUse) {
            sendActionBar(player, partType.serializedName() + ": " + state.durability() + " / " + state.maxDurability());
            damageHeldTool(player, hand, wrench);
            return;
        }
        if (!state.installed()) {
            damageHeldTool(player, hand, wrench);
            return;
        }
        ItemStack removed = DusterbikePartItems.createPartStack(state);
        state.setInstalled(false);
        updateInstalledMask();
        if (!player.addItem(removed)) spawnAtLocation(removed);
        damageHeldTool(player, hand, wrench);
    }

    public void handleKeyPortInteraction(Player player, InteractionHand hand) {
        if (this.level().isClientSide) return;
        ItemStack stack = player.getItemInHand(hand);
        if (handleKeyPortItemInteraction(player, hand, stack, player.isSecondaryUseActive())) return;
        if (stack.getItem() instanceof SprayCanItem) {
            handleKeySprayInteraction(player, stack, player.isSecondaryUseActive());
        }
    }

    private void handleKeySprayInteraction(Player player, ItemStack sprayCan, boolean secondaryUse) {
        if (!(sprayCan.getItem() instanceof DyeableLeatherItem dyeable)) return;
        DusterbikePartState state = getPartState(DusterbikePartType.KEY);
        int color = dyeable.getColor(sprayCan) & 0xFFFFFF;
        if (secondaryUse) {
            state.setGlowColor(color);
            sendActionBar(player, DusterbikePartType.KEY.serializedName() + " glow color set");
        } else {
            state.setMainColor(color);
            sendActionBar(player, DusterbikePartType.KEY.serializedName() + " main color set");
        }
        updateSyncedColors();
    }

    private void handleSprayInteraction(
            Player player,
            ItemStack sprayCan,
            DusterbikePartTargetType targetType,
            boolean secondaryUse) {
        if (!(sprayCan.getItem() instanceof DyeableLeatherItem dyeable)) return;
        DusterbikePartType partType = targetType.partType();
        if (partType == null) partType = DusterbikePartType.FRAME;
        DusterbikePartState state = getPartState(partType);
        int color = dyeable.getColor(sprayCan) & 0xFFFFFF;
        if (secondaryUse) {
            state.setGlowColor(color);
            sendActionBar(player, partType.serializedName() + " glow color set");
        } else {
            state.setMainColor(color);
            sendActionBar(player, partType.serializedName() + " main color set");
        }
        updateSyncedColors();
    }

    public void handleWheelSprayInteraction(Player player, ItemStack sprayCan,
                                            DusterbikeWheelEntity.WheelType wheelType,
                                            boolean secondaryUse) {
        if (!(sprayCan.getItem() instanceof DyeableLeatherItem dyeable)) return;
        DusterbikePartType partType = wheelType == DusterbikeWheelEntity.WheelType.FRONT
                ? DusterbikePartType.FRONT_WHEEL : DusterbikePartType.REAR_WHEEL;
        DusterbikePartState state = getPartState(partType);
        int color = dyeable.getColor(sprayCan) & 0xFFFFFF;

        if (secondaryUse) {
            state.setGlowColor(color);
            sendActionBar(player, partType.serializedName() + " glow color set");
        } else {
            state.setMainColor(color);
            sendActionBar(player, partType.serializedName() + " main color set");
        }
        updateSyncedColors();
    }

    private boolean handleKeyPortItemInteraction(Player player, InteractionHand hand, ItemStack stack, boolean secondaryUse) {
        if (!stack.isEmpty() && stack.getItem() instanceof BikeKeyItem) {
            if (!BikeKeyItem.hasLinkedBike(stack)) {
                if (!player.getAbilities().instabuild) return true;
                BikeKeyItem.setLinkedBike(stack, this.getUUID());
            }
            UUID linkedBike = BikeKeyItem.getLinkedBike(stack);
            if (!this.getUUID().equals(linkedBike)) return true;
            if (engineState.insertedKeyBikeUuid() != null) return true;

            engineState.setInsertedKeyBikeUuid(linkedBike);
            DusterbikePartState keyState = getPartState(DusterbikePartType.KEY);
            keyState.setInstalled(true);

            if (stack.getItem() instanceof DyeableLeatherItem dyeable && dyeable.hasCustomColor(stack)) {
                keyState.setMainColor(dyeable.getColor(stack));
            }
            updateInstalledMask();
            updateSyncedColors();
            if (!player.getAbilities().instabuild) stack.shrink(1);
            return true;
        }

        if (stack.isEmpty() && secondaryUse && engineState.insertedKeyBikeUuid() != null) {
            ItemStack keyStack = new ItemStack(ItemRegistry.BIKE_KEY.get());
            BikeKeyItem.setLinkedBike(keyStack, engineState.insertedKeyBikeUuid());
            Integer keyColor = getPartState(DusterbikePartType.KEY).mainColor();
            if (keyColor != null && keyStack.getItem() instanceof DyeableLeatherItem dyeable) {
                dyeable.setColor(keyStack, keyColor);
            }
            DusterbikePartState keyState = getPartState(DusterbikePartType.KEY);
            keyState.setInstalled(false);
            engineState.setInsertedKeyBikeUuid(null);
            updateInstalledMask();

            if (!player.addItem(keyStack)) spawnAtLocation(keyStack);
            return true;
        }
        return false;
    }

    private boolean handlePartInstall(Player player, InteractionHand hand, ItemStack stack, DusterbikePartTargetType targetType) {
        DusterbikePartType partType = targetType.partType();
        if (partType == null || stack.isEmpty() || !partType.hasItemForm() || !stack.is(partType.item())) return false;
        DusterbikePartState state = getPartState(partType);
        if (state.installed()) return true;
        DusterbikePartItems.applyStackToState(stack, state);
        updateInstalledMask();
        updateSyncedColors();
        if (!player.getAbilities().instabuild) stack.shrink(1);
        return true;
    }

    private void handleFuelTransfer(Player player, ItemStack jerrycan, boolean drainBike) {
        int moved;
        if (drainBike) {
            int removedFromBike = removeFuelMilliBuckets(1000);
            moved = JerrycanItem.addFuel(jerrycan, removedFromBike);
            if (moved < removedFromBike) addFuelMilliBuckets(removedFromBike - moved);
            sendActionBar(player, moved > 0 ? "fuel tank: drained " + moved + " mB" : "fuel tank: no fuel moved");
            return;
        }
        int removedFromCan = JerrycanItem.removeFuel(jerrycan, 1000);
        moved = addFuelMilliBuckets(removedFromCan);
        if (moved < removedFromCan) JerrycanItem.addFuel(jerrycan, removedFromCan - moved);
        sendActionBar(player, moved > 0 ? "fuel tank: filled " + moved + " mB" : "fuel tank: no fuel moved");
    }

    private boolean isPistonBlockedBySparkPlug(DusterbikePartType partType) {
        return (partType == DusterbikePartType.PISTON_FRONT && getPartState(DusterbikePartType.SPARK_PLUG_FRONT).installed())
                || (partType == DusterbikePartType.PISTON_REAR && getPartState(DusterbikePartType.SPARK_PLUG_REAR).installed());
    }

    private EngineHoistEntity findEmptyEngineHoist() {
        EngineHoistEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (EngineHoistEntity hoist : level().getEntitiesOfClass(EngineHoistEntity.class, getBoundingBox().inflate(8.0D))) {
            if (!hoist.isEmpty()) continue;
            double distance = distanceToSqr(hoist);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = hoist;
            }
        }
        return nearest;
    }

    private void damageHeldTool(Player player, InteractionHand hand, ItemStack stack) {
        stack.hurtAndBreak(1, player, brokenPlayer -> brokenPlayer.broadcastBreakEvent(hand));
    }
    private static void sendActionBar(Player player, String message) {
        player.displayClientMessage(Component.literal(message), true);
    }

    private void beginIgnitionSequence() {
        ignitionPhase = IgnitionPhase.STARTING;
        ignitionTicksRemaining = DusterbikeEngineSoundConstants.IGNITION_START_TICKS;
        engineState.setCurrentIgnitionDoomed(this.random.nextFloat() < getDoomedIgnitionChance());
        playSound(SoundRegistry.DUSTERBIKE_IGNITION_START.get(), 1.0F, 1.0F);
    }
    private void cancelIgnitionSequence() {
        ignitionPhase = IgnitionPhase.NONE;
        ignitionTicksRemaining = 0;
        ignitionAttempts = 0;
        syncKeyCrankingFromHoldState();
    }
    private void shutOffEngineFromKeyTurn() {
        setEngineRunning(false);
        ignitionPhase = IgnitionPhase.NONE;
        ignitionTicksRemaining = 0;
        ignitionAttempts = 0;
        playSound(SoundRegistry.DUSTERBIKE_ENGINE_OFF.get(), 1.0F, 1.0F);
    }

    private void tickFuelConsumption() {
        if (!isEngineRunning()) { fuelConsumptionTicks = 0; return; }
        fuelConsumptionTicks++;
        if (fuelConsumptionTicks < 20) return;
        fuelConsumptionTicks = 0;
        float speedMultiplier = Math.max(1.0F, Math.abs(getDriveForwardSpeed()) * 2.0F);
        int consumed = Math.max(1, Math.round(8.0F * speedMultiplier));
        int removed = removeFuelMilliBuckets(consumed);
        if (removed < consumed) {
            setEngineRunning(false);
            playSound(SoundRegistry.DUSTERBIKE_ENGINE_OFF.get(), 1.0F, 1.0F);
        }
    }

    private void tickPartWear() {
        if (isEngineRunning()) {
            engineWearTicks++;
            if (engineWearTicks >= 100) {
                engineWearTicks = 0;
                maybeDamagePart(DusterbikePartType.PISTON_FRONT, 0.25F);
                maybeDamagePart(DusterbikePartType.PISTON_REAR, 0.25F);
                maybeDamagePart(DusterbikePartType.SPARK_PLUG_FRONT, 0.25F);
                maybeDamagePart(DusterbikePartType.SPARK_PLUG_REAR, 0.25F);
            }
        } else {
            engineWearTicks = 0;
        }
        if (Math.abs(getDriveForwardSpeed()) > DusterbikePhysics.SPEED_EPSILON) {
            wheelWearTicks++;
            if (wheelWearTicks >= 100) {
                wheelWearTicks = 0;
                maybeDamagePart(DusterbikePartType.FRONT_WHEEL, 0.25F);
                maybeDamagePart(DusterbikePartType.REAR_WHEEL, 0.25F);
            }
        } else {
            wheelWearTicks = 0;
        }
    }
    private void maybeDamagePart(DusterbikePartType type, float chance) {
        if (this.random.nextFloat() >= chance) return;
        DusterbikePartState state = getPartState(type);
        state.damage(1);
    }

    private void tickIgnition() {
        if (ignitionPhase == IgnitionPhase.NONE) {
            syncKeyCrankingFromHoldState();
            return;
        }
        if (keyHeldByPlayerId == NO_KEY_HOLDER) {
            cancelIgnitionSequence();
            return;
        }
        ignitionTicksRemaining--;
        if (ignitionTicksRemaining > 0) return;
        if (ignitionPhase == IgnitionPhase.STARTING) {
            resolveIgnitionAttempt();
        } else if (ignitionPhase == IgnitionPhase.COOLDOWN) {
            if (keyHeldByPlayerId != NO_KEY_HOLDER) resolveIgnitionAttempt();
            else ignitionPhase = IgnitionPhase.NONE;
        }
    }
    private void resolveIgnitionAttempt() {
        ignitionAttempts++;
        maybeDamagePart(DusterbikePartType.BATTERY, 0.25F);
        int attemptCap = getIgnitionAttemptCap();
        boolean success = !engineState.currentIgnitionDoomed()
                && (ignitionAttempts >= attemptCap || this.random.nextFloat() < DusterbikeEngineSoundConstants.IGNITION_SUCCESS_CHANCE);
        if (success) {
            setEngineRunning(true);
            playSound(SoundRegistry.DUSTERBIKE_IGNITION_SUCCESS.get(), 1.0F, 1.0F);
            ignitionPhase = IgnitionPhase.NONE;
            ignitionTicksRemaining = 0;
            ignitionAttempts = 0;
            syncKeyCrankingFromHoldState();
            return;
        }
        playSound(SoundRegistry.DUSTERBIKE_IGNITION_ATTEMPT.get(), 1.0F, 1.0F);
        ignitionPhase = IgnitionPhase.COOLDOWN;
        ignitionTicksRemaining = DusterbikeEngineSoundConstants.IGNITION_ATTEMPT_TICKS;
    }

    private int getIgnitionAttemptCap() {
        float front = partCondition(DusterbikePartType.SPARK_PLUG_FRONT);
        float rear = partCondition(DusterbikePartType.SPARK_PLUG_REAR);
        return Mth.clamp(Math.round(((front + rear) * 0.5F) * 10.0F), 0, 10);
    }
    private float getDoomedIgnitionChance() {
        float front = partCondition(DusterbikePartType.SPARK_PLUG_FRONT);
        float rear = partCondition(DusterbikePartType.SPARK_PLUG_REAR);
        return Mth.clamp(1.0F - ((front + rear) * 0.5F), 0.0F, 1.0F);
    }
    private float partCondition(DusterbikePartType type) {
        DusterbikePartState state = getPartState(type);
        if (!state.installed() || state.maxDurability() <= 0) return 0.0F;
        return Mth.clamp(state.durability() / (float) state.maxDurability(), 0.0F, 1.0F);
    }

    public float getSyncedPitch() { return this.entityData.get(PITCH); }
    private void setSyncedPitch(float pitch) { this.entityData.set(PITCH, pitch); }
    public float getRenderPitch(float partialTick) { return Mth.lerp(partialTick, previousRenderPitch, renderPitch); }
    public float getSyncedSteerAngle() { return this.entityData.get(STEER_ANGLE); }
    private void setSyncedSteerAngle(float steer) {
        this.steerAngle = steer;
        if (!usesClientDriveAuthority()) this.entityData.set(STEER_ANGLE, steer);
    }
    public float getRenderSteer(float partialTick) { return Mth.lerp(partialTick, previousRenderSteer, renderSteer); }
    public float getRenderRoll(float partialTick) {
        float steer = getRenderSteer(partialTick);
        float speed = getDriveForwardSpeed();
        float maxSteer = DusterbikePhysics.computeMaxSteerDegrees(Math.abs(speed));
        return DusterbikePhysics.computeRollDegrees(speed, steer, maxSteer);
    }
    public float getRenderYaw(float partialTick) { return Mth.rotLerp(partialTick, this.yRotO, this.getYRot()); }
    public float getSyncedForwardSpeed() { return this.entityData.get(FORWARD_SPEED); }
    public float getDriveForwardSpeed() {
        if (usesClientDriveAuthority()) return this.forwardSpeed;
        return getSyncedForwardSpeed();
    }
    private boolean usesClientDriveAuthority() { return hasLocalDriver(); }
    private boolean hasLocalDriver() {
        if (!this.level().isClientSide) return false;
        LivingEntity passenger = getControllingPassenger();
        return passenger != null && passenger.isControlledByLocalInstance();
    }
    private void setSyncedForwardSpeed(float speed) {
        this.forwardSpeed = speed;
        if (!usesClientDriveAuthority()) this.entityData.set(FORWARD_SPEED, speed);
    }
    private void publishDriveStateToServer() {
        if (!usesClientDriveAuthority()) return;
        sendDriveStateToServer();
    }
    public void flushDriveStateBeforeDisconnect() {
        if (this.level().isClientSide) {
            commitWheelRotationToRenderState(getFrontWheel(), getRearWheel());
            sendDriveStateToServer();
        }
    }
    public void applyRiderInput(boolean forward, boolean backward, boolean left, boolean right, boolean handbrake) {
        this.inputForward = forward;
        this.inputBackward = backward;
        this.inputLeft = left;
        this.inputRight = right;
        this.inputHandbrake = handbrake;
    }

    public DusterbikeGear getGear() {
        if (this.level().isClientSide && getControllingPassenger() != null && this.driverGear != null) {
            return this.driverGear;
        }
        return gearFromSyncedData();
    }
    private DusterbikeGear gearFromSyncedData() {
        byte ordinal = this.entityData.get(GEAR);
        DusterbikeGear[] gears = DusterbikeGear.values();
        if (ordinal < 0 || ordinal >= gears.length) return DusterbikeGear.N;
        return gears[ordinal];
    }
    private void setGear(DusterbikeGear gear) {
        this.entityData.set(GEAR, (byte) gear.ordinal());
        if (this.level().isClientSide && getControllingPassenger() != null) this.driverGear = gear;
    }
    public void applyGearFromServer(DusterbikeGear gear) {
        if (!this.level().isClientSide) return;
        setGear(gear);
        this.driverGear = gear;
        this.forwardSpeed = DusterbikePhysics.clampSpeedForGear(this.forwardSpeed, gear);
        this.entityData.set(FORWARD_SPEED, this.forwardSpeed);
    }
    public boolean shiftGear(int direction) {
        DusterbikeGear current = getGear();
        DusterbikeGear next = current.shift(direction);
        if (next == current) return false;
        setGear(next);
        if (this.level().isClientSide) this.driverGear = next;
        setSyncedForwardSpeed(DusterbikePhysics.clampSpeedForGear(forwardSpeed, next));
        return true;
    }

    private void beginLocalControl() {
        this.driverGear = gearFromSyncedData();
        this.forwardSpeed = DusterbikePhysics.clampSpeedForGear(getSyncedForwardSpeed(), this.driverGear);
        this.steerAngle = getSyncedSteerAngle();
    }
    private void endLocalControl() {
        DusterbikeWheelEntity frontWheel = getFrontWheel();
        DusterbikeWheelEntity rearWheel = getRearWheel();
        commitWheelRotationToRenderState(frontWheel, rearWheel);
        sendDriveStateToServer();
        this.driverGear = null;
        this.inputForward = false;
        this.inputBackward = false;
        this.inputLeft = false;
        this.inputRight = false;
        this.inputHandbrake = false;
    }

    private void sendDriveStateToServer() {
        if (!this.level().isClientSide) return;
        DusterbikeGear gear = this.driverGear != null ? this.driverGear : getGear();
        DusterbikeWheelEntity frontWheel = getFrontWheel();
        DusterbikeWheelEntity rearWheel = getRearWheel();
        float frontRotation = frontWheel != null ? frontWheel.getSyncedRotationAngle() : savedFrontWheelRotation;
        float rearRotation = rearWheel != null ? rearWheel.getSyncedRotationAngle() : savedRearWheelRotation;
        this.entityData.set(FORWARD_SPEED, this.forwardSpeed);
        this.entityData.set(STEER_ANGLE, this.steerAngle);
        this.entityData.set(GEAR, (byte) gear.ordinal());
        this.entityData.set(FRONT_WHEEL_ROTATION, frontRotation);
        this.entityData.set(REAR_WHEEL_ROTATION, rearRotation);
        PacketHandlerRegistry.INSTANCE.sendToServer(new ServerBoundDusterbikeDrivePacket(
                getId(), (byte) gear.ordinal(), this.forwardSpeed, this.steerAngle, frontRotation, rearRotation));
    }
    public void applyClientDriveState(DusterbikeGear gear, float speed, float steer, float frontWheelRotation, float rearWheelRotation) {
        if (this.level().isClientSide) return;
        float previousSpeed = Math.abs(this.forwardSpeed);
        this.driverGear = null;
        this.entityData.set(GEAR, (byte) gear.ordinal());
        float clampedSpeed = DusterbikePhysics.clampSpeedForGear(speed, gear);
        if (Math.abs(clampedSpeed) <= DusterbikePhysics.SPEED_EPSILON && previousSpeed > DusterbikePhysics.SPEED_EPSILON) {
            pendingImpactSpeed = previousSpeed;
        } else if (Math.abs(clampedSpeed) > DusterbikePhysics.SPEED_EPSILON) {
            pendingImpactSpeed = 0.0F;
        }
        this.forwardSpeed = clampedSpeed;
        this.entityData.set(FORWARD_SPEED, this.forwardSpeed);
        this.steerAngle = steer;
        this.entityData.set(STEER_ANGLE, steer);
        applyClientWheelRotation(frontWheelRotation, rearWheelRotation);
    }

    public void handleServerWallImpactReport(Player player) {
        if (this.level().isClientSide || !(player instanceof ServerPlayer serverPlayer)) return;
        if (getControllingPassenger() != player) return;
        float impactSpeed = Math.abs(this.forwardSpeed);
        if (impactSpeed <= DusterbikePhysics.SPEED_EPSILON) impactSpeed = pendingImpactSpeed;
        pendingImpactSpeed = 0.0F;
        if (impactSpeed <= DusterbikePhysics.SPEED_EPSILON) return;
        float damage = DusterbikePhysics.computeWallImpactDamage(impactSpeed);
        this.forwardSpeed = 0.0F;
        this.entityData.set(FORWARD_SPEED, 0.0F);
        DusterbikeWheelEntity frontWheel = getFrontWheel();
        DusterbikeWheelEntity rearWheel = getRearWheel();
        if (frontWheel != null && rearWheel != null) haltWheelSpin(frontWheel, rearWheel);
        if (damage > 0.0F) {
            damageFrame(damage);
            if (getFrameHealth() <= 0) { destroyBikeAndDropParts(); return; }
            serverPlayer.hurt(DamageTypeRegistry.getSimpleDamageSource(level(), DamageTypeRegistry.DUSTERBIKE_IMPACT), damage);
        }
    }

    private void applyClientWheelRotation(float frontWheelRotation, float rearWheelRotation) {
        savedFrontWheelRotation = frontWheelRotation;
        savedRearWheelRotation = rearWheelRotation;
        this.entityData.set(FRONT_WHEEL_ROTATION, frontWheelRotation);
        this.entityData.set(REAR_WHEEL_ROTATION, rearWheelRotation);
        DusterbikeWheelEntity frontWheel = getFrontWheel();
        DusterbikeWheelEntity rearWheel = getRearWheel();
        if (frontWheel == null || rearWheel == null) return;
        frontWheel.setRotationAngle(frontWheelRotation);
        rearWheel.setRotationAngle(rearWheelRotation);
        if (Math.abs(forwardSpeed) <= DusterbikePhysics.SPEED_EPSILON) {
            savedFrontWheelAngularVelocity = 0.0D;
            savedRearWheelAngularVelocity = 0.0D;
            frontWheel.setAngularVelocity(0.0D);
            rearWheel.setAngularVelocity(0.0D);
        }
    }
    private void commitWheelRotationToRenderState(DusterbikeWheelEntity frontWheel, DusterbikeWheelEntity rearWheel) {
        if (!this.level().isClientSide || frontWheel == null || rearWheel == null) return;
        float frontRotation = frontWheel.getSyncedRotationAngle();
        float rearRotation = rearWheel.getSyncedRotationAngle();
        savedFrontWheelRotation = frontRotation;
        savedRearWheelRotation = rearRotation;
        this.entityData.set(FRONT_WHEEL_ROTATION, frontRotation);
        this.entityData.set(REAR_WHEEL_ROTATION, rearRotation);
        this.previousFrontWheelRotation = frontRotation;
        this.renderFrontWheelRotation = frontRotation;
        this.previousRearWheelRotation = rearRotation;
        this.renderRearWheelRotation = rearRotation;
    }
    private static boolean shouldPreferSyncedWheelRotation(DusterbikeWheelEntity wheel, float syncedRender) {
        return Math.abs(wheel.getSyncedRotationAngle()) < 0.001F && Math.abs(syncedRender) > 0.001F;
    }
    public float getFrontWheelRotation(float partialTick) {
        float syncedRender = Mth.lerp(partialTick, previousFrontWheelRotation, renderFrontWheelRotation);
        DusterbikeWheelEntity wheel = getFrontWheel();
        if (wheel == null || shouldPreferSyncedWheelRotation(wheel, syncedRender)) return syncedRender;
        return wheel.getRotationAngle(partialTick);
    }
    public float getRearWheelRotation(float partialTick) {
        float syncedRender = Mth.lerp(partialTick, previousRearWheelRotation, renderRearWheelRotation);
        DusterbikeWheelEntity wheel = getRearWheel();
        if (wheel == null || shouldPreferSyncedWheelRotation(wheel, syncedRender)) return syncedRender;
        return wheel.getRotationAngle(partialTick);
    }
    private void syncWheelRotationToClient(DusterbikeWheelEntity frontWheel, DusterbikeWheelEntity rearWheel) {
        float frontAngle = frontWheel.getSyncedRotationAngle();
        float rearAngle = rearWheel.getSyncedRotationAngle();
        savedFrontWheelRotation = frontAngle;
        savedRearWheelRotation = rearAngle;
        this.entityData.set(FRONT_WHEEL_ROTATION, frontAngle);
        this.entityData.set(REAR_WHEEL_ROTATION, rearAngle);
    }
    public float getGearMaxSpeedMagnitude() { return DusterbikePhysics.gearMaxSpeedMagnitude(getGear()); }

    @Override
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int posRotationIncrements, boolean teleport) {
        if (this.level().isClientSide && this.isControlledByLocalInstance()) return;
        super.lerpTo(x, y, z, yaw, pitch, posRotationIncrements, teleport);
    }

    private static double computeBodyOriginY(double frontCenterY, double rearCenterY) {
        double avgCenterY = (frontCenterY + rearCenterY) * 0.5D;
        return avgCenterY - DusterbikeTransforms.WHEEL_HALF_HEIGHT;
    }
    @Override
    protected AABB makeBoundingBox() {
        return DusterbikeTransforms.bodyColliderBox(getX(), getY(), getZ(), getYRot());
    }

    public int getFrontWheelId() { return this.entityData.get(FRONT_WHEEL_ID); }
    public int getRearWheelId() { return this.entityData.get(REAR_WHEEL_ID); }
    public void setWheelIds(int frontId, int rearId) {
        this.entityData.set(FRONT_WHEEL_ID, frontId);
        this.entityData.set(REAR_WHEEL_ID, rearId);
    }
    public void setWheelRefs(DusterbikeWheelEntity frontWheel, DusterbikeWheelEntity rearWheel) {
        setWheelIds(frontWheel.getId(), rearWheel.getId());
        this.entityData.set(FRONT_WHEEL_UUID, Optional.of(frontWheel.getUUID()));
        this.entityData.set(REAR_WHEEL_UUID, Optional.of(rearWheel.getUUID()));
        this.savedFrontUuid = frontWheel.getUUID();
        this.savedRearUuid = rearWheel.getUUID();
    }
    public DusterbikeWheelEntity getFrontWheel() {
        return getWheelEntity(getFrontWheelId(), this.entityData.get(FRONT_WHEEL_UUID).orElse(savedFrontUuid));
    }
    public DusterbikeWheelEntity getRearWheel() {
        return getWheelEntity(getRearWheelId(), this.entityData.get(REAR_WHEEL_UUID).orElse(savedRearUuid));
    }
    public int getKeyId() { return this.entityData.get(KEY_ID); }
    public DusterbikeKeyEntity getKeyEntity() {
        return getKeyEntity(getKeyId(), this.entityData.get(KEY_UUID).orElse(savedKeyUuid));
    }
    private DusterbikeKeyEntity getKeyEntity(int id, UUID uuid) {
        if (id != NO_LINKED_ENTITY) {
            Entity entity = this.level().getEntity(id);
            if (entity instanceof DusterbikeKeyEntity key) return key;
        }
        if (uuid == null) return null;
        for (DusterbikeKeyEntity key : this.level().getEntitiesOfClass(DusterbikeKeyEntity.class, this.getBoundingBox().inflate(16.0D))) {
            if (key.getUUID().equals(uuid)) return key;
        }
        return null;
    }
    public void setKeyRef(DusterbikeKeyEntity keyEntity) {
        this.entityData.set(KEY_ID, keyEntity.getId());
        this.entityData.set(KEY_UUID, Optional.of(keyEntity.getUUID()));
        this.savedKeyUuid = keyEntity.getUUID();
    }

    private Vec3 computeKeyWorldCenter() {
        float speed = usesClientDriveAuthority() ? forwardSpeed : getSyncedForwardSpeed();
        float steer = usesClientDriveAuthority() ? steerAngle : getSyncedSteerAngle();
        float pitch = getSyncedPitch();
        float maxSteer = DusterbikePhysics.computeMaxSteerDegrees(Math.abs(speed));
        float roll = DusterbikePhysics.computeRollDegrees(speed, steer, maxSteer);
        return DusterbikeTransforms.computeKeyWorldCenter(position(), getYRot(), pitch, roll);
    }
    private Vec3 computePartTargetWorldCenter(DusterbikePartTargetType targetType) {
        float speed = usesClientDriveAuthority() ? forwardSpeed : getSyncedForwardSpeed();
        float steer = usesClientDriveAuthority() ? steerAngle : getSyncedSteerAngle();
        float pitch = getSyncedPitch();
        float maxSteer = DusterbikePhysics.computeMaxSteerDegrees(Math.abs(speed));
        float roll = DusterbikePhysics.computeRollDegrees(speed, steer, maxSteer);
        return DusterbikeTransforms.computePartTargetWorldCenter(position(), getYRot(), pitch, roll, targetType);
    }
    private void syncKeyColliderPosition() {
        DusterbikeKeyEntity key = getKeyEntity();
        if (key == null) return;
        Vec3 center = computeKeyWorldCenter();
        key.syncColliderPosition(center.x, center.y, center.z);
    }
    private void syncPartTargetColliderPositions() {
        for (DusterbikePartInteractionEntity target : level().getEntitiesOfClass(DusterbikePartInteractionEntity.class, this.getBoundingBox().inflate(8.0D),
                t -> t.getParentUuid().map(this.getUUID()::equals).orElse(false) || t.getParentId() == getId())) {
            Vec3 center = computePartTargetWorldCenter(target.getTargetType());
            target.syncColliderPosition(center.x, center.y, center.z);
        }
    }

    private void ensureKeySpawned() {
        if (getKeyEntity() != null) return;
        Vec3 center = computeKeyWorldCenter();
        DusterbikeKeyEntity key = new DusterbikeKeyEntity(EntityRegistry.DUSTERBIKE_KEY.get(), level(), getId(), getUUID(),
                center.x, center.y, center.z);
        setKeyRef(key);
        level().addFreshEntity(key);
    }
    private void ensurePartTargetsSpawned() {
        for (DusterbikePartTargetType targetType : DusterbikePartTargetType.values()) {
            if (getPartTargetEntity(targetType) != null) continue;
            Vec3 center = computePartTargetWorldCenter(targetType);
            DusterbikePartInteractionEntity target = new DusterbikePartInteractionEntity(
                    EntityRegistry.DUSTERBIKE_PART_INTERACTION.get(), level(), getId(), getUUID(),
                    targetType, center.x, center.y, center.z);
            level().addFreshEntity(target);
        }
    }
    public DusterbikePartInteractionEntity getPartTargetEntity(DusterbikePartTargetType targetType) {
        for (DusterbikePartInteractionEntity target : level().getEntitiesOfClass(DusterbikePartInteractionEntity.class, this.getBoundingBox().inflate(8.0D))) {
            boolean matchesParent = target.getParentId() == getId() || target.getParentUuid().map(this.getUUID()::equals).orElse(false);
            if (matchesParent && target.getTargetType() == targetType) return target;
        }
        return null;
    }

    private DusterbikeWheelEntity getWheelEntity(int id, UUID uuid) {
        if (id != NO_LINKED_ENTITY) {
            Entity entity = this.level().getEntity(id);
            if (entity instanceof DusterbikeWheelEntity wheel) return wheel;
        }
        if (uuid == null) return null;
        for (DusterbikeWheelEntity wheel : this.level().getEntitiesOfClass(DusterbikeWheelEntity.class, this.getBoundingBox().inflate(16.0D))) {
            if (wheel.getUUID().equals(uuid)) return wheel;
        }
        return null;
    }

    public static DusterbikeEntity spawn(Level level, double x, double y, double z, float yaw) {
        DusterbikeEntity parent = EntityRegistry.DUSTERBIKE.get().create(level);
        if (parent == null) return null;
        parent.setYRot(yaw);
        parent.setPos(x, y, z);
        parent.engineState.setLinkedBikeUuid(parent.getUUID());

        Vec3 frontOffset = DusterbikeTransforms.rotateLocalOffset(DusterbikeTransforms.FRONT_WHEEL_LOCAL, yaw);
        Vec3 rearOffset = DusterbikeTransforms.rotateLocalOffset(DusterbikeTransforms.REAR_WHEEL_LOCAL, yaw);

        DusterbikeWheelEntity frontWheel = new DusterbikeWheelEntity(
                EntityRegistry.DUSTERBIKE_WHEEL.get(), level, parent.getId(), parent.getUUID(),
                DusterbikeWheelEntity.WheelType.FRONT,
                x + frontOffset.x, y + frontOffset.y, z + frontOffset.z);
        DusterbikeWheelEntity rearWheel = new DusterbikeWheelEntity(
                EntityRegistry.DUSTERBIKE_WHEEL.get(), level, parent.getId(), parent.getUUID(),
                DusterbikeWheelEntity.WheelType.REAR,
                x + rearOffset.x, y + rearOffset.y, z + rearOffset.z);

        Vec3 keyCenter = DusterbikeTransforms.computeKeyWorldCenter(parent.position(), parent.getYRot(), parent.getSyncedPitch(), 0.0F);
        DusterbikeKeyEntity key = new DusterbikeKeyEntity(
                EntityRegistry.DUSTERBIKE_KEY.get(), level, parent.getId(), parent.getUUID(),
                keyCenter.x, keyCenter.y, keyCenter.z);

        parent.setWheelRefs(frontWheel, rearWheel);
        parent.setKeyRef(key);
        parent.wheelsInitialized = true;
        parent.needsGroundSnap = true;

        level.addFreshEntity(frontWheel);
        level.addFreshEntity(rearWheel);
        level.addFreshEntity(key);
        level.addFreshEntity(parent);
        return parent;
    }

    private void ensureWheelsSpawned() {
        if (getFrontWheel() != null && getRearWheel() != null) {
            wheelsInitialized = true;
            ensureKeySpawned();
            return;
        }
        Vec3 pos = position();
        float yaw = getYRot();
        Vec3 frontOffset = DusterbikeTransforms.rotateSteeredWheelLocalOffset(DusterbikeTransforms.FRONT_WHEEL_LOCAL, steerAngle, yaw);
        Vec3 rearOffset = DusterbikeTransforms.rotateLocalOffset(DusterbikeTransforms.REAR_WHEEL_LOCAL, yaw);

        DusterbikeWheelEntity frontWheel = new DusterbikeWheelEntity(
                EntityRegistry.DUSTERBIKE_WHEEL.get(), level(), getId(), getUUID(),
                DusterbikeWheelEntity.WheelType.FRONT,
                pos.x + frontOffset.x, pos.y + frontOffset.y, pos.z + frontOffset.z);
        DusterbikeWheelEntity rearWheel = new DusterbikeWheelEntity(
                EntityRegistry.DUSTERBIKE_WHEEL.get(), level(), getId(), getUUID(),
                DusterbikeWheelEntity.WheelType.REAR,
                pos.x + rearOffset.x, pos.y + rearOffset.y, pos.z + rearOffset.z);

        setWheelRefs(frontWheel, rearWheel);
        level().addFreshEntity(frontWheel);
        level().addFreshEntity(rearWheel);
        applySavedWheelSpinToWheels(frontWheel, rearWheel);
        wheelsInitialized = true;
        ensureKeySpawned();
    }

    @Override
    public void tick() {
        super.tick();

        previousRenderPitch = renderPitch;
        renderPitch = getSyncedPitch();
        previousRenderSteer = renderSteer;
        renderSteer = usesClientDriveAuthority() ? steerAngle : getSyncedSteerAngle();
        previousFrontWheelRotation = renderFrontWheelRotation;
        previousRearWheelRotation = renderRearWheelRotation;
        renderFrontWheelRotation = this.entityData.get(FRONT_WHEEL_ROTATION);
        renderRearWheelRotation = this.entityData.get(REAR_WHEEL_ROTATION);

        if (!this.level().isClientSide) {
            if (!wheelsInitialized) ensureWheelsSpawned();
            relinkWheelsIfNeeded();
            relinkKeyIfNeeded();
            ensurePartTargetsSpawned();
            tickIgnition();
            tickFuelConsumption();
            tickPartWear();
        }

        DusterbikeWheelEntity frontWheel = getFrontWheel();
        DusterbikeWheelEntity rearWheel = getRearWheel();
        if (frontWheel == null || rearWheel == null) {
            if (!this.level().isClientSide) {
                missingWheelTicks++;
                if (missingWheelTicks > MISSING_WHEEL_GRACE_TICKS) discardWithWheels();
            }
            return;
        }
        if (!this.level().isClientSide) {
            missingWheelTicks = 0;
            ensureKeySpawned();
        }

        syncKeyColliderPosition();
        syncPartTargetColliderPositions();
        restoreWheelSpinStateIfNeeded(frontWheel, rearWheel);

        if (this.level().isClientSide) {
            frontWheel.beginRenderTick();
            rearWheel.beginRenderTick();
        }

        if (!this.level().isClientSide && needsGroundSnap) {
            snapToGround(frontWheel, rearWheel);
            needsGroundSnap = false;
        }

        boolean clientDriving = hasLocalDriver();
        if (!clientDriving) {
            forwardSpeed = getSyncedForwardSpeed();
            steerAngle = getSyncedSteerAngle();
        }

        boolean serverCoasting = !this.level().isClientSide && getControllingPassenger() == null
                && Math.abs(forwardSpeed) > DusterbikePhysics.SPEED_EPSILON;

        if (clientDriving) {
            if (!wasLocallyControlled) beginLocalControl();
            wasLocallyControlled = true;
            syncPacketPositionCodec(getX(), getY(), getZ());
            seedWheelContactHeights(frontWheel, rearWheel);
            runDriveSimulation(frontWheel, rearWheel);
        } else {
            if (!this.level().isClientSide) this.setDeltaMovement(Vec3.ZERO);
            if (this.level().isClientSide && wasLocallyControlled && getControllingPassenger() == null) {
                resetInterpolationToCurrentPosition();
                wasLocallyControlled = false;
                endLocalControl();
            }
        }

        if (serverCoasting) {
            runDriveSimulation(frontWheel, rearWheel);
            if (Math.abs(forwardSpeed) <= DusterbikePhysics.SPEED_EPSILON) {
                haltWheelSpin(frontWheel, rearWheel);
                this.entityData.set(FORWARD_SPEED, 0.0F);
            }
        }

        if (!clientDriving && !serverCoasting && !this.level().isClientSide) {
            updateWheelPhysics(frontWheel, rearWheel);
            updateBodyFromWheels(frontWheel, rearWheel);
            if (Math.abs(forwardSpeed) <= DusterbikePhysics.SPEED_EPSILON) haltWheelSpin(frontWheel, rearWheel);
        }

        if (!clientDriving && !serverCoasting && this.level().isClientSide && getControllingPassenger() != null) {
            updateWheelPhysics(frontWheel, rearWheel);
        }

        LivingEntity rider = getControllingPassenger();
        if (rider != null) {
            rider.setDeltaMovement(Vec3.ZERO);
            rider.fallDistance = 0.0F;
            DusterbikeRiderAnimation.syncRiderToBike(rider, this);
        }

        if (!this.level().isClientSide) tickRanOverEntityCollision();
    }

    private void haltWheelSpin(DusterbikeWheelEntity frontWheel, DusterbikeWheelEntity rearWheel) {
        frontWheel.setAngularVelocity(0.0D);
        rearWheel.setAngularVelocity(0.0D);
        if (!this.level().isClientSide) {
            frontWheel.setRotationAngle(frontWheel.getSyncedRotationAngle());
            rearWheel.setRotationAngle(rearWheel.getSyncedRotationAngle());
        }
        captureWheelSpinState(frontWheel, rearWheel);
    }
    private void snapToGround(DusterbikeWheelEntity frontWheel, DusterbikeWheelEntity rearWheel) {
        seedWheelContactHeights(frontWheel, rearWheel);
        updateWheelPhysics(frontWheel, rearWheel);
        double targetY = computeBodyOriginY(frontWheel.getContactY(), rearWheel.getContactY());
        setPos(getX(), targetY, getZ());
        setBoundingBox(makeBoundingBox());
        syncPacketPositionCodec(getX(), getY(), getZ());
        haltWheelSpin(frontWheel, rearWheel);
    }
    private void applyWallImpact(DusterbikeWheelEntity frontWheel, DusterbikeWheelEntity rearWheel) {
        float impactSpeed = forwardSpeed;
        float damage = DusterbikePhysics.computeWallImpactDamage(impactSpeed);
        setSyncedForwardSpeed(0.0F);
        forwardSpeed = 0.0F;
        haltWheelSpin(frontWheel, rearWheel);
        if (!this.level().isClientSide) this.entityData.set(FORWARD_SPEED, 0.0F);
        if (!this.level().isClientSide && damage > 0.0F) {
            damageFrame(damage);
            if (getFrameHealth() <= 0) { destroyBikeAndDropParts(); return; }
            LivingEntity rider = getControllingPassenger();
            if (rider != null) rider.hurt(DamageTypeRegistry.getSimpleDamageSource(level(), DamageTypeRegistry.DUSTERBIKE_IMPACT), damage);
        } else if (this.level().isClientSide && damage > 0.0F) {
            PacketHandlerRegistry.INSTANCE.sendToServer(new ServerBoundDusterbikeImpactPacket(getId()));
        }
    }
    private void tickRanOverEntityCollision() {
        float speed = Math.abs(getDriveForwardSpeed());
        if (speed <= DusterbikePhysics.SPEED_EPSILON) return;
        float damage = DusterbikePhysics.computeWallImpactDamage(speed);
        if (damage <= 0.0F) return;
        LivingEntity rider = getControllingPassenger();
        float yawRad = getYRot() * Mth.DEG_TO_RAD;
        Vec3 knockback = new Vec3(-Mth.sin(yawRad) * speed * 0.8D, 0.12D, Mth.cos(yawRad) * speed * 0.8D);
        for (Entity entity : level().getEntities(this, getBoundingBox().inflate(0.2D), e -> e instanceof LivingEntity)) {
            if (entity == rider || entity instanceof DusterbikeWheelEntity || entity instanceof DusterbikeKeyEntity
                    || entity instanceof DusterbikePartInteractionEntity) continue;
            entity.hurt(DamageTypeRegistry.getSimpleDamageSource(level(), DamageTypeRegistry.DUSTERBIKE_IMPACT), damage);
            entity.push(knockback.x, knockback.y, knockback.z);
        }
    }

    private static void seedWheelContactHeights(DusterbikeWheelEntity frontWheel, DusterbikeWheelEntity rearWheel) {
        frontWheel.setContactY(frontWheel.getY());
        rearWheel.setContactY(rearWheel.getY());
    }
    private void captureWheelSpinState(DusterbikeWheelEntity frontWheel, DusterbikeWheelEntity rearWheel) {
        savedFrontWheelRotation = frontWheel.getSyncedRotationAngle();
        savedFrontWheelAngularVelocity = frontWheel.getAngularVelocity();
        savedRearWheelRotation = rearWheel.getSyncedRotationAngle();
        savedRearWheelAngularVelocity = rearWheel.getAngularVelocity();
        if (!this.level().isClientSide) syncWheelRotationToClient(frontWheel, rearWheel);
    }
    private void applySavedWheelSpinToWheels(DusterbikeWheelEntity frontWheel, DusterbikeWheelEntity rearWheel) {
        frontWheel.applySpinState(savedFrontWheelRotation, savedFrontWheelAngularVelocity);
        rearWheel.applySpinState(savedRearWheelRotation, savedRearWheelAngularVelocity);
        this.entityData.set(FRONT_WHEEL_ROTATION, savedFrontWheelRotation);
        this.entityData.set(REAR_WHEEL_ROTATION, savedRearWheelRotation);
    }
    private void restoreWheelSpinStateIfNeeded(DusterbikeWheelEntity frontWheel, DusterbikeWheelEntity rearWheel) {
        if (!pendingWheelSpinRestore) return;
        if (Math.abs(forwardSpeed) <= DusterbikePhysics.SPEED_EPSILON && Math.abs(getSyncedForwardSpeed()) <= DusterbikePhysics.SPEED_EPSILON) {
            savedFrontWheelAngularVelocity = 0.0D;
            savedRearWheelAngularVelocity = 0.0D;
        }
        applySavedWheelSpinToWheels(frontWheel, rearWheel);
        pendingWheelSpinRestore = false;
        if (Math.abs(forwardSpeed) <= DusterbikePhysics.SPEED_EPSILON && Math.abs(getSyncedForwardSpeed()) <= DusterbikePhysics.SPEED_EPSILON) {
            haltWheelSpin(frontWheel, rearWheel);
        }
    }
    private void resetInterpolationToCurrentPosition() {
        this.xo = getX();
        this.yo = getY();
        this.zo = getZ();
        this.yRotO = getYRot();
        this.xRotO = getXRot();
    }

    private void runDriveSimulation(DusterbikeWheelEntity frontWheel, DusterbikeWheelEntity rearWheel) {
        updateWheelPhysics(frontWheel, rearWheel);
        tickDrivePhysics(frontWheel, rearWheel);
        updateWheelPhysics(frontWheel, rearWheel);
        updateBodyFromWheels(frontWheel, rearWheel);
    }
    private void tickDrivePhysics(DusterbikeWheelEntity frontWheel, DusterbikeWheelEntity rearWheel) {
        boolean rearGrounded = rearWheel.isGrounded();
        boolean frontGrounded = frontWheel.isGrounded();
        DusterbikeGear gear = getGear();
        boolean engineRunning = isEngineRunning();
        boolean hasFrontWheelPart = hasUsable(DusterbikePartType.FRONT_WHEEL);
        boolean hasRearWheelPart = hasUsable(DusterbikePartType.REAR_WHEEL);
        if (!hasRearWheelPart) {
            setSyncedForwardSpeed(0.0F);
            forwardSpeed = 0.0F;
            haltWheelSpin(frontWheel, rearWheel);
            publishDriveStateToServer();
            return;
        }

        boolean holdingForward = engineRunning && gear.allowsForwardThrottle() && inputForward && !inputBackward;

        if (inputHandbrake) {
            setSyncedForwardSpeed(DusterbikePhysics.applyCoastDrag(forwardSpeed, DusterbikePhysics.HANDBRAKE_DECEL));
        } else if (holdingForward) {
            if (forwardSpeed < 0.0F) {
                setSyncedForwardSpeed(DusterbikePhysics.applyCoastDrag(forwardSpeed, DusterbikePhysics.BRAKE_DECEL));
            } else {
                float accel = DusterbikePhysics.computeForwardThrottleAcceleration(forwardSpeed, gear);
                setSyncedForwardSpeed(DusterbikePhysics.approachSpeed(forwardSpeed, gear.maxSpeed(), accel));
            }
        } else if (engineRunning && gear.allowsReverseThrottle() && inputForward && !inputBackward && rearGrounded) {
            if (forwardSpeed > DusterbikePhysics.SPEED_EPSILON) {
                setSyncedForwardSpeed(DusterbikePhysics.applyCoastDrag(forwardSpeed, DusterbikePhysics.BRAKE_DECEL));
            } else {
                float accel = DusterbikePhysics.computeReverseThrottleAcceleration(forwardSpeed);
                setSyncedForwardSpeed(DusterbikePhysics.approachSpeed(forwardSpeed, -gear.maxSpeed(), accel));
            }
        } else if (gear.allowsForwardThrottle() && inputBackward && !inputForward && rearGrounded) {
            if (forwardSpeed > DusterbikePhysics.SPEED_EPSILON) {
                setSyncedForwardSpeed(DusterbikePhysics.applyCoastDrag(forwardSpeed, DusterbikePhysics.BRAKE_DECEL));
            } else {
                setSyncedForwardSpeed(DusterbikePhysics.applyCoastDrag(forwardSpeed, DusterbikePhysics.COAST_DRAG));
            }
        } else if (gear.allowsReverseThrottle() && inputBackward && !inputForward && rearGrounded) {
            if (forwardSpeed < -DusterbikePhysics.SPEED_EPSILON) {
                setSyncedForwardSpeed(DusterbikePhysics.applyCoastDrag(forwardSpeed, DusterbikePhysics.BRAKE_DECEL));
            } else {
                setSyncedForwardSpeed(DusterbikePhysics.applyCoastDrag(forwardSpeed, DusterbikePhysics.COAST_DRAG));
            }
        } else {
            if (!holdingForward) setSyncedForwardSpeed(DusterbikePhysics.applyCoastDrag(forwardSpeed, DusterbikePhysics.COAST_DRAG));
        }

        forwardSpeed = DusterbikePhysics.clampSpeedForGear(forwardSpeed, gear);
        if (!hasFrontWheelPart) {
            forwardSpeed = Mth.clamp(forwardSpeed, -0.08F, 0.18F);
            steerAngle *= 0.25F;
            this.entityData.set(STEER_ANGLE, steerAngle);
        }
        setSyncedForwardSpeed(forwardSpeed);

        if (Math.abs(forwardSpeed) <= DusterbikePhysics.SPEED_EPSILON) {
            haltWheelSpin(frontWheel, rearWheel);
        } else {
            if (frontGrounded) frontWheel.setAngularVelocity(DusterbikePhysics.linearSpeedToAngular(forwardSpeed));
            else frontWheel.applyAirDrag();

            if (rearGrounded) rearWheel.setAngularVelocity(DusterbikePhysics.linearSpeedToAngular(forwardSpeed));
            else {
                rearWheel.applyAirDrag();
                if (engineRunning && gear.allowsForwardThrottle() && inputForward && !inputBackward) {
                    rearWheel.setAngularVelocity(rearWheel.getAngularVelocity() - DusterbikePhysics.AIR_REAR_DRIVE_TORQUE);
                } else if (engineRunning && gear.allowsReverseThrottle() && inputForward && !inputBackward) {
                    rearWheel.setAngularVelocity(rearWheel.getAngularVelocity() + DusterbikePhysics.AIR_REAR_DRIVE_TORQUE);
                }
            }

            frontWheel.integrateRotation();
            rearWheel.integrateRotation();
            captureWheelSpinState(frontWheel, rearWheel);
        }

        applySteering();
        moveBodyHorizontally(frontWheel, rearWheel);
        publishDriveStateToServer();
    }

    private void applySteering() {
        float steerInput = 0.0F;
        if (inputLeft && !inputRight) steerInput = -1.0F;
        else if (inputRight && !inputLeft) steerInput = 1.0F;

        float absSpeed = Math.abs(forwardSpeed);
        float maxSteer = DusterbikePhysics.computeMaxSteerDegrees(absSpeed);
        if (steerInput != 0.0F) {
            setSyncedSteerAngle(DusterbikePhysics.approachSpeed(steerAngle, steerInput * maxSteer, DusterbikePhysics.STEER_RATE));
        } else if (hasLocalDriver()) {
            setSyncedSteerAngle(DusterbikePhysics.approachSpeed(steerAngle, 0.0F, DusterbikePhysics.STEER_RETURN_RATE));
        }

        if (absSpeed > DusterbikePhysics.SPEED_EPSILON) {
            float yawRate = DusterbikePhysics.computeYawRateDegrees(forwardSpeed, steerAngle);
            setYRot(getYRot() + yawRate);
        }
    }

    private void moveBodyHorizontally(DusterbikeWheelEntity frontWheel, DusterbikeWheelEntity rearWheel) {
        if (Math.abs(forwardSpeed) <= DusterbikePhysics.SPEED_EPSILON) return;
        float yawRad = getYRot() * Mth.DEG_TO_RAD;
        Vec3 delta = new Vec3(-Mth.sin(yawRad) * forwardSpeed, 0.0D, Mth.cos(yawRad) * forwardSpeed);

        double nextX = getX() + delta.x;
        double nextZ = getZ() + delta.z;
        float yaw = getYRot();

        Vec3 frontOffset = DusterbikeTransforms.rotateSteeredWheelLocalOffset(DusterbikeTransforms.FRONT_WHEEL_LOCAL, steerAngle, yaw);
        Vec3 rearOffset = DusterbikeTransforms.rotateLocalOffset(DusterbikeTransforms.REAR_WHEEL_LOCAL, yaw);

        boolean frontIsLead = frontOffset.dot(delta) > rearOffset.dot(delta);
        DusterbikeWheelEntity leadWheel = frontIsLead ? frontWheel : rearWheel;
        Vec3 leadOffset = frontIsLead ? frontOffset : rearOffset;
        float probeYaw = frontIsLead ? yaw + steerAngle : yaw;
        double leadFromX = leadWheel.getX();
        double leadFromZ = leadWheel.getZ();
        double leadNextX = nextX + leadOffset.x;
        double leadNextZ = nextZ + leadOffset.z;

        DusterbikePhysics.MovementAllowance allowance = DusterbikePhysics.probeMovementAllowance(
                level(), leadFromX, leadFromZ, leadNextX, leadNextZ, leadWheel.getContactY(), probeYaw);

        double travelFraction = allowance.fraction();
        if (travelFraction > 0.0D) setPos(getX() + delta.x * travelFraction, getY(), getZ() + delta.z * travelFraction);
        if (allowance.hitsWall()) applyWallImpact(frontWheel, rearWheel);
    }

    private void updateWheelPhysics(DusterbikeWheelEntity frontWheel, DusterbikeWheelEntity rearWheel) {
        Vec3 bodyPos = position();
        float yaw = getYRot();

        Vec3 frontOffset = DusterbikeTransforms.rotateSteeredWheelLocalOffset(DusterbikeTransforms.FRONT_WHEEL_LOCAL, steerAngle, yaw);
        Vec3 rearOffset = DusterbikeTransforms.rotateLocalOffset(DusterbikeTransforms.REAR_WHEEL_LOCAL, yaw);

        double frontTargetX = bodyPos.x + frontOffset.x;
        double frontTargetZ = bodyPos.z + frontOffset.z;
        double rearTargetX = bodyPos.x + rearOffset.x;
        double rearTargetZ = bodyPos.z + rearOffset.z;
        double frontRestY = bodyPos.y + DusterbikeTransforms.FRONT_WHEEL_LOCAL.y;
        double rearRestY = bodyPos.y + DusterbikeTransforms.REAR_WHEEL_LOCAL.y;

        DusterbikePhysics.WheelContactResult frontResult = DusterbikePhysics.probeGround(
                level(), frontTargetX, frontTargetZ, frontWheel.getContactY(), frontRestY, yaw + steerAngle);
        DusterbikePhysics.WheelContactResult rearResult = DusterbikePhysics.probeGround(
                level(), rearTargetX, rearTargetZ, rearWheel.getContactY(), rearRestY, yaw);

        double[] resolved = DusterbikePhysics.resolveAnchoredWheelHeights(
                frontWheel.getContactY(), frontResult.contactY(), frontRestY,
                rearWheel.getContactY(), rearResult.contactY(), rearRestY);

        frontWheel.syncColliderPosition(frontTargetX, resolved[0], frontTargetZ);
        rearWheel.syncColliderPosition(rearTargetX, resolved[1], rearTargetZ);

        frontWheel.setGrounded(frontResult.grounded());
        rearWheel.setGrounded(rearResult.grounded());

        DusterbikePhysics.BodyUpdateResult pitchResult = DusterbikePhysics.computePitch(frontWheel.getContactY(), rearWheel.getContactY());
        setSyncedPitch(pitchResult.pitchDegrees());
    }

    private void updateBodyFromWheels(DusterbikeWheelEntity frontWheel, DusterbikeWheelEntity rearWheel) {
        double targetY = computeBodyOriginY(frontWheel.getContactY(), rearWheel.getContactY());
        setPos(getX(), targetY, getZ());
        setBoundingBox(makeBoundingBox());
    }
    public void updateBodyFromWheels() {
        DusterbikeWheelEntity frontWheel = getFrontWheel();
        DusterbikeWheelEntity rearWheel = getRearWheel();
        if (frontWheel == null || rearWheel == null) return;
        updateWheelPhysics(frontWheel, rearWheel);
        updateBodyFromWheels(frontWheel, rearWheel);
    }

    private void relinkWheelsIfNeeded() {
        if (getFrontWheel() != null && getRearWheel() != null) return;
        UUID frontUuid = this.entityData.get(FRONT_WHEEL_UUID).orElse(savedFrontUuid);
        UUID rearUuid = this.entityData.get(REAR_WHEEL_UUID).orElse(savedRearUuid);
        if (frontUuid == null && rearUuid == null) return;
        int frontId = getFrontWheelId();
        int rearId = getRearWheelId();
        for (DusterbikeWheelEntity wheel : this.level().getEntitiesOfClass(DusterbikeWheelEntity.class, this.getBoundingBox().inflate(16.0D))) {
            if (frontUuid != null && wheel.getUUID().equals(frontUuid)) frontId = wheel.getId();
            if (rearUuid != null && wheel.getUUID().equals(rearUuid)) rearId = wheel.getId();
        }
        if (frontId != NO_LINKED_ENTITY || rearId != NO_LINKED_ENTITY) {
            setWheelIds(frontId == NO_LINKED_ENTITY ? getFrontWheelId() : frontId,
                    rearId == NO_LINKED_ENTITY ? getRearWheelId() : rearId);
        }
        if (getFrontWheel() != null && getRearWheel() != null) missingWheelTicks = 0;
    }
    private void relinkKeyIfNeeded() {
        if (getKeyEntity() != null) return;
        UUID keyUuid = this.entityData.get(KEY_UUID).orElse(savedKeyUuid);
        if (keyUuid == null) return;
        int keyId = getKeyId();
        for (DusterbikeKeyEntity key : this.level().getEntitiesOfClass(DusterbikeKeyEntity.class, this.getBoundingBox().inflate(16.0D))) {
            if (key.getUUID().equals(keyUuid)) { keyId = key.getId(); break; }
        }
        if (keyId != NO_LINKED_ENTITY) {
            this.entityData.set(KEY_ID, keyId);
            this.entityData.set(KEY_UUID, Optional.of(keyUuid));
            missingWheelTicks = 0;
        }
    }

    public void onKeyRemoved(DusterbikeKeyEntity key) { if (!discarding) discardWithWheels(); }
    public void onWheelRemoved(DusterbikeWheelEntity wheel) { if (!discarding) discardWithWheels(); }
    public void discardWithWheels() {
        if (discarding) return;
        discarding = true;
        DusterbikeWheelEntity frontWheel = getFrontWheel();
        DusterbikeWheelEntity rearWheel = getRearWheel();
        if (frontWheel != null && frontWheel.isAlive()) frontWheel.discard();
        if (rearWheel != null && rearWheel.isAlive()) rearWheel.discard();
        DusterbikeKeyEntity key = getKeyEntity();
        if (key != null && key.isAlive()) key.discard();
        discardPartTargets();
        this.discard();
    }
    private void destroyBikeAndDropParts() {
        if (discarding) return;
        for (DusterbikePartState state : engineState.parts()) {
            if (!state.installed() || !state.type().hasItemForm()) continue;
            ItemStack stack = DusterbikePartItems.createPartStack(state);
            if (!stack.isEmpty()) Block.popResource(level(), blockPosition(), stack);
            state.setInstalled(false);
        }
        updateInstalledMask();
        discardWithWheels();
    }
    private void discardPartTargets() {
        for (DusterbikePartInteractionEntity target : level().getEntitiesOfClass(DusterbikePartInteractionEntity.class,
                this.getBoundingBox().inflate(16.0D),
                t -> t.getParentId() == getId() || t.getParentUuid().map(this.getUUID()::equals).orElse(false))) {
            if (target.isAlive()) target.discard();
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide && !discarding && reason.shouldDestroy()) {
            discarding = true;
            DusterbikeWheelEntity frontWheel = getFrontWheel();
            DusterbikeWheelEntity rearWheel = getRearWheel();
            if (frontWheel != null && frontWheel.isAlive()) frontWheel.discard();
            if (rearWheel != null && rearWheel.isAlive()) rearWheel.discard();
            DusterbikeKeyEntity key = getKeyEntity();
            if (key != null && key.isAlive()) key.discard();
            discardPartTargets();
        }
        super.remove(reason);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("FrontWheel")) this.entityData.set(FRONT_WHEEL_ID, tag.getInt("FrontWheel"));
        if (tag.contains("RearWheel")) this.entityData.set(REAR_WHEEL_ID, tag.getInt("RearWheel"));
        if (tag.hasUUID("FrontWheelUuid")) {
            savedFrontUuid = tag.getUUID("FrontWheelUuid");
            this.entityData.set(FRONT_WHEEL_UUID, Optional.of(savedFrontUuid));
        }
        if (tag.hasUUID("RearWheelUuid")) {
            savedRearUuid = tag.getUUID("RearWheelUuid");
            this.entityData.set(REAR_WHEEL_UUID, Optional.of(savedRearUuid));
        }
        if (tag.contains("Key")) this.entityData.set(KEY_ID, tag.getInt("Key"));
        if (tag.hasUUID("KeyUuid")) {
            savedKeyUuid = tag.getUUID("KeyUuid");
            this.entityData.set(KEY_UUID, Optional.of(savedKeyUuid));
        }
        if (tag.contains("Pitch")) setSyncedPitch(tag.getFloat("Pitch"));
        if (tag.contains("WheelsInitialized")) wheelsInitialized = tag.getBoolean("WheelsInitialized");
        if (tag.contains("ForwardSpeed")) setSyncedForwardSpeed(tag.getFloat("ForwardSpeed"));
        else if (tag.contains("Speed")) setSyncedForwardSpeed(tag.getFloat("Speed"));
        if (tag.contains("SteerAngle")) {
            float steer = tag.getFloat("SteerAngle");
            this.steerAngle = steer;
            this.entityData.set(STEER_ANGLE, steer);
            this.previousRenderSteer = steer;
            this.renderSteer = steer;
        }
        if (tag.contains("Gear")) {
            byte ordinal = tag.getByte("Gear");
            DusterbikeGear[] gears = DusterbikeGear.values();
            if (ordinal >= 0 && ordinal < gears.length) setGear(gears[ordinal]);
        }
        if (tag.contains("FrontWheelRotation")) {
            savedFrontWheelRotation = tag.getFloat("FrontWheelRotation");
            savedRearWheelRotation = tag.getFloat("RearWheelRotation");
            savedFrontWheelAngularVelocity = tag.getDouble("FrontWheelAngularVelocity");
            savedRearWheelAngularVelocity = tag.getDouble("RearWheelAngularVelocity");
            this.entityData.set(FRONT_WHEEL_ROTATION, savedFrontWheelRotation);
            this.entityData.set(REAR_WHEEL_ROTATION, savedRearWheelRotation);
            this.renderFrontWheelRotation = savedFrontWheelRotation;
            this.previousFrontWheelRotation = savedFrontWheelRotation;
            this.renderRearWheelRotation = savedRearWheelRotation;
            this.previousRearWheelRotation = savedRearWheelRotation;
            pendingWheelSpinRestore = true;
        }
        if (tag.contains("EngineRunning")) setEngineRunning(tag.getBoolean("EngineRunning"));
        if (tag.contains("HeadlightsOn")) this.entityData.set(HEADLIGHTS_ON, (byte) (tag.getBoolean("HeadlightsOn") ? 1 : 0));
        if (tag.contains("IgnitionAttempts")) ignitionAttempts = tag.getInt("IgnitionAttempts");

        if (tag.contains("DusterbikeState")) {
            engineState.load(tag.getCompound("DusterbikeState"));
            this.entityData.set(FUEL_MILLI_BUCKETS, engineState.fuelMilliBuckets());
            this.entityData.set(FRAME_HEALTH, engineState.frameHealth());
        } else {
            if (tag.contains("FuelMilliBuckets")) setFuelMilliBuckets(tag.getInt("FuelMilliBuckets"));
            if (tag.contains("FrameHealth")) setFrameHealth(tag.getInt("FrameHealth"));
        }
        if (engineState.linkedBikeUuid() == null) engineState.setLinkedBikeUuid(this.getUUID());

        updateInstalledMask();
        updateSyncedColors();
        needsGroundSnap = true;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        DusterbikeWheelEntity frontWheel = getFrontWheel();
        DusterbikeWheelEntity rearWheel = getRearWheel();
        if (frontWheel != null && rearWheel != null) captureWheelSpinState(frontWheel, rearWheel);
        if (frontWheel != null) {
            tag.putInt("FrontWheel", frontWheel.getId());
            tag.putUUID("FrontWheelUuid", frontWheel.getUUID());
        } else if (savedFrontUuid != null) tag.putUUID("FrontWheelUuid", savedFrontUuid);
        if (rearWheel != null) {
            tag.putInt("RearWheel", rearWheel.getId());
            tag.putUUID("RearWheelUuid", rearWheel.getUUID());
        } else if (savedRearUuid != null) tag.putUUID("RearWheelUuid", savedRearUuid);
        DusterbikeKeyEntity key = getKeyEntity();
        if (key != null) {
            tag.putInt("Key", key.getId());
            tag.putUUID("KeyUuid", key.getUUID());
        } else if (savedKeyUuid != null) tag.putUUID("KeyUuid", savedKeyUuid);
        tag.putFloat("Pitch", getSyncedPitch());
        tag.putBoolean("WheelsInitialized", wheelsInitialized);
        tag.putFloat("ForwardSpeed", forwardSpeed);
        tag.putFloat("SteerAngle", steerAngle);
        this.entityData.set(STEER_ANGLE, steerAngle);
        this.entityData.set(FORWARD_SPEED, forwardSpeed);
        tag.putByte("Gear", (byte) getGear().ordinal());
        tag.putFloat("FrontWheelRotation", savedFrontWheelRotation);
        tag.putFloat("RearWheelRotation", savedRearWheelRotation);
        tag.putDouble("FrontWheelAngularVelocity", savedFrontWheelAngularVelocity);
        tag.putDouble("RearWheelAngularVelocity", savedRearWheelAngularVelocity);
        tag.putBoolean("EngineRunning", isEngineRunning());
        tag.putBoolean("HeadlightsOn", this.entityData.get(HEADLIGHTS_ON) != 0);
        tag.putInt("IgnitionAttempts", ignitionAttempts);
        if (engineState.linkedBikeUuid() == null) engineState.setLinkedBikeUuid(this.getUUID());
        tag.put("DusterbikeState", engineState.save());
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source) || source.is(DamageTypeTags.IS_FALL)) return false;
        if (!this.level().isClientSide) {
            damageFrame(amount);
            if (getFrameHealth() <= 0) destroyBikeAndDropParts();
        }
        return true;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getItem() instanceof BikeKeyItem && player.getAbilities().instabuild && !BikeKeyItem.hasLinkedBike(stack)) {
            if (!this.level().isClientSide) {
                BikeKeyItem.setLinkedBike(stack, this.getUUID());
                sendActionBar(player, "bike key linked");
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        if (player.isSecondaryUseActive() || !getPassengers().isEmpty()) return InteractionResult.PASS;
        if (!this.level().isClientSide) player.startRiding(this);
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) { return getPassengers().isEmpty(); }

    @Override
    protected void removePassenger(Entity passenger) {
        if (this.level().isClientSide && passenger instanceof Player player && player.isControlledByLocalInstance()) {
            DusterbikeWheelEntity frontWheel = getFrontWheel();
            DusterbikeWheelEntity rearWheel = getRearWheel();
            commitWheelRotationToRenderState(frontWheel, rearWheel);
            sendDriveStateToServer();
        }
        if (passenger instanceof LivingEntity living) DusterbikeRiderAnimation.clearRider(living);
        super.removePassenger(passenger);
    }

    @Override
    public LivingEntity getControllingPassenger() {
        Entity entity = getFirstPassenger();
        return entity instanceof LivingEntity living ? living : null;
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction moveFunction) {
        if (!hasPassenger(passenger)) return;
        float roll, pitch;
        if (this.level().isClientSide) {
            roll = getRenderRoll(1.0F);
            pitch = getRenderPitch(1.0F);
        } else {
            pitch = getSyncedPitch();
            roll = DusterbikePhysics.computeRollDegrees(getDriveForwardSpeed(), steerAngle,
                    DusterbikePhysics.computeMaxSteerDegrees(Math.abs(getDriveForwardSpeed())));
        }
        Vec3 feet = DusterbikeTransforms.worldPointFromLocal(position(), getYRot(), pitch, roll, DusterbikeTransforms.RIDER_FEET_LOCAL);
        moveFunction.accept(passenger, feet.x, feet.y, feet.z);
    }

    @Override
    public boolean shouldRiderSit() { return true; }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        Vec3 offset = DusterbikeTransforms.rotateLocalOffset(new Vec3(0.75D, 0.0D, 0.0D), getYRot());
        return position().add(offset);
    }

    @Override
    public boolean isPickable() { return true; }
    @Override
    public boolean isPushable() { return false; }
    @Override
    public boolean canBeCollidedWith() { return false; }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public double getTick(Object o) {
        return 0;
    }
}