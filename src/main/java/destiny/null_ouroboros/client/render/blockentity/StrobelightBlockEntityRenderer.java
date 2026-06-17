package destiny.null_ouroboros.client.render.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.client.render.model.StrobelightBlockModel;
import destiny.null_ouroboros.server.block.StrobelightBlock;
import destiny.null_ouroboros.server.block.entity.StrobelightBlockEntity;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class StrobelightBlockEntityRenderer implements BlockEntityRenderer<StrobelightBlockEntity> {
    private final StrobelightBlockModel model;

    public StrobelightBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {
        this.model = new StrobelightBlockModel(ctx.getModelSet().bakeLayer(StrobelightBlockModel.LAYER_LOCATION));
    }

    public void render(StrobelightBlockEntity strobelightBlockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        BlockState state = strobelightBlockEntity.getBlockState();
        boolean isOn = state.getValue(StrobelightBlock.LIT);
        Direction facing = state.getValue(StrobelightBlock.FACING);

        ResourceLocation texture = isOn ? ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/block/strobelight.png")
                : ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/block/strobelight_off.png");

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityTranslucent(texture));

        ModelPart bone = this.model.bone;
        float pivotX = bone.x / 16.0f;
        float pivotY = bone.y / 16.0f;
        float pivotZ = bone.z / 16.0f;

        poseStack.pushPose();

        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(facing.getOpposite().getRotation());
        poseStack.translate(0.5, 0.5, -0.5);
        poseStack.translate(-pivotX, -pivotY, -pivotZ);

        float angle = strobelightBlockEntity.getRotationAngle() + strobelightBlockEntity.getRotationSpeed() * partialTick;
        this.model.siren.yRot = (float) Math.toRadians(angle % 360f);

        bone.render(poseStack, vertexConsumer, packedLight, packedOverlay);

        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(StrobelightBlockEntity entity) {
        return true;
    }

    @Override
    public boolean shouldRender(StrobelightBlockEntity entity, Vec3 vec3) {
        return true;
    }
}
