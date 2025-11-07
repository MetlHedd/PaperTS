import { Event } from "org.bukkit.event";
import { CommandSender } from "org.bukkit.command";
import { PersistentDataContainer } from "org.bukkit.persistence";
import { List } from "java.util";
import { Runnable } from "java.lang";
import { JavaPlugin } from "org.bukkit.plugin.java";
import { PlayerJoinEvent } from "org.bukkit.event.player";

declare namespace PaperTS {
  export function registerEvent<T extends Event>(
    eventClass: { new (...args: any[]): T },
    listener: (event: T) => void,
  ): void;

  export function registerCommand(
    name: string,
    description: string,
    usageMessage: string,
    permission: string,
    aliases: string[],
    executor: (sender: CommandSender, args: string[]) => void,
  ): void;

  export function getPersistentContainerString(
    key: string,
    container: PersistentDataContainer,
  ): string;

  export function getPersistentContainerStringList(
    key: string,
    container: PersistentDataContainer,
  ): string[];

  export function hasKeyPersistentContainer(
    key: string,
    container: PersistentDataContainer,
  ): boolean;

  export function setPersistentContainerString(
    key: string,
    value: string,
    container: PersistentDataContainer,
  ): void;

  export function setPersistentContainerStringList(
    key: string,
    value: List<String>,
    container: PersistentDataContainer,
  ): void;

  export function getJavaPlugin(): JavaPlugin;

  export function createRunnable(runnable: () => void): Runnable;
}

export class ServerModule {
  constructor() {
    // This constructor is called when the module is loaded
    console.log("My plugin has been loaded!");
  }

  public handlePlayerJoin(event: PlayerJoinEvent) {
    // Handle player join event
    event.player.sendMessage(event.player.name, "Welcome to the server!");
  }

  public helloCommand(sender: CommandSender, args: string[]) {
    sender.sendMessage("Hello from PaperTS!");
  }
}

const server = new ServerModule();

PaperTS.registerEvent(
  PlayerJoinEvent,
  server.handlePlayerJoin.bind(server),
);
PaperTS.registerCommand(
  "hello",
  "Says hello to the player",
  "/hello",
  "",
  [],
  server.helloCommand.bind(server),
);
