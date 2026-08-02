package destiny.null_ouroboros.server.block.entity;

import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.server.registry.BlockEntityRegistry;
import destiny.null_ouroboros.server.terminal.filesystem.ComputerRecord;
import destiny.null_ouroboros.server.terminal.filesystem.TerminusFileSystem;
import destiny.null_ouroboros.server.terminal.filesystem.TerminusSavedData;
import destiny.null_ouroboros.server.terminal.template.FileSystemPreset;
import destiny.null_ouroboros.server.terminal.template.TerminusTemplate;
import destiny.null_ouroboros.server.terminal.template.TerminusTemplateManager;
import destiny.null_ouroboros.server.terminal.template.TerminusTemplateSaver;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.nio.file.Path;

public class TerminusTemplateLoaderBlockEntity extends BlockEntity {
    public enum Mode {
        SAVE,
        LOAD;

        public static Mode byName(String name) {
            for (Mode mode : values()) {
                if (mode.name().equalsIgnoreCase(name)) {
                    return mode;
                }
            }
            return LOAD;
        }
    }

    private static final String MODE = "Mode";
    private static final String TEMPLATE_NAME = "TemplateName";
    private static final String TEMPLATE = "Template";
    private static final String POS_X = "PosX";
    private static final String POS_Y = "PosY";
    private static final String POS_Z = "PosZ";
    private static final String PRESET_NAME = "PresetName";
    private static final String WEIGHT = "Weight";
    private static final String KEEP_AS_MARKER = "KeepAsMarker";
    private static final String TRIGGERED = "Triggered";
    private static final String SHOW_BOUNDING_BOX = "ShowBoundingBox";

    private Mode mode = Mode.LOAD;
    private String templateName = "";
    @Nullable
    private ResourceLocation templateId = null;
    private int posX;
    private int posY;
    private int posZ;
    private String presetName = "";
    private int weight = 1;
    private boolean keepAsMarker;
    private boolean triggered;
    private boolean showBoundingBox = true;

    public TerminusTemplateLoaderBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.TERMINUS_TEMPLATE_LOADER_BLOCK_ENTITY.get(), pos, state);
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode != null ? mode : Mode.LOAD;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName != null ? templateName : "";
    }

    @Nullable
    public ResourceLocation getTemplateId() {
        return templateId;
    }

    public void setTemplateId(@Nullable ResourceLocation templateId) {
        this.templateId = templateId;
    }

    public int getPosX() {
        return posX;
    }

    public int getPosY() {
        return posY;
    }

    public int getPosZ() {
        return posZ;
    }

    public void setRelativePos(int x, int y, int z) {
        this.posX = x;
        this.posY = y;
        this.posZ = z;
    }

    public String getPresetName() {
        return presetName;
    }

    public void setPresetName(String presetName) {
        this.presetName = presetName != null ? presetName : "";
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = Math.max(1, weight);
    }

    public boolean isKeepAsMarker() {
        return keepAsMarker;
    }

    public void setKeepAsMarker(boolean keepAsMarker) {
        this.keepAsMarker = keepAsMarker;
    }

    public boolean isShowBoundingBox() {
        return showBoundingBox;
    }

    public void setShowBoundingBox(boolean showBoundingBox) {
        this.showBoundingBox = showBoundingBox;
    }

    public BlockPos getTargetPos() {
        return worldPosition.offset(posX, posY, posZ);
    }

    public void armForStructurePlacement() {
        this.keepAsMarker = false;
        this.triggered = false;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TerminusTemplateLoaderBlockEntity be) {
        if (be.keepAsMarker || be.triggered) {
            return;
        }
        if (be.mode == Mode.LOAD) {
            be.triggerLoad(false);
        }
    }

    public boolean triggerLoad(boolean forceKeep) {
        if (level == null || level.isClientSide) {
            return false;
        }
        if (triggered && !forceKeep) {
            return false;
        }
        boolean success = applyLoad((ServerLevel) level);
        if (!forceKeep && !keepAsMarker) {
            triggered = true;
            level.setBlock(worldPosition, Blocks.AIR.defaultBlockState(), 3);
        } else {
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
        return success;
    }

    public boolean triggerSave() {
        if (level == null || level.isClientSide || !(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        BlockPos target = getTargetPos();
        if (!(serverLevel.getBlockEntity(target) instanceof DustyComputerBlockEntity computer)) {
            return false;
        }
        ensureComputerIpv(computer, serverLevel);
        TerminusSavedData data = TerminusSavedData.get(serverLevel);
        if (data == null || computer.getIpvInf() == null) {
            return false;
        }
        TerminusFileSystem fs = data.getOrCreateFileSystem(computer.getIpvInf());
        Path root = TerminusTemplateManager.INSTANCE.getWorldSaveTemplatesRoot();
        return TerminusTemplateSaver.save(root, templateName, presetName, weight, fs);
    }

    private boolean applyLoad(ServerLevel serverLevel) {
        BlockPos target = getTargetPos();
        if (!(serverLevel.getBlockEntity(target) instanceof DustyComputerBlockEntity computer)) {
            NullOuroboros.LOGGER.warn("Terminus template loader at {} could not find dusty computer at {}", worldPosition, target);
            return false;
        }
        ensureComputerIpv(computer, serverLevel);
        TerminusSavedData data = TerminusSavedData.get(serverLevel);
        if (data == null || computer.getIpvInf() == null) {
            return false;
        }
        ComputerRecord record = data.getOrCreateComputer(computer.getIpvInf(), target);
        if (record.isPresetApplied()) {
            return true;
        }
        TerminusTemplate template = resolveTemplate();
        if (template == null) {
            NullOuroboros.LOGGER.warn("Terminus template loader at {} could not resolve template", worldPosition);
            return false;
        }
        RandomSource random = serverLevel.getRandom();
        FileSystemPreset preset = template.roll(random);
        if (preset == null) {
            return false;
        }
        TerminusFileSystem fs = record.getFileSystem();
        preset.applyTo(fs);
        record.setPresetApplied(true);
        record.setAppliedPreset(template.getName() + "/" + preset.getName());
        data.setDirty();
        return true;
    }

    @Nullable
    private TerminusTemplate resolveTemplate() {
        if (templateId != null) {
            TerminusTemplate byId = TerminusTemplateManager.INSTANCE.get(templateId);
            if (byId != null) {
                return byId;
            }
        }
        if (templateName != null && !templateName.isBlank()) {
            return TerminusTemplateManager.INSTANCE.getByWorldSaveName(templateName);
        }
        if (templateId != null) {
            return TerminusTemplateManager.INSTANCE.getByWorldSaveName(templateId.getPath());
        }
        return null;
    }

    private static void ensureComputerIpv(DustyComputerBlockEntity computer, ServerLevel level) {
        if (TerminusSavedData.isValidIpvInf(computer.getIpvInf())) {
            return;
        }
        TerminusSavedData data = TerminusSavedData.get(level);
        String ipv = data != null ? data.generateUniqueIpvInf() : TerminusSavedData.generateIpvInfValue();
        computer.setIpvInf(ipv);
    }

    public void applyFromPacket(Mode mode, String templateName, @Nullable ResourceLocation templateId,
                                int x, int y, int z, String presetName, int weight, boolean showBoundingBox,
                                boolean runAction, @Nullable ServerPlayer player) {
        this.mode = mode;
        this.templateName = templateName != null ? templateName : "";
        this.templateId = templateId;
        this.posX = x;
        this.posY = y;
        this.posZ = z;
        setPresetName(presetName);
        setWeight(weight);
        this.showBoundingBox = showBoundingBox;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            if (runAction) {
                if (mode == Mode.SAVE) {
                    boolean success = triggerSave();
                    if (player != null) {
                        String name = this.templateName.isEmpty() ? "-" : this.templateName;
                        player.displayClientMessage(Component.translatable(
                                success ? "terminus_template_loader.save_success" : "terminus_template_loader.save_failure",
                                name), false);
                    }
                } else if (mode == Mode.LOAD) {
                    String name = this.templateId != null ? this.templateId.toString()
                            : (this.templateName.isEmpty() ? "-" : this.templateName);
                    if (resolveTemplate() == null) {
                        if (player != null) {
                            player.displayClientMessage(Component.translatable(
                                    "terminus_template_loader.load_not_found", name), false);
                        }
                    } else {
                        boolean wasMarker = keepAsMarker;
                        boolean success = triggerLoad(wasMarker);
                        if (player != null) {
                            player.displayClientMessage(Component.translatable(
                                    success ? "terminus_template_loader.load_success" : "terminus_template_loader.load_failure",
                                    name), false);
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString(MODE, mode.name());
        tag.putString(TEMPLATE_NAME, templateName);
        if (templateId != null) {
            tag.putString(TEMPLATE, templateId.toString());
        }
        tag.putInt(POS_X, posX);
        tag.putInt(POS_Y, posY);
        tag.putInt(POS_Z, posZ);
        tag.putString(PRESET_NAME, presetName);
        tag.putInt(WEIGHT, weight);
        tag.putBoolean(KEEP_AS_MARKER, keepAsMarker);
        tag.putBoolean(TRIGGERED, triggered);
        tag.putBoolean(SHOW_BOUNDING_BOX, showBoundingBox);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        mode = Mode.byName(tag.getString(MODE));
        templateName = tag.getString(TEMPLATE_NAME);
        if (tag.contains(TEMPLATE)) {
            templateId = ResourceLocation.tryParse(tag.getString(TEMPLATE));
        } else {
            templateId = null;
        }
        posX = tag.getInt(POS_X);
        posY = tag.getInt(POS_Y);
        posZ = tag.getInt(POS_Z);
        presetName = tag.contains(PRESET_NAME) ? tag.getString(PRESET_NAME) : "";
        weight = tag.contains(WEIGHT) ? Math.max(1, tag.getInt(WEIGHT)) : 1;
        keepAsMarker = tag.getBoolean(KEEP_AS_MARKER);
        triggered = tag.getBoolean(TRIGGERED);
        showBoundingBox = !tag.contains(SHOW_BOUNDING_BOX) || tag.getBoolean(SHOW_BOUNDING_BOX);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
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
