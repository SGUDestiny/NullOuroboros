package destiny.null_ouroboros.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import destiny.null_ouroboros.common.dimension.VergeOfRealityDimension;
import destiny.null_ouroboros.server.capability.ClientManifoldingHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LightTexture.class, priority = -200)
public class LightTextureMixin {
    @Shadow
    @Final
    private DynamicTexture lightTexture;
    @Shadow
    @Final
    private NativeImage lightPixels;
    @Shadow
    private boolean updateLightTexture;

    @Inject(method = "updateLightTexture(F)V", at = @At("HEAD"), cancellable = true)
    private void nullOuroboros$vergeLightMap(float partialTick, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;

        if (level == null || !VergeOfRealityDimension.isVergeOfReality(level)) return;

        float lightDim = ClientManifoldingHolder.getLightDim();
        float surfaceBrightness = 0.5f * (1f - lightDim);
        float ambientLight = level.dimensionType().ambientLight();

        for (int skyLight = 0; skyLight < 16; skyLight++) {
            float skyContribution = surfaceBrightness * (skyLight / 15f);

            for (int blockLight = 0; blockLight < 16; blockLight++) {
                float blockContribution = vanillaBlockBrightness(ambientLight, blockLight);
                float brightness = Mth.clamp(skyContribution + blockContribution, 0f, 1f);
                int value = (int) (brightness * 255f);

                lightPixels.setPixelRGBA(blockLight, skyLight, 0xFF000000 | (value << 16) | (value << 8) | value);
            }
        }
        lightTexture.upload();
        this.updateLightTexture = false;
        ci.cancel();
    }

    private static float vanillaBlockBrightness(float ambientLight, int blockLight) {
        float level = blockLight / 15f;
        float curve = level / (4f - 3f * level);
        return Mth.lerp(ambientLight, curve, 1f);
    }
}
