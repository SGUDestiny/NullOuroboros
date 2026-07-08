package destiny.null_ouroboros.server.event;

import destiny.null_ouroboros.server.registry.ArmorMaterialRegistry;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class ClientEvents {
    private static final Map<UUID, boolean[]> ORIGINAL_VISIBILITIES = new HashMap<>();

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        PlayerModel<?> model = event.getRenderer().getModel();

        boolean hasChestplate = player.getInventory().armor.get(2).getItem() instanceof ArmorItem &&
                ((ArmorItem) player.getInventory().armor.get(2).getItem()).getMaterial() == ArmorMaterialRegistry.LIQUIDATOR;
        boolean hasLeggings = player.getInventory().armor.get(1).getItem() instanceof ArmorItem &&
                ((ArmorItem) player.getInventory().armor.get(1).getItem()).getMaterial() == ArmorMaterialRegistry.LIQUIDATOR;

        boolean[] vis = new boolean[]{
                model.body.visible,
                model.leftArm.visible,
                model.rightArm.visible,
                model.leftLeg.visible,
                model.rightLeg.visible,
                model.jacket.visible,
                model.leftSleeve.visible,
                model.rightSleeve.visible,
                model.leftPants.visible,
                model.rightPants.visible
        };
        ORIGINAL_VISIBILITIES.put(player.getUUID(), vis);

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

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();
        PlayerModel<?> model = event.getRenderer().getModel();
        boolean[] vis = ORIGINAL_VISIBILITIES.remove(player.getUUID());
        if (vis != null) {
            model.body.visible = vis[0];
            model.leftArm.visible = vis[1];
            model.rightArm.visible = vis[2];
            model.leftLeg.visible = vis[3];
            model.rightLeg.visible = vis[4];
            model.jacket.visible = vis[5];
            model.leftSleeve.visible = vis[6];
            model.rightSleeve.visible = vis[7];
            model.leftPants.visible = vis[8];
            model.rightPants.visible = vis[9];
        }
    }
}