package destiny.null_ouroboros.client.util;

import destiny.null_ouroboros.common.DusterbikeEngineSoundConstants;
import destiny.null_ouroboros.common.dusterbike.DusterbikePartTargetType;
import destiny.null_ouroboros.server.entity.DusterbikeEntity;
import destiny.null_ouroboros.server.entity.DusterbikePartInteractionEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class DusterbikePartTargeting {
    private DusterbikePartTargeting() {}

    @Nullable
    public static Target findPartTarget(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        Level level = minecraft.level;
        if (player == null || level == null) {
            return null;
        }

        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = eyePos.add(look.scale(DusterbikeEngineSoundConstants.KEY_INTERACTION_REACH));
        AABB searchBox = player.getBoundingBox().inflate(DusterbikeEngineSoundConstants.KEY_INTERACTION_REACH);

        DusterbikePartInteractionEntity bestTarget = null;
        double bestDistance = Double.MAX_VALUE;

        for (Entity entity : level.getEntities(player, searchBox, e -> e instanceof DusterbikePartInteractionEntity)) {
            if (!(entity instanceof DusterbikePartInteractionEntity targetEntity)) {
                continue;
            }

            var clip = targetEntity.getBoundingBox().clip(eyePos, end);
            if (clip.isEmpty()) {
                continue;
            }

            double distance = eyePos.distanceToSqr(clip.get());
            if (distance < bestDistance) {
                bestDistance = distance;
                bestTarget = targetEntity;
            }
        }

        if (bestTarget == null) {
            return null;
        }

        DusterbikeEntity bike = bestTarget.getParent();
        if (bike == null) {
            bike = bestTarget.findParent();
        }
        return bike == null ? null : new Target(bike, bestTarget.getTargetType(), bestTarget);
    }

    public static boolean isTargetingPart(Minecraft minecraft) {
        return findPartTarget(minecraft) != null;
    }

    public static boolean blocksVanillaUse(Minecraft minecraft) {
        return findPartTarget(minecraft) != null && minecraft.options.keyUse.isDown();
    }

    public record Target(
            DusterbikeEntity bike,
            DusterbikePartTargetType targetType,
            DusterbikePartInteractionEntity entity) {
    }
}
