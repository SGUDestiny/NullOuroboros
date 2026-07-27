package destiny.null_ouroboros.client.render.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import destiny.null_ouroboros.NullOuroboros;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

public class RespiratorModel extends EntityModel<LivingEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "respirator"), "main");

    final ModelPart Armor;
    final ModelPart Head;
    final ModelPart Helmet;
    final ModelPart Respirator;
    final ModelPart LeftFilter;
    final ModelPart LeftFilterEmissive;
    final ModelPart RightFilter;
    final ModelPart RightFilterEmissive;

    public RespiratorModel(ModelPart root) {
        this.Armor = root.getChild("Armor");
        this.Head = this.Armor.getChild("Head");
        this.Helmet = this.Head.getChild("Helmet");
        this.Respirator = this.Helmet.getChild("Respirator");
        this.LeftFilter = this.Respirator.getChild("LeftFilter");
        this.LeftFilterEmissive = this.LeftFilter.getChild("LeftFilterEmissive");
        this.RightFilter = this.Respirator.getChild("RightFilter");
        this.RightFilterEmissive = this.RightFilter.getChild("RightFilterEmissive");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition Armor = partdefinition.addOrReplaceChild("Armor", CubeListBuilder.create(), PartPose.offset(0.0F, 12.0F, 0.0F));
        PartDefinition Head = Armor.addOrReplaceChild("Head", CubeListBuilder.create(), PartPose.offset(0.0F, -12.0F, 0.0F));
        PartDefinition Helmet = Head.addOrReplaceChild("Helmet", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));
        PartDefinition Respirator = Helmet.addOrReplaceChild("Respirator", CubeListBuilder.create().texOffs(10, 0).addBox(2.0F, -2.5F, -1.5F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(-3.5F, -23.5F, -4.0F));
        Respirator.addOrReplaceChild("Respirator_r1", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-1.0F, -1.5F, 0.5F, 3.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.3927F, 0.7854F, 0.0F));
        Respirator.addOrReplaceChild("Respirator_r2", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -1.5F, 0.5F, 3.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(7.0F, 0.0F, 0.0F, 0.3927F, -0.7854F, 0.0F));

        PartDefinition LeftFilter = Respirator.addOrReplaceChild("LeftFilter", CubeListBuilder.create(), PartPose.offset(7.0F, 0.0F, 0.0F));
        LeftFilter.addOrReplaceChild("LeftFilter_r1", CubeListBuilder.create().texOffs(0, 9).addBox(-2.0F, -1.5F, -0.5F, 3.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.3927F, -0.7854F, 0.0F));
        PartDefinition LeftFilterEmissive = LeftFilter.addOrReplaceChild("LeftFilterEmissive", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
        LeftFilterEmissive.addOrReplaceChild("LeftFilterEmissive_r1", CubeListBuilder.create().texOffs(0, 5).addBox(-2.0F, -1.5F, -0.5F, 3.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.3927F, -0.7854F, 0.0F));

        PartDefinition RightFilter = Respirator.addOrReplaceChild("RightFilter", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
        RightFilter.addOrReplaceChild("RightFilter_r1", CubeListBuilder.create().texOffs(0, 9).mirror().addBox(-1.0F, -1.5F, -0.5F, 3.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.3927F, 0.7854F, 0.0F));
        PartDefinition RightFilterEmissive = RightFilter.addOrReplaceChild("RightFilterEmissive", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
        RightFilterEmissive.addOrReplaceChild("RightFilterEmissive_r1", CubeListBuilder.create().texOffs(0, 5).mirror().addBox(-1.0F, -1.5F, -0.5F, 3.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.3927F, 0.7854F, 0.0F));

        return LayerDefinition.create(meshdefinition, 32, 32);
    }

    public void copyFrom(HumanoidModel<?> model) {
        Head.xRot = model.head.xRot;
        Head.yRot = model.head.yRot;
        Head.zRot = model.head.zRot;
        Head.x = model.head.x;
        Head.y = model.head.y - 12;
        Head.z = model.head.z;
        Head.xScale = model.head.xScale;
        Head.yScale = model.head.yScale;
        Head.zScale = model.head.zScale;
    }

    public void setFiltersVisible(boolean left, boolean right) {
        LeftFilter.visible = left;
        RightFilter.visible = right;
    }

    @Override
    public void setupAnim(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        Armor.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    public void renderEmissive(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay) {
        renderPartWithAncestors(LeftFilterEmissive, poseStack, consumer, packedLight, packedOverlay);
        renderPartWithAncestors(RightFilterEmissive, poseStack, consumer, packedLight, packedOverlay);
    }

    private void renderPartWithAncestors(ModelPart part, PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay) {
        if (!part.visible) {
            return;
        }
        poseStack.pushPose();
        Armor.translateAndRotate(poseStack);
        Head.translateAndRotate(poseStack);
        Helmet.translateAndRotate(poseStack);
        Respirator.translateAndRotate(poseStack);
        if (part == LeftFilterEmissive) {
            LeftFilter.translateAndRotate(poseStack);
        } else {
            RightFilter.translateAndRotate(poseStack);
        }
        part.render(poseStack, consumer, packedLight, packedOverlay);
        poseStack.popPose();
    }
}
