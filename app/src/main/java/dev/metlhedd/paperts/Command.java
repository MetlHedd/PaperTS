package dev.metlhedd.paperts;

import java.nio.file.Path;
import java.util.Set;

import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.jetbrains.annotations.NotNull;

public class Command extends BukkitCommand {
  private PaperTSPlugin plugin;

  protected Command(@NotNull String name, PaperTSPlugin plugin) {
    super(name);
    this.plugin = plugin;

    this.setDescription("Command for managing PaperTS modules");
    this.setUsage("/paperts <subcommand>");
    this.setPermission("paperts.command");
  }

  @Override
  public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel,
      @NotNull String @NotNull [] args) {
    if (args.length == 0) {
      sender.sendMessage("Usage: /paperts <subcommand>");
      return false;
    } else if (args.length == 1) {
      String subcommand = args[0].toLowerCase();
      switch (subcommand) {
        case "reload":
          sender.sendMessage("Reloading PaperTS modules...");

          try {
            plugin.reloadModule(args[1]);
            sender.sendMessage("Modules reloaded successfully.");
          } catch (Exception e) {
            sender.sendMessage("Failed to reload modules: " + e.getMessage());
            return false;
          }

          // Call the reload logic here
          return true;
        case "list":
          sender.sendMessage("Listing PaperTS modules...");

          Set<Path> modules = plugin.listModules();

          for (Path module : modules) {
            sender.sendMessage("Module: " + module.getFileName());
          }

          return true;
        default:
          sender.sendMessage("Unknown subcommand: " + subcommand);
          return false;
      }
    } else {
      String subCommand = args[0].toLowerCase();
      String[] subArgs = new String[args.length - 1];

      System.arraycopy(args, 1, subArgs, 0, args.length - 1);

      // Handle subcommands with additional arguments
      switch (subCommand) {
        case "load":
          String moduleName = subArgs[0];
          sender.sendMessage("Loading PaperTS module: " + moduleName);

          try {
            plugin.loadModule(moduleName);
            sender.sendMessage("Module loaded successfully.");
          } catch (Exception e) {
            sender.sendMessage("Failed to load module: " + e.getMessage());
            e.printStackTrace();
            return false;
          }

          return true;
        case "unload":
          String unloadModuleName = subArgs[0];
          sender.sendMessage("Unloading PaperTS module: " + unloadModuleName);

          try {
            plugin.unloadModule(unloadModuleName);
            sender.sendMessage("Module unloaded successfully.");
          } catch (Exception e) {
            sender.sendMessage("Failed to unload module: " + e.getMessage());
            e.printStackTrace();
            return false;
          }

          return true;
        default:
          sender.sendMessage("Unknown subcommand: " + subCommand);
          return false;
      }
    }
  }

}
