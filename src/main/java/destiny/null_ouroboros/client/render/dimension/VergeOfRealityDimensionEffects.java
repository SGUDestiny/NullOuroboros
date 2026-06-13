package destiny.null_ouroboros.client.render.dimension;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import destiny.null_ouroboros.NullOuroboros;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

public class VergeOfRealityDimensionEffects extends DimensionSpecialEffects {
    public static final ResourceLocation VERGE_OF_REALITY_DIMENSION_EFFECTS = ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "verge_of_reality_effects");
    private static VergeOfRealityDimensionEffects instance;

    private static final float SKY_DISC_HEIGHT = 16.0F;
    private static final float STAR_DISTANCE = 96.0F;
    private static final float TWO_PI = (float) (Math.PI * 2.0);

    private final VertexBuffer upperSkyBuffer;
    private final VertexBuffer lowerSkyBuffer;
    private final VertexBuffer skyCylinderBuffer;

    private final VertexBuffer riftBuffer;
    private Vec3 riftDirection = new Vec3(0.8, 0.7, 1);
    private float riftRotation = 0.0F;
    private float riftSize = 100.0F;
    private ResourceLocation riftTexture = ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/environment/origin_rift.png");

    public VergeOfRealityDimensionEffects() {
        super(Float.NaN, true, SkyType.NONE, false, false);
        instance = this;

        this.upperSkyBuffer = createSkyDisc(SKY_DISC_HEIGHT);
        this.lowerSkyBuffer = createSkyDisc(-SKY_DISC_HEIGHT);
        this.skyCylinderBuffer = createSkyCylinder(-SKY_DISC_HEIGHT, SKY_DISC_HEIGHT, 100.0F, 128);
        this.riftBuffer = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
    }

    public static VergeOfRealityDimensionEffects getInstance() { return instance; }
    public static boolean isVergeOfReality(ClientLevel level) {
        return level.dimension().location().getPath().contains("verge_of_reality");
    }

    private VertexBuffer createSkyDisc(float yLevel) {
        VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.getBuilder();

        builder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION);
        float radius = 100.0F;
        int segments = 128;

        builder.vertex(0.0F, yLevel, 0.0F).endVertex();
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

        RenderSystem.setShader(GameRenderer::getPositionShader);
        RenderSystem.setShaderColor(1.0F, 0.0F, 0.0F, 1.0F);

        RenderSystem.setShaderFogStart(1000000.0F);
        RenderSystem.setShaderFogEnd(1000000.0F);

        this.upperSkyBuffer.bind();
        this.upperSkyBuffer.drawWithShader(poseStack.last().pose(), projectionMatrix, RenderSystem.getShader());
        this.lowerSkyBuffer.bind();
        this.lowerSkyBuffer.drawWithShader(poseStack.last().pose(), projectionMatrix, RenderSystem.getShader());
        this.skyCylinderBuffer.bind();
        this.skyCylinderBuffer.drawWithShader(poseStack.last().pose(), projectionMatrix, RenderSystem.getShader());
        VertexBuffer.unbind();

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderFogStart(0.0F);
        RenderSystem.setShaderFogEnd(1.0F);
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        return true;
    }

    public void renderOverlay(ClientLevel level, float partialTick, PoseStack poseStack, Camera camera, Matrix4f projectionMatrix) {
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
        RenderSystem.setShaderTexture(0, riftTexture);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        addTexturedSpriteQuad(builder, riftDirection, STAR_DISTANCE, riftSize, riftRotation, 1.0F, 1.0F, 1.0F, 1.0F);

        BufferBuilder.RenderedBuffer rendered = builder.end();
        riftBuffer.bind();
        riftBuffer.upload(rendered);
        riftBuffer.drawWithShader(poseStack.last().pose(), projectionMatrix, RenderSystem.getShader());
        VertexBuffer.unbind();

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

    @Override public Vec3 getBrightnessDependentFogColor(Vec3 color, float sunHeight) {
        return color;
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