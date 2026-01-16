# PaperTS

PaperTS is a Paper Minecraft plugin that enables you to write Minecraft server plugins in TypeScript, running on the Node.js JavaScript engine via [Javet](https://github.com/caoccao/Javet). It provides a runtime environment for TypeScript modules, allowing you to leverage the power and flexibility of JavaScript/TypeScript for Minecraft server development. **Node.JS runtime is downloaded at runtime**, the plugins install it from [Javet Official Depedencies](https://www.caoccao.com/Javet/tutorial/basic/installation.html), you may verify if your platform is supported.

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

### Note

#### Importing Node Modules

The root for the imports of node modules is the root of the working directory, so if your directory structure is like this:

```
my-plugin/
├── package.json
├── src/
│   └── index.ts
├── dist/
│   └── index.js
└── node_modules/
    └── some-node-module/
```

PaperTS will look for modules in `my-plugin` directory, you may need to adjust the `require` functiion if you want to import scripts from diferents paths that the root of the module. The required function is already modified with the following code to allow importing the java classes:

```js
const Module = require("module");
const originalRequire = Module.prototype.require;

Module.prototype.require = function () {
  if (arguments.length === 1 && typeof arguments[0] === "string" && (arguments[0].startsWith("org.") || arguments[0].startsWith("java.") || arguments[0].startsWith("net.") || arguments[0].startsWith("com."))) {
    return javet.package[arguments[0]];
  }

  return originalRequire.apply(this, arguments);
};
```

#### Get and Set methods

According to the Javet wiki, Javet guesses the `get` and `set` methods. For example, `getName()` turns into `name`, and `setName(value)` becomes `name = value`. So, when using these methods, you should use the property-like syntax. For example, instead of calling `player.getName()`, you should use `player.name`. Similarly, to set a value, use the assignment syntax like `player.name = "NewName"`.

### PaperTS Global API

The following global API is available in your TypeScript/JavaScript code:

```ts
import { Event } from "org.bukkit.event";
import { CommandSender } from "org.bukkit.command";
import { PersistentDataContainer } from "org.bukkit.persistence";
import { List } from "java.util";
import { Runnable } from "java.lang";
import { JavaPlugin } from "org.bukkit.plugin.java";

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
    container: PersistentDataContainer
  ): string;

  export function hasKeyPersistentContainer(
    key: string,
    container: PersistentDataContainer
  ): boolean;

  export function setPersistentContainerString(
    key: string,
    value: string,
    container: PersistentDataContainer
  ): void;

  export function getJavaPlugin(): JavaPlugin;

  export function createRunnable(runnable: () => void): Runnable;
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
    "build": "bun build ./src/index.ts --outdir=./dist --target=node --format=cjs --external=org.* --external=com.* --external=net.* --external=io.* --external=java.* --external=javet.* --watch"
  },
  "dependencies": {
    "bun": "^1.2.19",
    "paperts-java-ts-bind": "github:MetlHedd/java-ts-bind#1.21.4-R0.1-SNAPSHOT",
    "typescript": "^5.8.3",
    "@types/node": "^24.0.15"
  }
}
```

Then you can use `npm run build` to bundle your TypeScript code into a single JavaScript file that can be run by PaperTS.

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
### Java Bridge API



PaperTS provides a global `Java` object that allows you to interact with Java classes, enums, and static methods directly from TypeScript/JavaScript. This is useful when you need to access Java functionality that isn't directly exposed through the PaperTS API.



#### Available Methods
To use the Java Bridge with full TypeScript support, create a `javaBridge.ts` file in your project and add the following type declarations:
```ts
declare const Java: {
   /**
    * Get an enum value by class name and value name.
    */
   enumValue<T = unknown>(className: string, valueName: string): T;

   /**
    * Get all values of an enum.
    */
   enumValues<T = unknown>(className: string): T[];

   /**
    * Create a new instance of a Java class.
    */
   newInstance<T = unknown>(className: string, ...args: unknown[]): T;

   /**
    * Call a static method on a Java class.
    */
   callStatic<T = unknown>(
           className: string,
           methodName: string,
           ...args: unknown[]
   ): T;

   /**
    * Get a static field value from a Java class.
    */
   getStatic<T = unknown>(className: string, fieldName: string): T;

   /**
    * Check if a Java class exists.
    */
   classExists(className: string): boolean;
};

```

#### Usage Examples

##### Working with Enums
```ts
const player = event.getPlayer();
const allGameModes: any = Java.enumValues("org.bukkit.GameMode");
player.sendMessage(`Game mode: ${allGameModes.toString()}`);
```

##### Creating Java Objects
```ts 
// Create a new Location
const world = Java.callStatic("org.bukkit.Bukkit", "getWorld", "world");
const location = Java.newInstance("org.bukkit.Location", world, 100, 64, 100);
player.teleport(location);

```

##### Calling Static Methods
```ts 
Java.callStatic("org.bukkit.Bukkit", "broadcastMessage", "Hello from PaperTS!");

const onlinePlayer: any[] = Java.callStatic(
        "org.bukkit.Bukkit",
        "getOnlinePlayers"
);
onlinePlayer.forEach((player) => player.sendMessage("Hi everyone"));

```
##### Checking Class Existence
```ts 
// Check if a class exists before using it
if (Java.classExists("com.example.CustomPlugin")) {
   const customValue = Java.callStatic("com.example.CustomPlugin", "getValue");
   player.sendMessage(`Custom plugin value: ${customValue}`);
} else {
   player.sendMessage("Custom plugin not found");
}

// Useful for optional integrations
const hasVault = Java.classExists("net.milkbowl.vault.economy.Economy");
if (hasVault) {
   // Initialize Vault integration
}
```
##### Getting Static Fields

```ts
// Get a static constant
const legacyPrefix = Java.getStatic("org.bukkit.Material", "LEGACY_PREFIX");;
player.sendMessage(legacyPrefix);
```

### Internationalization (i18n)

PaperTS supports internationalization (i18n) through the Node.js runtime, enabling it may lead to more compatible behavior with some Node.js modules. To enable i18n, create a folder name `node-icu` in the data folder of the plugin (normally `plugins/PaperTS/node-icu`) and place the ICU data files there. You can download the ICU data files from any actions run of the [Javet repository](https://github.com/caoccao/Javet). The plugin will automatically detect the presence of the `node-icu` folder and enable i18n support.

### Setting up the Run Type

You can configure how the script runs by specifying the `runType` field in the `package.json` of your module. This determines whether the script executes synchronously or asynchronously, and on which thread. Below is an example configuration:

```json
{
  "runType": "SynchronousOnNextTick"
}
```

The available options for `runType` are:

- **`SynchronousOnNextTick`**: Executes the script synchronously on the next server tick.
- **`AsynchronousOnNextTick`**: Executes the script asynchronously on the next server tick.
- **`NewThread`**: Executes the script asynchronously on a separate thread.

### Resource Cleanup

When unloading or reloading a module, PaperTS attempts to clean up resources by calling a `cleanup` function if it exists in the module's main script. This function should handle any necessary cleanup tasks, such as closing database connections, closing a http server, or freeing up memory.

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
- [ScriptCraft](https://github.com/walterhiggins/ScriptCraft)
