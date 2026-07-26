package destiny.null_ouroboros.server.item;

import destiny.null_ouroboros.client.item.HeavyRevolverClientExtensions;
import destiny.null_ouroboros.common.revolver.RevolverAction;
import destiny.null_ouroboros.common.revolver.RevolverCartridge;
import destiny.null_ouroboros.common.revolver.RevolverState;
import destiny.null_ouroboros.common.player_anim.HeavyRevolverPlayerAnims;
import destiny.null_ouroboros.common.player_anim.PlayerAnimInstance;
import destiny.null_ouroboros.common.player_anim.PlayerAnimTracker;
import destiny.null_ouroboros.common.player_anim.PlayerAnimation;
import destiny.null_ouroboros.common.player_anim.PlayOptions;
import destiny.null_ouroboros.server.entity.BulletEntity;
import destiny.null_ouroboros.server.entity.CartridgeEntity;
import destiny.null_ouroboros.server.registry.CapabilityRegistry;
import destiny.null_ouroboros.server.registry.EntityRegistry;
import destiny.null_ouroboros.server.registry.SoundRegistry;
import destiny.null_ouroboros.server.util.ModUtil;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.fml.DistExecutor;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class HeavyRevolverItem extends Item implements GeoItem {
    private static final Set<UUID> DRAW_DONE = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Map<UUID, PendingAction> PENDING_ACTIONS = new ConcurrentHashMap<>();

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public HeavyRevolverItem(Properties properties) {
        super(properties);
    }

    public static void clearDrawState(UUID playerId) {
        DRAW_DONE.remove(playerId);
        PENDING_ACTIONS.remove(playerId);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide || !(entity instanceof Player player)) {
            return;
        }

        RevolverState.ensureVisualId(stack);
        boolean mainHandSelected = isSelected && player.getMainHandItem() == stack;
        PlayerAnimInstance active = PlayerAnimTracker.get(player.getUUID());

        if (mainHandSelected) {
            processPendingAction(player, stack);

            if (active != null && HeavyRevolverPlayerAnims.HOLD_ID.equals(active.animationId())) {
                long elapsed = level.getGameTime() - active.startGameTime();
                long holdTicks = Math.max(1L, Math.round(HeavyRevolverPlayerAnims.HOLD_LENGTH_SECONDS * 20.0F));
                if (elapsed >= holdTicks) {
                    DRAW_DONE.add(player.getUUID());
                }
                return;
            }

            if (active != null && HeavyRevolverPlayerAnims.isRevolverAnim(active.animationId())) {
                return;
            }

            if (DRAW_DONE.contains(player.getUUID())) {
                PlayerAnimation.playAtEnd(player, HeavyRevolverPlayerAnims.HOLD_ID, HeavyRevolverPlayerAnims.holdOptions());
            } else {
                level.playSound(null, player.blockPosition(), SoundRegistry.FIREARM_HOLSTER_OUT.get(), SoundSource.PLAYERS, 1f, 1f);
                PlayerAnimation.play(player, HeavyRevolverPlayerAnims.HOLD_ID, HeavyRevolverPlayerAnims.holdOptions());
            }
            return;
        }

        if (player.getMainHandItem().getItem() instanceof HeavyRevolverItem) {
            return;
        }

        if (active != null && HeavyRevolverPlayerAnims.isRevolverAnim(active.animationId())) {
            PlayerAnimation.cancelAll(player);
            DRAW_DONE.remove(player.getUUID());
        }
        PENDING_ACTIONS.remove(player.getUUID());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        return hand == InteractionHand.MAIN_HAND ? InteractionResultHolder.fail(stack) : InteractionResultHolder.pass(stack);
    }

    public static void handleAction(ServerPlayer player, RevolverAction action) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof HeavyRevolverItem) || action == null || !isActionIdle(player)) {
            return;
        }

        boolean reloading = RevolverState.isReloading(stack);
        switch (action) {
            case TOGGLE_CYLINDER -> queueAction(player, stack, action, 5,
                    reloading ? HeavyRevolverPlayerAnims.CYLINDER_IN_ID : HeavyRevolverPlayerAnims.CYLINDER_OUT_ID);
            case EJECT_SELECTED -> {
                if (reloading && RevolverState.getChamber(stack, RevolverState.getSelected(stack)) != RevolverCartridge.EMPTY) {
                    queueAction(player, stack, action, 5, HeavyRevolverPlayerAnims.CYLINDER_TAKE_ID);
                }
            }
            case EJECT_ALL -> {
                if (reloading && !RevolverState.allEmpty(stack)) {
                    queueAction(player, stack, action, 5, HeavyRevolverPlayerAnims.CYLINDER_EJECT_ID);
                }
            }
            case INSERT_SELECTED -> {
                if (!reloading) {
                    return;
                }
                if (player.getOffhandItem().getItem() instanceof SpeedloaderItem) {
                    if (RevolverState.allEmpty(stack) && hasLoadedSpeedloader(player)) {
                        queueAction(player, stack, RevolverAction.SPEEDLOAD, 5, HeavyRevolverPlayerAnims.CYLINDER_SPEEDLOADER_ID);
                    }
                    return;
                }
                if (RevolverState.getChamber(stack, RevolverState.getSelected(stack)) == RevolverCartridge.EMPTY
                        && findAmmo(player) != null) {
                    queueAction(player, stack, action, 15, HeavyRevolverPlayerAnims.CYLINDER_PUT_ID);
                }
            }
            case ROTATE_FORWARD -> {
                if (reloading) {
                    queueAction(player, stack, action, 3, HeavyRevolverPlayerAnims.CYLINDER_ROTATE_ID);
                }
            }
            case ROTATE_BACKWARD -> {
                if (reloading) {
                    queueAction(player, stack, action, 3, HeavyRevolverPlayerAnims.CYLINDER_ROTATE_ID,
                            HeavyRevolverPlayerAnims.actionOptions(true));
                }
            }
            case FIRE -> {
                if (!reloading && RevolverState.isCocked(stack)) {
                    fire(player, stack);
                }
            }
            case TOGGLE_COCK -> {
                if (!reloading) {
                    queueAction(player, stack, action, 3,
                            RevolverState.isCocked(stack) ? HeavyRevolverPlayerAnims.DECOCK_ID : HeavyRevolverPlayerAnims.COCK_ID);
                }
            }
            case SPEEDLOAD -> {
                if (reloading && RevolverState.allEmpty(stack) && hasLoadedSpeedloader(player)) {
                    queueAction(player, stack, action, 5, HeavyRevolverPlayerAnims.CYLINDER_SPEEDLOADER_ID);
                }
            }
        }
    }

    private static boolean isActionIdle(ServerPlayer player) {
        if (PENDING_ACTIONS.containsKey(player.getUUID()) || !DRAW_DONE.contains(player.getUUID())) {
            return false;
        }
        PlayerAnimInstance active = PlayerAnimTracker.get(player.getUUID());
        return active == null || HeavyRevolverPlayerAnims.HOLD_ID.equals(active.animationId());
    }

    private static boolean hasLoadedSpeedloader(Player player) {
        return player.getOffhandItem().getItem() instanceof SpeedloaderItem
                && !SpeedloaderItem.getRounds(player.getOffhandItem()).isEmpty();
    }

    private static void queueAction(ServerPlayer player, ItemStack stack, RevolverAction action, int commitDelay, ResourceLocation animationId) {
        queueAction(player, stack, action, commitDelay, animationId, HeavyRevolverPlayerAnims.actionOptions());
    }

    private static void queueAction(ServerPlayer player, ItemStack stack, RevolverAction action, int commitDelay,
                                    ResourceLocation animationId, PlayOptions options) {
        PENDING_ACTIONS.put(player.getUUID(), new PendingAction(stack, action, player.level().getGameTime() + commitDelay));
        DRAW_DONE.add(player.getUUID());
        PlayerAnimation.play(player, animationId, options);
    }

    private static void processPendingAction(Player player, ItemStack stack) {
        PendingAction pending = PENDING_ACTIONS.get(player.getUUID());
        if (pending == null || pending.stack() != stack || player.level().getGameTime() < pending.commitGameTime()) {
            return;
        }
        PENDING_ACTIONS.remove(player.getUUID());
        commitAction(player, stack, pending.action());
    }

    private static void commitAction(Player player, ItemStack stack, RevolverAction action) {
        switch (action) {
            case TOGGLE_CYLINDER -> {
                if (RevolverState.isReloading(stack)) {
                    RevolverState.setReloading(stack, false);
                    RevolverState.rotate(stack, -1);
                    playSound(player, SoundRegistry.HEAVY_REVOLVER_CYLINDER_CLOSE.get(), 1.0F);
                } else {
                    RevolverState.setReloading(stack, true);
                    RevolverState.rotate(stack, 1);
                    playSound(player, SoundRegistry.HEAVY_REVOLVER_CYLINDER_OPEN.get(), 1.0F);
                }
            }
            case EJECT_SELECTED -> {
                takeChamber(player, stack, RevolverState.getSelected(stack));
                playSound(player, SoundRegistry.HEAVY_REVOLVER_CYLINDER_TAKE.get(), 1.0F);
            }
            case EJECT_ALL -> {
                for (int chamber = 0; chamber < RevolverState.CHAMBER_COUNT; chamber++) {
                    ejectChamber(player, stack, chamber);
                }
                playSound(player, SoundRegistry.HEAVY_REVOLVER_CYLINDER_EJECT.get(), 1.0F);
            }
            case INSERT_SELECTED -> {
                ItemStack ammo = findAmmo(player);
                if (ammo != null) {
                    RevolverState.setChamber(stack, RevolverState.getSelected(stack), RevolverCartridge.fromItem(ammo.getItem()));
                    if (!player.getAbilities().instabuild) {
                        ammo.shrink(1);
                    }
                    playSound(player, SoundRegistry.HEAVY_REVOLVER_CYLINDER_INSERT.get(), 1.0F);
                }
            }
            case ROTATE_FORWARD -> {
                RevolverState.rotate(stack, 1);
                playSound(player, SoundRegistry.HEAVY_REVOLVER_CYLINDER_ROTATE.get(), 1.0F);
            }
            case ROTATE_BACKWARD -> {
                RevolverState.rotate(stack, -1);
                playSound(player, SoundRegistry.HEAVY_REVOLVER_CYLINDER_ROTATE.get(), 1.0F);
            }
            case TOGGLE_COCK -> {
                if (RevolverState.isCocked(stack)) {
                    RevolverState.setCocked(stack, false);
                    playSound(player, SoundRegistry.HEAVY_REVOLVER_DECOCK.get(), 1.0F);
                } else {
                    RevolverState.setCocked(stack, true);
                    RevolverState.rotate(stack, 1);
                    playSound(player, SoundRegistry.HEAVY_REVOLVER_COCK.get(), 1.0F);
                }
            }
            case SPEEDLOAD -> {
                if (hasLoadedSpeedloader(player)) {
                    SpeedloaderItem speedloader = (SpeedloaderItem) player.getOffhandItem().getItem();
                    speedloader.loadInto(stack, player.getOffhandItem());
                    playSound(player, SoundRegistry.HEAVY_REVOLVER_CYLINDER_EJECT.get(), 1.0F);
                }
            }
            default -> {
            }
        }
        syncStack(player);
    }

    private static void fire(ServerPlayer player, ItemStack stack) {
        int selected = RevolverState.getSelected(stack);
        RevolverCartridge cartridge = RevolverState.getChamber(stack, selected);
        RevolverState.setCocked(stack, false);
        if (cartridge.isLive()) {
            RevolverState.setChamber(stack, selected, RevolverCartridge.CASING);
            spawnBullet(player, cartridge);
            playSound(player, SoundRegistry.HEAVY_REVOLVER_SHOOT.get(), 16.0F);
            stack.hurtAndBreak(1, player, broken -> broken.broadcastBreakEvent(InteractionHand.MAIN_HAND));
            DRAW_DONE.add(player.getUUID());
            PlayerAnimation.play(player, HeavyRevolverPlayerAnims.SHOOT_ID, HeavyRevolverPlayerAnims.shootOptions());
        }
        playSound(player, SoundRegistry.HEAVY_REVOLVER_DECOCK.get(), 1.0F);
        syncStack(player);
    }

    private static void spawnBullet(ServerPlayer player, RevolverCartridge cartridge) {
        Vec3 look = player.getLookAngle();
        BulletEntity bullet = EntityRegistry.BULLET.get().create(player.level());
        if (bullet == null) {
            return;
        }
        bullet.setOwner(player);
        bullet.setCartridge(cartridge);
        bullet.setPos(player.getEyePosition().add(look.scale(0.5)));
        bullet.shoot(look.x, look.y, look.z, 4.0F);
        bullet.setDeltaMovement(bullet.getDeltaMovement().add(player.getDeltaMovement()));
        player.level().addFreshEntity(bullet);
    }

    private static void ejectChamber(Player player, ItemStack stack, int chamber) {
        RevolverCartridge cartridge = RevolverState.getChamber(stack, chamber);
        if (cartridge == RevolverCartridge.EMPTY) {
            return;
        }
        RevolverState.setChamber(stack, chamber, RevolverCartridge.EMPTY);
        CartridgeEntity cartridgeEntity = EntityRegistry.CARTRIDGE.get().create(player.level());
        if (cartridgeEntity != null) {
            cartridgeEntity.initialize(player, chamber, cartridge);
            player.level().addFreshEntity(cartridgeEntity);
        }
    }

    private static void takeChamber(Player player, ItemStack stack, int chamber) {
        RevolverCartridge cartridge = RevolverState.getChamber(stack, chamber);
        if (cartridge == RevolverCartridge.EMPTY) {
            return;
        }
        RevolverState.setChamber(stack, chamber, RevolverCartridge.EMPTY);
        ItemStack cartridgeStack = new ItemStack(cartridge.item());
        if (!player.addItem(cartridgeStack)) {
            player.spawnAtLocation(cartridgeStack);
        }
    }

    private static ItemStack findAmmo(Player player) {
        ItemStack offhand = player.getOffhandItem();
        if (RevolverCartridge.fromItem(offhand.getItem()).isLive()) {
            return offhand;
        }
        for (ItemStack candidate : player.getInventory().items) {
            if (RevolverCartridge.fromItem(candidate.getItem()).isLive()) {
                return candidate;
            }
        }
        return null;
    }

    private static void playSound(Player player, net.minecraft.sounds.SoundEvent sound, float volume) {
        player.level().playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, volume, 1.0F);
    }

    private static void syncStack(Player player) {
        player.getInventory().setChanged();
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.inventoryMenu.broadcastChanges();
            serverPlayer.containerMenu.broadcastChanges();
        }
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> HeavyRevolverClientExtensions.register(consumer));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    private record PendingAction(ItemStack stack, RevolverAction action, long commitGameTime) {
    }
}
