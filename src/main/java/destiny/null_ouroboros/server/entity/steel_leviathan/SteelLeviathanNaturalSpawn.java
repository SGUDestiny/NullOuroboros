package destiny.null_ouroboros.server.entity.steel_leviathan;

import destiny.null_ouroboros.common.steel_leviathan.SteelLeviathanConstants;
import destiny.null_ouroboros.server.registry.EntityRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

public final class SteelLeviathanNaturalSpawn {
    private SteelLeviathanNaturalSpawn() {
    }

    public static void trySpawnInNewChunk(ServerLevel level, LevelChunk chunk) {
        if (SteelLeviathanConstants.NATURAL_SPAWN_CHANCE <= 0) {
            return;
        }
        long chunkKey = chunk.getPos().toLong();
        if (!SteelLeviathanSpawnData.get(level).tryMarkConsidered(chunkKey)) {
            return;
        }
        if (level.random.nextInt(SteelLeviathanConstants.NATURAL_SPAWN_CHANCE) != 0) {
            return;
        }
        if (!SteelLeviathanHeadEntity.canNaturalSpawn(level)) {
            return;
        }

        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        double x = minX + level.random.nextInt(16) + 0.5D;
        double z = minZ + level.random.nextInt(16) + 0.5D;
        int groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z));
        if (groundY <= level.getMinBuildHeight()) {
            return;
        }

        SteelLeviathanHeadEntity head = new SteelLeviathanHeadEntity(
                EntityRegistry.STEEL_LEVIATHAN_HEAD.get(), level);
        head.setPos(x, groundY, z);
        head.setYRot(level.random.nextFloat() * 360.0F);
        head.setNaturalSpawn(true);
        SteelLeviathanChunkTickets.forceChunkNow(level, head.getUUID(), x, z);
        level.addFreshEntity(head);
    }
}
