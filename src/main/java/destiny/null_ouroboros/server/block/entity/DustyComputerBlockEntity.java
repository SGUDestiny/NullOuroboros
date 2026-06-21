package destiny.null_ouroboros.server.block.entity;

import destiny.null_ouroboros.server.menu.DustyComputerMenu;
import destiny.null_ouroboros.client.network.ClientBoundDustyComputerSyncPacket;
import destiny.null_ouroboros.client.sound.DustyComputerLoopingSound;
import destiny.null_ouroboros.server.block.DustyComputerBlock;
import destiny.null_ouroboros.server.registry.BlockEntityRegistry;
import destiny.null_ouroboros.server.registry.PacketHandlerRegistry;
import destiny.null_ouroboros.server.registry.SoundRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DustyComputerBlockEntity extends BlockEntity implements MenuProvider {
    private static final String LINES = "Lines";
    private static final String CURRENT_USER = "CurrentUser";

    private DustyComputerLoopingSound loopingSound;
    private final List<String> lines = new ArrayList<>();

    @Nullable
    private UUID currentUserId = null;

    public DustyComputerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.DUSTY_COMPUTER_BLOCK_ENTITY.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, DustyComputerBlockEntity blockEntity) {
        if (level.isClientSide) {
            blockEntity.clientTickSounds();
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

    public void addLine(String line) {
        lines.add(line);
        setChanged();

        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void setLines(List<String> lines) {
        this.lines.clear();
        this.lines.addAll(lines);

        setChanged();

        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void clearTerminal() {
        lines.clear();

        setChanged();

        if (level != null && !level.isClientSide) {
            LevelChunk chunk = level.getChunkAt(worldPosition);

            PacketDistributor.PacketTarget target = PacketDistributor.TRACKING_CHUNK.with(() -> chunk);
            PacketHandlerRegistry.INSTANCE.send(target,
                    new ClientBoundDustyComputerSyncPacket(worldPosition, new ArrayList<>(lines)));

            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void shutdown() {
        if (level == null || level.isClientSide) return;

        BlockState state = getBlockState();
        if (state.getValue(DustyComputerBlock.POWERED)) {
            level.setBlock(worldPosition, state.setValue(DustyComputerBlock.POWERED, false), 3);
            level.playSound(null, worldPosition, SoundRegistry.DUSTY_COMPUTER_STOP.get(), SoundSource.BLOCKS, 0.8f, 1f);

            clearTerminal();
            unclaim(currentUserId);
            setChanged();
        }
    }

    public boolean tryClaim(UUID userId) {
        if (level == null || level.isClientSide) return false;

        if (currentUserId != null && !currentUserId.equals(userId)) {
            return false;
        }

        currentUserId = userId;
        setChanged();
        return true;
    }

    public void unclaim(UUID userId) {
        if (level == null || level.isClientSide) return;

        if (userId.equals(currentUserId)) {
            currentUserId = null;
            setChanged();
        }
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
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ListTag list = new ListTag();

        for (String line : lines) {
            list.add(StringTag.valueOf(line));
        }

        tag.put(LINES, list);

        if (currentUserId != null) {
            tag.putUUID(CURRENT_USER, currentUserId);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        lines.clear();
        ListTag list = tag.getList(LINES, 8);

        for (int i = 0; i < list.size(); i++) {
            lines.add(list.getString(i));
        }

        if (tag.hasUUID(CURRENT_USER)) {
            currentUserId = tag.getUUID(CURRENT_USER);
        } else {
            currentUserId = null;
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
    public Component getDisplayName() {
        return Component.empty();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new DustyComputerMenu(containerId, inventory, ContainerLevelAccess.create(level, worldPosition), worldPosition);
    }
}