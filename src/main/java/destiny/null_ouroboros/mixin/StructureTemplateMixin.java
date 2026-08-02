package destiny.null_ouroboros.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import destiny.null_ouroboros.server.fuse.FuseStructureLinks;
import destiny.null_ouroboros.server.structure.StructureMarkerArming;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(StructureTemplate.class)
public class StructureTemplateMixin {
    @ModifyArg(
            method = "fillFromWorld",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplate$StructureBlockInfo;<init>(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/nbt/CompoundTag;)V",
                    ordinal = 0
            ),
            index = 2
    )
    private CompoundTag nullOuroboros$relativeFuseLinksOnSave(
            CompoundTag nbt,
            @Local BlockEntity blockEntity) {
        if (nbt != null && blockEntity != null) {
            FuseStructureLinks.toRelative(nbt, blockEntity.getBlockPos());
        }
        return nbt;
    }

    @WrapOperation(
            method = "placeInWorld",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/entity/BlockEntity;load(Lnet/minecraft/nbt/CompoundTag;)V"
            )
    )
    private void nullOuroboros$resolveFuseLinksAndArmStructureMarker(
            BlockEntity blockEntity,
            CompoundTag tag,
            Operation<Void> original,
            @Local(argsOnly = true) StructurePlaceSettings settings) {
        FuseStructureLinks.toAbsolute(tag, blockEntity.getBlockPos(), settings.getMirror(), settings.getRotation());
        original.call(blockEntity, tag);
        StructureMarkerArming.armIfStructureMarker(blockEntity);
    }
}
