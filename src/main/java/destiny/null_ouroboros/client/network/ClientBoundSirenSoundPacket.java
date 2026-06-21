package destiny.null_ouroboros.client.network;

import destiny.null_ouroboros.client.sound.SirenSoundManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public class ClientBoundSirenSoundPacket {
    private final BlockPos pos;
    @Nullable private final SoundEvent normalEvent;
    @Nullable private final SoundEvent distantEvent;
    private final boolean looping;

    public ClientBoundSirenSoundPacket(BlockPos pos, @Nullable SoundEvent normalEvent, @Nullable SoundEvent distantEvent, boolean looping) {
        this.pos = pos;
        this.normalEvent = normalEvent;
        this.distantEvent = distantEvent;
        this.looping = looping;
    }

    public static void encode(ClientBoundSirenSoundPacket pkt, FriendlyByteBuf buf) {
        buf.writeBlockPos(pkt.pos);
        buf.writeBoolean(pkt.normalEvent != null);
        if (pkt.normalEvent != null) buf.writeResourceLocation(ForgeRegistries.SOUND_EVENTS.getKey(pkt.normalEvent));
        buf.writeBoolean(pkt.distantEvent != null);
        if (pkt.distantEvent != null) buf.writeResourceLocation(ForgeRegistries.SOUND_EVENTS.getKey(pkt.distantEvent));
        buf.writeBoolean(pkt.looping);
    }

    public static ClientBoundSirenSoundPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        SoundEvent normal = null, distant = null;
        if (buf.readBoolean()) normal = ForgeRegistries.SOUND_EVENTS.getValue(buf.readResourceLocation());
        if (buf.readBoolean()) distant = ForgeRegistries.SOUND_EVENTS.getValue(buf.readResourceLocation());
        boolean looping = buf.readBoolean();

        return new ClientBoundSirenSoundPacket(pos, normal, distant, looping);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var level = Minecraft.getInstance().level;
            if (level != null) {
                if (normalEvent != null && distantEvent != null) {
                    SirenSoundManager.play(pos, normalEvent, distantEvent, looping);
                } else {
                    SirenSoundManager.stop(pos);
                }
            }
        });
        return true;
    }
}