package destiny.null_ouroboros.client.util;

import destiny.null_ouroboros.common.dusterbike.DusterbikeInteractionConstants;
import destiny.null_ouroboros.server.entity.DusterbikeEntity;
import destiny.null_ouroboros.server.entity.DusterbikeKeyEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class DusterbikeKeyTargeting {
    private DusterbikeKeyTargeting() {}

    @Nullable
    public static DusterbikeEntity findKeyTarget(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        Level level = minecraft.level;
        if (player == null || level == null) {
            return null;
        }

        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = eyePos.add(look.scale(DusterbikeInteractionConstants.PART_INTERACTION_REACH));
        AABB searchBox = player.getBoundingBox().inflate(DusterbikeInteractionConstants.PART_INTERACTION_REACH);

        DusterbikeKeyEntity bestKey = null;
        double bestDistance = Double.MAX_VALUE;

        for (Entity entity : level.getEntities(player, searchBox, e -> e instanceof DusterbikeKeyEntity)) {
            if (!(entity instanceof DusterbikeKeyEntity keyEntity)) {
                continue;
            }

            AABB keyBox = keyEntity.getBoundingBox();
            var clip = keyBox.clip(eyePos, end);
            if (clip.isEmpty()) {
                continue;
            }

            Vec3 hit = clip.get();
            double distance = eyePos.distanceToSqr(hit);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestKey = keyEntity;
            }
        }

        if (bestKey == null) {
            return null;
        }

        DusterbikeEntity parent = bestKey.getParent();
        return parent != null ? parent : bestKey.findParent();
    }

    public static boolean isTargetingKey(Minecraft minecraft) {
        return findKeyTarget(minecraft) != null;
    }

    public static boolean blocksVanillaUse(Minecraft minecraft) {
        if (findKeyTarget(minecraft) == null) {
            return false;
        }
        return minecraft.options.keyUse.isDown();
    }
}
