package destiny.null_ouroboros.server.capability;

import destiny.null_ouroboros.common.dimension.VergeOfRealityDimension;
import destiny.null_ouroboros.server.item.RespiratorGear;
import destiny.null_ouroboros.server.registry.CapabilityRegistry;
import destiny.null_ouroboros.server.registry.DamageTypeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.UUID;

public class RespiratoryCapability implements INBTSerializable<CompoundTag> {
    public static final UUID MOVEMENT_MODIFIER_ID = UUID.fromString("a3c8e1f0-5b2d-4e9a-9c1f-7d6b4a2e8f10");

    private static final int GRACE_MIN = 20 * 60 * 10;
    private static final int GRACE_MAX = 20 * 60 * 15;
    private static final int INTERMEDIATE_MIN = 20 * 60 * 3;
    private static final int INTERMEDIATE_MAX = 20 * 60 * 6;
    private static final int TERMINAL_MIN = 20 * 60 * 1;
    private static final int TERMINAL_MAX = 20 * 60 * 2;

    private static final float[] MOVE_MULTIPLIERS = {-0.15F, -0.30F, -0.45F, -0.60F};
    private static final float[] DIG_MULTIPLIERS = {0.85F, 0.70F, 0.55F, 0.40F};
    private static final float[] REGEN_MULTIPLIERS = {0.85F, 0.70F, 0.55F, 0.40F};

    private int graceMax;
    private int grace;
    private int intermediateMax;
    private int intermediate;
    private int terminalMax;
    private int terminal;
    private boolean initialized;
    private int damageCooldown;
    private long manifoldingExposedGameTime = -1L;
    private int appliedMoveTier = -1;
    private boolean wasManifoldingExposed;

    public void serverTick(ServerPlayer player) {
        if (player.isCreative() || player.isSpectator()) {
            clearMovementModifier(player);
            appliedMoveTier = -1;
            wasManifoldingExposed = false;
            return;
        }

        if (!initialized) {
            initialize(player);
        }

        boolean onVerge = VergeOfRealityDimension.isVergeOfReality(player.level());
        boolean protectedGear = RespiratorGear.isProtected(player);
        boolean manifoldingExposed = isManifoldingExposed(player.level().getGameTime());

        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (onVerge && RespiratorGear.hasAnyDrainableFilter(head) && player.tickCount % 20 == 0) {
            RespiratorGear.hurtRandomFilter(player, head, 1);
        }

        if (manifoldingExposed && !protectedGear) {
            if (!wasManifoldingExposed) {
                onManifoldingExposure();
            }
            drain(5);
        } else if (!onVerge || protectedGear) {
            recover(1);
        } else {
            boolean inCleanAir = player.level().getCapability(CapabilityRegistry.ASH_ATMOSPHERE_CAPABILITY)
                    .map(ash -> !ash.isAshyAir(player.level(), BlockPos.containing(player.getEyePosition())))
                    .orElse(false);
            if (inCleanAir) {
                recover(1);
            } else {
                drain(1);
            }
        }
        wasManifoldingExposed = manifoldingExposed && !protectedGear;

        updateImpairment(player);
        if (!manifoldingExposed) {
            applyTerminalDamage(player);
        }
    }

    public void markManifoldingExposed(long gameTime) {
        this.manifoldingExposedGameTime = gameTime;
    }

    public boolean isManifoldingExposed(long gameTime) {
        return manifoldingExposedGameTime >= 0L && gameTime - manifoldingExposedGameTime <= 1L;
    }

    public void onManifoldingExposure() {
        grace = 0;
        intermediate = intermediateMax;
    }

    public void ensureInitialized(Player player) {
        if (!initialized) {
            initialize(player);
        }
    }

    public int getStage() {
        if (!initialized) {
            return 0;
        }
        if (grace > 0) {
            return 0;
        }
        if (intermediate > 0) {
            return 1;
        }
        return 2;
    }

    public void setStage(int stage) {
        if (!initialized) {
            return;
        }
        switch (stage) {
            case 0 -> {
                grace = graceMax;
                intermediate = intermediateMax;
                terminal = terminalMax;
            }
            case 1 -> {
                grace = 0;
                intermediate = intermediateMax;
                terminal = terminalMax;
            }
            case 2 -> {
                grace = 0;
                intermediate = 0;
                terminal = terminalMax;
            }
            default -> {
                return;
            }
        }
        damageCooldown = 0;
        appliedMoveTier = -1;
    }

    public boolean isInIntermediate() {
        return initialized && grace <= 0 && intermediate > 0;
    }

    public boolean isInTerminal() {
        return initialized && grace <= 0 && intermediate <= 0;
    }

