package destiny.null_ouroboros.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.client.render.DusterbikeRenderTransforms;
import destiny.null_ouroboros.client.render.RenderTypeRegistry;
import destiny.null_ouroboros.client.render.model.DusterbikeEntityModel;
import destiny.null_ouroboros.common.dusterbike.DusterbikePartType;
import destiny.null_ouroboros.server.entity.DusterbikeEntity;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public class DusterbikeEntityRenderer extends EntityRenderer<DusterbikeEntity> {
    private static final ResourceLocation DUSTERBIKE_TEXTURE = ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/entity/dusterbike.png");
    private static final ResourceLocation DUSTERBIKE_OFF_TEXTURE = ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/entity/dusterbike_off.png");
    private static final ResourceLocation DUSTERBIKE_COLORED_TEXTURE = ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/entity/dusterbike_colored.png");
    private static final ResourceLocation DUSTERBIKE_COLORED_OFF_TEXTURE = ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/entity/dusterbike_colored_off.png");

    private final DusterbikeEntityModel model;

    public DusterbikeEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new DusterbikeEntityModel(context.bakeLayer(DusterbikeEntityModel.LAYER_LOCATION));
        this.shadowRadius = 0.75F;
    }

    @Override
    public ResourceLocation getTextureLocation(DusterbikeEntity entity) {
        return entity.isEngineRunning() ? DUSTERBIKE_TEXTURE : DUSTERBIKE_OFF_TEXTURE;
    }

    @Override
    public void render(DusterbikeEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        DusterbikeRenderTransforms.applyEntityRenderPose(poseStack, entity, partialTicks);
        this.model.setupAnim(entity, 0.0F, 0.0F, entity.tickCount + partialTicks, 0.0F, 0.0F);

        int modelLight = getBikeModelLight(entity, packedLight);
        boolean engineRunning = entity.isEngineRunning();

        ResourceLocation bodyTex = engineRunning ? DUSTERBIKE_TEXTURE : DUSTERBIKE_OFF_TEXTURE;
        VertexConsumer defaultBodyConsumer = buffer.getBuffer(RenderType.entityTranslucent(bodyTex));

        // ---------- No colours? Render everything with the default texture ----------
        if (!hasAnyColor(entity)) {
            this.model.renderToBuffer(poseStack, defaultBodyConsumer, modelLight,
                    OverlayTexture.NO_OVERLAY, 1.0f, 1.0f, 1.0f, 1.0f);
            poseStack.popPose();
            super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
            return;
        }

        // ---------- Some parts are coloured – use the full pipeline ----------
        ResourceLocation coloredBodyTex = engineRunning ? DUSTERBIKE_COLORED_TEXTURE : DUSTERBIKE_COLORED_OFF_TEXTURE;
        VertexConsumer coloredBodyConsumer = buffer.getBuffer(RenderType.entityTranslucent(coloredBodyTex));

        // Emissive off-pass consumers
        VertexConsumer defaultEmissiveOff = buffer.getBuffer(RenderType.entityTranslucent(DUSTERBIKE_OFF_TEXTURE));
        VertexConsumer coloredEmissiveOff = buffer.getBuffer(RenderType.entityTranslucent(DUSTERBIKE_COLORED_OFF_TEXTURE));

        // Emissive on-pass consumers (full bright)
        VertexConsumer defaultEmissiveOn = buffer.getBuffer(RenderTypeRegistry.getEmissiveRenderType(DUSTERBIKE_TEXTURE));
        VertexConsumer coloredEmissiveOn = buffer.getBuffer(RenderTypeRegistry.getEmissiveRenderType(DUSTERBIKE_COLORED_TEXTURE));

        // Main body rendering – per‑part, each with its own consumer
        this.model.renderBody(poseStack, defaultBodyConsumer, coloredBodyConsumer, modelLight,
                OverlayTexture.NO_OVERLAY, entity);

        // Emissive rendering (unchanged)
        this.model.renderEmissive(entity, false, poseStack, defaultEmissiveOff, coloredEmissiveOff,
                modelLight, OverlayTexture.NO_OVERLAY);
        this.model.renderEmissive(entity, true, poseStack, defaultEmissiveOn, coloredEmissiveOn,
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private static boolean hasAnyColor(DusterbikeEntity entity) {
        for (DusterbikePartType type : DusterbikePartType.values()) {
            if (entity.getPartMainColor(type) != null || entity.getPartGlowColor(type) != null) {
                return true;
            }
        }
        return false;
    }

    private static int getBikeModelLight(DusterbikeEntity entity, int packedLight) {
        BlockPos bodyTop = BlockPos.containing(entity.getX(), entity.getBoundingBox().maxY + 0.25D, entity.getZ());
        BlockPos headRoom = bodyTop.above();
        return Math.max(packedLight, Math.max(LevelRenderer.getLightColor(entity.level(), bodyTop), LevelRenderer.getLightColor(entity.level(), headRoom)));
    }
}