package destiny.null_ouroboros.server.block.entity;

import destiny.null_ouroboros.client.sound.DustyComputerLoopingSound;
import destiny.null_ouroboros.client.network.ClientBoundDustyComputerSyncPacket;
import destiny.null_ouroboros.server.block.DustyComputerBlock;
import destiny.null_ouroboros.server.block.ElectromagneticAssemblyBlock;
import destiny.null_ouroboros.server.menu.DustyComputerMenu;
import destiny.null_ouroboros.server.registry.BlockEntityRegistry;
import destiny.null_ouroboros.server.registry.SoundRegistry;
import destiny.null_ouroboros.server.terminal.TerminusSession;
import destiny.null_ouroboros.server.terminal.filesystem.TerminusSavedData;
import destiny.null_ouroboros.server.terminal.filesystem.TerminusFileSystem;
import net.minecraft.client.Minecraft;
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
    public static final String FILESYSTEM_ID = "FilesystemId";
    public static final String LEGACY_ITEM_FILESYSTEM_ID = "ComputerUUID";

    private static final String CONNECTED_EMA_POS = "ConnectedEmaPos";

    private final List<String> lines = new ArrayList<>();
    private String currentPath = "T:\\";

    private ClientBoundDustyComputerSyncPacket.FileSessionType fileSessionType =
            ClientBoundDustyComputerSyncPacket.FileSessionType.NONE;
    private String fileSessionContent = "";
    private String fileSessionPath = "";

    private DustyComputerLoopingSound loopingSound;

    @Nullable
    private UUID currentUserId = null;
    @Nullable
    private UUID filesystemId = null;
    private long poweredOnGameTime = -1L;
    @Nullable
    private BlockPos connectedEmaPos = null;

    public DustyComputerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.DUSTY_COMPUTER_BLOCK_ENTITY.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, DustyComputerBlockEntity be) {
        if (level.isClientSide) {
            be.clientTickSounds();
        } else {
            if (be.filesystemId != null) {
                TerminusSavedData data = TerminusSavedData.get(level);
                if (data != null) {
                    TerminusSession session = data.getOrCreateSession(be.filesystemId, pos);
                    if (session.getActiveCommand() != null) {
                        TerminusFileSystem fs = data.getOrCreateFileSystem(be.filesystemId);
                        boolean changed = session.tickActiveCommand(fs);
                        if (changed && be.currentUserId != null && level instanceof ServerLevel serverLevel) {
                            Player player = serverLevel.getPlayerByUUID(be.currentUserId);
                            if (player instanceof ServerPlayer serverPlayer) {
                                session.setPlayerId(be.currentUserId);
                                session.syncToClient(serverPlayer, fs);
                            }
                        }
                    }
                }
            }
        }
    }

    private void clientTickSounds() {
        boolean powered = getBlockState().getValue(DustyComputerBlock.POWERED);
        if (powered) {
            if (loopingSound == null || loopingSound.isStopped()) {
                loopingSound = new DustyComputerLoopingSound(SoundRegistry.DUSTY_COMPUTER_LOOP.get(), this);
                Minecraft.getInstance().getSoundManager().play(loopingSound);
            }
        } else {
            if (loopingSound != null) {
                loopingSound.stopSound();
                loopingSound = null;
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

    @Nullable
    public UUID getFilesystemId() {
        return filesystemId;
    }

    public void setFilesystemId(UUID id) {
        this.filesystemId = id;
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

        if (filesystemId != null) {
            TerminusSavedData data = TerminusSavedData.get(level);
            if (data != null) {
                TerminusSession session = data.getOrCreateSession(filesystemId, worldPosition);
                session.setPlayerId(userId);
                session.syncFromFileSystem(data.getOrCreateFileSystem(filesystemId));
            }
        }
        return true;
    }

    public void syncSessionToPlayer(ServerPlayer player) {
        if (level == null || level.isClientSide || filesystemId == null) return;
        TerminusSavedData data = TerminusSavedData.get(level);
        if (data == null) return;
        TerminusSession session = data.getOrCreateSession(filesystemId, worldPosition);
        TerminusFileSystem fs = data.getOrCreateFileSystem(filesystemId);
        session.syncFromFileSystem(fs);
        session.setPlayerId(player.getUUID());
        session.syncToClient(player, fs);
    }

    public void closeFileSession(@Nullable String contentToSave, ServerPlayer player) {
        if (level == null || level.isClientSide) return;
        if (filesystemId == null) return;
        if (currentUserId == null || !currentUserId.equals(player.getUUID())) return;

        TerminusSavedData data = TerminusSavedData.get(level);
        if (data == null) return;
        TerminusSession session = data.getOrCreateSession(filesystemId, worldPosition);
        if (!session.isInFileSession()) return;

        TerminusFileSystem fs = data.getOrCreateFileSystem(filesystemId);
        session.closeFileSession(fs, contentToSave);

        if (fs.isDirty()) {
            data.setDirty();
            fs.clearDirty();
        }

        session.syncToClient(player, fs);
    }

    public void unclaim(UUID userId) {
        if (level == null || level.isClientSide) return;
        if (userId.equals(currentUserId)) {
            currentUserId = null;
            setChanged();

            if (filesystemId != null) {
                TerminusSavedData data = TerminusSavedData.get(level);
                if (data != null) {
                    data.getOrCreateSession(filesystemId, worldPosition).setPlayerId(null);
                }
            }
        }
    }

    public void processCommand(String rawLine, ServerPlayer player) {
        if (level == null || level.isClientSide) return;
        if (filesystemId == null) return;
        TerminusSavedData data = TerminusSavedData.get(level);
        if (data == null) return;
        TerminusSession session = data.getOrCreateSession(filesystemId, worldPosition);

        TerminusFileSystem fs = data.getOrCreateFileSystem(filesystemId);

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

    public void clearSessionOnBreak() {
        if (level == null || level.isClientSide || filesystemId == null) {
            return;
        }

        TerminusSavedData data = TerminusSavedData.get(level);
        if (data == null) {
            return;
        }

        TerminusSession session = data.getOrCreateSession(filesystemId, worldPosition);
        TerminusFileSystem fs = data.getOrCreateFileSystem(filesystemId);

        if (session.getActiveCommand() != null) {
            session.getActiveCommand().cancel();
            session.clearActiveCommand();
        }
        session.cancelFileSession(fs);
        session.clearLines();
        session.setPlayerId(null);
        currentUserId = null;
    }

    public void clearTerminal() {
        if (level == null || level.isClientSide) return;
        if (filesystemId == null) return;
        TerminusSavedData data = TerminusSavedData.get(level);
        if (data == null) return;
        TerminusSession session = data.getOrCreateSession(filesystemId, worldPosition);
        if (session.getActiveCommand() != null) {
            session.getActiveCommand().cancel();
            session.clearActiveCommand();
        }
        session.clearLines();

        TerminusFileSystem fs = data.getOrCreateFileSystem(filesystemId);
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

            if (filesystemId != null) {
                TerminusSavedData data = TerminusSavedData.get(level);
                if (data != null) {
                    TerminusSession session = data.getOrCreateSession(filesystemId, worldPosition);
                    TerminusFileSystem fs = data.getOrCreateFileSystem(filesystemId);
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
        if (filesystemId != null) {
            tag.putUUID(FILESYSTEM_ID, filesystemId);
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
        if (tag.hasUUID(FILESYSTEM_ID)) {
            filesystemId = tag.getUUID(FILESYSTEM_ID);
        } else {
            filesystemId = null;
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
        if (level != null && level.isClientSide && loopingSound != null) {
            loopingSound.stopSound();
            loopingSound = null;
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