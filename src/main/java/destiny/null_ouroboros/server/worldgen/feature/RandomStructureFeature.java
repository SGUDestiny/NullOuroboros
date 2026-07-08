package destiny.null_ouroboros.server.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public class RandomStructureFeature extends Feature<RandomStructureFeatureConfiguration> {
    public RandomStructureFeature() {
        super(RandomStructureFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<RandomStructureFeatureConfiguration> context) {
        RandomStructureFeatureConfiguration config = context.config();
        ResourceLocation structureId = config.structure();

        BlockPos origin = context.origin();
        WorldGenLevel level = context.level();
        ServerLevel serverLevel = level.getLevel();
        StructureTemplateManager manager = serverLevel.getStructureManager();
        StructureTemplate template = manager.get(structureId).orElse(null);

        if (template == null) {
            return false;
        }

        int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, origin.getX(), origin.getZ());
        BlockPos placementPos = new BlockPos(origin.getX(), surfaceY, origin.getZ());
        BlockPos offset = new BlockPos(-template.getSize().getX() / 2, 0, -template.getSize().getZ() / 2);
        BlockPos corner = placementPos.offset(offset);

        template.placeInWorld(serverLevel, corner, corner, new StructurePlaceSettings().setIgnoreEntities(false), serverLevel.getRandom(), Block.UPDATE_CLIENTS);
        return true;
    }
}
