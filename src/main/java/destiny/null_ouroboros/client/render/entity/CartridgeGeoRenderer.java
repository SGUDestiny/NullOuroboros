package destiny.null_ouroboros.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.client.render.RenderTypeRegistry;
import destiny.null_ouroboros.client.render.model.RevolverCartridgeGeoModel;
import destiny.null_ouroboros.common.revolver.RevolverCartridge;
import destiny.null_ouroboros.server.entity.CartridgeEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.RenderUtils;

public class CartridgeGeoRenderer extends GeoEntityRenderer<CartridgeEntity> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/item/heavy_revolver.png");
    private static final ResourceLocation EMPTY_CASING_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/item/heavy_revolver_empty_casing.png");

    public CartridgeGeoRenderer(EntityRendererProvider.Context context) {
        super(context, new RevolverCartridgeGeoModel());
        shadowRadius = 0.08F;
    }

    @Override
    public void actuallyRender(PoseStack poseStack, CartridgeEntity animatable, BakedGeoModel model,
                               RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                               boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        poseStack.mulPose(Axis.YP.rotationDegrees(-Mth.lerp(partialTick, animatable.yRotO, animatable.getYRot())));
        poseStack.mulPose(Axis.XP.rotationDegrees(animatable.getRoll(partialTick)));
        model.getBone("bullet").ifPresent(bone -> bone.setHidden(!animatable.getCartridge().isLive()));
        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }

    @Override
    public void renderRecursively(PoseStack poseStack, CartridgeEntity animatable, GeoBone bone,
                                  RenderType renderType, MultiBufferSource bufferSource,
                                  VertexConsumer buffer, boolean isReRender, float partialTick,
                                  int packedLight, int packedOverlay,
                                  float red, float green, float blue, float alpha) {
        RevolverCartridge cartridge = animatable.getCartridge();
        String name = bone.getName();
        String lower = name.toLowerCase();
        boolean emissiveBone = lower.contains("emissive") || lower.startsWith("bullet");
        boolean casing = cartridge == RevolverCartridge.CASING;
        ResourceLocation texture = casing ? EMPTY_CASING_TEXTURE : TEXTURE;

        RenderType boneRenderType;
        int light = packedLight;
        int overlay = packedOverlay;
        if (emissiveBone && cartridge.isLive()) {
            boneRenderType = RenderTypeRegistry.getOpaqueEmissiveRenderType(texture);
            light = LightTexture.FULL_BRIGHT;
            overlay = OverlayTexture.NO_OVERLAY;
        } else {
            boneRenderType = RenderType.entityCutoutNoCull(texture);
        }
        VertexConsumer boneBuffer = bufferSource.getBuffer(boneRenderType);

        poseStack.pushPose();
        RenderUtils.translateMatrixToBone(poseStack, bone);
        RenderUtils.translateToPivotPoint(poseStack, bone);
        RenderUtils.rotateMatrixAroundBone(poseStack, bone);
        RenderUtils.scaleMatrixForBone(poseStack, bone);
        RenderUtils.translateAwayFromPivotPoint(poseStack, bone);

        if (!bone.isHidden()) {
            super.renderCubesOfBone(poseStack, bone, boneBuffer, light, overlay, red, green, blue, alpha);
        }
        for (GeoBone child : bone.getChildBones()) {
            this.renderRecursively(poseStack, animatable, child, boneRenderType, bufferSource,
                    boneBuffer, isReRender, partialTick, packedLight, packedOverlay,
                    red, green, blue, alpha);
        }
        poseStack.popPose();
    }
}
