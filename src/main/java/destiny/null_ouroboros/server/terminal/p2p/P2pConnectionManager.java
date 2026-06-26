package destiny.null_ouroboros.server.terminal.p2p;

import destiny.null_ouroboros.server.block.entity.DustyComputerBlockEntity;
import destiny.null_ouroboros.server.registry.SoundRegistry;
import destiny.null_ouroboros.server.terminal.TerminusLoadingBar;
import destiny.null_ouroboros.server.terminal.TerminusSession;
import destiny.null_ouroboros.server.terminal.command.P2pIncomingRequestCommand;
import destiny.null_ouroboros.server.terminal.command.P2pIncomingTransferCommand;
import destiny.null_ouroboros.server.terminal.command.P2pTransferLoadingCommand;
import destiny.null_ouroboros.server.terminal.filesystem.ComputerRecord;
import destiny.null_ouroboros.server.terminal.filesystem.FileSystemException;
import destiny.null_ouroboros.server.terminal.filesystem.TerminusDirectory;
import destiny.null_ouroboros.server.terminal.filesystem.TerminusFileSystem;
import destiny.null_ouroboros.server.terminal.filesystem.TerminusNode;
import destiny.null_ouroboros.server.terminal.filesystem.TerminusSavedData;
import destiny.null_ouroboros.server.terminal.filesystem.TerminusTextFile;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public final class P2pConnectionManager {
    private static final int REQUEST_TIMEOUT_TICKS = 300;

    public enum DisconnectCause {
        LOCAL_CLOSED,
        LOST,
        LOCAL_FILTER
    }

    private record PendingRequest(String requester, String target, long expiryTick, ServerLevel level) {}

    private static final class Connection {
        private final String a;
        private final String b;
        private final ServerLevel level;
        private final P2pLogger loggerA;
        private final P2pLogger loggerB;
        @Nullable
        private Transfer transfer;

        private Connection(String a, String b, ServerLevel level, P2pLogger loggerA, P2pLogger loggerB) {
            this.a = a;
            this.b = b;
            this.level = level;
            this.loggerA = loggerA;
            this.loggerB = loggerB;
        }

        private String other(String one) {
            return one.equals(a) ? b : a;
        }

        private P2pLogger logger(String one) {
            return one.equals(a) ? loggerA : loggerB;
        }
    }

    public static final class Transfer {
        private final String sender;
        private final String receiver;
        private final String sourcePath;
        private final String mode;
        private final TerminusLoadingBar loadingBar;
        private int senderBarLine = -1;
        private int receiverBarLine = -1;

        private Transfer(String sender, String receiver, String sourcePath, String mode, TerminusLoadingBar loadingBar) {
            this.sender = sender;
            this.receiver = receiver;
            this.sourcePath = sourcePath;
            this.mode = mode;
            this.loadingBar = loadingBar;
        }
    }

    private static final Map<String, Connection> connections = new HashMap<>();
    private static final Map<String, PendingRequest> pendingByTarget = new HashMap<>();
    private static long lastTick = -1L;

    private P2pConnectionManager() {}

    public static void requestConnection(ServerLevel level, String requester, String target) {
        TerminusSavedData data = TerminusSavedData.get(level);
        if (data == null) return;
        ComputerRecord requesterRecord = data.getByIpvInf(requester);
        ComputerRecord targetRecord = data.getByIpvInf(target);
        if (requesterRecord == null || requesterRecord.getEndpoint() == null) {
            return;
        }
        TerminusSession requesterSession = data.getOrCreateSession(requester, requesterRecord.getEndpoint().pos());

        if (requesterRecord == null || targetRecord == null || targetRecord.getEndpoint() == null) {
            requesterSession.addLine(Component.translatable("message.null_ouroboros.terminus.p2p.not_found").getString());
            sync(level, data, requester);
            return;
        }
        if (connections.containsKey(requester) || connections.containsKey(target)) {
            requesterSession.addLine(Component.translatable("message.null_ouroboros.terminus.p2p.busy").getString());
            sync(level, data, requester);
            return;
        }
        if (!level.dimension().location().equals(targetRecord.getEndpoint().dimension())) {
            requesterSession.addLine(errorLine("message.null_ouroboros.terminus.p2p.error"));
            sync(level, data, requester);
            return;
        }
        TerminusFileSystem targetFs = targetRecord.getFileSystem();
        if (!targetRecord.getP2pSettings().passesFilter(requester, targetFs)) {
            requesterSession.addLine(errorLine("message.null_ouroboros.terminus.p2p.error"));
            sync(level, data, requester);
            return;
        }

        PendingRequest pending = new PendingRequest(requester, target, level.getGameTime() + REQUEST_TIMEOUT_TICKS, level);
        pendingByTarget.put(target, pending);
        requesterSession.addLine(Component.translatable("message.null_ouroboros.terminus.p2p.request_sent").getString());
        sync(level, data, requester);

        TerminusSession targetSession = data.getOrCreateSession(target, targetRecord.getEndpoint().pos());
        targetSession.setActiveCommand(new P2pIncomingRequestCommand(targetFs, targetRecord.getEndpoint().pos(), level, requester, target));
        String requesterDisplay = requesterRecord.getP2pSettings().displayName(requester);
        targetSession.addLine(Component.translatable("message.null_ouroboros.terminus.p2p.incoming", requesterDisplay).getString());
        targetSession.addLine(Component.translatable("message.null_ouroboros.terminus.rm.confirm_hint").getString());
        sync(level, data, target);
    }

    public static void acceptRequest(ServerLevel level, String target, String requester) {
        PendingRequest pending = pendingByTarget.remove(target);
        if (pending == null || !pending.requester().equals(requester)) {
            return;
        }
        TerminusSavedData data = TerminusSavedData.get(level);
        if (data == null) return;
        ComputerRecord requesterRecord = data.getByIpvInf(requester);
        ComputerRecord targetRecord = data.getByIpvInf(target);
        if (requesterRecord == null || targetRecord == null || requesterRecord.getEndpoint() == null || targetRecord.getEndpoint() == null) {
            return;
        }
        if (!requesterRecord.getEndpoint().dimension().equals(targetRecord.getEndpoint().dimension())) {
            data.getOrCreateSession(requester, requesterRecord.getEndpoint().pos())
                    .addLine(errorLine("message.null_ouroboros.terminus.p2p.error"));
            sync(level, data, requester);
            return;
        }
        TerminusFileSystem requesterFs = requesterRecord.getFileSystem();
        TerminusFileSystem targetFs = targetRecord.getFileSystem();
        Connection connection = new Connection(
                requester,
                target,
                level,
                P2pLogger.start(requesterFs, requesterRecord.getP2pSettings(), requester),
                P2pLogger.start(targetFs, targetRecord.getP2pSettings(), requester)
        );
        connections.put(requester, connection);
        connections.put(target, connection);

        data.getOrCreateSession(requester, requesterRecord.getEndpoint().pos())
                .beginP2pSession(target, targetRecord.getP2pSettings().displayName(target), requesterRecord.getP2pSettings().displayName(requester));
        TerminusSession targetSession = data.getOrCreateSession(target, targetRecord.getEndpoint().pos());
        targetSession.clearActiveCommand();
        targetSession.beginP2pSession(requester, requesterRecord.getP2pSettings().displayName(requester), targetRecord.getP2pSettings().displayName(target));
        sync(level, data, requester);
        sync(level, data, target);
    }

    public static void rejectRequest(ServerLevel level, String target, String requester) {
        pendingByTarget.remove(target);
        TerminusSavedData data = TerminusSavedData.get(level);
        if (data == null) return;
        ComputerRecord requesterRecord = data.getByIpvInf(requester);
        if (requesterRecord != null && requesterRecord.getEndpoint() != null) {
            data.getOrCreateSession(requester, requesterRecord.getEndpoint().pos())
                    .addLine(Component.translatable("message.null_ouroboros.terminus.p2p.rejected").getString());
            sync(level, data, requester);
        }
    }

    public static void sendMessage(String from, String message) {
        Connection connection = connections.get(from);
        if (connection == null) return;
        ServerLevel level = connection.level;
        TerminusSavedData data = TerminusSavedData.get(level);
        if (data == null || !validateLiveConnection(data, connection, from)) return;

        String to = connection.other(from);
        ComputerRecord fromRecord = data.getByIpvInf(from);
        ComputerRecord toRecord = data.getByIpvInf(to);
        if (fromRecord == null || toRecord == null || toRecord.getEndpoint() == null) return;

        String alias = fromRecord.getP2pSettings().displayName(from);
        data.getOrCreateSession(to, toRecord.getEndpoint().pos())
                .addP2pLine(Component.translatable("message.null_ouroboros.terminus.p2p.message", alias, message).getString());
        connection.logger(from).message(fromRecord.getFileSystem(), alias, message);
        connection.logger(to).message(toRecord.getFileSystem(), alias, message);
        level.playSound(null, toRecord.getEndpoint().pos(), SoundRegistry.DUSTY_COMPUTER_LOAD_SHORT.get(), SoundSource.BLOCKS, 0.5f, 1.0f);
        sync(level, data, to);
    }

    public static void startTransferRequest(ServerLevel level, String sender, String sourcePath, String mode) {
        Connection connection = connections.get(sender);
        if (connection == null || connection.transfer != null) return;
        TerminusSavedData data = TerminusSavedData.get(level);
        if (data == null || !validateLiveConnection(data, connection, sender)) return;
        String receiver = connection.other(sender);
        ComputerRecord receiverRecord = data.getByIpvInf(receiver);
        ComputerRecord senderRecord = data.getByIpvInf(sender);
        if (receiverRecord == null || receiverRecord.getEndpoint() == null || senderRecord == null) return;
        TerminusNode node = senderRecord.getFileSystem().resolvePath(sourcePath);
        if (!(node instanceof TerminusTextFile)) {
            data.getOrCreateSession(sender, senderRecord.getEndpoint().pos()).addLine(Component.translatable("message.null_ouroboros.terminus.p2p.send.not_file").getString());
            sync(level, data, sender);
            return;
        }
        TerminusSession receiverSession = data.getOrCreateSession(receiver, receiverRecord.getEndpoint().pos());
        receiverSession.setActiveCommand(new P2pIncomingTransferCommand(receiverRecord.getFileSystem(), receiverRecord.getEndpoint().pos(), level, sender, receiver, sourcePath, mode));
        receiverSession.addLine(Component.translatable("message.null_ouroboros.terminus.p2p.transfer.incoming", sourcePath).getString());
        receiverSession.addLine(Component.translatable("message.null_ouroboros.terminus.rm.confirm_hint").getString());
        data.getOrCreateSession(sender, senderRecord.getEndpoint().pos())
                .addLine(Component.translatable("message.null_ouroboros.terminus.p2p.transfer.awaiting_confirmation").getString());
        sync(level, data, sender);
        sync(level, data, receiver);
    }

    public static void acceptTransfer(ServerLevel level, String sender, String receiver, String sourcePath, String mode) {
        Connection connection = connections.get(sender);
        if (connection == null || connection.transfer != null) return;
        TerminusSavedData data = TerminusSavedData.get(level);
        if (data == null) return;
        ComputerRecord senderRecord = data.getByIpvInf(sender);
        ComputerRecord receiverRecord = data.getByIpvInf(receiver);
        if (senderRecord == null || receiverRecord == null || senderRecord.getEndpoint() == null || receiverRecord.getEndpoint() == null) return;
        Transfer transfer = new Transfer(sender, receiver, sourcePath, mode, new TerminusLoadingBar(level));
        connection.transfer = transfer;
        beginTransferLoading(level, data, connection, transfer);
    }

    public static void rejectTransfer(ServerLevel level, String sender, String receiver) {
        Connection connection = connections.get(sender);
        if (connection == null) return;
        TerminusSavedData data = TerminusSavedData.get(level);
        if (data == null) return;
        ComputerRecord senderRecord = data.getByIpvInf(sender);
        if (senderRecord == null || senderRecord.getEndpoint() == null) return;
        data.getOrCreateSession(sender, senderRecord.getEndpoint().pos())
                .addLine(Component.translatable("message.null_ouroboros.terminus.p2p.transfer.rejected").getString());
        sync(level, data, sender);
    }

    public static void cancelTransfer(String computer) {
        Connection connection = connections.get(computer);
        if (connection == null || connection.transfer == null) return;
        ServerLevel level = connection.level;
        TerminusSavedData data = TerminusSavedData.get(level);
        if (data == null) return;
        finishTransfer(level, data, connection, false, Component.translatable("message.null_ouroboros.terminus.p2p.transfer.aborted").getString());
    }

    public static void disconnect(String computer, DisconnectCause cause) {
        Connection connection = connections.remove(computer);
        if (connection == null) return;
        String other = connection.other(computer);
        connections.remove(other);
        ServerLevel level = connection.level;
        TerminusSavedData data = TerminusSavedData.get(level);
        if (data == null) return;

        closeSide(level, data, connection, computer, cause == DisconnectCause.LOCAL_CLOSED
                ? P2pLogger.Reason.CLOSED_BY_LOCAL
                : cause == DisconnectCause.LOCAL_FILTER ? P2pLogger.Reason.LOCAL_FILTER : P2pLogger.Reason.LOST);
        closeSide(level, data, connection, other, cause == DisconnectCause.LOCAL_CLOSED
                ? P2pLogger.Reason.CLOSED_BY_REMOTE
                : P2pLogger.Reason.LOST);
    }

    public static void tick(ServerLevel level) {
        long now = level.getGameTime();
        if (now == lastTick) {
            return;
        }
        lastTick = now;
        TerminusSavedData data = TerminusSavedData.get(level);
        if (data == null) return;

        pendingByTarget.values().removeIf(pending -> {
            if (pending.level() != level || now < pending.expiryTick()) {
                return false;
            }
            ComputerRecord requesterRecord = data.getByIpvInf(pending.requester());
            if (requesterRecord != null && requesterRecord.getEndpoint() != null) {
                data.getOrCreateSession(pending.requester(), requesterRecord.getEndpoint().pos())
                        .addLine(Component.translatable("message.null_ouroboros.terminus.p2p.expired").getString());
                sync(level, data, pending.requester());
            }
            ComputerRecord targetRecord = data.getByIpvInf(pending.target());
            if (targetRecord != null && targetRecord.getEndpoint() != null) {
                TerminusSession targetSession = data.getOrCreateSession(pending.target(), targetRecord.getEndpoint().pos());
                targetSession.clearActiveCommand();
                targetSession.addLine(Component.translatable("message.null_ouroboros.terminus.p2p.expired").getString());
                sync(level, data, pending.target());
            }
            return true;
        });

        for (Connection connection : java.util.Set.copyOf(connections.values())) {
            if (connection.level != level || connection.transfer == null) {
                continue;
            }
            Transfer transfer = connection.transfer;
            boolean complete = transfer.loadingBar.tick(level);
            updateTransferBars(data, connection, transfer);
            sync(level, data, transfer.sender);
            sync(level, data, transfer.receiver);
            if (complete) {
                finishTransfer(level, data, connection, true, null);
            }
        }
    }

    private static boolean validateLiveConnection(TerminusSavedData data, Connection connection, String actor) {
        String other = connection.other(actor);
        ComputerRecord actorRecord = data.getByIpvInf(actor);
        ComputerRecord otherRecord = data.getByIpvInf(other);
        if (actorRecord == null || otherRecord == null || actorRecord.getEndpoint() == null || otherRecord.getEndpoint() == null) {
            disconnect(actor, DisconnectCause.LOST);
            return false;
        }
        if (!actorRecord.getEndpoint().dimension().equals(otherRecord.getEndpoint().dimension())) {
            disconnect(actor, DisconnectCause.LOST);
            return false;
        }
        if (!actorRecord.getP2pSettings().passesFilter(other, actorRecord.getFileSystem())) {
            disconnect(actor, DisconnectCause.LOCAL_FILTER);
            return false;
        }
        if (!otherRecord.getP2pSettings().passesFilter(actor, otherRecord.getFileSystem())) {
            disconnect(actor, DisconnectCause.LOST);
            return false;
        }
        return true;
    }

    private static void beginTransferLoading(ServerLevel level, TerminusSavedData data, Connection connection, Transfer transfer) {
        ComputerRecord senderRecord = data.getByIpvInf(transfer.sender);
        ComputerRecord receiverRecord = data.getByIpvInf(transfer.receiver);
        if (senderRecord == null || receiverRecord == null || senderRecord.getEndpoint() == null || receiverRecord.getEndpoint() == null) return;

        TerminusSession senderSession = data.getOrCreateSession(transfer.sender, senderRecord.getEndpoint().pos());
        TerminusSession receiverSession = data.getOrCreateSession(transfer.receiver, receiverRecord.getEndpoint().pos());
        transfer.senderBarLine = senderSession.getLines().size() + 1;
        senderSession.addLine(Component.translatable("message.null_ouroboros.terminus.p2p.transfer.sending").getString());
        senderSession.addLine(transfer.loadingBar.buildBarComponent().getString());
        senderSession.addLine(Component.translatable("message.null_ouroboros.terminus.cancel_hint").getString());
        senderSession.setP2pTransferLoading(true);
        senderSession.setActiveCommand(new P2pTransferLoadingCommand(senderRecord.getFileSystem(), senderRecord.getEndpoint().pos(), level, transfer.sender));

        transfer.receiverBarLine = receiverSession.getLines().size() + 1;
        receiverSession.clearActiveCommand();
        receiverSession.addLine(Component.translatable("message.null_ouroboros.terminus.p2p.transfer.receiving").getString());
        receiverSession.addLine(transfer.loadingBar.buildBarComponent().getString());
        receiverSession.addLine(Component.translatable("message.null_ouroboros.terminus.cancel_hint").getString());
        receiverSession.setP2pTransferLoading(true);
        receiverSession.setActiveCommand(new P2pTransferLoadingCommand(receiverRecord.getFileSystem(), receiverRecord.getEndpoint().pos(), level, transfer.receiver));

        level.playSound(null, senderRecord.getEndpoint().pos(), SoundRegistry.DUSTY_COMPUTER_LOAD.get(), SoundSource.BLOCKS, 0.5f, 1.0f);
        level.playSound(null, receiverRecord.getEndpoint().pos(), SoundRegistry.DUSTY_COMPUTER_LOAD.get(), SoundSource.BLOCKS, 0.5f, 1.0f);
        sync(level, data, transfer.sender);
        sync(level, data, transfer.receiver);
    }

    private static void updateTransferBars(TerminusSavedData data, Connection connection, Transfer transfer) {
        ComputerRecord senderRecord = data.getByIpvInf(transfer.sender);
        ComputerRecord receiverRecord = data.getByIpvInf(transfer.receiver);
        if (senderRecord == null || receiverRecord == null || senderRecord.getEndpoint() == null || receiverRecord.getEndpoint() == null) return;
        String bar = transfer.loadingBar.buildBarComponent().getString();
        data.getOrCreateSession(transfer.sender, senderRecord.getEndpoint().pos()).replaceLine(transfer.senderBarLine, bar);
        data.getOrCreateSession(transfer.receiver, receiverRecord.getEndpoint().pos()).replaceLine(transfer.receiverBarLine, bar);
    }

    private static void finishTransfer(ServerLevel level, TerminusSavedData data, Connection connection, boolean success, @Nullable String failureLine) {
        Transfer transfer = connection.transfer;
        if (transfer == null) return;
        connection.transfer = null;
        ComputerRecord senderRecord = data.getByIpvInf(transfer.sender);
        ComputerRecord receiverRecord = data.getByIpvInf(transfer.receiver);
        if (senderRecord == null || receiverRecord == null || senderRecord.getEndpoint() == null || receiverRecord.getEndpoint() == null) return;
        TerminusSession senderSession = data.getOrCreateSession(transfer.sender, senderRecord.getEndpoint().pos());
        TerminusSession receiverSession = data.getOrCreateSession(transfer.receiver, receiverRecord.getEndpoint().pos());
        senderSession.setP2pTransferLoading(false);
        receiverSession.setP2pTransferLoading(false);
        senderSession.clearActiveCommand();
        receiverSession.clearActiveCommand();
        if (!success) {
            senderSession.addLine(failureLine);
            receiverSession.addLine(failureLine);
            sync(level, data, transfer.sender);
            sync(level, data, transfer.receiver);
            return;
        }
        try {
            TerminusNode source = senderRecord.getFileSystem().resolvePath(transfer.sourcePath);
            if (!(source instanceof TerminusTextFile sourceFile)) {
                throw new FileSystemException(Component.translatable(
                        "message.null_ouroboros.terminus.p2p.error.source_not_found").getString());
            }
            P2pSettings.ensureDirectory(receiverRecord.getFileSystem(), receiverRecord.getP2pSettings().getReceiverDirectory());
            TerminusNode receiverDirNode = receiverRecord.getFileSystem().resolvePath(receiverRecord.getP2pSettings().getReceiverDirectory());
            if (!(receiverDirNode instanceof TerminusDirectory receiverDir)) {
                throw new FileSystemException(Component.translatable(
                        "message.null_ouroboros.terminus.p2p.error.receiver_directory_not_found").getString());
            }
            String targetName = receiverDir.getChild(sourceFile.getName()) == null
                    ? sourceFile.getName()
                    : receiverRecord.getFileSystem().resolveDuplicateName(receiverDir, sourceFile.getName());
            String destPath = receiverRecord.getP2pSettings().getReceiverDirectory();
            if (!destPath.endsWith("\\")) destPath += "\\";
            destPath += targetName;
            receiverRecord.getFileSystem().createTextFile(destPath, sourceFile.getContent());
            if ("CUT".equalsIgnoreCase(transfer.mode)) {
                senderRecord.getFileSystem().delete(transfer.sourcePath, true);
            }
            senderSession.addLine(Component.translatable("message.null_ouroboros.terminus.p2p.transfer.sent", transfer.sourcePath).getString());
            receiverSession.addLine(Component.translatable("message.null_ouroboros.terminus.p2p.transfer.received", targetName).getString());
            connection.logger(transfer.sender).sent(senderRecord.getFileSystem(), transfer.sourcePath);
            connection.logger(transfer.receiver).received(receiverRecord.getFileSystem(), targetName);
            data.setDirty();
        } catch (FileSystemException e) {
            senderSession.addLine(Component.translatable("message.null_ouroboros.terminus.error", e.getMessage()).getString());
            receiverSession.addLine(Component.translatable("message.null_ouroboros.terminus.p2p.transfer.aborted").getString());
        }
        sync(level, data, transfer.sender);
        sync(level, data, transfer.receiver);
    }

    private static void closeSide(ServerLevel level, TerminusSavedData data, Connection connection, String computer, P2pLogger.Reason reason) {
        ComputerRecord record = data.getByIpvInf(computer);
        if (record == null || record.getEndpoint() == null) return;
        connection.logger(computer).end(record.getFileSystem(), reason);
        TerminusSession session = data.getOrCreateSession(computer, record.getEndpoint().pos());
        session.setP2pTransferLoading(false);
        session.clearActiveCommand();
        session.closeP2pSession(reason.text());
        sync(level, data, computer);
    }

    private static String errorLine(String messageKey) {
        return Component.translatable("message.null_ouroboros.terminus.error",
                Component.translatable(messageKey)).getString();
    }

    private static void sync(ServerLevel level, TerminusSavedData data, String computer) {
        ComputerRecord record = data.getByIpvInf(computer);
        if (record == null || record.getEndpoint() == null) return;
        BlockEntity blockEntity = level.getBlockEntity(record.getEndpoint().pos());
        if (!(blockEntity instanceof DustyComputerBlockEntity be) || be.getCurrentUserId() == null) {
            return;
        }
        Player player = level.getPlayerByUUID(be.getCurrentUserId());
        if (player instanceof ServerPlayer serverPlayer) {
            data.getOrCreateSession(computer, record.getEndpoint().pos()).syncToClient(serverPlayer, record.getFileSystem());
            if (record.getFileSystem().isDirty()) {
                data.setDirty();
                record.getFileSystem().clearDirty();
            }
        }
    }
}
