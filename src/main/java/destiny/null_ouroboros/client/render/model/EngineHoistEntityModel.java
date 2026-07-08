package destiny.null_ouroboros.client.render.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.server.entity.EngineHoistEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;

public class EngineHoistEntityModel extends HierarchicalModel<EngineHoistEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "engine_hoist"), "main");
    private final ModelPart All;
    private final ModelPart Emissive;
    private final ModelPart Wheel2;
    private final ModelPart Wheel2Emissive;
    private final ModelPart Wheel1;
    private final ModelPart Wheel1Emissive;
    private final ModelPart Wheel3;
    private final ModelPart Wheel3Emissive;
    private final ModelPart Wheel4;
    private final ModelPart Wheel4Emissive;
    private final ModelPart Wheel5;
    private final ModelPart Wheel5Emissive;
    private final ModelPart Wheel6;
    private final ModelPart Wheel6Emissive;
    private final ModelPart CranePiston;
    private final ModelPart CranePistonExtend;
    private final ModelPart CranePistonEmissive;
    private final ModelPart Crane;
    private final ModelPart CraneEmissive;
    private final ModelPart Hoist;
    private final ModelPart HoistLower;
    private final ModelPart HoistLowerEmissive;
    private final ModelPart Engine;
    private final ModelPart PistonRear;
    private final ModelPart PistonRearEmissive;
    private final ModelPart SparkPlugRear;
    private final ModelPart SparkPlugRearEmissive;
    private final ModelPart PistonFront;
    private final ModelPart PistonFrontEmissive;
    private final ModelPart SparkPlugFront;
    private final ModelPart SparkPlugFrontEmissive;
    private final ModelPart Key;
    private final ModelPart KeyEmissive;

    public EngineHoistEntityModel(ModelPart root) {
        this.All = root.getChild("All");
        this.Emissive = this.All.getChild("Emissive");
        this.Wheel2 = this.All.getChild("Wheel2");
        this.Wheel2Emissive = this.Wheel2.getChild("Wheel2Emissive");
        this.Wheel1 = this.All.getChild("Wheel1");
        this.Wheel1Emissive = this.Wheel1.getChild("Wheel1Emissive");
        this.Wheel3 = this.All.getChild("Wheel3");
        this.Wheel3Emissive = this.Wheel3.getChild("Wheel3Emissive");
        this.Wheel4 = this.All.getChild("Wheel4");
        this.Wheel4Emissive = this.Wheel4.getChild("Wheel4Emissive");
        this.Wheel5 = this.All.getChild("Wheel5");
        this.Wheel5Emissive = this.Wheel5.getChild("Wheel5Emissive");
        this.Wheel6 = this.All.getChild("Wheel6");
        this.Wheel6Emissive = this.Wheel6.getChild("Wheel6Emissive");
        this.CranePiston = this.All.getChild("CranePiston");
        this.CranePistonExtend = this.CranePiston.getChild("CranePistonExtend");
        this.CranePistonEmissive = this.CranePistonExtend.getChild("CranePistonEmissive");
        this.Crane = this.All.getChild("Crane");
        this.CraneEmissive = this.Crane.getChild("CraneEmissive");
        this.Hoist = this.Crane.getChild("Hoist");
        this.HoistLower = this.Hoist.getChild("HoistLower");
        this.HoistLowerEmissive = this.HoistLower.getChild("HoistLowerEmissive");
        this.Engine = this.HoistLower.getChild("Engine");
        this.PistonRear = this.Engine.getChild("PistonRear");
        this.PistonRearEmissive = this.PistonRear.getChild("PistonRearEmissive");
        this.SparkPlugRear = this.PistonRear.getChild("SparkPlugRear");
        this.SparkPlugRearEmissive = this.SparkPlugRear.getChild("SparkPlugRearEmissive");
        this.PistonFront = this.Engine.getChild("PistonFront");
        this.PistonFrontEmissive = this.PistonFront.getChild("PistonFrontEmissive");
        this.SparkPlugFront = this.PistonFront.getChild("SparkPlugFront");
        this.SparkPlugFrontEmissive = this.SparkPlugFront.getChild("SparkPlugFrontEmissive");
        this.Key = this.Engine.getChild("Key");
        this.KeyEmissive = this.Key.getChild("KeyEmissive");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition All = partdefinition.addOrReplaceChild("All", CubeListBuilder.create().texOffs(58, 15).addBox(-5.0F, -1.0F, 0.0F, 16.0F, 2.0F, 2.0F, new CubeDeformation(0.01F))
                .texOffs(87, 0).addBox(2.0F, -24.6646F, -1.8565F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(79, 2).addBox(2.0F, -26.6646F, -1.8565F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(58, 11).addBox(-2.0F, -1.0F, -8.0F, 10.0F, 2.0F, 2.0F, new CubeDeformation(-0.01F)), PartPose.offset(-3.0F, 21.0F, 14.0F));

        PartDefinition cube_r1 = All.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(60, 30).mirror().addBox(-2.0F, -1.0F, -32.0F, 2.0F, 2.0F, 32.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.1745F, 0.0F));

        PartDefinition cube_r2 = All.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(60, 30).addBox(0.0F, -1.0F, -32.0F, 2.0F, 2.0F, 32.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(6.0F, 0.0F, 0.0F, 0.0F, -0.1745F, 0.0F));

        PartDefinition cube_r3 = All.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(120, 30).addBox(-1.0F, -24.0F, 0.0F, 2.0F, 24.0F, 2.0F, new CubeDeformation(-0.01F)), PartPose.offsetAndRotation(3.0F, -1.0F, -8.0F, -0.2618F, 0.0F, 0.0F));

        PartDefinition Emissive = All.addOrReplaceChild("Emissive", CubeListBuilder.create(), PartPose.offset(8.5F, 0.0F, -1.5F));

        PartDefinition cube_r4 = Emissive.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(116, 30).mirror().addBox(-0.5F, -22.0F, -0.5F, 1.0F, 22.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-11.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.2182F));

        PartDefinition cube_r5 = Emissive.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(116, 30).addBox(-0.5F, -22.0F, -0.5F, 1.0F, 22.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.2182F));

        PartDefinition Wheel2 = All.addOrReplaceChild("Wheel2", CubeListBuilder.create().texOffs(106, 0).mirror().addBox(-0.5F, -0.5F, -1.5F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.01F)).mirror(false), PartPose.offset(-5.8755F, 1.5F, -27.9422F));

        PartDefinition Wheel2Emissive = Wheel2.addOrReplaceChild("Wheel2Emissive", CubeListBuilder.create().texOffs(100, 0).mirror().addBox(-0.5F, -0.5F, -1.0F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(0.0F, 0.0F, -1.5F));

        PartDefinition Wheel1 = All.addOrReplaceChild("Wheel1", CubeListBuilder.create().texOffs(106, 0).addBox(-0.5F, -0.5F, -1.5F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.01F)), PartPose.offset(11.8755F, 1.5F, -27.9422F));

        PartDefinition Wheel1Emissive = Wheel1.addOrReplaceChild("Wheel1Emissive", CubeListBuilder.create().texOffs(100, 0).addBox(-0.5F, -0.5F, -1.0F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, -1.5F));

        PartDefinition Wheel3 = All.addOrReplaceChild("Wheel3", CubeListBuilder.create().texOffs(106, 0).addBox(-0.49F, -0.51F, -1.49F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.01F)), PartPose.offsetAndRotation(9.99F, 1.51F, 0.99F, 0.0F, -2.0944F, 0.0F));

        PartDefinition Wheel3Emissive = Wheel3.addOrReplaceChild("Wheel3Emissive", CubeListBuilder.create().texOffs(100, 0).addBox(-0.49F, -0.51F, -0.99F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, -1.5F));

        PartDefinition Wheel4 = All.addOrReplaceChild("Wheel4", CubeListBuilder.create().texOffs(106, 0).mirror().addBox(-0.51F, -0.51F, -1.49F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.01F)).mirror(false), PartPose.offsetAndRotation(-3.99F, 1.51F, 0.99F, 0.0F, 2.0944F, 0.0F));

        PartDefinition Wheel4Emissive = Wheel4.addOrReplaceChild("Wheel4Emissive", CubeListBuilder.create().texOffs(100, 0).mirror().addBox(-0.51F, -0.51F, -0.99F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(0.0F, 0.0F, -1.5F));

        PartDefinition Wheel5 = All.addOrReplaceChild("Wheel5", CubeListBuilder.create().texOffs(106, 0).mirror().addBox(-0.51F, -0.76F, -1.49F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.01F)).mirror(false), PartPose.offsetAndRotation(-2.49F, 1.76F, -8.01F, 0.0F, 1.3963F, 0.0F));

        PartDefinition Wheel5Emissive = Wheel5.addOrReplaceChild("Wheel5Emissive", CubeListBuilder.create().texOffs(100, 0).mirror().addBox(-0.51F, -0.51F, -0.99F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(0.0F, -0.25F, -1.5F));

        PartDefinition Wheel6 = All.addOrReplaceChild("Wheel6", CubeListBuilder.create().texOffs(106, 0).addBox(-0.49F, -0.51F, -1.49F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.01F)), PartPose.offsetAndRotation(8.49F, 1.51F, -8.01F, 0.0F, -1.3963F, 0.0F));

        PartDefinition Wheel6Emissive = Wheel6.addOrReplaceChild("Wheel6Emissive", CubeListBuilder.create().texOffs(100, 0).addBox(-0.49F, -0.51F, -0.99F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, -1.5F));

        PartDefinition CranePiston = All.addOrReplaceChild("CranePiston", CubeListBuilder.create().texOffs(100, 4).addBox(-1.5F, 0.25F, -0.5F, 2.0F, 10.0F, 2.0F, new CubeDeformation(0.01F)), PartPose.offset(3.5F, -16.25F, -7.5F));

        PartDefinition CranePistonExtend = CranePiston.addOrReplaceChild("CranePistonExtend", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition CranePistonEmissive = CranePistonExtend.addOrReplaceChild("CranePistonEmissive", CubeListBuilder.create().texOffs(112, 0).addBox(-1.0F, -11.0F, 0.0F, 1.0F, 15.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition Crane = All.addOrReplaceChild("Crane", CubeListBuilder.create().texOffs(68, 0).addBox(-1.0F, -3.5F, -27.0F, 2.0F, 2.0F, 28.0F, new CubeDeformation(0.0F))
                .texOffs(71, 0).addBox(-1.0F, -1.5F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.01F)), PartPose.offset(3.0F, -25.6646F, -0.8565F));

        PartDefinition CraneEmissive = Crane.addOrReplaceChild("CraneEmissive", CubeListBuilder.create().texOffs(79, 0).addBox(-2.0F, -23.0F, 1.0F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(68, 64).addBox(-1.5F, -26.0F, -25.5F, 2.0F, 2.0F, 28.0F, new CubeDeformation(0.0F)), PartPose.offset(0.5F, 22.5F, -1.5F));

        PartDefinition Hoist = Crane.addOrReplaceChild("Hoist", CubeListBuilder.create(), PartPose.offset(0.0F, -1.5F, -25.0F));

        PartDefinition cube_r6 = Hoist.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(58, -2).addBox(0.0F, -2.5F, -1.0F, 0.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 2.5F, 0.0F, 0.0F, -0.7854F, 0.0F));

        PartDefinition cube_r7 = Hoist.addOrReplaceChild("cube_r7", CubeListBuilder.create().texOffs(58, -2).addBox(0.0F, -2.5F, -1.0F, 0.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 2.5F, 0.0F, 0.0F, 0.7854F, 0.0F));

        PartDefinition HoistLower = Hoist.addOrReplaceChild("HoistLower", CubeListBuilder.create().texOffs(58, 0).addBox(-1.0F, 0.0F, -2.0F, 2.0F, 2.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 5.0F, 0.0F));

        PartDefinition HoistLowerEmissive = HoistLower.addOrReplaceChild("HoistLowerEmissive", CubeListBuilder.create(), PartPose.offset(0.0F, 7.5F, 2.5F));

        PartDefinition cube_r8 = HoistLowerEmissive.addOrReplaceChild("cube_r8", CubeListBuilder.create().texOffs(32, -13).addBox(0.0F, -6.5F, -6.5F, 0.0F, 14.0F, 13.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.7854F, 0.0F));

        PartDefinition cube_r9 = HoistLowerEmissive.addOrReplaceChild("cube_r9", CubeListBuilder.create().texOffs(32, -13).addBox(0.0F, -6.5F, -6.5F, 0.0F, 14.0F, 13.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -2.3562F, 0.0F));

        PartDefinition Engine = HoistLower.addOrReplaceChild("Engine", CubeListBuilder.create().texOffs(18, 0).addBox(-3.5F, 6.5F, -2.0F, 2.0F, 4.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(18, 0).addBox(1.5F, 6.5F, -5.0F, 2.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 4.6646F, 2.3817F, 0.0F, 3.1416F, 0.0F));

        PartDefinition cube_r10 = Engine.addOrReplaceChild("cube_r10", CubeListBuilder.create().texOffs(23, 25).addBox(2.0F, 2.0F, -1.5F, 1.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 8.5F, -3.0F, 1.5708F, 0.0F, 0.0F));

        PartDefinition cube_r11 = Engine.addOrReplaceChild("cube_r11", CubeListBuilder.create().texOffs(27, 19).addBox(0.5F, 3.5F, -0.5F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(23, 19).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 5.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.5F, 5.5F, 0.0F, 1.2654F, 0.0F, 0.0F));

        PartDefinition cube_r12 = Engine.addOrReplaceChild("cube_r12", CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, -0.5F, 0.0F, 1.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.0F, 4.5F, 0.0F, 1.5708F, 0.0F, 0.7854F));

        PartDefinition cube_r13 = Engine.addOrReplaceChild("cube_r13", CubeListBuilder.create().texOffs(0, 14).addBox(-1.5F, -1.5F, -3.5F, 3.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 2.3562F, 0.0F, 0.0F));

        PartDefinition cube_r14 = Engine.addOrReplaceChild("cube_r14", CubeListBuilder.create().texOffs(0, 14).addBox(-1.5F, -1.5F, -3.5F, 3.0F, 3.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(0, 6).addBox(-3.0F, -1.5F, -1.5F, 6.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.7854F, 0.0F, 0.0F));

        PartDefinition cube_r15 = Engine.addOrReplaceChild("cube_r15", CubeListBuilder.create().texOffs(23, 25).addBox(-0.5F, -1.0F, -1.5F, 1.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.5F, 5.5F, 0.0F, 3.1416F, 0.0F, 0.0F));

        PartDefinition PistonRear = Engine.addOrReplaceChild("PistonRear", CubeListBuilder.create(), PartPose.offset(0.0F, 8.5F, 5.0F));

        PartDefinition cube_r16 = PistonRear.addOrReplaceChild("cube_r16", CubeListBuilder.create().texOffs(0, 33).addBox(-2.5F, -10.0F, -2.0F, 5.0F, 10.0F, 4.0F, new CubeDeformation(-0.06F)), PartPose.offsetAndRotation(0.0F, 0.0F, -5.0F, 0.5236F, 0.0F, 0.0F));

        PartDefinition PistonRearEmissive = PistonRear.addOrReplaceChild("PistonRearEmissive", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition cube_r17 = PistonRearEmissive.addOrReplaceChild("cube_r17", CubeListBuilder.create().texOffs(40, 33).addBox(-2.5F, -10.0F, -2.0F, 5.0F, 10.0F, 4.0F, new CubeDeformation(-0.06F)), PartPose.offsetAndRotation(0.0F, 0.0F, -5.0F, 0.5236F, 0.0F, 0.0F));

        PartDefinition SparkPlugRear = PistonRear.addOrReplaceChild("SparkPlugRear", CubeListBuilder.create().texOffs(8, 64).addBox(-0.5F, 4.875F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(12, 64).addBox(-0.5F, -5.125F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(0, 75).addBox(-1.5F, -2.125F, -1.5F, 3.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -2.875F, -9.5252F, -1.9635F, 0.0F, 0.0F));

        PartDefinition SparkPlugRearEmissive = SparkPlugRear.addOrReplaceChild("SparkPlugRearEmissive", CubeListBuilder.create().texOffs(0, 64).addBox(-1.0F, -4.5F, -1.0F, 2.0F, 9.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.375F, 0.0F));

        PartDefinition PistonFront = Engine.addOrReplaceChild("PistonFront", CubeListBuilder.create(), PartPose.offset(0.0F, 8.5F, 5.0F));

        PartDefinition cube_r18 = PistonFront.addOrReplaceChild("cube_r18", CubeListBuilder.create().texOffs(0, 19).addBox(-2.5F, -10.0F, -2.0F, 5.0F, 10.0F, 4.0F, new CubeDeformation(-0.05F)), PartPose.offsetAndRotation(0.0F, 0.0F, -5.0F, -0.5236F, 0.0F, 0.0F));

        PartDefinition PistonFrontEmissive = PistonFront.addOrReplaceChild("PistonFrontEmissive", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition cube_r19 = PistonFrontEmissive.addOrReplaceChild("cube_r19", CubeListBuilder.create().texOffs(40, 19).addBox(-2.5F, -10.0F, -2.0F, 5.0F, 10.0F, 4.0F, new CubeDeformation(-0.05F)), PartPose.offsetAndRotation(0.0F, 0.0F, -5.0F, -0.5236F, 0.0F, 0.0F));

        PartDefinition SparkPlugFront = PistonFront.addOrReplaceChild("SparkPlugFront", CubeListBuilder.create().texOffs(8, 64).addBox(-0.5F, 4.875F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(12, 64).addBox(-0.5F, -5.125F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(0, 75).addBox(-1.5F, -2.125F, -1.5F, 3.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -2.875F, -0.5252F, 1.9635F, 0.0F, 0.0F));

        PartDefinition SparkPlugFrontEmissive = SparkPlugFront.addOrReplaceChild("SparkPlugFrontEmissive", CubeListBuilder.create(), PartPose.offset(0.0F, 0.375F, 0.0F));

        PartDefinition cube_r20 = SparkPlugFrontEmissive.addOrReplaceChild("cube_r20", CubeListBuilder.create().texOffs(0, 64).addBox(-1.0F, -4.5F, -1.0F, 2.0F, 9.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 3.1416F, 0.0F));

        PartDefinition Key = Engine.addOrReplaceChild("Key", CubeListBuilder.create(), PartPose.offset(-3.5F, 0.0F, 5.0F));

        PartDefinition KeyEmissive = Key.addOrReplaceChild("KeyEmissive", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition cube_r21 = KeyEmissive.addOrReplaceChild("cube_r21", CubeListBuilder.create().texOffs(-2, 12).addBox(-0.5F, 0.0F, -1.5F, 1.0F, 0.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, -5.0F, 1.5708F, 0.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 128, 128);
    }

    @Override
    public void setupAnim(EngineHoistEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.Engine.visible = entity.hasEngine();
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        All.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    @Override
    public ModelPart root() {
        return this.All;
    }
}
