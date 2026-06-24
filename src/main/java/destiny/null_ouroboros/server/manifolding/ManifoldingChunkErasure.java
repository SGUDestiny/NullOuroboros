package destiny.null_ouroboros.server.manifolding;

import destiny.null_ouroboros.server.capability.ManifoldingCapability;
import destiny.null_ouroboros.server.capability.ManifoldingPhase;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;

public final class ManifoldingChunkErasure {
    private ManifoldingChunkErasure() {}

    public static void processNewChunk(ServerLevel level, LevelChunk chunk, ManifoldingCapability cap) {
        if (cap.getPhase() != ManifoldingPhase.ACTIVE) return;

        long elapsed = level.getGameTime() - cap.getPhaseStartTime();
        float chance = Mth.clamp((float) elapsed / cap.getActiveDuration(), 0.0f, 1.0f);
        if (chance <= 0) return;

        RandomSource random = level.getRandom();
        int minSection = chunk.getMinSection();
        int sectionCount = chunk.getMaxSection() - minSection;
        int chunkX = chunk.getPos().getMinBlockX();
        int chunkZ = chunk.getPos().getMinBlockZ();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int sectionIdx = 0; sectionIdx < sectionCount; sectionIdx++) {
            LevelChunkSection section = chunk.getSection(sectionIdx);
            if (section.hasOnlyAir()) continue;

            if (!section.getStates().maybeHas(state -> !state.is(ManifoldingCapability.ISNT_CONVERTED_BY_MANIFOLDING))) continue;

            int sectionY = (minSection + sectionIdx) * 16;

            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        BlockState state = section.getBlockState(x, y, z);
                        if (state.is(ManifoldingCapability.ISNT_CONVERTED_BY_MANIFOLDING)) continue;

                        pos.set(chunkX + x, sectionY + y, chunkZ + z);

                        int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ()) - 1;
                        if (pos.getY() < surfaceY) continue;

                        if (!ManifoldingErasure.hasExposedFace(level, pos)) continue;
                        if (random.nextFloat() >= chance) continue;

                        ManifoldingErasure.convertBlock(level, pos, state);
                    }
                }
            }
        }
    }
}
