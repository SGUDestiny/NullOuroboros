package destiny.null_ouroboros.server.worldgen.feature;

import destiny.null_ouroboros.server.block.AshPileBlock;
import destiny.null_ouroboros.server.registry.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.HashMap;
import java.util.Map;

public class AshPileFeature extends Feature<NoneFeatureConfiguration> {
    public AshPileFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        ChunkAccess chunk = level.getChunk(origin);

        int minX = chunk.getPos().getMinBlockX();
        int maxX = chunk.getPos().getMaxBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        int maxZ = chunk.getPos().getMaxBlockZ();
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();

        int[][] localHeights = new int[16][16];
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                int h = minY;

                for (int y = maxY; y >= minY; y--) {
                    pos.set(x, y, z);
                    BlockState state = chunk.getBlockState(pos);

                    if (!state.isAir() && !state.is(Blocks.WATER) && !state.is(BlockRegistry.ASH_PILE.get())) {
                        h = y;
                        break;
                    }
                }

                localHeights[x - minX][z - minZ] = h;
            }
        }

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                int surfaceY = -1;
                for (int y = maxY; y >= minY; y--) {
                    pos.set(x, y, z);
                    BlockState state = chunk.getBlockState(pos);

                    if (state.is(BlockRegistry.ASH_BLOCK.get()) && y + 1 <= maxY && chunk.getBlockState(pos.above()).isAir()) {
                        surfaceY = y;
                        break;
                    }
                }

                if (surfaceY == -1) continue;

                int layers = calculateLayers(level, localHeights, chunk, x, z, surfaceY, minY);
                if (layers > 0) {
                    pos.set(x, surfaceY + 1, z);
                    BlockState pile = BlockRegistry.ASH_PILE.get().defaultBlockState().setValue(AshPileBlock.LAYERS, layers);
                    chunk.setBlockState(pos, pile, false);
                }
            }
        }
        return true;
    }

    private int calculateLayers(WorldGenLevel level, int[][] localHeights, ChunkAccess homeChunk, int worldX, int worldZ, int myHeight, int minY) {
        final int SEARCH_RADIUS = 4;
        int closestDist = Integer.MAX_VALUE;

        Map<Long, Integer> externalHeightCache = new HashMap<>();

        for (int dx = -SEARCH_RADIUS; dx <= SEARCH_RADIUS; dx++) {
            for (int dz = -SEARCH_RADIUS; dz <= SEARCH_RADIUS; dz++) {
                if (dx == 0 && dz == 0) continue;

                int nx = worldX + dx;
                int nz = worldZ + dz;
                int nh;

                if (nx >= homeChunk.getPos().getMinBlockX() && nx <= homeChunk.getPos().getMaxBlockX() &&
                        nz >= homeChunk.getPos().getMinBlockZ() && nz <= homeChunk.getPos().getMaxBlockZ()) {
                    nh = localHeights[nx - homeChunk.getPos().getMinBlockX()][nz - homeChunk.getPos().getMinBlockZ()];
                } else {
                    long key = ((long) nx << 32) | (nz & 0xFFFFFFFFL);
                    Integer cached = externalHeightCache.get(key);

                    if (cached != null) {
                        nh = cached;
                    } else {
                        nh = getTopSolidHeightInColumn(level, nx, nz, minY);
                        externalHeightCache.put(key, nh);
                    }
                }

                if (nh > myHeight) {
                    int dist = Math.max(Math.abs(dx), Math.abs(dz));

                    if (dist < closestDist) {
                        closestDist = dist;
                        if (closestDist == 1) break;
                    }
                }
            }

            if (closestDist == 1) break;
        }

        if (closestDist == Integer.MAX_VALUE) return 0;

        return switch (closestDist) {
            case 1 -> 6;
            case 2 -> 4;
            case 3 -> 2;
            default -> 0;
        };
    }

    private int getTopSolidHeightInColumn(WorldGenLevel level, int x, int z, int minY) {
        ChunkAccess chunk = level.getChunk(new BlockPos(x, 0, z));
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int maxY = level.getMaxBuildHeight();

        for (int y = maxY; y >= minY; y--) {
            pos.set(x, y, z);
            BlockState state = chunk.getBlockState(pos);

            if (!state.isAir() && !state.is(BlockRegistry.ASH_PILE.get())) {
                return y;
            }
        }
        return minY;
    }
}