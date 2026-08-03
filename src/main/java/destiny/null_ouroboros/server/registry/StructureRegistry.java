package destiny.null_ouroboros.server.registry;

import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.server.worldgen.structure.RandomTemplatePiece;
import destiny.null_ouroboros.server.worldgen.structure.RandomTemplateStructure;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class StructureRegistry {
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, NullOuroboros.MODID);
    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, NullOuroboros.MODID);

    public static final RegistryObject<StructureType<RandomTemplateStructure>> RANDOM_TEMPLATE =
            STRUCTURE_TYPES.register("random_template", () -> () -> RandomTemplateStructure.CODEC);

    public static final RegistryObject<StructurePieceType> RANDOM_TEMPLATE_PIECE =
            STRUCTURE_PIECES.register("random_template", () -> (StructurePieceType.StructureTemplateType) RandomTemplatePiece::new);
}
