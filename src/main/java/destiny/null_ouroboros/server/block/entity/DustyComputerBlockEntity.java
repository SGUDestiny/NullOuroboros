package destiny.null_ouroboros.server.block.entity;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import destiny.null_ouroboros.client.network.ClientBoundDustyComputerSyncPacket;
import destiny.null_ouroboros.server.block.DustyComputerBlock;
import destiny.null_ouroboros.server.block.ElectromagneticAssemblyBlock;
import destiny.null_ouroboros.server.menu.DustyComputerMenu;
import destiny.null_ouroboros.server.registry.BlockEntityRegistry;
import destiny.null_ouroboros.server.registry.SoundRegistry;
import destiny.null_ouroboros.server.terminal.TerminusSession;
import destiny.null_ouroboros.server.terminal.filesystem.TerminusSavedData;
import destiny.null_ouroboros.server.terminal.filesystem.TerminusFileSystem;
import destiny.null_ouroboros.server.terminal.p2p.P2pConnectionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DustyComputerBlockEntity extends BlockEntity implements MenuProvider {
    private static final String CURRENT_USER = "CurrentUser";
    public static final String IPV_INF = "IpvInf";
    public static final String FILESYSTEM_ID = "FilesystemId";
    public static final String LEGACY_ITEM_FILESYSTEM_ID = "ComputerUUID";

    private static final String CONNECTED_EMA_POS = "ConnectedEmaPos";

    private final List<String> lines = new ArrayList<>();
    private String currentPath = "T:\\";

    private ClientBoundDustyComputerSyncPacket.FileSessionType fileSessionType =
            ClientBoundDustyComputerSyncPacket.FileSessionType.NONE;
    private String fileSessionContent = "";
    private String fileSessionPath = "";
    private boolean p2pActive = false;
    private String p2pPeerDisplay = "";
    private String p2pSendMode = "MSG";
    private boolean p2pLoadingActive = false;
    private int p2pLoadingPercent = 0;
    private String p2pLoadingMessageKey = "";

    @Nullable
    private UUID currentUserId = null;
    @Nullable
    private String ipvInf = null;
    private long poweredOnGameTime = -1L;
    @Nullable
    private BlockPos connectedEmaPos = null;

    public DustyComputerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.DUSTY_COMPUTER_BLOCK_ENTITY.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, DustyComputerBlockEntity be) {
        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    destiny.null_ouroboros.client.sound.DustyComputerClientSoundHandler.tick(be)
            );
        } else {
            if (be.ipvInf != null) {
                TerminusSavedData data = TerminusSavedData.get(level);
                if (data != null) {
                    data.relocateComputer(be.ipvInf, pos, level.dimension().location());
                    TerminusSession session = data.getOrCreateSession(be.ipvInf, pos);
                    if (session.getActiveCommand() != null) {
                        TerminusFileSystem fs = data.getOrCreateFileSystem(be.ipvInf);
                        boolean changed = session.tickActiveCommand(fs);
                        if (changed && be.currentUserId != null && level instanceof ServerLevel serverLevel) {
                            Player player = serverLevel.getPlayerByUUID(be.currentUserId);
                            if (player instanceof ServerPlayer serverPlayer) {
                                session.setPlayerId(be.currentUserId);
                                session.syncToClient(serverPlayer, fs);
                            }
                        }
                    }
                    if (level instanceof ServerLevel serverLevel) {
                        P2pConnectionManager.tick(serverLevel);
                    }
                }
            }
        }
    }

    public List<String> getLines() {
        return lines;
    }

    public String getCurrentPath() {
        return currentPath;
    }

    public void setLines(List<String> newLines) {
        lines.clear();
        lines.addAll(newLines);
    }

    public void setCurrentPath(String path) {
        this.currentPath = path;
    }

    public ClientBoundDustyComputerSyncPacket.FileSessionType getFileSessionType() {
        return fileSessionType;
    }

    public String getFileSessionContent() {
        return fileSessionContent;
    }

    public String getFileSessionPath() {
        return fileSessionPath;
    }

    public void setFileSessionState(ClientBoundDustyComputerSyncPacket.FileSessionType type,
                                   String content, String path) {
        this.fileSessionType = type != null ? type : ClientBoundDustyComputerSyncPacket.FileSessionType.NONE;
        this.fileSessionContent = content != null ? content : "";
        this.fileSessionPath = path != null ? path : "";
    }

    public void setP2pClientState(boolean active, String peerDisplay, String sendMode,
                                  boolean loadingActive, int loadingPercent, String loadingMessageKey) {
        this.p2pActive = active;
        this.p2pPeerDisplay = peerDisplay != null ? peerDisplay : "";
        this.p2pSendMode = sendMode != null ? sendMode : "MSG";
        this.p2pLoadingActive = loadingActive;
        this.p2pLoadingPercent = loadingPercent;
        this.p2pLoadingMessageKey = loadingMessageKey != null ? loadingMessageKey : "";
    }

    public boolean isP2pActive() { return p2pActive; }
    public String getP2pPeerDisplay() { return p2pPeerDisplay; }
    public String getP2pSendMode() { return p2pSendMode; }
    public boolean isP2pLoadingActive() { return p2pLoadingActive; }
    public int getP2pLoadingPercent() { return p2pLoadingPercent; }
    public String getP2pLoadingMessageKey() { return p2pLoadingMessageKey; }
    @Nullable public UUID getCurrentUserId() { return currentUserId; }

    @Nullable
    public String getFilesystemId() {
        return ipvInf;
    }

    public void setFilesystemId(String id) {
        setIpvInf(id);
    }

    @Nullable
    public String getIpvInf() {
        return ipvInf;
    }

    public void setIpvInf(@Nullable String id) {
        this.ipvInf = id;
        if (id != null && level != null && !level.isClientSide) {
            TerminusSavedData data = TerminusSavedData.get(level);
            if (data != null) {
                data.getOrCreateComputer(id, worldPosition);
                data.relocateComputer(id, worldPosition, level.dimension().location());
            }
        }
        setChanged();
    }

    public long getPoweredOnGameTime() {
        return poweredOnGameTime;
    }

    public void markPoweredOn(long gameTime) {
        this.poweredOnGameTime = gameTime;
    }

    public void clearPoweredOn() {
        this.poweredOnGameTime = -1L;
    }

    public void refreshEmaConnection() {
        if (level == null || level.isClientSide) {
            return;
        }

        BlockPos above = worldPosition.above();
        BlockPos previous = connectedEmaPos;
        if (level.getBlockState(above).getBlock() instanceof ElectromagneticAssemblyBlock) {
            connectedEmaPos = above;
        } else {
            connectedEmaPos = null;
        }

        if (previous != connectedEmaPos && (previous != null || connectedEmaPos != null)) {
            setChanged();
        }
    }

    public boolean hasConnectedEma() {
        return connectedEmaPos != null;
    }

    @Nullable
    public BlockPos getConnectedEmaPos() {
        return connectedEmaPos;
    }

    public boolean tryClaim(UUID userId) {
        if (level == null || level.isClientSide) return false;
        if (currentUserId != null && !currentUserId.equals(userId)) {
            return false;
        }
        currentUserId = userId;
        setChanged();

        if (ipvInf != null) {
            TerminusSavedData data = TerminusSavedData.get(level);
            if (data != null) {
                TerminusSession session = data.getOrCreateSession(ipvInf, worldPosition);
                session.setPlayerId(userId);
                session.syncFromFileSystem(data.getOrCreateFileSystem(ipvInf));
            }
        }
        return true;
    }

    public void syncSessionToPlayer(ServerPlayer player) {
        if (level == null || level.isClientSide || ipvInf == null) return;
        TerminusSavedData data = TerminusSavedData.get(level);
        if (data == null) return;
        TerminusSession session = data.getOrCreateSession(ipvInf, worldPosition);
        TerminusFileSystem fs = data.getOrCreateFileSystem(ipvInf);
        session.syncFromFileSystem(fs);
        session.setPlayerId(player.getUUID());
        session.syncToClient(player, fs);
    }

    public void closeFileSession(@Nullable String contentToSave, ServerPlayer player) {
        if (level == null || level.isClientSide) return;
        if (ipvInf == null) return;
        if (currentUserId == null || !currentUserId.equals(player.getUUID())) return;

        TerminusSavedData data = TerminusSavedData.get(level);
        if (data == null) return;
        TerminusSession session = data.getOrCreateSession(ipvInf, worldPosition);
        if (!session.isInFileSession()) return;

        TerminusFileSystem fs = data.getOrCreateFileSystem(ipvInf);
        session.closeFileSession(fs, contentToSave);

        if (fs.isDirty()) {
            data.setDirty();
            fs.clearDirty();
        }

        session.syncToClient(player, fs);
    }

    public void unclaim(UUID userId) {
        if (level == null || level.isClientSide) return;
        if (userId == null) return;
        if (userId.equals(currentUserId)) {
            currentUserId = null;
            setChanged();

            if (ipvInf != null) {
                TerminusSavedData data = TerminusSavedData.get(level);
                if (data != null) {
                    data.getOrCreateSession(ipvInf, worldPosition).setPlayerId(null);
                }
            }
        }
    }

    public void processCommand(String rawLine, ServerPlayer player) {
        if (level == null || level.isClientSide) return;
        if (ipvInf == null) return;
        TerminusSavedData data = TerminusSavedData.get(level);
        if (data == null) return;
        TerminusSession session = data.getOrCreateSession(ipvInf, worldPosition);

        TerminusFileSystem fs = data.getOrCreateFileSystem(ipvInf);

        session.setPlayerId(player.getUUID());
        session.processCommand(rawLine, fs, level);

        if (fs.isDirty()) {
            data.setDirty();
            fs.clearDirty();
        }

        session.syncToClient(player, fs);

        if (session.consumeShutdownRequest()) {
            shutdown();
            player.closeContainer();
        }
    }

    public void toggleP2pMode(ServerPlayer player) {
        if (level == null || level.isClientSide || ipvInf == null) return;
        if (currentUserId == null || !currentUserId.equals(player.getUUID())) return;
        TerminusSavedData data = TerminusSavedData.get(level);
        if (data == null) return;
        TerminusSession session = data.getOrCreateSession(ipvInf, worldPosition);
        session.toggleP2pSendMode();
        session.syncToClient(player, data.getOrCreateFileSystem(ipvInf));
    }

    public void clearSessionOnBreak() {
        if (level == null || level.isClientSide || ipvInf == null) {
            return;
        }

        TerminusSavedData data = TerminusSavedData.get(level);
        if (data == null) {
            return;
        }

        TerminusSession session = data.getOrCreateSession(ipvInf, worldPosition);
        TerminusFileSystem fs = data.getOrCreateFileSystem(ipvInf);

        if (session.getActiveCommand() != null) {
            session.getActiveCommand().cancel();
            session.clearActiveCommand();
        }
        session.cancelFileSession(fs);
        session.clearLines();
        session.setPlayerId(null);
        data.clearEndpoint(ipvInf);
        P2pConnectionManager.disconnect(ipvInf, P2pConnectionManager.DisconnectCause.LOST);
        currentUserId = null;
    }

    public void clearTerminal() {
        if (level == null || level.isClientSide) return;
        if (ipvInf == null) return;
        TerminusSavedData data = TerminusSavedData.get(level);
        if (data == null) return;
        TerminusSession session = data.getOrCreateSession(ipvInf, worldPosition);
        if (session.getActiveCommand() != null) {
            session.getActiveCommand().cancel();
            session.clearActiveCommand();
        }
        session.clearLines();

        TerminusFileSystem fs = data.getOrCreateFileSystem(ipvInf);
        session.cancelFileSession(fs);
        fs.setCurrentDirectory(fs.getRoot());
        session.syncFromFileSystem(fs);
        if (fs.isDirty()) {
            data.setDirty();
            fs.clearDirty();
        }

        if (currentUserId != null && level instanceof ServerLevel serverLevel) {
            Player player = serverLevel.getPlayerByUUID(currentUserId);
            if (player instanceof ServerPlayer serverPlayer) {
                session.setPlayerId(currentUserId);
                session.syncToClient(serverPlayer, fs);
            }
        }
    }

    public void shutdown() {
        if (level == null || level.isClientSide) return;
        BlockState state = getBlockState();
        if (state.getValue(DustyComputerBlock.POWERED)) {

            if (ipvInf != null) {
                TerminusSavedData data = TerminusSavedData.get(level);
                if (data != null) {
                    P2pConnectionManager.disconnect(ipvInf, P2pConnectionManager.DisconnectCause.LOST);
                    TerminusSession session = data.getOrCreateSession(ipvInf, worldPosition);
                    TerminusFileSystem fs = data.getOrCreateFileSystem(ipvInf);
                    if (session.getActiveCommand() != null) {
                        session.getActiveCommand().cancel();
                        session.clearActiveCommand();
                    }
                    session.cancelFileSession(fs);
                    session.setPlayerId(null);
                }
            }
            level.setBlock(worldPosition, state.setValue(DustyComputerBlock.POWERED, false), 3);
            level.playSound(null, worldPosition, SoundRegistry.DUSTY_COMPUTER_STOP.get(), SoundSource.BLOCKS, 0.6f, 1f);
            clearPoweredOn();
            clearTerminal();
            unclaim(currentUserId);
            setChanged();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (currentUserId != null) {
            tag.putUUID(CURRENT_USER, currentUserId);
        }
        if (ipvInf != null) {
            tag.putString(IPV_INF, ipvInf);
        }
        if (connectedEmaPos != null) {
            tag.putLong(CONNECTED_EMA_POS, connectedEmaPos.asLong());
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.hasUUID(CURRENT_USER)) {
            currentUserId = tag.getUUID(CURRENT_USER);
        } else {
            currentUserId = null;
        }
        if (tag.contains(IPV_INF, net.minecraft.nbt.Tag.TAG_STRING)) {
            ipvInf = tag.getString(IPV_INF);
        } else if (tag.contains(FILESYSTEM_ID, net.minecraft.nbt.Tag.TAG_STRING)) {
            ipvInf = tag.getString(FILESYSTEM_ID);
        } else {
            ipvInf = null;
        }
        if (tag.contains(CONNECTED_EMA_POS, net.minecraft.nbt.Tag.TAG_LONG)) {
            connectedEmaPos = BlockPos.of(tag.getLong(CONNECTED_EMA_POS));
        } else {
            connectedEmaPos = null;
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        refreshEmaConnection();
        if (level != null && !level.isClientSide) {
            TerminusSavedData data = TerminusSavedData.get(level);
            if (!TerminusSavedData.isValidIpvInf(ipvInf)) {
                ipvInf = data != null ? data.generateUniqueIpvInf() : TerminusSavedData.generateIpvInfValue();
                setChanged();
            }
            if (data != null) {
                data.getOrCreateComputer(ipvInf, worldPosition);
                data.relocateComputer(ipvInf, worldPosition, level.dimension().location());
            }
        }
        if (level instanceof ServerLevel serverLevel && currentUserId != null) {
            if (serverLevel.getPlayerByUUID(currentUserId) == null) {
                currentUserId = null;
                setChanged();
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        load(tag);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    destiny.null_ouroboros.client.sound.DustyComputerClientSoundHandler.stop(this)
            );
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.empty();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new DustyComputerMenu(containerId, inventory, ContainerLevelAccess.create(level, worldPosition), worldPosition, poweredOnGameTime);
    }
}