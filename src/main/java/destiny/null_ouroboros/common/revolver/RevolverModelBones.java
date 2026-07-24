package destiny.null_ouroboros.common.revolver;

import net.minecraft.world.phys.Vec3;

public final class RevolverModelBones {
    private static final Vec3[] CARTRIDGE_PIVOTS = {
            new Vec3(-1.0D, 4.425D, 3.275D),
            new Vec3(-2.0D, 2.425D, 3.275D),
            new Vec3(-1.0D, 0.425D, 3.275D),
            new Vec3(1.0D, 0.425D, 3.275D),
            new Vec3(2.0D, 2.425D, 3.275D),
            new Vec3(1.0D, 4.425D, 3.275D)
    };
    private static final Vec3 CYLINDER_PIVOT = new Vec3(0.0D, 2.425D, 2.0D);

    private RevolverModelBones() {
    }

    public static Vec3 cartridgeOffset(int chamber) {
        int clamped = Math.floorMod(chamber, RevolverState.CHAMBER_COUNT);
        return CARTRIDGE_PIVOTS[clamped].subtract(CYLINDER_PIVOT).scale(1.0D / 16.0D);
    }
}
