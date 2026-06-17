package destiny.null_ouroboros.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;

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
}
