package destiny.null_ouroboros.server.network;

import destiny.null_ouroboros.server.block.entity.TerminusTemplateLoaderBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public class ServerboundTerminusTemplateLoaderPacket {
    private final BlockPos pos;
    private final TerminusTemplateLoaderBlockEntity.Mode mode;
    private final String templateName;
    @Nullable
    private final ResourceLocation templateId;
    private final int posX;
    private final int posY;
    private final int posZ;
    private final String presetName;
    private final int weight;
    private final boolean showBoundingBox;
    private final boolean runAction;

    public ServerboundTerminusTemplateLoaderPacket(BlockPos pos, TerminusTemplateLoaderBlockEntity.Mode mode,
                                                   String templateName, @Nullable ResourceLocation templateId,
                                                   int posX, int posY, int posZ, String presetName, int weight,
                                                   boolean showBoundingBox, boolean runAction) {
        this.pos = pos;
        this.mode = mode;
        this.templateName = templateName;
        this.templateId = templateId;
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
        this.presetName = presetName;
        this.weight = weight;
        this.showBoundingBox = showBoundingBox;
        this.runAction = runAction;
    }

    public static void encode(ServerboundTerminusTemplateLoaderPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeEnum(msg.mode);
        buf.writeUtf(msg.templateName);
        buf.writeBoolean(msg.templateId != null);
        if (msg.templateId != null) {
            buf.writeResourceLocation(msg.templateId);
        }
        buf.writeVarInt(msg.posX);
        buf.writeVarInt(msg.posY);
        buf.writeVarInt(msg.posZ);
        buf.writeUtf(msg.presetName);
        buf.writeVarInt(msg.weight);
        buf.writeBoolean(msg.showBoundingBox);
        buf.writeBoolean(msg.runAction);
    }

    public static ServerboundTerminusTemplateLoaderPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        TerminusTemplateLoaderBlockEntity.Mode mode = buf.readEnum(TerminusTemplateLoaderBlockEntity.Mode.class);
        String templateName = buf.readUtf();
        ResourceLocation templateId = buf.readBoolean() ? buf.readResourceLocation() : null;
        int posX = buf.readVarInt();
        int posY = buf.readVarInt();
        int posZ = buf.readVarInt();
        String presetName = buf.readUtf();
        int weight = buf.readVarInt();
        boolean showBoundingBox = buf.readBoolean();
        boolean runAction = buf.readBoolean();
        return new ServerboundTerminusTemplateLoaderPacket(pos, mode, templateName, templateId, posX, posY, posZ,
                presetName, weight, showBoundingBox, runAction);
    }

    public static boolean handle(ServerboundTerminusTemplateLoaderPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null || !player.canUseGameMasterBlocks()) {
                return;
            }
            Level level = player.level();
            if (!(level.getBlockEntity(msg.pos) instanceof TerminusTemplateLoaderBlockEntity be)) {
                return;
            }
            if (player.distanceToSqr(msg.pos.getX() + 0.5, msg.pos.getY() + 0.5, msg.pos.getZ() + 0.5) > 64.0) {
                return;
            }
            be.applyFromPacket(msg.mode, msg.templateName, msg.templateId, msg.posX, msg.posY, msg.posZ,
                    msg.presetName, msg.weight, msg.showBoundingBox, msg.runAction, player);
        });
        return true;
    }
}
