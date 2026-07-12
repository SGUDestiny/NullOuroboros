package destiny.null_ouroboros.client.render;

import destiny.null_ouroboros.common.dimension.VergeOfRealityDimension;
import destiny.null_ouroboros.server.block.ElectromagneticAssemblyBlock;
import destiny.null_ouroboros.server.block.entity.ElectromagneticAssemblyBlockEntity;
import destiny.null_ouroboros.server.capability.ClientManifoldingHolder;
import destiny.null_ouroboros.server.capability.ManifoldingPhase;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.util.Mth;

import static destiny.null_ouroboros.server.block.entity.ElectromagneticAssemblyBlockEntity.*;

public final class ElectromagneticAssemblyClientTicker {
    private ElectromagneticAssemblyClientTicker() {}

    public static void tick(Level level, BlockState state, ElectromagneticAssemblyBlockEntity be) {
        if (!(level instanceof ClientLevel clientLevel) || !VergeOfRealityDimension.isVergeOfReality(clientLevel)) {
            if (be.spinnerSpeed > 0f) {
                be.spinnerSpeed = Math.max(0f, be.spinnerSpeed - SPINNER_DECELERATION);
                be.spinnerAngle = (be.spinnerAngle + be.spinnerSpeed) % 360f;
            }
            be.vaneReachedTarget = false;
            return;
        }

        float windStrength = ClientManifoldingHolder.getWindStrength();
        ManifoldingPhase phase = ClientManifoldingHolder.getPhase();
        float windAngle = ClientManifoldingHolder.getWindAngle();
        Direction facing = state.getValue(ElectromagneticAssemblyBlock.HORIZONTAL_FACING);

        float targetSpinnerSpeed = windStrength * MAX_SPINNER_SPEED;
        if (be.spinnerSpeed < targetSpinnerSpeed) {
            be.spinnerSpeed = Math.min(be.spinnerSpeed + SPINNER_ACCELERATION, targetSpinnerSpeed);
        } else if (be.spinnerSpeed > targetSpinnerSpeed) {
            be.spinnerSpeed = Math.max(be.spinnerSpeed - SPINNER_DECELERATION, targetSpinnerSpeed);
        }
        be.spinnerAngle = (be.spinnerAngle + be.spinnerSpeed) % 360f;

        float targetLocal = computeVaneTargetAngle(windAngle, facing);
        float targetDiff = angleDiff(be.vaneBaseAngle, targetLocal);

        if (phase != ManifoldingPhase.CLEAR && windStrength > 0f) {
            if (Math.abs(targetDiff) > WIND_ANGLE_RESET_THRESHOLD) {
                be.vaneReachedTarget = false;
            }
            if (!be.vaneReachedTarget) {
                float approachSpeed = phase == ManifoldingPhase.PRE_EVENT ? VANE_APPROACH_SPEED * 0.5f : VANE_APPROACH_SPEED;
                if (Math.abs(targetDiff) <= APPROACH_THRESHOLD) {
                    be.vaneBaseAngle = targetLocal;
                    be.vaneReachedTarget = true;
                } else {
                    float step = Math.min(approachSpeed, Math.abs(targetDiff));
                    be.vaneBaseAngle = normalizeDegrees(be.vaneBaseAngle + Math.signum(targetDiff) * step);
                }
            } else {
                be.vaneBaseAngle = targetLocal;
            }
        }

        if (be.vaneReachedTarget && windStrength > 0f) {
            float freqHz = SWAY_BASE_HZ + windStrength * SWAY_FREQ_SCALE_HZ;
            be.vaneOscPhase += freqHz * 2f * (float) Math.PI / 20f;
        } else if (windStrength <= 0f) {
            be.vaneReachedTarget = false;
        }
    }
    public static float getVaneDisplayAngle(ElectromagneticAssemblyBlockEntity be) {
        float windStrength = ClientManifoldingHolder.getWindStrength();
        if (be.vaneReachedTarget && windStrength > 0f) {
            float leeway = Mth.clamp(SWAY_LEEWAY_MAX - windStrength * (SWAY_LEEWAY_MAX - SWAY_LEEWAY_MIN),
                    SWAY_LEEWAY_MIN, SWAY_LEEWAY_MAX);
            if (ClientManifoldingHolder.getPhase() == ManifoldingPhase.POST_EVENT) {
                leeway *= windStrength;
            }
            return be.vaneBaseAngle + leeway * (float) Math.sin(be.vaneOscPhase);
        }
        return be.vaneBaseAngle;
    }
}