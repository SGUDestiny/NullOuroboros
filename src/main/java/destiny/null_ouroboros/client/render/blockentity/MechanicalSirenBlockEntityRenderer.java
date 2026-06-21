package destiny.null_ouroboros.client.render.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.client.render.model.MechanicalSirenBlockModel;
import destiny.null_ouroboros.server.block.entity.MechanicalSirenBlockEntity;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public class MechanicalSirenBlockEntityRenderer implements BlockEntityRenderer<MechanicalSirenBlockEntity> {
    public final MechanicalSirenBlockModel model;

    public MechanicalSirenBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {
        this.model = new MechanicalSirenBlockModel(ctx.getModelSet().bakeLayer(MechanicalSirenBlockModel.LAYER_LOCATION));
    }

    @Override
    public void render(MechanicalSirenBlockEntity sirenEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/block/mechanical_siren.png");

        ModelPart bone = this.model.bb_main;
        float pivotX = bone.x / 16;
        float pivotY = bone.y / 16;
        float pivotZ = bone.z / 16;

        poseStack.pushPose();

        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Direction.UP.getOpposite().getRotation());
        poseStack.translate(0.5, 0.5, -0.5);
        poseStack.translate(-pivotX, -pivotY, -pivotZ);
        poseStack.translate(-0.5, 0, 0.5);

        VertexConsumer mainConsumer = buffer.getBuffer(RenderType.entityTranslucent(texture));
        bone.render(poseStack, mainConsumer, packedLight, packedOverlay);

        poseStack.pushPose();

        ModelPart blades = this.model.blades;
        float angle = sirenEntity.getRotationAngle() + sirenEntity.getRotationSpeed() * partialTick;
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(angle % 360));

        blades.render(poseStack, mainConsumer, packedLight, packedOverlay);

        poseStack.popPose();
        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(MechanicalSirenBlockEntity entity) {
        return true;
    }

    @Override
    public boolean shouldRender(MechanicalSirenBlockEntity entity, Vec3 vec3) {
        return true;
    }
}
