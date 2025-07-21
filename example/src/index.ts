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

console.log("Hello, PaperTS!");

PaperTS.registerEvent(PlayerJoinEvent, (event) => {
  const player = event.player;
  player.sendMessage("Welcome to the server, " + player.name + "!");
});

PaperTS.registerCommand(
  "greet",
  "Greet a player",
  "/greet <player>",
  "",
  [],
  (sender, args) => {
    console.log("Greet command executed by: " + sender.name);
    console.log("Arguments: " + args.join(", "));
  },
);
