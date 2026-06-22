package destiny.null_ouroboros.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.server.menu.DustyComputerMenu;
import destiny.null_ouroboros.server.block.entity.DustyComputerBlockEntity;
import destiny.null_ouroboros.server.network.ServerBoundDustyComputerCommandPacket;
import destiny.null_ouroboros.server.network.ServerBoundDustyComputerShutdownPacket;
import destiny.null_ouroboros.server.registry.PacketHandlerRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class DustyComputerScreen extends AbstractContainerScreen<DustyComputerMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "textures/gui/dusty_computer_gui.png");
    private static final int GUI_WIDTH = 192;
    private static final int GUI_HEIGHT = 170;

    private static final int TEXT_LEFT = 5;
    private static final int TEXT_TOP = 5;
    private static final int TEXT_WIDTH = 184;
    private static final int TEXT_HEIGHT = 152;

    private final List<String> serverLines = new ArrayList<>();
    private final List<FormattedCharSequence> wrappedHistory = new ArrayList<>();

    @Nullable
    private String pendingLine = null;

    private String inputBuffer = "";
    private int cursorBlink = 0;
    private int scrollOffset = 0;

    private String currentPath = "T:\\";

    public DustyComputerScreen(DustyComputerMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        this.setFocused(null);
        refreshFromBlockEntity();
    }

    @Override
    public void containerTick() {
        super.containerTick();
        refreshFromBlockEntity();
        cursorBlink++;
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
    }

    private void refreshFromBlockEntity() {
        DustyComputerBlockEntity be = getBlockEntity();

        if (be == null) return;

        String path = be.getCurrentPath();
        if (path != null && !path.equals(currentPath)) {
            currentPath = path;
        }

        List<String> fresh = be.getLines();

        if (!fresh.equals(serverLines)) {
            serverLines.clear();
            serverLines.addAll(fresh);

            if (pendingLine != null && serverLines.contains(pendingLine)) {
                pendingLine = null;
                inputBuffer = "";
            }

            rebuildWrappedLines();
        }
    }

    private void rebuildWrappedLines() {
        wrappedHistory.clear();
        Font font = Minecraft.getInstance().font;

        for (String line : serverLines) {
            wrappedHistory.addAll(font.split(Component.literal(line), TEXT_WIDTH));
        }

        if (pendingLine != null) {
            wrappedHistory.addAll(font.split(Component.literal(pendingLine), TEXT_WIDTH));
        }

        scrollToBottom();
    }

    private FormattedCharSequence getInputLine() {
        String prefix = "> " + currentPath + (currentPath.endsWith("\\") ? "" : "\\") + " ";
        return Component.literal(prefix + inputBuffer).getVisualOrderText();
    }

    private int totalLines() {
        return wrappedHistory.size() + 1;
    }

    private int maxVisibleLines() {
        return TEXT_HEIGHT / Minecraft.getInstance().font.lineHeight;
    }

    private void scrollToBottom() {
        scrollOffset = Math.max(0, totalLines() - maxVisibleLines());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTextArea(graphics);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, GUI_WIDTH, GUI_HEIGHT, 256, 256);
    }

    private void renderTextArea(GuiGraphics graphics) {
        Font font = Minecraft.getInstance().font;
        int x = leftPos + TEXT_LEFT;
        int yBase = topPos + TEXT_TOP;
        int lineHeight = font.lineHeight;
        int maxLines = maxVisibleLines();

        enableScissor(x, yBase, TEXT_WIDTH, TEXT_HEIGHT);

        int startIndex = scrollOffset;
        int endIndex = Math.min(startIndex + maxLines, wrappedHistory.size());

        for (int i = startIndex; i < endIndex; i++) {
            int drawY = yBase + (i - startIndex) * lineHeight;
            graphics.drawString(font, wrappedHistory.get(i), x, drawY, 0xFF0000);
        }

        int inputLineIndex = totalLines() - 1;
        if (inputLineIndex >= startIndex && inputLineIndex < startIndex + maxLines) {
            int drawY = yBase + (inputLineIndex - startIndex) * lineHeight;
            FormattedCharSequence inputSeq = getInputLine();
            graphics.drawString(font, inputSeq, x, drawY, 0xFF0000);

            if (cursorBlink % 20 < 10) {
                String fullPrefix = "> " + currentPath + (currentPath.endsWith("\\") ? "" : "\\") + " ";
                int cursorX = x + font.width(fullPrefix + inputBuffer);
                int maxX = x + TEXT_WIDTH;

                if (cursorX < maxX) {
                    int charWidth = font.width("_");
                    graphics.fill(cursorX, drawY + lineHeight - 2, cursorX + charWidth, drawY + lineHeight - 1, 0xFFFF0000);
                }
            }
        }

        disableScissor();
    }

    private void enableScissor(int x, int y, int width, int height) {
        double guiScale = minecraft.getWindow().getGuiScale();
        int scissorX = (int) (x * guiScale);
        int scissorY = (int) (minecraft.getWindow().getHeight() - (y + height) * guiScale);
        int scissorWidth = (int) (width * guiScale);
        int scissorHeight = (int) (height * guiScale);

        RenderSystem.enableScissor(scissorX, scissorY, scissorWidth, scissorHeight);
    }

    private void disableScissor() {
        RenderSystem.disableScissor();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            sendCommand();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (!inputBuffer.isEmpty()) {
                inputBuffer = inputBuffer.substring(0, inputBuffer.length() - 1);
                scrollToBottom();
                PacketHandlerRegistry.INSTANCE.sendToServer(new ServerBoundDustyComputerCommandPacket(getMenu().getBlockPos(), ServerBoundDustyComputerCommandPacket.Action.ERASE));
            }

            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (codePoint >= 32 && codePoint != 127) {
            inputBuffer += codePoint;
            scrollToBottom();

            PacketHandlerRegistry.INSTANCE.sendToServer(
                    new ServerBoundDustyComputerCommandPacket(getMenu().getBlockPos(), ServerBoundDustyComputerCommandPacket.Action.TYPE));
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    private void sendCommand() {
        String command = inputBuffer.trim();

        if (command.isEmpty()) return;

        pendingLine = "> " + command;

        rebuildWrappedLines();
        scrollToBottom();

        BlockPos pos = getMenu().getBlockPos();
        PacketHandlerRegistry.INSTANCE.sendToServer(new ServerBoundDustyComputerCommandPacket(pos, command));
    }

    public static boolean isValidCommand(String command) {
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (isMouseOverTextArea(mouseX, mouseY)) {
            int maxScroll = Math.max(0, totalLines() - maxVisibleLines());
            scrollOffset = (int) Math.max(0, Math.min(scrollOffset - delta, maxScroll));

            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int bx = leftPos + 151;
            int by = topPos + 157;

            if (mouseX >= bx && mouseX < bx + 3 && mouseY >= by && mouseY < by + 3) {
                PacketHandlerRegistry.INSTANCE.sendToServer(new ServerBoundDustyComputerShutdownPacket(getMenu().getBlockPos()));

                this.minecraft.setScreen(null);
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isMouseOverTextArea(double mouseX, double mouseY) {
        return mouseX >= leftPos + TEXT_LEFT && mouseX < leftPos + TEXT_LEFT + TEXT_WIDTH && mouseY >= topPos + TEXT_TOP
                && mouseY < topPos + TEXT_TOP + TEXT_HEIGHT;
    }

    private DustyComputerBlockEntity getBlockEntity() {
        if (minecraft.level == null) return null;

        BlockPos pos = getMenu().getBlockPos();

        if (pos == null) return null;

        return (DustyComputerBlockEntity) minecraft.level.getBlockEntity(pos);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}