package destiny.null_ouroboros.server.entity;

import destiny.null_ouroboros.common.revolver.RevolverCartridge;
import destiny.null_ouroboros.common.revolver.RevolverTransforms;
import destiny.null_ouroboros.server.registry.SoundRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class CartridgeEntity extends Entity implements GeoAnimatable {
    private static final EntityDataAccessor<Byte> CARTRIDGE =
            SynchedEntityData.defineId(CartridgeEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Float> ROLL =
            SynchedEntityData.defineId(CartridgeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ROLL_SPEED =
            SynchedEntityData.defineId(CartridgeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> RESTING =
            SynchedEntityData.defineId(CartridgeEntity.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private float prevRoll;
    private float clientRoll;

    public CartridgeEntity(EntityType<? extends CartridgeEntity> type, Level level) {
        super(type, level);
        setNoGravity(false);
    }

    public void initialize(Player player, int chamber, RevolverCartridge cartridge) {
        setCartridge(cartridge);
        setPos(RevolverTransforms.cartridgeWorldPosition(player, player.getMainHandItem(), chamber));
        setYRot(random.nextFloat() * 360.0F);
        setXRot(0.0F);
        prevRoll = 0.0F;
        clientRoll = 0.0F;
        entityData.set(ROLL, 0.0F);
        entityData.set(ROLL_SPEED, 12.0F + random.nextFloat() * 16.0F);
        entityData.set(RESTING, false);
        Vec3 look = player.getLookAngle();
        Vec3 side = new Vec3(look.z, 0.0D, -look.x).normalize();
        setDeltaMovement(player.getDeltaMovement()
                .add(look.scale(0.1D))
                .add(side.scale((random.nextDouble() - 0.5D) * 0.25D))
                .add(0.0D, 0.15D + random.nextDouble() * 0.1D, 0.0D));
    }

    public void setCartridge(RevolverCartridge cartridge) {
        entityData.set(CARTRIDGE, (byte) cartridge.ordinal());
    }

    public RevolverCartridge getCartridge() {
        return RevolverCartridge.byOrdinal(entityData.get(CARTRIDGE));
    }

    public float getRoll(float partialTick) {
        return Mth.lerp(partialTick, prevRoll, clientRoll);
    }

    @Override
    public void tick() {
        super.tick();
        prevRoll = clientRoll;

        if (level().isClientSide) {
            if (entityData.get(RESTING)) {
                clientRoll = Mth.approach(clientRoll, 0.0F, 24.0F);
            } else {
                clientRoll += entityData.get(ROLL_SPEED);
            }
            return;
        }

        if (entityData.get(RESTING) && isSupported()) {
            setDeltaMovement(Vec3.ZERO);
            float settled = Mth.approach(entityData.get(ROLL), 0.0F, 24.0F);
            entityData.set(ROLL, settled);
            clientRoll = settled;
            setXRot(settled);
            return;
        }

        if (entityData.get(RESTING)) {
            setYRot(random.nextFloat() * 360.0F);
            entityData.set(ROLL_SPEED, 12.0F + random.nextFloat() * 16.0F);
            entityData.set(RESTING, false);
        }

        Vec3 motion = getDeltaMovement().add(0.0D, -0.04D, 0.0D);
        setDeltaMovement(motion);
        move(MoverType.SELF, motion);
        if (isSupported()) {
            setDeltaMovement(Vec3.ZERO);
            entityData.set(RESTING, true);
            entityData.set(ROLL_SPEED, 0.0F);
            float settled = Mth.approach(entityData.get(ROLL), 0.0F, 24.0F);
            entityData.set(ROLL, settled);
            clientRoll = settled;
            setXRot(settled);
            level().playSound(null, getX(), getY(), getZ(),
                    SoundRegistry.CARTRIDGE_HIT_GROUND.get(), SoundSource.NEUTRAL, 0.6F, 1.0F);
            return;
        }

        setDeltaMovement(getDeltaMovement().scale(0.98D));
        float nextRoll = entityData.get(ROLL) + entityData.get(ROLL_SPEED);
        entityData.set(ROLL, nextRoll);
        clientRoll = nextRoll;
        setXRot(nextRoll);
    }

    private boolean isSupported() {
        return !level().noCollision(this, getBoundingBox().move(0.0D, -0.03D, 0.0D));
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (level().isClientSide && (key.equals(ROLL) || key.equals(RESTING) || key.equals(ROLL_SPEED))) {
            if (key.equals(ROLL) && entityData.get(RESTING)) {
                clientRoll = entityData.get(ROLL);
            }
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!level().isClientSide) {
            ItemStack stack = new ItemStack(getCartridge().item());
            if (!player.addItem(stack)) {
                player.spawnAtLocation(stack);
            }
            level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F,
                    ((player.getRandom().nextFloat() - player.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F);
            discard();
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide) {
            discard();
        }
        return true;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(CARTRIDGE, (byte) RevolverCartridge.CASING.ordinal());
        entityData.define(ROLL, 0.0F);
        entityData.define(ROLL_SPEED, 0.0F);
        entityData.define(RESTING, false);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(CARTRIDGE, tag.getByte("Cartridge"));
        entityData.set(ROLL, tag.getFloat("Roll"));
        entityData.set(ROLL_SPEED, tag.getFloat("RollSpeed"));
        entityData.set(RESTING, tag.getBoolean("Resting"));
        prevRoll = entityData.get(ROLL);
        clientRoll = entityData.get(ROLL);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putByte("Cartridge", entityData.get(CARTRIDGE));
        tag.putFloat("Roll", entityData.get(ROLL));
        tag.putFloat("RollSpeed", entityData.get(ROLL_SPEED));
        tag.putBoolean("Resting", entityData.get(RESTING));
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public double getTick(Object animatable) {
        return tickCount;
    }
}
