package destiny.null_ouroboros.client.render.model;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class EmptyArmorModel extends HumanoidModel<LivingEntity> {
    private static final EmptyArmorModel INSTANCE = new EmptyArmorModel();

    private EmptyArmorModel() {
        super(createEmptyRoot());
        this.head.visible = false;
        this.hat.visible = false;
        this.body.visible = false;
        this.leftArm.visible = false;
        this.rightArm.visible = false;
        this.leftLeg.visible = false;
        this.rightLeg.visible = false;
    }

    private static ModelPart createEmptyRoot() {
        Map<String, ModelPart> children = new HashMap<>();
        children.put("head", new ModelPart(new ArrayList<>(), new HashMap<>()));
        children.put("hat", new ModelPart(new ArrayList<>(), new HashMap<>()));
        children.put("body", new ModelPart(new ArrayList<>(), new HashMap<>()));
        children.put("left_arm", new ModelPart(new ArrayList<>(), new HashMap<>()));
        children.put("right_arm", new ModelPart(new ArrayList<>(), new HashMap<>()));
        children.put("left_leg", new ModelPart(new ArrayList<>(), new HashMap<>()));
        children.put("right_leg", new ModelPart(new ArrayList<>(), new HashMap<>()));
        return new ModelPart(new ArrayList<>(), children);
    }

    public static HumanoidModel<?> get() {
        return INSTANCE;
    }
}