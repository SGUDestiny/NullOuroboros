package destiny.null_ouroboros.client.render.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import destiny.null_ouroboros.client.render.model.CodelockSafeGeoModel;
import destiny.null_ouroboros.server.block.entity.CodelockSafeBlockEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.util.RenderUtils;

public class CodelockSafeGeoBlockEntityRenderer extends GeoBlockRenderer<CodelockSafeBlockEntity> {
    public CodelockSafeGeoBlockEntityRenderer() {
        super(new CodelockSafeGeoModel());
    }

    @Override
    public void actuallyRender(PoseStack poseStack, CodelockSafeBlockEntity animatable, BakedGeoModel model,
                               RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                               boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        float degrees = animatable.getWheelDegrees();
        model.getBone("Wheel").ifPresent(bone -> bone.setRotZ((float) Math.toRadians(degrees)));

        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }

    @Override
    public void renderRecursively(PoseStack poseStack, CodelockSafeBlockEntity animatable, GeoBone bone,
                                  RenderType renderType, MultiBufferSource bufferSource,
                                  VertexConsumer buffer, boolean isReRender, float partialTick,
                                  int packedLight, int packedOverlay,
                                  float red, float green, float blue, float alpha) {
        if (bone.isHidden()) {
            return;
        }

        ResourceLocation mainTexture = getTextureLocation(animatable);
        RenderType boneRenderType = RenderType.entityCutout(mainTexture);
        int light = packedLight;
        int overlay = packedOverlay;

        String name = bone.getName();
        if (isEmissiveBone(name)) {
            if (shouldRenderEmissive(animatable, name)) {
                boneRenderType = RenderType.entityTranslucentEmissive(CodelockSafeGeoModel.onTexture(animatable));
                light = LightTexture.FULL_BRIGHT;
                overlay = OverlayTexture.NO_OVERLAY;
            } else {
                boneRenderType = RenderType.entityCutout(mainTexture);
            }
        }

        VertexConsumer boneBuffer = bufferSource.getBuffer(boneRenderType);

        poseStack.pushPose();
        RenderUtils.translateMatrixToBone(poseStack, bone);
        RenderUtils.translateToPivotPoint(poseStack, bone);
        RenderUtils.rotateMatrixAroundBone(poseStack, bone);
        RenderUtils.scaleMatrixForBone(poseStack, bone);
        RenderUtils.translateAwayFromPivotPoint(poseStack, bone);

        super.renderCubesOfBone(poseStack, bone, boneBuffer, light, overlay, red, green, blue, alpha);

        RenderType childType = RenderType.entityCutout(mainTexture);
        VertexConsumer childBuffer = bufferSource.getBuffer(childType);
        for (GeoBone child : bone.getChildBones()) {
            this.renderRecursively(poseStack, animatable, child, childType, bufferSource,
                    childBuffer, isReRender, partialTick, packedLight, packedOverlay,
                    red, green, blue, alpha);
        }
        poseStack.popPose();
    }

    private static boolean shouldRenderEmissive(CodelockSafeBlockEntity animatable, String name) {
        if (name.equals("Emissive")) {
            return animatable.canSpinBothWays()
                    || animatable.getScreenStatus() == CodelockSafeBlockEntity.STATUS_CORR
                    || animatable.getScreenStatus() == CodelockSafeBlockEntity.STATUS_SET;
        }
        Integer key = keyForEmissiveBone(name);
        return key != null && animatable.getLitKey() == key;
    }

    private static boolean isEmissiveBone(String name) {
        return name.equals("Emissive") || name.startsWith("Emissive");
    }

    private static Integer keyForEmissiveBone(String name) {
        return switch (name) {
            case "Emissive2" -> CodelockSafeBlockEntity.KEY_1;
            case "Emissive3" -> CodelockSafeBlockEntity.KEY_2;
            case "Emissive4" -> CodelockSafeBlockEntity.KEY_3;
            case "Emissive5" -> CodelockSafeBlockEntity.KEY_4;
            case "Emissive6" -> CodelockSafeBlockEntity.KEY_5;
            case "Emissive7" -> CodelockSafeBlockEntity.KEY_6;
            case "Emissive8" -> CodelockSafeBlockEntity.KEY_7;
            case "Emissive9" -> CodelockSafeBlockEntity.KEY_8;
            case "Emissive10" -> CodelockSafeBlockEntity.KEY_9;
            case "Emissive12" -> CodelockSafeBlockEntity.KEY_X;
            case "Emissive11" -> CodelockSafeBlockEntity.KEY_0;
            case "Emissive13" -> CodelockSafeBlockEntity.KEY_CONFIRM;
            default -> null;
        };
    }
}
