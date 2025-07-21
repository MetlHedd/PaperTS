# PaperTS (Work in Progress)

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
    // Access PaperTS global for event/command registration
    PaperTS.registerEvent(org.bukkit.event.player.PlayerJoinEvent, (event) => {
      PaperTS.sendMessage(event.player.name, 'Welcome to the server!');
    });
    ```
5. Start your Paper server. PaperTS will automatically load your module. You can reload the modules any time using the `/paperts reload` command.

### Using typescript

To use TypeScript, you can set up a `tsconfig.json` in your module directory:

```json
{
  "compilerOptions": {
    "incremental": true,
    "module": "commonjs",
    "esModuleInterop": false,
    "strict": true,
    "outDir": "./dist",
    "forceConsistentCasingInFileNames": true,
    "skipLibCheck": true,
    "typeRoots": [
      "node_modules/paperts-java-ts-bind"
    ]
  },
  "include": [
    "src/**/*"
  ]
}
```

Install the necessary depedencies in your module directory:

```sh
npm install --save github:MetlHedd/java-ts-bind#1.21.4-R0.1-SNAPSHOT
```

You can substitute the version, with another version of the `paperts-java-ts-bind` package, if you want to use a different version. You can also request another version by opening an issue on this repository.

Then, you can write your TypeScript code in the `src` directory and compile it to JavaScript in the `dist` directory.

Your `package.json` should look like this:

```json
{
  "main": "dist/index.js",
  "scripts": {
    "watch": "tsc --project . --watch"
  },
  "dependencies": {
    "paperts-java-ts-bind": "github:MetlHedd/java-ts-bind#1.21.4-R0.1-SNAPSHOT",
    "typescript": "^5.8.3"
  }
}
```

### Plugin Commands

You can use the following commands to manage your PaperTS modules:
- `/paperts reload`: Reloads all modules.
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

