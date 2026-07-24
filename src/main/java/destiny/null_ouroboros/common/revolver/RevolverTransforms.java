package destiny.null_ouroboros.common.revolver;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class RevolverTransforms {
    private RevolverTransforms() {
    }

    public static Vec3 cartridgeWorldPosition(Player player, ItemStack revolver, int chamber) {
        Vec3 look = player.getLookAngle().normalize();
        Vec3 right = new Vec3(look.z, 0.0D, -look.x).normalize();
        Vec3 local = rotateCylinder(RevolverModelBones.cartridgeOffset(chamber), RevolverState.getCylinderAngle(revolver));
        Vec3 base = player.getEyePosition().add(look.scale(0.45D)).add(right.scale(0.18D)).add(0.0D, -0.28D, 0.0D);
        return base.add(right.scale(local.x)).add(0.0D, local.y, 0.0D).add(look.scale(-local.z));
    }

    private static Vec3 rotateCylinder(Vec3 value, float degrees) {
        double radians = Math.toRadians(degrees);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new Vec3(value.x * cos - value.y * sin, value.x * sin + value.y * cos, value.z);
    }
}
