package destiny.null_ouroboros.server.terminal;

import java.util.ArrayList;
import java.util.List;

public final class TerminalArgumentParser {
    private TerminalArgumentParser() {}

    public static List<String> parse(String raw) {
        List<String> args = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        boolean escaping = false;

        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (escaping) {
                current.append(c);
                escaping = false;
                continue;
            }
            if (c == '\\' && quoted) {
                escaping = true;
                continue;
            }
            if (c == '"') {
                quoted = !quoted;
                continue;
            }
            if (Character.isWhitespace(c) && !quoted) {
                if (current.length() > 0) {
                    args.add(current.toString());
                    current.setLength(0);
                }
                continue;
            }
            current.append(c);
        }

        if (current.length() > 0 || quoted) {
            args.add(current.toString());
        }
        return args;
    }
}
