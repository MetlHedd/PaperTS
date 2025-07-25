package dev.metlhedd.paperts;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bukkit.plugin.java.JavaPlugin;

import com.caoccao.javet.enums.JSRuntimeType;
import com.caoccao.javet.exceptions.JavetException;
import com.caoccao.javet.interception.jvm.JavetJVMInterceptor;
import com.caoccao.javet.interop.NodeRuntime;
import com.caoccao.javet.interop.V8Runtime;
import com.caoccao.javet.interop.converters.JavetProxyConverter;
import com.caoccao.javet.interop.engine.IJavetEngine;
import com.caoccao.javet.interop.engine.IJavetEnginePool;
import com.caoccao.javet.interop.engine.JavetEnginePool;
import com.caoccao.javet.interop.options.NodeRuntimeOptions;
import com.caoccao.javet.node.modules.NodeModuleModule;
import com.google.gson.JsonSyntaxException;

/**
 * Pool class for managing Javet engines and V8 runtimes.
 * This class handles the initialization, starting, and releasing of runtimes.
 */
public class Pool {
  /**
   * The Javet engine pool for managing V8 runtimes.
   */
  private IJavetEnginePool<V8Runtime> javetEnginePool;
  /**
   * Converter for Javet proxies, used to convert Java objects to JavaScript
   * objects and vice versa.
   */
  private JavetProxyConverter proxyConverter;
  /**
   * The JavaPlugin instance associated with this pool, used for logging and
   * accessing plugin resources.
   */
  private JavaPlugin plugin;
  /**
   * A map to track whether a runtime can be closed for each path.
   * The key is the path to the module directory, and the value is an
   * AtomicBoolean
   * indicating whether the runtime can be closed.
   * This is used to manage the lifecycle of runtimes and ensure they are closed
   * properly when they are no longer needed.
   */
  private HashMap<Path, AtomicBoolean> runtimeCanBeClosed;
  private JSRuntimeType runtimeType;

  /**
   * Constructor for the Pool class.
   * Initializes the Javet engine pool and sets the JavaScript runtime type to
   * Node.js.
   * 
   * @param plugin         The JavaPlugin instance associated with this pool.
   * @param enableNodeI18n A boolean indicating whether to enable Node.js with
   *                       internationalization (i18n) support.
   *                       If true, the Node.js runtime will be configured to use
   *                       the ICU data directory specified in the plugin's data
   *                       folder.
   *                       If false, the Node.js runtime will be configured
   *                       without
   *                       i18n support.
   * @throws JavetException if there is an error initializing the Javet engine
   *                        pool.
   */
  public Pool(JavaPlugin plugin, boolean enableNodeI18n) throws JavetException {
    // Initialize the Javet engine pool and runtimes map
    this.javetEnginePool = new JavetEnginePool<>();

    // Set the JavaScript runtime type to Node.js
    NodeRuntimeOptions nodeRuntimeOptions = new NodeRuntimeOptions();
    nodeRuntimeOptions.setConsoleArguments(new String[] { "--input-type=commonjs" });

    if (enableNodeI18n) {
      this.runtimeType = JSRuntimeType.NodeI18n;
      this.runtimeType.isRuntimeOptionsValid(nodeRuntimeOptions);
    } else {
      this.runtimeType = JSRuntimeType.Node;
      this.runtimeType.isRuntimeOptionsValid(nodeRuntimeOptions);
    }

    this.javetEnginePool.getConfig().setJSRuntimeType(this.runtimeType);

    this.proxyConverter = new JavetProxyConverter();
    this.plugin = plugin;
    this.runtimeCanBeClosed = new HashMap<>();

  }

