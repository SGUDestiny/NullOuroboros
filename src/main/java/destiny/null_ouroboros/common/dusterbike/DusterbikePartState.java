package destiny.null_ouroboros.common.dusterbike;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

public class DusterbikePartState {
    public static final String ITEM_PART_TAG = "DusterbikePart";
    public static final String ITEM_PART_TYPE_TAG = "PartType";
    public static final String ITEM_DURABILITY_TAG = "PartDurability";
    public static final String ITEM_MAIN_COLOR_TAG = "MainColor";
    public static final String ITEM_GLOW_COLOR_TAG = "GlowColor";

    private final DusterbikePartType type;
    private int durability;
    private boolean installed;
    private Integer mainColor;
    private Integer glowColor;

    public DusterbikePartState(DusterbikePartType type) {
        this(type, type.maxDurability(), true);
    }

    public DusterbikePartState(DusterbikePartType type, int durability, boolean installed) {
        this.type = type;
        this.durability = clampDurability(durability);
        this.installed = installed;
    }

    public DusterbikePartType type() {
        return type;
    }

    public int durability() {
        return durability;
    }

    public int maxDurability() {
        return type.maxDurability();
    }

    public boolean installed() {
        return installed;
    }

    public void setInstalled(boolean installed) {
        this.installed = installed;
    }

    public boolean isUsable() {
        return installed && (!type.hasDurability() || durability > 0);
    }

    public Integer mainColor() {
        return mainColor;
    }

    public void setMainColor(Integer mainColor) {
        this.mainColor = normalizeColor(mainColor);
    }

    public Integer glowColor() {
        return glowColor;
    }

    public void setGlowColor(Integer glowColor) {
        this.glowColor = normalizeColor(glowColor);
    }

    public void damage(int amount) {
        if (amount <= 0 || !type.hasDurability()) {
            return;
        }
        durability = clampDurability(durability - amount);
    }

    public void setDurability(int durability) {
        this.durability = clampDurability(durability);
    }

    public void copyFrom(DusterbikePartState other) {
        if (other == null || other.type != this.type) {
            return;
        }
        this.durability = clampDurability(other.durability);
        this.installed = other.installed;
        this.mainColor = other.mainColor;
        this.glowColor = other.glowColor;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Type", type.serializedName());
        tag.putInt("Durability", durability);
        tag.putBoolean("Installed", installed);
        if (mainColor != null) {
            tag.putInt("MainColor", mainColor);
        }
        if (glowColor != null) {
            tag.putInt("GlowColor", glowColor);
        }
        return tag;
    }

    public void load(CompoundTag tag) {
        if (tag.contains("Durability")) {
            durability = clampDurability(tag.getInt("Durability"));
        }
        if (tag.contains("Installed")) {
            installed = tag.getBoolean("Installed");
        }
        mainColor = tag.contains("MainColor") ? normalizeColor(tag.getInt("MainColor")) : null;
        glowColor = tag.contains("GlowColor") ? normalizeColor(tag.getInt("GlowColor")) : null;
    }

    public CompoundTag saveForItem() {
        CompoundTag tag = new CompoundTag();
        tag.putString(ITEM_PART_TYPE_TAG, type.serializedName());
        tag.putInt(ITEM_DURABILITY_TAG, durability);
        if (mainColor != null) {
            tag.putInt(ITEM_MAIN_COLOR_TAG, mainColor);
        }
        if (glowColor != null) {
            tag.putInt(ITEM_GLOW_COLOR_TAG, glowColor);
        }
        return tag;
    }

    public void loadFromItem(CompoundTag tag) {
        if (tag.contains(ITEM_DURABILITY_TAG)) {
            durability = clampDurability(tag.getInt(ITEM_DURABILITY_TAG));
        }
        mainColor = tag.contains(ITEM_MAIN_COLOR_TAG) ? normalizeColor(tag.getInt(ITEM_MAIN_COLOR_TAG)) : null;
        glowColor = tag.contains(ITEM_GLOW_COLOR_TAG) ? normalizeColor(tag.getInt(ITEM_GLOW_COLOR_TAG)) : null;
    }

    private int clampDurability(int value) {
        if (!type.hasDurability()) {
            return 0;
        }
        return Mth.clamp(value, 0, type.maxDurability());
    }

    private static Integer normalizeColor(Integer color) {
        return color == null ? null : color & 0xFFFFFF;
    }
}
