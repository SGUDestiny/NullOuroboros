package destiny.null_ouroboros.server.block;

import destiny.null_ouroboros.server.registry.ParticleTypeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;

public class AshBlock extends FallingBlock {
    public AshBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource randomSource) {
        if (randomSource.nextFloat() > 0.95f && level.getBlockState(pos.above()).isAir()) {
            ParticleUtils.spawnParticleBelow(level, pos.above(), level.random, ParticleTypeRegistry.ASH.get());
        }
    }
}
