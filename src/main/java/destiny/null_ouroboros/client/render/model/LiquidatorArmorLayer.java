package destiny.null_ouroboros.client.render.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.client.render.RenderTypeRegistry;
import destiny.null_ouroboros.server.registry.ArmorMaterialRegistry;
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
import net.minecraft.world.item.ArmorItem;

public class LiquidatorArmorLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {
    private final LiquidatorArmorModel armorModel;

    public LiquidatorArmorLayer(RenderLayerParent renderer) {
        super(renderer);
        this.armorModel = new LiquidatorArmorModel(Minecraft.getInstance().getEntityModels().bakeLayer(LiquidatorArmorModel.LAYER_LOCATION));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        armorModel.copyFrom(getParentModel());

        boolean hasHelmet = entity.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof ArmorItem &&
                ((ArmorItem) entity.getItemBySlot(EquipmentSlot.HEAD).getItem()).getMaterial() == ArmorMaterialRegistry.LIQUIDATOR;
        boolean hasChestplate = entity.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof ArmorItem &&
                ((ArmorItem) entity.getItemBySlot(EquipmentSlot.CHEST).getItem()).getMaterial() == ArmorMaterialRegistry.LIQUIDATOR;
        boolean hasLeggings = entity.getItemBySlot(EquipmentSlot.LEGS).getItem() instanceof ArmorItem &&
                ((ArmorItem) entity.getItemBySlot(EquipmentSlot.LEGS).getItem()).getMaterial() == ArmorMaterialRegistry.LIQUIDATOR;
        boolean hasBoots = entity.getItemBySlot(EquipmentSlot.FEET).getItem() instanceof ArmorItem &&
                ((ArmorItem) entity.getItemBySlot(EquipmentSlot.FEET).getItem()).getMaterial() == ArmorMaterialRegistry.LIQUIDATOR;

        if (!hasHelmet && !hasChestplate && !hasLeggings && !hasBoots) return;

        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/entity/liquidator_armor.png");

        armorModel.Armor.visible = true;
        armorModel.Head.visible = true;
        armorModel.Body.visible = true;
        armorModel.RightArm.visible = true;
        armorModel.LeftArm.visible = true;
        armorModel.RightLeg.visible = true;
        armorModel.LeftLeg.visible = true;

        armorModel.Helmet.visible = hasHelmet;
        armorModel.ChestplateBody.visible = hasChestplate;
        armorModel.ChestplateRightArm.visible = hasChestplate;
        armorModel.ChestplateLeftArm.visible = hasChestplate;
        armorModel.LeggingsBodyBelt.visible = hasLeggings;
        armorModel.RightLegging.visible = hasLeggings;
        armorModel.LeftLegging.visible = hasLeggings;
        armorModel.RightBoot.visible = hasBoots;
        armorModel.LeftBoot.visible = hasBoots;

        armorModel.HelmetEmissive.visible = hasHelmet;
        armorModel.LeftFilterEmissive.visible = hasHelmet;
        armorModel.RightFilterEmissive.visible = hasHelmet;
        armorModel.ChestplateBodyEmissive.visible = hasChestplate;
        armorModel.LeggingsBodyBeltEmissive.visible = hasLeggings;
        armorModel.ChestplateRightArmEmissive.visible = hasChestplate;
        armorModel.ChestplateLeftArmEmissive.visible = hasChestplate;
        armorModel.RightBootEmissive.visible = hasBoots;
        armorModel.LeftBootEmissive.visible = hasBoots;

        armorModel.HelmetEmissive.visible = false;
        armorModel.LeftFilterEmissive.visible = false;
        armorModel.RightFilterEmissive.visible = false;
        armorModel.ChestplateBodyEmissive.visible = false;
        armorModel.LeggingsBodyBeltEmissive.visible = false;
        armorModel.ChestplateRightArmEmissive.visible = false;
        armorModel.ChestplateLeftArmEmissive.visible = false;
        armorModel.RightBootEmissive.visible = false;
        armorModel.LeftBootEmissive.visible = false;

        VertexConsumer normalConsumer = buffer.getBuffer(RenderType.entityTranslucent(texture));
        armorModel.renderToBuffer(poseStack, normalConsumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);

        armorModel.HelmetEmissive.visible = hasHelmet;
        armorModel.LeftFilterEmissive.visible = hasHelmet;
        armorModel.RightFilterEmissive.visible = hasHelmet;
        armorModel.ChestplateBodyEmissive.visible = hasChestplate;
        armorModel.LeggingsBodyBeltEmissive.visible = hasLeggings;
        armorModel.ChestplateRightArmEmissive.visible = hasChestplate;
        armorModel.ChestplateLeftArmEmissive.visible = hasChestplate;
        armorModel.RightBootEmissive.visible = hasBoots;
        armorModel.LeftBootEmissive.visible = hasBoots;

        VertexConsumer emissiveConsumer = buffer.getBuffer(RenderTypeRegistry.getEmissiveRenderType(texture));
        armorModel.renderEmissive(poseStack, emissiveConsumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
    }
}