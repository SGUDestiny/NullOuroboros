package destiny.null_ouroboros.common.revolver;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public final class RevolverState {
    public static final int CHAMBER_COUNT = 6;
    private static final String CHAMBERS_TAG = "RevolverChambers";
    private static final String SELECTED_TAG = "RevolverSelected";
    private static final String RELOAD_TAG = "RevolverReload";
    private static final String COCKED_TAG = "RevolverCocked";
    private static final String CYLINDER_ANGLE_TAG = "RevolverCylinderAngle";
    private static final String VISUAL_ID_TAG = "RevolverVisualId";

    private RevolverState() {
    }

    public static RevolverCartridge getChamber(ItemStack stack, int chamber) {
        if (chamber < 0 || chamber >= CHAMBER_COUNT) {
            return RevolverCartridge.EMPTY;
        }
        int[] chambers = stack.getOrCreateTag().getIntArray(CHAMBERS_TAG);
        return chamber < chambers.length ? RevolverCartridge.byOrdinal(chambers[chamber]) : RevolverCartridge.EMPTY;
    }

    public static void setChamber(ItemStack stack, int chamber, RevolverCartridge cartridge) {
        if (chamber < 0 || chamber >= CHAMBER_COUNT) {
            return;
        }
        CompoundTag tag = stack.getOrCreateTag();
        int[] chambers = tag.getIntArray(CHAMBERS_TAG);
        if (chambers.length != CHAMBER_COUNT) {
            int[] normalized = new int[CHAMBER_COUNT];
            System.arraycopy(chambers, 0, normalized, 0, Math.min(chambers.length, CHAMBER_COUNT));
            chambers = normalized;
        }
        chambers[chamber] = cartridge.ordinal();
        tag.putIntArray(CHAMBERS_TAG, chambers);
    }

    public static int getSelected(ItemStack stack) {
        return Mth.positiveModulo(stack.getOrCreateTag().getInt(SELECTED_TAG), CHAMBER_COUNT);
    }

    public static void rotate(ItemStack stack, int direction) {
        if (direction == 0) {
            return;
        }
        int step = direction > 0 ? 1 : -1;
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt(SELECTED_TAG, Mth.positiveModulo(getSelected(stack) + step, CHAMBER_COUNT));
        tag.putFloat(CYLINDER_ANGLE_TAG, getCylinderAngle(stack) + 60.0F * step);
    }

    public static boolean isReloading(ItemStack stack) {
        return stack.getOrCreateTag().getBoolean(RELOAD_TAG);
    }

    public static void setReloading(ItemStack stack, boolean reloading) {
        stack.getOrCreateTag().putBoolean(RELOAD_TAG, reloading);
    }

    public static boolean isCocked(ItemStack stack) {
        return stack.getOrCreateTag().getBoolean(COCKED_TAG);
    }

    public static void setCocked(ItemStack stack, boolean cocked) {
        stack.getOrCreateTag().putBoolean(COCKED_TAG, cocked);
    }

    public static float getCylinderAngle(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        return tag.contains(CYLINDER_ANGLE_TAG) ? tag.getFloat(CYLINDER_ANGLE_TAG) : getSelected(stack) * 60.0F;
    }

    public static boolean allEmpty(ItemStack stack) {
        for (int chamber = 0; chamber < CHAMBER_COUNT; chamber++) {
            if (getChamber(stack, chamber) != RevolverCartridge.EMPTY) {
                return false;
            }
        }
        return true;
    }

    public static UUID ensureVisualId(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.hasUUID(VISUAL_ID_TAG)) {
            tag.putUUID(VISUAL_ID_TAG, UUID.randomUUID());
        }
        return tag.getUUID(VISUAL_ID_TAG);
    }

    public static UUID getVisualId(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.hasUUID(VISUAL_ID_TAG) ? tag.getUUID(VISUAL_ID_TAG) : null;
    }
}
