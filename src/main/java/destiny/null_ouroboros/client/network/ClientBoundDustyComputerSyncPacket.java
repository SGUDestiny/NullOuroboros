package destiny.null_ouroboros.client.network;

import destiny.null_ouroboros.server.block.entity.DustyComputerBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ClientBoundDustyComputerSyncPacket {
    public enum FileSessionType {
        NONE,
        VIEW,
        EDIT
    }

    private final BlockPos pos;
    private final List<String> lines;
    private final String currentPath;
    private final FileSessionType fileSessionType;
    private final String fileContent;
    private final String filePath;
    private final boolean p2pActive;
    private final String p2pPeerDisplay;
    private final String p2pSendMode;
    private final boolean loadingActive;
    private final int loadingPercent;
    private final String loadingMessageKey;
    private final String clipboardText;

    public ClientBoundDustyComputerSyncPacket(BlockPos pos, List<String> lines, String currentPath,
                                                FileSessionType fileSessionType, String fileContent, String filePath) {
        this(pos, lines, currentPath, fileSessionType, fileContent, filePath, false, "", "MSG", false, 0, "", "");
    }

    public ClientBoundDustyComputerSyncPacket(BlockPos pos, List<String> lines, String currentPath,
                                                FileSessionType fileSessionType, String fileContent, String filePath,
                                                boolean p2pActive, String p2pPeerDisplay, String p2pSendMode,
                                                boolean loadingActive, int loadingPercent, String loadingMessageKey) {
        this(pos, lines, currentPath, fileSessionType, fileContent, filePath,
                p2pActive, p2pPeerDisplay, p2pSendMode, loadingActive, loadingPercent, loadingMessageKey, "");
    }

    public ClientBoundDustyComputerSyncPacket(BlockPos pos, List<String> lines, String currentPath,
                                                FileSessionType fileSessionType, String fileContent, String filePath,
                                                boolean p2pActive, String p2pPeerDisplay, String p2pSendMode,
                                                boolean loadingActive, int loadingPercent, String loadingMessageKey,
                                                String clipboardText) {
        this.pos = pos;
        this.lines = lines;
        this.currentPath = currentPath;
        this.fileSessionType = fileSessionType;
        this.fileContent = fileContent;
        this.filePath = filePath;
        this.p2pActive = p2pActive;
        this.p2pPeerDisplay = p2pPeerDisplay;
        this.p2pSendMode = p2pSendMode;
        this.loadingActive = loadingActive;
        this.loadingPercent = loadingPercent;
        this.loadingMessageKey = loadingMessageKey;
        this.clipboardText = clipboardText;
    }

    public static void encode(ClientBoundDustyComputerSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeCollection(msg.lines, FriendlyByteBuf::writeUtf);
        buf.writeUtf(msg.currentPath);
        buf.writeEnum(msg.fileSessionType);
        if (msg.fileSessionType != FileSessionType.NONE) {
            buf.writeUtf(msg.fileContent);
            buf.writeUtf(msg.filePath);
        }
        buf.writeBoolean(msg.p2pActive);
        buf.writeUtf(msg.p2pPeerDisplay);
        buf.writeUtf(msg.p2pSendMode);
        buf.writeBoolean(msg.loadingActive);
        buf.writeInt(msg.loadingPercent);
        buf.writeUtf(msg.loadingMessageKey);
        buf.writeUtf(msg.clipboardText);
    }

    public static ClientBoundDustyComputerSyncPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        List<String> lines = buf.readCollection(ArrayList::new, FriendlyByteBuf::readUtf);
        String currentPath = buf.readUtf();
        FileSessionType fileSessionType = buf.readEnum(FileSessionType.class);
        String fileContent = "";
        String filePath = "";
        if (fileSessionType != FileSessionType.NONE) {
            fileContent = buf.readUtf();
            filePath = buf.readUtf();
        }
        boolean p2pActive = buf.readBoolean();
        String p2pPeerDisplay = buf.readUtf();
        String p2pSendMode = buf.readUtf();
        boolean loadingActive = buf.readBoolean();
        int loadingPercent = buf.readInt();
        String loadingMessageKey = buf.readUtf();
        String clipboardText = buf.readUtf();
        return new ClientBoundDustyComputerSyncPacket(pos, lines, currentPath, fileSessionType, fileContent, filePath,
                p2pActive, p2pPeerDisplay, p2pSendMode, loadingActive, loadingPercent, loadingMessageKey, clipboardText);
    }

    public static boolean handle(ClientBoundDustyComputerSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var level = Minecraft.getInstance().level;
            if (level == null) return;
            var be = level.getBlockEntity(msg.pos);
            if (be instanceof DustyComputerBlockEntity computer) {
                computer.setLines(msg.lines);
                computer.setCurrentPath(msg.currentPath);
                computer.setFileSessionState(msg.fileSessionType, msg.fileContent, msg.filePath);
                computer.setP2pClientState(msg.p2pActive, msg.p2pPeerDisplay, msg.p2pSendMode,
                        msg.loadingActive, msg.loadingPercent, msg.loadingMessageKey);
                if (!msg.clipboardText.isEmpty()) {
                    Minecraft.getInstance().keyboardHandler.setClipboard(msg.clipboardText);
                }
            }
        });
        return true;
    }
}
