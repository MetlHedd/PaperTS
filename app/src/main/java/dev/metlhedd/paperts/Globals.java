package dev.metlhedd.paperts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

class EventHandler {
  private Function<Event, Void> handler;
  private Listener listener;

  public EventHandler(Function<Event, Void> handler) {
    this.handler = handler;
    this.listener = new Listener() {
    };
  }

  public Listener getListener() {
    return this.listener;
  }

  public void handleEvent(Event event) {
    try {
      handler.apply(event);
    } catch (Exception e) {
      Bukkit.getLogger().severe("Error handling event: " + e.getMessage());
      e.printStackTrace();
    }
  }
}

class CommandExecutor extends BukkitCommand {
  private BiFunction<CommandSender, String[], Void> handler;

  public CommandExecutor(BiFunction<CommandSender, String[], Void> handler, String commandName, String description,
      String usageMessage, String permission, ArrayList<String> aliases) {
    super(commandName);

    this.handler = handler;
    this.setDescription(description);
    this.setUsage(usageMessage);
    this.setPermission(permission);
    this.setAliases(aliases);
  }

  @Override
  public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel,
      @NotNull String @NotNull [] args) {
    try {
      handler.apply(sender, args);

      return true;
    } catch (Exception e) {
      Bukkit.getLogger().severe("Error executing command: " + e.getMessage());
      e.printStackTrace();

      return false;
    }
  }

}

/**
 * Globals class for managing global event handlers and commands.
 * This class allows for registering and unregistering event handlers and
 * commands.
 */
public class Globals {
  /**
   * The JavaPlugin instance for the plugin.
   * This is used to register events and commands within the Bukkit server.
   */
  private JavaPlugin plugin;

  /**
   * Maps event classes to their corresponding event handlers.
   * This allows for handling events in a type-safe manner.
   * Each event class can have a single handler registered to it.
   */
  private HashMap<Class<?>, EventHandler> eventHandlers;
  /**
   * Maps command names to their corresponding command executors.
   * This allows for handling commands in a type-safe manner.
   * Each command can have a single executor registered to it.
   * If a command is registered multiple times, it will throw an exception.
   */
  private HashMap<String, CommandExecutor> commands;

  /**
   * Constructor for the Globals class.
   * Initializes the plugin instance and the maps for event handlers and commands.
   * 
   * @param plugin The JavaPlugin instance for the plugin.
   */

  public Globals(JavaPlugin plugin) {
    this.plugin = plugin;

    this.eventHandlers = new HashMap<>();
    this.commands = new HashMap<>();
  }

  /**
   * Registers an event handler for a specific event class.
   *
   * @param eventClass The class of the event to register.
   * @param handler    The function to handle the event.
   * @param <T>        The type of the event.
   */
  public <T extends Event> void registerEvent(Class<T> eventClass, Function<Event, Void> handler) {
    // Check if the event class is already registered
    if (eventHandlers.containsKey(eventClass)) {
      throw new RuntimeException("Event " + eventClass.getName() + " is already registered.");
    }

    EventHandler eventHandler = new EventHandler(handler);

    Bukkit.getPluginManager().registerEvent(eventClass, eventHandler.getListener(),
        org.bukkit.event.EventPriority.NORMAL,
        (Listener l, Event event) -> {
          eventHandler.handleEvent(event);
        }, this.plugin);
  }

  /**
   * Unregisters an event handler for a specific event class.
   *
   * @param eventClass The class of the event to unregister.
   * @throws RuntimeException if the event class is not registered.
   */
  public void unregisterEvent(Class<? extends Event> eventClass) throws RuntimeException {
    if (!eventHandlers.containsKey(eventClass)) {
      throw new RuntimeException("Event " + eventClass.getName() + " is not registered.");
    }

    EventHandler handler = eventHandlers.remove(eventClass);
    HandlerList.unregisterAll(handler.getListener());
  }

  /**
   * Unregisters all event handlers.
   * This is useful for cleaning up when the plugin is disabled.
   */
  public void unregisterAllEvents() {
    for (EventHandler handler : eventHandlers.values()) {
      HandlerList.unregisterAll(handler.getListener());
    }
    eventHandlers.clear();
  }

  /**
   * Registers a command with the plugin's command map.
   * If the command is already registered, it will throw an exception.
   *
   * @param commandName  The name of the command.
   * @param description  The description of the command.
   * @param usageMessage The usage message for the command.
   * @param permission   The permission required to execute the command.
   * @param aliases      The aliases for the command.
   * @param handler      The function to handle the command execution.
   * @throws RuntimeException if the command is already registered.
   */
  public void registerCommand(String commandName,
      String description, String usageMessage, String permission, ArrayList<String> aliases,
      BiFunction<CommandSender, String[], Void> handler) throws RuntimeException {
    if (this.commands.containsKey(commandName)) {
      throw new RuntimeException("Command " + commandName + " is already registered.");
    }

    CommandExecutor commandExecutor = new CommandExecutor(handler, commandName, description, usageMessage, permission,
        aliases);

    this.plugin.getServer().getCommandMap().register(commandName, commandExecutor);
    this.commands.put(commandName, commandExecutor);
  }

