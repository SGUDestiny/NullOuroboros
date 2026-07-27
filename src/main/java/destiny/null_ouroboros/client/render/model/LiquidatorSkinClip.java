package destiny.null_ouroboros.client.render.model;

import destiny.null_ouroboros.server.item.RespiratorGear;
import destiny.null_ouroboros.server.registry.ArmorMaterialRegistry;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayDeque;
import java.util.Deque;

public final class LiquidatorSkinClip {
    private static final ThreadLocal<Deque<boolean[]>> SAVED = ThreadLocal.withInitial(ArrayDeque::new);

    private LiquidatorSkinClip() {}

    public static void apply(Player player, PlayerModel<?> model) {
        boolean hasHelmet = isLiquidator(player.getItemBySlot(EquipmentSlot.HEAD));
        boolean hasChestplate = isLiquidator(player.getItemBySlot(EquipmentSlot.CHEST));
        boolean hasLeggings = isLiquidator(player.getItemBySlot(EquipmentSlot.LEGS));
        if (!hasHelmet && !hasChestplate && !hasLeggings) {
            return;
        }

        SAVED.get().push(new boolean[]{
                model.hat.visible,
                model.body.visible,
                model.leftArm.visible,
                model.rightArm.visible,
                model.jacket.visible,
                model.leftSleeve.visible,
                model.rightSleeve.visible,
                model.leftPants.visible,
                model.rightPants.visible
        });

        if (hasHelmet) {
            model.hat.visible = false;
        }
        if (hasChestplate) {
            model.body.visible = false;
            model.leftArm.visible = false;
            model.rightArm.visible = false;
            model.jacket.visible = false;
            model.leftSleeve.visible = false;
            model.rightSleeve.visible = false;
        }
        if (hasLeggings) {
            model.leftPants.visible = false;
            model.rightPants.visible = false;
        }
    }

    public static void restore(PlayerModel<?> model) {
        Deque<boolean[]> stack = SAVED.get();
        if (stack.isEmpty()) {
            return;
        }
        boolean[] vis = stack.pop();
        model.hat.visible = vis[0];
        model.body.visible = vis[1];
        model.leftArm.visible = vis[2];
        model.rightArm.visible = vis[3];
        model.jacket.visible = vis[4];
        model.leftSleeve.visible = vis[5];
        model.rightSleeve.visible = vis[6];
        model.leftPants.visible = vis[7];
        model.rightPants.visible = vis[8];
        if (stack.isEmpty()) {
            SAVED.remove();
        }
    }

    private static boolean isLiquidator(ItemStack stack) {
        return stack.getItem() instanceof ArmorItem armor
                && armor.getMaterial() == ArmorMaterialRegistry.LIQUIDATOR;
    }
}
