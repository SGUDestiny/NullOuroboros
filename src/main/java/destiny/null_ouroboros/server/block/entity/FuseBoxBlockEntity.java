package destiny.null_ouroboros.server.block.entity;

import destiny.null_ouroboros.server.fuse.FusePowerTracker;
import destiny.null_ouroboros.server.item.FuseItem;
import destiny.null_ouroboros.server.menu.FuseBoxMenu;
import destiny.null_ouroboros.server.registry.BlockEntityRegistry;
import destiny.null_ouroboros.server.registry.ItemRegistry;
import destiny.null_ouroboros.server.registry.SoundRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class FuseBoxBlockEntity extends BlockEntity implements GeoBlockEntity, MenuProvider {
    public static final int SLOT_COUNT = 14;
    private static final int CYCLE_OPEN_TICKS = 20;
    private static final int CYCLE_TOTAL_TICKS = 40;
    private static final int PULSE_DURATION_TICKS = 10;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final boolean[] switchOn = new boolean[SLOT_COUNT];
    private final boolean[] powerActive = new boolean[SLOT_COUNT];
    private final int[] pulseTicks = new int[SLOT_COUNT];
    private final BlockPos[] poweredLinks = new BlockPos[SLOT_COUNT];
    private final ItemStackHandler inventory = new ItemStackHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            ItemStack stack = getStackInSlot(slot);
            if (stack.isEmpty()) {
                if (switchOn[slot] || powerActive[slot]) {
                    clearPower(slot);
                    switchOn[slot] = false;
                }
                if (!suppressSlotSounds && level != null && !level.isClientSide) {
                    playSound(SoundRegistry.FUSE_BOX_TAKE.get());
                }
            } else if (!suppressSlotSounds && level != null && !level.isClientSide) {
                playSound(SoundRegistry.FUSE_BOX_INSERT.get());
            }
            setChangedAndSync();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return stack.is(ItemRegistry.FUSE.get());
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    };

    private LazyOptional<IItemHandler> itemHandler = LazyOptional.of(() -> inventory);
    private int openCount;
    private int cycleTicks = -1;
    private boolean lastRedstone;
    private boolean pendingCycle;
    private boolean suppressSlotSounds;
    private boolean powerRestored;
    private boolean clientOpen;
    private boolean clientCycling;

    public FuseBoxBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.FUSE_BOX_BLOCK_ENTITY.get(), pos, state);
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public boolean isSwitchOn(int slot) {
        return slot >= 0 && slot < SLOT_COUNT && switchOn[slot];
    }

    public boolean hasFuse(int slot) {
        return slot >= 0 && slot < SLOT_COUNT && !inventory.getStackInSlot(slot).isEmpty();
    }

    public boolean isOpen() {
        if (level != null && level.isClientSide) {
            return clientOpen;
        }
        return openCount > 0 || cycleTicks >= 0;
    }

    public boolean isCycling() {
        if (level != null && level.isClientSide) {
            return clientCycling;
        }
        return cycleTicks >= 0;
    }

    public void startOpen(Player player) {
        if (player.isSpectator() || level == null || level.isClientSide) {
            return;
        }
        if (openCount < 0) {
            openCount = 0;
        }
        boolean wasOpen = isOpen();
        openCount++;
        if (!wasOpen) {
            playSound(SoundRegistry.FUSE_BOX_OPEN.get());
            setChangedAndSync();
        }
    }

    public void stopOpen(Player player) {
        if (player.isSpectator() || level == null || level.isClientSide) {
            return;
        }
        openCount = Math.max(0, openCount - 1);
        if (!isOpen()) {
            playSound(SoundRegistry.FUSE_BOX_CLOSE.get());
            setChangedAndSync();
        }
    }

    public boolean tryStartCycle() {
        if (level == null || level.isClientSide || isCycling()) {
            return false;
        }
        boolean wasOpen = isOpen();
        cycleTicks = 0;
        if (!wasOpen) {
            playSound(SoundRegistry.FUSE_BOX_OPEN.get());
        }
        setChangedAndSync();
        return true;
    }

    public void cycleInstantly() {
        if (level == null || level.isClientSide) {
            return;
        }
        if (cycleTicks >= 0 && cycleTicks < CYCLE_OPEN_TICKS) {
            cycleTicks = CYCLE_OPEN_TICKS;
        }
        pendingCycle = false;

        boolean anySwitchedOn = false;
        boolean anySwitchedOff = false;
        boolean anyBlown = false;
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (inventory.getStackInSlot(i).isEmpty()) {
                continue;
            }
            boolean wasOn = switchOn[i];
            boolean blown = setSwitch(i, !wasOn, false, true);
            if (blown) {
                anyBlown = true;
                anySwitchedOn = true;
            } else if (switchOn[i] != wasOn) {
                if (switchOn[i]) {
                    anySwitchedOn = true;
                } else {
                    anySwitchedOff = true;
                }
            }
        }
        if (anySwitchedOn) {
            playSound(SoundRegistry.FUSE_BOX_SWITCH_ON.get());
        }
        if (anySwitchedOff) {
            playSound(SoundRegistry.FUSE_BOX_SWITCH_OFF.get());
        }
        if (anyBlown) {
            playSound(SoundRegistry.FUSE_BOX_BLOW.get());
        }
        setChangedAndSync();
    }

    public boolean toggleSwitch(int slot) {
        if (level == null || level.isClientSide || isCycling() || slot < 0 || slot >= SLOT_COUNT) {
            return false;
        }
        if (inventory.getStackInSlot(slot).isEmpty()) {
            return false;
        }
        setSwitch(slot, !switchOn[slot], true, false);
        return true;
    }

    public void onRemove() {
        if (level instanceof ServerLevel serverLevel) {
            for (int i = 0; i < SLOT_COUNT; i++) {
                clearPower(i);
            }
            FusePowerTracker.clearBox(serverLevel, worldPosition);
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FuseBoxBlockEntity box) {
        if (level.isClientSide) {
            return;
        }

        if (!box.powerRestored) {
            box.powerRestored = true;
            for (int i = 0; i < SLOT_COUNT; i++) {
                if (box.switchOn[i] && !box.inventory.getStackInSlot(i).isEmpty()) {
                    box.applyPower(i);
                }
            }
        }

        for (int i = 0; i < SLOT_COUNT; i++) {
            if (box.pulseTicks[i] > 0) {
                box.pulseTicks[i]--;
                if (box.pulseTicks[i] <= 0) {
                    box.switchOn[i] = false;
                    box.clearPower(i);
                    box.playSound(SoundRegistry.FUSE_BOX_SWITCH_OFF.get());
                    box.setChangedAndSync();
                }
            }
        }

        if (box.cycleTicks >= 0) {
            box.cycleTicks++;
            if (box.cycleTicks == CYCLE_OPEN_TICKS) {
                box.cycleAllSwitches();
            } else if (box.cycleTicks >= CYCLE_TOTAL_TICKS) {
                if (box.pendingCycle) {
                    box.pendingCycle = false;
                    box.cycleTicks = 0;
                    box.setChangedAndSync();
                } else {
                    box.cycleTicks = -1;
                    if (box.openCount <= 0) {
                        box.playSound(SoundRegistry.FUSE_BOX_CLOSE.get());
                    }
                    box.setChangedAndSync();
                }
            }
        }
    }

    private void cycleAllSwitches() {
        boolean anyBlown = false;
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (inventory.getStackInSlot(i).isEmpty()) {
                continue;
            }
            boolean blown = setSwitch(i, !switchOn[i], true, true);
            if (blown) {
                anyBlown = true;
            }
        }
        if (anyBlown) {
            playSound(SoundRegistry.FUSE_BOX_BLOW.get());
        }
        setChangedAndSync();
    }

    private boolean setSwitch(int slot, boolean on, boolean playSwitchSound, boolean suppressBlowSound) {
        if (inventory.getStackInSlot(slot).isEmpty()) {
            return false;
        }

        boolean wasOn = switchOn[slot];
        if (wasOn == on) {
            return false;
        }

        boolean blown = false;
        if (on) {
            ItemStack fuse = inventory.getStackInSlot(slot);
            if (fuse.hurt(1, level.getRandom(), null)) {
                suppressSlotSounds = true;
                inventory.setStackInSlot(slot, ItemStack.EMPTY);
                suppressSlotSounds = false;
                switchOn[slot] = false;
                clearPower(slot);
                blown = true;
                if (playSwitchSound) {
                    playSound(SoundRegistry.FUSE_BOX_SWITCH_ON.get());
                }
                if (!suppressBlowSound) {
                    playSound(SoundRegistry.FUSE_BOX_BLOW.get());
                }
                setChangedAndSync();
                return true;
            }

            switchOn[slot] = true;
            applyPower(slot);
            if (playSwitchSound) {
                playSound(SoundRegistry.FUSE_BOX_SWITCH_ON.get());
            }
        } else {
            switchOn[slot] = false;
            clearPower(slot);
            if (playSwitchSound) {
                playSound(SoundRegistry.FUSE_BOX_SWITCH_OFF.get());
            }
        }

        setChangedAndSync();
        return blown;
    }

    private void applyPower(int slot) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        ItemStack fuse = inventory.getStackInSlot(slot);
        BlockPos linked = FuseItem.getLinkedPos(fuse);
        if (linked == null) {
            return;
        }

        if (powerActive[slot]) {
            clearPower(slot);
        }

        FusePowerTracker.addPower(serverLevel, worldPosition, linked);
        poweredLinks[slot] = linked.immutable();
        powerActive[slot] = true;

        if (FuseItem.isPulse(fuse)) {
            pulseTicks[slot] = PULSE_DURATION_TICKS;
        } else {
            pulseTicks[slot] = 0;
        }
    }

    private void clearPower(int slot) {
        pulseTicks[slot] = 0;
        if (!powerActive[slot] || !(level instanceof ServerLevel serverLevel)) {
            powerActive[slot] = false;
            poweredLinks[slot] = null;
            return;
        }

        BlockPos linked = poweredLinks[slot];
        if (linked != null) {
            FusePowerTracker.removePower(serverLevel, worldPosition, linked);
        }
        poweredLinks[slot] = null;
        powerActive[slot] = false;
    }

    private void playSound(net.minecraft.sounds.SoundEvent sound) {
        if (level == null || level.isClientSide) {
            return;
        }
        level.playSound(null, worldPosition, sound, SoundSource.BLOCKS, 1f, 1f);
    }

    private void setChangedAndSync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.null_ouroboros.fuse_box");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new FuseBoxMenu(containerId, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", inventory.serializeNBT());
        byte[] switches = new byte[SLOT_COUNT];
        for (int i = 0; i < SLOT_COUNT; i++) {
            switches[i] = (byte) (switchOn[i] ? 1 : 0);
        }
        tag.putByteArray("Switches", switches);
        tag.putBoolean("LastRedstone", lastRedstone);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(tag.getCompound("Inventory"));
        }
        if (tag.contains("Switches")) {
            byte[] switches = tag.getByteArray("Switches");
            for (int i = 0; i < SLOT_COUNT && i < switches.length; i++) {
                switchOn[i] = switches[i] != 0;
            }
        }
        openCount = 0;
        cycleTicks = -1;
        pendingCycle = false;
        lastRedstone = tag.getBoolean("LastRedstone");
        powerRestored = false;
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        tag.putBoolean("Open", openCount > 0 || cycleTicks >= 0);
        tag.putBoolean("Cycling", cycleTicks >= 0);
        return tag;
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
        clientOpen = tag.getBoolean("Open");
        clientCycling = tag.getBoolean("Cycling");
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            handleUpdateTag(tag);
        }
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemHandler.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        itemHandler = LazyOptional.of(() -> inventory);
    }

    public boolean getLastRedstone() {
        return lastRedstone;
    }

    public void setLastRedstone(boolean lastRedstone) {
        this.lastRedstone = lastRedstone;
    }

    public void setPendingCycle(boolean pendingCycle) {
        this.pendingCycle = pendingCycle;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
