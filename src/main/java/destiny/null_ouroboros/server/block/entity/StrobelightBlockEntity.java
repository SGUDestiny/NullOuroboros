package destiny.null_ouroboros.server.block.entity;

import destiny.null_ouroboros.client.sound.StrobelightLoopingSound;
import destiny.null_ouroboros.server.block.StrobelightBlock;
import destiny.null_ouroboros.server.registry.BlockEntityRegistry;
import destiny.null_ouroboros.server.registry.SoundRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class StrobelightBlockEntity extends BlockEntity {
    public static final String ROTATION_ANGLE = "RotationAngle";
    public static final String ROTATION_SPEED = "RotationSpeed";

    private static final float MAX_RPM = 40f;
    private static final float MAX_SPEED = MAX_RPM * 360f / 60f / 20f;
    private static final float ACCELERATION = MAX_SPEED / (2f * 20f);

    private float rotationAngle = 0f;
    private float rotationSpeed = 0f;

    private StrobelightLoopingSound alarmSound;

    public StrobelightBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.STROBELIGHT_BLOCK_ENTITY.get(), pos, state);
    }

    public float getRotationAngle() {
        return rotationAngle;
    }

    public float getRotationSpeed() {
        return rotationSpeed;
    }

    public static float getMaxSpeed() {
        return MAX_SPEED;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, StrobelightBlockEntity strobelightBlockEntity) {
        boolean lit = state.getValue(StrobelightBlock.LIT);
        float targetSpeed = lit ? MAX_SPEED : 0f;

        if (strobelightBlockEntity.rotationSpeed < targetSpeed) {
            strobelightBlockEntity.rotationSpeed = Math.min(strobelightBlockEntity.rotationSpeed + ACCELERATION, targetSpeed);
        } else if (strobelightBlockEntity.rotationSpeed > targetSpeed) {
            strobelightBlockEntity.rotationSpeed = Math.max(strobelightBlockEntity.rotationSpeed - ACCELERATION, targetSpeed);
        }

        strobelightBlockEntity.rotationAngle = (strobelightBlockEntity.rotationAngle + strobelightBlockEntity.rotationSpeed) % 360f;

        if (level.isClientSide) {
            strobelightBlockEntity.clientTickSounds();
        }
    }

    private void clientTickSounds() {
        boolean lit = getBlockState().getValue(StrobelightBlock.LIT);
        float speed = getRotationSpeed();
        boolean active = lit || speed > 0;

        if (active) {
            if (alarmSound == null || alarmSound.isStopped()) {
                alarmSound = new StrobelightLoopingSound(SoundRegistry.STROBELIGHT_ALARM.get(), this);
                Minecraft.getInstance().getSoundManager().play(alarmSound);
            }
        } else {
            if (alarmSound != null) {
                alarmSound.isStopped();
                alarmSound = null;
            }
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && level.isClientSide && alarmSound != null) {
            alarmSound.isStopped();
            alarmSound = null;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        tag.putFloat(ROTATION_ANGLE, rotationAngle);
        tag.putFloat(ROTATION_SPEED, rotationSpeed);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        rotationAngle = tag.getFloat(ROTATION_ANGLE);
        rotationSpeed = tag.getFloat(ROTATION_SPEED);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();

        tag.putFloat(ROTATION_ANGLE, rotationAngle);
        tag.putFloat(ROTATION_SPEED, rotationSpeed);

        return tag;
    }
}