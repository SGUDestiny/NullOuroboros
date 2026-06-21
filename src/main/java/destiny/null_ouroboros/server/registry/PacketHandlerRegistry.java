package destiny.null_ouroboros.server.registry;

import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.client.network.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class PacketHandlerRegistry {
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID, "main_network"),
            () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);

    private PacketHandlerRegistry(){}

    public static void register() {
        int index = 0;

        INSTANCE.messageBuilder(ClientBoundParticlePacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ClientBoundParticlePacket::encode)
                .decoder(ClientBoundParticlePacket::decode)
                .consumerMainThread(ClientBoundParticlePacket::handle)
                .add();

        INSTANCE.messageBuilder(ClientBoundManifoldingPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ClientBoundManifoldingPacket::encode)
                .decoder(ClientBoundManifoldingPacket::new)
                .consumerMainThread(ClientBoundManifoldingPacket::handle)
                .add();

        INSTANCE.messageBuilder(ClientBoundDetectorPulsePacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ClientBoundDetectorPulsePacket::encode)
                .decoder(ClientBoundDetectorPulsePacket::decode)
                .consumerMainThread(ClientBoundDetectorPulsePacket::handle)
                .add();

        INSTANCE.messageBuilder(ClientBoundDetectorPlacePacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ClientBoundDetectorPlacePacket::encode)
                .decoder(ClientBoundDetectorPlacePacket::decode)
                .consumerMainThread(ClientBoundDetectorPlacePacket::handle)
                .add();

        INSTANCE.messageBuilder(ClientBoundSirenSoundPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ClientBoundSirenSoundPacket::encode)
                .decoder(ClientBoundSirenSoundPacket::decode)
                .consumerMainThread(ClientBoundSirenSoundPacket::handle)
                .add();
    }
}
