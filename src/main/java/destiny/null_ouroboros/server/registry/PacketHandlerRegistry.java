package destiny.null_ouroboros.server.registry;

import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.client.network.*;
import destiny.null_ouroboros.server.network.ServerBoundDustyComputerCommandPacket;
import destiny.null_ouroboros.server.network.ServerBoundDustyComputerShutdownPacket;
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

        INSTANCE.messageBuilder(ClientBoundBurrowBeaconSyncPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ClientBoundBurrowBeaconSyncPacket::encode)
                .decoder(ClientBoundBurrowBeaconSyncPacket::decode)
                .consumerMainThread(ClientBoundBurrowBeaconSyncPacket::handle)
                .add();

        INSTANCE.messageBuilder(ServerBoundDustyComputerCommandPacket.class, index++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ServerBoundDustyComputerCommandPacket::encode)
                .decoder(ServerBoundDustyComputerCommandPacket::decode)
                .consumerMainThread(ServerBoundDustyComputerCommandPacket::handle)
                .add();

        INSTANCE.messageBuilder(ClientBoundDustyComputerSyncPacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ClientBoundDustyComputerSyncPacket::encode)
                .decoder(ClientBoundDustyComputerSyncPacket::decode)
                .consumerMainThread(ClientBoundDustyComputerSyncPacket::handle)
                .add();

        INSTANCE.messageBuilder(ServerBoundDustyComputerShutdownPacket.class, index++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ServerBoundDustyComputerShutdownPacket::encode)
                .decoder(ServerBoundDustyComputerShutdownPacket::decode)
                .consumerMainThread(ServerBoundDustyComputerShutdownPacket::handle)
                .add();
    }
}
