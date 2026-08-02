package destiny.null_ouroboros.client.render.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import destiny.null_ouroboros.server.block.entity.TerminusTemplateLoaderBlockEntity;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class TerminusTemplateLoaderRenderer implements BlockEntityRenderer<TerminusTemplateLoaderBlockEntity> {
    public TerminusTemplateLoaderRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(TerminusTemplateLoaderBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (!be.isShowBoundingBox()) {
            return;
        }
        if (be.getPosX() == 0 && be.getPosY() == 0 && be.getPosZ() == 0) {
            return;
        }
        BlockPos target = be.getTargetPos();
        BlockPos origin = be.getBlockPos();
        double dx = target.getX() - origin.getX();
        double dy = target.getY() - origin.getY();
        double dz = target.getZ() - origin.getZ();
        AABB box = new AABB(dx, dy, dz, dx + 1.0D, dy + 1.0D, dz + 1.0D).inflate(0.002D);
        VertexConsumer consumer = buffer.getBuffer(RenderType.lines());
        LevelRenderer.renderLineBox(pose, consumer, box, 1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    public boolean shouldRenderOffScreen(TerminusTemplateLoaderBlockEntity be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    @Override
    public boolean shouldRender(TerminusTemplateLoaderBlockEntity be, Vec3 cameraPos) {
        return true;
    }
}
