package destiny.null_ouroboros.server.worldgen.grower;

import destiny.null_ouroboros.server.registry.FeatureRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.grower.AbstractTreeGrower;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

import javax.annotation.Nullable;

public class ScorchedTreeGrower extends AbstractTreeGrower {
    @Override
    protected @Nullable ResourceKey<ConfiguredFeature<?, ?>> getConfiguredFeature(RandomSource randomSource, boolean b) {
        return FeatureRegistry.SCORCHED_TREE;
    }

    @Override
    public boolean growTree(ServerLevel pLevel, ChunkGenerator pGenerator, BlockPos pPos, BlockState pState, RandomSource pRandom) {
        return super.growTree(pLevel, pGenerator, pPos, pState, pRandom);
    }
}