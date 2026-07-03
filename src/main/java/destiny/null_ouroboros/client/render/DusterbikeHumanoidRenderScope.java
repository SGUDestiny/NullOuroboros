package destiny.null_ouroboros.client.render;

public final class DusterbikeHumanoidRenderScope {
    private static int entityRenderDepth;

    private DusterbikeHumanoidRenderScope() {}

    public static void beginEntityRenderSetup() {
        entityRenderDepth++;
    }

    public static void endEntityRenderSetup() {
        if (entityRenderDepth > 0) {
            entityRenderDepth--;
        }
    }

    public static boolean isEntityRenderSetupActive() {
        return entityRenderDepth > 0;
    }
}
