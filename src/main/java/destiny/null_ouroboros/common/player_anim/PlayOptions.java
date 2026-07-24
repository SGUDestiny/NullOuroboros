package destiny.null_ouroboros.common.player_anim;

import net.minecraft.network.FriendlyByteBuf;

public final class PlayOptions {
    private final LoopMode loopMode;
    private final boolean override;
    private final boolean renderFirstPerson;
    private final boolean renderFirstPersonHead;
    private final boolean renderFirstPersonBody;
    private final boolean aimFollowArms;
    private final boolean aimFollowArmsX;
    private final AimFollowMode aimFollowMode;
    private final boolean mirrorForLeftHanded;
    private final boolean startAtEnd;

    private PlayOptions(
            LoopMode loopMode,
            boolean override,
            boolean renderFirstPerson,
            boolean renderFirstPersonHead,
            boolean renderFirstPersonBody,
            boolean aimFollowArms,
            boolean aimFollowArmsX,
            AimFollowMode aimFollowMode,
            boolean mirrorForLeftHanded,
            boolean startAtEnd) {
        this.loopMode = loopMode;
        this.override = override;
        this.renderFirstPerson = renderFirstPerson;
        this.renderFirstPersonHead = renderFirstPersonHead;
        this.renderFirstPersonBody = renderFirstPersonBody;
        this.aimFollowArms = aimFollowArms;
        this.aimFollowArmsX = aimFollowArmsX;
        this.aimFollowMode = aimFollowMode;
        this.mirrorForLeftHanded = mirrorForLeftHanded;
        this.startAtEnd = startAtEnd;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static PlayOptions defaults() {
        return builder().build();
    }

    public LoopMode loopMode() {
        return loopMode;
    }

    public boolean override() {
        return override;
    }

    public boolean renderFirstPerson() {
        return renderFirstPerson;
    }

    public boolean renderFirstPersonHead() {
        return renderFirstPersonHead;
    }

    public boolean renderFirstPersonBody() {
        return renderFirstPersonBody;
    }

    public boolean aimFollowArms() {
        return aimFollowArms;
    }

    public boolean aimFollowArmsX() {
        return aimFollowArmsX;
    }

    public AimFollowMode aimFollowMode() {
        return aimFollowMode;
    }

    public boolean mirrorForLeftHanded() {
        return mirrorForLeftHanded;
    }

    public boolean startAtEnd() {
        return startAtEnd;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeByte(loopMode.ordinal());
        int flags = 0;
        if (override) {
            flags |= 1;
        }
        if (renderFirstPerson) {
            flags |= 2;
        }
        if (renderFirstPersonHead) {
            flags |= 4;
        }
        if (renderFirstPersonBody) {
            flags |= 8;
        }
        if (aimFollowArms) {
            flags |= 16;
        }
        if (mirrorForLeftHanded) {
            flags |= 32;
        }
        if (startAtEnd) {
            flags |= 64;
        }
        if (aimFollowArmsX) {
            flags |= 128;
        }
        buf.writeByte(flags);
        buf.writeByte(aimFollowMode.ordinal());
    }

    public static PlayOptions decode(FriendlyByteBuf buf) {
        LoopMode loopMode = LoopMode.byOrdinal(buf.readUnsignedByte());
        int flags = buf.readUnsignedByte();
        AimFollowMode aimFollowMode = AimFollowMode.byOrdinal(buf.readUnsignedByte());
        return new PlayOptions(
                loopMode,
                (flags & 1) != 0,
                (flags & 2) != 0,
                (flags & 4) != 0,
                (flags & 8) != 0,
                (flags & 16) != 0,
                (flags & 128) != 0,
                aimFollowMode,
                (flags & 32) != 0,
                (flags & 64) != 0
        );
    }

    public static final class Builder {
        private LoopMode loopMode = LoopMode.PLAY_ONCE;
        private boolean override = true;
        private boolean renderFirstPerson = false;
        private boolean renderFirstPersonHead = false;
        private boolean renderFirstPersonBody = true;
        private boolean aimFollowArms = false;
        private boolean aimFollowArmsX = true;
        private AimFollowMode aimFollowMode = AimFollowMode.ARMS_LOCAL;
        private boolean mirrorForLeftHanded = false;
        private boolean startAtEnd = false;

        public Builder loopMode(LoopMode loopMode) {
            this.loopMode = loopMode;
            return this;
        }

        public Builder override(boolean override) {
            this.override = override;
            return this;
        }

        public Builder renderFirstPerson(boolean renderFirstPerson) {
            this.renderFirstPerson = renderFirstPerson;
            return this;
        }

        public Builder renderFirstPersonHead(boolean renderFirstPersonHead) {
            this.renderFirstPersonHead = renderFirstPersonHead;
            return this;
        }

        public Builder renderFirstPersonBody(boolean renderFirstPersonBody) {
            this.renderFirstPersonBody = renderFirstPersonBody;
            return this;
        }

        public Builder aimFollowArms(boolean aimFollowArms) {
            this.aimFollowArms = aimFollowArms;
            return this;
        }

        public Builder aimFollowArmsX(boolean aimFollowArmsX) {
            this.aimFollowArmsX = aimFollowArmsX;
            return this;
        }

        public Builder aimFollowMode(AimFollowMode aimFollowMode) {
            this.aimFollowMode = aimFollowMode;
            return this;
        }

        public Builder mirrorForLeftHanded(boolean mirrorForLeftHanded) {
            this.mirrorForLeftHanded = mirrorForLeftHanded;
            return this;
        }

        public Builder startAtEnd(boolean startAtEnd) {
            this.startAtEnd = startAtEnd;
            return this;
        }

        public PlayOptions build() {
            return new PlayOptions(
                    loopMode,
                    override,
                    renderFirstPerson,
                    renderFirstPersonHead,
                    renderFirstPersonBody,
                    aimFollowArms,
                    aimFollowArmsX,
                    aimFollowMode,
                    mirrorForLeftHanded,
                    startAtEnd
            );
        }
    }
}
