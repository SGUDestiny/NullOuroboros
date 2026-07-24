package destiny.null_ouroboros.client.render.player_anim;

import destiny.null_ouroboros.common.player_anim.PlayOptions;
import net.minecraft.client.model.PlayerModel;

import java.util.ArrayDeque;
import java.util.Deque;

public final class PlayerAnimFirstPersonRenderScope {
    private static final Deque<PlayOptions> OPTIONS = new ArrayDeque<>();
    private static final Deque<Visibility> VISIBILITY = new ArrayDeque<>();

    private PlayerAnimFirstPersonRenderScope() {}

    public static void begin(PlayOptions options) {
        OPTIONS.push(options);
    }

    public static void end() {
        if (!OPTIONS.isEmpty()) {
            OPTIONS.pop();
        }
    }

    public static boolean isActive() {
        return !OPTIONS.isEmpty();
    }

    public static void capture(PlayerModel<?> model) {
        if (!isActive()) {
            return;
        }
        VISIBILITY.push(new Visibility(
                model.head.visible,
                model.hat.visible,
                model.body.visible,
                model.jacket.visible,
                model.leftArm.visible,
                model.rightArm.visible,
                model.leftSleeve.visible,
                model.rightSleeve.visible,
                model.leftLeg.visible,
                model.rightLeg.visible,
                model.leftPants.visible,
                model.rightPants.visible
        ));
    }

    public static void apply(PlayerModel<?> model) {
        if (!isActive()) {
            return;
        }
        PlayOptions options = OPTIONS.peek();
        boolean body = options.renderFirstPersonBody();
        boolean head = options.renderFirstPersonHead();

        model.head.visible = head;
        model.hat.visible = head;
        model.body.visible = body;
        model.jacket.visible = body;
        model.leftLeg.visible = body;
        model.rightLeg.visible = body;
        model.leftPants.visible = body;
        model.rightPants.visible = body;
        model.leftArm.visible = true;
        model.rightArm.visible = true;
        model.leftSleeve.visible = true;
        model.rightSleeve.visible = true;
    }

    public static void restore(PlayerModel<?> model) {
        if (VISIBILITY.isEmpty()) {
            return;
        }
        Visibility visibility = VISIBILITY.pop();
        model.head.visible = visibility.head;
        model.hat.visible = visibility.hat;
        model.body.visible = visibility.body;
        model.jacket.visible = visibility.jacket;
        model.leftArm.visible = visibility.leftArm;
        model.rightArm.visible = visibility.rightArm;
        model.leftSleeve.visible = visibility.leftSleeve;
        model.rightSleeve.visible = visibility.rightSleeve;
        model.leftLeg.visible = visibility.leftLeg;
        model.rightLeg.visible = visibility.rightLeg;
        model.leftPants.visible = visibility.leftPants;
        model.rightPants.visible = visibility.rightPants;
    }

    private static final class Visibility {
        private final boolean head;
        private final boolean hat;
        private final boolean body;
        private final boolean jacket;
        private final boolean leftArm;
        private final boolean rightArm;
        private final boolean leftSleeve;
        private final boolean rightSleeve;
        private final boolean leftLeg;
        private final boolean rightLeg;
        private final boolean leftPants;
        private final boolean rightPants;

        private Visibility(
                boolean head,
                boolean hat,
                boolean body,
                boolean jacket,
                boolean leftArm,
                boolean rightArm,
                boolean leftSleeve,
                boolean rightSleeve,
                boolean leftLeg,
                boolean rightLeg,
                boolean leftPants,
                boolean rightPants) {
            this.head = head;
            this.hat = hat;
            this.body = body;
            this.jacket = jacket;
            this.leftArm = leftArm;
            this.rightArm = rightArm;
            this.leftSleeve = leftSleeve;
            this.rightSleeve = rightSleeve;
            this.leftLeg = leftLeg;
            this.rightLeg = rightLeg;
            this.leftPants = leftPants;
            this.rightPants = rightPants;
        }
    }
}