  /**
   * Unregisters a command from the plugin's command map.
   * If the command is not registered, it will throw an exception.
   *
   * @param commandName The name of the command to unregister.
   * @throws RuntimeException if the command is not registered.
   */
  public void unregisterCommand(String commandName) throws RuntimeException {
    if (!this.commands.containsKey(commandName)) {
      throw new RuntimeException("Command " + commandName + " is not registered.");
    }

    this.commands.remove(commandName);
    this.plugin.getServer().getCommandMap().getKnownCommands().remove(commandName);
  }

  /**
   * Unregisters all commands from the plugin's command map.
   * This is useful for cleaning up when the plugin is disabled.
   */
  public void unregisterAllCommands() {
    for (String commandName : this.commands.keySet()) {
      this.plugin.getServer().getCommandMap().getKnownCommands().remove(commandName);
    }
    this.commands.clear();
  }

  /**
   * Retrieves a string value from a PersistentDataContainer.
   * This method allows for retrieving a string value associated with a specific
   * key in the container.
   * 
   * @param key       The key to look up in the container.
   * @param container The PersistentDataContainer to retrieve the value from.
   * @throws IllegalArgumentException if the container is null.
   * @return The string value associated with the key, or null if not found.
   */
  public String getPersistentContainerString(String key, PersistentDataContainer container)
      throws IllegalArgumentException {
    if (container == null) {
      throw new IllegalArgumentException("PersistentDataContainer cannot be null.");
    }

    return container.get(new NamespacedKey(this.plugin, key), PersistentDataType.STRING);
  }

  /**
   * Retrieves a list of strings from a PersistentDataContainer.
   * This method allows for retrieving a list of strings associated with a
   * specific
   * key in the container.
   * 
   * @param key       The key to look up in the container.
   * @param container The PersistentDataContainer to retrieve the list from.
   * @return An array of strings associated with the key, or an empty array if not
   *         found.
   * @throws IllegalArgumentException if the container is null.
   */
  public String[] getPersistentContainerStringList(String key, PersistentDataContainer container)
      throws IllegalArgumentException {
    if (container == null) {
      throw new IllegalArgumentException("PersistentDataContainer cannot be null.");
    }

    List<String> list = container.get(new NamespacedKey(this.plugin, key), PersistentDataType.LIST.strings());
    String[] array = new String[list.size()];

    return list.toArray(array);
  }

  /**
   * Checks if a key exists in a PersistentDataContainer.
   * This method allows for checking if a specific key is present in the
   * container.
   * 
   * @param key       The key to check for in the container.
   * @param container The PersistentDataContainer to check.
   * @return true if the key exists, false otherwise.
   * @throws IllegalArgumentException if the container is null.
   */
  public boolean hasKeyPersistentContainer(String key, PersistentDataContainer container)
      throws IllegalArgumentException {
    if (container == null) {
      throw new IllegalArgumentException("PersistentDataContainer cannot be null.");
    }

    return container.has(new NamespacedKey(this.plugin, key));
  }

  /**
   * Sets a string value in a PersistentDataContainer.
   * This method allows for setting a string value associated with a specific key
   * in the container.
   *
   * @param key       The key to associate with the value.
   * @param value     The string value to set.
   * @param container The PersistentDataContainer to set the value in.
   * @throws IllegalArgumentException if the container is null.
   */
  public void setPersistentContainerString(String key, String value, PersistentDataContainer container)
      throws IllegalArgumentException {
    if (container == null) {
      throw new IllegalArgumentException("PersistentDataContainer cannot be null.");
    }

    container.set(new NamespacedKey(this.plugin, key), PersistentDataType.STRING, value);
  }

  /**
   * Sets a list of strings in a PersistentDataContainer.
   * This method allows for setting a list of strings associated with a specific
   * key
   * in the container.
   * 
   * @param key       The key to associate with the list of strings.
   * @param value     The list of strings to set.
   * @param container The PersistentDataContainer to set the list in.
   * @throws IllegalArgumentException if the container is null.
   */
  public void setPersistentContainerStringList(String key, String[] value,
      PersistentDataContainer container) throws IllegalArgumentException {
    if (container == null) {
      throw new IllegalArgumentException("PersistentDataContainer cannot be null.");
    }

    container.set(new NamespacedKey(this.plugin, key), PersistentDataType.LIST.strings(), List.of(value));
  }
}
