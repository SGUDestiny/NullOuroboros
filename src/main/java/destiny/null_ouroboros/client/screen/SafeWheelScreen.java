package destiny.null_ouroboros.client.screen;

import com.mojang.math.Axis;
import destiny.null_ouroboros.NullOuroboros;
import destiny.null_ouroboros.server.block.entity.CodelockSafeBlockEntity;
import destiny.null_ouroboros.server.block.entity.DeadlockSafeBlockEntity;
import destiny.null_ouroboros.server.block.entity.SafeBlockEntity;
import destiny.null_ouroboros.server.menu.SafeWheelMenu;
import destiny.null_ouroboros.server.network.ServerboundSafeKeypadPacket;
import destiny.null_ouroboros.server.network.ServerboundSafeWheelRotatePacket;
import destiny.null_ouroboros.server.registry.PacketHandlerRegistry;
import destiny.null_ouroboros.server.registry.SoundRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public class SafeWheelScreen extends AbstractContainerScreen<SafeWheelMenu> {
    private static final int TEX_SIZE = 512;
    private static final int WHEEL_W = 144;
    private static final int WHEEL_H = 144;
    private static final int DIME_W = 85;
    private static final int DIME_H = 86;
    private static final int KEY_SIZE = 16;
    private static final int KEYPAD_COLS = 3;
    private static final int KEYPAD_ROWS = 4;
    private static final int KEYPAD_SHADE_W = 56;
    private static final int KEYPAD_SHADE_H = 87;
    private static final int LIGHT_SHADE_W = 55;
    private static final int LIGHT_SHADE_H = 55;
    private static final int SCREEN_W = 48;
    private static final int SCREEN_H = 15;
    private static final int LIGHT_W = 47;
    private static final int LIGHT_H = 47;

    private final ResourceLocation texture;
    private final boolean codelock;

    private boolean dragging;
    private float lastAngle = Float.NaN;
    private float displayDegrees;
    private float sentDegrees;
    private int lastSoundMark = Integer.MIN_VALUE;

    public SafeWheelScreen(SafeWheelMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        SafeBlockEntity safe = menu.getBlockEntity();
        this.codelock = safe instanceof CodelockSafeBlockEntity;
        this.texture = ResourceLocation.fromNamespaceAndPath(NullOuroboros.MODID,
                codelock ? "textures/gui/codelock_safe_gui.png" : "textures/gui/deadlock_safe_gui.png");
        this.imageWidth = WHEEL_W;
        this.imageHeight = WHEEL_H;
        this.displayDegrees = safe.getWheelDegrees();
        this.sentDegrees = this.displayDegrees;
        this.lastSoundMark = soundMark(this.displayDegrees);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = -1000;
        this.inventoryLabelX = -1000;
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        SafeBlockEntity safe = menu.getBlockEntity();
        if (!dragging) {
            displayDegrees = safe.getWheelDegrees();
            sentDegrees = displayDegrees;
        }

        int wheelX = leftPos;
        int wheelY = topPos;
        float cx = wheelX + WHEEL_H * 0.5f;
        float cy = wheelY + WHEEL_H * 0.5f;

        graphics.pose().pushPose();
        graphics.pose().translate(cx, cy, 0);
        graphics.pose().mulPose(Axis.ZP.rotationDegrees(displayDegrees));
        graphics.pose().translate(-WHEEL_H * 0.5f, -WHEEL_H * 0.5f, 0);
        graphics.blit(texture, 0, 0, 0, 0, WHEEL_W, WHEEL_H, TEX_SIZE, TEX_SIZE);
        graphics.pose().popPose();

        int dimeX = leftPos + (WHEEL_W - DIME_W) / 2;
        int dimeY = topPos + (WHEEL_H - DIME_H) / 2;
        graphics.blit(texture, dimeX, dimeY, 0, 144, DIME_W, DIME_H, TEX_SIZE, TEX_SIZE);

        if (codelock) {
            renderCodelockOverlay(graphics, safe);
        } else {
            renderDeadlockOverlay(graphics, safe);
        }
    }

    private void renderDeadlockOverlay(GuiGraphics graphics, SafeBlockEntity safe) {
        int shadeX = leftPos + (WHEEL_W - LIGHT_SHADE_W) / 2;
        int shadeY = topPos + (WHEEL_H - LIGHT_SHADE_H) / 2;
        graphics.blit(texture, shadeX, shadeY, 0, 286, LIGHT_SHADE_W, LIGHT_SHADE_H, TEX_SIZE, TEX_SIZE);

        boolean on = safe instanceof DeadlockSafeBlockEntity deadlock && deadlock.isIndicatorOn();
        int lightU = on ? 4 : 60;
        int lightX = shadeX + (LIGHT_SHADE_W - LIGHT_W) / 2;
        int lightY = shadeY + (LIGHT_SHADE_H - LIGHT_H) / 2;
        graphics.blit(texture, lightX, lightY, lightU, 234, LIGHT_W, LIGHT_H, TEX_SIZE, TEX_SIZE);
    }

    private void renderCodelockOverlay(GuiGraphics graphics, SafeBlockEntity safe) {
        int shadeX = leftPos + (WHEEL_W - (KEYPAD_SHADE_W - 1)) / 2;
        int shadeY = topPos + (WHEEL_H - KEYPAD_SHADE_H) / 2;
        graphics.blit(texture, shadeX, shadeY, 0, 318, KEYPAD_SHADE_W, KEYPAD_SHADE_H, TEX_SIZE, TEX_SIZE);

        boolean screenOn = safe.isWheelUnlockedClient()
                || (safe instanceof CodelockSafeBlockEntity code
                && (code.getScreenStatus() == CodelockSafeBlockEntity.STATUS_CORR
                || code.getScreenStatus() == CodelockSafeBlockEntity.STATUS_SET));
        int screenU = screenOn ? 4 : 60;
        int contentW = KEYPAD_SHADE_W - 1;
        int screenX = shadeX + (contentW - SCREEN_W) / 2 + 1;
        int screenY = shadeY + 4;
        graphics.blit(texture, screenX, screenY, screenU, 234, SCREEN_W, SCREEN_H, TEX_SIZE, TEX_SIZE);

        int keysX = shadeX + (contentW - KEY_SIZE * KEYPAD_COLS) / 2 + 1;
        int keysY = screenY + SCREEN_H;
        int litKey = safe.getLitKey();
        for (int i = 0; i < 12; i++) {
            int col = i % KEYPAD_COLS;
            int row = i / KEYPAD_COLS;
            boolean lit = litKey == i;
            int u = (lit ? 4 : 60) + col * KEY_SIZE;
            int v = 250 + row * KEY_SIZE;
            graphics.blit(texture, keysX + col * KEY_SIZE, keysY + row * KEY_SIZE, u, v, KEY_SIZE, KEY_SIZE, TEX_SIZE, TEX_SIZE);
        }

        if (safe instanceof CodelockSafeBlockEntity code) {
            renderScreenText(graphics, screenX, screenY, code, screenOn);
        }
    }

    private void renderScreenText(GuiGraphics graphics, int screenX, int screenY, CodelockSafeBlockEntity code, boolean screenOn) {
        String text;
        int status = code.getScreenStatus();
        if (status == CodelockSafeBlockEntity.STATUS_ERR) {
            text = trimmedTranslation("gui.null_ouroboros.codelock_safe.error");
        } else if (status == CodelockSafeBlockEntity.STATUS_INCR) {
            text = trimmedTranslation("gui.null_ouroboros.codelock_safe.incorrect");
        } else if (status == CodelockSafeBlockEntity.STATUS_CORR) {
            text = trimmedTranslation("gui.null_ouroboros.codelock_safe.correct");
        } else if (status == CodelockSafeBlockEntity.STATUS_SET) {
            text = trimmedTranslation("gui.null_ouroboros.codelock_safe.set");
        } else {
            text = spacedStars(code.getInputLength());
        }
        if (text.isEmpty()) {
            return;
        }

        float fieldWidth = font.width("* * * *");
        float fieldX = screenX + (SCREEN_W - fieldWidth) * 0.5f;
        int textY = screenY + (SCREEN_H - 8) / 2 + 3;
        if (status == CodelockSafeBlockEntity.STATUS_ERR
                || status == CodelockSafeBlockEntity.STATUS_INCR
                || status == CodelockSafeBlockEntity.STATUS_CORR
                || status == CodelockSafeBlockEntity.STATUS_SET) {
            fieldX += 2;
            textY -= 1;
        }
        int color = screenOn ? 0x281A2D : 0xEE243D;
        graphics.drawString(font, text, Math.round(fieldX), textY, color, false);
    }

    private static String spacedStars(int count) {
        if (count <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(count * 2 - 1);
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append('*');
        }
        return sb.toString();
    }

    private static String trimmedTranslation(String key) {
        String value = Component.translatable(key).getString();
        if (value.length() <= 4) {
            return value;
        }
        return value.substring(0, 4);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && codelock) {
            int key = hitKey(mouseX, mouseY);
            if (key >= 0) {
                PacketHandlerRegistry.INSTANCE.sendToServer(
                        new ServerboundSafeKeypadPacket(menu.getBlockEntity().getBlockPos(), key));
                return true;
            }
        }
        if (button == 0 && hitWheel(mouseX, mouseY) && !hitKeypadArea(mouseX, mouseY)) {
            dragging = true;
            lastAngle = angleAt(mouseX, mouseY);
            lastSoundMark = soundMark(displayDegrees);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            dragging = false;
            lastAngle = Float.NaN;
            flushRotation();
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging && button == 0) {
            float angle = angleAt(mouseX, mouseY);
            if (!Float.isNaN(lastAngle)) {
                float delta = Mth.wrapDegrees(angle - lastAngle);
                SafeBlockEntity safe = menu.getBlockEntity();
                boolean bothWays = safe.isWheelUnlockedClient();
                boolean latchLocked = safe.isLatchLocked();
                if (delta < 0f && !bothWays) {
                    delta = 0f;
                }
                float prev = displayDegrees;
                float next = Mth.clamp(displayDegrees + delta, -SafeBlockEntity.FULL_TURN, 0f);
                delta = next - displayDegrees;
                if (delta != 0f) {
                    displayDegrees = next;
                    int mark = soundMark(displayDegrees);
                    if (mark != lastSoundMark) {
                        Minecraft.getInstance().getSoundManager().play(
                                SimpleSoundInstance.forUI(SoundRegistry.SAFE_ROTATE.get(), 1f));
                        lastSoundMark = mark;
                    }
                    if (Math.abs(displayDegrees - sentDegrees) >= 1f
                            || displayDegrees <= -SafeBlockEntity.FULL_TURN
                            || (prev < 0f && displayDegrees >= 0f)) {
                        float sendDelta = displayDegrees - sentDegrees;
                        PacketHandlerRegistry.INSTANCE.sendToServer(
                                new ServerboundSafeWheelRotatePacket(safe.getBlockPos(), sendDelta));
                        sentDegrees = displayDegrees;
                    }
                    if (prev < 0f && displayDegrees >= 0f && !latchLocked) {
                        displayDegrees = 0f;
                        sentDegrees = 0f;
                        lastSoundMark = soundMark(displayDegrees);
                    }
                }
            }
            lastAngle = angle;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    private static int soundMark(float degrees) {
        return Mth.floor(degrees / 30f);
    }

    private void flushRotation() {
        float sendDelta = displayDegrees - sentDegrees;
        if (Math.abs(sendDelta) >= 0.01f) {
            PacketHandlerRegistry.INSTANCE.sendToServer(
                    new ServerboundSafeWheelRotatePacket(menu.getBlockEntity().getBlockPos(), sendDelta));
            sentDegrees = displayDegrees;
        }
    }

    private float angleAt(double mouseX, double mouseY) {
        float cx = leftPos + WHEEL_H * 0.5f;
        float cy = topPos + WHEEL_H * 0.5f;
        return (float) Math.toDegrees(Math.atan2(mouseY - cy, mouseX - cx));
    }

    private boolean hitWheel(double mouseX, double mouseY) {
        float cx = leftPos + WHEEL_H * 0.5f;
        float cy = topPos + WHEEL_H * 0.5f;
        float dx = (float) mouseX - cx;
        float dy = (float) mouseY - cy;
        return dx * dx + dy * dy <= (WHEEL_H * 0.5f) * (WHEEL_H * 0.5f);
    }

    private boolean hitKeypadArea(double mouseX, double mouseY) {
        if (!codelock) {
            return false;
        }
        int shadeX = leftPos + (WHEEL_W - (KEYPAD_SHADE_W - 1)) / 2;
        int shadeY = topPos + (WHEEL_H - KEYPAD_SHADE_H) / 2;
        return mouseX >= shadeX && mouseX < shadeX + KEYPAD_SHADE_W
                && mouseY >= shadeY && mouseY < shadeY + KEYPAD_SHADE_H;
    }

    private int hitKey(double mouseX, double mouseY) {
        if (!codelock) {
            return -1;
        }
        int shadeX = leftPos + (WHEEL_W - (KEYPAD_SHADE_W - 1)) / 2;
        int shadeY = topPos + (WHEEL_H - KEYPAD_SHADE_H) / 2;
        int contentW = KEYPAD_SHADE_W - 1;
        int screenY = shadeY + 4;
        int keysX = shadeX + (contentW - KEY_SIZE * KEYPAD_COLS) / 2 + 1;
        int keysY = screenY + SCREEN_H;
        int relX = (int) mouseX - keysX;
        int relY = (int) mouseY - keysY;
        if (relX < 0 || relY < 0 || relX >= KEY_SIZE * KEYPAD_COLS || relY >= KEY_SIZE * KEYPAD_ROWS) {
            return -1;
        }
        int col = relX / KEY_SIZE;
        int row = relY / KEY_SIZE;
        return row * KEYPAD_COLS + col;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
