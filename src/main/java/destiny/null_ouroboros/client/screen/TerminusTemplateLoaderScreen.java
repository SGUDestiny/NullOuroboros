package destiny.null_ouroboros.client.screen;

import destiny.null_ouroboros.server.block.entity.TerminusTemplateLoaderBlockEntity;
import destiny.null_ouroboros.server.network.ServerboundTerminusTemplateLoaderPacket;
import destiny.null_ouroboros.server.registry.PacketHandlerRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TerminusTemplateLoaderScreen extends Screen {
    private static final Component TITLE = Component.translatable("block.null_ouroboros.terminus_template_loader");
    private static final Component TEMPLATE_PATH_LABEL = Component.translatable("terminus_template_loader.template_path");
    private static final Component POSITION_LABEL = Component.translatable("structure_block.position");
    private static final Component PRESET_NAME_LABEL = Component.translatable("terminus_template_loader.preset_name");
    private static final Component WEIGHT_LABEL = Component.translatable("terminus_template_loader.weight");
    private static final Component SHOW_BOUNDING_BOX_LABEL = Component.translatable("structure_block.show_boundingbox");

    private final TerminusTemplateLoaderBlockEntity loader;
    private TerminusTemplateLoaderBlockEntity.Mode mode;
    private EditBox templatePathEdit;
    private EditBox posXEdit;
    private EditBox posYEdit;
    private EditBox posZEdit;
    private EditBox presetNameEdit;
    private EditBox weightEdit;
    private Button saveActionButton;
    private Button loadActionButton;
    private CycleButton<Boolean> toggleBoundingBox;
    private boolean showBoundingBox;

    public TerminusTemplateLoaderScreen(TerminusTemplateLoaderBlockEntity loader) {
        super(TITLE);
        this.loader = loader;
        this.mode = loader.getMode();
        this.showBoundingBox = loader.isShowBoundingBox();
    }

    public static void open(TerminusTemplateLoaderBlockEntity loader) {
        Minecraft.getInstance().setScreen(new TerminusTemplateLoaderScreen(loader));
    }

    @Override
    protected void init() {
        mode = loader.getMode();
        showBoundingBox = loader.isShowBoundingBox();

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onDone(false))
                .bounds(width / 2 - 4 - 150, 210, 150, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> onClose())
                .bounds(width / 2 + 4, 210, 150, 20).build());

        saveActionButton = addRenderableWidget(Button.builder(Component.translatable("structure_block.button.save"), button -> {
            if (mode == TerminusTemplateLoaderBlockEntity.Mode.SAVE) {
                onDone(true);
            }
        }).bounds(width / 2 + 4 + 100, 185, 50, 20).build());

        loadActionButton = addRenderableWidget(Button.builder(Component.translatable("structure_block.button.load"), button -> {
            if (mode == TerminusTemplateLoaderBlockEntity.Mode.LOAD) {
                onDone(true);
            }
        }).bounds(width / 2 + 4 + 100, 185, 50, 20).build());

        addRenderableWidget(CycleButton.<TerminusTemplateLoaderBlockEntity.Mode>builder(value ->
                        Component.translatable("structure_block.mode." + value.name().toLowerCase()))
                .withValues(TerminusTemplateLoaderBlockEntity.Mode.values())
                .displayOnlyValue()
                .withInitialValue(mode)
                .create(width / 2 - 4 - 150, 185, 50, 20, Component.literal("MODE"), (button, value) -> {
                    mode = value;
                    updateMode();
                }));

        toggleBoundingBox = addRenderableWidget(CycleButton.onOffBuilder(showBoundingBox).displayOnlyValue()
                .create(width / 2 + 4 + 100, 80, 50, 20, SHOW_BOUNDING_BOX_LABEL, (button, value) -> showBoundingBox = value));

        templatePathEdit = new EditBox(font, width / 2 - 152, 40, 300, 20, TEMPLATE_PATH_LABEL);
        templatePathEdit.setMaxLength(128);
        templatePathEdit.setValue(currentTemplatePathValue());
        addWidget(templatePathEdit);

        posXEdit = new EditBox(font, width / 2 - 152, 80, 80, 20, Component.literal("X"));
        posXEdit.setMaxLength(15);
        posXEdit.setValue(Integer.toString(loader.getPosX()));
        addWidget(posXEdit);

        posYEdit = new EditBox(font, width / 2 - 72, 80, 80, 20, Component.literal("Y"));
        posYEdit.setMaxLength(15);
        posYEdit.setValue(Integer.toString(loader.getPosY()));
        addWidget(posYEdit);

        posZEdit = new EditBox(font, width / 2 + 8, 80, 80, 20, Component.literal("Z"));
        posZEdit.setMaxLength(15);
        posZEdit.setValue(Integer.toString(loader.getPosZ()));
        addWidget(posZEdit);

        presetNameEdit = new EditBox(font, width / 2 - 152, 120, 200, 20, PRESET_NAME_LABEL);
        presetNameEdit.setMaxLength(64);
        presetNameEdit.setValue(loader.getPresetName());
        addWidget(presetNameEdit);

        weightEdit = new EditBox(font, width / 2 + 56, 120, 80, 20, WEIGHT_LABEL);
        weightEdit.setMaxLength(8);
        weightEdit.setValue(Integer.toString(loader.getWeight()));
        addWidget(weightEdit);

        updateMode();
    }

    private String currentTemplatePathValue() {
        if (mode == TerminusTemplateLoaderBlockEntity.Mode.SAVE) {
            return loader.getTemplateName() != null ? loader.getTemplateName() : "";
        }
        if (loader.getTemplateId() != null) {
            return loader.getTemplateId().toString();
        }
        return "";
    }

    private void updateMode() {
        boolean save = mode == TerminusTemplateLoaderBlockEntity.Mode.SAVE;
        presetNameEdit.setVisible(save);
        weightEdit.setVisible(save);
        saveActionButton.visible = save;
        loadActionButton.visible = !save;
        toggleBoundingBox.visible = true;
        templatePathEdit.setValue(currentTemplatePathValue());
    }

    @Override
    public void tick() {
        templatePathEdit.tick();
        posXEdit.tick();
        posYEdit.tick();
        posZEdit.tick();
        presetNameEdit.tick();
        weightEdit.tick();
    }

    private void onDone(boolean runAction) {
        int x = parseInt(posXEdit.getValue());
        int y = parseInt(posYEdit.getValue());
        int z = parseInt(posZEdit.getValue());
        int weight = Math.max(1, parseInt(weightEdit.getValue()));
        String pathValue = templatePathEdit.getValue().trim();
        String templateName = "";
        ResourceLocation templateId = null;
        if (mode == TerminusTemplateLoaderBlockEntity.Mode.SAVE) {
            templateName = pathValue;
        } else if (!pathValue.isEmpty()) {
            templateId = ResourceLocation.tryParse(pathValue);
            if (templateId != null) {
                templateName = templateId.getPath();
            }
        }
        PacketHandlerRegistry.INSTANCE.sendToServer(new ServerboundTerminusTemplateLoaderPacket(
                loader.getBlockPos(),
                mode,
                templateName,
                templateId,
                x, y, z,
                presetNameEdit.getValue().trim(),
                weight,
                showBoundingBox,
                runAction
        ));
        minecraft.setScreen(null);
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        boolean save = mode == TerminusTemplateLoaderBlockEntity.Mode.SAVE;
        graphics.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);
        graphics.drawString(font, TEMPLATE_PATH_LABEL, width / 2 - 153, 30, 0xA0A0A0);
        templatePathEdit.render(graphics, mouseX, mouseY, partialTick);
        if (save) {
            graphics.drawString(font, PRESET_NAME_LABEL, width / 2 - 153, 110, 0xA0A0A0);
            graphics.drawString(font, WEIGHT_LABEL, width / 2 + 55, 110, 0xA0A0A0);
            presetNameEdit.render(graphics, mouseX, mouseY, partialTick);
            weightEdit.render(graphics, mouseX, mouseY, partialTick);
        }
        graphics.drawString(font, POSITION_LABEL, width / 2 - 153, 70, 0xA0A0A0);
        graphics.drawString(font, SHOW_BOUNDING_BOX_LABEL, width / 2 + 154 - font.width(SHOW_BOUNDING_BOX_LABEL), 70, 0xA0A0A0);
        posXEdit.render(graphics, mouseX, mouseY, partialTick);
        posYEdit.render(graphics, mouseX, mouseY, partialTick);
        posZEdit.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawString(font, Component.translatable("structure_block.mode_info." + mode.name().toLowerCase()),
                width / 2 - 153, 174, 0xA0A0A0);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
