package destiny.null_ouroboros.server.block.entity;

import destiny.null_ouroboros.server.entity.DusterbikeEntity;
import destiny.null_ouroboros.server.registry.BlockEntityRegistry;
import destiny.null_ouroboros.server.registry.EntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class AbandonedDusterbikeSpawnerBlockEntity extends BlockEntity {
    private static final String YAW = "Yaw";
    private static final String KEEP_AS_MARKER = "KeepAsMarker";
    private static final String TRIGGERED = "Triggered";

    private float yaw;
    private boolean keepAsMarker;
    private boolean triggered;

    public AbandonedDusterbikeSpawnerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.ABANDONED_DUSTERBIKE_SPAWNER_BLOCK_ENTITY.get(), pos, state);
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public void setKeepAsMarker(boolean keepAsMarker) {
        this.keepAsMarker = keepAsMarker;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AbandonedDusterbikeSpawnerBlockEntity be) {
        if (!be.keepAsMarker && !be.triggered) {
            be.triggerSpawn();
        }
    }

    public void triggerSpawn() {
        if (triggered || level == null || level.isClientSide) {
            return;
        }
        triggered = true;

        DusterbikeEntity bike = new DusterbikeEntity(EntityRegistry.DUSTERBIKE.get(), level);
        bike.setPos(worldPosition.getX() + 0.5D, worldPosition.getY(), worldPosition.getZ() + 0.5D);
        bike.setYRot(yaw);
        level.addFreshEntity(bike);
        bike.initializeAbandoned();

        level.setBlock(worldPosition, Blocks.AIR.defaultBlockState(), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putFloat(YAW, yaw);
        tag.putBoolean(KEEP_AS_MARKER, keepAsMarker);
        tag.putBoolean(TRIGGERED, triggered);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        yaw = tag.getFloat(YAW);
        keepAsMarker = tag.getBoolean(KEEP_AS_MARKER);
        triggered = tag.getBoolean(TRIGGERED);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putFloat(YAW, yaw);
        tag.putBoolean(KEEP_AS_MARKER, keepAsMarker);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            load(tag);
        }
    }
}
