package destiny.null_ouroboros.client.util;

import destiny.null_ouroboros.server.entity.HoistPartInteractionEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

public final class HoistPartTargeting {

    @Nullable
    public static HoistPartInteractionEntity findPartTarget(Minecraft minecraft) {
        Player player = minecraft.player;
        if (player == null) return null;

        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F);
        double reach = 6.0D;
        Vec3 end = eyePos.add(look.x * reach, look.y * reach, look.z * reach);

        AABB searchBox = player.getBoundingBox()
                .expandTowards(look.x * reach, look.y * reach, look.z * reach)
                .inflate(1.0D);

        List<HoistPartInteractionEntity> list = minecraft.level.getEntitiesOfClass(
                HoistPartInteractionEntity.class, searchBox,
                entity -> entity.isPickable() && entity.isAlive());

        HoistPartInteractionEntity best = null;
        double bestDist = reach * reach;
        for (HoistPartInteractionEntity part : list) {
            AABB box = part.getBoundingBox();
            Vec3 clip = box.clip(eyePos, end).orElse(null);
            if (clip != null) {
                double dist = eyePos.distanceToSqr(clip);
                if (dist < bestDist) {
                    bestDist = dist;
                    best = part;
                }
            }
        }
        return best;
    }

    public static boolean blocksVanillaUse(Minecraft minecraft) {
        return findPartTarget(minecraft) != null;
    }
}