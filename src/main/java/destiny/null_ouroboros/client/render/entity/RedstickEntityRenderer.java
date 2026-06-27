package destiny.null_ouroboros.client.render.entity;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.client.render.model.RedstickEntityModel;
import destiny.null_ouroboros.server.entity.RedstickEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public class RedstickEntityRenderer extends EntityRenderer<RedstickEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/entity/redstick.png");
    private static final ResourceLocation OFF_TEXTURE = ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/entity/redstick_off.png");
    private static final Vector3f MODEL_AXIS = new Vector3f(0.0F, -1.0F, 0.0F);

    private final RedstickEntityModel model;

    public RedstickEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new RedstickEntityModel(context.bakeLayer(RedstickEntityModel.LAYER_LOCATION));
    }

    @Override
    public ResourceLocation getTextureLocation(RedstickEntity entity) {
        return entity.isBurnedOut() ? OFF_TEXTURE : TEXTURE;
    }

    @Override
    public void render(RedstickEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        Vec3 axis = entity.getRenderAxis(partialTicks);

        poseStack.pushPose();
        applyStickRotation(poseStack, axis);

        this.model.setupAnim(entity, 0.0F, 0.0F, entity.tickCount + partialTicks, 0.0F, 0.0F);

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(entity)));
        this.model.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);

        float glowBrightness = entity.getGlowBrightness(partialTicks);
        if (glowBrightness > 0.0F) {
            VertexConsumer glowConsumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE));
            this.model.renderEmissive(poseStack, glowConsumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, glowBrightness);
        }

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    static void applyStickRotation(PoseStack poseStack, Vec3 axis) {
        Vector3f target = new Vector3f((float) axis.x, (float) axis.y, (float) axis.z);
        if (target.lengthSquared() < 1.0E-6F) return;

        target.normalize();
        poseStack.mulPose(new Quaternionf().rotationTo(MODEL_AXIS, target));
    }
}
