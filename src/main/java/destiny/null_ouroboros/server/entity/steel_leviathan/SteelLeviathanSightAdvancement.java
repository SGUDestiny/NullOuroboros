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

public final class SteelLeviathanSightAdvancement {
    private static final ResourceLocation ADVANCEMENT_ID =
            ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "steel_leviathan_sighted");

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

        double range = level.getServer().getPlayerList().getViewDistance() * 16.0D;
        double rangeSqr = range * range;
        AABB search = player.getBoundingBox().inflate(range);

        for (SteelLeviathanPartEntity part : level.getEntitiesOfClass(SteelLeviathanPartEntity.class, search)) {
            if (!part.isAlive() || part.isUnderground()) {
                continue;
            }
            if (player.distanceToSqr(part) > rangeSqr) {
                continue;
            }

            for (String criterion : progress.getRemainingCriteria()) {
                player.getAdvancements().award(advancement, criterion);
            }
            return;
        }
    }
}
