package destiny.null_ouroboros.client.render.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.client.render.model.ElectromagneticAssemblyBlockModel;
import destiny.null_ouroboros.server.block.ElectromagneticAssemblyBlock;
import destiny.null_ouroboros.server.block.entity.ElectromagneticAssemblyBlockEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public class ElectromagneticAssemblyBlockEntityRenderer implements BlockEntityRenderer<ElectromagneticAssemblyBlockEntity> {
    private static final ResourceLocation TEXTURE_ON = ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/block/electromagnetic_assembly.png");
    private static final ResourceLocation TEXTURE_OFF = ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/block/electromagnetic_assembly_off.png");

    private final ElectromagneticAssemblyBlockModel model;

    public ElectromagneticAssemblyBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {
        this.model = new ElectromagneticAssemblyBlockModel(ctx.bakeLayer(ElectromagneticAssemblyBlockModel.LAYER_LOCATION));
    }

    @Override
    public void render(ElectromagneticAssemblyBlockEntity be, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        boolean powered = be.getBlockState().getValue(ElectromagneticAssemblyBlock.POWERED);
        ResourceLocation texture = powered ? TEXTURE_ON : TEXTURE_OFF;

        Direction facing = be.getBlockState().getValue(ElectromagneticAssemblyBlock.HORIZONTAL_FACING);
        float facingYRot = ElectromagneticAssemblyBlockEntity.getFacingYRot(facing);

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(facingYRot));
        poseStack.translate(0, 1, 0);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180));

        VertexConsumer mainConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(texture));
        model.renderStaticBody(poseStack, mainConsumer, packedLight, packedOverlay);

        poseStack.pushPose();
        model.bone.translateAndRotate(poseStack);

        float compassRot = ElectromagneticAssemblyBlockEntity.getCompassCounterRotation(facing);
        model.compass.yRot = compassRot * (float) Math.PI / 180f;
        model.compass.render(poseStack, mainConsumer, packedLight, packedOverlay);

        model.vane.yRot = be.getVaneRenderAngle() * (float) Math.PI / 180f;
        model.vane.render(poseStack, mainConsumer, packedLight, packedOverlay);

        model.spinner.yRot = be.getSpinnerAngleForRender(partialTick) * (float) Math.PI / 180f;
        model.spinner.render(poseStack, mainConsumer, packedLight, packedOverlay);

        if (powered) {
            VertexConsumer glow = bufferSource.getBuffer(RenderType.entityTranslucentEmissive(texture));
            model.emissive1.render(poseStack, glow, LightTexture.FULL_BRIGHT, packedOverlay);
            model.emissive.render(poseStack, glow, LightTexture.FULL_BRIGHT, packedOverlay);
            model.emissive2.render(poseStack, glow, LightTexture.FULL_BRIGHT, packedOverlay);
        } else {
            model.emissive1.render(poseStack, mainConsumer, packedLight, packedOverlay);
            model.emissive.render(poseStack, mainConsumer, packedLight, packedOverlay);
            model.emissive2.render(poseStack, mainConsumer, packedLight, packedOverlay);
        }
        poseStack.popPose();

        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(ElectromagneticAssemblyBlockEntity entity) {
        return true;
    }

    @Override
    public boolean shouldRender(ElectromagneticAssemblyBlockEntity entity, Vec3 vec3) {
        return true;
    }
}
