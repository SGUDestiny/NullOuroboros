package destiny.null_ouroboros.mixin;

import destiny.null_ouroboros.server.structure.StructureMarkerArming;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(StructureTemplate.class)
public class StructureTemplateMixin {
    @Redirect(
            method = "placeInWorld",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/entity/BlockEntity;load(Lnet/minecraft/nbt/CompoundTag;)V"
            )
    )
    private void nullOuroboros$loadAndArmStructureMarker(BlockEntity blockEntity, CompoundTag tag) {
        blockEntity.load(tag);
        StructureMarkerArming.armIfStructureMarker(blockEntity);
    }
}
