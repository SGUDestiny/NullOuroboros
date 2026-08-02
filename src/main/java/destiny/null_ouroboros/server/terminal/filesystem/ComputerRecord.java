package destiny.null_ouroboros.server.terminal.filesystem;

import destiny.null_ouroboros.server.terminal.p2p.ComputerEndpoint;
import destiny.null_ouroboros.server.terminal.p2p.P2pSettings;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;

public class ComputerRecord {
    private String ipvInf;
    private final TerminusFileSystem fileSystem;
    private final P2pSettings p2pSettings;
    @Nullable
    private ComputerEndpoint endpoint;
    private boolean presetApplied;
    @Nullable
    private String appliedPreset;

    public ComputerRecord(String ipvInf) {
        this(ipvInf, new TerminusFileSystem(), null);
    }

    public ComputerRecord(String ipvInf, TerminusFileSystem fileSystem, @Nullable P2pSettings p2pSettings) {
        this.ipvInf = ipvInf;
        this.fileSystem = fileSystem;
        this.p2pSettings = p2pSettings != null ? p2pSettings : new P2pSettings(fileSystem.getDriveSpecifier());
    }

    public String getIpvInf() { return ipvInf; }
    public void setIpvInf(String ipvInf) { this.ipvInf = ipvInf; }
    public TerminusFileSystem getFileSystem() { return fileSystem; }
    public P2pSettings getP2pSettings() { return p2pSettings; }
    @Nullable public ComputerEndpoint getEndpoint() { return endpoint; }
    public void setEndpoint(@Nullable ComputerEndpoint endpoint) { this.endpoint = endpoint; }

    public boolean isPresetApplied() {
        return presetApplied;
    }

    public void setPresetApplied(boolean presetApplied) {
        this.presetApplied = presetApplied;
    }

    @Nullable
    public String getAppliedPreset() {
        return appliedPreset;
    }

    public void setAppliedPreset(@Nullable String appliedPreset) {
        this.appliedPreset = appliedPreset;
    }

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("IpvInf", ipvInf);
        tag.put("FS", fileSystem.toNBT());
        tag.put("P2p", p2pSettings.toNBT());
        if (endpoint != null) {
            tag.put("Endpoint", endpoint.toNBT());
        }
        tag.putBoolean("PresetApplied", presetApplied);
        if (appliedPreset != null) {
            tag.putString("AppliedPreset", appliedPreset);
        }
        return tag;
    }

    public static ComputerRecord fromNBT(CompoundTag tag) {
        String ipvInf = tag.getString("IpvInf");
        TerminusFileSystem fs = new TerminusFileSystem();
        fs.fromNBT(tag.getCompound("FS"));
        P2pSettings settings = new P2pSettings(fs.getDriveSpecifier());
        if (tag.contains("P2p")) {
            settings.fromNBT(tag.getCompound("P2p"), fs.getDriveSpecifier());
        }
        ComputerRecord record = new ComputerRecord(ipvInf, fs, settings);
        if (tag.contains("Endpoint")) {
            record.setEndpoint(ComputerEndpoint.fromNBT(tag.getCompound("Endpoint")));
        }
        record.presetApplied = tag.getBoolean("PresetApplied");
        if (tag.contains("AppliedPreset")) {
            record.appliedPreset = tag.getString("AppliedPreset");
        }
        return record;
    }
}
