package destiny.null_ouroboros.client.render.entity.steel_leviathan;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import destiny.null_ouroboros.client.render.model.steel_leviathan.BurrowMissileGeoModel;
import destiny.null_ouroboros.common.steel_leviathan.SteelLeviathanBones;
import destiny.null_ouroboros.server.entity.steel_leviathan.BurrowMissileEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class BurrowMissileGeoRenderer extends GeoEntityRenderer<BurrowMissileEntity> {
    public BurrowMissileGeoRenderer(EntityRendererProvider.Context context) {
        super(context, new BurrowMissileGeoModel());
        this.shadowRadius = 0.25F;
    }

    @Override
    public boolean shouldRender(BurrowMissileEntity entity, Frustum frustum, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public void actuallyRender(PoseStack poseStack, BurrowMissileEntity animatable, BakedGeoModel model,
                               RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                               boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        float yaw = Mth.lerp(partialTick, animatable.yRotO, animatable.getYRot());
        float pitch = Mth.lerp(partialTick, animatable.xRotO, animatable.getXRot());
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        float drillAngle = animatable.getDrillSpinAngle();
        for (GeoBone top : model.topLevelBones()) {
            applyDrillSpin(top, drillAngle);
        }
        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }

    private void applyDrillSpin(GeoBone bone, float drillAngle) {
        if (SteelLeviathanBones.isDrillBone(bone.getName())) {
            bone.setRotZ(drillAngle);
        }
        for (GeoBone child : bone.getChildBones()) {
            applyDrillSpin(child, drillAngle);
        }
    }
}

