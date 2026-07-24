package destiny.null_ouroboros.server.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.INBTSerializable;

public class RecoilCapability implements INBTSerializable<CompoundTag> {
    public float tempPitch, tempYaw;
    private float previousOffsetPitch, previousOffsetYaw;
    private float lastOffsetPitch, lastOffsetYaw;
    private float decayFactor = 0.85F;

    public void clientTick(Player player) {
        removeLastOffset(player);
        previousOffsetPitch = tempPitch;
        previousOffsetYaw = tempYaw;

        tempPitch *= decayFactor;
        tempYaw *= decayFactor;
    }

    public void renderTick(Player player, float partialTick) {
        removeLastOffset(player);

        float offsetPitch = Mth.lerp(partialTick, previousOffsetPitch, tempPitch);
        float offsetYaw = Mth.lerp(partialTick, previousOffsetYaw, tempYaw);
        if (Math.abs(offsetPitch) < 1e-6F && Math.abs(offsetYaw) < 1e-6F) {
            return;
        }

        float appliedPitch = Mth.clamp(player.getXRot() + offsetPitch, -90.0F, 90.0F) - player.getXRot();
        player.setXRot(player.getXRot() + appliedPitch);
        player.setYRot(player.getYRot() + offsetYaw);
        player.setYHeadRot(player.getYHeadRot() + offsetYaw);

        lastOffsetPitch = appliedPitch;
        lastOffsetYaw = offsetYaw;
    }

    private void removeLastOffset(Player player) {
        if (lastOffsetPitch == 0.0F && lastOffsetYaw == 0.0F) {
            return;
        }

        player.setXRot(player.getXRot() - lastOffsetPitch);
        player.setYRot(player.getYRot() - lastOffsetYaw);
        player.setYHeadRot(player.getYHeadRot() - lastOffsetYaw);
        lastOffsetPitch = 0.0F;
        lastOffsetYaw = 0.0F;
    }

    public void addRecoil(float pitch, float yaw) {
        tempPitch -= pitch;
        tempYaw += yaw;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("tempPitch", tempPitch);
        tag.putFloat("tempYaw", tempYaw);
        tag.putFloat("decay", decayFactor);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        tempPitch = tag.getFloat("tempPitch");
        tempYaw = tag.getFloat("tempYaw");
        decayFactor = tag.getFloat("decay");
        previousOffsetPitch = previousOffsetYaw = lastOffsetPitch = lastOffsetYaw = 0F;
    }
}