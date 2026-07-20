package destiny.null_ouroboros.client.gui;

import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.common.steel_leviathan.SteelLeviathanConstants;
import destiny.null_ouroboros.server.entity.steel_leviathan.SteelLeviathanHeadEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = NullOuroboros.MODID, value = Dist.CLIENT)
public class SteelLeviathanBossBarOverlay {
    private static final int FRAME_U = 0;
    private static final int FRAME_V = 0;
    private static final int FRAME_W = 202;
    private static final int FRAME_H = 18;
    private static final int BAR_U = 0;
    private static final int BAR_FULL_V = 19;
    private static final int BAR_EMPTY_V = 25;
    private static final int BAR_W = 172;
    private static final int BAR_H = 6;

    private static final int TITLE_U = 0;
    private static final int TITLE_V = 31;
    private static final int TITLE_W = 63;
    private static final int TITLE_H = 39;

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.BOSS_EVENT_PROGRESS.type()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        SteelLeviathanHeadEntity head = findNearbyBoss(mc);
        if (head == null || !head.isBossBarVisible()) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        int screenW = event.getWindow().getGuiScaledWidth();
        int x = (screenW - FRAME_W) / 2;
        int y = 12;

        int barX = x + (FRAME_W - BAR_W) / 2 + 2;
        int barY = y + 6;

        graphics.blit(SteelLeviathanConstants.BOSS_BAR, x, y, FRAME_U, FRAME_V, FRAME_W, FRAME_H, 256, 256);
        graphics.blit(SteelLeviathanConstants.BOSS_BAR, barX, barY, BAR_U, BAR_EMPTY_V, BAR_W, BAR_H, 256, 256);
        float ratio = Mth.clamp(head.getMainHealth() / SteelLeviathanConstants.MAX_HEALTH, 0.0F, 1.0F);
        int filled = Mth.ceil(BAR_W * ratio);
        if (filled > 0) {
            graphics.blit(SteelLeviathanConstants.BOSS_BAR, barX, barY, BAR_U, BAR_FULL_V, filled, BAR_H, 256, 256);
        }
        int titleX = x + (FRAME_W - TITLE_W) / 2;
        int titleY = y + FRAME_H + 1;
        graphics.blit(SteelLeviathanConstants.BOSS_BAR, titleX, titleY, TITLE_U, TITLE_V, TITLE_W, TITLE_H, 256, 256);
    }

    private static SteelLeviathanHeadEntity findNearbyBoss(Minecraft mc) {
        AABB box = mc.player.getBoundingBox().inflate(96.0D);
        SteelLeviathanHeadEntity closest = null;
        double best = Double.MAX_VALUE;
        for (Entity entity : mc.level.getEntities(mc.player, box, e -> e instanceof SteelLeviathanHeadEntity h && h.isBossBarVisible())) {
            double d = entity.distanceToSqr(mc.player);
            if (d < best) {
                best = d;
                closest = (SteelLeviathanHeadEntity) entity;
            }
        }
        return closest;
    }
}

