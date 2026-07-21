package destiny.null_ouroboros.server.entity.steel_leviathan;

import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.common.dimension.VergeOfRealityDimension;
import destiny.null_ouroboros.common.steel_leviathan.SteelLeviathanConstants;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class SteelLeviathanSightAdvancement {
    private static final ResourceLocation ADVANCEMENT_ID =
            ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "steel_leviathan_sight");

    private SteelLeviathanSightAdvancement() {}

    public static void tick(ServerPlayer player) {
        if (player.tickCount % SteelLeviathanConstants.SIGHT_ADVANCEMENT_CHECK_INTERVAL != 0) {
            return;
        }
        if (!VergeOfRealityDimension.isVergeOfReality(player.level())) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        Advancement advancement = level.getServer().getAdvancements().getAdvancement(ADVANCEMENT_ID);
        if (advancement == null) {
            return;
        }
        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
        if (progress.isDone()) {
            return;
        }

        double outer = level.getServer().getPlayerList().getViewDistance() * 16.0D;
        double inner = Math.max(0.0D, outer - SteelLeviathanConstants.SIGHT_ADVANCEMENT_RING_WIDTH);
        double outerSqr = outer * outer;
        double innerSqr = inner * inner;

        AABB search = player.getBoundingBox().inflate(outer + 8.0D);
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);

        for (SteelLeviathanHeadEntity head : level.getEntitiesOfClass(SteelLeviathanHeadEntity.class, search)) {
            if (!head.isAlive() || head.isUnderground()) {
                continue;
            }

            double dx = head.getX() - player.getX();
            double dz = head.getZ() - player.getZ();
            double distSqr = dx * dx + dz * dz;
            if (distSqr < innerSqr || distSqr > outerSqr) {
                continue;
            }

            Vec3 toHead = head.getEyePosition().subtract(eye);
            double len = toHead.length();
            if (len < 1.0E-4D) {
                continue;
            }
            if (look.dot(toHead.scale(1.0D / len)) <= 0.0D) {
                continue;
            }

            for (String criterion : progress.getRemainingCriteria()) {
                player.getAdvancements().award(advancement, criterion);
            }
            return;
        }
    }
}
