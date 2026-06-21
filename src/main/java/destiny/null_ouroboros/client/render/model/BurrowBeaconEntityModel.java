package destiny.null_ouroboros.client.render.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.client.render.animation.BurrowBeaconEntityAnimation;
import destiny.null_ouroboros.server.entity.BurrowBeaconEntity;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

public class BurrowBeaconEntityModel extends HierarchicalModel<BurrowBeaconEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "burrow_beacon"), "main");

    private final ModelPart root;
    private final ModelPart bone;
    private final ModelPart Emissive;
    private final ModelPart Drill;
    private final ModelPart LegFront;
    private final ModelPart LegFrontOuter;
    private final ModelPart LegRight;
    private final ModelPart LegRightOuter;
    private final ModelPart LegLeft;
    private final ModelPart LegLeftOuter;
    private final ModelPart LegBack;
    private final ModelPart LegBackOuter;

    public BurrowBeaconEntityModel(ModelPart root) {
        this.root = root;
        this.bone = root.getChild("bone");
        this.Emissive = this.bone.getChild("Emissive");
        this.Drill = this.bone.getChild("Drill");
        this.LegFront = this.bone.getChild("LegFront");
        this.LegFrontOuter = this.LegFront.getChild("LegFrontOuter");
        this.LegRight = this.bone.getChild("LegRight");
        this.LegRightOuter = this.LegRight.getChild("LegRightOuter");
        this.LegLeft = this.bone.getChild("LegLeft");
        this.LegLeftOuter = this.LegLeft.getChild("LegLeftOuter");
        this.LegBack = this.bone.getChild("LegBack");
        this.LegBackOuter = this.LegBack.getChild("LegBackOuter");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition bone = partdefinition.addOrReplaceChild("bone", CubeListBuilder.create().texOffs(16, 0).addBox(-3.0F, -31.0F, -3.0F, 6.0F, 1.0F, 6.0F, new CubeDeformation(0.0F))
                .texOffs(40, 0).addBox(-3.0F, -28.0F, -3.0F, 6.0F, 1.0F, 6.0F, new CubeDeformation(0.0F))
                .texOffs(0, 0).addBox(-2.0F, -27.0F, -2.0F, 4.0F, 27.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 19.0F, 0.0F));

        PartDefinition Emissive = bone.addOrReplaceChild("Emissive", CubeListBuilder.create().texOffs(16, 7).addBox(-2.0F, -28.0F, -2.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -2.0F, 0.0F));

        PartDefinition Drill = bone.addOrReplaceChild("Drill", CubeListBuilder.create().texOffs(16, 13).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 3.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(16, 20).addBox(-1.0F, 3.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition LegFront = bone.addOrReplaceChild("LegFront", CubeListBuilder.create().texOffs(48, 7).addBox(-1.0F, -14.0F, 0.0F, 2.0F, 14.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -1.0F, -2.0F));

        PartDefinition LegFrontOuter = LegFront.addOrReplaceChild("LegFrontOuter", CubeListBuilder.create().texOffs(0, 31).mirror().addBox(-2.0F, -2.0F, 0.0F, 4.0F, 16.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(0.0F, -14.0F, -0.025F));

        PartDefinition LegRight = bone.addOrReplaceChild("LegRight", CubeListBuilder.create().texOffs(40, 7).addBox(0.0F, -14.0F, -1.0F, 2.0F, 14.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(-2.0F, -1.0F, 0.0F));

        PartDefinition LegRightOuter = LegRight.addOrReplaceChild("LegRightOuter", CubeListBuilder.create().texOffs(36, 31).mirror().addBox(0.0F, -2.0F, -2.0F, 1.0F, 16.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(-0.025F, -14.0F, 0.0F));

        PartDefinition LegLeft = bone.addOrReplaceChild("LegLeft", CubeListBuilder.create().texOffs(40, 7).mirror().addBox(-2.0F, -14.0F, -1.0F, 2.0F, 14.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(2.0F, -1.0F, 0.0F));

        PartDefinition LegLeftOuter = LegLeft.addOrReplaceChild("LegLeftOuter", CubeListBuilder.create().texOffs(10, 31).mirror().addBox(-1.0F, -2.0F, -2.0F, 1.0F, 16.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(0.025F, -14.0F, 0.0F));

        PartDefinition LegBack = bone.addOrReplaceChild("LegBack", CubeListBuilder.create().texOffs(32, 7).addBox(-1.0F, -14.0F, -2.0F, 2.0F, 14.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -1.0F, 2.0F));

        PartDefinition LegBackOuter = LegBack.addOrReplaceChild("LegBackOuter", CubeListBuilder.create().texOffs(20, 31).addBox(-2.0F, -2.0F, -0.975F, 4.0F, 16.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -14.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(BurrowBeaconEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);

        BurrowBeaconEntity.State state = entity.getAnimationState();
        int startTime = entity.getAnimationStartTime();
        float elapsedTicks = Math.max(0f, ageInTicks - startTime);

        AnimationDefinition anim = switch (state) {
            case DEPLOY -> BurrowBeaconEntityAnimation.deploy;
            case LAND   -> BurrowBeaconEntityAnimation.land;
            case DRILL, DRILL_IDLE -> BurrowBeaconEntityAnimation.drill;
        };

        long elapsedMs = (long)(elapsedTicks * 50);
        long lengthMs = (long)(anim.lengthInSeconds() * 1000);

        long animationMs;
        if (state == BurrowBeaconEntity.State.DRILL_IDLE) {
            animationMs = (long)(BurrowBeaconEntityAnimation.drill.lengthInSeconds() * 1000);
        } else if (state == BurrowBeaconEntity.State.DEPLOY) {
            animationMs = Math.min(elapsedMs, lengthMs);
        } else {
            animationMs = Math.min(elapsedMs, lengthMs);
        }

        KeyframeAnimations.animate(this, anim, animationMs, 1.0F, new Vector3f());
    }

    public ModelPart getEmissive() {
        return this.Emissive;
    }

    public void renderEmissive(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay) {
        this.bone.translateAndRotate(poseStack);
        this.Emissive.render(poseStack, consumer, packedLight, packedOverlay);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        bone.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}