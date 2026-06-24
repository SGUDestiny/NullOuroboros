package destiny.null_ouroboros.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.client.render.RenderTypeRegistry;
import destiny.null_ouroboros.client.render.model.BurrowBeaconEntityModel;
import destiny.null_ouroboros.server.entity.BurrowBeaconEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.List;
import java.util.Set;

public class BurrowBeaconEntityRenderer extends LivingEntityRenderer<BurrowBeaconEntity, BurrowBeaconEntityModel> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/entity/burrow_beacon.png");
    private static final ResourceLocation TEXTURE_OFF = ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/entity/burrow_beacon_off.png");

    private static final float TETHER_THICKNESS = 2f / 16f;
    private static final float TETHER_SAG = -0.75f;
    private static final float PULSE_ACTIVE_DURATION = 1f;
    private static final float PULSE_IDLE_DURATION = 2f;
    private static final float PULSE_CYCLE_TOTAL = PULSE_ACTIVE_DURATION + PULSE_IDLE_DURATION;

    public BurrowBeaconEntityRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new BurrowBeaconEntityModel(ctx.bakeLayer(BurrowBeaconEntityModel.LAYER_LOCATION)), 0f);
    }

    @Override
    public ResourceLocation getTextureLocation(BurrowBeaconEntity entity) {
        return entity.getAnimationState() == BurrowBeaconEntity.State.ACTIVE ? TEXTURE : TEXTURE_OFF;
    }

    @Override
    protected boolean shouldShowName(BurrowBeaconEntity entity) { return false; }

    @Override
    public boolean shouldRender(BurrowBeaconEntity entity, Frustum frustum, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    protected void setupRotations(BurrowBeaconEntity entity, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTicks) {
        float damageState = entity.getDamageState();

        if (damageState > 0f) {
            float timeSinceHit = (float)(entity.level().getGameTime() - entity.lastHit) + partialTicks;

            if (timeSinceHit < 5f) {
                float wobbleAngle = Mth.sin(timeSinceHit / 1.5f * (float)Math.PI) * 3f * damageState;

                poseStack.mulPose(Axis.YP.rotationDegrees(wobbleAngle));
            }
        }
    }

    @Override
    public void render(BurrowBeaconEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        boolean wasVisible = this.model.getEmissive().visible;

        if (entity.getAnimationState() == BurrowBeaconEntity.State.ACTIVE) {
            this.model.getEmissive().visible = false;
        }

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
        this.model.getEmissive().visible = wasVisible;

        if (entity.getAnimationState() == BurrowBeaconEntity.State.ACTIVE) {
            poseStack.pushPose();
            poseStack.translate(0.0, 2.25, 0.0);

            RenderType emissiveType = RenderType.entityTranslucentEmissive(TEXTURE);
            VertexConsumer glowConsumer = buffer.getBuffer(emissiveType);
            this.model.renderEmissive(poseStack, glowConsumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);

            poseStack.popPose();
        }

        if (entity.getAnimationState() != BurrowBeaconEntity.State.ACTIVE) return;

        Set<BlockPos> connections = entity.getConnectedPositions();

        if (!connections.isEmpty()) {
            double time = (entity.level().getGameTime() + partialTicks) / 20.0;
            poseStack.pushPose();
            poseStack.translate(-entity.getX(), -entity.getY(), -entity.getZ());

            for (BlockPos target : connections) {
                List<BurrowBeaconEntity> others = entity.level().getEntitiesOfClass(BurrowBeaconEntity.class, new AABB(target).inflate(0.5));

                for (BurrowBeaconEntity other : others) {
                    if (other.getAnimationState() != BurrowBeaconEntity.State.ACTIVE) continue;

                    if (other != entity && other.isAlive()) {
                        if (entity.getId() < other.getId()) {
                            renderTether(poseStack, buffer, entity, other, time, packedLight);
                        }
                    }
                }
            }
            poseStack.popPose();
        }
    }

    private void renderTether(PoseStack poseStack, MultiBufferSource buffer, BurrowBeaconEntity from, BurrowBeaconEntity to, double time, int packedLight) {
        Vec3 start = from.getEyePosition(0).add(0, 0.4, 0);
        Vec3 end = to.getEyePosition(0).add(0, 0.4, 0);
        Vec3 mid = start.add(end).scale(0.5).add(0, TETHER_SAG, 0);

        int steps = 32;
        Vec3[] curve = new Vec3[steps + 1];
        for (int i = 0; i <= steps; i++) {
            float t = i / (float) steps;
            curve[i] = quadraticBezier(start, mid, end, t);
        }

        float pulseOffset = from.getPulseOffset();
        float cycleTime = (float)((time + pulseOffset * PULSE_CYCLE_TOTAL) % PULSE_CYCLE_TOTAL);
        boolean pulseActive = cycleTime < PULSE_ACTIVE_DURATION;
        float pulseProgress = pulseActive ? cycleTime / PULSE_ACTIVE_DURATION : -1f;
        int pulseIndex = pulseActive ? (int)(pulseProgress * steps) : -1;

        VertexConsumer consumer = buffer.getBuffer(RenderTypeRegistry.TETHER_RENDER_TYPE);

        for (int i = 0; i < steps; i++) {
            int r = 255, g = 0, b = 0, a = 255;

            if (i == pulseIndex) {
                r = 255; g = 128; b = 128; a = 255;
            }

            renderThickSegment(poseStack, consumer, curve[i], curve[i + 1], packedLight, r, g, b, a);
        }
    }

    private void renderThickSegment(PoseStack poseStack, VertexConsumer consumer, Vec3 p0, Vec3 p1, int packedLight, int r, int g, int b, int a) {
        Camera cam = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 camPos = cam.getPosition();
        Vec3 segCenter = p0.add(p1).scale(0.5);
        Vec3 toCamera = camPos.subtract(segCenter).normalize();
        Vec3 segDir = p1.subtract(p0);
        Vec3 perp = toCamera.cross(segDir).normalize().scale(TETHER_THICKNESS * 0.5);

        Vec3 v0 = p0.add(perp);
        Vec3 v1 = p0.subtract(perp);
        Vec3 v2 = p1.subtract(perp);
        Vec3 v3 = p1.add(perp);

        PoseStack.Pose last = poseStack.last();
        Matrix4f mat = last.pose();
        consumer.vertex(mat, (float) v0.x, (float) v0.y, (float) v0.z).color(r, g, b, a).endVertex();
        consumer.vertex(mat, (float) v1.x, (float) v1.y, (float) v1.z).color(r, g, b, a).endVertex();
        consumer.vertex(mat, (float) v2.x, (float) v2.y, (float) v2.z).color(r, g, b, a).endVertex();
        consumer.vertex(mat, (float) v3.x, (float) v3.y, (float) v3.z).color(r, g, b, a).endVertex();
    }

    private Vec3 quadraticBezier(Vec3 p0, Vec3 p1, Vec3 p2, float t) {
        float inv = 1 - t;
        return p0.scale(inv * inv).add(p1.scale(2 * inv * t)).add(p2.scale(t * t));
    }
}