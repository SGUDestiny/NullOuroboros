package destiny.null_ouroboros.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import destiny.null_ouroboros.client.render.dimension.VergeOfRealityDimensionEffects;
import destiny.null_ouroboros.server.capability.ClientManifoldingHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightTexture.class)
public class LightTextureMixin {
    @Shadow
    @Final
    private DynamicTexture lightTexture;
    @Shadow
    @Final
    private NativeImage lightPixels;

    @Inject(method = "updateLightTexture(F)V", at = @At("TAIL"))
    private void nullOuroboros$vergeLightMap(float partialTick, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;

        if (level == null || !VergeOfRealityDimensionEffects.isVergeOfReality(level)) return;

        float lightDim = ClientManifoldingHolder.getLightDim();
        float surfaceBrightness = 0.5f * (1f - lightDim);

        DimensionType dimensionType = level.dimensionType();

        for (int skyLight = 0; skyLight < 16; skyLight++) {
            float skyContribution = surfaceBrightness * (skyLight / 15f);

            for (int blockLight = 0; blockLight < 16; blockLight++) {
                float blockContribution = LightTexture.getBrightness(dimensionType, blockLight);
                float brightness = Mth.clamp(skyContribution + blockContribution, 0f, 1f);
                int value = (int) (brightness * 255f);

                lightPixels.setPixelRGBA(blockLight, skyLight, 0xFF000000 | (value << 16) | (value << 8) | value);
            }
        }
        lightTexture.upload();
    }
}