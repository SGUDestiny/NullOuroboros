package destiny.null_ouroboros.server.block.entity;

import destiny.null_ouroboros.client.sound.DustyComputerLoopingSound;
import destiny.null_ouroboros.server.block.DustyComputerBlock;
import destiny.null_ouroboros.server.menu.DustyComputerMenu;
import destiny.null_ouroboros.server.registry.BlockEntityRegistry;
import destiny.null_ouroboros.server.registry.SoundRegistry;
import destiny.null_ouroboros.server.terminal.TerminusSession;
import destiny.null_ouroboros.server.terminal.filesystem.TerminusSavedData;
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
    private static final String FILESYSTEM_ID = "FilesystemId";

    private final List<String> lines = new ArrayList<>();
    private String currentPath = "T:\\";

    private DustyComputerLoopingSound loopingSound;

    @Nullable
    private UUID currentUserId = null;
    @Nullable
    private UUID filesystemId = null;

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
                        boolean done = session.getActiveCommand().tick();
                        if (done) {
                            session.clearActiveCommand();
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

    @Nullable
    public UUID getFilesystemId() {
        return filesystemId;
    }

    public void setFilesystemId(UUID id) {
        this.filesystemId = id;
        setChanged();
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
                data.getOrCreateSession(filesystemId, worldPosition).setPlayerId(userId);
            }
        }
        return true;
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

        session.setPlayerId(player.getUUID());
        session.processCommand(rawLine, data.getOrCreateFileSystem(filesystemId));

        session.syncToClient(player);
    }

    public void clearTerminal() {
        if (level == null || level.isClientSide) return;
        if (filesystemId == null) return;
        TerminusSavedData data = TerminusSavedData.get(level);
        if (data == null) return;
        TerminusSession session = data.getOrCreateSession(filesystemId, worldPosition);
        session.clearLines();

        if (currentUserId != null && level instanceof ServerLevel serverLevel) {
            Player player = serverLevel.getPlayerByUUID(currentUserId);
            if (player instanceof ServerPlayer serverPlayer) {
                session.setPlayerId(currentUserId);
                session.syncToClient(serverPlayer);
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
                    if (session.getActiveCommand() != null) {
                        session.getActiveCommand().cancel();
                        session.clearActiveCommand();
                    }
                    session.setPlayerId(null);
                }
            }
            level.setBlock(worldPosition, state.setValue(DustyComputerBlock.POWERED, false), 3);
            level.playSound(null, worldPosition, SoundRegistry.DUSTY_COMPUTER_STOP.get(), SoundSource.BLOCKS, 0.8f, 1f);
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
    }

    @Override
    public void onLoad() {
        super.onLoad();
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
        return new DustyComputerMenu(containerId, inventory, ContainerLevelAccess.create(level, worldPosition), worldPosition);
    }
}