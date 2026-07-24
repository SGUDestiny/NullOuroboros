package destiny.null_ouroboros.client.render.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.client.render.RenderTypeRegistry;
import destiny.null_ouroboros.common.revolver.RevolverCartridge;
import destiny.null_ouroboros.common.revolver.RevolverState;
import destiny.null_ouroboros.server.item.HeavyRevolverItem;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.RenderUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HeavyRevolverGeoRenderer extends GeoItemRenderer<HeavyRevolverItem> {
    private static final UUID FALLBACK_VISUAL_ID = new UUID(0L, 0L);
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/item/heavy_revolver.png");
    private static final ResourceLocation EMPTY_CASING_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/item/heavy_revolver_empty_casing.png");
    private static final Set<UUID> SNAP_HAMMER = ConcurrentHashMap.newKeySet();

    private final Map<UUID, VisualState> visualStates = new HashMap<>();
    private ItemStack renderedStack = ItemStack.EMPTY;

    public HeavyRevolverGeoRenderer() {
        super(new DefaultedItemGeoModel<>(ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "heavy_revolver")));
    }

    public static void requestHammerSnap(ItemStack stack) {
        SNAP_HAMMER.add(RevolverState.ensureVisualId(stack));
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        renderedStack = stack;
        super.renderByItem(stack, displayContext, poseStack, bufferSource, packedLight, packedOverlay);
        renderedStack = ItemStack.EMPTY;
    }

    @Override
    public void actuallyRender(PoseStack poseStack, HeavyRevolverItem animatable, BakedGeoModel model,
                               RenderType renderType, MultiBufferSource bufferSource,
                               VertexConsumer buffer, boolean isReRender, float partialTick,
                               int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        if (!renderedStack.isEmpty()) {
            applyState(renderedStack);
        }
        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }

    @Override
    public void renderRecursively(PoseStack poseStack, HeavyRevolverItem animatable, GeoBone bone,
                                  RenderType renderType, MultiBufferSource bufferSource,
                                  VertexConsumer buffer, boolean isReRender, float partialTick,
                                  int packedLight, int packedOverlay,
                                  float red, float green, float blue, float alpha) {
        String name = bone.getName();
        boolean emissive = isEmissiveBone(name);
        int chamber = resolveChamber(bone);
        RevolverCartridge cartridge = chamber >= 0 && !renderedStack.isEmpty()
                ? RevolverState.getChamber(renderedStack, chamber)
                : null;

        if (cartridge != null) {
            if (name.toLowerCase().startsWith("cartridge") && cartridge == RevolverCartridge.EMPTY) {
                return;
            }
            if (name.toLowerCase().startsWith("bullet") && !cartridge.isLive()) {
                return;
            }
            if (emissive && cartridge == RevolverCartridge.EMPTY) {
                return;
            }
        }

        ResourceLocation texture = resolveTexture(name, cartridge);
        RenderType boneRenderType;
        int light = packedLight;
        int overlay = packedOverlay;
        boolean glow = cartridge != null && cartridge.isLive()
                && (emissive || name.toLowerCase().startsWith("bullet"));
        if (glow) {
            boneRenderType = RenderTypeRegistry.getOpaqueEmissiveRenderType(texture);
            light = LightTexture.FULL_BRIGHT;
            overlay = OverlayTexture.NO_OVERLAY;
        } else if (emissive && cartridge == RevolverCartridge.CASING) {
            boneRenderType = RenderType.entityCutoutNoCull(EMPTY_CASING_TEXTURE);
        } else {
            boneRenderType = RenderType.entityCutoutNoCull(texture);
        }
        VertexConsumer boneBuffer = bufferSource.getBuffer(boneRenderType);

        poseStack.pushPose();
        RenderUtils.translateMatrixToBone(poseStack, bone);
        RenderUtils.translateToPivotPoint(poseStack, bone);
        RenderUtils.rotateMatrixAroundBone(poseStack, bone);
        RenderUtils.scaleMatrixForBone(poseStack, bone);
        RenderUtils.translateAwayFromPivotPoint(poseStack, bone);

        super.renderCubesOfBone(poseStack, bone, boneBuffer, light, overlay, red, green, blue, alpha);
        for (GeoBone child : bone.getChildBones()) {
            this.renderRecursively(poseStack, animatable, child, boneRenderType, bufferSource,
                    boneBuffer, isReRender, partialTick, packedLight, packedOverlay,
                    red, green, blue, alpha);
        }
        poseStack.popPose();
    }

    private ResourceLocation resolveTexture(String boneName, RevolverCartridge cartridge) {
        if (cartridge == RevolverCartridge.CASING) {
            return EMPTY_CASING_TEXTURE;
        }
        return TEXTURE;
    }

    private void applyState(ItemStack stack) {
        UUID visualId = RevolverState.getVisualId(stack);
        UUID stateId = visualId == null ? FALLBACK_VISUAL_ID : visualId;
        VisualState visual = visualStates.computeIfAbsent(stateId,
                ignored -> new VisualState(
                        RevolverState.getCylinderAngle(stack),
                        RevolverState.isCocked(stack) ? 22.5F : 0.0F,
                        RevolverState.isReloading(stack) ? 1.0F : 0.0F));
        long now = System.nanoTime();
        float seconds = Math.min((now - visual.lastUpdateNanos) / 1_000_000_000.0F, 0.05F);
        visual.lastUpdateNanos = now;

        float targetCylinder = RevolverState.getCylinderAngle(stack);
        float targetHammer = RevolverState.isCocked(stack) ? 22.5F : 0.0F;
        float targetOpen = RevolverState.isReloading(stack) ? 1.0F : 0.0F;
        visual.cylinderAngle = Mth.approach(visual.cylinderAngle, targetCylinder, seconds * 240.0F);
        if (visualId != null && SNAP_HAMMER.remove(visualId)) {
            visual.hammerAngle = 0.0F;
        } else {
            visual.hammerAngle = Mth.approach(visual.hammerAngle, targetHammer, seconds * 90.0F);
        }
        visual.cylinderOpen = Mth.approach(visual.cylinderOpen, targetOpen, seconds * 4.0F);

        getGeoModel().getBone("cylinder").ifPresent(bone -> {
            bone.setRotZ((-27.5F + visual.cylinderAngle) * Mth.DEG_TO_RAD);
            bone.setPosX(4.0F * visual.cylinderOpen);
            bone.setPosY(-visual.cylinderOpen);
        });
        getGeoModel().getBone("hammer").ifPresent(bone -> bone.setRotX(visual.hammerAngle * Mth.DEG_TO_RAD));
    }

    private static boolean isEmissiveBone(String name) {
        return name.toLowerCase().contains("emissive");
    }

    private static int resolveChamber(GeoBone bone) {
        GeoBone current = bone;
        while (current != null) {
            int chamber = chamberFromBone(current.getName());
            if (chamber >= 0) {
                return chamber;
            }
            current = current.getParent();
        }
        return -1;
    }

    private static int chamberFromBone(String name) {
        String lower = name.toLowerCase();
        if (lower.startsWith("cartridge")) {
            return parseChamberSuffix(name.substring("cartridge".length()));
        }
        if (lower.startsWith("shell")) {
            return parseChamberSuffix(name.substring("shell".length()));
        }
        if (lower.startsWith("bullet")) {
            return parseChamberSuffix(name.substring("bullet".length()));
        }
        if (lower.startsWith("emissive")) {
            return parseChamberSuffix(name.substring("emissive".length()));
        }
        return -1;
    }

    private static int parseChamberSuffix(String suffix) {
        if (suffix.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(suffix) - 1;
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static final class VisualState {
        private float cylinderAngle;
        private float hammerAngle;
        private float cylinderOpen;
        private long lastUpdateNanos;

        private VisualState(float cylinderAngle, float hammerAngle, float cylinderOpen) {
            this.cylinderAngle = cylinderAngle;
            this.hammerAngle = hammerAngle;
            this.cylinderOpen = cylinderOpen;
            this.lastUpdateNanos = System.nanoTime();
        }
    }
}
