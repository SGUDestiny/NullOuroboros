package destiny.null_ouroboros.client.render.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import destiny.null_ouroboros.NullOuroboros;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public class ElectromagneticAssemblyBlockModel extends EntityModel<Entity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "electromagnetic_assembly"), "main");
    private final ModelPart bone;
    private final ModelPart emissive1;
    private final ModelPart emissive;
    private final ModelPart emissive2;
    private final ModelPart vane;
    private final ModelPart spinner;

    public ElectromagneticAssemblyBlockModel(ModelPart root) {
        this.bone = root.getChild("bone");
        this.emissive1 = this.bone.getChild("emissive1");
        this.emissive = this.bone.getChild("emissive");
        this.emissive2 = this.bone.getChild("emissive2");
        this.vane = this.bone.getChild("vane");
        this.spinner = this.bone.getChild("spinner");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition bone = partdefinition.addOrReplaceChild("bone", CubeListBuilder.create().texOffs(0, 29).addBox(-9.0F, 6.0F, -1.0F, 10.0F, 2.0F, 10.0F, new CubeDeformation(0.0F))
                .texOffs(0, 0).addBox(-11.0F, 1.0F, -3.0F, 14.0F, 5.0F, 14.0F, new CubeDeformation(0.0F))
                .texOffs(0, 19).addBox(-10.0F, -2.0F, 3.0F, 7.0F, 3.0F, 7.0F, new CubeDeformation(0.0F))
                .texOffs(42, 8).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(28, 19).addBox(-4.5F, -7.0F, 0.0F, 9.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offset(4.0F, 20.0F, -4.0F));

        PartDefinition cube_r1 = bone.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(46, 19).addBox(-0.5F, -4.0F, 0.0F, 1.0F, 8.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -6.0F, 0.0F, 0.0F, -0.7854F, 0.0F));

        PartDefinition cube_r2 = bone.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(46, 19).addBox(-0.5F, -4.0F, 0.0F, 1.0F, 8.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -6.0F, 0.0F, 0.0F, -2.3562F, 0.0F));

        PartDefinition cube_r3 = bone.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(28, 22).addBox(-4.5F, -1.5F, 0.0F, 9.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -5.5F, 0.0F, 0.0F, 1.5708F, 0.0F));

        PartDefinition cube_r4 = bone.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, -5.0F, 0.0F, 7.0F, 10.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.5F, -7.0F, 6.5F, 0.0F, 0.7854F, 0.0F));

        PartDefinition cube_r5 = bone.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, -5.0F, 0.0F, 7.0F, 10.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.5F, -7.0F, 6.5F, 0.0F, -0.7854F, 0.0F));

        PartDefinition emissive1 = bone.addOrReplaceChild("emissive1", CubeListBuilder.create().texOffs(0, 42).addBox(-7.0F, -5.0F, -7.0F, 14.0F, 5.0F, 14.0F, new CubeDeformation(0.0F)), PartPose.offset(-4.0F, 6.0F, 4.0F));

        PartDefinition emissive = bone.addOrReplaceChild("emissive", CubeListBuilder.create().texOffs(0, 60).addBox(-6.0F, -3.0F, -1.0F, 7.0F, 3.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offset(-4.0F, 1.0F, 4.0F));

        PartDefinition emissive2 = bone.addOrReplaceChild("emissive2", CubeListBuilder.create(), PartPose.offset(-6.5F, -7.0F, 6.5F));

        PartDefinition emissive2_r1 = emissive2.addOrReplaceChild("emissive2_r1", CubeListBuilder.create().texOffs(0, 41).addBox(-3.5F, -5.0F, 0.0F, 7.0F, 10.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.7854F, 0.0F));

        PartDefinition emissive2_r2 = emissive2.addOrReplaceChild("emissive2_r2", CubeListBuilder.create().texOffs(0, 41).addBox(-3.5F, -5.0F, 0.0F, 7.0F, 10.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.7854F, 0.0F));

        PartDefinition vane = bone.addOrReplaceChild("vane", CubeListBuilder.create().texOffs(30, 27).addBox(-5.5F, -2.0F, 0.0F, 11.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -10.0F, 0.0F));

        PartDefinition spinner = bone.addOrReplaceChild("spinner", CubeListBuilder.create().texOffs(42, 0).addBox(-3.0F, -2.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(42, 4).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition spinner_r1 = spinner.addOrReplaceChild("spinner_r1", CubeListBuilder.create().texOffs(42, 0).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.0F, -1.0F, 0.0F, 0.0F, 3.1416F, 0.0F));

        return LayerDefinition.create(meshdefinition, 80, 80);
    }

    @Override
    public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        bone.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
