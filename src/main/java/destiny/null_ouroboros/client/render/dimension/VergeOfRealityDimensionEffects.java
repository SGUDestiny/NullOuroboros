package destiny.null_ouroboros.client.render.dimension;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.server.capability.ClientManifoldingHolder;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

public class VergeOfRealityDimensionEffects extends DimensionSpecialEffects {
    public static final ResourceLocation VERGE_OF_REALITY_DIMENSION_EFFECTS = ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "verge_of_reality_effects");
    private static VergeOfRealityDimensionEffects instance;

    private static final float SKY_DISC_HEIGHT = 16f;
    private static final float STAR_DISTANCE = 96f;
    private static final float TWO_PI = (float) (Math.PI * 2);

    private final VertexBuffer upperSkyBuffer;
    private final VertexBuffer lowerSkyBuffer;
    private final VertexBuffer skyCylinderBuffer;

    private final VertexBuffer riftBuffer;
    private Vec3 riftDirection = new Vec3(0.8, 0.7, 1);
    private float riftRotation = 0f;
    private float riftSize = 100f;
    private float riftSizePulse = 0.3f;
    private ResourceLocation riftTexture = ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/environment/origin_rift.png");
    private ResourceLocation riftMiddleTexture = ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/environment/origin_rift_middle.png");

    private final VertexBuffer horizonRingBuffer;
    private static final float RING_RADIUS = 120;
    private static final float RING_LOWER_Y = -18;
    private static final float RING_UPPER_Y = 18;
    private static final float RING_HORIZON_ALPHA_BASE = 0.6F;

    private static final float[] DARK_SKY_HSV = { 0, 1, 0.3f };
    private static final float[] REGULAR_SKY_HSV = { 0, 1, 1 };
    private static final float[] PULSE_SKY_HSV = { 0, 0.75f, 1 };

    public VergeOfRealityDimensionEffects() {
        super(Float.NaN, true, SkyType.NONE, false, false);
        instance = this;

        this.upperSkyBuffer = createSkyDisc(SKY_DISC_HEIGHT);
        this.lowerSkyBuffer = createSkyDisc(-SKY_DISC_HEIGHT);
        this.skyCylinderBuffer = createSkyCylinder(-SKY_DISC_HEIGHT, SKY_DISC_HEIGHT, 100, 128);
        this.riftBuffer = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
        this.horizonRingBuffer = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
    }

    public static VergeOfRealityDimensionEffects getInstance() {
        return instance;
    }

    public static boolean isVergeOfReality(Level level) {
        return level.dimension().location().getPath().contains("verge_of_reality");
    }

    private VertexBuffer createSkyDisc(float yLevel) {
        VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.getBuilder();

        builder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION);
        float radius = 100;
        int segments = 128;

        builder.vertex(0, yLevel, 0).endVertex();
        for (int i = 0; i <= segments; i++) {
            float angle = TWO_PI * i / segments;
            float x = Mth.cos(angle) * radius;
            float z = Mth.sin(angle) * radius;
            builder.vertex(x, yLevel, z).endVertex();
        }

        BufferBuilder.RenderedBuffer rendered = builder.end();
        buffer.bind();
        buffer.upload(rendered);
        VertexBuffer.unbind();

        return buffer;
    }

    private VertexBuffer createSkyCylinder(float bottomY, float topY, float radius, int segments) {
        VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.getBuilder();

        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        for (int i = 0; i < segments; i++) {
            float angle1 = TWO_PI * i / segments;
            float angle2 = TWO_PI * (i + 1) / segments;
            float x1 = Mth.cos(angle1) * radius;
            float z1 = Mth.sin(angle1) * radius;
            float x2 = Mth.cos(angle2) * radius;
            float z2 = Mth.sin(angle2) * radius;

            builder.vertex(x1, bottomY, z1).endVertex();
            builder.vertex(x2, bottomY, z2).endVertex();
            builder.vertex(x2, topY,   z2).endVertex();
            builder.vertex(x1, topY,   z1).endVertex();
        }

        BufferBuilder.RenderedBuffer rendered = builder.end();
        buffer.bind();
        buffer.upload(rendered);
        VertexBuffer.unbind();

        return buffer;
    }

    @Override
    public boolean renderSky(ClientLevel level, int ticks, float partialTick, PoseStack poseStack, Camera camera, Matrix4f projectionMatrix, boolean isFoggy, Runnable setupFog) {
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        RenderSystem.depthFunc(GL11.GL_ALWAYS);

        float pulse = ClientManifoldingHolder.getThunderPulse();
        float lightDim = ClientManifoldingHolder.getLightDim();
        setSkyColor(pulse, lightDim);
        RenderSystem.setShader(GameRenderer::getPositionShader);

        RenderSystem.setShaderFogStart(1000000);
        RenderSystem.setShaderFogEnd(1000000);

        this.upperSkyBuffer.bind();
        this.upperSkyBuffer.drawWithShader(poseStack.last().pose(), projectionMatrix, RenderSystem.getShader());
        this.lowerSkyBuffer.bind();
        this.lowerSkyBuffer.drawWithShader(poseStack.last().pose(), projectionMatrix, RenderSystem.getShader());
        this.skyCylinderBuffer.bind();
        this.skyCylinderBuffer.drawWithShader(poseStack.last().pose(), projectionMatrix, RenderSystem.getShader());
        VertexBuffer.unbind();

        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.setShaderFogStart(0);
        RenderSystem.setShaderFogEnd(1);
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        return true;
    }

    private static float[] computeSkyColor(float pulse, float lightDim) {
        float[] dimHSV = lerpHSV(REGULAR_SKY_HSV, DARK_SKY_HSV, lightDim);
        float[] finalHSV = lerpHSV(dimHSV, PULSE_SKY_HSV, pulse);
        return hsvToRgb(finalHSV);
    }

    private static void setSkyColor(float pulse, float lightDim) {
        float[] rgb = computeSkyColor(pulse, lightDim);
        RenderSystem.setShaderColor(rgb[0], rgb[1], rgb[2], 1);
    }

    private void renderHorizonRing(PoseStack poseStack, Matrix4f projectionMatrix) {
        float lightDim = ClientManifoldingHolder.getLightDim();

        if (lightDim <= 0) return;

        float pulse = ClientManifoldingHolder.getThunderPulse();
        float[] skyRgb = computeSkyColor(pulse, lightDim);
        float sr = skyRgb[0], sg = skyRgb[1], sb = skyRgb[2];

        float ringR = 1, ringG = 0, ringB = 0;
        float horizonAlpha = lightDim * RING_HORIZON_ALPHA_BASE;

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.depthFunc(GL11.GL_ALWAYS);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.getBuilder();
        int segments = 64;
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (int i = 0; i < segments; i++) {
            float fromAngle = TWO_PI * i / segments;
            float toAngle = TWO_PI * (i + 1) / segments;

            Vec3 lowerFrom = new Vec3(Mth.cos(fromAngle) * RING_RADIUS, RING_LOWER_Y, Mth.sin(fromAngle) * RING_RADIUS);
            Vec3 lowerTo   = new Vec3(Mth.cos(toAngle)   * RING_RADIUS, RING_LOWER_Y, Mth.sin(toAngle)   * RING_RADIUS);
            Vec3 horizonFrom = new Vec3(Mth.cos(fromAngle) * RING_RADIUS, 0.0, Mth.sin(fromAngle) * RING_RADIUS);
            Vec3 horizonTo   = new Vec3(Mth.cos(toAngle)   * RING_RADIUS, 0.0, Mth.sin(toAngle)   * RING_RADIUS);
            Vec3 upperFrom = new Vec3(Mth.cos(fromAngle) * RING_RADIUS, RING_UPPER_Y, Mth.sin(fromAngle) * RING_RADIUS);
            Vec3 upperTo   = new Vec3(Mth.cos(toAngle)   * RING_RADIUS, RING_UPPER_Y, Mth.sin(toAngle)   * RING_RADIUS);

            addColorQuad(builder, lowerFrom, sr, sg, sb, 0, lowerTo, sr, sg, sb, 0, horizonTo, ringR, ringG, ringB, horizonAlpha,
                    horizonFrom, ringR, ringG, ringB, horizonAlpha);

            addColorQuad(builder, horizonFrom, ringR, ringG, ringB, horizonAlpha, horizonTo, ringR, ringG, ringB, horizonAlpha,
                    upperTo, sr, sg, sb, 0, upperFrom, sr, sg, sb, 0);
        }

        BufferBuilder.RenderedBuffer rendered = builder.end();
        horizonRingBuffer.bind();
        horizonRingBuffer.upload(rendered);
        horizonRingBuffer.drawWithShader(poseStack.last().pose(), projectionMatrix, RenderSystem.getShader());
        VertexBuffer.unbind();

        RenderSystem.depthFunc(GL11.GL_LEQUAL);
    }

    private void addColorQuad(BufferBuilder builder, Vec3 v0, float r0, float g0, float b0, float a0, Vec3 v1, float r1, float g1, float b1, float a1,
                              Vec3 v2, float r2, float g2, float b2, float a2, Vec3 v3, float r3, float g3, float b3, float a3) {
        builder.vertex(v0.x, v0.y, v0.z).color(r0, g0, b0, a0).endVertex();
        builder.vertex(v1.x, v1.y, v1.z).color(r1, g1, b1, a1).endVertex();
        builder.vertex(v2.x, v2.y, v2.z).color(r2, g2, b2, a2).endVertex();
        builder.vertex(v3.x, v3.y, v3.z).color(r3, g3, b3, a3).endVertex();
    }

    public void renderOverlay(ClientLevel level, float partialTick, PoseStack poseStack, Camera camera, Matrix4f projectionMatrix) {
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorTexShader);

        float lightDim = ClientManifoldingHolder.getLightDim();
        float thunderPulse = ClientManifoldingHolder.getThunderPulse();

        float dimFactor = 1f - lightDim * 0.7f;
        float brightness = dimFactor + (1f - dimFactor) * thunderPulse;

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.getBuilder();

        RenderSystem.setShaderTexture(0, riftTexture);
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        addTexturedSpriteQuad(builder, riftDirection, STAR_DISTANCE, riftSize, riftRotation, brightness, brightness, brightness, 1f);
        BufferBuilder.RenderedBuffer rendered = builder.end();
        riftBuffer.bind();
        riftBuffer.upload(rendered);
        riftBuffer.drawWithShader(poseStack.last().pose(), projectionMatrix, RenderSystem.getShader());

        int rt = ClientManifoldingHolder.getRiftTicks();
        if (rt >= 0 && rt < 20) {
            float progress = rt / 20f;
            float pulseSize = riftSize * (1f + progress * riftSizePulse);
            float alpha = 1f - progress;

            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
            addTexturedSpriteQuad(builder, riftDirection, STAR_DISTANCE, pulseSize, riftRotation, brightness, brightness, brightness, alpha);
            BufferBuilder.RenderedBuffer pulseRendered = builder.end();
            riftBuffer.bind();
            riftBuffer.upload(pulseRendered);
            riftBuffer.drawWithShader(poseStack.last().pose(), projectionMatrix, RenderSystem.getShader());
        }

        RenderSystem.setShaderTexture(0, riftMiddleTexture);

        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        addTexturedSpriteQuad(builder, riftDirection, STAR_DISTANCE, riftSize, riftRotation, 1f, 1f, 1f, 1f);
        BufferBuilder.RenderedBuffer middleRendered = builder.end();
        riftBuffer.bind();
        riftBuffer.upload(middleRendered);
        riftBuffer.drawWithShader(poseStack.last().pose(), projectionMatrix, RenderSystem.getShader());

        if (rt >= 0 && rt < 20) {
            float progress = rt / 20f;
            float pulseSize = riftSize * (1f + progress * riftSizePulse);
            float alpha = 1f - progress;

            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
            addTexturedSpriteQuad(builder, riftDirection, STAR_DISTANCE, pulseSize, riftRotation, 1f, 1f, 1f, alpha);
            BufferBuilder.RenderedBuffer middlePulseRendered = builder.end();
            riftBuffer.bind();
            riftBuffer.upload(middlePulseRendered);
            riftBuffer.drawWithShader(poseStack.last().pose(), projectionMatrix, RenderSystem.getShader());
        }

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        VertexBuffer.unbind();

        renderHorizonRing(poseStack, projectionMatrix);

        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
    }

    private void addTexturedSpriteQuad(BufferBuilder builder, Vec3 direction, float distance, float size, float rotation, float r, float g, float b, float a) {
        Basis basis = getBasis(direction, rotation);
        Vec3 center = direction.scale(distance);
        Vec3 right = basis.right.scale(size);
        Vec3 up = basis.up.scale(size);
        Vec3 topLeft = center.subtract(right).add(up);
        Vec3 bottomLeft = center.subtract(right).subtract(up);
        Vec3 bottomRight = center.add(right).subtract(up);
        Vec3 topRight = center.add(right).add(up);

        builder.vertex((float) topLeft.x, (float) topLeft.y, (float) topLeft.z).color(r, g, b, a).uv(0, 0).endVertex();
        builder.vertex((float) bottomLeft.x, (float) bottomLeft.y, (float) bottomLeft.z).color(r, g, b, a).uv(0, 1).endVertex();
        builder.vertex((float) bottomRight.x, (float) bottomRight.y, (float) bottomRight.z).color(r, g, b, a).uv(1, 1).endVertex();
        builder.vertex((float) topRight.x, (float) topRight.y, (float) topRight.z).color(r, g, b, a).uv(1, 0).endVertex();
    }

    private Basis getBasis(Vec3 direction, float rotation) {
        Vec3 norm = direction.normalize();
        Vec3 ref = Math.abs(norm.y) > 0.98 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 right = ref.cross(norm).normalize();
        Vec3 up = norm.cross(right).normalize();

        if (rotation != 0) {
            double cos = Math.cos(rotation);
            double sin = Math.sin(rotation);
            Vec3 rotRight = right.scale(cos).add(up.scale(sin));
            Vec3 rotUp = up.scale(cos).subtract(right.scale(sin));
            return new Basis(rotRight, rotUp);
        }

        return new Basis(right, up);
    }

    private static float[] lerpHSV(float[] a, float[] b, float t) {
        return new float[] {
                a[0] + (b[0] - a[0]) * t,
                a[1] + (b[1] - a[1]) * t,
                a[2] + (b[2] - a[2]) * t
        };
    }

    private static float[] hsvToRgb(float[] hsv) {
        float h = hsv[0], s = hsv[1], v = hsv[2];
        int hi = (int) (h * 6) % 6;
        float f = h * 6 - (int)(h * 6);
        float p = v * (1 - s);
        float q = v * (1 - f * s);
        float t = v * (1 - (1 - f) * s);
        return switch (hi) {
            case 0 -> new float[]{v, t, p};
            case 1 -> new float[]{q, v, p};
            case 2 -> new float[]{p, v, t};
            case 3 -> new float[]{p, q, v};
            case 4 -> new float[]{t, p, v};
            case 5 -> new float[]{v, p, q};
            default -> new float[]{0,0,0};
        };
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 color, float sunHeight) {
        float lightDim = ClientManifoldingHolder.getLightDim();
        Vec3 darkFog = new Vec3(DARK_SKY_HSV[2], 0.0, 0.0);
        return color.scale(1 - lightDim).add(darkFog.scale(lightDim));
    }

    @Override public boolean isFoggyAt(int x, int z) {
        return false;
    }

    @Override public boolean renderClouds(ClientLevel level, int ticks, float partialTick, PoseStack poseStack, double camX, double camY, double camZ, Matrix4f projectionMatrix) {
        return true;
    }

    @Override public boolean renderSnowAndRain(ClientLevel level, int ticks, float partialTick, LightTexture lightTexture, double camX, double camY, double camZ) {
        return false;
    }

    private record Basis(Vec3 right, Vec3 up) {}
}