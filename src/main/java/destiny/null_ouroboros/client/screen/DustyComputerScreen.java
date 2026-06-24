package destiny.null_ouroboros.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.client.network.ClientBoundDustyComputerSyncPacket;
import destiny.null_ouroboros.server.menu.DustyComputerMenu;
import destiny.null_ouroboros.server.block.entity.DustyComputerBlockEntity;
import destiny.null_ouroboros.server.network.ServerBoundDustyComputerCloseFileSessionPacket;
import destiny.null_ouroboros.server.network.ServerBoundDustyComputerCommandPacket;
import destiny.null_ouroboros.server.network.ServerBoundDustyComputerShutdownPacket;
import destiny.null_ouroboros.server.registry.PacketHandlerRegistry;
import destiny.null_ouroboros.server.registry.SoundRegistry;
import destiny.null_ouroboros.server.terminal.format.TerminusTextFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
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
    private static final int MC_KEY_RIGHT = 262;
    private static final int MC_KEY_LEFT = 263;
    private static final int MC_KEY_DOWN = 264;
    private static final int MC_KEY_UP = 265;

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
    private int cursorVisualIndex = 0;
    private int preferredCursorRelativeCol = 0;
    private int headerLineCount = 0;
    private int hintLineCount = 0;
    private long lastEditSoundMs = 0;

    private String currentPath = "T:\\";
    private String fileSessionPath = "";

    private enum TerminalSound {
        TYPE,
        ERASE,
        ENTER
    }

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

    private static boolean isUpKey(int keyCode, int scanCode) {
        return keyCode == MC_KEY_UP || keyCode == GLFW.GLFW_KEY_UP || keyCode == InputConstants.KEY_UP;
    }

    private static boolean isDownKey(int keyCode, int scanCode) {
        return keyCode == MC_KEY_DOWN || keyCode == GLFW.GLFW_KEY_DOWN || keyCode == InputConstants.KEY_DOWN;
    }

    private static boolean isLeftKey(int keyCode, int scanCode) {
        return keyCode == MC_KEY_LEFT || keyCode == GLFW.GLFW_KEY_LEFT || keyCode == InputConstants.KEY_LEFT;
    }

    private static boolean isRightKey(int keyCode, int scanCode) {
        return keyCode == MC_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == InputConstants.KEY_RIGHT;
    }

    private boolean inFileEdit() {
        if (fileSessionType == ClientBoundDustyComputerSyncPacket.FileSessionType.EDIT) {
            return true;
        }
        DustyComputerBlockEntity be = getBlockEntity();
        return be != null && be.getFileSessionType() == ClientBoundDustyComputerSyncPacket.FileSessionType.EDIT;
    }

    private boolean inFileView() {
        if (fileSessionType == ClientBoundDustyComputerSyncPacket.FileSessionType.VIEW) {
            return true;
        }
        DustyComputerBlockEntity be = getBlockEntity();
        return be != null && be.getFileSessionType() == ClientBoundDustyComputerSyncPacket.FileSessionType.VIEW;
    }

    private int logicalLineLength(int lineIndex) {
        if (lineIndex < 0 || lineIndex >= fileLines.size()) {
            return 0;
        }
        if (!inFileView()) {
            return fileLines.get(lineIndex).length();
        }
        int lastSegIdx = findLastSegmentIndexForLine(lineIndex);
        if (lastSegIdx < 0) {
            return 0;
        }
        return fileVisualSegments.get(lastSegIdx).logicalEndCol;
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
        fileBufferInitialized = true;
        rebuildFileVisualSegments();
        cursorLine = Math.max(0, fileLines.size() - 1);
        cursorColumn = logicalLineLength(cursorLine);
        updateCursorVisualIndex();
        updatePreferredCursorRelativeCol();
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
        headerLineCount = 0;
        hintLineCount = 0;
        for (FormattedCharSequence segment : font.split(headerComponent, TEXT_WIDTH)) {
            fileVisualSegments.add(new FileVisualSegment(segment, -1, 0, 0, true));
            headerLineCount++;
        }

        for (int lineIndex = 0; lineIndex < fileLines.size(); lineIndex++) {
            String line = fileLines.get(lineIndex);
            if (formatted) {
                appendFormattedLineSegments(font, line, lineIndex);
            } else {
                appendPlainEditLineSegments(font, line, lineIndex);
            }
        }

        hintLineCount = 0;
        List<FormattedCharSequence> hintWrapped = font.split(
                Component.translatable("message.null_ouroboros.terminus.file.close_hint"), TEXT_WIDTH);
        for (FormattedCharSequence segment : hintWrapped) {
            fileVisualSegments.add(new FileVisualSegment(segment, -1, 0, 0, true));
            hintLineCount++;
        }

        updateCursorVisualIndex();
    }

    private void rebuildFileVisualSegmentsForLine(int lineIndex) {
        if (!inFileSession()) return;
        if (fileSessionType == ClientBoundDustyComputerSyncPacket.FileSessionType.VIEW) {
            rebuildFileVisualSegments();
            return;
        }

        int removeFrom = -1;
        int removeTo = -1;
        for (int i = 0; i < fileVisualSegments.size(); i++) {
            FileVisualSegment seg = fileVisualSegments.get(i);
            if (!seg.chrome && seg.logicalLine == lineIndex) {
                if (removeFrom < 0) {
                    removeFrom = i;
                }
                removeTo = i;
            }
        }

        int insertAt = removeFrom >= 0 ? removeFrom : findInsertIndexForLine(lineIndex);
        if (removeFrom >= 0) {
            fileVisualSegments.subList(removeFrom, removeTo + 1).clear();
        }

        Font font = Minecraft.getInstance().font;
        appendPlainEditLineSegments(font, fileLines.get(lineIndex), lineIndex, insertAt);
        updateCursorVisualIndex();
    }

    private void rebuildContentSegments() {
        if (!inFileSession()) return;

        int contentStart = headerLineCount;
        int contentEnd = fileVisualSegments.size() - hintLineCount;
        if (contentEnd < contentStart) {
            rebuildFileVisualSegments();
            return;
        }
        fileVisualSegments.subList(contentStart, contentEnd).clear();

        Font font = Minecraft.getInstance().font;
        boolean formatted = fileSessionType == ClientBoundDustyComputerSyncPacket.FileSessionType.VIEW;
        int insertAt = contentStart;
        for (int lineIndex = 0; lineIndex < fileLines.size(); lineIndex++) {
            String line = fileLines.get(lineIndex);
            if (formatted) {
                insertAt = appendFormattedLineSegments(font, line, lineIndex, insertAt);
            } else {
                insertAt = appendPlainEditLineSegments(font, line, lineIndex, insertAt);
            }
        }

        updateCursorVisualIndex();
    }

    private int findInsertIndexForLine(int lineIndex) {
        for (int i = headerLineCount; i < fileVisualSegments.size() - hintLineCount; i++) {
            FileVisualSegment seg = fileVisualSegments.get(i);
            if (!seg.chrome && seg.logicalLine >= lineIndex) {
                return i;
            }
        }
        return Math.max(headerLineCount, fileVisualSegments.size() - hintLineCount);
    }

    private void updatePreferredCursorRelativeCol() {
        if (cursorVisualIndex >= 0 && cursorVisualIndex < fileVisualSegments.size()) {
            FileVisualSegment seg = fileVisualSegments.get(cursorVisualIndex);
            preferredCursorRelativeCol = Math.max(0, cursorColumn - seg.logicalStartCol);
        } else {
            preferredCursorRelativeCol = 0;
        }
    }

    private void updateCursorVisualIndex() {
        int found = findSegmentIndexForCursor(cursorLine, cursorColumn);
        if (found >= 0) {
            cursorVisualIndex = found;
        }
    }

    private int findSegmentIndexForCursor(int lineIndex, int column) {
        int firstSegIdx = -1;
        int lastSegIdx = -1;
        for (int i = 0; i < fileVisualSegments.size(); i++) {
            FileVisualSegment seg = fileVisualSegments.get(i);
            if (!seg.chrome && seg.logicalLine == lineIndex) {
                if (firstSegIdx < 0) {
                    firstSegIdx = i;
                }
                lastSegIdx = i;
            }
        }
        if (firstSegIdx < 0) {
            return -1;
        }

        for (int i = firstSegIdx; i <= lastSegIdx; i++) {
            FileVisualSegment seg = fileVisualSegments.get(i);
            if (column > seg.logicalStartCol && column < seg.logicalEndCol) {
                return i;
            }
        }

        if (cursorVisualIndex >= firstSegIdx && cursorVisualIndex <= lastSegIdx) {
            FileVisualSegment current = fileVisualSegments.get(cursorVisualIndex);
            if (!current.chrome && current.logicalLine == lineIndex
                    && column >= current.logicalStartCol && column <= current.logicalEndCol) {
                return cursorVisualIndex;
            }
        }

        for (int i = firstSegIdx; i <= lastSegIdx; i++) {
            FileVisualSegment seg = fileVisualSegments.get(i);
            if (column == seg.logicalStartCol) {
                return i;
            }
        }

        for (int i = firstSegIdx; i <= lastSegIdx; i++) {
            FileVisualSegment seg = fileVisualSegments.get(i);
            if (column == seg.logicalEndCol && i == lastSegIdx) {
                return i;
            }
        }

        FileVisualSegment lastSeg = fileVisualSegments.get(lastSegIdx);
        if (column > lastSeg.logicalEndCol) {
            return lastSegIdx;
        }
        return firstSegIdx;
    }

    private int findFirstSegmentIndexForLine(int lineIndex) {
        for (int i = 0; i < fileVisualSegments.size(); i++) {
            FileVisualSegment seg = fileVisualSegments.get(i);
            if (!seg.chrome && seg.logicalLine == lineIndex) {
                return i;
            }
        }
        return -1;
    }

    private int findLastSegmentIndexForLine(int lineIndex) {
        int last = -1;
        for (int i = 0; i < fileVisualSegments.size(); i++) {
            FileVisualSegment seg = fileVisualSegments.get(i);
            if (!seg.chrome && seg.logicalLine == lineIndex) {
                last = i;
            }
        }
        return last;
    }

    private int appendPlainEditLineSegments(Font font, String line, int lineIndex, int insertAt) {
        if (line.isEmpty()) {
            insertFileVisualSegment(
                    Component.literal("").withStyle(style -> style.withColor(0xFF0000)).getVisualOrderText(),
                    lineIndex, 0, 0, insertAt);
            return insertAt + 1;
        }

        int startCol = 0;
        while (startCol < line.length()) {
            String rest = line.substring(startCol);
            String slice = font.plainSubstrByWidth(rest, TEXT_WIDTH);
            if (slice.isEmpty()) {
                slice = rest.substring(0, 1);
            }

            FormattedCharSequence segment = Component.literal(slice)
                    .withStyle(style -> style.withColor(0xFF0000))
                    .getVisualOrderText();
            insertFileVisualSegment(segment, lineIndex, startCol, startCol + slice.length(), insertAt);
            insertAt++;
            startCol += slice.length();
        }
        return insertAt;
    }

    private void appendPlainEditLineSegments(Font font, String line, int lineIndex) {
        appendPlainEditLineSegments(font, line, lineIndex, fileVisualSegments.size() - hintLineCount);
    }

    private static String extractPlainText(FormattedCharSequence sequence) {
        StringBuilder sb = new StringBuilder();
        sequence.accept((index, style, codePoint) -> {
            sb.appendCodePoint(codePoint);
            return true;
        });
        return sb.toString();
    }

    private int appendFormattedLineSegments(Font font, String line, int lineIndex, int insertAt) {
        Component lineComponent = TerminusTextFormat.parseForDisplay(line);
        List<FormattedCharSequence> wrapped = font.split(lineComponent, TEXT_WIDTH);
        int col = 0;
        for (FormattedCharSequence segment : wrapped) {
            int segmentLen = extractPlainText(segment).length();
            insertFileVisualSegment(segment, lineIndex, col, col + segmentLen, insertAt);
            insertAt++;
            col += segmentLen;
        }
        if (wrapped.isEmpty()) {
            insertFileVisualSegment(Component.empty().getVisualOrderText(), lineIndex, 0, 0, insertAt);
            insertAt++;
        }
        return insertAt;
    }

    private void appendFormattedLineSegments(Font font, String line, int lineIndex) {
        appendFormattedLineSegments(font, line, lineIndex, fileVisualSegments.size() - hintLineCount);
    }

    private void insertFileVisualSegment(FormattedCharSequence text, int logicalLine, int logicalStartCol,
                                         int logicalEndCol, int insertAt) {
        fileVisualSegments.add(insertAt, new FileVisualSegment(text, logicalLine, logicalStartCol, logicalEndCol, false));
    }

    private void playEditSound(boolean typing) {
        playTerminalSound(typing ? TerminalSound.TYPE : TerminalSound.ERASE);
    }

    private void playTerminalSound(TerminalSound sound) {
        playTerminalSound(sound, sound != TerminalSound.ENTER);
    }

    private void playTerminalSound(TerminalSound sound, boolean throttle) {
        long now = System.currentTimeMillis();
        if (throttle && now - lastEditSoundMs < 50) {
            return;
        }
        if (throttle) {
            lastEditSoundMs = now;
        }
        if (minecraft == null || minecraft.getSoundManager() == null) {
            return;
        }
        var soundEvent = switch (sound) {
            case TYPE -> SoundRegistry.DUSTY_COMPUTER_TYPE.get();
            case ERASE -> SoundRegistry.DUSTY_COMPUTER_ERASE.get();
            case ENTER -> SoundRegistry.DUSTY_COMPUTER_ENTER.get();
        };
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(soundEvent, 1.0F));
    }

    private static boolean hasControl(int modifiers) {
        return (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
    }

    private static void drawTerminalString(GuiGraphics graphics, Font font, FormattedCharSequence text, int x, int y) {
        graphics.drawString(font, text, x, y, 0xFF0000, false);
    }

    private static void drawTerminalString(GuiGraphics graphics, Font font, String text, int x, int y) {
        graphics.drawString(font, text, x, y, 0xFF0000, false);
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

    private int contentSegmentCount() {
        return Math.max(0, fileVisualSegments.size() - headerLineCount - hintLineCount);
    }

    private int maxContentVisibleLines() {
        if (inFileSession()) {
            return Math.max(1, maxVisibleLines() - headerLineCount - hintLineCount);
        }
        return maxVisibleLines();
    }

    private int maxContentScroll() {
        if (inFileSession()) {
            return Math.max(0, contentSegmentCount() - maxContentVisibleLines());
        }
        return Math.max(0, totalLines() - maxVisibleLines());
    }

    private boolean isContentVisualIndex(int visualIndex) {
        return visualIndex >= headerLineCount && visualIndex < fileVisualSegments.size() - hintLineCount;
    }

    private void scrollToBottom() {
        scrollOffset = maxContentScroll();
    }

    private void scrollAfterEdit() {
        if (!inFileSession()) {
            return;
        }
        if (!isContentVisualIndex(cursorVisualIndex)) {
            return;
        }
        int contentScrollIndex = cursorVisualIndex - headerLineCount;
        int visible = maxContentVisibleLines();
        int maxScroll = maxContentScroll();

        if (contentScrollIndex < scrollOffset) {
            scrollOffset = contentScrollIndex;
        } else if (contentScrollIndex >= scrollOffset + visible) {
            scrollOffset = contentScrollIndex - visible + 1;
        }

        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
    }

    private void drawFileCursor(GuiGraphics graphics, Font font, FileVisualSegment seg, int x, int drawY, int lineHeight) {
        int drawCol = Math.min(cursorColumn, seg.logicalEndCol);
        int relCol = Math.max(0, drawCol - seg.logicalStartCol);
        String before;
        if (inFileView()) {
            String segmentText = extractPlainText(seg.text);
            before = segmentText.substring(0, Math.min(relCol, segmentText.length()));
        } else {
            String line = fileLines.get(cursorLine);
            before = line.substring(seg.logicalStartCol, drawCol);
        }
        int cursorX = x + font.width(before);
        int charWidth = font.width("_");
        graphics.fill(cursorX, drawY + lineHeight - 2, cursorX + charWidth, drawY + lineHeight - 1, 0xFFFF0000);
    }

    private void renderFileSessionText(GuiGraphics graphics, Font font, int x, int yBase, int lineHeight, int maxLines) {
        for (int i = 0; i < headerLineCount; i++) {
            FileVisualSegment seg = fileVisualSegments.get(i);
            drawTerminalString(graphics, font, seg.text, x, yBase + i * lineHeight);
        }

        int hintStart = fileVisualSegments.size() - hintLineCount;
        for (int i = 0; i < hintLineCount; i++) {
            FileVisualSegment seg = fileVisualSegments.get(hintStart + i);
            drawTerminalString(graphics, font, seg.text, x, yBase + (maxLines - hintLineCount + i) * lineHeight);
        }

        int contentVisible = maxContentVisibleLines();
        for (int contentVisualIndex = headerLineCount; contentVisualIndex < hintStart; contentVisualIndex++) {
            int contentScrollIndex = contentVisualIndex - headerLineCount;
            if (contentScrollIndex < scrollOffset || contentScrollIndex >= scrollOffset + contentVisible) {
                continue;
            }

            FileVisualSegment seg = fileVisualSegments.get(contentVisualIndex);
            int drawY = yBase + (headerLineCount + (contentScrollIndex - scrollOffset)) * lineHeight;
            drawTerminalString(graphics, font, seg.text, x, drawY);

            if (contentVisualIndex == cursorVisualIndex && cursorBlink % 20 < 10) {
                drawFileCursor(graphics, font, seg, x, drawY, lineHeight);
            }
        }
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
                    drawTerminalString(graphics, font, wrappedHistory.get(i), x, drawY);
                }
            }

            int inputLineIndex = totalLines() - 1;
            if (inputLineIndex >= startIndex && inputLineIndex < endIndex) {
                int drawY = yBase + (inputLineIndex - startIndex) * lineHeight;
                drawTerminalString(graphics, font, getInputLine(), x, drawY);

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
            renderFileSessionText(graphics, font, x, yBase, lineHeight, maxLines);
        }

        disableScissor();
    }

    private void renderLoadingScreen(GuiGraphics graphics) {
        Font font = Minecraft.getInstance().font;
        int xBase = leftPos + TEXT_LEFT;
        int yBase = topPos + TEXT_TOP;
        int lineHeight = font.lineHeight;
        int gapLines = 1;

        enableScissor(xBase, yBase, TEXT_WIDTH, TEXT_HEIGHT);

        int contentLines = LOADING_LOGO.length + gapLines + 1 + gapLines + 1 + gapLines + LOADING_EYE.length;
        int contentHeight = contentLines * lineHeight;
        int startY = yBase + (TEXT_HEIGHT - contentHeight) / 2 - lineHeight;

        int y = startY;
        int logoCharCount = maxCharCount(LOADING_LOGO);
        for (String line : LOADING_LOGO) {
            drawMonospaceLine(graphics, font, line, xBase, TEXT_WIDTH, logoCharCount, y);
            y += lineHeight;
        }

        y += gapLines * lineHeight;

        String spinner = String.valueOf(LOADING_SPINNER.charAt((cursorBlink / 4) % LOADING_SPINNER.length()));
        drawTerminalString(graphics, font, spinner, centeredX(font, spinner, xBase, TEXT_WIDTH), y);
        y += lineHeight;

        y += gapLines * lineHeight;

        long elapsedTicks = minecraft.level.getGameTime() - menu.getPoweredOnGameTime();
        int loadingFrame = (int) Math.min(LOADING_MESSAGES.length - 1, elapsedTicks / LOADING_MESSAGE_TICKS);
        int loadingAnchorX = xBase + (TEXT_WIDTH - font.width(LOADING_MESSAGES[2])) / 2;
        drawTerminalString(graphics, font, LOADING_MESSAGES[loadingFrame], loadingAnchorX, y);
        y += lineHeight;

        y += gapLines * lineHeight;

        int eyeCharCount = maxCharCount(LOADING_EYE);
        for (String line : LOADING_EYE) {
            drawMonospaceLine(graphics, font, line, xBase, TEXT_WIDTH, eyeCharCount, y);
            y += lineHeight;
        }

        drawTerminalString(graphics, font, LOADING_VERSION, xBase + TEXT_WIDTH - font.width(LOADING_VERSION), yBase + TEXT_HEIGHT - lineHeight);

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
                                          int refCharCount, int y) {
        int charAdvance = font.width("W");
        int gridWidth = refCharCount * charAdvance;
        int startX = areaLeft + (areaWidth - gridWidth) / 2;

        for (int i = 0; i < line.length(); i++) {
            drawTerminalString(graphics, font, String.valueOf(line.charAt(i)), startX + i * charAdvance, y);
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
            if (keyCode == GLFW.GLFW_KEY_LEFT_CONTROL) {
                playTerminalSound(TerminalSound.TYPE, false);
                return true;
            }

            if (keyCode == GLFW.GLFW_KEY_R && hasControl(modifiers)) {
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
            } else if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER
                    || keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                return true;
            }

            if (isLeftKey(keyCode, scanCode)) {
                moveCursorHorizontal(-1);
                return true;
            }
            if (isRightKey(keyCode, scanCode)) {
                moveCursorHorizontal(1);
                return true;
            }
            if (isUpKey(keyCode, scanCode)) {
                moveCursorVertical(-1);
                return true;
            }
            if (isDownKey(keyCode, scanCode)) {
                moveCursorVertical(1);
                return true;
            }

            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        if (isUpKey(keyCode, scanCode)) {
            moveCommandHistory(-1);
            return true;
        }
        if (isDownKey(keyCode, scanCode)) {
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
            if (hasControl(modifiers) && (codePoint == 'R' || codePoint == 'r')) {
                return true;
            }
            if (inFileEdit() && codePoint >= 32 && codePoint != 127) {
                insertChar(codePoint);
                return true;
            }
            if (codePoint >= 32 && codePoint != 127) {
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
        rebuildFileVisualSegmentsForLine(cursorLine);
        updateCursorVisualIndex();
        updatePreferredCursorRelativeCol();
        scrollAfterEdit();
        playEditSound(true);
    }

    private void insertLineBreak() {
        String line = fileLines.get(cursorLine);
        String before = line.substring(0, cursorColumn);
        String after = line.substring(cursorColumn);
        fileLines.set(cursorLine, before);
        fileLines.add(cursorLine + 1, after);
        cursorLine++;
        cursorColumn = 0;
        rebuildContentSegments();
        updateCursorVisualIndex();
        updatePreferredCursorRelativeCol();
        scrollAfterEdit();
        playEditSound(true);
    }

    private void deleteBackward() {
        if (cursorColumn > 0) {
            String line = fileLines.get(cursorLine);
            fileLines.set(cursorLine, line.substring(0, cursorColumn - 1) + line.substring(cursorColumn));
            cursorColumn--;
            rebuildFileVisualSegmentsForLine(cursorLine);
        } else if (cursorLine > 0) {
            String prev = fileLines.get(cursorLine - 1);
            String current = fileLines.get(cursorLine);
            cursorColumn = prev.length();
            fileLines.set(cursorLine - 1, prev + current);
            fileLines.remove(cursorLine);
            cursorLine--;
            rebuildContentSegments();
        } else {
            return;
        }
        updateCursorVisualIndex();
        updatePreferredCursorRelativeCol();
        scrollAfterEdit();
        playEditSound(false);
    }

    private void moveCursorHorizontal(int deltaCol) {
        int previousLine = cursorLine;
        int previousColumn = cursorColumn;
        int previousVisualIndex = cursorVisualIndex;

        if (deltaCol < 0) {
            if (cursorColumn > 0) {
                cursorColumn--;
            } else if (cursorLine > 0) {
                cursorLine--;
                cursorColumn = logicalLineLength(cursorLine);
            }
        } else {
            if (cursorColumn < logicalLineLength(cursorLine)) {
                cursorColumn++;
            } else if (cursorLine < fileLines.size() - 1) {
                cursorLine++;
                cursorColumn = 0;
            }
        }

        if (cursorLine != previousLine || cursorColumn != previousColumn) {
            if (deltaCol > 0 && cursorLine == previousLine
                    && previousVisualIndex >= 0 && previousVisualIndex < fileVisualSegments.size()) {
                FileVisualSegment fromSeg = fileVisualSegments.get(previousVisualIndex);
                if (!fromSeg.chrome && fromSeg.logicalLine == cursorLine
                        && cursorColumn == fromSeg.logicalEndCol
                        && previousVisualIndex + 1 < fileVisualSegments.size()) {
                    FileVisualSegment nextSeg = fileVisualSegments.get(previousVisualIndex + 1);
                    if (nextSeg.logicalLine == cursorLine && nextSeg.logicalStartCol == cursorColumn) {
                        cursorVisualIndex = previousVisualIndex + 1;
                        updatePreferredCursorRelativeCol();
                        playTerminalSound(TerminalSound.TYPE);
                        scrollAfterEdit();
                        return;
                    }
                }
            }

            updateCursorVisualIndex();
            updatePreferredCursorRelativeCol();
            playTerminalSound(TerminalSound.TYPE);
        }
        scrollAfterEdit();
    }

    private void moveCursorVertical(int direction) {
        int previousLine = cursorLine;
        int previousColumn = cursorColumn;
        int previousVisualIndex = cursorVisualIndex;

        if (cursorVisualIndex < 0 || cursorVisualIndex >= fileVisualSegments.size()) {
            return;
        }

        FileVisualSegment currentSeg = fileVisualSegments.get(cursorVisualIndex);
        if (currentSeg.chrome) {
            return;
        }

        int lineIndex = currentSeg.logicalLine;
        int firstSegIdx = findFirstSegmentIndexForLine(lineIndex);
        int lastSegIdx = findLastSegmentIndexForLine(lineIndex);
        if (firstSegIdx < 0) {
            return;
        }

        if (direction < 0) {
            if (cursorVisualIndex > firstSegIdx) {
                int targetIdx = cursorVisualIndex - 1;
                FileVisualSegment prevSeg = fileVisualSegments.get(targetIdx);
                FileVisualSegment lowerSeg = fileVisualSegments.get(cursorVisualIndex);
                int maxCol = prevSeg.logicalEndCol;
                if (lowerSeg.logicalLine == lineIndex && lowerSeg.logicalStartCol == prevSeg.logicalEndCol) {
                    maxCol = prevSeg.logicalEndCol - 1;
                }
                cursorLine = lineIndex;
                cursorVisualIndex = targetIdx;
                cursorColumn = Math.max(prevSeg.logicalStartCol, Math.min(prevSeg.logicalStartCol + preferredCursorRelativeCol, maxCol));
            } else if (lineIndex > 0) {
                cursorLine = lineIndex - 1;
                int targetIdx = findLastSegmentIndexForLine(cursorLine);
                if (targetIdx >= 0) {
                    FileVisualSegment targetSeg = fileVisualSegments.get(targetIdx);
                    cursorVisualIndex = targetIdx;
                    cursorColumn = Math.max(targetSeg.logicalStartCol, Math.min(targetSeg.logicalStartCol + preferredCursorRelativeCol, targetSeg.logicalEndCol));
                } else {
                    cursorColumn = Math.min(preferredCursorRelativeCol, logicalLineLength(cursorLine));
                    updateCursorVisualIndex();
                }
            } else {
                return;
            }
        } else {
            if (cursorVisualIndex < lastSegIdx) {
                int targetIdx = cursorVisualIndex + 1;
                FileVisualSegment nextSeg = fileVisualSegments.get(targetIdx);
                int maxCol = targetIdx < lastSegIdx ? nextSeg.logicalEndCol - 1 : nextSeg.logicalEndCol;
                cursorLine = lineIndex;
                cursorVisualIndex = targetIdx;
                cursorColumn = Math.max(
                        nextSeg.logicalStartCol,
                        Math.min(nextSeg.logicalStartCol + preferredCursorRelativeCol, maxCol));
                cursorColumn = Math.min(cursorColumn, logicalLineLength(cursorLine));
            } else if (lineIndex < fileLines.size() - 1) {
                cursorLine = lineIndex + 1;
                int targetIdx = findFirstSegmentIndexForLine(cursorLine);
                if (targetIdx >= 0) {
                    FileVisualSegment targetSeg = fileVisualSegments.get(targetIdx);
                    cursorVisualIndex = targetIdx;
                    int maxCol = targetIdx < findLastSegmentIndexForLine(cursorLine) ? targetSeg.logicalEndCol - 1 : targetSeg.logicalEndCol;
                    cursorColumn = Math.max(targetSeg.logicalStartCol, Math.min(targetSeg.logicalStartCol + preferredCursorRelativeCol, maxCol));
                } else {
                    cursorColumn = Math.min(preferredCursorRelativeCol, logicalLineLength(cursorLine));
                    updateCursorVisualIndex();
                }
            } else {
                return;
            }
        }

        if (cursorLine != previousLine || cursorColumn != previousColumn || cursorVisualIndex != previousVisualIndex) {
            updatePreferredCursorRelativeCol();
            playTerminalSound(TerminalSound.TYPE);
        }
        scrollAfterEdit();
    }

    private void closeFileSession() {
        playTerminalSound(TerminalSound.ENTER, false);
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

        String previousInput = inputBuffer;

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

        if (!inputBuffer.equals(previousInput)) {
            playTerminalSound(TerminalSound.TYPE);
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
            scrollOffset = (int) Math.max(0, Math.min(scrollOffset - delta, maxContentScroll()));
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