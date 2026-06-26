package destiny.null_ouroboros.server.terminal.p2p;

import destiny.null_ouroboros.server.terminal.filesystem.TerminusFileSystem;

public final class P2pFilter {
    private P2pFilter() {}

    public static boolean passes(P2pSettings settings, String remoteIpvInf, TerminusFileSystem fs) {
        return settings.passesFilter(remoteIpvInf, fs);
    }
}
