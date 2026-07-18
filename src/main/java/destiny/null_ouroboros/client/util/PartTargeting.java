package destiny.null_ouroboros.client.util;

import destiny.null_ouroboros.common.dusterbike.DusterbikeInteractionConstants;
import destiny.null_ouroboros.common.dusterbike.DusterbikePartTargetType;
import destiny.null_ouroboros.server.entity.DusterbikeEntity;
import destiny.null_ouroboros.server.entity.DusterbikePartInteractionEntity;
import destiny.null_ouroboros.server.entity.HoistPartInteractionEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

public final class PartTargeting {
    private PartTargeting() {}

    @Nullable
    public static HoistPartInteractionEntity findHoistPartTarget(Minecraft minecraft) {
        return findClosest(minecraft, HoistPartInteractionEntity.class, Entity::isPickable);
    }

    @Nullable
    public static BikePartTarget findBikePartTarget(Minecraft minecraft) {
        DusterbikePartInteractionEntity best = findClosest(minecraft, DusterbikePartInteractionEntity.class, e -> true);
        if (best == null) {
            return null;
        }
        DusterbikeEntity bike = best.getParent();
        if (bike == null) {
            bike = best.findParent();
        }
        return bike == null ? null : new BikePartTarget(bike, best.getTargetType(), best);
    }

    public static boolean blocksVanillaUse(Minecraft minecraft) {
        if (minecraft.options == null || !minecraft.options.keyUse.isDown()) {
            return false;
        }
        return findHoistPartTarget(minecraft) != null || findBikePartTarget(minecraft) != null;
    }

    @Nullable
    private static <T extends Entity> T findClosest(Minecraft minecraft, Class<T> type, Predicate<T> filter) {
        Player player = minecraft.player;
        Level level = minecraft.level;
        if (player == null || level == null) {
            return null;
        }

        double reach = DusterbikeInteractionConstants.PART_INTERACTION_REACH;
        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = eyePos.add(look.scale(reach));
        AABB searchBox = player.getBoundingBox()
                .expandTowards(look.x * reach, look.y * reach, look.z * reach)
                .inflate(1.0D);

        List<T> candidates = level.getEntitiesOfClass(type, searchBox,
                entity -> entity.isAlive() && filter.test(entity));

        T best = null;
        double bestDist = reach * reach;
        for (T entity : candidates) {
            var clip = entity.getBoundingBox().clip(eyePos, end);
            if (clip.isEmpty()) {
                continue;
            }
            double dist = eyePos.distanceToSqr(clip.get());
            if (dist < bestDist) {
                bestDist = dist;
                best = entity;
            }
        }
        return best;
    }

    public record BikePartTarget(
            DusterbikeEntity bike,
            DusterbikePartTargetType targetType,
            DusterbikePartInteractionEntity entity) {
    }
}
