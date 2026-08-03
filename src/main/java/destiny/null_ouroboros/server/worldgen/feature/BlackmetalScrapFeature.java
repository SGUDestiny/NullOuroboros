package destiny.null_ouroboros.server.worldgen.feature;

import destiny.null_ouroboros.server.block.SharpScrapBlock;
import destiny.null_ouroboros.server.registry.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BlackmetalScrapFeature extends Feature<NoneFeatureConfiguration> {
    private static final int MIN_SIZE = 8;
    private static final int MAX_SIZE = 16;
    private static final float SHARP_SCRAP_CHANCE = 0.80F;

    public BlackmetalScrapFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();

        int surfaceY = findSurfaceY(level, origin);
        if (surfaceY == Integer.MIN_VALUE) {
            return false;
        }

        int size = MIN_SIZE + random.nextInt(MAX_SIZE - MIN_SIZE + 1);
        int bury = 1 + random.nextInt(2);
        int minY = surfaceY - bury;
        int maxY = Math.min(surfaceY, minY + 1);
        if (maxY < surfaceY && random.nextBoolean()) {
            minY++;
            maxY++;
        }

        BlockPos start = new BlockPos(origin.getX(), minY, origin.getZ());
        if (!canReplace(level.getBlockState(start))) {
            start = new BlockPos(origin.getX(), maxY, origin.getZ());
            if (!canReplace(level.getBlockState(start))) {
                return false;
            }
        }

        BlockState scrap = BlockRegistry.BLACKMETAL_SCRAP_BLOCK.get().defaultBlockState();
        List<BlockPos> placed = new ArrayList<>();
        Set<BlockPos> placedSet = new HashSet<>();

        if (!tryPlaceScrap(level, start, scrap, placed, placedSet, minY, maxY)) {
            return false;
        }

        int attempts = 0;
        int maxAttempts = size * 24;
        while (placed.size() < size && attempts < maxAttempts) {
            attempts++;
            BlockPos base = placed.get(random.nextInt(placed.size()));
            Direction direction = Direction.values()[random.nextInt(Direction.values().length)];
            if (direction == Direction.DOWN && base.getY() <= minY) {
                continue;
            }
            if (direction == Direction.UP && base.getY() >= maxY) {
                continue;
            }

            BlockPos next = base.relative(direction);
            if (next.getY() < minY || next.getY() > maxY) {
                continue;
            }
            if (placedSet.contains(next)) {
                continue;
            }
            if (!hasSolidOrScrapSupport(level, next, placedSet)) {
                continue;
            }
            tryPlaceScrap(level, next, scrap, placed, placedSet, minY, maxY);
        }

        placeSharpScrap(level, random, placed);
        return true;
    }

    private static boolean tryPlaceScrap(WorldGenLevel level, BlockPos pos, BlockState scrap, List<BlockPos> placed, Set<BlockPos> placedSet, int minY, int maxY) {
        if (pos.getY() < minY || pos.getY() > maxY) {
            return false;
        }
        if (placedSet.contains(pos)) {
            return false;
        }
        if (!canReplace(level.getBlockState(pos))) {
            return false;
        }
        level.setBlock(pos, scrap, Block.UPDATE_CLIENTS);
        placed.add(pos.immutable());
        placedSet.add(pos.immutable());
        return true;
    }

    private static void placeSharpScrap(WorldGenLevel level, RandomSource random, List<BlockPos> scrapPositions) {
        BlockState sharpBase = BlockRegistry.SHARP_SCRAP.get().defaultBlockState();
        Set<BlockPos> occupied = new HashSet<>();

        for (BlockPos scrapPos : scrapPositions) {
            if (!level.getBlockState(scrapPos).is(BlockRegistry.BLACKMETAL_SCRAP_BLOCK.get())) {
                continue;
            }
            for (Direction direction : Direction.values()) {
                if (direction == Direction.DOWN) {
                    continue;
                }
                BlockPos target = scrapPos.relative(direction);
                if (occupied.contains(target)) {
                    continue;
                }
                BlockState existing = level.getBlockState(target);
                if (!existing.isAir()) {
                    continue;
                }
                if (random.nextFloat() > SHARP_SCRAP_CHANCE) {
                    continue;
                }
                BlockState sharp = sharpBase.setValue(SharpScrapBlock.FACING, direction);
                if (!sharp.canSurvive(level, target)) {
                    continue;
                }
                level.setBlock(target, sharp, Block.UPDATE_CLIENTS);
                occupied.add(target.immutable());
            }
        }
    }

    private static boolean hasSolidOrScrapSupport(WorldGenLevel level, BlockPos pos, Set<BlockPos> placedSet) {
        BlockPos below = pos.below();
        if (placedSet.contains(below)) {
            return true;
        }
        BlockState belowState = level.getBlockState(below);
        return belowState.isFaceSturdy(level, below, Direction.UP) || belowState.is(BlockRegistry.BLACKMETAL_SCRAP_BLOCK.get());
    }

    private static boolean canReplace(BlockState state) {
        return state.is(BlockRegistry.ASH_BLOCK.get())
                || state.is(BlockRegistry.TRAMPLED_ASH.get())
                || state.is(BlockRegistry.VEINED_ASH_BLOCK.get())
                || state.is(BlockRegistry.VEINED_TRAMPLED_ASH.get())
                || state.isAir()
                || state.canBeReplaced();
    }

    private static int findSurfaceY(WorldGenLevel level, BlockPos origin) {
        BlockPos.MutableBlockPos cursor = origin.mutable();
        for (int y = origin.getY() + 4; y >= origin.getY() - 8; y--) {
            cursor.setY(y);
            BlockState state = level.getBlockState(cursor);
            if ((state.is(BlockRegistry.ASH_BLOCK.get()) || state.is(BlockRegistry.TRAMPLED_ASH.get())
                    || state.is(BlockRegistry.VEINED_ASH_BLOCK.get()) || state.is(BlockRegistry.VEINED_TRAMPLED_ASH.get()))
                    && level.getBlockState(cursor.above()).isAir()) {
                return y;
            }
        }
        return Integer.MIN_VALUE;
    }
}
