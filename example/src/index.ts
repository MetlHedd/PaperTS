import { Event } from "org.bukkit.event";
import { PlayerJoinEvent } from "org.bukkit.event.player";
import { CommandSender } from "org.bukkit.command";

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
