package destiny.null_ouroboros.server.entity;

import destiny.null_ouroboros.common.dusterbike.*;
import destiny.null_ouroboros.server.item.BikeKeyItem;
import destiny.null_ouroboros.server.item.SprayCanItem;
import destiny.null_ouroboros.server.registry.EntityRegistry;
import destiny.null_ouroboros.server.registry.ItemRegistry;
import destiny.null_ouroboros.server.registry.SoundRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.*;

public class EngineHoistEntity extends Entity implements GeoAnimatable {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private final Map<DusterbikePartType, DusterbikePartState> partStates = new HashMap<>();
    @Nullable
    private UUID keyBikeUuid;

    private static final EntityDataAccessor<Integer> INSTALLED_PARTS_MASK =
            SynchedEntityData.defineId(EngineHoistEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> HOIST_HEALTH =
            SynchedEntityData.defineId(EngineHoistEntity.class, EntityDataSerializers.BYTE);

    private static final EntityDataAccessor<Integer> ENGINE_MAIN_COLOR =
            SynchedEntityData.defineId(EngineHoistEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PISTON_FRONT_MAIN_COLOR =
            SynchedEntityData.defineId(EngineHoistEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PISTON_FRONT_GLOW_COLOR =
            SynchedEntityData.defineId(EngineHoistEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PISTON_REAR_MAIN_COLOR =
            SynchedEntityData.defineId(EngineHoistEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PISTON_REAR_GLOW_COLOR =
            SynchedEntityData.defineId(EngineHoistEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SPARK_PLUG_FRONT_MAIN_COLOR =
            SynchedEntityData.defineId(EngineHoistEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SPARK_PLUG_FRONT_GLOW_COLOR =
            SynchedEntityData.defineId(EngineHoistEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SPARK_PLUG_REAR_MAIN_COLOR =
            SynchedEntityData.defineId(EngineHoistEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SPARK_PLUG_REAR_GLOW_COLOR =
            SynchedEntityData.defineId(EngineHoistEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> KEY_MAIN_COLOR =
            SynchedEntityData.defineId(EngineHoistEntity.class, EntityDataSerializers.INT);

    private long lastDamageTick;
    private static final byte EVENT_DAMAGE_WOBBLE = 32;
    private static final int MAX_HEALTH = 3;
    private static final long DAMAGE_RESET_TICKS = 20;

    public EngineHoistEntity(EntityType<?> type, Level level) {
        super(type, level);
        for (DusterbikePartType pt : List.of(
                DusterbikePartType.ENGINE, DusterbikePartType.PISTON_FRONT, DusterbikePartType.PISTON_REAR,
                DusterbikePartType.SPARK_PLUG_FRONT, DusterbikePartType.SPARK_PLUG_REAR, DusterbikePartType.KEY)) {
            partStates.put(pt, new DusterbikePartState(pt, pt.maxDurability(), false));
        }
        updateMask();
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(INSTALLED_PARTS_MASK, 0);
        this.entityData.define(HOIST_HEALTH, (byte) MAX_HEALTH);
        this.entityData.define(ENGINE_MAIN_COLOR, -1);
        this.entityData.define(PISTON_FRONT_MAIN_COLOR, -1);
        this.entityData.define(PISTON_FRONT_GLOW_COLOR, -1);
        this.entityData.define(PISTON_REAR_MAIN_COLOR, -1);
        this.entityData.define(PISTON_REAR_GLOW_COLOR, -1);
        this.entityData.define(SPARK_PLUG_FRONT_MAIN_COLOR, -1);
        this.entityData.define(SPARK_PLUG_FRONT_GLOW_COLOR, -1);
        this.entityData.define(SPARK_PLUG_REAR_MAIN_COLOR, -1);
        this.entityData.define(SPARK_PLUG_REAR_GLOW_COLOR, -1);
        this.entityData.define(KEY_MAIN_COLOR, -1);
    }

    private int computeMask() {
        int mask = 0;
        for (DusterbikePartType pt : partStates.keySet()) {
            if (partStates.get(pt).installed()) mask |= (1 << pt.ordinal());
        }
        return mask;
    }

    private void updateMask() {
        this.entityData.set(INSTALLED_PARTS_MASK, computeMask());
    }

    public boolean isPartInstalled(DusterbikePartType type) {
        if (type == null) return false;
        int mask = this.entityData.get(INSTALLED_PARTS_MASK);
        return (mask & (1 << type.ordinal())) != 0;
    }

    public boolean hasEngine() { return isPartInstalled(DusterbikePartType.ENGINE); }
    public boolean isEmpty()   { return !hasEngine(); }

    public Integer getPartMainColor(DusterbikePartType type) {
        EntityDataAccessor<Integer> acc = switch (type) {
            case ENGINE -> ENGINE_MAIN_COLOR;
            case PISTON_FRONT -> PISTON_FRONT_MAIN_COLOR;
            case PISTON_REAR -> PISTON_REAR_MAIN_COLOR;
            case SPARK_PLUG_FRONT -> SPARK_PLUG_FRONT_MAIN_COLOR;
            case SPARK_PLUG_REAR -> SPARK_PLUG_REAR_MAIN_COLOR;
            case KEY -> KEY_MAIN_COLOR;
            default -> null;
        };
        if (acc == null) return null;
        int val = this.entityData.get(acc);
        return val >= 0 ? val : null;
    }

    public Integer getPartGlowColor(DusterbikePartType type) {
        EntityDataAccessor<Integer> acc = switch (type) {
            case PISTON_FRONT -> PISTON_FRONT_GLOW_COLOR;
            case PISTON_REAR -> PISTON_REAR_GLOW_COLOR;
            case SPARK_PLUG_FRONT -> SPARK_PLUG_FRONT_GLOW_COLOR;
            case SPARK_PLUG_REAR -> SPARK_PLUG_REAR_GLOW_COLOR;
            default -> null;
        };
        if (acc == null) return null;
        int val = this.entityData.get(acc);
        return val >= 0 ? val : null;
    }

    private void syncAllColors() {
        for (DusterbikePartType pt : List.of(
                DusterbikePartType.ENGINE, DusterbikePartType.PISTON_FRONT, DusterbikePartType.PISTON_REAR,
                DusterbikePartType.SPARK_PLUG_FRONT, DusterbikePartType.SPARK_PLUG_REAR, DusterbikePartType.KEY)) {
            DusterbikePartState state = partStates.get(pt);
            if (state != null) {
                switch (pt) {
                    case ENGINE -> this.entityData.set(ENGINE_MAIN_COLOR, state.mainColor() != null ? state.mainColor() : -1);
                    case PISTON_FRONT -> {
                        this.entityData.set(PISTON_FRONT_MAIN_COLOR, state.mainColor() != null ? state.mainColor() : -1);
                        this.entityData.set(PISTON_FRONT_GLOW_COLOR, state.glowColor() != null ? state.glowColor() : -1);
                    }
                    case PISTON_REAR -> {
                        this.entityData.set(PISTON_REAR_MAIN_COLOR, state.mainColor() != null ? state.mainColor() : -1);
                        this.entityData.set(PISTON_REAR_GLOW_COLOR, state.glowColor() != null ? state.glowColor() : -1);
                    }
                    case SPARK_PLUG_FRONT -> {
                        this.entityData.set(SPARK_PLUG_FRONT_MAIN_COLOR, state.mainColor() != null ? state.mainColor() : -1);
                        this.entityData.set(SPARK_PLUG_FRONT_GLOW_COLOR, state.glowColor() != null ? state.glowColor() : -1);
                    }
                    case SPARK_PLUG_REAR -> {
                        this.entityData.set(SPARK_PLUG_REAR_MAIN_COLOR, state.mainColor() != null ? state.mainColor() : -1);
                        this.entityData.set(SPARK_PLUG_REAR_GLOW_COLOR, state.glowColor() != null ? state.glowColor() : -1);
                    }
                    case KEY -> this.entityData.set(KEY_MAIN_COLOR, state.mainColor() != null ? state.mainColor() : -1);
                }
            }
        }
    }

    public void setEngineState(DusterbikeEngineState bikeEngineState, @Nullable UUID keyLinkedBikeUuid) {
        for (DusterbikePartType pt : List.of(
                DusterbikePartType.ENGINE, DusterbikePartType.PISTON_FRONT, DusterbikePartType.PISTON_REAR,
                DusterbikePartType.SPARK_PLUG_FRONT, DusterbikePartType.SPARK_PLUG_REAR, DusterbikePartType.KEY)) {
            DusterbikePartState src = bikeEngineState.part(pt);

            DusterbikePartState dest = new DusterbikePartState(pt, src.durability(), src.installed());
            dest.setMainColor(src.mainColor());
            dest.setGlowColor(src.glowColor());
            partStates.put(pt, dest);
        }
        this.keyBikeUuid = keyLinkedBikeUuid;
        updateMask();
        syncAllColors();
        playEngineHoistInteractSound(level(), blockPosition());
        if (!level().isClientSide) {
            ensurePartTargetsSpawned();
        }
    }

    public DusterbikePartState getPartState(DusterbikePartType type) { return partStates.get(type); }

    private boolean isPistonBlockedBySparkPlug(DusterbikePartType type) {
        if (type == DusterbikePartType.PISTON_FRONT) return isPartInstalled(DusterbikePartType.SPARK_PLUG_FRONT);
        if (type == DusterbikePartType.PISTON_REAR)  return isPartInstalled(DusterbikePartType.SPARK_PLUG_REAR);
        return false;
    }

    private DusterbikeEntity findNearestBikeWithoutEngine() {
        return level().getEntitiesOfClass(DusterbikeEntity.class, getBoundingBox().inflate(8.0D),
                        bike -> !bike.isPartInstalled(DusterbikePartType.ENGINE))
                .stream()
                .min(Comparator.comparingDouble(this::distanceToSqr))
                .orElse(null);
    }

    private void transferEngineToBike(DusterbikeEntity bike, BlockPos playerPos) {
        for (DusterbikePartType pt : List.of(
                DusterbikePartType.ENGINE, DusterbikePartType.PISTON_FRONT, DusterbikePartType.PISTON_REAR,
                DusterbikePartType.SPARK_PLUG_FRONT, DusterbikePartType.SPARK_PLUG_REAR, DusterbikePartType.KEY)) {
            DusterbikePartState src = partStates.get(pt);
            DusterbikePartState dest = bike.getPartState(pt);
            dest.setInstalled(src.installed());
            dest.setMainColor(src.mainColor());
            dest.setGlowColor(src.glowColor());
        }
        if (keyBikeUuid != null) {
            bike.getEngineState().setInsertedKeyBikeUuid(keyBikeUuid);
        } else {
            bike.getEngineState().setInsertedKeyBikeUuid(null);
        }
        bike.updateInstalledMask();
        bike.updateSyncedColors();

        for (DusterbikePartType pt : List.of(
                DusterbikePartType.ENGINE, DusterbikePartType.PISTON_FRONT, DusterbikePartType.PISTON_REAR,
                DusterbikePartType.SPARK_PLUG_FRONT, DusterbikePartType.SPARK_PLUG_REAR, DusterbikePartType.KEY)) {
            partStates.get(pt).setInstalled(false);
        }
        this.keyBikeUuid = null;
        updateMask();
        discardPartTargets();
        playEngineHoistInteractSound(level(), blockPosition());
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    public void handlePartInteraction(Player player, InteractionHand hand,
                                      HoistPartTargetType partTarget, boolean secondaryUse) {
        if (!level().isClientSide) {
            ItemStack stack = player.getItemInHand(hand);
            DusterbikePartType partType = partTarget.partType();

            if (partType == DusterbikePartType.KEY) {
                handleKeyPortInteraction(player, hand);
                return;
            }

            if (stack.is(ItemRegistry.WRENCH.get())) {
                handleWrenchInteraction(player, hand, stack, partType, secondaryUse);
            } else if (stack.getItem() instanceof SprayCanItem) {
                handleSprayInteraction(player, stack, partType, secondaryUse, hand);
            } else if (partType != null && partType.hasItemForm() && !stack.isEmpty() && stack.is(partType.item())) {
                handlePartInstall(player, hand, stack, partType);
            }
        }
    }

    public void handleKeyPortInteraction(Player player, InteractionHand hand) {
        if (this.level().isClientSide) return;
        ItemStack stack = player.getItemInHand(hand);

        if (stack.getItem() instanceof SprayCanItem) {
            handleKeySprayInteraction(player, stack, hand);
            return;
        }

        DusterbikePartState keyState = getPartState(DusterbikePartType.KEY);

        if (!stack.isEmpty() && stack.getItem() instanceof BikeKeyItem) {
            if (!BikeKeyItem.hasLinkedBike(stack)) {
                if (!player.getAbilities().instabuild) return;
                BikeKeyItem.setLinkedBike(stack, this.getUUID());
                sendActionBar(player, "key linked");
                player.swing(hand, true);
                return;
            }

            if (keyState.installed()) {
                sendActionBar(player, "Key slot already occupied");
                return;
            }

            UUID linkedBike = BikeKeyItem.getLinkedBike(stack);
            keyState.setInstalled(true);
            this.keyBikeUuid = linkedBike;
            if (stack.getItem() instanceof DyeableLeatherItem dyeable && dyeable.hasCustomColor(stack)) {
                keyState.setMainColor(dyeable.getColor(stack));
            }
            updateMask();
            syncAllColors();
            if (!player.getAbilities().instabuild) stack.shrink(1);
            sendActionBar(player, "Key inserted");
            player.swing(hand, true);
            playKeyInsertSound(level(), player.blockPosition());
            return;
        }

        if (stack.isEmpty() && player.isSecondaryUseActive() && keyState.installed()) {
            ItemStack keyStack = new ItemStack(ItemRegistry.BIKE_KEY.get());
            BikeKeyItem.setLinkedBike(keyStack, this.keyBikeUuid);
            Integer color = keyState.mainColor();
            if (color != null && keyStack.getItem() instanceof DyeableLeatherItem dyeable) {
                dyeable.setColor(keyStack, color);
            }
            keyState.setInstalled(false);
            keyState.setMainColor(null);
            this.keyBikeUuid = null;
            updateMask();
            syncAllColors();
            if (!player.addItem(keyStack)) spawnAtLocation(keyStack);
            sendActionBar(player, "Key removed");
            player.swing(hand, true);
            playKeyRemoveSound(level(), player.blockPosition());
        }
    }

    private void handleKeySprayInteraction(Player player, ItemStack sprayCan, InteractionHand hand) {
        DusterbikePartState keyState = getPartState(DusterbikePartType.KEY);
        if (!keyState.installed()) {
            sendActionBar(player, "No key inserted");
            return;
        }
        SprayCanItem sprayItem = (SprayCanItem) sprayCan.getItem();
        boolean clearing = !sprayItem.hasCustomColor(sprayCan);
        if (clearing) {
            keyState.setMainColor(null);
            sendActionBar(player, "key color removed");
        } else {
            int color = sprayItem.getColor(sprayCan) & 0xFFFFFF;
            keyState.setMainColor(color);
            sendActionBar(player, "key main color set");
        }
        syncAllColors();
        player.swing(hand);
        playSpraySound(level(), player.blockPosition());
    }

    private void handlePartInstall(Player player, InteractionHand hand, ItemStack stack, DusterbikePartType partType) {
        DusterbikePartState state = getPartState(partType);
        if (state.installed()) {
            sendActionBar(player, partType.serializedName() + " already installed");
            return;
        }
        DusterbikePartItems.applyStackToState(stack, state);
        state.setInstalled(true);
        updateMask();
        syncAllColors();
        if (!player.getAbilities().instabuild) stack.shrink(1);
        sendActionBar(player, partType.serializedName() + " installed");
        player.swing(hand);
        playPartInstallSound(level(), player.blockPosition());
    }

    private void handleWrenchInteraction(Player player, InteractionHand hand, ItemStack wrench, DusterbikePartType partType, boolean secondaryUse) {
        DusterbikePartState state = getPartState(partType);
        if (state == null || !state.installed()) return;

        if (!secondaryUse) {
            if (partType == DusterbikePartType.ENGINE) return;
            
            sendActionBar(player, partType.serializedName() + ": " + state.durability() + " / " + state.maxDurability());
            damageTool(player, hand, wrench);
            player.swing(hand, true);
            playWrenchSound(level(), player.blockPosition());
            return;
        }

        if (partType == DusterbikePartType.ENGINE) {
            DusterbikeEntity targetBike = findNearestBikeWithoutEngine();
            if (targetBike != null) {
                transferEngineToBike(targetBike, player.blockPosition());
                damageTool(player, hand, wrench);
                player.swing(hand, true);
                playWrenchSound(level(), player.blockPosition());
            }
            return;
        }

        if (partType.isRemovable()) {
            if (isPistonBlockedBySparkPlug(partType)) {
                return;
            }

            ItemStack removed = DusterbikePartItems.createPartStack(state);
            state.setInstalled(false);
            state.setMainColor(null);
            state.setGlowColor(null);
            updateMask();
            syncAllColors();
            if (!player.addItem(removed)) spawnAtLocation(removed);
            sendActionBar(player, partType.serializedName() + " removed");
            player.swing(hand, true);
            playWrenchSound(level(), player.blockPosition());
        }
        damageTool(player, hand, wrench);
    }

    private void handleSprayInteraction(Player player, ItemStack sprayCan, DusterbikePartType partType, boolean secondaryUse, InteractionHand hand) {
        if (!(sprayCan.getItem() instanceof SprayCanItem sprayItem)) return;
        boolean clearing = !sprayItem.hasCustomColor(sprayCan);
        DusterbikePartState state = getPartState(partType);
        if (state == null) return;

        if (partType == DusterbikePartType.KEY && !state.installed()) {
            return;
        }

        if (partType == DusterbikePartType.ENGINE) {
            if (clearing) state.setMainColor(null);
            else state.setMainColor(sprayItem.getColor(sprayCan) & 0xFFFFFF);
            sendActionBar(player, "engine main color " + (clearing ? "removed" : "set"));

            DusterbikePartType[] subs = { DusterbikePartType.PISTON_FRONT, DusterbikePartType.PISTON_REAR,
                    DusterbikePartType.SPARK_PLUG_FRONT, DusterbikePartType.SPARK_PLUG_REAR };
            for (DusterbikePartType sub : subs) {
                DusterbikePartState subState = getPartState(sub);
                if (secondaryUse) {
                    if (clearing) subState.setGlowColor(null);
                    else subState.setGlowColor(sprayItem.getColor(sprayCan) & 0xFFFFFF);
                } else {
                    if (clearing) subState.setMainColor(null);
                    else subState.setMainColor(sprayItem.getColor(sprayCan) & 0xFFFFFF);
                }
            }
        } else if (partType == DusterbikePartType.KEY) {
            handleKeySprayInteraction(player, sprayCan, hand);
            return;
        } else {
            if (secondaryUse) {
                if (clearing) state.setGlowColor(null);
                else state.setGlowColor(sprayItem.getColor(sprayCan) & 0xFFFFFF);
            } else {
                if (clearing) state.setMainColor(null);
                else state.setMainColor(sprayItem.getColor(sprayCan) & 0xFFFFFF);
            }
            sendActionBar(player, partType.serializedName() + " color " + (clearing ? "removed" : "set"));
        }
        syncAllColors();
        player.swing(hand);
        playSpraySound(level(), player.blockPosition());
    }

    private void damageTool(Player player, InteractionHand hand, ItemStack stack) {
        stack.hurtAndBreak(1, player, broken -> broken.broadcastBreakEvent(hand));
    }

    private static void sendActionBar(Player player, String message) {
        player.displayClientMessage(Component.literal(message), true);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source) || source.is(DamageTypeTags.IS_FALL)) {
            return false;
        }
        if (!this.level().isClientSide) {
            int health = this.entityData.get(HOIST_HEALTH);
            if (health <= 0) return false;

            health--;
            this.entityData.set(HOIST_HEALTH, (byte) health);
            this.lastDamageTick = this.level().getGameTime();
            this.level().broadcastEntityEvent(this, EVENT_DAMAGE_WOBBLE);

            if (health <= 0) {
                destroyHoist();
            }
        }
        return true;
    }

    private void destroyHoist() {
        for (DusterbikePartType pt : partStates.keySet()) {
            partStates.get(pt).setInstalled(false);
        }
        this.keyBikeUuid = null;
        discardPartTargets();
        spawnAtLocation(new ItemStack(ItemRegistry.ENGINE_HOIST.get()));
        this.discard();
    }

    public void setFacing(Player player) {
        setYRot(player.getYRot());
    }

    public int getHoistHealth() {
        return this.entityData.get(HOIST_HEALTH);
    }

    public long getLastDamageTick() {
        return lastDamageTick;
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_DAMAGE_WOBBLE) {
            this.lastDamageTick = this.level().getGameTime();
        } else {
            super.handleEntityEvent(id);
        }
    }

    private void resetDamageIfUntouched() {
        if (!level().isClientSide && this.isAlive()) {
            if (this.level().getGameTime() - this.lastDamageTick > DAMAGE_RESET_TICKS) {
                this.entityData.set(HOIST_HEALTH, (byte) MAX_HEALTH);
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("EngineState")) {
            DusterbikeEngineState temp = new DusterbikeEngineState();
            temp.load(tag.getCompound("EngineState"));
            for (DusterbikePartType pt : List.of(
                    DusterbikePartType.ENGINE, DusterbikePartType.PISTON_FRONT, DusterbikePartType.PISTON_REAR,
                    DusterbikePartType.SPARK_PLUG_FRONT, DusterbikePartType.SPARK_PLUG_REAR, DusterbikePartType.KEY)) {
                partStates.put(pt, temp.part(pt));
            }
        } else {
            for (DusterbikePartType pt : partStates.keySet()) {
                partStates.get(pt).setInstalled(false);
            }
        }
        if (tag.hasUUID("KeyBikeUuid")) {
            this.keyBikeUuid = tag.getUUID("KeyBikeUuid");
        } else {
            this.keyBikeUuid = null;
        }
        if (tag.contains("HoistHealth")) {
            this.entityData.set(HOIST_HEALTH, (byte) tag.getInt("HoistHealth"));
        }
        if (tag.contains("LastDamageTick")) {
            this.lastDamageTick = tag.getLong("LastDamageTick");
        }
        updateMask();
        syncAllColors();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        DusterbikeEngineState temp = new DusterbikeEngineState();
        for (DusterbikePartType pt : List.of(
                DusterbikePartType.ENGINE, DusterbikePartType.PISTON_FRONT, DusterbikePartType.PISTON_REAR,
                DusterbikePartType.SPARK_PLUG_FRONT, DusterbikePartType.SPARK_PLUG_REAR, DusterbikePartType.KEY)) {
            DusterbikePartState hoistState = partStates.get(pt);
            DusterbikePartState tempState = temp.part(pt);
            int currentDur = tempState.durability();
            int desiredDur = hoistState.durability();
            if (currentDur > desiredDur) {
                tempState.damage(currentDur - desiredDur);
            }
            tempState.setInstalled(hoistState.installed());
            tempState.setMainColor(hoistState.mainColor());
            tempState.setGlowColor(hoistState.glowColor());
        }
        tag.put("EngineState", temp.save());
        if (this.keyBikeUuid != null) {
            tag.putUUID("KeyBikeUuid", this.keyBikeUuid);
        }
        tag.putInt("HoistHealth", this.entityData.get(HOIST_HEALTH));
        tag.putLong("LastDamageTick", this.lastDamageTick);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "state", 0, this::predicate));
    }

    private PlayState predicate(AnimationState<EngineHoistEntity> state) {
        String currentAnim = state.getController().getCurrentAnimation() != null
                ? state.getController().getCurrentAnimation().animation().name()
                : "";
        if (hasEngine() && !"hoist_hold".equals(currentAnim)) {
            state.getController().setAnimation(RawAnimation.begin().thenPlay("hoist_hold"));
        } else if (!hasEngine() && !"hoist_idle".equals(currentAnim)) {
            state.getController().setAnimation(RawAnimation.begin().thenPlay("hoist_idle"));
        }
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    @Override
    public double getTick(Object o) { return tickCount; }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            if (hasEngine()) {
                ensurePartTargetsSpawned();
            } else {
                discardPartTargets();
            }
            syncPartTargetColliderPositions();
            resetDamageIfUntouched();
        }
    }

    private void ensurePartTargetsSpawned() {
        if (!hasEngine()) return;
        for (HoistPartTargetType targetType : HoistPartTargetType.values()) {
            if (getPartTargetEntity(targetType) != null) continue;
            Vec3 center = getPartWorldPosition(targetType);
            HoistPartInteractionEntity target = new HoistPartInteractionEntity(
                    EntityRegistry.HOIST_PART_INTERACTION.get(), level(),
                    getId(), getUUID(),
                    targetType,
                    center.x, center.y, center.z);
            level().addFreshEntity(target);
        }
    }

    private void syncPartTargetColliderPositions() {
        for (HoistPartInteractionEntity target : level().getEntitiesOfClass(
                HoistPartInteractionEntity.class,
                getBoundingBox().inflate(8.0D),
                t -> t.getParentId() == getId() || t.getParentUuid().map(this.getUUID()::equals).orElse(false))) {
            Vec3 center = getPartWorldPosition(target.getTargetType());
            target.syncColliderPosition(center.x, center.y, center.z);
        }
    }

    @Nullable
    public HoistPartInteractionEntity getPartTargetEntity(HoistPartTargetType targetType) {
        for (HoistPartInteractionEntity target : level().getEntitiesOfClass(
                HoistPartInteractionEntity.class,
                getBoundingBox().inflate(8.0D),
                t -> t.getParentId() == getId() || t.getParentUuid().map(this.getUUID()::equals).orElse(false))) {
            if (target.getTargetType() == targetType) return target;
        }
        return null;
    }

    private void discardPartTargets() {
        for (HoistPartInteractionEntity target : level().getEntitiesOfClass(
                HoistPartInteractionEntity.class,
                getBoundingBox().inflate(16.0D),
                t -> t.getParentId() == getId() || t.getParentUuid().map(this.getUUID()::equals).orElse(false))) {
            if (target.isAlive()) target.discard();
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide) discardPartTargets();
        super.remove(reason);
    }

    private Vec3 getPartWorldPosition(HoistPartTargetType target) {
        Vec3 local = target.localCenter();
        double rad = Math.toRadians(getYRot());
        double x = getX() + local.x * Math.cos(rad) - local.z * Math.sin(rad);
        double y = getY() + local.y;
        double z = getZ() + local.x * Math.sin(rad) + local.z * Math.cos(rad);
        return new Vec3(x, y, z);
    }

    @Override
    protected AABB makeBoundingBox() {
        double hw = 0.3, hh = 1.5, hd = 0.3;
        Vec3 localCenter = new Vec3(0, 0.5, 0.6);
        double rad = Math.toRadians(getYRot());
        double x = getX() + localCenter.x * Math.cos(rad) - localCenter.z * Math.sin(rad);
        double y = getY() + localCenter.y;
        double z = getZ() + localCenter.x * Math.sin(rad) + localCenter.z * Math.cos(rad);
        return new AABB(x - hw, y - hh, z - hd, x + hw, y + hh, z + hd);
    }

    @Override
    public boolean isPushable() { return false; }
    @Override
    public boolean canBeCollidedWith() { return false; }
    @Override
    public boolean isPickable() { return true; }

    private void playWrenchSound(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundRegistry.WRENCH_INTERACT.get(), SoundSource.PLAYERS, 0.5f, 1f);
    }
    private void playSpraySound(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundRegistry.SPRAY_CAN_INTERACT.get(), SoundSource.PLAYERS, 0.5f, 1f);
    }
    private void playPartInstallSound(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundRegistry.PART_INSTALL.get(), SoundSource.PLAYERS, 0.5f, 1f);
    }
    private void playKeyInsertSound(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundRegistry.KEY_INSERT.get(), SoundSource.PLAYERS, 0.5f, 1f);
    }
    private void playKeyRemoveSound(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundRegistry.KEY_INSERT.get(), SoundSource.PLAYERS, 0.5f, 0.8f);
    }
    private void playEngineHoistInteractSound(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundRegistry.ENGINE_HOIST_INTERACT.get(), SoundSource.PLAYERS, 0.5f, 1f);
    }
}