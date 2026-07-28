package destiny.null_ouroboros.client.render.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import destiny.null_ouroboros.server.camouflage.Camouflageable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;

public class CamouflageBlockEntityRenderer<T extends BlockEntity & Camouflageable> implements BlockEntityRenderer<T> {
    public CamouflageBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(T blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        BlockState camouflage = blockEntity.getCamouflage();
        if (camouflage == null || blockEntity.getLevel() == null) {
            return;
        }
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                camouflage,
                poseStack,
                buffer,
                packedLight,
                packedOverlay,
                ModelData.EMPTY,
                null
        );
    }
}
