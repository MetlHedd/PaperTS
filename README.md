# PaperTS

PaperTS is a Paper Minecraft plugin that enables you to write Minecraft server plugins in TypeScript, running on the Node.js JavaScript engine via [Javet](https://github.com/caoccao/Javet). It provides a runtime environment for TypeScript modules, allowing you to leverage the power and flexibility of JavaScript/TypeScript for Minecraft server development.

## Features

- **TypeScript/JavaScript Plugin Support**: Write Minecraft plugins in TypeScript or JavaScript and run them directly on your Paper server.
- **Automatic Module Loading**: Scans the plugin's data folder for modules (directories with a `package.json`), initializes, and starts them automatically.
- **Node.js Environment**: Provides a Node.js-like environment using Javet's NodeRuntime.
- **Event and Command Registration**: Exposes APIs for registering/unregistering Bukkit events and commands from TypeScript.
- **Hot Reloading**: Modules can be released and reloaded without restarting the server.

## How It Works

1. On server start, PaperTS scans its data folder for subdirectories containing a `package.json` file.
2. For each module, it reads the `main` field in `package.json` to find the entry script.
3. The entry script is executed in a Node.js runtime, with access to PaperTS APIs for interacting with the server.
4. Modules can register event handlers and commands using the provided global `PaperTS` object.

## Getting Started

1. **Install PaperTS**: Place the PaperTS plugin JAR in your server's `plugins` folder.
2. **Create a Module**:
    - In the `plugins/PaperTS` folder, create a new directory for your module (e.g., `my-plugin`).
    - Add a `package.json` file with a `main` field pointing to your entry script (e.g., `index.js`).
    - Write your TypeScript/JavaScript code and compile it to JavaScript if needed.
3. **Example `package.json`**:
    ```json
    {
      "name": "my-plugin",
      "main": "index.js"
    }
    ```
4. **Example `index.js`**:
    ```js
    class ServerModule {
      constructor() {
        console.log("My plugin has been loaded!");
      }

      public handlePlayerJoin(event) {
        // Handle player join event
        event.player.sendMessage(event.player.name, "Welcome to the server!");
      }

      public helloCommand(sender, args) {
        sender.sendMessage("Hello from PaperTS!");
      }
    }

    const server = new ServerModule();

    PaperTS.registerEvent(
      org.bukkit.event.player.PlayerJoinEvent,
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

    ```
5. Start your Paper server. PaperTS will automatically load your module. You can reload the modules any time using the `/paperts reload` command.

### PaperTS Global API

The following global API is available in your TypeScript/JavaScript code:

```ts
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
```

#### Registering Events

To register events, use the `PaperTS.registerEvent` method. You can pass the event class and a callback function that will be executed when the event is triggered.

```js
export class ServerModule {
  public handlePlayerJoin(event) {
    console.log(`Player ${event.player.name} has joined the server!`);
  }
}

const server = new ServerModule();

PaperTS.registerEvent(
  org.bukkit.event.PlayerJoinEvent,
  server.handlePlayerJoin.bind(server),
);
```

#### Registering Commands

To register commands, use the `PaperTS.registerCommand` method. You can specify the command name, description, usage message, permission, aliases, and an executor function that will handle the command execution.

```js
class ServerModule {
  public command(sender, args) {
    console.log(`Command executed by ${sender.displayName()} with args: ${args.join(", ")}`);
  }
}

const server = new ServerModule();

PaperTS.registerCommand(
  "commandname",
  "Command description",
  "/commandname <args>",
  "command.permission",
  ["alias1", "alias2"],
  server.command.bind(server),
);
```

#### Note

Using `bind` is necessary when passing methods as callbacks to ensure the correct context (`this`) is maintained.

### Using typescript

To use TypeScript, you can set up a `tsconfig.json` in your module directory:

```json
{
  "compilerOptions": {
    "incremental": true,
    "module": "commonjs",
    "esModuleInterop": true,
    "strict": true,
    "outDir": "./dist",
    "forceConsistentCasingInFileNames": true,
    "skipLibCheck": true,
    "typeRoots": [
      "node_modules/paperts-java-ts-bind",
      "node_modules/@types"
    ]
  },
  "include": [
    "src/**/*"
  ]
}
```

Install the necessary depedencies in your module directory:

```sh
npm install --save github:MetlHedd/java-ts-bind#1.20-R0.1-SNAPSHOT
```

You can substitute the version, with another version of the `paperts-java-ts-bind` package, if you want to use a different version. You can also request another version by opening an issue on this repository.

Then, you can write your TypeScript code in the `src` directory and compile it to JavaScript in the `dist` directory.

Your `package.json` should look like this:

```json
{
  "main": "dist/index.js",
  "scripts": {
    "watch": "tsc --project . --watch",
    "bundle": "esbuild src/index.ts --watch --bundle --outfile=dist/index.js --platform=node --target=node22 --external:org.* --external:com.* --external:net.* --external:java.*"
  },
  "dependencies": {
    "esbuild": "^0.25.8",
    "paperts-java-ts-bind": "github:MetlHedd/java-ts-bind#1.20-R0.1-SNAPSHOT",
    "typescript": "^5.8.3",
    "@types/node": "^24.0.15"
  }
}
```

Then you can use `npm run bundle` to bundle your TypeScript code into a single JavaScript file that can be run by PaperTS.

Example `index.ts`:

```ts
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
```

### Internationalization (i18n)

PaperTS supports internationalization (i18n) through the Node.js runtime, enabling it may lead to more compatible behavior with some Node.js modules. To enable i18n, create a folder name `node-icu` in the data folder of the plugin (normally `plugins/PaperTS/node-icu`) and place the ICU data files there. You can download the ICU data files from any actions run of the [Javet repository](https://github.com/caoccao/Javet). The plugin will automatically detect the presence of the `node-icu` folder and enable i18n support.

## Plugin Commands

You can use the following commands to manage your PaperTS modules:
- `/paperts reload`: Reloads all modules.
- `/paperts reload <module>`: Reloads a specific module.
- `/paperts list`: Lists all loaded modules.
- `/paperts unload <module>`: Unloads a specific module.
- `/paperts load <module>`: Loads a specific module.

## Development

PaperTS uses [Javet](https://github.com/caoccao/Javet) to embed the V8 engine and Node.js runtime. The plugin manages engine pools, working directories, and exposes a `Globals` API for event and command management.

### Building

Build with Gradle:

```sh
./gradlew build
```

The output JAR will be in `app/build/libs/`.

## Contributing

Contributions are welcome! Please open issues or pull requests for bug fixes, features, or documentation improvements.

## Inspiration

Some projects that inspired PaperTS:
- [Grakkit](https://github.com/grakkit/grakkit)
- [Javet](https://github.com/caoccao/Javet)
- [Custom Realms](https://github.com/customrealms)
- [CraftJS](https://github.com/Dysfold/craftjs)