package destiny.null_ouroboros.server.terminal.p2p;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

public record ComputerEndpoint(ResourceLocation dimension, BlockPos pos) {
    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Dimension", dimension.toString());
        tag.putLong("Pos", pos.asLong());
        return tag;
    }

    @Nullable
    public static ComputerEndpoint fromNBT(CompoundTag tag) {
        if (!tag.contains("Dimension") || !tag.contains("Pos")) {
            return null;
        }
        ResourceLocation dimension = ResourceLocation.tryParse(tag.getString("Dimension"));
        if (dimension == null) {
            return null;
        }
        return new ComputerEndpoint(dimension, BlockPos.of(tag.getLong("Pos")));
    }
}
