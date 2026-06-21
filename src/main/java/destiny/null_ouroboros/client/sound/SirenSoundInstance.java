package destiny.null_ouroboros.client.sound;

import destiny.null_ouroboros.server.block.entity.MechanicalSirenBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

public class SirenSoundInstance extends AbstractTickableSoundInstance {
    private final BlockPos pos;
    private final float minDist;
    private final float maxDist;

    public SirenSoundInstance(SoundEvent soundEvent, SoundSource source, BlockPos pos, boolean looping, float minDist, float maxDist) {
        super(soundEvent, source, SoundInstance.createUnseededRandom());
        this.pos = pos;
        this.looping = looping;
        this.relative = false;
        this.x = pos.getX() + 0.5;
        this.y = pos.getY() + 0.5;
        this.z = pos.getZ() + 0.5;
        this.attenuation = Attenuation.NONE;
        this.volume = 0f;
        this.minDist = minDist;
        this.maxDist = maxDist;
    }

    @Override
    public void tick() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.isRemoved()) {
            stop();
            return;
        }

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !(level.getBlockEntity(pos) instanceof MechanicalSirenBlockEntity)) {
            stop();
            return;
        }

        double dist = Math.sqrt(player.distanceToSqr(x, y, z));
        float vol = 0f;
        if (dist <= minDist) {
            vol = 1f;
        } else if (dist >= maxDist) {
            vol = 0f;
        } else {
            vol = 1f - (float)((dist - minDist) / (maxDist - minDist));
        }
        this.volume = Mth.clamp(vol, 0f, 1f);
    }

    @Override
    public float getVolume() {
        return this.volume;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }
}