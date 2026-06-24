package destiny.null_ouroboros.server.capability;

public class ClientManifoldingHolder {
    private static ManifoldingPhase phase = ManifoldingPhase.CLEAR;
    private static float windStrength = 0;
    private static float windAngle = 0;
    private static float thunderPulse = 0;
    private static int riftTicks = 0;
    private static float lightDim = 0;
    private static long phaseStartTime = 0;
    private static int preEventDuration = 0;
    private static int activeDuration = 0;
    private static int postEventDuration = 0;
    private static boolean exposed = false;
    private static float exposureLevel = 1f;

    public static void set(ManifoldingPhase p, float ws, float wa, float tp, int rt, float ld, long pst, int pre, int act, int post, boolean e) {
        phase = p;
        windStrength = ws;
        windAngle = wa;
        thunderPulse = tp;
        riftTicks = rt;
        lightDim = ld;
        phaseStartTime = pst;
        preEventDuration = pre;
        activeDuration = act;
        postEventDuration = post;
        exposed = e;
    }

    public static void updateExposure() {
        float target = exposed ? 1f : 0f;
        exposureLevel += (target - exposureLevel) * 0.2f;
    }

    public static float getThunderPulse() {
        return thunderPulse;
    }
    public static float getWindStrength() {
        return windStrength;
    }
    public static float getWindAngle() {
        return windAngle;
    }
    public static ManifoldingPhase getPhase() {
        return phase;
    }
    public static int getRiftTicks() {
        return riftTicks;
    }
    public static float getLightDim() {
        return lightDim;
    }
    public static long getPhaseStartTime() {
        return phaseStartTime;
    }
    public static int getPreDuration() {
        return preEventDuration;
    }
    public static int getActiveDuration() {
        return activeDuration;
    }
    public static int getPostDuration() {
        return postEventDuration;
    }
    public static float getExposureLevel() {
        return exposureLevel;
    }

    public static boolean isExposed() {
        return exposed;
    }
}
