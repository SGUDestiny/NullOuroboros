package destiny.null_ouroboros.common.dusterbike;

import net.minecraft.nbt.CompoundTag;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public class DusterbikeEngineState {
    public static final int BIKE_FUEL_CAPACITY_MB = 12_000;
    public static final int FRAME_MAX_HEALTH = 100;

    private final EnumMap<DusterbikePartType, DusterbikePartState> parts = new EnumMap<>(DusterbikePartType.class);
    private int fuelMilliBuckets;
    private int frameHealth = FRAME_MAX_HEALTH;
    private UUID linkedBikeUuid;
    private UUID insertedKeyBikeUuid;
    private boolean currentIgnitionDoomed;

    public DusterbikeEngineState() {
        for (DusterbikePartType type : DusterbikePartType.values()) {
            parts.put(type, new DusterbikePartState(type));
        }
        part(DusterbikePartType.KEY).setInstalled(false);
    }

    public DusterbikePartState part(DusterbikePartType type) {
        return parts.get(type);
    }

    public Iterable<DusterbikePartState> parts() {
        return parts.values();
    }

    public boolean hasUsable(DusterbikePartType type) {
        return part(type).isUsable();
    }

    public int fuelMilliBuckets() {
        return fuelMilliBuckets;
    }

    public void setFuelMilliBuckets(int fuelMilliBuckets) {
        this.fuelMilliBuckets = Math.max(0, Math.min(BIKE_FUEL_CAPACITY_MB, fuelMilliBuckets));
    }

    public int addFuel(int amount) {
        if (amount <= 0) {
            return 0;
        }
        int accepted = Math.min(amount, BIKE_FUEL_CAPACITY_MB - fuelMilliBuckets);
        fuelMilliBuckets += accepted;
        return accepted;
    }

    public int removeFuel(int amount) {
        if (amount <= 0) {
            return 0;
        }
        int removed = Math.min(amount, fuelMilliBuckets);
        fuelMilliBuckets -= removed;
        return removed;
    }

    public int frameHealth() {
        return frameHealth;
    }

    public void setFrameHealth(int frameHealth) {
        this.frameHealth = Math.max(0, Math.min(FRAME_MAX_HEALTH, frameHealth));
    }

    public void damageFrame(float amount) {
        if (amount <= 0.0F) {
            return;
        }
        setFrameHealth(frameHealth - Math.round(amount));
    }

    public UUID linkedBikeUuid() {
        return linkedBikeUuid;
    }

    public void setLinkedBikeUuid(UUID linkedBikeUuid) {
        this.linkedBikeUuid = linkedBikeUuid;
    }

    public UUID insertedKeyBikeUuid() {
        return insertedKeyBikeUuid;
    }

    public void setInsertedKeyBikeUuid(UUID insertedKeyBikeUuid) {
        this.insertedKeyBikeUuid = insertedKeyBikeUuid;
        part(DusterbikePartType.KEY).setInstalled(insertedKeyBikeUuid != null);
    }

    public boolean currentIgnitionDoomed() {
        return currentIgnitionDoomed;
    }

    public void setCurrentIgnitionDoomed(boolean currentIgnitionDoomed) {
        this.currentIgnitionDoomed = currentIgnitionDoomed;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        CompoundTag partTags = new CompoundTag();
        for (Map.Entry<DusterbikePartType, DusterbikePartState> entry : parts.entrySet()) {
            partTags.put(entry.getKey().serializedName(), entry.getValue().save());
        }
        tag.put("Parts", partTags);
        tag.putInt("FuelMilliBuckets", fuelMilliBuckets);
        tag.putInt("FrameHealth", frameHealth);
        tag.putBoolean("CurrentIgnitionDoomed", currentIgnitionDoomed);
        if (linkedBikeUuid != null) {
            tag.putUUID("LinkedBikeUuid", linkedBikeUuid);
        }
        if (insertedKeyBikeUuid != null) {
            tag.putUUID("InsertedKeyBikeUuid", insertedKeyBikeUuid);
        }
        return tag;
    }

    public void load(CompoundTag tag) {
        if (tag.contains("Parts")) {
            CompoundTag partTags = tag.getCompound("Parts");
            for (DusterbikePartType type : DusterbikePartType.values()) {
                if (partTags.contains(type.serializedName())) {
                    part(type).load(partTags.getCompound(type.serializedName()));
                }
            }
        }
        if (tag.contains("FuelMilliBuckets")) {
            setFuelMilliBuckets(tag.getInt("FuelMilliBuckets"));
        }
        if (tag.contains("FrameHealth")) {
            setFrameHealth(tag.getInt("FrameHealth"));
        }
        currentIgnitionDoomed = tag.getBoolean("CurrentIgnitionDoomed");
        linkedBikeUuid = tag.hasUUID("LinkedBikeUuid") ? tag.getUUID("LinkedBikeUuid") : null;
        insertedKeyBikeUuid = tag.hasUUID("InsertedKeyBikeUuid") ? tag.getUUID("InsertedKeyBikeUuid") : null;
        part(DusterbikePartType.KEY).setInstalled(insertedKeyBikeUuid != null);
    }
}