  /**
   * Initializes a runtime for the given path.
   * If a runtime already exists for the path, it does nothing.
   * Otherwise, it creates a new runtime, sets up the working directory, and
   * prepares the Node.js modules.
   * 
   * @param path The path to the module directory.
   * @throws RuntimeException    if the runtime cannot be initialized.
   * @throws IOException         if there is an error reading the module files.
   * @throws JsonSyntaxException if the package.json file is malformed.
   * @throws JavetException      if there is an error with the Javet engine.
   * @throws InterruptedException if the thread is interrupted while waiting for
   *                              the runtime to be ready.
   */
  public void initRuntime(Path path)
      throws RuntimeException, IOException, JsonSyntaxException, JavetException, InterruptedException {
        

    try (IJavetEngine<V8Runtime> javetEngine = this.javetEnginePool.getEngine()) {
      if (javetEngine == null) {
        throw new RuntimeException("Failed to get Javet engine from pool.");
      }

      // Set the runtime type to Node.js
      javetEngine.getConfig().setJSRuntimeType(this.runtimeType);

      try (V8Runtime runtime = javetEngine.getV8Runtime()) {
        if (runtime == null) {
          throw new RuntimeException("Failed to create V8 runtime.");
        }

        NodeRuntime nodeRuntime = (NodeRuntime) runtime;
        WorkingDirectory workingDirectory = new WorkingDirectory(path);
        JavetJVMInterceptor javetJVMInterceptor = new JavetJVMInterceptor(runtime);
        Globals globals = new Globals(plugin);
        AtomicBoolean scriptIsUp = new AtomicBoolean(false);

        javetEngine.getConfig().setAllowEval(true);
        nodeRuntime.getNodeModule(NodeModuleModule.class).setRequireRootDirectory(path.toFile());
        // nodeRuntime.getNodeModule(NodeModuleProcess.class).setWorkingDirectory(path.toFile());

        runtime.setConverter(proxyConverter);
        javetJVMInterceptor.register(runtime.getGlobalObject());

        runtime.getGlobalObject().set("PaperTS", globals);

        // Set the global objects for the runtime
        runtime.getExecutor("let org = javet.package.org").executeVoid();
        runtime.getExecutor("let java = javet.package.java").executeVoid();

        // Setup required function
        runtime
            .getExecutor(
                """
                    const Module = require("module");
                    const originalRequire = Module.prototype.require;

                    Module.prototype.require = function () {
                      if (arguments.length === 1 && typeof arguments[0] === "string" && (arguments[0].startsWith("org.") || arguments[0].startsWith("java.") || arguments[0].startsWith("net.") || arguments[0].startsWith("com."))) {
                        return javet.package[arguments[0]];
                      }

                      return originalRequire.apply(this, arguments);
                    };
                    """)
            .executeVoid();
        // Handle uncaught exceptions in the runtime
        runtime.getExecutor(
            """
                  process.on("uncaughtException", function (err) {
                      console.error("Uncaught Exception:", err);
                  });
                """)
            .executeVoid();
        // Prevent exports and module from being undefined
        runtime.getExecutor(
            """
                var exports = exports || {};
                """).executeVoid();
        runtime.getExecutor(
            """
                var module = module || {};
                """).executeVoid();

        this.runtimeCanBeClosed.put(path, new AtomicBoolean(false));

        Thread thread = new Thread(() -> {
          try {
            runtime.getExecutor(workingDirectory.getIndexScriptContent()).executeVoid();
            scriptIsUp.set(true);
            plugin.getLogger().info("Script is up and running for path: " + path);
            runtime.await();
          } catch (Exception e) {
            plugin.getLogger().severe("Failed to start runtime for path " + path + ": " + e.getMessage());
            e.printStackTrace();
            scriptIsUp.set(true);
            this.runtimeCanBeClosed.get(path).set(true);
          }
        });

        thread.start();

        // Wait for the script to be up before proceeding
        while (!scriptIsUp.get()) {
          TimeUnit.MILLISECONDS.sleep(1000);
        }

        // Wait for the runtime to be closed
        while (true) {
          TimeUnit.MILLISECONDS.sleep(100);

          if (this.runtimeCanBeClosed.get(path).get()) {
            plugin.getLogger().info("Closing runtime for path: " + path);
            
            globals.unregisterAllCommands();
            globals.unregisterAllEvents();
            runtime.close();
            javetEngine.close();
            runtime.resetContext();
            javetEngine.resetContext();
            this.javetEnginePool.releaseEngine(javetEngine);
            this.runtimeCanBeClosed.remove(path);

            return;
          }
        }
      }
    }
  }

  /**
   * Releases the runtime for the given path.
   * It checks if the runtime exists, and if so, it releases the engine and
   * removes
   * the runtime and engine from the maps.
   * It also performs garbage collection to free up resources.
   * 
   * @param path The path to the module directory.
   * @throws JavetException if there is an error releasing the runtime.
   *                        * @throws InterruptedException if the thread is
   *                        interrupted while waiting for
   *                        the runtime to be ready.
   */
  public void releaseRuntime(Path path) throws JavetException, InterruptedException {
    AtomicBoolean canBeClosed = this.runtimeCanBeClosed.get(path);

    if (canBeClosed != null) {
      canBeClosed.set(true);
    }

    // Wait until the runtime is closed
    while (this.runtimeCanBeClosed.containsKey(path)) {
      TimeUnit.MILLISECONDS.sleep(1000);
    }
  }

  /**
   * Releases all runtimes managed by this pool.
   * This method iterates through all runtimes, releasing each one and clearing
   * the maps.
   * 
   * @throws JavetException if there is an error releasing the runtimes.
   */
  public void releaseAllRuntimes() throws JavetException {
    for (Path path : this.runtimeCanBeClosed.keySet()) {
      AtomicBoolean canBeClosed = this.runtimeCanBeClosed.get(path);

      if (canBeClosed != null) {
        canBeClosed.set(true);
      }
    }
  }

  /**
   * Returns a set of paths for all runtimes managed by this pool.
   * This method is useful for iterating through the runtimes or checking which
   * runtimes are currently active.
   * 
   * @return A set of paths representing the runtimes managed by this pool.
   */
  public Set<Path> getRuntimes() {
    return this.runtimeCanBeClosed.keySet();
  }
}