    public int getImpairmentTier() {
        if (!isInIntermediate() && !isInTerminal()) {
            return -1;
        }
        if (isInTerminal() || intermediateMax <= 0) {
            return 3;
        }
        float p = 1.0F - (float) intermediate / (float) intermediateMax;
        if (p < 0.25F) {
            return 0;
        }
        if (p < 0.5F) {
            return 1;
        }
        if (p < 0.75F) {
            return 2;
        }
        return 3;
    }

    public float getDigMultiplier() {
        int tier = getImpairmentTier();
        return tier < 0 ? 1.0F : DIG_MULTIPLIERS[tier];
    }

    public float getRegenMultiplier() {
        int tier = getImpairmentTier();
        return tier < 0 ? 1.0F : REGEN_MULTIPLIERS[tier];
    }

    private void initialize(Player player) {
        graceMax = GRACE_MIN + player.getRandom().nextInt(GRACE_MAX - GRACE_MIN + 1);
        intermediateMax = INTERMEDIATE_MIN + player.getRandom().nextInt(INTERMEDIATE_MAX - INTERMEDIATE_MIN + 1);
        terminalMax = TERMINAL_MIN + player.getRandom().nextInt(TERMINAL_MAX - TERMINAL_MIN + 1);
        grace = graceMax;
        intermediate = intermediateMax;
        terminal = terminalMax;
        damageCooldown = 0;
        initialized = true;
    }

    private void drain(int amount) {
        int remaining = amount;
        if (grace > 0) {
            int take = Math.min(grace, remaining);
            grace -= take;
            remaining -= take;
        }
        if (remaining > 0 && intermediate > 0) {
            int take = Math.min(intermediate, remaining);
            intermediate -= take;
            remaining -= take;
        }
        if (remaining > 0 && terminal > 0) {
            terminal = Math.max(0, terminal - remaining);
        }
    }

    private void recover(int amount) {
        int remaining = amount;
        if (terminal < terminalMax) {
            int add = Math.min(terminalMax - terminal, remaining);
            terminal += add;
            remaining -= add;
        }
        if (remaining > 0 && intermediate < intermediateMax) {
            int add = Math.min(intermediateMax - intermediate, remaining);
            intermediate += add;
            remaining -= add;
        }
        if (remaining > 0 && grace < graceMax) {
            grace = Math.min(graceMax, grace + remaining);
        }
    }

    private void updateImpairment(ServerPlayer player) {
        boolean impair = isInIntermediate() || isInTerminal();
        if (!impair) {
            clearMovementModifier(player);
            appliedMoveTier = -1;
            return;
        }

        int tier = getImpairmentTier();
        if (tier == appliedMoveTier) {
            return;
        }
        clearMovementModifier(player);
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.addTransientModifier(new AttributeModifier(
                    MOVEMENT_MODIFIER_ID,
                    "null_ouroboros_ash_impairment",
                    MOVE_MULTIPLIERS[tier],
                    AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
        appliedMoveTier = tier;
    }

    private void clearMovementModifier(Player player) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(MOVEMENT_MODIFIER_ID);
        }
    }

    private void applyTerminalDamage(ServerPlayer player) {
        if (!isInTerminal()) {
            damageCooldown = 0;
            return;
        }

        if (terminal <= 0) {
            player.hurt(DamageTypeRegistry.getSimpleDamageSource(player.level(), DamageTypeRegistry.ASH_ASPHYXIATION), Float.MAX_VALUE);
            return;
        }

        float p = 1.0F - (float) terminal / (float) Math.max(1, terminalMax);
        int interval = Mth.floor(Mth.lerp(p, 40.0F, 5.0F));
        float amount = Mth.lerp(p, 1.0F, 4.0F);

        if (damageCooldown > 0) {
            damageCooldown--;
            return;
        }

        player.hurt(DamageTypeRegistry.getSimpleDamageSource(player.level(), DamageTypeRegistry.ASH_ASPHYXIATION), amount);
        damageCooldown = Math.max(1, interval);
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("initialized", initialized);
        tag.putInt("graceMax", graceMax);
        tag.putInt("grace", grace);
        tag.putInt("intermediateMax", intermediateMax);
        tag.putInt("intermediate", intermediate);
        tag.putInt("terminalMax", terminalMax);
        tag.putInt("terminal", terminal);
        tag.putInt("damageCooldown", damageCooldown);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        initialized = tag.getBoolean("initialized");
        graceMax = tag.getInt("graceMax");
        grace = tag.getInt("grace");
        intermediateMax = tag.getInt("intermediateMax");
        intermediate = tag.getInt("intermediate");
        terminalMax = tag.getInt("terminalMax");
        terminal = tag.getInt("terminal");
        damageCooldown = tag.getInt("damageCooldown");
        appliedMoveTier = -1;
        manifoldingExposedGameTime = -1L;
        wasManifoldingExposed = false;
    }
}
