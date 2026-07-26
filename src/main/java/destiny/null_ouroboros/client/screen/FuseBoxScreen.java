package destiny.null_ouroboros.client.screen;

import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.server.block.entity.FuseBoxBlockEntity;
import destiny.null_ouroboros.server.menu.FuseBoxMenu;
import destiny.null_ouroboros.server.network.ServerboundFuseBoxTogglePacket;
import destiny.null_ouroboros.server.registry.PacketHandlerRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class FuseBoxScreen extends AbstractContainerScreen<FuseBoxMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/gui/fuse_box_gui.png");
    private static final int GUI_WIDTH = 175;
    private static final int GUI_HEIGHT = 286;
    private static final int TEX_SIZE = 512;

    public FuseBoxScreen(FuseBoxMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, GUI_WIDTH, GUI_HEIGHT, TEX_SIZE, TEX_SIZE);

        FuseBoxBlockEntity box = menu.getBlockEntity();
        for (int i = 0; i < FuseBoxMenu.FUSE_SLOT_COUNT; i++) {
            if (!box.hasFuse(i)) {
                continue;
            }

            int col = i % 2;
            int row = i / 2;
            int overlayX = leftPos + 13 + 76 * col;
            int overlayY = topPos + 5 + 26 * row;
            graphics.blit(TEXTURE, overlayX, overlayY, 176, 0, 74, 26, TEX_SIZE, TEX_SIZE);

            boolean on = box.isSwitchOn(i);
            int indicatorU = on ? 176 : 186;
            int indicatorV = 26;
            graphics.blit(TEXTURE, leftPos + 19 + 76 * col, topPos + 13 + 26 * row,
                    indicatorU, indicatorV, 10, 10, TEX_SIZE, TEX_SIZE);

            int switchU = on ? 176 : 196;
            graphics.blit(TEXTURE, leftPos + 58 + 76 * col, topPos + 13 + 26 * row,
                    switchU, 36, 20, 14, TEX_SIZE, TEX_SIZE);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && !menu.getBlockEntity().isCycling()) {
            int relX = (int) mouseX - leftPos;
            int relY = (int) mouseY - topPos;
            for (int i = 0; i < FuseBoxMenu.FUSE_SLOT_COUNT; i++) {
                if (!menu.getBlockEntity().hasFuse(i)) {
                    continue;
                }
                int col = i % 2;
                int row = i / 2;
                int switchX = 58 + 76 * col;
                int switchY = 13 + 26 * row;
                if (relX >= switchX && relX < switchX + 20 && relY >= switchY && relY < switchY + 14) {
                    PacketHandlerRegistry.INSTANCE.sendToServer(
                            new ServerboundFuseBoxTogglePacket(menu.getBlockEntity().getBlockPos(), i));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
