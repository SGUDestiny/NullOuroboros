package destiny.null_ouroboros.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.client.network.ClientBoundDustyComputerSyncPacket;
import destiny.null_ouroboros.server.menu.DustyComputerMenu;
import destiny.null_ouroboros.server.block.entity.DustyComputerBlockEntity;
import destiny.null_ouroboros.server.network.ServerBoundDustyComputerCloseFileSessionPacket;
import destiny.null_ouroboros.server.network.ServerBoundDustyComputerCommandPacket;
import destiny.null_ouroboros.server.network.ServerBoundDustyComputerShutdownPacket;
import destiny.null_ouroboros.server.registry.PacketHandlerRegistry;
import destiny.null_ouroboros.server.terminal.format.TerminusTextFormat;
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

    private static final int LOADING_DURATION_TICKS = 60;

    private static final String[] LOADING_LOGO = {
            " _____              _             ",
            "|_   _|__ _ _ _ __ [_]_ _ _  _ ___",
            "  | |/ -_) '_| '  \\| | ' \\ || (_-<",
            "  |_|\\___|_| |_|_|_|_|_||_\\_,_/__/"
    };
    private static final String LOADING_SPINNER = "/-\\|";
    private static final String[] LOADING_MESSAGES = {
            "Loading systems.  ",
            "Loading systems.. ",
            "Loading systems..."
    };
    private static final String[] LOADING_EYE = {
            " ___ ",
            "<_X_>"
    };
    private static final String LOADING_VERSION = "v.IX";
    private static final int LOADING_MESSAGE_TICKS = 20;

    private final List<String> serverLines = new ArrayList<>();
    private final List<FormattedCharSequence> wrappedHistory = new ArrayList<>();
    private final List<String> fileLines = new ArrayList<>();
    private final List<FileVisualSegment> fileVisualSegments = new ArrayList<>();

    @Nullable
    private String pendingLine = null;

    private boolean awaitingCommandResponse = false;
    private ClientBoundDustyComputerSyncPacket.FileSessionType fileSessionType =
            ClientBoundDustyComputerSyncPacket.FileSessionType.NONE;
    private boolean fileBufferInitialized = false;

    private String inputBuffer = "";
    private final List<String> commandHistory = new ArrayList<>();
    private int historyIndex = -1;
    private String historyDraft = "";
    private boolean seededHistoryFromServer = false;
    private int cursorBlink = 0;
    private int scrollOffset = 0;
    private int cursorLine = 0;
    private int cursorColumn = 0;

    private String currentPath = "T:\\";
    private String fileSessionPath = "";

    private static final class FileVisualSegment {
        final FormattedCharSequence text;
        final int logicalLine;
        final int logicalStartCol;
        final int logicalEndCol;
        final boolean chrome;

        FileVisualSegment(FormattedCharSequence text, int logicalLine, int logicalStartCol, int logicalEndCol, boolean chrome) {
            this.text = text;
            this.logicalLine = logicalLine;
            this.logicalStartCol = logicalStartCol;
            this.logicalEndCol = logicalEndCol;
            this.chrome = chrome;
        }
    }

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

    private boolean inFileSession() {
        return fileSessionType != ClientBoundDustyComputerSyncPacket.FileSessionType.NONE;
    }

    private boolean inFileEdit() {
        return fileSessionType == ClientBoundDustyComputerSyncPacket.FileSessionType.EDIT;
    }

    private boolean isLoading() {
        if (minecraft.level == null) {
            return false;
        }

        long poweredOn = menu.getPoweredOnGameTime();
        if (poweredOn < 0) {
            return false;
        }

        return minecraft.level.getGameTime() - poweredOn < LOADING_DURATION_TICKS;
    }

    private void refreshFromBlockEntity() {
        DustyComputerBlockEntity be = getBlockEntity();
        if (be == null) return;

        String path = be.getCurrentPath();
        if (path != null && !path.equals(currentPath)) {
            currentPath = path;
        }

        ClientBoundDustyComputerSyncPacket.FileSessionType serverSessionType = be.getFileSessionType();
        if (serverSessionType != fileSessionType) {
            if (serverSessionType == ClientBoundDustyComputerSyncPacket.FileSessionType.NONE) {
                fileSessionType = ClientBoundDustyComputerSyncPacket.FileSessionType.NONE;
                fileBufferInitialized = false;
                fileSessionPath = "";
                fileLines.clear();
                cursorLine = 0;
                cursorColumn = 0;
                rebuildFileVisualSegments();
            } else {
                fileSessionType = serverSessionType;
                fileBufferInitialized = false;
            }
        }

        List<String> fresh = be.getLines();
        if (!inFileSession() && !fresh.equals(serverLines)) {
            serverLines.clear();
            serverLines.addAll(fresh);
            rebuildWrappedHistory();
            scrollToBottom();
            if (fresh.isEmpty()) {
                commandHistory.clear();
                historyIndex = -1;
                historyDraft = "";
                seededHistoryFromServer = false;
            } else if (!seededHistoryFromServer) {
                seedCommandHistoryFromServerLines(fresh);
                seededHistoryFromServer = true;
            }
        }

        if (inFileSession() && !fileBufferInitialized) {
            fileSessionPath = be.getFileSessionPath();
            initializeFileBuffer(be.getFileSessionContent());
        }

        if (awaitingCommandResponse) {
            pendingLine = null;
            inputBuffer = "";
            awaitingCommandResponse = false;
            rebuildWrappedHistory();
            scrollToBottom();
        }
    }

    private void initializeFileBuffer(String content) {
        fileLines.clear();
        if (content == null || content.isEmpty()) {
            fileLines.add("");
        } else {
            fileLines.addAll(List.of(content.split("\n", -1)));
        }
        cursorLine = Math.max(0, fileLines.size() - 1);
        cursorColumn = fileLines.get(cursorLine).length();
        fileBufferInitialized = true;
        rebuildFileVisualSegments();
        scrollToBottom();
    }

    private void rebuildWrappedHistory() {
        wrappedHistory.clear();
        Font font = Minecraft.getInstance().font;

        for (String line : serverLines) {
            wrappedHistory.addAll(font.split(Component.literal(line), TEXT_WIDTH));
        }

        if (pendingLine != null) {
            wrappedHistory.addAll(font.split(Component.literal(pendingLine), TEXT_WIDTH));
        }
    }

    private void rebuildFileVisualSegments() {
        fileVisualSegments.clear();
        if (!inFileSession()) return;

        Font font = Minecraft.getInstance().font;
        boolean formatted = fileSessionType == ClientBoundDustyComputerSyncPacket.FileSessionType.VIEW;

        String fileName = fileNameFromPath(fileSessionPath);
        Component headerComponent = formatted
                ? Component.translatable("message.null_ouroboros.terminus.file.reading_header", fileName)
                : Component.translatable("message.null_ouroboros.terminus.file.editing_header", fileName);
        for (FormattedCharSequence segment : font.split(headerComponent, TEXT_WIDTH)) {
            fileVisualSegments.add(new FileVisualSegment(segment, -1, 0, 0, true));
        }

        for (int lineIndex = 0; lineIndex < fileLines.size(); lineIndex++) {
            String line = fileLines.get(lineIndex);
            Component lineComponent = formatted
                    ? TerminusTextFormat.parseForDisplay(line)
                    : Component.literal(line).withStyle(style -> style.withColor(0xFF0000));
            List<FormattedCharSequence> wrapped = font.split(lineComponent, TEXT_WIDTH);
            int col = 0;
            for (FormattedCharSequence segment : wrapped) {
                int segmentLen = segment.toString().length();
                fileVisualSegments.add(new FileVisualSegment(segment, lineIndex, col, col + segmentLen, false));
                col += segmentLen;
            }
            if (wrapped.isEmpty()) {
                fileVisualSegments.add(new FileVisualSegment(Component.empty().getVisualOrderText(), lineIndex, 0, 0, false));
            }
        }

        List<FormattedCharSequence> hintWrapped = font.split(
                Component.translatable("message.null_ouroboros.terminus.file.close_hint"), TEXT_WIDTH);
        for (FormattedCharSequence segment : hintWrapped) {
            fileVisualSegments.add(new FileVisualSegment(segment, -1, 0, 0, true));
        }
    }

    private static String fileNameFromPath(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        path = path.replaceAll("\\\\+$", "");
        int lastBackslash = path.lastIndexOf('\\');
        return lastBackslash >= 0 ? path.substring(lastBackslash + 1) : path;
    }

    private FormattedCharSequence getInputLine() {
        String prefix = "> " + currentPath + (currentPath.endsWith("\\") ? "" : "\\") + " ";
        return Component.literal(prefix + inputBuffer).getVisualOrderText();
    }

    private int totalLines() {
        if (inFileSession()) {
            return fileVisualSegments.size();
        }
        return wrappedHistory.size() + 1;
    }

    private int maxVisibleLines() {
        return TEXT_HEIGHT / Minecraft.getInstance().font.lineHeight;
    }

    private void scrollToBottom() {
        scrollOffset = Math.max(0, totalLines() - maxVisibleLines());
    }

    private void scrollToCursor() {
        int cursorVisualIndex = findCursorVisualIndex();
        int maxLines = maxVisibleLines();
        if (cursorVisualIndex < scrollOffset) {
            scrollOffset = cursorVisualIndex;
        } else if (cursorVisualIndex >= scrollOffset + maxLines) {
            scrollOffset = cursorVisualIndex - maxLines + 1;
        }
    }

    private int findCursorVisualIndex() {
        for (int i = 0; i < fileVisualSegments.size(); i++) {
            FileVisualSegment seg = fileVisualSegments.get(i);
            if (!seg.chrome && seg.logicalLine == cursorLine
                    && cursorColumn >= seg.logicalStartCol && cursorColumn <= seg.logicalEndCol) {
                return i;
            }
        }
        return Math.max(0, fileVisualSegments.size() - 1);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (isLoading()) {
            renderLoadingScreen(graphics);
        } else {
            renderTextArea(graphics);
        }
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

        int visualIndex = 0;
        int startIndex = scrollOffset;
        int endIndex = startIndex + maxLines;

        if (!inFileSession()) {
            for (int i = 0; i < wrappedHistory.size(); i++, visualIndex++) {
                if (visualIndex >= startIndex && visualIndex < endIndex) {
                    int drawY = yBase + (visualIndex - startIndex) * lineHeight;
                    graphics.drawString(font, wrappedHistory.get(i), x, drawY, 0xFF0000);
                }
            }

            int inputLineIndex = totalLines() - 1;
            if (inputLineIndex >= startIndex && inputLineIndex < endIndex) {
                int drawY = yBase + (inputLineIndex - startIndex) * lineHeight;
                graphics.drawString(font, getInputLine(), x, drawY, 0xFF0000);

                if (cursorBlink % 20 < 10) {
                    String fullPrefix = "> " + currentPath + (currentPath.endsWith("\\") ? "" : "\\") + " ";
                    int cursorX = x + font.width(fullPrefix + inputBuffer);
                    if (cursorX < x + TEXT_WIDTH) {
                        int charWidth = font.width("_");
                        graphics.fill(cursorX, drawY + lineHeight - 2, cursorX + charWidth, drawY + lineHeight - 1, 0xFFFF0000);
                    }
                }
            }
        } else {
            for (FileVisualSegment seg : fileVisualSegments) {
                if (visualIndex >= startIndex && visualIndex < endIndex) {
                    int drawY = yBase + (visualIndex - startIndex) * lineHeight;
                    graphics.drawString(font, seg.text, x, drawY, 0xFF0000);

                    if (inFileEdit() && !seg.chrome && seg.logicalLine == cursorLine
                            && cursorColumn >= seg.logicalStartCol && cursorColumn <= seg.logicalEndCol
                            && cursorBlink % 20 < 10) {
                        String line = fileLines.get(cursorLine);
                        String before = line.substring(seg.logicalStartCol, Math.min(cursorColumn, line.length()));
                        int cursorX = x + font.width(before);
                        if (cursorX < x + TEXT_WIDTH) {
                            int charWidth = font.width("_");
                            graphics.fill(cursorX, drawY + lineHeight - 2, cursorX + charWidth, drawY + lineHeight - 1, 0xFFFF0000);
                        }
                    }
                }
                visualIndex++;
            }
        }

        disableScissor();
    }

    private void renderLoadingScreen(GuiGraphics graphics) {
        Font font = Minecraft.getInstance().font;
        int xBase = leftPos + TEXT_LEFT;
        int yBase = topPos + TEXT_TOP;
        int lineHeight = font.lineHeight;
        int color = 0xFF0000;
        int gapLines = 1;

        enableScissor(xBase, yBase, TEXT_WIDTH, TEXT_HEIGHT);

        int contentLines = LOADING_LOGO.length + gapLines + 1 + gapLines + 1 + gapLines + LOADING_EYE.length;
        int contentHeight = contentLines * lineHeight;
        int startY = yBase + (TEXT_HEIGHT - contentHeight) / 2 - lineHeight;

        int y = startY;
        int logoCharCount = maxCharCount(LOADING_LOGO);
        for (String line : LOADING_LOGO) {
            drawMonospaceLine(graphics, font, line, xBase, TEXT_WIDTH, logoCharCount, y, color);
            y += lineHeight;
        }

        y += gapLines * lineHeight;

        String spinner = String.valueOf(LOADING_SPINNER.charAt((cursorBlink / 4) % LOADING_SPINNER.length()));
        graphics.drawString(font, spinner, centeredX(font, spinner, xBase, TEXT_WIDTH), y, color);
        y += lineHeight;

        y += gapLines * lineHeight;

        long elapsedTicks = minecraft.level.getGameTime() - menu.getPoweredOnGameTime();
        int loadingFrame = (int) Math.min(LOADING_MESSAGES.length - 1, elapsedTicks / LOADING_MESSAGE_TICKS);
        int loadingAnchorX = xBase + (TEXT_WIDTH - font.width(LOADING_MESSAGES[2])) / 2;
        graphics.drawString(font, LOADING_MESSAGES[loadingFrame], loadingAnchorX, y, color);
        y += lineHeight;

        y += gapLines * lineHeight;

        int eyeCharCount = maxCharCount(LOADING_EYE);
        for (String line : LOADING_EYE) {
            drawMonospaceLine(graphics, font, line, xBase, TEXT_WIDTH, eyeCharCount, y, color);
            y += lineHeight;
        }

        graphics.drawString(
                font,
                LOADING_VERSION,
                xBase + TEXT_WIDTH - font.width(LOADING_VERSION),
                yBase + TEXT_HEIGHT - lineHeight,
                color);

        disableScissor();
    }

    private static int centeredX(Font font, String text, int areaLeft, int areaWidth) {
        return areaLeft + (areaWidth - font.width(text)) / 2;
    }

    private static int maxCharCount(String[] lines) {
        int max = 0;
        for (String line : lines) {
            max = Math.max(max, line.length());
        }
        return max;
    }

    private static void drawMonospaceLine(GuiGraphics graphics, Font font, String line, int areaLeft, int areaWidth,
                                          int refCharCount, int y, int color) {
        int charAdvance = font.width("W");
        int gridWidth = refCharCount * charAdvance;
        int startX = areaLeft + (areaWidth - gridWidth) / 2;

        for (int i = 0; i < line.length(); i++) {
            graphics.drawString(font, String.valueOf(line.charAt(i)), startX + i * charAdvance, y, color);
        }
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
        if (isLoading()) {
            if (minecraft.options.keyInventory.matches(keyCode, scanCode)) {
                return true;
            }
            return true;
        }

        if (minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            return true;
        }

        if (inFileSession()) {
            if (keyCode == GLFW.GLFW_KEY_R && (modifiers & GLFW.GLFW_MOD_SHIFT) != 0) {
                closeFileSession();
                return true;
            }

            if (inFileEdit()) {
                if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                    insertLineBreak();
                    return true;
                }

                if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                    deleteBackward();
                    return true;
                }

                if (keyCode == GLFW.GLFW_KEY_LEFT) {
                    moveCursor(-1, 0);
                    return true;
                }
                if (keyCode == GLFW.GLFW_KEY_RIGHT) {
                    moveCursor(1, 0);
                    return true;
                }
                if (keyCode == GLFW.GLFW_KEY_UP) {
                    moveCursor(0, -1);
                    return true;
                }
                if (keyCode == GLFW.GLFW_KEY_DOWN) {
                    moveCursor(0, 1);
                    return true;
                }
            }

            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        if (keyCode == GLFW.GLFW_KEY_UP) {
            moveCommandHistory(-1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DOWN) {
            moveCommandHistory(1);
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            sendCommand();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            historyIndex = -1;
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
        if (isLoading()) {
            return true;
        }

        if (inFileSession()) {
            if ((modifiers & GLFW.GLFW_MOD_SHIFT) != 0 && (codePoint == 'R' || codePoint == 'r')) {
                return true;
            }
            if (inFileEdit() && codePoint >= 32 && codePoint != 127) {
                insertChar(codePoint);
                return true;
            }
            return super.charTyped(codePoint, modifiers);
        }

        if (codePoint >= 32 && codePoint != 127) {
            historyIndex = -1;
            inputBuffer += codePoint;
            scrollToBottom();
            PacketHandlerRegistry.INSTANCE.sendToServer(
                    new ServerBoundDustyComputerCommandPacket(getMenu().getBlockPos(), ServerBoundDustyComputerCommandPacket.Action.TYPE));
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    private void insertChar(char c) {
        String line = fileLines.get(cursorLine);
        fileLines.set(cursorLine, line.substring(0, cursorColumn) + c + line.substring(cursorColumn));
        cursorColumn++;
        rebuildFileVisualSegments();
        scrollToCursor();
        PacketHandlerRegistry.INSTANCE.sendToServer(
                new ServerBoundDustyComputerCommandPacket(getMenu().getBlockPos(), ServerBoundDustyComputerCommandPacket.Action.TYPE));
    }

    private void insertLineBreak() {
        String line = fileLines.get(cursorLine);
        String before = line.substring(0, cursorColumn);
        String after = line.substring(cursorColumn);
        fileLines.set(cursorLine, before);
        fileLines.add(cursorLine + 1, after);
        cursorLine++;
        cursorColumn = 0;
        rebuildFileVisualSegments();
        scrollToCursor();
        PacketHandlerRegistry.INSTANCE.sendToServer(
                new ServerBoundDustyComputerCommandPacket(getMenu().getBlockPos(), ServerBoundDustyComputerCommandPacket.Action.TYPE));
    }

    private void deleteBackward() {
        if (cursorColumn > 0) {
            String line = fileLines.get(cursorLine);
            fileLines.set(cursorLine, line.substring(0, cursorColumn - 1) + line.substring(cursorColumn));
            cursorColumn--;
        } else if (cursorLine > 0) {
            String prev = fileLines.get(cursorLine - 1);
            String current = fileLines.get(cursorLine);
            cursorColumn = prev.length();
            fileLines.set(cursorLine - 1, prev + current);
            fileLines.remove(cursorLine);
            cursorLine--;
        } else {
            return;
        }
        rebuildFileVisualSegments();
        scrollToCursor();
        PacketHandlerRegistry.INSTANCE.sendToServer(
                new ServerBoundDustyComputerCommandPacket(getMenu().getBlockPos(), ServerBoundDustyComputerCommandPacket.Action.ERASE));
    }

    private void moveCursor(int deltaCol, int deltaLine) {
        if (deltaLine != 0) {
            int newLine = cursorLine + deltaLine;
            if (newLine < 0 || newLine >= fileLines.size()) return;
            cursorLine = newLine;
            cursorColumn = Math.min(cursorColumn, fileLines.get(cursorLine).length());
        } else if (deltaCol < 0) {
            if (cursorColumn > 0) {
                cursorColumn--;
            } else if (cursorLine > 0) {
                cursorLine--;
                cursorColumn = fileLines.get(cursorLine).length();
            }
        } else {
            String line = fileLines.get(cursorLine);
            if (cursorColumn < line.length()) {
                cursorColumn++;
            } else if (cursorLine < fileLines.size() - 1) {
                cursorLine++;
                cursorColumn = 0;
            }
        }
        scrollToCursor();
    }

    private void closeFileSession() {
        boolean save = inFileEdit();
        String content = save ? String.join("\n", fileLines) : "";
        PacketHandlerRegistry.INSTANCE.sendToServer(
                new ServerBoundDustyComputerCloseFileSessionPacket(getMenu().getBlockPos(), save, content));
    }

    private void seedCommandHistoryFromServerLines(List<String> lines) {
        for (String line : lines) {
            if (line.startsWith("> ")) {
                commandHistory.add(line.substring(2));
            }
        }
    }

    private void moveCommandHistory(int direction) {
        if (commandHistory.isEmpty()) {
            return;
        }

        if (direction < 0) {
            if (historyIndex < commandHistory.size() - 1) {
                if (historyIndex == -1) {
                    historyDraft = inputBuffer;
                }
                historyIndex++;
                inputBuffer = commandHistory.get(commandHistory.size() - 1 - historyIndex);
                scrollToBottom();
            }
        } else if (historyIndex > 0) {
            historyIndex--;
            inputBuffer = commandHistory.get(commandHistory.size() - 1 - historyIndex);
            scrollToBottom();
        } else if (historyIndex == 0) {
            historyIndex = -1;
            inputBuffer = historyDraft;
            scrollToBottom();
        }
    }

    private void sendCommand() {
        String command = inputBuffer.trim();
        if (command.isEmpty()) return;

        commandHistory.add(command);
        historyIndex = -1;
        historyDraft = "";

        pendingLine = "> " + command;
        inputBuffer = "";
        awaitingCommandResponse = true;

        rebuildWrappedHistory();
        scrollToBottom();

        BlockPos pos = getMenu().getBlockPos();
        PacketHandlerRegistry.INSTANCE.sendToServer(new ServerBoundDustyComputerCommandPacket(pos, command));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (isLoading()) {
            return true;
        }

        if (isMouseOverTextArea(mouseX, mouseY)) {
            int maxScroll = Math.max(0, totalLines() - maxVisibleLines());
            scrollOffset = (int) Math.max(0, Math.min(scrollOffset - delta, maxScroll));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isLoading()) {
            return true;
        }

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
        return mouseX >= leftPos + TEXT_LEFT && mouseX < leftPos + TEXT_LEFT + TEXT_WIDTH
                && mouseY >= topPos + TEXT_TOP && mouseY < topPos + TEXT_TOP + TEXT_HEIGHT;
    }

    @Nullable
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