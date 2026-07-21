package destiny.null_ouroboros.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public class RenderTypeRegistry extends RenderType {
    public RenderTypeRegistry(String pName, VertexFormat pFormat, VertexFormat.Mode pMode, int pBufferSize, boolean pAffectsCrumbling, boolean pSortOnUpload, Runnable pSetupState, Runnable pClearState) {
        super(pName, pFormat, pMode, pBufferSize, pAffectsCrumbling, pSortOnUpload, pSetupState, pClearState);
    }

    public static final RenderType BEAM_RENDER_TYPE = RenderType.create(
            "strobelight_beam", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 256, true, true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderType.RENDERTYPE_LIGHTNING_SHADER)
                    .setTransparencyState(RenderType.TRANSLUCENT_TRANSPARENCY)
                    .setCullState(RenderType.NO_CULL)
                    .setLightmapState(RenderType.NO_LIGHTMAP)
                    .setOverlayState(RenderType.NO_OVERLAY)
                    .setLayeringState(RenderType.VIEW_OFFSET_Z_LAYERING)
                    .setOutputState(RenderType.ITEM_ENTITY_TARGET)
                    .createCompositeState(false)
    );

    public static final RenderType SCAN_BEAM_RENDER_TYPE = RenderType.create(
            "steel_leviathan_scan_beam", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 256, true, true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderType.RENDERTYPE_LIGHTNING_SHADER)
                    .setTransparencyState(RenderType.TRANSLUCENT_TRANSPARENCY)
                    .setCullState(RenderType.NO_CULL)
                    .setLightmapState(RenderType.NO_LIGHTMAP)
                    .setOverlayState(RenderType.NO_OVERLAY)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setDepthTestState(RenderType.LEQUAL_DEPTH_TEST)
                    .setLayeringState(RenderType.VIEW_OFFSET_Z_LAYERING)
                    .setOutputState(RenderType.ITEM_ENTITY_TARGET)
                    .createCompositeState(false)
    );

    public static final RenderType TETHER_RENDER_TYPE = RenderType.create("tether", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS,
            256, false, true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE)
                    .setTransparencyState(RenderStateShard.ADDITIVE_TRANSPARENCY)
                    .setLightmapState(RenderStateShard.NO_LIGHTMAP)
                    .setOverlayState(RenderStateShard.NO_OVERLAY)
                    .createCompositeState(false)
    );

    public static RenderType getEmissiveRenderType(ResourceLocation texture) {
        return RenderType.create("liquidator_emissive", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, false, true,
                RenderType.CompositeState.builder()
                        .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                        .setShaderState(RenderStateShard.RENDERTYPE_ARMOR_CUTOUT_NO_CULL_SHADER)
                        .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                        .setLightmapState(RenderStateShard.LIGHTMAP)
                        .setOverlayState(RenderStateShard.OVERLAY)
                        .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                        .setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE)
                        .setCullState(RenderStateShard.NO_CULL)
                        .createCompositeState(false)
        );
    }

    public static RenderType getOpaqueEmissiveRenderType(ResourceLocation texture) {
        return RenderType.create("opaque_emissive", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true, false,
                RenderType.CompositeState.builder()
                        .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                        .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_CUTOUT_NO_CULL_SHADER)
                        .setTransparencyState(RenderStateShard.NO_TRANSPARENCY)
                        .setCullState(RenderStateShard.NO_CULL)
                        .setLightmapState(RenderStateShard.LIGHTMAP)
                        .setOverlayState(RenderStateShard.OVERLAY)
                        .setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE)
                        .createCompositeState(true)
        );
    }
}