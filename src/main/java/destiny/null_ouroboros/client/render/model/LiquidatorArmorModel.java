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

public class LiquidatorArmorModel extends EntityModel<LivingEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "liquidator_armor"), "main");
	final ModelPart Armor;
	final ModelPart Head;
	final ModelPart Helmet;
	final ModelPart HelmetEmissive;
	final ModelPart Head2;
	final ModelPart Helmet2;
	final ModelPart Respirator;
	final ModelPart LeftFilter;
	final ModelPart LeftFilterEmissive;
	final ModelPart RightFilter;
	final ModelPart RightFilterEmissive;
	final ModelPart Body;
	final ModelPart ChestplateBody;
	final ModelPart ChestplateBodyEmissive;
	final ModelPart LeggingsBodyBelt;
	final ModelPart LeggingsBodyBeltEmissive;
	final ModelPart RightArm;
	final ModelPart ChestplateRightArm;
	final ModelPart ChestplateRightShoulderPlate;
	final ModelPart ChestplateRightArmEmissive;
	final ModelPart LeftArm;
	final ModelPart ChestplateLeftArm;
	final ModelPart ChestplateLeftArmEmissive;
	final ModelPart ChestplateLeftShoulderPlate;
	final ModelPart RightLeg;
	final ModelPart RightLegging;
	final ModelPart RightBoot;
	final ModelPart RightBootEmissive;
	final ModelPart LeftLeg;
	final ModelPart LeftLegging;
	final ModelPart LeftBoot;
	final ModelPart LeftBootEmissive;

    public LiquidatorArmorModel(ModelPart root) {
		this.Armor = root.getChild("Armor");
		this.Head = this.Armor.getChild("Head");
		this.Helmet = this.Head.getChild("Helmet");
		this.HelmetEmissive = this.Helmet.getChild("HelmetEmissive");
		this.Head2 = this.Head.getChild("Head2");
		this.Helmet2 = this.Head2.getChild("Helmet2");
		this.Respirator = this.Helmet2.getChild("Respirator");
		this.LeftFilter = this.Respirator.getChild("LeftFilter");
		this.LeftFilterEmissive = this.LeftFilter.getChild("LeftFilterEmissive");
		this.RightFilter = this.Respirator.getChild("RightFilter");
		this.RightFilterEmissive = this.RightFilter.getChild("RightFilterEmissive");
		this.Body = this.Armor.getChild("Body");
		this.ChestplateBody = this.Body.getChild("ChestplateBody");
		this.ChestplateBodyEmissive = this.ChestplateBody.getChild("ChestplateBodyEmissive");
		this.LeggingsBodyBelt = this.Body.getChild("LeggingsBodyBelt");
		this.LeggingsBodyBeltEmissive = this.LeggingsBodyBelt.getChild("LeggingsBodyBeltEmissive");
		this.RightArm = this.Armor.getChild("RightArm");
		this.ChestplateRightArm = this.RightArm.getChild("ChestplateRightArm");
		this.ChestplateRightShoulderPlate = this.ChestplateRightArm.getChild("ChestplateRightShoulderPlate");
		this.ChestplateRightArmEmissive = this.ChestplateRightArm.getChild("ChestplateRightArmEmissive");
		this.LeftArm = this.Armor.getChild("LeftArm");
		this.ChestplateLeftArm = this.LeftArm.getChild("ChestplateLeftArm");
		this.ChestplateLeftArmEmissive = this.ChestplateLeftArm.getChild("ChestplateLeftArmEmissive");
		this.ChestplateLeftShoulderPlate = this.ChestplateLeftArm.getChild("ChestplateLeftShoulderPlate");
		this.RightLeg = this.Armor.getChild("RightLeg");
		this.RightLegging = this.RightLeg.getChild("RightLegging");
		this.RightBoot = this.RightLeg.getChild("RightBoot");
		this.RightBootEmissive = this.RightBoot.getChild("RightBootEmissive");
		this.LeftLeg = this.Armor.getChild("LeftLeg");
		this.LeftLegging = this.LeftLeg.getChild("LeftLegging");
		this.LeftBoot = this.LeftLeg.getChild("LeftBoot");
		this.LeftBootEmissive = this.LeftBoot.getChild("LeftBootEmissive");
    }

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition Armor = partdefinition.addOrReplaceChild("Armor", CubeListBuilder.create(), PartPose.offset(0.0F, 12.0F, 0.0F));

		PartDefinition Head = Armor.addOrReplaceChild("Head", CubeListBuilder.create(), PartPose.offset(0.0F, -12.0F, 0.0F));

		PartDefinition Helmet = Head.addOrReplaceChild("Helmet", CubeListBuilder.create().texOffs(66, 38).addBox(-4.0F, -32.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.01F)), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition HelmetEmissive = Helmet.addOrReplaceChild("HelmetEmissive", CubeListBuilder.create().texOffs(66, 54).addBox(-4.0F, -32.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.01F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition Head2 = Head.addOrReplaceChild("Head2", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition Helmet2 = Head2.addOrReplaceChild("Helmet2", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition Respirator = Helmet2.addOrReplaceChild("Respirator", CubeListBuilder.create().texOffs(10, 0).addBox(2.0F, -2.5F, -1.5F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(-3.5F, -23.5F, -4.0F));

		PartDefinition Respirator_r1 = Respirator.addOrReplaceChild("Respirator_r1", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-1.0F, -1.5F, 0.5F, 3.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.3927F, 0.7854F, 0.0F));

		PartDefinition Respirator_r2 = Respirator.addOrReplaceChild("Respirator_r2", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -1.5F, 0.5F, 3.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(7.0F, 0.0F, 0.0F, 0.3927F, -0.7854F, 0.0F));

		PartDefinition LeftFilter = Respirator.addOrReplaceChild("LeftFilter", CubeListBuilder.create(), PartPose.offset(7.0F, 0.0F, 0.0F));

		PartDefinition LeftFilter_r1 = LeftFilter.addOrReplaceChild("LeftFilter_r1", CubeListBuilder.create().texOffs(0, 9).addBox(-2.0F, -1.5F, -0.5F, 3.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.3927F, -0.7854F, 0.0F));

		PartDefinition LeftFilterEmissive = LeftFilter.addOrReplaceChild("LeftFilterEmissive", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition LeftFilterEmissive_r1 = LeftFilterEmissive.addOrReplaceChild("LeftFilterEmissive_r1", CubeListBuilder.create().texOffs(0, 5).addBox(-2.0F, -1.5F, -0.5F, 3.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.3927F, -0.7854F, 0.0F));

		PartDefinition RightFilter = Respirator.addOrReplaceChild("RightFilter", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition RightFilter_r1 = RightFilter.addOrReplaceChild("RightFilter_r1", CubeListBuilder.create().texOffs(0, 9).mirror().addBox(-1.0F, -1.5F, -0.5F, 3.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.3927F, 0.7854F, 0.0F));

		PartDefinition RightFilterEmissive = RightFilter.addOrReplaceChild("RightFilterEmissive", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition RightFilterEmissive_r1 = RightFilterEmissive.addOrReplaceChild("RightFilterEmissive_r1", CubeListBuilder.create().texOffs(0, 5).mirror().addBox(-1.0F, -1.5F, -0.5F, 3.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.3927F, 0.7854F, 0.0F));

		PartDefinition Body = Armor.addOrReplaceChild("Body", CubeListBuilder.create(), PartPose.offset(0.0F, -12.0F, 0.0F));

		PartDefinition ChestplateBody = Body.addOrReplaceChild("ChestplateBody", CubeListBuilder.create().texOffs(72, 22).addBox(-4.0F, -24.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.01F))
		.texOffs(96, 22).addBox(-4.0F, -24.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.3F)), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition ChestplateBodyEmissive = ChestplateBody.addOrReplaceChild("ChestplateBodyEmissive", CubeListBuilder.create().texOffs(104, 14).addBox(-3.0F, -24.0F, -3.0F, 6.0F, 1.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -0.5F, 0.0F));

		PartDefinition LeggingsBodyBelt = Body.addOrReplaceChild("LeggingsBodyBelt", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition LeggingsBodyBeltEmissive = LeggingsBodyBelt.addOrReplaceChild("LeggingsBodyBeltEmissive", CubeListBuilder.create().texOffs(72, 16).addBox(-4.0F, -14.0F, -2.0F, 8.0F, 2.0F, 4.0F, new CubeDeformation(0.2F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition RightArm = Armor.addOrReplaceChild("RightArm", CubeListBuilder.create(), PartPose.offset(-5.0F, -10.0F, 0.0F));

		PartDefinition ChestplateRightArm = RightArm.addOrReplaceChild("ChestplateRightArm", CubeListBuilder.create().texOffs(56, 0).addBox(-8.0F, -24.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.1F))
		.texOffs(56, 16).addBox(-8.0F, -19.0F, -2.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.3F)), PartPose.offset(5.0F, 22.0F, 0.0F));

		PartDefinition ChestplateRightShoulderPlate = ChestplateRightArm.addOrReplaceChild("ChestplateRightShoulderPlate", CubeListBuilder.create(), PartPose.offsetAndRotation(-6.5F, -22.5F, 0.0F, 0.0F, 0.0F, 0.3927F));

		PartDefinition ChestplateRightShoulderPlate_r1 = ChestplateRightShoulderPlate.addOrReplaceChild("ChestplateRightShoulderPlate_r1", CubeListBuilder.create().texOffs(88, 0).addBox(-3.0F, -2.5F, -3.0F, 6.0F, 5.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.7854F, 0.0F));

		PartDefinition ChestplateRightArmEmissive = ChestplateRightArm.addOrReplaceChild("ChestplateRightArmEmissive", CubeListBuilder.create().texOffs(32, 0).addBox(3.0F, -17.0F, -4.0F, 6.0F, 1.0F, 6.0F, new CubeDeformation(0.0F))
		.texOffs(40, 7).addBox(4.0F, -24.0F, -3.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.1F)), PartPose.offset(-12.0F, 0.0F, 1.0F));

		PartDefinition LeftArm = Armor.addOrReplaceChild("LeftArm", CubeListBuilder.create(), PartPose.offset(5.0F, -10.0F, 0.0F));

		PartDefinition ChestplateLeftArm = LeftArm.addOrReplaceChild("ChestplateLeftArm", CubeListBuilder.create().texOffs(56, 0).mirror().addBox(-2.5F, -1.5F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.1F)).mirror(false)
		.texOffs(56, 16).mirror().addBox(-2.5F, 3.5F, -2.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.3F)).mirror(false), PartPose.offset(1.5F, -0.5F, 0.0F));

		PartDefinition ChestplateLeftArmEmissive = ChestplateLeftArm.addOrReplaceChild("ChestplateLeftArmEmissive", CubeListBuilder.create().texOffs(32, 0).mirror().addBox(-9.0F, -17.0F, -4.0F, 6.0F, 1.0F, 6.0F, new CubeDeformation(0.0F)).mirror(false)
		.texOffs(40, 7).mirror().addBox(-8.0F, -24.0F, -3.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.1F)).mirror(false), PartPose.offset(5.5F, 22.5F, 1.0F));

		PartDefinition ChestplateLeftShoulderPlate = ChestplateLeftArm.addOrReplaceChild("ChestplateLeftShoulderPlate", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.3927F));

		PartDefinition ChestplateLeftShoulderPlate_r1 = ChestplateLeftShoulderPlate.addOrReplaceChild("ChestplateLeftShoulderPlate_r1", CubeListBuilder.create().texOffs(88, 0).mirror().addBox(-3.0F, -2.5F, -3.0F, 6.0F, 5.0F, 6.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.7854F, 0.0F));

		PartDefinition RightLeg = Armor.addOrReplaceChild("RightLeg", CubeListBuilder.create(), PartPose.offset(-1.9F, 0.0F, 0.0F));

		PartDefinition RightLegging = RightLeg.addOrReplaceChild("RightLegging", CubeListBuilder.create().texOffs(108, 6).addBox(-4.4F, -10.0F, -2.5F, 5.0F, 3.0F, 5.0F, new CubeDeformation(0.01F))
		.texOffs(56, 22).addBox(-3.9F, -12.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.01F)), PartPose.offset(1.9F, 12.0F, 0.0F));

		PartDefinition RightBoot = RightLeg.addOrReplaceChild("RightBoot", CubeListBuilder.create(), PartPose.offset(1.9F, 12.0F, -1.0F));

		PartDefinition RightBootEmissive = RightBoot.addOrReplaceChild("RightBootEmissive", CubeListBuilder.create().texOffs(32, 0).addBox(-13.0F, -17.0F, -4.0F, 6.0F, 1.0F, 6.0F, new CubeDeformation(0.0F))
		.texOffs(84, 0).addBox(-11.9F, -13.0F, -4.0F, 4.0F, 2.0F, 1.0F, new CubeDeformation(0.01F))
		.texOffs(72, 0).addBox(-11.9F, -17.0F, -3.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.01F)), PartPose.offset(8.0F, 11.0F, 2.0F));

		PartDefinition LeftLeg = Armor.addOrReplaceChild("LeftLeg", CubeListBuilder.create(), PartPose.offset(1.9F, 0.0F, 0.0F));

		PartDefinition LeftLegging = LeftLeg.addOrReplaceChild("LeftLegging", CubeListBuilder.create().texOffs(56, 22).mirror().addBox(0.4F, -12.0F, -2.5F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.01F)).mirror(false)
		.texOffs(108, 6).mirror().addBox(-0.1F, -10.0F, -3.0F, 5.0F, 3.0F, 5.0F, new CubeDeformation(0.01F)).mirror(false), PartPose.offset(-2.4F, 12.0F, 0.5F));

		PartDefinition LeftBoot = LeftLeg.addOrReplaceChild("LeftBoot", CubeListBuilder.create(), PartPose.offset(-1.9F, 12.0F, -1.0F));

		PartDefinition LeftBootEmissive = LeftBoot.addOrReplaceChild("LeftBootEmissive", CubeListBuilder.create().texOffs(32, 0).mirror().addBox(-9.0F, -17.0F, -4.0F, 6.0F, 1.0F, 6.0F, new CubeDeformation(0.0F)).mirror(false)
		.texOffs(84, 0).mirror().addBox(-8.1F, -13.0F, -4.0F, 4.0F, 2.0F, 1.0F, new CubeDeformation(0.01F)).mirror(false)
		.texOffs(72, 0).mirror().addBox(-8.1F, -17.0F, -3.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.01F)).mirror(false), PartPose.offset(8.0F, 11.0F, 2.0F));

		return LayerDefinition.create(meshdefinition, 128, 128);
	}

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        Armor.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    @Override
    public void setupAnim(LivingEntity entity, float v, float v1, float v2, float v3, float v4) { }

    public void copyFrom(HumanoidModel<?> model) {
        Head.xRot = model.head.xRot;
        Head.yRot = model.head.yRot;
        Head.zRot = model.head.zRot;

        Body.xRot = model.body.xRot;
        Body.yRot = model.body.yRot;
        Body.zRot = model.body.zRot;

        RightArm.xRot = model.rightArm.xRot;
        RightArm.yRot = model.rightArm.yRot;
        RightArm.zRot = model.rightArm.zRot;

        LeftArm.xRot = model.leftArm.xRot;
        LeftArm.yRot = model.leftArm.yRot;
        LeftArm.zRot = model.leftArm.zRot;

        RightLeg.xRot = model.rightLeg.xRot;
        RightLeg.yRot = model.rightLeg.yRot;
        RightLeg.zRot = model.rightLeg.zRot;

        LeftLeg.xRot = model.leftLeg.xRot;
        LeftLeg.yRot = model.leftLeg.yRot;
        LeftLeg.zRot = model.leftLeg.zRot;

        Head.x = model.head.x;
        Head.y = model.head.y - 12;
        Head.z = model.head.z;

        Body.x = model.body.x;
        Body.y = model.body.y - 12;
        Body.z = model.body.z;

        RightArm.x = model.rightArm.x;
        RightArm.y = model.rightArm.y - 12;
        RightArm.z = model.rightArm.z;

        LeftArm.x = model.leftArm.x;
        LeftArm.y = model.leftArm.y - 12;
        LeftArm.z = model.leftArm.z;

        RightLeg.x = model.rightLeg.x;
        RightLeg.y = model.rightLeg.y - 12;
        RightLeg.z = model.rightLeg.z;

        LeftLeg.x = model.leftLeg.x;
        LeftLeg.y = model.leftLeg.y - 12;
        LeftLeg.z = model.leftLeg.z;

        Head.xScale = model.head.xScale;
        Head.yScale = model.head.yScale;
        Head.zScale = model.head.zScale;

        Body.xScale = model.body.xScale;
        Body.yScale = model.body.yScale;
        Body.zScale = model.body.zScale;

        RightArm.xScale = model.rightArm.xScale;
        RightArm.yScale = model.rightArm.yScale;
        RightArm.zScale = model.rightArm.zScale;

        LeftArm.xScale = model.leftArm.xScale;
        LeftArm.yScale = model.leftArm.yScale;
        LeftArm.zScale = model.leftArm.zScale;

        RightLeg.xScale = model.rightLeg.xScale;
        RightLeg.yScale = model.rightLeg.yScale;
        RightLeg.zScale = model.rightLeg.zScale;

        LeftLeg.xScale = model.leftLeg.xScale;
        LeftLeg.yScale = model.leftLeg.yScale;
        LeftLeg.zScale = model.leftLeg.zScale;
    }

    public void setFiltersVisible(boolean left, boolean right) {
        LeftFilter.visible = left;
        RightFilter.visible = right;
    }

    public void renderEmissive(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay) {
        renderPartWithAncestors(HelmetEmissive, poseStack, consumer, packedLight, packedOverlay);
        renderPartWithAncestors(LeftFilterEmissive, poseStack, consumer, packedLight, packedOverlay);
        renderPartWithAncestors(RightFilterEmissive, poseStack, consumer, packedLight, packedOverlay);
        renderPartWithAncestors(ChestplateBodyEmissive, poseStack, consumer, packedLight, packedOverlay);
        renderPartWithAncestors(LeggingsBodyBeltEmissive, poseStack, consumer, packedLight, packedOverlay);
        renderPartWithAncestors(ChestplateRightArmEmissive, poseStack, consumer, packedLight, packedOverlay);
        renderPartWithAncestors(ChestplateLeftArmEmissive, poseStack, consumer, packedLight, packedOverlay);
        renderPartWithAncestors(RightBootEmissive, poseStack, consumer, packedLight, packedOverlay);
        renderPartWithAncestors(LeftBootEmissive, poseStack, consumer, packedLight, packedOverlay);
    }

    private void renderPartWithAncestors(ModelPart part, PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay) {
        if (!part.visible) return;
        poseStack.pushPose();

        Armor.translateAndRotate(poseStack);

        if (part == HelmetEmissive) {
            Head.translateAndRotate(poseStack);
            Helmet.translateAndRotate(poseStack);
        } else if (part == LeftFilterEmissive || part == RightFilterEmissive) {
            Head.translateAndRotate(poseStack);
            Helmet.translateAndRotate(poseStack);
            Respirator.translateAndRotate(poseStack);
            if (part == LeftFilterEmissive) {
                LeftFilter.translateAndRotate(poseStack);
            } else {
                RightFilter.translateAndRotate(poseStack);
            }
        } else if (part == ChestplateBodyEmissive) {
            Body.translateAndRotate(poseStack);
            ChestplateBody.translateAndRotate(poseStack);
        } else if (part == LeggingsBodyBeltEmissive) {
            Body.translateAndRotate(poseStack);
            LeggingsBodyBelt.translateAndRotate(poseStack);
        } else if (part == ChestplateRightArmEmissive) {
            RightArm.translateAndRotate(poseStack);
            ChestplateRightArm.translateAndRotate(poseStack);
        } else if (part == ChestplateLeftArmEmissive) {
            LeftArm.translateAndRotate(poseStack);
            ChestplateLeftArm.translateAndRotate(poseStack);
        } else if (part == RightBootEmissive) {
            RightLeg.translateAndRotate(poseStack);
            RightBoot.translateAndRotate(poseStack);
        } else if (part == LeftBootEmissive) {
            LeftLeg.translateAndRotate(poseStack);
            LeftBoot.translateAndRotate(poseStack);
        }
        part.render(poseStack, consumer, packedLight, packedOverlay);
        poseStack.popPose();
    }
}