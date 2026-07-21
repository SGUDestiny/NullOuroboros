package destiny.null_ouroboros.server.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.INBTSerializable;

public class RecoilCapability implements INBTSerializable<CompoundTag> {
    public float tempPitch, tempYaw;
    private float permPitch, permYaw;
    private float lastOffsetPitch, lastOffsetYaw;
    private float decayFactor = 0.85F;

    public void clientTick(Player player) {
        tempPitch *= decayFactor;
        tempYaw *= decayFactor;

        float totalPitch = tempPitch + permPitch;
        float totalYaw = tempYaw + permYaw;

        if (Math.abs(totalPitch) < 1e-6F && Math.abs(totalYaw) < 1e-6F && lastOffsetPitch == 0.0F && lastOffsetYaw == 0.0F) {
            return;
        }

        float prevPitch = player.xRotO;
        float prevYaw = player.yRotO;

        float basePitch = player.getXRot() - lastOffsetPitch;
        float baseYaw = player.getYRot() - lastOffsetYaw;

        float newPitch = basePitch + totalPitch;
        float newYaw = baseYaw + totalYaw;

        player.setXRot(newPitch);
        player.setYRot(newYaw);

        player.xRotO = prevPitch;
        player.yRotO = prevYaw;

        player.setYHeadRot(newYaw);

        lastOffsetPitch = totalPitch;
        lastOffsetYaw = totalYaw;
    }

    public void addRecoil(float pitch, float yaw, float permanentFactor) {
        float perm = Mth.clamp(permanentFactor, 0F, 1F);
        tempPitch -= pitch * (1F - perm);
        tempYaw += yaw * (1F - perm);
        permPitch -= pitch * perm;
        permYaw += yaw * perm;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("tempPitch", tempPitch);
        tag.putFloat("tempYaw", tempYaw);
        tag.putFloat("permPitch", permPitch);
        tag.putFloat("permYaw", permYaw);
        tag.putFloat("decay", decayFactor);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        tempPitch = tag.getFloat("tempPitch");
        tempYaw = tag.getFloat("tempYaw");
        permPitch = tag.getFloat("permPitch");
        permYaw = tag.getFloat("permYaw");
        decayFactor = tag.getFloat("decay");
        lastOffsetPitch = lastOffsetYaw = 0F;
    }
}