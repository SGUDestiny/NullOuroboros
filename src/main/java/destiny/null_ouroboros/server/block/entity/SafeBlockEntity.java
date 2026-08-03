package destiny.null_ouroboros.server.block.entity;

import destiny.null_ouroboros.server.menu.SafeInventoryMenu;
import destiny.null_ouroboros.server.registry.SoundRegistry;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;

public abstract class SafeBlockEntity extends BlockEntity implements GeoBlockEntity, MenuProvider {
    public static final int SLOT_COUNT = 25;
    public static final int LIT_KEY_TICKS = 5;
    public static final float FULL_TURN = 360f;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ItemStackHandler inventory = new ItemStackHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            if (!resolvingLoot) {
                setChangedAndSync();
            }
        }

        @Override
        public void setStackInSlot(int slot, @NotNull ItemStack stack) {
            if (!resolvingLoot) {
                unpackLootTable(null);
            }
            super.setStackInSlot(slot, stack);
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            if (!resolvingLoot) {
                unpackLootTable(null);
            }
            return super.getStackInSlot(slot);
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (!resolvingLoot) {
                unpackLootTable(null);
            }
            return super.insertItem(slot, stack, simulate);
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (!resolvingLoot) {
                unpackLootTable(null);
            }
            return super.extractItem(slot, amount, simulate);
        }
    };

    private LazyOptional<IItemHandler> itemHandler = LazyOptional.of(() -> inventory);

    protected boolean latchLocked;
    protected float wheelDegrees;
    protected int openCount;
    protected int litKey = -1;
    protected int litKeyTicks;
    protected boolean clientOpen;
    protected boolean clientLatchLocked;
    protected float clientWheelDegrees;
    protected boolean clientWheelUnlocked;
    protected int clientLitKey = -1;
    protected int clientInputLength;
    protected int clientScreenStatus;
    @Nullable
    protected ResourceLocation lootTable;
    protected long lootTableSeed;
    private boolean resolvingLoot;

    protected SafeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, boolean initialLatchLocked) {
        super(type, pos, state);
        this.latchLocked = initialLatchLocked;
    }

    public static void setLootTable(BlockGetter level, RandomSource random, BlockPos pos, ResourceLocation lootTable) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof SafeBlockEntity safe) {
            safe.setLootTable(lootTable, random.nextLong());
        }
    }

    public void setLootTable(ResourceLocation lootTable, long seed) {
        this.lootTable = lootTable;
        this.lootTableSeed = seed;
        setChanged();
    }

    @Nullable
    public ResourceLocation getLootTable() {
        return lootTable;
    }

    public void unpackLootTable(@Nullable Player player) {
        if (resolvingLoot || lootTable == null || level == null || level.isClientSide || level.getServer() == null) {
            return;
        }
        LootTable table = level.getServer().getLootData().getLootTable(lootTable);
        if (player instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.GENERATE_LOOT.trigger(serverPlayer, lootTable);
        }
        lootTable = null;
        long seed = lootTableSeed;
        lootTableSeed = 0L;

        LootParams.Builder builder = new LootParams.Builder((ServerLevel) level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(worldPosition));
        if (player != null) {
            builder.withLuck(player.getLuck()).withParameter(LootContextParams.THIS_ENTITY, player);
        }

        resolvingLoot = true;
        try {
            SimpleContainer temp = new SimpleContainer(SLOT_COUNT);
            table.fill(temp, builder.create(LootContextParamSets.CHEST), seed);
            for (int i = 0; i < SLOT_COUNT; i++) {
                inventory.setStackInSlot(i, temp.getItem(i));
            }
        } finally {
            resolvingLoot = false;
        }
        setChangedAndSync();
    }

    private boolean tryLoadLootTable(CompoundTag tag) {
        if (tag.contains(RandomizableContainerBlockEntity.LOOT_TABLE_TAG, 8)) {
            lootTable = new ResourceLocation(tag.getString(RandomizableContainerBlockEntity.LOOT_TABLE_TAG));
            lootTableSeed = tag.getLong(RandomizableContainerBlockEntity.LOOT_TABLE_SEED_TAG);
            return true;
        }
        return false;
    }

    private boolean trySaveLootTable(CompoundTag tag) {
        if (lootTable == null) {
            return false;
        }
        tag.putString(RandomizableContainerBlockEntity.LOOT_TABLE_TAG, lootTable.toString());
        if (lootTableSeed != 0L) {
            tag.putLong(RandomizableContainerBlockEntity.LOOT_TABLE_SEED_TAG, lootTableSeed);
        }
        return true;
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public boolean isLatchLocked() {
        if (level != null && level.isClientSide) {
            return clientLatchLocked;
        }
        return latchLocked;
    }

    public boolean isVisuallyOpen() {
        if (level != null && level.isClientSide) {
            return clientOpen;
        }
        return openCount > 0;
    }

    public float getWheelDegrees() {
        if (level != null && level.isClientSide) {
            return clientWheelDegrees;
        }
        return wheelDegrees;
    }

    public int getLitKey() {
        if (level != null && level.isClientSide) {
            return clientLitKey;
        }
        return litKey;
    }

    public abstract boolean canSpinBothWays();

    public float getWheelMinDegrees() {
        return -FULL_TURN;
    }

    public float getWheelMaxDegrees() {
        return 0f;
    }

    public boolean isWheelUnlockedClient() {
        return clientWheelUnlocked;
    }

    public int getClientInputLength() {
        return clientInputLength;
    }

    public int getClientScreenStatus() {
        return clientScreenStatus;
    }

    public void startOpen(Player player) {
        if (player.isSpectator() || level == null || level.isClientSide) {
            return;
        }
        unpackLootTable(player);
        if (openCount < 0) {
            openCount = 0;
        }
        boolean wasOpen = openCount > 0;
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
        if (openCount <= 0) {
            playSound(SoundRegistry.FUSE_BOX_CLOSE.get());
            setChangedAndSync();
        }
    }

    public enum WheelResult {
        NONE,
        LATCH_UNLOCKED,
        LATCH_LOCKED
    }

    public WheelResult applyWheelDelta(float deltaDegrees, @Nullable ServerPlayer actor) {
        if (level == null || level.isClientSide || deltaDegrees == 0f) {
            return WheelResult.NONE;
        }

        boolean bothWays = canSpinBothWays();
        if (deltaDegrees < 0f && !bothWays) {
            return WheelResult.NONE;
        }

        float next = net.minecraft.util.Mth.clamp(wheelDegrees + deltaDegrees, -FULL_TURN, 0f);
        if (next == wheelDegrees) {
            return WheelResult.NONE;
        }

        float prev = wheelDegrees;
        boolean wasLocked = latchLocked;
        wheelDegrees = next;

        if (wheelDegrees <= -FULL_TURN && wasLocked && bothWays) {
            unlockLatch(actor);
            return WheelResult.LATCH_UNLOCKED;
        }
        if (prev < 0f && wheelDegrees >= 0f && !wasLocked) {
            wheelDegrees = 0f;
            lockLatch();
            return WheelResult.LATCH_LOCKED;
        }

        setChangedAndSync();
        return WheelResult.NONE;
    }

    public void unlockLatch(@Nullable ServerPlayer actor) {
        if (level == null || level.isClientSide || !latchLocked) {
            return;
        }
        latchLocked = false;
        playSound(SoundRegistry.SAFE_LOCK.get());
        setChangedAndSync();
        if (actor != null) {
            openInventory(actor);
        }
    }

    public void lockLatch() {
        if (level == null || level.isClientSide || latchLocked) {
            return;
        }
        latchLocked = true;
        wheelDegrees = 0f;
        onLatchLocked();
        playSound(SoundRegistry.SAFE_LOCK.get());
        kickInventoryViewers();
        setChangedAndSync();
    }

    protected void onLatchLocked() {
    }

    public void openInventory(ServerPlayer player) {
        if (level == null || level.isClientSide) {
            return;
        }
        NetworkHooks.openScreen(player, this, buf -> buf.writeBlockPos(worldPosition));
    }

    public void openWheel(ServerPlayer player) {
        if (level == null || level.isClientSide) {
            return;
        }
        MenuProvider provider = new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return SafeBlockEntity.this.getDisplayName();
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                return new destiny.null_ouroboros.server.menu.SafeWheelMenu(id, inv, SafeBlockEntity.this);
            }
        };
        NetworkHooks.openScreen(player, provider, buf -> buf.writeBlockPos(worldPosition));
    }

    public void kickInventoryViewers() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        List<ServerPlayer> viewers = new ArrayList<>();
        for (ServerPlayer player : serverLevel.players()) {
            if (player.containerMenu instanceof SafeInventoryMenu menu && menu.getBlockEntity() == this) {
                viewers.add(player);
            }
        }
        for (ServerPlayer player : viewers) {
            player.closeContainer();
        }
    }

    public void setLitKey(int key) {
        litKey = key;
        litKeyTicks = LIT_KEY_TICKS;
        setChangedAndSync();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SafeBlockEntity safe) {
        if (level.isClientSide) {
            return;
        }
        safe.serverTick();
    }

    protected void serverTick() {
        if (litKeyTicks > 0) {
            litKeyTicks--;
            if (litKeyTicks <= 0) {
                litKey = -1;
                setChangedAndSync();
            }
        }
    }

    public ItemStack createItemStack() {
        ItemStack stack = new ItemStack(getBlockState().getBlock());
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        tag.remove("OpenCount");
        stack.getOrCreateTag().put("BlockEntityTag", tag);
        return stack;
    }

    public void clearInventoryForDrop() {
        lootTable = null;
        lootTableSeed = 0L;
        resolvingLoot = true;
        try {
            for (int i = 0; i < inventory.getSlots(); i++) {
                inventory.setStackInSlot(i, ItemStack.EMPTY);
            }
        } finally {
            resolvingLoot = false;
        }
    }

    protected void playSound(net.minecraft.sounds.SoundEvent sound) {
        if (level == null || level.isClientSide) {
            return;
        }
        level.playSound(null, worldPosition, sound, SoundSource.BLOCKS, 1f, 1f);
    }

    protected void setChangedAndSync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    @Override
    public abstract Component getDisplayName();

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        unpackLootTable(player);
        return new SafeInventoryMenu(containerId, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!trySaveLootTable(tag)) {
            tag.put("Inventory", inventory.serializeNBT());
        }
        tag.putBoolean("LatchLocked", latchLocked);
        tag.putFloat("WheelDegrees", wheelDegrees);
        saveSafeExtra(tag);
    }

    protected void saveSafeExtra(CompoundTag tag) {
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (!tryLoadLootTable(tag) && tag.contains("Inventory")) {
            resolvingLoot = true;
            try {
                inventory.deserializeNBT(tag.getCompound("Inventory"));
            } finally {
                resolvingLoot = false;
            }
        }
        latchLocked = tag.getBoolean("LatchLocked");
        wheelDegrees = net.minecraft.util.Mth.clamp(tag.getFloat("WheelDegrees"), -FULL_TURN, 0f);
        openCount = 0;
        litKey = -1;
        litKeyTicks = 0;
        loadSafeExtra(tag);
    }

    protected void loadSafeExtra(CompoundTag tag) {
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        tag.putBoolean("Open", openCount > 0);
        tag.putInt("LitKey", litKey);
        writeClientExtra(tag);
        return tag;
    }

    protected void writeClientExtra(CompoundTag tag) {
        tag.putBoolean("WheelUnlocked", canSpinBothWays());
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
        clientLatchLocked = tag.getBoolean("LatchLocked");
        clientWheelDegrees = tag.getFloat("WheelDegrees");
        clientLitKey = tag.getInt("LitKey");
        clientWheelUnlocked = tag.getBoolean("WheelUnlocked");
        readClientExtra(tag);
    }

    protected void readClientExtra(CompoundTag tag) {
        clientInputLength = tag.getInt("InputLength");
        clientScreenStatus = tag.getInt("ScreenStatus");
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

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
