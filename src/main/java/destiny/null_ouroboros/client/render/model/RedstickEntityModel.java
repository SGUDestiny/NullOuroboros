package destiny.null_ouroboros.client.render.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.server.entity.RedstickEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;

public class RedstickEntityModel extends EntityModel<RedstickEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "redstick"), "main");
    public final ModelPart bone;
    public final ModelPart emissive;
    public final ModelPart top;
    public final ModelPart bottom;

    public RedstickEntityModel(ModelPart root) {
        this.bone = root.getChild("bone");
        this.emissive = this.bone.getChild("emissive");
        this.top = this.bone.getChild("top");
        this.bottom = this.bone.getChild("bottom");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition bone = partdefinition.addOrReplaceChild("bone", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5F, -4.0F, -0.5F, 1.0F, 8.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.ZERO);

        bone.addOrReplaceChild("emissive", CubeListBuilder.create().texOffs(4, 0).addBox(-0.5F, -4.0F, -0.5F, 1.0F, 8.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

        bone.addOrReplaceChild("top", CubeListBuilder.create(), PartPose.offset(0.0F, -4.0F, 0.0F));

        bone.addOrReplaceChild("bottom", CubeListBuilder.create(), PartPose.offset(0.0F, 4.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 16, 16);
    }

    @Override
    public void setupAnim(RedstickEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.bone.resetPose();
    }

    public void renderEmissive(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay, float alpha) {
        poseStack.pushPose();
        this.bone.translateAndRotate(poseStack);
        this.emissive.render(poseStack, consumer, packedLight, packedOverlay, 1.0F, 1.0F, 1.0F, alpha);
        poseStack.popPose();
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        bone.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
