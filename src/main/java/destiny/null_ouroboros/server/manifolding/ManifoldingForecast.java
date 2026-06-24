package destiny.null_ouroboros.server.manifolding;

import destiny.null_ouroboros.server.capability.ManifoldingCapability;
import destiny.null_ouroboros.server.capability.ManifoldingPhase;

public record ManifoldingForecast(
        TimeValue eta,
        TimeValue etd,
        AngleValue estimatedAngle,
        boolean insufficient
) {
    public enum TimeKind {
        NULL,
        NOW,
        UNDER_MIN,
        MINUTES
    }

    public record TimeValue(TimeKind kind, int minutes) {
        public static TimeValue nullValue() {
            return new TimeValue(TimeKind.NULL, 0);
        }

        public static TimeValue now() {
            return new TimeValue(TimeKind.NOW, 0);
        }

        public static TimeValue fromTicks(int ticks) {
            if (ticks <= 0) {
                return new TimeValue(TimeKind.UNDER_MIN, 0);
            }
            double minutesExact = ticks / 1200.0;
            if (minutesExact < 1.0) {
                return new TimeValue(TimeKind.UNDER_MIN, 0);
            }
            return new TimeValue(TimeKind.MINUTES, (int) Math.ceil(minutesExact));
        }
    }

    public record AngleValue(AngleKind kind, int degrees, String directionKey) {
        public enum AngleKind {
            NULL,
            DEGREES
        }

        public static AngleValue nullValue() {
            return new AngleValue(AngleKind.NULL, 0, "");
        }

        public static AngleValue fromYaw(float yaw, String directionKey) {
            int normalized = Math.floorMod(Math.round(yaw), 360);
            return new AngleValue(AngleKind.DEGREES, normalized, directionKey);
        }
    }

    public static ManifoldingForecast createInsufficient() {
        return new ManifoldingForecast(
                TimeValue.nullValue(),
                TimeValue.nullValue(),
                AngleValue.nullValue(),
                true
        );
    }

    public static ManifoldingForecast from(ManifoldingCapability cap, long gameTime) {
        ManifoldingPhase phase = cap.getPhase();
        TimeValue eta;
        TimeValue etd;

        switch (phase) {
            case CLEAR -> {
                eta = TimeValue.fromTicks(cap.getTimeUntilNextEvent());
                etd = TimeValue.fromTicks(cap.getPreEventDuration() + cap.getActiveDuration() + cap.getPostEventDuration());
            }
            case PRE_EVENT -> {
                eta = TimeValue.now();
                long elapsed = gameTime - cap.getPhaseStartTime();
                long remaining = (cap.getPreEventDuration() - elapsed)
                        + cap.getActiveDuration()
                        + cap.getPostEventDuration();
                etd = TimeValue.fromTicks((int) Math.max(0, remaining));
            }
            case ACTIVE -> {
                eta = TimeValue.now();
                long elapsed = gameTime - cap.getPhaseStartTime();
                long remaining = (cap.getActiveDuration() - elapsed) + cap.getPostEventDuration();
                etd = TimeValue.fromTicks((int) Math.max(0, remaining));
            }
            case POST_EVENT -> {
                eta = TimeValue.now();
                long elapsed = gameTime - cap.getPhaseStartTime();
                long remaining = cap.getPostEventDuration() - elapsed;
                etd = TimeValue.fromTicks((int) Math.max(0, remaining));
            }
            default -> {
                eta = TimeValue.nullValue();
                etd = TimeValue.nullValue();
            }
        }

        String directionKey = destiny.null_ouroboros.server.util.WindCompass.directionKey(cap.getWindDirectionYaw());
        AngleValue angle = AngleValue.fromYaw(cap.getWindDirectionYaw(), directionKey);

        return new ManifoldingForecast(eta, etd, angle, false);
    }
}
