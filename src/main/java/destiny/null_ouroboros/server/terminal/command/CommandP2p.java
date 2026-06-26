package destiny.null_ouroboros.server.terminal.command;



import destiny.null_ouroboros.server.block.entity.DustyComputerBlockEntity;

import destiny.null_ouroboros.server.terminal.TerminalArgumentParser;

import destiny.null_ouroboros.server.terminal.TerminalCommand;

import destiny.null_ouroboros.server.terminal.filesystem.ComputerRecord;

import destiny.null_ouroboros.server.terminal.filesystem.TerminusFileSystem;

import destiny.null_ouroboros.server.terminal.filesystem.TerminusSavedData;

import destiny.null_ouroboros.server.terminal.p2p.P2pConnectionManager;

import destiny.null_ouroboros.server.terminal.p2p.P2pSettings;

import net.minecraft.core.BlockPos;

import net.minecraft.server.level.ServerLevel;

import net.minecraft.world.level.Level;

import net.minecraft.world.level.block.entity.BlockEntity;



import javax.annotation.Nullable;

import java.util.List;



public class CommandP2p extends TerminalCommand {

    private final String args;



    public CommandP2p(TerminusFileSystem fs, BlockPos pos, @Nullable Level level, String args) {

        super(fs, pos, level);

        this.args = args.trim();

    }



    @Override

    public void execute() {

        if (!(level instanceof ServerLevel serverLevel)) {

            printlnTranslatable("message.null_ouroboros.terminus.internal_error");

            setDone();

            return;

        }

        BlockEntity blockEntity = serverLevel.getBlockEntity(computerPos);

        if (!(blockEntity instanceof DustyComputerBlockEntity computer) || computer.getIpvInf() == null) {

            printlnTranslatable("message.null_ouroboros.terminus.internal_error");

            setDone();

            return;

        }

        computer.refreshEmaConnection();

        if (!computer.hasConnectedEma()) {

            printlnTranslatable("message.null_ouroboros.terminus.peripheral.not_found");

            setDone();

            return;

        }

        TerminusSavedData data = TerminusSavedData.get(serverLevel);

        if (data == null) {

            printlnTranslatable("message.null_ouroboros.terminus.internal_error");

            setDone();

            return;

        }

        String localIpvInf = computer.getIpvInf();

        ComputerRecord record = data.getOrCreateComputer(localIpvInf, computerPos);

        P2pSettings settings = record.getP2pSettings();

        settings.bootstrap(fs);



        List<String> parsed = TerminalArgumentParser.parse(args);

        if (parsed.isEmpty()) {

            printlnTranslatable("message.null_ouroboros.terminus.p2p.usage");

            setDone();

            return;

        }



        String sub = parsed.get(0).toLowerCase();

        try {

            switch (sub) {

                case "alias" -> {

                    if (parsed.size() <= 1) {

                        println(settings.getAlias());

                    } else {

                        settings.setAlias(parsed.get(1));

                    }

                }

                case "filter" -> handleFilterSubcommand(parsed, settings);

                case "log_directory" -> {

                    settings.setLogDirectory(parsed.size() > 1 ? parsed.get(1) : "");

                }

                case "reciever_directory" -> {

                    if (parsed.size() < 2) {

                        printlnTranslatable("message.null_ouroboros.terminus.p2p.reciever_directory.usage");

                    } else {

                        settings.setReceiverDirectory(parsed.get(1));

                        P2pSettings.ensureDirectory(fs, settings.getReceiverDirectory());

                    }

                }

                case "request" -> {

                    if (parsed.size() < 2) {

                        printlnTranslatable("message.null_ouroboros.terminus.p2p.request.usage");

                    } else if (!TerminusSavedData.isValidIpvInf(parsed.get(1))) {

                        printlnTranslatable("message.null_ouroboros.terminus.p2p.invalid_ipvinf");

                    } else {

                        P2pConnectionManager.requestConnection(serverLevel, localIpvInf, parsed.get(1));

                    }

                }

                case "disconnect" -> P2pConnectionManager.disconnect(localIpvInf, P2pConnectionManager.DisconnectCause.LOCAL_CLOSED);

                case "send" -> {

                    if (parsed.size() < 3) {

                        printlnTranslatable("message.null_ouroboros.terminus.p2p.send.usage");

                    } else {

                        String mode = parsed.get(2).toUpperCase();

                        if (!mode.equals("COPY") && !mode.equals("CUT")) {

                            printlnTranslatable("message.null_ouroboros.terminus.p2p.send.bad_mode");

                        } else {

                            P2pConnectionManager.startTransferRequest(serverLevel, localIpvInf, parsed.get(1), mode);

                        }

                    }

                }

                default -> printlnTranslatable("message.null_ouroboros.terminus.p2p.unknown_subcommand", sub);

            }

        } catch (IllegalArgumentException e) {

            printlnTranslatable("message.null_ouroboros.terminus.p2p.invalid_ipvinf");

        }

        setDone();

    }



    private void handleFilterSubcommand(List<String> parsed, P2pSettings settings) {

        if (parsed.size() < 2) {

            printlnTranslatable("message.null_ouroboros.terminus.p2p.filter.usage");

            return;

        }

        String action = parsed.get(1).toLowerCase();

        if (action.equals("file")) {

            if (parsed.size() < 3 || !TerminusFileSystem.isTextFileName(parsed.get(2))) {

                if (parsed.size() < 3) {

                    printlnTranslatable("message.null_ouroboros.terminus.p2p.filter.file.usage");

                } else {

                    printlnTranslatable("message.null_ouroboros.terminus.p2p.filter_file_invalid");

                }

                return;

            }

            settings.setFilterFilePath(parsed.get(2));

            settings.bootstrap(fs);

            return;

        }

        if (action.equals("add") || action.equals("remove")) {

            if (parsed.size() < 3) {

                printlnTranslatable(action.equals("add")

                        ? "message.null_ouroboros.terminus.p2p.filter.add.usage"

                        : "message.null_ouroboros.terminus.p2p.filter.remove.usage");

                return;

            }

            try {

                String ipvInf = parsed.get(2);

                if (!TerminusSavedData.isValidIpvInf(ipvInf)) {

                    throw new IllegalArgumentException();

                }

                if (action.equals("add")) {

                    settings.addFilterEntry(ipvInf, fs);

                } else {

                    settings.removeFilterEntry(ipvInf, fs);

                }

            } catch (Exception e) {

                printlnTranslatable("message.null_ouroboros.terminus.p2p.invalid_ipvinf");

            }

            return;

        }

        P2pSettings.FilterMode mode = P2pSettings.FilterMode.parse(parsed.get(1));

        if (mode == null) {

            printlnTranslatable("message.null_ouroboros.terminus.p2p.filter_invalid");

            return;

        }

        settings.setFilterMode(mode);

    }

}

