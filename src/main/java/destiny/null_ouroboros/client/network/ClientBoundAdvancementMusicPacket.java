package destiny.null_ouroboros.client.network;

import destiny.null_ouroboros.client.sound.AdvancementMusicManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientBoundAdvancementMusicPacket {
    private final ResourceLocation advancementId;

    public ClientBoundAdvancementMusicPacket(ResourceLocation advancementId) {
        this.advancementId = advancementId;
    }

    public static void encode(ClientBoundAdvancementMusicPacket pkt, FriendlyByteBuf buf) {
        buf.writeResourceLocation(pkt.advancementId);
    }

    public static ClientBoundAdvancementMusicPacket decode(FriendlyByteBuf buf) {
        return new ClientBoundAdvancementMusicPacket(buf.readResourceLocation());
    }

    public static boolean handle(ClientBoundAdvancementMusicPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> AdvancementMusicManager.onAdvancementEarned(pkt.advancementId));
        return true;
    }
}
