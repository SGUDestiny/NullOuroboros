package destiny.null_ouroboros.server.worldgen.decorator;

import com.mojang.serialization.MapCodec;
import destiny.null_ouroboros.server.registry.BlockRegistry;
import destiny.null_ouroboros.server.registry.FeatureRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;

import java.util.Comparator;
import java.util.List;

public class VeinedConeDecorator extends TreeDecorator {
    public static final MapCodec<VeinedConeDecorator> CODEC = MapCodec.unit(VeinedConeDecorator::new);

    @Override
    protected TreeDecoratorType<?> type() {
        return FeatureRegistry.ROOT_CONE.get();
    }

    @Override
    public void place(Context context) {
        List<BlockPos> logs = context.logs();
        if (logs.isEmpty()) return;

        BlockPos base = logs.stream().min(Comparator.comparingInt(BlockPos::getY)).orElseThrow().below();

        RandomSource random = context.random();
        LevelReader level = (LevelAccessor) context.level();
        int maxDepth = 8;

        for (int depth = 1; depth <= maxDepth; depth++) {
            int y = base.getY() - depth;
            double radius = 1.0 + depth * 0.8;
            int maxR = (int) Math.ceil(radius);

            for (int dx = -maxR; dx <= maxR; dx++) {
                for (int dz = -maxR; dz <= maxR; dz++) {
                    double distSq = dx * dx + dz * dz;
                    if (distSq > radius * radius) continue;

                    BlockPos pos = base.offset(dx, -depth, dz);
                    BlockState current = level.getBlockState(pos);

                    BlockState rootState = null;
                    if (current.is(BlockRegistry.ASH_BLOCK.get())) {
                        rootState = BlockRegistry.VEINED_ASH_BLOCK.get().defaultBlockState();
                    } else if (current.is(BlockRegistry.TRAMPLED_ASH.get())) {
                        rootState = BlockRegistry.VEINED_TRAMPLED_ASH.get().defaultBlockState();
                    }

                    if (rootState == null) continue;

                    if (depth == 1 && dx == 0 && dz == 0) {
                        context.setBlock(pos.above(), rootState);
                        continue;
                    }

                    double depthFactor = 1.0 - (depth / (double) maxDepth);
                    double radialFactor = 1.0 - (Math.sqrt(distSq) / radius);
                    double chance = 0.8 * depthFactor * radialFactor;

                    if (random.nextDouble() < chance) {
                        context.setBlock(pos, rootState);
                    }
                }
            }
        }
    }
}
