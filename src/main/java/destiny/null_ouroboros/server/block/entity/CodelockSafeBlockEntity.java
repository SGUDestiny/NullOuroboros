package destiny.null_ouroboros.server.block.entity;

import destiny.null_ouroboros.server.registry.BlockEntityRegistry;
import destiny.null_ouroboros.server.registry.SoundRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;

public class CodelockSafeBlockEntity extends SafeBlockEntity {
    public static final int STATUS_NONE = 0;
    public static final int STATUS_ERR = 1;
    public static final int STATUS_INCR = 2;
    public static final int STATUS_CORR = 3;
    public static final int STATUS_SET = 4;
    public static final int STATUS_FLASH_TICKS = 20;

    public static final int KEY_1 = 0;
    public static final int KEY_2 = 1;
    public static final int KEY_3 = 2;
    public static final int KEY_4 = 3;
    public static final int KEY_5 = 4;
    public static final int KEY_6 = 5;
    public static final int KEY_7 = 6;
    public static final int KEY_8 = 7;
    public static final int KEY_9 = 8;
    public static final int KEY_X = 9;
    public static final int KEY_0 = 10;
    public static final int KEY_CONFIRM = 11;

    private boolean hasCode;
    private int code;
    private final StringBuilder input = new StringBuilder(4);
    private boolean wheelUnlocked;
    private boolean awaitingNewCode;
    private int screenStatus = STATUS_NONE;
    private int statusTicks;

    public CodelockSafeBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.CODELOCK_SAFE_BLOCK_ENTITY.get(), pos, state, false);
    }

    @Override
    public boolean canSpinBothWays() {
        if (level != null && level.isClientSide) {
            return clientWheelUnlocked;
        }
        return wheelUnlocked;
    }

    public boolean hasCode() {
        return hasCode;
    }

    public int getInputLength() {
        if (level != null && level.isClientSide) {
            return clientInputLength;
        }
        return input.length();
    }

    public int getScreenStatus() {
        if (level != null && level.isClientSide) {
            return clientScreenStatus;
        }
        return screenStatus;
    }

    public void handleKey(int keyId) {
        if (level == null || level.isClientSide) {
            return;
        }
        if (keyId < 0 || keyId > KEY_CONFIRM) {
            return;
        }

        setLitKey(keyId);

        if (keyId == KEY_X) {
            boolean hadInput = !input.isEmpty();
            clearInputAndLockWheel();
            if (hadInput) {
                playSound(SoundRegistry.KEYPAD_DENY.get());
            } else {
                playSound(SoundRegistry.KEYPAD_PRESS.get());
            }
            return;
        }

        if (screenStatus == STATUS_ERR || screenStatus == STATUS_INCR || screenStatus == STATUS_SET) {
            return;
        }

        if (keyId == KEY_CONFIRM) {
            handleConfirm();
            return;
        }

        if (screenStatus == STATUS_CORR) {
            return;
        }

        char digit = keyToDigit(keyId);
        if (digit == 0) {
            return;
        }
        if (input.length() >= 4) {
            return;
        }
        input.append(digit);
        playSound(SoundRegistry.KEYPAD_PRESS.get());
        setChangedAndSync();
    }

    private void handleConfirm() {
        if (canArmReprogram()) {
            input.setLength(0);
            awaitingNewCode = true;
            screenStatus = STATUS_NONE;
            statusTicks = 0;
            playSound(SoundRegistry.KEYPAD_PRESS.get());
            setChangedAndSync();
            return;
        }

        if (!hasCode || awaitingNewCode) {
            if (input.length() < 4) {
                flashStatus(STATUS_ERR);
                playSound(SoundRegistry.KEYPAD_DENY.get());
                return;
            }
            code = Integer.parseInt(input.toString());
            hasCode = true;
            awaitingNewCode = false;
            wheelUnlocked = true;
            flashStatus(STATUS_SET);
            playSound(SoundRegistry.KEYPAD_CONFIRM.get());
            return;
        }

        if (latchLocked) {
            if (input.length() < 4) {
                flashStatus(STATUS_ERR);
                playSound(SoundRegistry.KEYPAD_DENY.get());
                return;
            }
            int entered = Integer.parseInt(input.toString());
            if (entered != code) {
                flashStatus(STATUS_INCR);
                playSound(SoundRegistry.KEYPAD_DENY.get());
                return;
            }
            wheelUnlocked = true;
            flashStatus(STATUS_CORR);
            playSound(SoundRegistry.KEYPAD_CONFIRM.get());
        }
    }

    private boolean canArmReprogram() {
        if (!hasCode || awaitingNewCode) {
            return false;
        }
        if (screenStatus == STATUS_CORR) {
            return true;
        }
        if (!latchLocked) {
            return true;
        }
        return wheelUnlocked && input.isEmpty();
    }

    private void flashStatus(int status) {
        screenStatus = status;
        statusTicks = STATUS_FLASH_TICKS;
        setChangedAndSync();
    }

    private void clearInputAndLockWheel() {
        input.setLength(0);
        wheelUnlocked = false;
        awaitingNewCode = false;
        screenStatus = STATUS_NONE;
        statusTicks = 0;
        setChangedAndSync();
    }

    @Override
    protected void serverTick() {
        super.serverTick();
        if (statusTicks > 0) {
            statusTicks--;
            if (statusTicks <= 0 && (screenStatus == STATUS_ERR
                    || screenStatus == STATUS_INCR
                    || screenStatus == STATUS_CORR
                    || screenStatus == STATUS_SET)) {
                input.setLength(0);
                screenStatus = STATUS_NONE;
                setChangedAndSync();
            }
        }
    }

    private static char keyToDigit(int keyId) {
        return switch (keyId) {
            case KEY_1 -> '1';
            case KEY_2 -> '2';
            case KEY_3 -> '3';
            case KEY_4 -> '4';
            case KEY_5 -> '5';
            case KEY_6 -> '6';
            case KEY_7 -> '7';
            case KEY_8 -> '8';
            case KEY_9 -> '9';
            case KEY_0 -> '0';
            default -> 0;
        };
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.null_ouroboros.codelock_safe");
    }

    @Override
    protected void saveSafeExtra(CompoundTag tag) {
        tag.putBoolean("HasCode", hasCode);
        tag.putInt("Code", code);
        tag.putString("Input", input.toString());
        tag.putBoolean("WheelUnlocked", wheelUnlocked);
        tag.putBoolean("AwaitingNewCode", awaitingNewCode);
        tag.putInt("ScreenStatus", screenStatus);
        tag.putInt("StatusTicks", statusTicks);
    }

    @Override
    protected void loadSafeExtra(CompoundTag tag) {
        hasCode = tag.getBoolean("HasCode");
        code = tag.getInt("Code");
        input.setLength(0);
        input.append(tag.getString("Input"));
        if (input.length() > 4) {
            input.setLength(4);
        }
        wheelUnlocked = tag.getBoolean("WheelUnlocked");
        awaitingNewCode = tag.getBoolean("AwaitingNewCode");
        screenStatus = tag.getInt("ScreenStatus");
        statusTicks = tag.getInt("StatusTicks");
    }

    @Override
    protected void writeClientExtra(CompoundTag tag) {
        tag.putBoolean("WheelUnlocked", wheelUnlocked);
        tag.putInt("InputLength", input.length());
        tag.putInt("ScreenStatus", screenStatus);
    }

    @Override
    protected void readClientExtra(CompoundTag tag) {
        clientWheelUnlocked = tag.getBoolean("WheelUnlocked");
        clientInputLength = tag.getInt("InputLength");
        clientScreenStatus = tag.getInt("ScreenStatus");
    }
}
