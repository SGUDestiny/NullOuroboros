package destiny.null_ouroboros.server.entity.steel_leviathan;

import destiny.null_ouroboros.common.steel_leviathan.SteelLeviathanConstants;
import destiny.null_ouroboros.server.entity.ParentLinkedHitboxEntity;
import destiny.null_ouroboros.server.registry.SoundRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public class SteelLeviathanHeatsinkHitboxEntity extends ParentLinkedHitboxEntity {
    private static final EntityDataAccessor<Integer> SLOT =
            SynchedEntityData.defineId(SteelLeviathanHeatsinkHitboxEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> HEALTH =
            SynchedEntityData.defineId(SteelLeviathanHeatsinkHitboxEntity.class, EntityDataSerializers.FLOAT);

    public SteelLeviathanHeatsinkHitboxEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineAdditionalSynchedData() {
        this.entityData.define(SLOT, 0);
        this.entityData.define(HEALTH, SteelLeviathanConstants.HEATSINK_MAX_HP);
    }

    public void init(SteelLeviathanPartEntity parent, int slot, double x, double y, double z) {
        initParentLink(parent.getId(), parent.getUUID(), x, y, z);
        this.entityData.set(SLOT, slot);
        resetHealth();
    }

    public int getSlot() {
        return this.entityData.get(SLOT);
    }

    public float getHeatsinkHealth() {
        return this.entityData.get(HEALTH);
    }

    public void resetHealth() {
        this.entityData.set(HEALTH, SteelLeviathanConstants.HEATSINK_MAX_HP);
    }

    @Override
    protected Entity resolveParent() {
        return findParentOfType(SteelLeviathanPartEntity.class);
    }

    @Override
    protected AABB makeBoundingBox() {
        float half = SteelLeviathanConstants.HEATSINK_SIZE * 0.5F;
        return new AABB(this.getX() - half, this.getY() - half, this.getZ() - half,
                this.getX() + half, this.getY() + half, this.getZ() + half);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        SteelLeviathanPartEntity parent = findParentOfType(SteelLeviathanPartEntity.class);
        if (parent == null) {
            return;
        }
        if (!parent.isHeatsinkSlotActive(getSlot())) {
            discard();
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide || isInvulnerableTo(source) || source.is(DamageTypeTags.IS_FALL)) {
            return false;
        }
        if (amount > SteelLeviathanConstants.ANTIBUTCHER_THRESHOLD) {
            return false;
        }
        SteelLeviathanPartEntity parent = findParentOfType(SteelLeviathanPartEntity.class);
        if (parent == null || !parent.isHeatsinkSlotActive(getSlot())) {
            return false;
        }
        if (!parent.damageHeatsink(getSlot(), source, amount)) {
            return false;
        }
        float pitch = 0.9F + random.nextFloat() * 0.2F;
        playSound(SoundRegistry.STEEL_LEVIATHAN_HEATSINK_HIT.get(), SteelLeviathanConstants.SOUND_VOLUME_64, pitch);
        float next = getHeatsinkHealth() - amount;
        this.entityData.set(HEALTH, next);
        if (next <= 0.0F) {
            playSound(SoundRegistry.STEEL_LEVIATHAN_HEATSINK_HISS.get(), SteelLeviathanConstants.SOUND_VOLUME_64, pitch);
            parent.onHeatsinkBroken(getSlot());
            discard();
        }
        return true;
    }

    @Override
    public boolean isPickable() {
        SteelLeviathanPartEntity parent = findParentOfType(SteelLeviathanPartEntity.class);
        return parent != null && parent.isHeatsinkSlotActive(getSlot());
    }

    @Override
    protected void readAdditionalHitboxData(CompoundTag tag) {
        this.entityData.set(SLOT, tag.getInt("Slot"));
        this.entityData.set(HEALTH, tag.contains("Health") ? tag.getFloat("Health") : SteelLeviathanConstants.HEATSINK_MAX_HP);
    }

    @Override
    protected void writeAdditionalHitboxData(CompoundTag tag) {
        tag.putInt("Slot", getSlot());
        tag.putFloat("Health", getHeatsinkHealth());
    }
}

