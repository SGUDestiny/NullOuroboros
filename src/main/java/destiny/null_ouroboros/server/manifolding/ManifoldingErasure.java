package destiny.null_ouroboros.server.manifolding;

import destiny.null_ouroboros.mixin.AccessorChunkMap;
import destiny.null_ouroboros.server.block.AshPileBlock;
import destiny.null_ouroboros.server.capability.ManifoldingCapability;
import destiny.null_ouroboros.server.capability.ManifoldingPhase;
import destiny.null_ouroboros.server.registry.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

public final class ManifoldingErasure {
    private static final int SURFACE_DEPTH = 4;
    private static final int MAX_COLUMN_SCAN = 48;
    private static final int ERASURE_ATTEMPTS = 256;
    private static final float ERASURE_CHANCE = 1.0f;

    private ManifoldingErasure() {}

    public static void tick(ServerLevel level, ManifoldingCapability cap) {
        if (cap.getPhase() != ManifoldingPhase.ACTIVE) return;
        if (!level.dimension().location().equals(ManifoldingCapability.DIMENSION_ID)) return;

        float windAngle = cap.getWindDirectionYaw();
        RandomSource random = level.getRandom();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        List<LevelChunk> tickingChunks = collectTickingChunks(level);
        if (tickingChunks.isEmpty()) return;

        for (int attempt = 0; attempt < ERASURE_ATTEMPTS; attempt++) {
            LevelChunk chunk = tickingChunks.get(random.nextInt(tickingChunks.size()));
            int x = chunk.getPos().getMinBlockX() + random.nextInt(16);
            int z = chunk.getPos().getMinBlockZ() + random.nextInt(16);

            int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
            if (surfaceY < level.getMinBuildHeight()) continue;

            int opaqueDepth = 0;
            for (int dy = 0; dy < MAX_COLUMN_SCAN && opaqueDepth <= SURFACE_DEPTH; dy++) {
                int y = surfaceY - dy;
                if (y < level.getMinBuildHeight()) break;

                pos.set(x, y, z);
                BlockState state = level.getBlockState(pos);

                if (state.isAir()) {
                    opaqueDepth = 0;
                    continue;
                }

                if (tryEraseBlock(level, pos, windAngle, random)) {
                    return;
                }

                if (state.canOcclude()) {
                    opaqueDepth++;
                }
            }
        }
    }

    private static List<LevelChunk> collectTickingChunks(ServerLevel level) {
        List<LevelChunk> tickingChunks = new ArrayList<>();
        for (ChunkHolder holder : ((AccessorChunkMap) level.getChunkSource().chunkMap).null_ouroboros$getChunks()) {
            LevelChunk chunk = holder.getTickingChunk();
            if (chunk != null) {
                tickingChunks.add(chunk);
            }
        }
        return tickingChunks;
    }

    public static boolean tryEraseBlock(ServerLevel level, BlockPos pos, float windAngle, RandomSource random) {
        BlockState state = level.getBlockState(pos);

        if (state.isAir() || !state.getFluidState().isEmpty()) return false;
        if (state.is(ManifoldingCapability.ISNT_CONVERTED_BY_MANIFOLDING)) return false;

        if (!hasExposedFace(level, pos)) return false;
        if (!ManifoldingCapability.isBlockExposedToWind(level, pos, windAngle)) return false;
        if (random.nextFloat() >= ERASURE_CHANCE) return false;

        convertBlock(level, pos, state);
        return true;
    }

    public static void convertBlock(ServerLevel level, BlockPos pos, BlockState state) {
        int layers = layersFromVisualShape(state, level, pos);

        level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, pos, Block.getId(state));
        level.playSound(null, pos, state.getSoundType().getBreakSound(), SoundSource.BLOCKS,
                1.0F, 0.8F + level.getRandom().nextFloat() * 0.4F);

        if (layers == 0) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        } else {
            level.playSound(null, pos, BlockRegistry.ASH_PILE.get().defaultBlockState().getSoundType().getPlaceSound(),
                    SoundSource.BLOCKS, 1.0F, 0.8F + level.getRandom().nextFloat() * 0.4F);
            level.setBlock(pos, BlockRegistry.ASH_PILE.get().defaultBlockState()
                    .setValue(AshPileBlock.LAYERS, layers), Block.UPDATE_ALL);
        }
    }

    public static int layersFromVisualShape(BlockState state, BlockGetter level, BlockPos pos) {
        VoxelShape shape = state.getShape(level, pos, CollisionContext.empty());
        if (shape.isEmpty()) return 0;

        double volume = shape.toAabbs().stream()
                .mapToDouble(aabb -> aabb.getXsize() * aabb.getYsize() * aabb.getZsize())
                .sum();

        return Mth.clamp(Mth.ceil(volume * 8.0 - 1e-6), 1, 8);
    }

    public static boolean hasExposedFace(ServerLevel level, BlockPos pos) {
        BlockPos.MutableBlockPos neighbor = new BlockPos.MutableBlockPos();
        for (Direction dir : Direction.values()) {
            neighbor.setWithOffset(pos, dir);
            if (!level.hasChunk(neighbor.getX() >> 4, neighbor.getZ() >> 4)) continue;
            if (level.getBlockState(neighbor).getLightBlock(level, neighbor) == 0) return true;
        }
        return false;
    }
}
