package destiny.null_ouroboros.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.client.render.RenderTypeRegistry;
import destiny.null_ouroboros.server.entity.BulletEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class BulletEntityRenderer extends EntityRenderer<BulletEntity> {
    private static final ResourceLocation BULLET_TEXTURE = ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/particle/blood.png");
    private static final float SIZE = 0.15F;
    private static final float TRAIL_HALF_WIDTH = 0.06F;

    public BulletEntityRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(BulletEntity bullet, float entityYaw, float partialTicks, PoseStack pose, MultiBufferSource buffer, int packedLight) {
        VertexConsumer sprite = buffer.getBuffer(RenderType.entityTranslucentEmissive(BULLET_TEXTURE));

        pose.pushPose();
        pose.mulPose(this.entityRenderDispatcher.cameraOrientation());
        pose.mulPose(Axis.YP.rotationDegrees(180.0F));
        drawQuad(pose, sprite, SIZE, 1.0F, packedLight);
        pose.popPose();

        if (bullet.tickCount < 2) return;

        Vec3[] trail = bullet.getTrailPositions(partialTicks);
        if (trail.length < 2) return;

        Vec3 entityPos = new Vec3(
                Mth.lerp(partialTicks, bullet.xo, bullet.getX()),
                Mth.lerp(partialTicks, bullet.yo, bullet.getY()),
                Mth.lerp(partialTicks, bullet.zo, bullet.getZ())
        );
        Vec3 camLocal = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().subtract(entityPos);

        int pointCount = trail.length + 1;
        Vec3[] points = new Vec3[pointCount];
        points[0] = Vec3.ZERO;
        System.arraycopy(trail, 0, points, 1, trail.length);

        VertexConsumer ribbon = buffer.getBuffer(RenderTypeRegistry.SCAN_BEAM_RENDER_TYPE);
        Matrix4f matrix = pose.last().pose();

        for (int i = 0; i < pointCount - 1; i++) {
            Vec3 p0 = points[i];
            Vec3 p1 = points[i + 1];
            Vec3 segDir = p1.subtract(p0);
            if (segDir.lengthSqr() < 1.0E-10D) continue;

            float t0 = i / (float) (pointCount - 1);
            float t1 = (i + 1) / (float) (pointCount - 1);
            float fade0 = 1.0F - t0;
            float fade1 = 1.0F - t1;
            float half0 = TRAIL_HALF_WIDTH * fade0;
            float half1 = TRAIL_HALF_WIDTH * fade1;

            Vec3 mid = p0.add(p1).scale(0.5D);
            Vec3 toCamera = camLocal.subtract(mid);
            Vec3 perp = toCamera.cross(segDir);
            if (perp.lengthSqr() < 1.0E-10D) {
                perp = segDir.cross(new Vec3(0.0D, 1.0D, 0.0D));
                if (perp.lengthSqr() < 1.0E-10D) {
                    perp = segDir.cross(new Vec3(1.0D, 0.0D, 0.0D));
                }
            }
            perp = perp.normalize();

            Vec3 o0 = perp.scale(half0);
            Vec3 o1 = perp.scale(half1);

            int a0 = Mth.floor(fade0 * fade0 * 220.0F);
            int a1 = Mth.floor(fade1 * fade1 * 220.0F);

            vertex(ribbon, matrix, p0.add(o0), a0);
            vertex(ribbon, matrix, p0.subtract(o0), a0);
            vertex(ribbon, matrix, p1.subtract(o1), a1);
            vertex(ribbon, matrix, p1.add(o1), a1);
        }
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Vec3 pos, int alpha) {
        consumer.vertex(matrix, (float) pos.x, (float) pos.y, (float) pos.z)
                .color(220, 30, 30, alpha)
                .endVertex();
    }

    private static void drawQuad(PoseStack pose, VertexConsumer builder, float size, float alpha, int packedLight) {
        PoseStack.Pose entry = pose.last();
        Matrix4f matrix = entry.pose();
        Matrix3f normal = entry.normal();
        int a = Mth.floor(alpha * 255.0F);

        builder.vertex(matrix, -size, -size, 0.0F).color(255, 255, 255, a)
                .uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                .normal(normal, 0, 1, 0).endVertex();
        builder.vertex(matrix, -size, size, 0.0F).color(255, 255, 255, a)
                .uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                .normal(normal, 0, 1, 0).endVertex();
        builder.vertex(matrix, size, size, 0.0F).color(255, 255, 255, a)
                .uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                .normal(normal, 0, 1, 0).endVertex();
        builder.vertex(matrix, size, -size, 0.0F).color(255, 255, 255, a)
                .uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                .normal(normal, 0, 1, 0).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(BulletEntity entity) {
        return BULLET_TEXTURE;
    }

    @Override
    public boolean shouldRender(BulletEntity p_114491_, Frustum p_114492_, double p_114493_, double p_114494_, double p_114495_) {
        return true;
    }
}
