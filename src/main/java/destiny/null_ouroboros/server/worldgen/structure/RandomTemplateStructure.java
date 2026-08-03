package destiny.null_ouroboros.server.worldgen.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import destiny.null_ouroboros.server.registry.StructureRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.Optional;

public class RandomTemplateStructure extends Structure {
    public static final Codec<RandomTemplateStructure> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    settingsCodec(instance),
                    ResourceLocation.CODEC.fieldOf("structure").forGetter(structure -> structure.structure),
                    Codec.INT.optionalFieldOf("y_offset", 0).forGetter(structure -> structure.yOffset)
            ).apply(instance, RandomTemplateStructure::new)
    );

    private final ResourceLocation structure;
    private final int yOffset;

    public RandomTemplateStructure(StructureSettings settings, ResourceLocation structure, int yOffset) {
        super(settings);
        this.structure = structure;
        this.yOffset = yOffset;
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        return onTopOfChunkCenter(context, Heightmap.Types.WORLD_SURFACE_WG, builder -> generatePieces(builder, context));
    }

    private void generatePieces(StructurePiecesBuilder builder, GenerationContext context) {
        StructureTemplateManager manager = context.structureTemplateManager();
        StructureTemplate template = manager.get(structure).orElse(null);
        if (template == null) {
            return;
        }

        ChunkPos chunkPos = context.chunkPos();
        int x = chunkPos.getMiddleBlockX();
        int z = chunkPos.getMiddleBlockZ();
        int y = context.chunkGenerator().getFirstOccupiedHeight(
                x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
        Vec3i size = template.getSize();
        BlockPos corner = new BlockPos(x - size.getX() / 2, y + yOffset, z - size.getZ() / 2);
        builder.addPiece(new RandomTemplatePiece(manager, structure, corner));
    }

    @Override
    public StructureType<?> type() {
        return StructureRegistry.RANDOM_TEMPLATE.get();
    }
}
