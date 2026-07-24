package destiny.null_ouroboros.client.render.player_anim;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class PlayerModelAnimatable extends HierarchicalModel<LivingEntity> {
    public static final float LEFT_ITEM_REST_X = 1.0F;
    public static final float RIGHT_ITEM_REST_X = -1.0F;
    public static final float ITEM_REST_Y = 10.0F;
    public static final float ITEM_REST_Z = -2.0F;

    private final ModelPart root;
    private final ModelPart leftItem;
    private final ModelPart rightItem;

    public PlayerModelAnimatable(HumanoidModel<?> model) {
        this.leftItem = new ModelPart(Collections.emptyList(), Map.of());
        this.rightItem = new ModelPart(Collections.emptyList(), Map.of());
        resetItemRest(this.leftItem, true);
        resetItemRest(this.rightItem, false);

        Map<String, ModelPart> children = new HashMap<>();
        children.put("head", model.head);
        children.put("body", model.body);
        children.put("left_arm", model.leftArm);
        children.put("right_arm", model.rightArm);
        children.put("left_leg", model.leftLeg);
        children.put("right_leg", model.rightLeg);
        children.put("left_item", this.leftItem);
        children.put("right_item", this.rightItem);
        if (model instanceof PlayerModel<?> playerModel) {
            children.put("hat", playerModel.hat);
            children.put("jacket", playerModel.jacket);
            children.put("left_sleeve", playerModel.leftSleeve);
            children.put("right_sleeve", playerModel.rightSleeve);
            children.put("left_pants", playerModel.leftPants);
            children.put("right_pants", playerModel.rightPants);
        }
        this.root = new ModelPart(Collections.emptyList(), children);
    }

    public static void resetItemRest(ModelPart item, boolean left) {
        item.x = left ? LEFT_ITEM_REST_X : RIGHT_ITEM_REST_X;
        item.y = ITEM_REST_Y;
        item.z = ITEM_REST_Z;
        item.xRot = 0.0F;
        item.yRot = 0.0F;
        item.zRot = 0.0F;
        item.xScale = 1.0F;
        item.yScale = 1.0F;
        item.zScale = 1.0F;
    }

    public ModelPart leftItem() {
        return leftItem;
    }

    public ModelPart rightItem() {
        return rightItem;
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
    }
}
