package destiny.null_ouroboros.server.structure;

import destiny.null_ouroboros.server.block.entity.AbandonedDusterbikeSpawnerBlockEntity;
import destiny.null_ouroboros.server.block.entity.TerminusTemplateLoaderBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class StructureMarkerArming {
    private StructureMarkerArming() {
    }

    public static void armIfStructureMarker(BlockEntity blockEntity) {
        if (blockEntity instanceof AbandonedDusterbikeSpawnerBlockEntity spawner) {
            spawner.armForStructurePlacement();
        } else if (blockEntity instanceof TerminusTemplateLoaderBlockEntity loader) {
            loader.armForStructurePlacement();
        }
    }
}
