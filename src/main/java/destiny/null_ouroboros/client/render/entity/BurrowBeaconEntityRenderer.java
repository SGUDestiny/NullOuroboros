package destiny.null_ouroboros.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.client.render.model.BurrowBeaconEntityModel;
import destiny.null_ouroboros.server.entity.BurrowBeaconEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public class BurrowBeaconEntityRenderer extends LivingEntityRenderer<BurrowBeaconEntity, BurrowBeaconEntityModel> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/entity/burrow_beacon.png");
    private static final ResourceLocation TEXTURE_OFF = ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/entity/burrow_beacon_off.png");

    public BurrowBeaconEntityRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new BurrowBeaconEntityModel(ctx.bakeLayer(BurrowBeaconEntityModel.LAYER_LOCATION)), 0.0f);
    }

    @Override
    public ResourceLocation getTextureLocation(BurrowBeaconEntity entity) {
        return entity.getAnimationState() == BurrowBeaconEntity.State.DRILL_IDLE ? TEXTURE : TEXTURE_OFF;
    }

    @Override
    protected boolean shouldShowName(BurrowBeaconEntity entity) {
        return false;
    }

    @Override
    public boolean shouldRender(BurrowBeaconEntity entity, Frustum frustum, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    protected void setupRotations(BurrowBeaconEntity entity, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTicks) {
    }

    @Override
    public void render(BurrowBeaconEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        boolean wasVisible = this.model.getEmissive().visible;
        if (entity.getAnimationState() == BurrowBeaconEntity.State.DRILL_IDLE) {
            this.model.getEmissive().visible = false;
        }

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);

        this.model.getEmissive().visible = wasVisible;

        if (entity.getAnimationState() == BurrowBeaconEntity.State.DRILL_IDLE) {
            poseStack.pushPose();

            poseStack.translate(0, 2.25, 0);

            RenderType emissiveType = RenderType.entityTranslucentEmissive(TEXTURE);
            VertexConsumer glowConsumer = buffer.getBuffer(emissiveType);
            this.model.renderEmissive(poseStack, glowConsumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);

            poseStack.popPose();
        }
    }
}