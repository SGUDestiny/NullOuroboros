package destiny.null_ouroboros.server.worldgen.feature;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import destiny.null_ouroboros.server.registry.FeatureRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacerType;

import java.util.List;
import java.util.function.BiConsumer;

public class ScorchedTrunkPlacer extends TrunkPlacer {
    public static final Codec<ScorchedTrunkPlacer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("base_height").forGetter(tree -> tree.baseHeight),
            Codec.INT.fieldOf("height_rand_a").forGetter(tree -> tree.heightRandA),
            Codec.INT.fieldOf("height_rand_b").forGetter(tree -> tree.heightRandB)
    ).apply(instance, ScorchedTrunkPlacer::new));

    public ScorchedTrunkPlacer(int pBaseHeight, int pHeightRandA, int pHeightRandB) {
        super(pBaseHeight, pHeightRandA, pHeightRandB);
    }

    @Override
    protected TrunkPlacerType<?> type() {
        return FeatureRegistry.SCORCHED_TRUNK.get();
    }

    @Override
    public List<FoliagePlacer.FoliageAttachment> placeTrunk(LevelSimulatedReader level, BiConsumer<BlockPos, BlockState> biConsumer, RandomSource randomSource, int i, BlockPos blockPos, TreeConfiguration treeConfiguration) {
        List<FoliagePlacer.FoliageAttachment> foliageAttachment = Lists.newArrayList();
        Direction direction = Direction.from3DDataValue(randomSource.nextInt(2, 6));

        BlockPos trackPos = blockPos;
        this.placeLog(level, biConsumer, randomSource, trackPos, treeConfiguration);

        trackPos = trackPos.above();
        this.placeLog(level, biConsumer, randomSource, trackPos, treeConfiguration);

        trackPos = trackPos.above();
        this.placeLog(level, biConsumer, randomSource, trackPos, treeConfiguration);



        BlockPos branchPos1 = trackPos.relative(direction);
        BlockPos branchPos2 = trackPos.relative(direction.getOpposite());
        trackPos = trackPos.above();

        this.placeLog(level, biConsumer, randomSource, branchPos1, treeConfiguration, (state) -> state.setValue(RotatedPillarBlock.AXIS, direction.getAxis()));

        branchPos1 = branchPos1.relative(direction);
        this.placeLog(level, biConsumer, randomSource, branchPos1, treeConfiguration, (state) -> state.setValue(RotatedPillarBlock.AXIS, direction.getAxis()));

        branchPos1 = branchPos1.above();
        this.placeLog(level, biConsumer, randomSource, branchPos1, treeConfiguration);

        branchPos1 = branchPos1.relative(direction);
        this.placeLog(level, biConsumer, randomSource, branchPos1, treeConfiguration, (state) -> state.setValue(RotatedPillarBlock.AXIS, direction.getAxis()));



        this.placeLog(level, biConsumer, randomSource, branchPos2, treeConfiguration, (state) -> state.setValue(RotatedPillarBlock.AXIS, direction.getOpposite().getAxis()));

        branchPos2 = branchPos2.above();
        this.placeLog(level, biConsumer, randomSource, branchPos2, treeConfiguration);

        branchPos2 = branchPos2.above();
        this.placeLog(level, biConsumer, randomSource, branchPos2, treeConfiguration);

        branchPos2 = branchPos2.relative(direction.getOpposite());
        this.placeLog(level, biConsumer, randomSource, branchPos2, treeConfiguration, (state) -> state.setValue(RotatedPillarBlock.AXIS, direction.getOpposite().getAxis()));



        foliageAttachment.add(new FoliagePlacer.FoliageAttachment(trackPos, 0, false));

        BlockPos bushLower = trackPos;
        bushLower = bushLower.above();
        bushLower = bushLower.relative(direction);
        bushLower = bushLower.relative(direction);
        foliageAttachment.add(new FoliagePlacer.FoliageAttachment(bushLower, 0, false));

        Direction directionOpposite = direction.getOpposite();

        BlockPos bushUpper = trackPos;
        bushUpper = bushUpper.below();
        bushUpper = bushUpper.relative(directionOpposite);
        bushUpper = bushUpper.relative(directionOpposite);
        bushUpper = bushUpper.relative(directionOpposite.getClockWise());
        foliageAttachment.add(new FoliagePlacer.FoliageAttachment(bushUpper, 0, false));

        return foliageAttachment;
    }
}