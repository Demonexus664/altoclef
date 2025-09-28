package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.Config;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.commandsystem.exception.BadCommandSyntaxException;
import adris.altoclef.commandsystem.exception.CommandException;
import adris.altoclef.movement.AdvancedParkourModule;
import adris.altoclef.movement.ParkourMode;

import java.util.Locale;

public class ParkourModeCommand extends Command {

    public ParkourModeCommand() {
        super("parkour", "Configure advanced parkour and smoothing");
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) throws CommandException {
        String option = parser.get(String.class);
        if (option == null) {
            Config config = mod.getConfig();
            log(String.format(Locale.ROOT, "Parkour mode: %s, smoothing: %s", config.parkourMode, config.smoothingEnabled ? "on" : "off"));
            finish();
            return;
        }
        String normalized = option.toLowerCase(Locale.ROOT);
        Config config = mod.getConfig();
        AdvancedParkourModule module = mod.getAdvancedParkourModule();
        switch (normalized) {
            case "off" -> {
                config.parkourMode = ParkourMode.OFF;
                module.setEnabled(false);
                log("Advanced parkour disabled.");
            }
            case "basic" -> {
                config.parkourMode = ParkourMode.BASIC;
                module.setEnabled(true);
                log("Advanced parkour set to BASIC.");
            }
            case "adv", "advanced" -> {
                config.parkourMode = ParkourMode.ADVANCED;
                module.setEnabled(true);
                log("Advanced parkour set to ADVANCED.");
            }
            case "smooth" -> {
                String toggle = parser.get(String.class);
                if (toggle == null) {
                    throw new BadCommandSyntaxException("Expected 'on' or 'off' for smoothing toggle.");
                }
                toggle = toggle.toLowerCase(Locale.ROOT);
                if (toggle.equals("on")) {
                    config.smoothingEnabled = true;
                    log("Movement smoothing enabled.");
                } else if (toggle.equals("off")) {
                    config.smoothingEnabled = false;
                    log("Movement smoothing disabled.");
                } else {
                    throw new BadCommandSyntaxException("Expected 'on' or 'off' for smoothing toggle.");
                }
            }
            default -> throw new BadCommandSyntaxException("Unknown parkour mode option: " + option);
        }
        finish();
    }
}
