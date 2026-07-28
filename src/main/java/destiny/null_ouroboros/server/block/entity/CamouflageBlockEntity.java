package destiny.null_ouroboros.server.block.entity;

import destiny.null_ouroboros.server.camouflage.Camouflage;
import destiny.null_ouroboros.server.camouflage.Camouflageable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public abstract class CamouflageBlockEntity extends BlockEntity implements Camouflageable {
    @Nullable
    private BlockState camouflage;

    protected CamouflageBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    @Nullable
    public BlockState getCamouflage() {
        return camouflage;
    }

    @Override
    public void setCamouflage(@Nullable BlockState camouflage) {
        this.camouflage = camouflage;
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = level.getBlockState(worldPosition);
            Camouflage.syncCamouflagedProperty(level, worldPosition, state, hasCamouflage());
            BlockState updated = level.getBlockState(worldPosition);
            level.sendBlockUpdated(worldPosition, updated, updated, 3);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide && hasCamouflage()) {
            Camouflage.syncCamouflagedProperty(level, worldPosition, getBlockState(), true);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        Camouflage.writeNbt(tag, camouflage);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        camouflage = Camouflage.readNbt(tag);
        if (level != null && level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            load(tag);
        }
    }
}
