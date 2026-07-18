package destiny.null_ouroboros.server.entity;

import destiny.null_ouroboros.common.dusterbike.*;
import destiny.null_ouroboros.server.item.BikeKeyItem;
import destiny.null_ouroboros.server.item.SprayCanItem;
import destiny.null_ouroboros.server.registry.EntityRegistry;
import destiny.null_ouroboros.server.registry.ItemRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.*;

public class EngineEntity extends Entity implements GeoAnimatable {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private final Map<DusterbikePartType, DusterbikePartState> partStates = new HashMap<>();
    @Nullable
    private UUID keyBikeUuid;

    private static final EntityDataAccessor<Integer> INSTALLED_PARTS_MASK =
            SynchedEntityData.defineId(EngineEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> ENGINE_HEALTH =
            SynchedEntityData.defineId(EngineEntity.class, EntityDataSerializers.BYTE);

    private static final EntityDataAccessor<Integer> ENGINE_MAIN_COLOR =
            SynchedEntityData.defineId(EngineEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PISTON_FRONT_MAIN_COLOR =
            SynchedEntityData.defineId(EngineEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PISTON_FRONT_GLOW_COLOR =
            SynchedEntityData.defineId(EngineEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PISTON_REAR_MAIN_COLOR =
            SynchedEntityData.defineId(EngineEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PISTON_REAR_GLOW_COLOR =
            SynchedEntityData.defineId(EngineEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SPARK_PLUG_FRONT_MAIN_COLOR =
            SynchedEntityData.defineId(EngineEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SPARK_PLUG_FRONT_GLOW_COLOR =
            SynchedEntityData.defineId(EngineEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SPARK_PLUG_REAR_MAIN_COLOR =
            SynchedEntityData.defineId(EngineEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SPARK_PLUG_REAR_GLOW_COLOR =
            SynchedEntityData.defineId(EngineEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> KEY_MAIN_COLOR =
            SynchedEntityData.defineId(EngineEntity.class, EntityDataSerializers.INT);

    private long lastDamageTick;
    private static final byte EVENT_DAMAGE_WOBBLE = 32;
    private static final int MAX_HEALTH = 3;
    private static final long DAMAGE_RESET_TICKS = 20;

    private EngineKeyEntity keyEntity;
    private static final Vec3 KEY_LOCAL_OFFSET = new Vec3(-0.25, 0.7, 0);

    public EngineEntity(EntityType<?> type, Level level) {
        super(type, level);
        for (DusterbikePartType pt : EngineAssembly.PARTS) {
            partStates.put(pt, new DusterbikePartState(pt, pt.maxDurability(), false));
        }
        this.noPhysics = false;
        this.setNoGravity(false);
        updateMask();
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(INSTALLED_PARTS_MASK, 0);
        this.entityData.define(ENGINE_HEALTH, (byte) MAX_HEALTH);
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

    private void updateMask() {
        this.entityData.set(INSTALLED_PARTS_MASK, EngineAssembly.computeMask(partStates::get));
    }

    public boolean isPartInstalled(DusterbikePartType type) {
        if (type == null) return false;
        int mask = this.entityData.get(INSTALLED_PARTS_MASK);
        return (mask & (1 << type.ordinal())) != 0;
    }

    public boolean hasEngine() { return isPartInstalled(DusterbikePartType.ENGINE); }

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
        for (DusterbikePartType pt : EngineAssembly.PARTS) {
            DusterbikePartState state = partStates.get(pt);
            if (state == null) continue;
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
                default -> {}
            }
        }
    }

    public void loadEngineState(Map<DusterbikePartType, DusterbikePartState> source, @Nullable UUID keyUuid) {
        EngineAssembly.copyPartsIntoMap(source::get, partStates);
        this.keyBikeUuid = keyUuid;
        updateMask();
        syncAllColors();
    }

    public DusterbikePartState getPartState(DusterbikePartType type) { return partStates.get(type); }

    private DusterbikeEntity findNearestBikeWithoutEngine() {
        return level().getEntitiesOfClass(DusterbikeEntity.class, getBoundingBox().inflate(8.0D),
                        bike -> !bike.isPartInstalled(DusterbikePartType.ENGINE))
                .stream()
                .min(Comparator.comparingDouble(this::distanceToSqr))
                .orElse(null);
    }

    private EngineHoistEntity findNearestHoistWithoutEngine() {
        return level().getEntitiesOfClass(EngineHoistEntity.class, getBoundingBox().inflate(8.0D),
                        hoist -> !hoist.isPartInstalled(DusterbikePartType.ENGINE))
                .stream()
                .min(Comparator.comparingDouble(this::distanceToSqr))
                .orElse(null);
    }

    private void transferToBike(DusterbikeEntity bike) {
        EngineAssembly.copyParts(partStates::get, bike::getPartState);
        bike.getEngineState().setInsertedKeyBikeUuid(keyBikeUuid);
        bike.updateInstalledMask();
        bike.updateSyncedColors();
    }

    private void transferToHoist(EngineHoistEntity hoist) {
        EngineAssembly.copyParts(partStates::get, hoist::getPartState);
        hoist.setKeyBikeUuid(keyBikeUuid);
        hoist.updateMask();
        hoist.syncAllColors();
        hoist.ensurePartTargetsSpawned();
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(ItemRegistry.WRENCH.get()) && player.isSecondaryUseActive()) {
            if (!level().isClientSide) {
                DusterbikeEntity bike = findNearestBikeWithoutEngine();
                if (bike != null) {
                    transferToBike(bike);
                    discardKeyEntity();
                    this.discard();
                    player.swing(hand, true);
                    PartInteraction.playWrenchSound(level(), player.blockPosition());
                    return InteractionResult.sidedSuccess(level().isClientSide);
                }
                EngineHoistEntity hoist = findNearestHoistWithoutEngine();
                if (hoist != null) {
                    transferToHoist(hoist);
                    discardKeyEntity();
                    this.discard();
                    player.swing(hand, true);
                    PartInteraction.playWrenchSound(level(), player.blockPosition());
                    return InteractionResult.sidedSuccess(level().isClientSide);
                }
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return InteractionResult.PASS;
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
                PartInteraction.sendActionBar(player, Component.translatable("message.null_ouroboros.dusterbike.key_linked"));
                player.swing(hand, true);
                return;
            }

            if (keyState.installed()) {
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
            player.swing(hand, true);
            PartInteraction.playKeyInsertSound(level(), player.blockPosition());
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
            player.swing(hand, true);
            PartInteraction.playKeyRemoveSound(level(), player.blockPosition());
        }
    }

    private void handleKeySprayInteraction(Player player, ItemStack sprayCan, InteractionHand hand) {
        DusterbikePartState keyState = getPartState(DusterbikePartType.KEY);
        if (!keyState.installed()) return;
        SprayCanItem sprayItem = (SprayCanItem) sprayCan.getItem();
        boolean clearing = !sprayItem.hasCustomColor(sprayCan);
        if (clearing) {
            keyState.setMainColor(null);
        } else {
            int color = sprayItem.getColor(sprayCan) & 0xFFFFFF;
            keyState.setMainColor(color);
        }
        syncAllColors();
        player.swing(hand, true);
        PartInteraction.playSpraySound(level(), player.blockPosition());
        PartInteraction.damageTool(player, hand, sprayCan);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerableTo(source) || source.is(DamageTypeTags.IS_FALL)) return false;
        if (!level().isClientSide) {
            int health = this.entityData.get(ENGINE_HEALTH);
            if (health <= 0) return false;
            health--;
            this.entityData.set(ENGINE_HEALTH, (byte) health);
            this.lastDamageTick = this.level().getGameTime();
            this.level().broadcastEntityEvent(this, EVENT_DAMAGE_WOBBLE);
            if (health <= 0) {
                dropAllParts();
                discardKeyEntity();
                this.discard();
            }
        }
        return true;
    }

    private void dropAllParts() {
        if (isPartInstalled(DusterbikePartType.ENGINE)) {
            spawnAtLocation(new ItemStack(ItemRegistry.ENGINE_BASE.get()));
        }
        for (DusterbikePartType pt : List.of(
                DusterbikePartType.PISTON_FRONT, DusterbikePartType.PISTON_REAR,
                DusterbikePartType.SPARK_PLUG_FRONT, DusterbikePartType.SPARK_PLUG_REAR)) {
            DusterbikePartState state = partStates.get(pt);
            if (state.installed()) {
                ItemStack stack = DusterbikePartItems.createPartStack(state);
                if (!stack.isEmpty()) spawnAtLocation(stack);
            }
        }
        if (isPartInstalled(DusterbikePartType.KEY)) {
            ItemStack keyStack = new ItemStack(ItemRegistry.BIKE_KEY.get());
            if (keyBikeUuid != null) BikeKeyItem.setLinkedBike(keyStack, keyBikeUuid);
            Integer color = partStates.get(DusterbikePartType.KEY).mainColor();
            if (color != null && keyStack.getItem() instanceof DyeableLeatherItem dyeable) {
                dyeable.setColor(keyStack, color);
            }
            spawnAtLocation(keyStack);
        }
    }

    public int getEngineHealth() { return this.entityData.get(ENGINE_HEALTH); }
    public long getLastDamageTick() { return lastDamageTick; }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_DAMAGE_WOBBLE) {
            this.lastDamageTick = this.level().getGameTime();
        } else {
            super.handleEntityEvent(id);
        }
    }

    private void resetDamageIfUntouched() {
        if (!level().isClientSide && isAlive()) {
            if (this.level().getGameTime() - this.lastDamageTick > DAMAGE_RESET_TICKS) {
                this.entityData.set(ENGINE_HEALTH, (byte) MAX_HEALTH);
            }
        }
    }

    private Vec3 getKeyWorldPosition() {
        double rad = Math.toRadians(getYRot());
        double x = getX() + KEY_LOCAL_OFFSET.x * Math.cos(rad) - KEY_LOCAL_OFFSET.z * Math.sin(rad);
        double y = getY() + KEY_LOCAL_OFFSET.y;
        double z = getZ() + KEY_LOCAL_OFFSET.x * Math.sin(rad) + KEY_LOCAL_OFFSET.z * Math.cos(rad);
        return new Vec3(x, y, z);
    }

    private void ensureKeyEntitySpawned() {
        if (keyEntity != null && keyEntity.isAlive()) return;
        Vec3 pos = getKeyWorldPosition();
        EngineKeyEntity newKey = new EngineKeyEntity(
                EntityRegistry.ENGINE_KEY.get(), level(),
                getId(), getUUID(),
                pos.x, pos.y, pos.z);
        this.keyEntity = newKey;
        level().addFreshEntity(newKey);
    }

    private void discardKeyEntity() {
        if (keyEntity != null) {
            keyEntity.discard();
            keyEntity = null;
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide) {
            discardKeyEntity();
        }
        super.remove(reason);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.isNoGravity()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.04, 0.0));
        }
        this.move(MoverType.SELF, this.getDeltaMovement());
        this.setDeltaMovement(this.getDeltaMovement().scale(0.98));
        if (this.onGround()) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(1.0, 0.0, 1.0));
        }

        if (!level().isClientSide && this.onGround() && this.fallDistance > 0.f) {
            float damage = Mth.clamp(this.fallDistance * 2f, 0, 20);
            AABB box = getBoundingBox().inflate(0.2);
            for (LivingEntity entity : level().getEntitiesOfClass(LivingEntity.class, box, Entity::isAlive)) {
                entity.hurt(level().damageSources().anvil(this), damage);
            }
            this.fallDistance = 0f;
        }

        if (keyEntity == null || !keyEntity.isAlive()) {
            if (!level().isClientSide) {
                ensureKeyEntitySpawned();
            }
        } else {
            Vec3 keyWorld = getKeyWorldPosition();
            keyEntity.syncColliderPosition(keyWorld.x, keyWorld.y, keyWorld.z);
        }

        if (!level().isClientSide) {
            resetDamageIfUntouched();
        }
    }

    @Override
    protected @NotNull AABB makeBoundingBox() {
        double w = 0.25, h = 1, d = 0.35;
        return new AABB(getX() - w, getY(), getZ() - d,
                getX() + w, getY() + h, getZ() + d);
    }

    @Override public boolean isPushable() { return false; }
    @Override public boolean canBeCollidedWith() { return false; }
    @Override public boolean isPickable() { return true; }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("EngineState")) {
            DusterbikeEngineState temp = new DusterbikeEngineState();
            temp.load(tag.getCompound("EngineState"));
            EngineAssembly.copyPartsIntoMap(temp::part, partStates);
        }
        if (tag.hasUUID("KeyBikeUuid")) {
            this.keyBikeUuid = tag.getUUID("KeyBikeUuid");
        }
        if (tag.contains("EngineHealth")) {
            this.entityData.set(ENGINE_HEALTH, (byte) tag.getInt("EngineHealth"));
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
        EngineAssembly.writeToEngineState(partStates::get, temp);
        tag.put("EngineState", temp.save());
        if (this.keyBikeUuid != null) {
            tag.putUUID("KeyBikeUuid", this.keyBikeUuid);
        }
        tag.putInt("EngineHealth", this.entityData.get(ENGINE_HEALTH));
        tag.putLong("LastDamageTick", this.lastDamageTick);
    }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    @Override public double getTick(Object o) { return tickCount; }
}