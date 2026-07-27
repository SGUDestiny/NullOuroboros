package destiny.null_ouroboros.client.render.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.client.render.RenderTypeRegistry;
import destiny.null_ouroboros.server.item.RespiratorGear;
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
import net.minecraft.world.item.ItemStack;

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
        M parent = getParentModel();

        armorModel.Armor.visible = true;
        armorModel.Head.visible = parent.head.visible;
        armorModel.Body.visible = parent.body.visible;
        armorModel.RightArm.visible = parent.rightArm.visible;
        armorModel.LeftArm.visible = parent.leftArm.visible;
        armorModel.RightLeg.visible = parent.rightLeg.visible;
        armorModel.LeftLeg.visible = parent.leftLeg.visible;

        armorModel.Helmet.visible = hasHelmet;
        armorModel.ChestplateBody.visible = hasChestplate;
        armorModel.ChestplateRightArm.visible = hasChestplate;
        armorModel.ChestplateLeftArm.visible = hasChestplate;
        armorModel.LeggingsBodyBelt.visible = hasLeggings;
        armorModel.RightLegging.visible = hasLeggings;
        armorModel.LeftLegging.visible = hasLeggings;
        armorModel.RightBoot.visible = hasBoots;
        armorModel.LeftBoot.visible = hasBoots;

        if (hasHelmet) {
            ItemStack headStack = entity.getItemBySlot(EquipmentSlot.HEAD);
            RespiratorGear.ensureDefaults(headStack);
            armorModel.setFiltersVisible(RespiratorGear.hasLeftFilter(headStack), RespiratorGear.hasRightFilter(headStack));
        } else {
            armorModel.setFiltersVisible(false, false);
        }

        boolean head = hasHelmet && parent.head.visible;
        boolean chest = hasChestplate && parent.body.visible;
        boolean rightArm = hasChestplate && parent.rightArm.visible;
        boolean leftArm = hasChestplate && parent.leftArm.visible;
        boolean legs = hasLeggings && parent.body.visible;
        boolean rightBoot = hasBoots && parent.rightLeg.visible;
        boolean leftBoot = hasBoots && parent.leftLeg.visible;

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

        armorModel.HelmetEmissive.visible = head;
        ItemStack headStack = entity.getItemBySlot(EquipmentSlot.HEAD);
        armorModel.LeftFilterEmissive.visible = head && RespiratorGear.hasLeftFilter(headStack);
        armorModel.RightFilterEmissive.visible = head && RespiratorGear.hasRightFilter(headStack);
        armorModel.ChestplateBodyEmissive.visible = chest;
        armorModel.LeggingsBodyBeltEmissive.visible = legs;
        armorModel.ChestplateRightArmEmissive.visible = rightArm;
        armorModel.ChestplateLeftArmEmissive.visible = leftArm;
        armorModel.RightBootEmissive.visible = rightBoot;
        armorModel.LeftBootEmissive.visible = leftBoot;

        VertexConsumer emissiveConsumer = buffer.getBuffer(RenderTypeRegistry.getEmissiveRenderType(texture));
        armorModel.renderEmissive(poseStack, emissiveConsumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
    }
}