package destiny.null_ouroboros.client.render.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.client.render.DusterbikePistonShakeManager;
import destiny.null_ouroboros.common.DusterbikeModelBones;
import destiny.null_ouroboros.common.DusterbikePistonShakeConstants;
import destiny.null_ouroboros.common.DusterbikeTransforms;
import destiny.null_ouroboros.server.entity.DusterbikeEntity;
import destiny.null_ouroboros.server.event.ClientForgeEvents;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DusterbikeEntityModel extends EntityModel<DusterbikeEntity> {
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "dusterbike"), "main");
	public final ModelPart Bike;
	private final ModelPart Front;
	private final ModelPart WheelFront;
	private final ModelPart WheelFrontEmissive;
	private final ModelPart WheelFrontColliderFull;
	private final ModelPart WheelFrontColliderTireless;
	private final ModelPart SuspensionFront;
	private final ModelPart CoverFront;
	private final ModelPart Headlight;
	private final ModelPart HeadlightEmissive;
	private final ModelPart HeadlightInteractionCollider;
	private final ModelPart SpeedGauge;
	private final ModelPart SpeedGaugeArrow;
	private final ModelPart FuelGauge;
	private final ModelPart FuelGaugeArrow;
	private final ModelPart HandleRight;
	private final ModelPart HandleLeft;
	private final ModelPart WheelRear;
	private final ModelPart WheelRearEmissive;
	private final ModelPart WheelRearColliderFull;
	private final ModelPart WheelRearColliderTireless;
	private final ModelPart SuspensionRear;
	private final ModelPart CoverRear;
	private final ModelPart RearBlinkerRight;
	private final ModelPart RearStopLight;
	private final ModelPart RearBlinkerLeft;
	private final ModelPart RearLightInteractionCollider;
	private final ModelPart CoverChain;
	private final ModelPart Body;
	private final ModelPart Engine;
	private final ModelPart PistonRear;
	private final ModelPart PistonFront;
	private final ModelPart Key;
	private final ModelPart KeyInteractionCollider;
	private final ModelPart EngineInteractionCollider;
	private final ModelPart Battery;
	private final ModelPart BatteryInteractionCollider;
	public final ModelPart DriverPos;
	private final ModelPart PassengerPos;
	private final ModelPart FuelIntakeInteractionCollider;
	private final ModelPart Exhaust;
	private final ModelPart ExhaustUpper;
	private final ModelPart ExhaustUpperSmokePoint;
	private final ModelPart ExhaustLower;
	private final ModelPart ExhaustLowerSmokePoint;
	private final ModelPart Piping;
	private final ModelPart Support;

	private final List<ModelPart> emissiveParts;
	private final Map<ModelPart, ModelPart> parentMap = new HashMap<>();

	private static final float SUPPORT_MOUNTED_X_ROT = (float) (-Math.PI / 2.0D);
	private static final float SUPPORT_DEPLOYED_X_ROT = 0.0F;

	public DusterbikeEntityModel(ModelPart root) {
		this.Bike = root.getChild("Bike");
		this.Front = this.Bike.getChild("Front");
		this.WheelFront = this.Front.getChild("WheelFront");
		this.WheelFrontEmissive = this.WheelFront.getChild("WheelFrontEmissive");
		this.WheelFrontColliderFull = this.WheelFront.getChild("WheelFrontColliderFull");
		this.WheelFrontColliderTireless = this.WheelFront.getChild("WheelFrontColliderTireless");
		this.SuspensionFront = this.Front.getChild("SuspensionFront");
		this.CoverFront = this.Front.getChild("CoverFront");
		this.Headlight = this.Front.getChild("Headlight");
		this.HeadlightEmissive = this.Headlight.getChild("HeadlightEmissive");
		this.HeadlightInteractionCollider = this.Headlight.getChild("HeadlightInteractionCollider");
		this.SpeedGauge = this.Headlight.getChild("SpeedGauge");
		this.SpeedGaugeArrow = this.SpeedGauge.getChild("SpeedGaugeArrow");
		this.FuelGauge = this.Headlight.getChild("FuelGauge");
		this.FuelGaugeArrow = this.FuelGauge.getChild("FuelGaugeArrow");
		this.HandleRight = this.Front.getChild("HandleRight");
		this.HandleLeft = this.Front.getChild("HandleLeft");
		this.WheelRear = this.Bike.getChild("WheelRear");
		this.WheelRearEmissive = this.WheelRear.getChild("WheelRearEmissive");
		this.WheelRearColliderFull = this.WheelRear.getChild("WheelRearColliderFull");
		this.WheelRearColliderTireless = this.WheelRear.getChild("WheelRearColliderTireless");
		this.SuspensionRear = this.Bike.getChild("SuspensionRear");
		this.CoverRear = this.Bike.getChild("CoverRear");
		this.RearBlinkerRight = this.CoverRear.getChild("RearBlinkerRight");
		this.RearStopLight = this.CoverRear.getChild("RearStopLight");
		this.RearBlinkerLeft = this.CoverRear.getChild("RearBlinkerLeft");
		this.RearLightInteractionCollider = this.CoverRear.getChild("RearLightInteractionCollider");
		this.CoverChain = this.Bike.getChild("CoverChain");
		this.Body = this.Bike.getChild("Body");
		this.Engine = this.Body.getChild("Engine");
		this.PistonRear = this.Engine.getChild("PistonRear");
		this.PistonFront = this.Engine.getChild("PistonFront");
		this.Key = this.Engine.getChild("Key");
		this.KeyInteractionCollider = this.Key.getChild("KeyInteractionCollider");
		this.EngineInteractionCollider = this.Engine.getChild("EngineInteractionCollider");
		this.Battery = this.Body.getChild("Battery");
		this.BatteryInteractionCollider = this.Battery.getChild("BatteryInteractionCollider");
		this.DriverPos = this.Body.getChild("DriverPos");
		this.PassengerPos = this.Body.getChild("PassengerPos");
		this.FuelIntakeInteractionCollider = this.Body.getChild("FuelIntakeInteractionCollider");
		this.Exhaust = this.Bike.getChild("Exhaust");
		this.ExhaustUpper = this.Exhaust.getChild("ExhaustUpper");
		this.ExhaustUpperSmokePoint = this.ExhaustUpper.getChild("ExhaustUpperSmokePoint");
		this.ExhaustLower = this.Exhaust.getChild("ExhaustLower");
		this.ExhaustLowerSmokePoint = this.ExhaustLower.getChild("ExhaustLowerSmokePoint");
		this.Piping = this.ExhaustLower.getChild("Piping");
		this.Support = this.Bike.getChild("Support");

		this.emissiveParts = List.of(
				this.WheelFrontEmissive,
				this.SuspensionFront.getChild("FrontBlinkerLeft").getChild("FrontBlinkerLeftEmissive"),
				this.SuspensionFront.getChild("FrontBlinkerRight").getChild("FrontBlinkerRightEmissive"),
				this.SuspensionFront.getChild("SuspensionFrontEmissive"),
				this.HeadlightEmissive,
				this.SpeedGauge.getChild("SpeedGaugeEmissive"),
				this.SpeedGaugeArrow.getChild("SpeedGaugeArrowEmissive"),
				this.FuelGauge.getChild("FuelGaugeEmissive"),
				this.FuelGaugeArrow.getChild("FuelGaugeArrowEmissive"),
				this.HandleRight.getChild("HandleRightEmissive"),
				this.HandleLeft.getChild("HandleLeftEmissive"),
				this.WheelRearEmissive,
				this.SuspensionRear.getChild("SuspensionRearEmissive"),
				this.RearBlinkerRight.getChild("RearBlinkerRightEmissive"),
				this.RearStopLight.getChild("RearStopLightEmissive"),
				this.RearBlinkerLeft.getChild("RearBlinkerLeftEmissive"),
				this.CoverChain.getChild("CoverChainEmissive"),
				this.PistonRear.getChild("PistonRearEmissive"),
				this.PistonFront.getChild("PistonFrontEmissive"),
				this.Key.getChild("KeyEmissive"),
				this.Battery.getChild("BatteryEmissive"),
				this.Support.getChild("SupportEmissive")
		);

		registerPartHierarchy();
	}

	private void link(ModelPart child, ModelPart parent) {
		this.parentMap.put(child, parent);
	}

	private void registerPartHierarchy() {
		link(this.Front, this.Bike);
		link(this.WheelFront, this.Front);
		link(this.WheelFrontEmissive, this.WheelFront);
		link(this.WheelFrontColliderFull, this.WheelFront);
		link(this.WheelFrontColliderTireless, this.WheelFront);
		link(this.SuspensionFront, this.Front);
		link(this.SuspensionFront.getChild("FrontBlinkerLeft"), this.SuspensionFront);
		link(this.SuspensionFront.getChild("FrontBlinkerLeft").getChild("FrontBlinkerLeftEmissive"), this.SuspensionFront.getChild("FrontBlinkerLeft"));
		link(this.SuspensionFront.getChild("FrontBlinkerRight"), this.SuspensionFront);
		link(this.SuspensionFront.getChild("FrontBlinkerRight").getChild("FrontBlinkerRightEmissive"), this.SuspensionFront.getChild("FrontBlinkerRight"));
		link(this.SuspensionFront.getChild("SuspensionFrontEmissive"), this.SuspensionFront);
		link(this.CoverFront, this.Front);
		link(this.Headlight, this.Front);
		link(this.HeadlightEmissive, this.Headlight);
		link(this.HeadlightInteractionCollider, this.Headlight);
		link(this.SpeedGauge, this.Headlight);
		link(this.SpeedGauge.getChild("SpeedGaugeEmissive"), this.SpeedGauge);
		link(this.SpeedGaugeArrow, this.SpeedGauge);
		link(this.SpeedGaugeArrow.getChild("SpeedGaugeArrowEmissive"), this.SpeedGaugeArrow);
		link(this.FuelGauge, this.Headlight);
		link(this.FuelGauge.getChild("FuelGaugeEmissive"), this.FuelGauge);
		link(this.FuelGaugeArrow, this.FuelGauge);
		link(this.FuelGaugeArrow.getChild("FuelGaugeArrowEmissive"), this.FuelGaugeArrow);
		link(this.HandleRight, this.Front);
		link(this.HandleRight.getChild("HandleRightEmissive"), this.HandleRight);
		link(this.HandleLeft, this.Front);
		link(this.HandleLeft.getChild("HandleLeftEmissive"), this.HandleLeft);
		link(this.WheelRear, this.Bike);
		link(this.WheelRearEmissive, this.WheelRear);
		link(this.WheelRearColliderFull, this.WheelRear);
		link(this.WheelRearColliderTireless, this.WheelRear);
		link(this.SuspensionRear, this.Bike);
		link(this.SuspensionRear.getChild("SuspensionRearEmissive"), this.SuspensionRear);
		link(this.CoverRear, this.Bike);
		link(this.RearBlinkerRight, this.CoverRear);
		link(this.RearBlinkerRight.getChild("RearBlinkerRightEmissive"), this.RearBlinkerRight);
		link(this.RearStopLight, this.CoverRear);
		link(this.RearStopLight.getChild("RearStopLightEmissive"), this.RearStopLight);
		link(this.RearBlinkerLeft, this.CoverRear);
		link(this.RearBlinkerLeft.getChild("RearBlinkerLeftEmissive"), this.RearBlinkerLeft);
		link(this.RearLightInteractionCollider, this.CoverRear);
		link(this.CoverChain, this.Bike);
		link(this.CoverChain.getChild("CoverChainEmissive"), this.CoverChain);
		link(this.Body, this.Bike);
		link(this.Engine, this.Body);
		link(this.PistonRear, this.Engine);
		link(this.PistonRear.getChild("PistonRearEmissive"), this.PistonRear);
		link(this.PistonFront, this.Engine);
		link(this.PistonFront.getChild("PistonFrontEmissive"), this.PistonFront);
		link(this.Key, this.Engine);
		link(this.Key.getChild("KeyEmissive"), this.Key);
		link(this.KeyInteractionCollider, this.Key);
		link(this.EngineInteractionCollider, this.Engine);
		link(this.Battery, this.Body);
		link(this.Battery.getChild("BatteryEmissive"), this.Battery);
		link(this.BatteryInteractionCollider, this.Battery);
		link(this.DriverPos, this.Body);
		link(this.PassengerPos, this.Body);
		link(this.FuelIntakeInteractionCollider, this.Body);
		link(this.Exhaust, this.Bike);
		link(this.ExhaustUpper, this.Exhaust);
		link(this.ExhaustUpperSmokePoint, this.ExhaustUpper);
		link(this.ExhaustLower, this.Exhaust);
		link(this.ExhaustLowerSmokePoint, this.ExhaustLower);
		link(this.Piping, this.ExhaustLower);
		link(this.Support, this.Bike);
		link(this.Support.getChild("SupportEmissive"), this.Support);
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition Bike = partdefinition.addOrReplaceChild("Bike", CubeListBuilder.create(), PartPose.offsetAndRotation(1.25F, 16.0F, 0.0F, 0.0F, 3.1416F, 0.0F));

		PartDefinition Front = Bike.addOrReplaceChild("Front", CubeListBuilder.create(), PartPose.offset(1.25F, -11.2441F, 10.5252F));

		PartDefinition WheelFront = Front.addOrReplaceChild("WheelFront", CubeListBuilder.create().texOffs(0, 8).addBox(-1.5F, 5.2441F, -3.0F, 3.0F, 2.0F, 6.0F, new CubeDeformation(0.0F))
		.texOffs(52, -11).addBox(0.0F, -5.5059F, -5.5F, 0.0F, 11.0F, 11.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 12.0F, 9.0F));

		WheelFront.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(0, 19).addBox(-1.5F, -1.5F, -1.5F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -0.0059F, 0.0F, 0.7854F, 0.0F, 0.0F));
		WheelFront.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(0, 8).addBox(-1.5F, -2.0F, -3.0F, 3.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -7.2412F, 0.0F, 3.1416F, 0.0F, 0.0F));
		WheelFront.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, -2.0F, -3.0F, 3.0F, 2.0F, 6.0F, new CubeDeformation(-0.01F)), PartPose.offsetAndRotation(0.0F, -5.1199F, 5.1213F, 2.3562F, 0.0F, 0.0F));
		WheelFront.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(0, 8).addBox(-1.5F, -2.0F, -3.0F, 3.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0015F, 7.2426F, 1.5708F, 0.0F, 0.0F));
		WheelFront.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, -2.0F, -3.0F, 3.0F, 2.0F, 6.0F, new CubeDeformation(-0.01F)), PartPose.offsetAndRotation(0.0F, 5.1228F, 5.1213F, 0.7854F, 0.0F, 0.0F));
		WheelFront.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, -2.0F, -3.0F, 3.0F, 2.0F, 6.0F, new CubeDeformation(-0.01F)), PartPose.offsetAndRotation(0.0F, 5.1228F, -5.1213F, -0.7854F, 0.0F, 0.0F));
		WheelFront.addOrReplaceChild("cube_r7", CubeListBuilder.create().texOffs(0, 8).addBox(-1.5F, -2.0F, -3.0F, 3.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0015F, -7.2426F, -1.5708F, 0.0F, 0.0F));
		WheelFront.addOrReplaceChild("cube_r8", CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, -2.0F, -3.0F, 3.0F, 2.0F, 6.0F, new CubeDeformation(-0.01F)), PartPose.offsetAndRotation(0.0F, -5.1199F, -5.1213F, -2.3562F, 0.0F, 0.0F));

		PartDefinition WheelFrontEmissive = WheelFront.addOrReplaceChild("WheelFrontEmissive", CubeListBuilder.create().texOffs(74, -11).addBox(0.0F, -11.0F, -5.5F, 0.0F, 11.0F, 11.0F, new CubeDeformation(0.0F)), PartPose.offset(-1.0F, 5.4941F, 0.0F));
		WheelFrontEmissive.addOrReplaceChild("cube_r9", CubeListBuilder.create().texOffs(0, 44).addBox(-4.0F, -0.5F, -0.5F, 8.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.0F, -5.5F, 0.0F, 0.7854F, 0.0F, 0.0F));

		WheelFront.addOrReplaceChild("WheelFrontColliderFull", CubeListBuilder.create().texOffs(218, 0).addBox(-2.0F, -9.0F, -8.0F, 4.0F, 15.0F, 15.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 1.5F, 0.5F));
		WheelFront.addOrReplaceChild("WheelFrontColliderTireless", CubeListBuilder.create().texOffs(230, 12).addBox(-2.0F, -3.0F, -2.0F, 4.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 1.5F, 0.5F));

		PartDefinition SuspensionFront = Front.addOrReplaceChild("SuspensionFront", CubeListBuilder.create(), PartPose.offsetAndRotation(-2.5F, 11.9941F, 9.0F, -0.0873F, 0.0F, 0.0F));
		SuspensionFront.addOrReplaceChild("cube_r10", CubeListBuilder.create().texOffs(0, 32).addBox(-1.0F, -9.0F, -1.0F, 2.0F, 10.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(0, 32).mirror().addBox(4.0F, -9.0F, -1.0F, 2.0F, 10.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false)
		.texOffs(0, 50).mirror().addBox(-1.0F, -17.0F, -1.0F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false)
		.texOffs(0, 50).mirror().addBox(4.0F, -17.0F, -1.0F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.6109F, 0.0F, 0.0F));

		PartDefinition FrontBlinkerLeft = SuspensionFront.addOrReplaceChild("FrontBlinkerLeft", CubeListBuilder.create(), PartPose.offset(-2.0F, -10.649F, -7.4565F));
		FrontBlinkerLeft.addOrReplaceChild("cube_r11", CubeListBuilder.create().texOffs(0, 132).mirror().addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0873F, 0.0F, 0.0F));
		PartDefinition FrontBlinkerLeftEmissive = FrontBlinkerLeft.addOrReplaceChild("FrontBlinkerLeftEmissive", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
		FrontBlinkerLeftEmissive.addOrReplaceChild("cube_r12", CubeListBuilder.create().texOffs(14, 132).mirror().addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0873F, 0.0F, 0.0F));

		PartDefinition FrontBlinkerRight = SuspensionFront.addOrReplaceChild("FrontBlinkerRight", CubeListBuilder.create(), PartPose.offset(7.0F, -10.649F, -7.4565F));
		FrontBlinkerRight.addOrReplaceChild("cube_r13", CubeListBuilder.create().texOffs(0, 132).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0873F, 0.0F, 0.0F));
		PartDefinition FrontBlinkerRightEmissive = FrontBlinkerRight.addOrReplaceChild("FrontBlinkerRightEmissive", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
		FrontBlinkerRightEmissive.addOrReplaceChild("cube_r14", CubeListBuilder.create().texOffs(14, 132).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0873F, 0.0F, 0.0F));

		PartDefinition SuspensionFrontEmissive = SuspensionFront.addOrReplaceChild("SuspensionFrontEmissive", CubeListBuilder.create(), PartPose.offset(5.0F, 0.0F, 0.0F));
		SuspensionFrontEmissive.addOrReplaceChild("cube_r15", CubeListBuilder.create().texOffs(0, 46).mirror().addBox(-0.5F, -12.0F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false)
		.texOffs(0, 46).mirror().addBox(-5.5F, -12.0F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.6109F, 0.0F, 0.0F));

		PartDefinition CoverFront = Front.addOrReplaceChild("CoverFront", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 5.4963F, 4.8661F, 0.1309F, 0.0F, 0.0F));
		CoverFront.addOrReplaceChild("cube_r16", CubeListBuilder.create().texOffs(0, 124).addBox(-2.0F, -2.0F, -3.0F, 4.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -1.4874F, 2.3839F, 3.1416F, 0.0F, 0.0F));
		CoverFront.addOrReplaceChild("cube_r17", CubeListBuilder.create().texOffs(0, 124).addBox(-2.0F, -1.5F, -3.0F, 4.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.9874F, -2.3839F, -0.7854F, 0.0F, -3.1416F));

		PartDefinition Headlight = Front.addOrReplaceChild("Headlight", CubeListBuilder.create().texOffs(0, 57).addBox(-2.5F, -3.0F, 0.0F, 5.0F, 5.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -0.0059F, -1.0F));
		Headlight.addOrReplaceChild("HeadlightEmissive", CubeListBuilder.create().texOffs(58, 57).addBox(-2.5F, -2.5F, -3.0F, 5.0F, 5.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -0.5F, 3.0F));
		Headlight.addOrReplaceChild("HeadlightInteractionCollider", CubeListBuilder.create().texOffs(224, 0).addBox(-5.0F, -3.0F, -3.0F, 10.0F, 6.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -0.5F, 3.0F));

		PartDefinition SpeedGauge = Headlight.addOrReplaceChild("SpeedGauge", CubeListBuilder.create().texOffs(0, 68).addBox(-1.5F, -4.0F, 0.0F, 3.0F, 4.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.8727F, 0.0F, 0.0F));
		SpeedGauge.addOrReplaceChild("SpeedGaugeEmissive", CubeListBuilder.create().texOffs(44, 68).addBox(-1.5F, -4.0F, 0.0F, 3.0F, 4.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));
		PartDefinition SpeedGaugeArrow = SpeedGauge.addOrReplaceChild("SpeedGaugeArrow", CubeListBuilder.create(), PartPose.offset(0.0F, -4.1546F, 0.5372F));
		SpeedGaugeArrow.addOrReplaceChild("SpeedGaugeArrowEmissive", CubeListBuilder.create().texOffs(-2, 68).addBox(-1.0F, 0.0F, -1.0F, 1.0F, 0.0F, 2.0F, new CubeDeformation(0.01F)), PartPose.offset(0.5F, 0.0692F, 0.4839F));

		PartDefinition FuelGauge = Headlight.addOrReplaceChild("FuelGauge", CubeListBuilder.create().texOffs(12, 68).addBox(-1.5F, -3.0F, 0.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.0F, 0.0F, 0.0F, 0.8727F, 0.0F, 0.0F));
		FuelGauge.addOrReplaceChild("FuelGaugeEmissive", CubeListBuilder.create().texOffs(56, 68).addBox(-1.5F, -3.0F, 0.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));
		PartDefinition FuelGaugeArrow = FuelGauge.addOrReplaceChild("FuelGaugeArrow", CubeListBuilder.create(), PartPose.offset(-0.5F, -3.1546F, 0.5372F));
		FuelGaugeArrow.addOrReplaceChild("FuelGaugeArrowEmissive", CubeListBuilder.create().texOffs(-2, 68).addBox(-1.0F, 0.0F, -1.0F, 1.0F, 0.0F, 2.0F, new CubeDeformation(0.01F)), PartPose.offset(0.5F, 0.0692F, 0.4839F));

		PartDefinition HandleRight = Front.addOrReplaceChild("HandleRight", CubeListBuilder.create(), PartPose.offsetAndRotation(1.5F, -2.0059F, -1.0F, 0.0F, 0.3927F, 0.0F));
		HandleRight.addOrReplaceChild("cube_r18", CubeListBuilder.create().texOffs(0, 81).mirror().addBox(-1.5F, -1.0F, -0.5F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false)
		.texOffs(0, 84).addBox(-1.0F, -6.0F, 0.0F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.8284F, -3.2426F, 0.0F, 0.0F, 0.0F, 1.5708F));
		HandleRight.addOrReplaceChild("cube_r19", CubeListBuilder.create().texOffs(0, 75).addBox(-1.0F, -5.0F, 0.0F, 1.0F, 5.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.7854F));
		PartDefinition HandleRightEmissive = HandleRight.addOrReplaceChild("HandleRightEmissive", CubeListBuilder.create(), PartPose.offset(2.8284F, -3.2426F, 0.0F));
		HandleRightEmissive.addOrReplaceChild("cube_r20", CubeListBuilder.create().texOffs(0, 91).addBox(-1.0F, -5.0F, 1.5F, 1.0F, 5.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.5708F));

		PartDefinition HandleLeft = Front.addOrReplaceChild("HandleLeft", CubeListBuilder.create(), PartPose.offsetAndRotation(-1.5F, -2.0059F, -1.0F, 0.0F, -0.3927F, 0.0F));
		HandleLeft.addOrReplaceChild("cube_r21", CubeListBuilder.create().texOffs(0, 81).addBox(-0.5F, -1.0F, -0.5F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(0, 84).mirror().addBox(0.0F, -6.0F, 0.0F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-2.8284F, -3.2426F, 0.0F, 0.0F, 0.0F, -1.5708F));
		HandleLeft.addOrReplaceChild("cube_r22", CubeListBuilder.create().texOffs(0, 75).mirror().addBox(0.0F, -5.0F, 0.0F, 1.0F, 5.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.7854F));
		PartDefinition HandleLeftEmissive = HandleLeft.addOrReplaceChild("HandleLeftEmissive", CubeListBuilder.create(), PartPose.offset(-2.8284F, -3.2426F, 0.0F));
		HandleLeftEmissive.addOrReplaceChild("cube_r23", CubeListBuilder.create().texOffs(0, 91).addBox(0.0F, -5.0F, 1.5F, 1.0F, 5.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.5708F));

		PartDefinition WheelRear = Bike.addOrReplaceChild("WheelRear", CubeListBuilder.create().texOffs(0, 8).addBox(-1.5F, 5.2441F, -3.0F, 3.0F, 2.0F, 6.0F, new CubeDeformation(0.0F))
		.texOffs(52, -11).addBox(0.0F, -5.5059F, -5.5F, 0.0F, 11.0F, 11.0F, new CubeDeformation(0.0F)), PartPose.offset(1.25F, 0.7559F, -19.4748F));

		WheelRear.addOrReplaceChild("cube_r24", CubeListBuilder.create().texOffs(0, 19).addBox(-1.5F, -1.5F, -1.5F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -0.0059F, 0.0F, 0.7854F, 0.0F, 0.0F));
		WheelRear.addOrReplaceChild("cube_r25", CubeListBuilder.create().texOffs(0, 8).addBox(-1.5F, -2.0F, -3.0F, 3.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -7.2412F, 0.0F, 3.1416F, 0.0F, 0.0F));
		WheelRear.addOrReplaceChild("cube_r26", CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, -2.0F, -3.0F, 3.0F, 2.0F, 6.0F, new CubeDeformation(-0.01F)), PartPose.offsetAndRotation(0.0F, -5.1199F, 5.1213F, 2.3562F, 0.0F, 0.0F));
		WheelRear.addOrReplaceChild("cube_r27", CubeListBuilder.create().texOffs(0, 8).addBox(-1.5F, -2.0F, -3.0F, 3.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0015F, 7.2426F, 1.5708F, 0.0F, 0.0F));
		WheelRear.addOrReplaceChild("cube_r28", CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, -2.0F, -3.0F, 3.0F, 2.0F, 6.0F, new CubeDeformation(-0.01F)), PartPose.offsetAndRotation(0.0F, 5.1228F, 5.1213F, 0.7854F, 0.0F, 0.0F));
		WheelRear.addOrReplaceChild("cube_r29", CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, -2.0F, -3.0F, 3.0F, 2.0F, 6.0F, new CubeDeformation(-0.01F)), PartPose.offsetAndRotation(0.0F, 5.1228F, -5.1213F, -0.7854F, 0.0F, 0.0F));
		WheelRear.addOrReplaceChild("cube_r30", CubeListBuilder.create().texOffs(0, 8).addBox(-1.5F, -2.0F, -3.0F, 3.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0015F, -7.2426F, -1.5708F, 0.0F, 0.0F));
		WheelRear.addOrReplaceChild("cube_r31", CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, -2.0F, -3.0F, 3.0F, 2.0F, 6.0F, new CubeDeformation(-0.01F)), PartPose.offsetAndRotation(0.0F, -5.1199F, -5.1213F, -2.3562F, 0.0F, 0.0F));

		PartDefinition WheelRearEmissive = WheelRear.addOrReplaceChild("WheelRearEmissive", CubeListBuilder.create().texOffs(74, -11).addBox(0.0F, -11.0F, -5.5F, 0.0F, 11.0F, 11.0F, new CubeDeformation(0.0F)), PartPose.offset(1.0F, 5.4941F, 0.0F));
		WheelRearEmissive.addOrReplaceChild("cube_r32", CubeListBuilder.create().texOffs(0, 44).addBox(-4.0F, -0.5F, -0.5F, 8.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.0F, -5.5F, 0.0F, 0.7854F, 0.0F, 0.0F));

		WheelRear.addOrReplaceChild("WheelRearColliderFull", CubeListBuilder.create().texOffs(218, 0).addBox(-2.0F, -9.0F, -7.0F, 4.0F, 15.0F, 15.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 1.5F, -0.5F));
		WheelRear.addOrReplaceChild("WheelRearColliderTireless", CubeListBuilder.create().texOffs(230, 12).addBox(-2.0F, -3.0F, -1.0F, 4.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 1.5F, -0.5F));

		PartDefinition SuspensionRear = Bike.addOrReplaceChild("SuspensionRear", CubeListBuilder.create(), PartPose.offsetAndRotation(-1.75F, -3.6988F, -11.2427F, 0.1309F, 0.0F, 0.0F));
		SuspensionRear.addOrReplaceChild("cube_r33", CubeListBuilder.create().texOffs(20, 151).mirror().addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false)
		.texOffs(20, 163).mirror().addBox(-1.0F, -1.0F, -4.0F, 2.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false)
		.texOffs(20, 163).addBox(-7.0F, -1.0F, -4.0F, 2.0F, 2.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(20, 151).addBox(-7.0F, -1.0F, 0.0F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(6.0F, 0.0F, 0.0F, 0.4887F, 0.0F, 0.0F));

		PartDefinition SuspensionRearEmissive = SuspensionRear.addOrReplaceChild("SuspensionRearEmissive", CubeListBuilder.create(), PartPose.offset(6.0F, 0.0F, 0.0F));
		SuspensionRearEmissive.addOrReplaceChild("cube_r34", CubeListBuilder.create().texOffs(20, 154).mirror().addBox(-0.5F, -0.5F, -6.0F, 1.0F, 1.0F, 6.0F, new CubeDeformation(0.0F)).mirror(false)
		.texOffs(20, 149).mirror().addBox(-7.5F, -0.5F, 1.0F, 9.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false)
		.texOffs(20, 154).addBox(-6.5F, -0.5F, -6.0F, 1.0F, 1.0F, 6.0F, new CubeDeformation(0.0F))
		.texOffs(20, 161).addBox(-7.0F, -0.5F, -7.0F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.4887F, 0.0F, 0.0F));

		PartDefinition CoverRear = Bike.addOrReplaceChild("CoverRear", CubeListBuilder.create(), PartPose.offsetAndRotation(1.25F, -5.7478F, -15.3078F, -0.5672F, 0.0F, 0.0F));
		CoverRear.addOrReplaceChild("cube_r35", CubeListBuilder.create().texOffs(40, 103).addBox(-2.0F, -1.6512F, -3.9253F, 4.0F, 3.0F, 7.0F, new CubeDeformation(-0.02F)), PartPose.offsetAndRotation(0.0F, -1.1059F, -0.4253F, -3.1416F, 0.0F, 0.0F));
		CoverRear.addOrReplaceChild("cube_r36", CubeListBuilder.create().texOffs(22, 85).addBox(-2.0F, -1.5463F, -2.1993F, 4.0F, 3.0F, 6.0F, new CubeDeformation(-0.01F)), PartPose.offsetAndRotation(0.0F, 1.2608F, -5.1596F, -0.7854F, 0.0F, -3.1416F));
		CoverRear.addOrReplaceChild("cube_r37", CubeListBuilder.create().texOffs(26, 94).addBox(-2.025F, -1.5463F, -3.1993F, 4.05F, 3.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.8355F, 4.7343F, 0.7854F, 0.0F, -3.1416F));

		PartDefinition RearBlinkerRight = CoverRear.addOrReplaceChild("RearBlinkerRight", CubeListBuilder.create(), PartPose.offset(3.0F, 2.4951F, -5.6213F));
		RearBlinkerRight.addOrReplaceChild("cube_r38", CubeListBuilder.create().texOffs(0, 132).mirror().addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 2.5744F, 0.0F, 3.1416F));
		PartDefinition RearBlinkerRightEmissive = RearBlinkerRight.addOrReplaceChild("RearBlinkerRightEmissive", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
		RearBlinkerRightEmissive.addOrReplaceChild("cube_r39", CubeListBuilder.create().texOffs(14, 132).mirror().addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 2.5744F, 0.0F, 3.1416F));

		PartDefinition RearStopLight = CoverRear.addOrReplaceChild("RearStopLight", CubeListBuilder.create().texOffs(8, 132).mirror().addBox(-4.0F, -2.25F, 1.0F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(3.0F, 2.4951F, -5.6213F, 2.138F, 0.0F, 0.0F));
		RearStopLight.addOrReplaceChild("RearStopLightEmissive", CubeListBuilder.create().texOffs(22, 132).mirror().addBox(-1.0F, -1.0F, 0.0F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(-3.0F, -1.25F, 1.0F));

		PartDefinition RearBlinkerLeft = CoverRear.addOrReplaceChild("RearBlinkerLeft", CubeListBuilder.create(), PartPose.offset(-3.0F, 2.4951F, -5.6213F));
		RearBlinkerLeft.addOrReplaceChild("cube_r40", CubeListBuilder.create().texOffs(0, 132).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 2.5744F, 0.0F, -3.1416F));
		PartDefinition RearBlinkerLeftEmissive = RearBlinkerLeft.addOrReplaceChild("RearBlinkerLeftEmissive", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
		RearBlinkerLeftEmissive.addOrReplaceChild("cube_r41", CubeListBuilder.create().texOffs(14, 132).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 2.5744F, 0.0F, -3.1416F));

		CoverRear.addOrReplaceChild("RearLightInteractionCollider", CubeListBuilder.create().texOffs(236, 0).addBox(-3.0F, -1.0F, 0.0F, 6.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 4.2984F, -6.7471F, 2.138F, 0.0F, 0.0F));

		PartDefinition CoverChain = Bike.addOrReplaceChild("CoverChain", CubeListBuilder.create().texOffs(38, 165).addBox(0.025F, -5.0F, -21.0F, 1.0F, 6.0F, 21.0F, new CubeDeformation(0.0F))
		.texOffs(38, 195).addBox(0.025F, -2.9941F, -19.0F, 2.0F, 2.0F, 14.0F, new CubeDeformation(0.0F)), PartPose.offset(-2.25F, 2.75F, -1.4748F));
		CoverChain.addOrReplaceChild("CoverChainEmissive", CubeListBuilder.create().texOffs(0, 44).addBox(-4.0F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(60, 174).addBox(-3.0F, -2.5F, -13.5F, 0.0F, 5.0F, 18.0F, new CubeDeformation(0.0F)), PartPose.offset(3.5F, -2.0F, -6.0F));

		PartDefinition Body = Bike.addOrReplaceChild("Body", CubeListBuilder.create().texOffs(18, 0).addBox(-2.5F, -2.5F, -7.0F, 5.0F, 5.0F, 12.0F, new CubeDeformation(0.0F)), PartPose.offset(1.25F, 2.0F, 1.5252F));
		Body.addOrReplaceChild("cube_r42", CubeListBuilder.create().texOffs(40, 57).addBox(-2.0F, 0.0F, -3.0F, 4.0F, 4.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -14.25F, 8.0F, 0.3927F, 0.0F, 0.0F));
		Body.addOrReplaceChild("cube_r43", CubeListBuilder.create().texOffs(0, 136).addBox(-0.5F, -4.5F, -0.5F, 1.0F, 5.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(0, 142).addBox(0.5F, -4.5F, -0.5F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(0, 136).mirror().addBox(-7.5F, -4.5F, -0.5F, 1.0F, 5.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false)
		.texOffs(0, 142).mirror().addBox(-10.5F, -4.5F, -0.5F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(3.5F, 1.2296F, 6.0538F, 1.7453F, 0.0F, 0.0F));
		Body.addOrReplaceChild("cube_r44", CubeListBuilder.create().texOffs(0, 97).addBox(-1.5F, -2.0F, -14.0F, 6.0F, 2.0F, 14.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.5F, -9.7492F, 9.5132F, 1.309F, 0.0F, 0.0F));
		Body.addOrReplaceChild("cube_r45", CubeListBuilder.create().texOffs(20, 122).addBox(-2.525F, -2.0F, -2.0F, 5.05F, 4.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -9.773F, -13.0935F, 0.9163F, 0.0F, 0.0F));
		Body.addOrReplaceChild("cube_r46", CubeListBuilder.create().texOffs(20, 128).addBox(-2.0F, -2.0F, -4.5F, 4.0F, 3.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -7.4263F, -9.1257F, 0.5672F, 0.0F, 0.0F));
		Body.addOrReplaceChild("cube_r47", CubeListBuilder.create().texOffs(20, 113).addBox(-2.5F, -1.0F, -6.0F, 5.0F, 2.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -9.8301F, -7.011F, 0.1745F, 0.0F, 0.0F));
		Body.addOrReplaceChild("cube_r48", CubeListBuilder.create().texOffs(18, 40).addBox(-3.0F, 0.0F, -10.0F, 6.0F, 4.0F, 10.0F, new CubeDeformation(-0.01F)), PartPose.offsetAndRotation(0.0F, -15.6983F, 2.6241F, 0.3054F, 0.0F, 0.0F));
		Body.addOrReplaceChild("cube_r49", CubeListBuilder.create().texOffs(18, 54).addBox(-3.0F, 1.5F, -6.5F, 6.0F, 2.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(18, 28).addBox(-3.0F, -2.5F, -3.5F, 6.0F, 6.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -12.5F, 5.5F, -0.2182F, 0.0F, 0.0F));
		Body.addOrReplaceChild("cube_r50", CubeListBuilder.create().texOffs(38, 211).addBox(-2.5F, 0.0F, 0.0F, 5.0F, 7.0F, 5.0F, new CubeDeformation(-0.01F)), PartPose.offsetAndRotation(0.0F, -3.0F, -7.0F, -1.2654F, 0.0F, 0.0F));

		PartDefinition Engine = Body.addOrReplaceChild("Engine", CubeListBuilder.create().texOffs(0, 242).addBox(0.0F, 6.5F, -2.0F, 2.0F, 4.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(0, 242).addBox(5.0F, 6.5F, -5.0F, 2.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(
				(float) DusterbikeModelBones.ENGINE_X,
				(float) DusterbikeModelBones.ENGINE_Y,
				(float) DusterbikeModelBones.ENGINE_Z));

		Engine.addOrReplaceChild("cube_r51", CubeListBuilder.create().texOffs(18, 23).addBox(2.0F, 2.0F, -1.5F, 1.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.5F, 8.5F, -3.0F, 1.5708F, 0.0F, 0.0F));
		Engine.addOrReplaceChild("cube_r52", CubeListBuilder.create().texOffs(22, 17).addBox(0.5F, 3.5F, -0.5F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(18, 17).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 5.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 5.5F, 0.0F, 1.2654F, 0.0F, 0.0F));
		Engine.addOrReplaceChild("cube_r53", CubeListBuilder.create().texOffs(22, 59).addBox(0.0F, -0.5F, 0.0F, 1.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.5F, 4.5F, 0.0F, 1.5708F, 0.0F, 0.7854F));
		Engine.addOrReplaceChild("cube_r54", CubeListBuilder.create().texOffs(22, 73).addBox(-1.5F, -1.5F, -3.5F, 3.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.5F, 0.0F, 0.0F, 2.3562F, 0.0F, 0.0F));
		Engine.addOrReplaceChild("cube_r55", CubeListBuilder.create().texOffs(22, 73).addBox(-1.5F, -1.5F, -3.5F, 3.0F, 3.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(22, 65).addBox(-3.0F, -1.5F, -1.5F, 6.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.5F, 0.0F, 0.0F, 0.7854F, 0.0F, 0.0F));
		Engine.addOrReplaceChild("cube_r56", CubeListBuilder.create().texOffs(18, 23).addBox(-0.5F, -1.0F, -1.5F, 1.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.0F, 5.5F, 0.0F, 3.1416F, 0.0F, 0.0F));

		PartDefinition PistonRear = Engine.addOrReplaceChild("PistonRear", CubeListBuilder.create(), PartPose.offset(3.5F, 8.5F, 0.0F));
		PistonRear.addOrReplaceChild("cube_r57", CubeListBuilder.create().texOffs(0, 158).addBox(-2.5F, -10.0F, -2.0F, 5.0F, 10.0F, 4.0F, new CubeDeformation(-0.06F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.5236F, 0.0F, 0.0F));
		PartDefinition PistonRearEmissive = PistonRear.addOrReplaceChild("PistonRearEmissive", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
		PistonRearEmissive.addOrReplaceChild("cube_r58", CubeListBuilder.create().texOffs(40, 158).addBox(-2.5F, -10.0F, -2.0F, 5.0F, 10.0F, 4.0F, new CubeDeformation(-0.06F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.5236F, 0.0F, 0.0F));

		PartDefinition PistonFront = Engine.addOrReplaceChild("PistonFront", CubeListBuilder.create(), PartPose.offset(3.5F, 8.5F, 0.0F));
		PistonFront.addOrReplaceChild("cube_r59", CubeListBuilder.create().texOffs(0, 144).addBox(-2.5F, -10.0F, -2.0F, 5.0F, 10.0F, 4.0F, new CubeDeformation(-0.05F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.5236F, 0.0F, 0.0F));
		PartDefinition PistonFrontEmissive = PistonFront.addOrReplaceChild("PistonFrontEmissive", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
		PistonFrontEmissive.addOrReplaceChild("cube_r60", CubeListBuilder.create().texOffs(40, 144).addBox(-2.5F, -10.0F, -2.0F, 5.0F, 10.0F, 4.0F, new CubeDeformation(-0.05F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.5236F, 0.0F, 0.0F));

		PartDefinition Key = Engine.addOrReplaceChild("Key", CubeListBuilder.create(), PartPose.offset(
				(float) DusterbikeModelBones.KEY_X,
				(float) DusterbikeModelBones.KEY_Y,
				(float) DusterbikeModelBones.KEY_Z));

		PartDefinition KeyEmissive = Key.addOrReplaceChild("KeyEmissive", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
		KeyEmissive.addOrReplaceChild("cube_r61", CubeListBuilder.create().texOffs(20, 71).addBox(-0.5F, 0.0F, -1.5F, 1.0F, 0.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 1.5708F, 0.0F, 0.0F));

		Key.addOrReplaceChild("KeyInteractionCollider", CubeListBuilder.create().texOffs(248, 0).addBox(
				(float) -DusterbikeModelBones.KEY_INTERACTION_COLLIDER_HALF_X,
				(float) -DusterbikeModelBones.KEY_INTERACTION_COLLIDER_HALF_Y,
				(float) -DusterbikeModelBones.KEY_INTERACTION_COLLIDER_HALF_Z,
				(float) (DusterbikeModelBones.KEY_INTERACTION_COLLIDER_HALF_X * 2.0D),
				(float) (DusterbikeModelBones.KEY_INTERACTION_COLLIDER_HALF_Y * 2.0D),
				(float) (DusterbikeModelBones.KEY_INTERACTION_COLLIDER_HALF_Z * 2.0D),
				new CubeDeformation(0.0F)), PartPose.offset(
				(float) DusterbikeModelBones.KEY_INTERACTION_COLLIDER_X,
				(float) DusterbikeModelBones.KEY_INTERACTION_COLLIDER_Y,
				(float) DusterbikeModelBones.KEY_INTERACTION_COLLIDER_Z));

		Engine.addOrReplaceChild("EngineInteractionCollider", CubeListBuilder.create().texOffs(220, 0).addBox(0.5F, -1.0F, -6.0F, 6.0F, 10.0F, 12.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition Battery = Body.addOrReplaceChild("Battery", CubeListBuilder.create(), PartPose.offset(0.0F, -7.4263F, -9.1257F));
		Battery.addOrReplaceChild("cube_r62", CubeListBuilder.create().texOffs(20, 139).addBox(-2.0F, 1.0F, -4.5F, 4.0F, 4.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.5672F, 0.0F, 0.0F));
		PartDefinition BatteryEmissive = Battery.addOrReplaceChild("BatteryEmissive", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
		BatteryEmissive.addOrReplaceChild("cube_r63", CubeListBuilder.create().texOffs(58, 139).addBox(-2.0F, 1.0F, -4.5F, 4.0F, 4.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.5672F, 0.0F, 0.0F));
		Battery.addOrReplaceChild("BatteryInteractionCollider", CubeListBuilder.create().texOffs(230, 0).addBox(-3.0F, 0.0F, -3.0F, 6.0F, 4.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		Body.addOrReplaceChild("DriverPos", CubeListBuilder.create(), PartPose.offset(0.0F, -10.8301F, -9.011F));
		Body.addOrReplaceChild("PassengerPos", CubeListBuilder.create(), PartPose.offset(0.0F, -10.8301F, -14.011F));
		Body.addOrReplaceChild("FuelIntakeInteractionCollider", CubeListBuilder.create().texOffs(240, 0).addBox(-2.0F, -4.0F, -4.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -12.5F, 5.5F));

		PartDefinition Exhaust = Bike.addOrReplaceChild("Exhaust", CubeListBuilder.create(), PartPose.offset(5.25F, 1.5F, -7.4748F));
		PartDefinition ExhaustUpper = Exhaust.addOrReplaceChild("ExhaustUpper", CubeListBuilder.create().texOffs(0, 172).addBox(-2.5F, -2.5F, -21.0F, 3.0F, 3.0F, 16.0F, new CubeDeformation(0.0F))
		.texOffs(0, 191).addBox(-2.0F, -2.0F, -5.0F, 2.0F, 2.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));
		ExhaustUpper.addOrReplaceChild("cube_r64", CubeListBuilder.create().texOffs(0, 232).addBox(-2.0F, -2.0F, 0.0F, 2.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.6848F, -0.2748F, -0.218F));
		ExhaustUpper.addOrReplaceChild("ExhaustUpperSmokePoint", CubeListBuilder.create(), PartPose.offset(-1.0F, -1.0F, -21.0F));

		PartDefinition ExhaustLower = Exhaust.addOrReplaceChild("ExhaustLower", CubeListBuilder.create().texOffs(0, 205).addBox(-1.0F, -1.274F, 24.312F, 2.0F, 2.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(0, 191).addBox(-1.0F, -2.5F, 15.0F, 2.0F, 2.0F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(0, 172).addBox(-1.5F, -3.0F, -1.0F, 3.0F, 3.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offset(-1.0F, 3.5F, -20.0F));
		ExhaustLower.addOrReplaceChild("cube_r65", CubeListBuilder.create().texOffs(0, 198).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 2.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -2.5F, 20.0F, -0.2618F, 0.0F, 0.0F));
		ExhaustLower.addOrReplaceChild("ExhaustLowerSmokePoint", CubeListBuilder.create(), PartPose.offset(0.0F, -1.5F, -1.0F));

		PartDefinition Piping = ExhaustLower.addOrReplaceChild("Piping", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 0.726F, 27.312F, 0.0873F, 0.0F, 0.0F));
		Piping.addOrReplaceChild("cube_r66", CubeListBuilder.create().texOffs(0, 210).addBox(-1.0F, -2.0F, 0.0F, 2.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.5672F, 0.0F, 0.0F));
		Piping.addOrReplaceChild("cube_r67", CubeListBuilder.create().texOffs(0, 218).addBox(-2.0F, -2.0F, 1.0F, 2.0F, 2.0F, 6.0F, new CubeDeformation(0.0F))
		.texOffs(0, 226).addBox(-4.0F, -4.0F, 5.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.2588F, -2.7048F, 4.2457F, 0.5831F, -0.2201F, -0.143F));

		PartDefinition Support = Bike.addOrReplaceChild("Support", CubeListBuilder.create(), PartPose.offsetAndRotation(-1.25F, 4.5F, -2.9748F, -1.5708F, 0.0F, 0.0F));
		Support.addOrReplaceChild("cube_r68", CubeListBuilder.create().texOffs(22, 83).addBox(-3.5F, -2.0F, -3.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.5F, -2.5F, 1.5F, 1.5708F, 0.0F, 0.0F));
		PartDefinition SupportEmissive = Support.addOrReplaceChild("SupportEmissive", CubeListBuilder.create(), PartPose.offset(-1.0F, 0.5F, 0.0F));
		SupportEmissive.addOrReplaceChild("cube_r69", CubeListBuilder.create().texOffs(22, 78).addBox(-1.0761F, -0.5F, -3.6173F, 2.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 1.5708F, 0.0F, 0.3927F));

		return LayerDefinition.create(meshdefinition, 256, 256);
	}

	public record ExhaustTips(Vec3 upper, Vec3 lower) {}

	@Override
	public void setupAnim(DusterbikeEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		this.Bike.resetPose();

		float partialTick = ageInTicks - entity.tickCount;
		float steerAngle = entity.getRenderSteer(partialTick);
		this.Front.yRot = steerAngle * Mth.DEG_TO_RAD;

		float frontRotation = entity.getFrontWheelRotation(partialTick);
		float rearRotation = entity.getRearWheelRotation(partialTick);
		this.WheelFront.xRot = frontRotation;
		this.WheelRear.xRot = rearRotation;

		if (ClientForgeEvents.isKeyCrankVisualActive(entity)) {
			this.Key.xRot = DusterbikeTransforms.KEY_CRANK_ANGLE_DEGREES * Mth.DEG_TO_RAD;
		} else {
			this.Key.xRot = 0.0F;
		}

		if (entity.getPassengers().isEmpty()) {
			this.Support.xRot = SUPPORT_DEPLOYED_X_ROT;
		} else {
			this.Support.xRot = SUPPORT_MOUNTED_X_ROT;
		}

		float speedArrowDegrees = DusterbikePistonShakeManager.getSpeedGaugeArrowDegrees(entity, partialTick);
		this.SpeedGaugeArrow.yRot = speedArrowDegrees * Mth.DEG_TO_RAD;

		animatePistons(entity, ageInTicks);
	}

	private void animatePistons(DusterbikeEntity entity, float ageInTicks) {
		this.PistonRear.resetPose();
		this.PistonFront.resetPose();

		float intensity = DusterbikePistonShakeManager.getShakeIntensity(entity);
		if (intensity <= 0.0F) {
			return;
		}

		applyPistonShake(this.PistonRear, entity, intensity, 0, ageInTicks);
		applyPistonShake(this.PistonFront, entity, intensity, 1, ageInTicks);
	}

	private static void applyPistonShake(
			ModelPart piston,
			DusterbikeEntity entity,
			float intensity,
			int pistonIndex,
			float ageInTicks) {
		float amplitude = DusterbikePistonShakeConstants.MAX_OFFSET_PIXELS * intensity;
		piston.x += shakeOffset(entity.getId(), pistonIndex, 0, ageInTicks, amplitude);
		piston.y += shakeOffset(entity.getId(), pistonIndex, 1, ageInTicks, amplitude);
		piston.z += shakeOffset(entity.getId(), pistonIndex, 2, ageInTicks, amplitude);
	}

	private static float shakeOffset(int entityId, int pistonIndex, int axis, float ageInTicks, float amplitude) {
		int tick = Mth.floor(ageInTicks);
		float partial = ageInTicks - tick;
		float current = randomSigned(entityId, pistonIndex, axis, tick);
		float next = randomSigned(entityId, pistonIndex, axis, tick + 1);
		float blended = Mth.lerp(partial, current, next);
		float wave = Mth.sin(ageInTicks * (1.9F + axis * 0.41F + pistonIndex * 0.67F)) * 0.35F;
		return (blended * 0.65F + wave) * amplitude;
	}

	private static float randomSigned(int entityId, int pistonIndex, int axis, int tick) {
		int hash = entityId * 31 + pistonIndex * 17 + axis * 13 + tick * 1013;
		hash ^= hash << 13;
		hash ^= hash >>> 17;
		hash ^= hash << 5;
		return (Math.floorMod(hash, 1000) / 500.0F) - 1.0F;
	}

	public ExhaustTips computeExhaustTipEntityLocals(DusterbikeEntity entity, float partialTick) {
		return new ExhaustTips(
				DusterbikeModelBones.deriveExhaustUpperSmokeEntityLocal(),
				DusterbikeModelBones.deriveExhaustLowerSmokeEntityLocal());
	}

	public void setDebugPartsVisible(boolean visible) {
		WheelFrontColliderFull.visible = visible;
		WheelFrontColliderTireless.visible = visible;
		WheelRearColliderFull.visible = visible;
		WheelRearColliderTireless.visible = visible;
		HeadlightInteractionCollider.visible = visible;
		RearLightInteractionCollider.visible = visible;
		KeyInteractionCollider.visible = visible;
		EngineInteractionCollider.visible = visible;
		BatteryInteractionCollider.visible = visible;
		FuelIntakeInteractionCollider.visible = visible;
	}

	private void setEmissivePartsVisible(boolean visible) {
		for (ModelPart emissivePart : this.emissiveParts) {
			emissivePart.visible = visible;
		}
	}

	public void renderEmissive(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay) {
		for (ModelPart emissivePart : this.emissiveParts) {
			renderEmissivePart(emissivePart, poseStack, vertexConsumer, packedLight, packedOverlay);
		}
	}

	private void renderEmissivePart(
			ModelPart emissivePart,
			PoseStack poseStack,
			VertexConsumer vertexConsumer,
			int packedLight,
			int packedOverlay) {
		List<ModelPart> ancestors = new ArrayList<>();
		ModelPart current = this.parentMap.get(emissivePart);
		while (current != null) {
			ancestors.add(0, current);
			current = this.parentMap.get(current);
		}

		poseStack.pushPose();
		for (ModelPart ancestor : ancestors) {
			ancestor.translateAndRotate(poseStack);
		}

		emissivePart.render(poseStack, vertexConsumer, packedLight, packedOverlay);
		poseStack.popPose();
	}

	public void renderBody(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay) {
		boolean frontFull = WheelFrontColliderFull.visible;
		boolean frontTireless = WheelFrontColliderTireless.visible;
		boolean rearFull = WheelRearColliderFull.visible;
		boolean rearTireless = WheelRearColliderTireless.visible;
		boolean headlight = HeadlightInteractionCollider.visible;
		boolean rearLight = RearLightInteractionCollider.visible;
		boolean key = KeyInteractionCollider.visible;
		boolean engine = EngineInteractionCollider.visible;
		boolean battery = BatteryInteractionCollider.visible;
		boolean fuel = FuelIntakeInteractionCollider.visible;

		setDebugPartsVisible(false);
		setEmissivePartsVisible(false);
		Bike.render(poseStack, vertexConsumer, packedLight, packedOverlay, 1.0F, 1.0F, 1.0F, 1.0F);
		setEmissivePartsVisible(true);

		WheelFrontColliderFull.visible = frontFull;
		WheelFrontColliderTireless.visible = frontTireless;
		WheelRearColliderFull.visible = rearFull;
		WheelRearColliderTireless.visible = rearTireless;
		HeadlightInteractionCollider.visible = headlight;
		RearLightInteractionCollider.visible = rearLight;
		KeyInteractionCollider.visible = key;
		EngineInteractionCollider.visible = engine;
		BatteryInteractionCollider.visible = battery;
		FuelIntakeInteractionCollider.visible = fuel;
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		renderBody(poseStack, vertexConsumer, packedLight, packedOverlay);
	}
}
