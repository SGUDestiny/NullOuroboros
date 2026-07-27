package destiny.null_ouroboros.client.render.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.client.render.RenderTypeRegistry;
import destiny.null_ouroboros.server.item.RespiratorGear;
import destiny.null_ouroboros.server.item.RespiratorItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class RespiratorLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/entity/respirator.png");

    private final RespiratorModel model;

    public RespiratorLayer(RenderLayerParent<T, M> renderer) {
        super(renderer);
        this.model = new RespiratorModel(Minecraft.getInstance().getEntityModels().bakeLayer(RespiratorModel.LAYER_LOCATION));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        ItemStack head = entity.getItemBySlot(EquipmentSlot.HEAD);
        if (!(head.getItem() instanceof RespiratorItem)) {
            return;
        }
        if (!getParentModel().head.visible) {
            return;
        }

        RespiratorGear.ensureDefaults(head);
        model.copyFrom(getParentModel());
        model.Armor.visible = true;
        model.Head.visible = true;
        model.setFiltersVisible(RespiratorGear.hasLeftFilter(head), RespiratorGear.hasRightFilter(head));

        model.LeftFilterEmissive.visible = false;
        model.RightFilterEmissive.visible = false;

        VertexConsumer normalConsumer = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
        model.renderToBuffer(poseStack, normalConsumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);

        model.LeftFilterEmissive.visible = RespiratorGear.hasLeftFilter(head);
        model.RightFilterEmissive.visible = RespiratorGear.hasRightFilter(head);
        VertexConsumer emissiveConsumer = buffer.getBuffer(RenderTypeRegistry.getEmissiveRenderType(TEXTURE));
        model.renderEmissive(poseStack, emissiveConsumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
    }
}
