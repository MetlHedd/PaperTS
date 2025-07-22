package dev.metlhedd.paperts;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Set;

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
   * Maps paths to their corresponding Javet engines.
   */
  private HashMap<Path, IJavetEngine<V8Runtime>> javetEngine;
  /**
   * Maps paths to their corresponding V8 runtimes.
   */
  private HashMap<Path, V8Runtime> runtimes;
  /**
   * Maps paths to their corresponding working directories.
   */
  private HashMap<Path, WorkingDirectory> workingDirectories;
  private HashMap<Path, JavetJVMInterceptor> javetJVMInterceptors;
  private HashMap<Path, Globals> globalsMap;
  /**
   * Converter for Javet proxies, used to convert Java objects to JavaScript
   * objects and vice versa.
   */
  private JavetProxyConverter proxyConverter;
  private JavaPlugin plugin;

  /**
   * Constructor for the Pool class.
   * Initializes the Javet engine pool and sets the JavaScript runtime type to
   * Node.js.
   * 
   * @throws JavetException if there is an error initializing the Javet engine
   *                        pool.
   */
  public Pool(JavaPlugin plugin) throws JavetException {
    // Initialize the Javet engine pool and runtimes map
    this.javetEnginePool = new JavetEnginePool<>();

    // Set the JavaScript runtime type to Node.js
    this.javetEnginePool.getConfig().setJSRuntimeType(JSRuntimeType.Node);

    // Initialize the maps for storing runtimes and Javet engines
    this.javetEngine = new HashMap<>();
    this.runtimes = new HashMap<>();
    this.workingDirectories = new HashMap<>();
    this.javetJVMInterceptors = new HashMap<>();
    this.globalsMap = new HashMap<>();
    this.proxyConverter = new JavetProxyConverter();
    this.plugin = plugin;
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
   */
  public void initRuntime(Path path) throws RuntimeException, IOException, JsonSyntaxException, JavetException {
    // Check if the runtime already exists
    if (this.runtimes.containsKey(path)) {
      return;
    }

    // Create a new runtime if it doesn't exist
    IJavetEngine<V8Runtime> javetEngine = this.javetEnginePool.getEngine();
    V8Runtime runtime = javetEngine.getV8Runtime();
    NodeRuntime nodeRuntime = (NodeRuntime) runtime;
    WorkingDirectory workingDirectory = new WorkingDirectory(path);
    JavetJVMInterceptor javetJVMInterceptor = new JavetJVMInterceptor(runtime);
    Globals globals = new Globals(plugin);

    javetEngine.getConfig().setAllowEval(true);
    nodeRuntime.getNodeModule(NodeModuleModule.class).setRequireRootDirectory(path.toFile());
    // nodeRuntime.getNodeModule(NodeModuleProcess.class).setWorkingDirectory(path.toFile());

    runtime.setConverter(proxyConverter);
    javetJVMInterceptor.register(runtime.getGlobalObject());

    runtime.getGlobalObject().set("PaperTS", globals);

    // Set the global objects for the runtime
    runtime.getExecutor("let org = javet.package.org").executeVoid();
    runtime.getExecutor("let java = javet.package.java").executeVoid();

    this.runtimes.put(path, runtime);
    this.javetEngine.put(path, javetEngine);
    this.javetJVMInterceptors.put(path, javetJVMInterceptor);
    this.workingDirectories.put(path, workingDirectory);
    this.globalsMap.put(path, globals);

    // Setup required function
    runtime
        .getExecutor(
            """
                const Module = require("module");
                const originalRequire = Module.prototype.require;

                Module.prototype.require = function () {
                  if (arguments.length === 1 && typeof arguments[0] === "string" && (arguments[0].startsWith("org") || arguments[0].startsWith("java"))) {
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
   */
  public void releaseRuntime(Path path) throws JavetException {
    // Check if the runtime exists before releasing it
    if (this.runtimes.containsKey(path)) {
      V8Runtime runtime = this.runtimes.get(path);
      NodeRuntime nodeRuntime = (NodeRuntime) runtime;
      JavetJVMInterceptor javetJVMInterceptor = this.javetJVMInterceptors.get(path);
      Globals globals = this.globalsMap.get(path);
      IJavetEngine<V8Runtime> javetEngine = this.javetEngine.get(path);

      // Release and await its completion
      nodeRuntime.terminateExecution();
      nodeRuntime.setStopping(true);
      nodeRuntime.close();
      javetEngine.close();
      globals.unregisterAllEvents();
      globals.unregisterAllCommands();
      javetJVMInterceptor.unregister(runtime.getGlobalObject());
      javetEngine.resetContext();
      javetEnginePool.releaseEngine(javetEngine);

      // Remove the runtime and engine from the maps
      this.runtimes.remove(path);
      this.javetEngine.remove(path);
      this.javetJVMInterceptors.remove(path);
      this.workingDirectories.remove(path);
      this.globalsMap.remove(path);

      System.gc();
    }
  }

  /**
   * Starts the runtime for the given path.
   * This method executes the index script in the runtime, which is expected to be
   * present in the working directory.
   * If the runtime does not exist for the given path, it throws a
   * RuntimeException.
   * 
   * @param path The path to the module directory.
   * @throws RuntimeException if the runtime is not found for the given path.
   * @throws JavetException   if there is an error starting the runtime.
   * @throws IOException      if there is an error reading the index script.
   */
  public void startRuntime(Path path) throws RuntimeException, JavetException, IOException {
    if (!this.runtimes.containsKey(path)) {
      throw new RuntimeException("Runtime not found: " + path);
    }

    try {
      V8Runtime runtime = this.runtimes.get(path);
      WorkingDirectory workingDirectory = this.workingDirectories.get(path);

      // Execute the index script in the runtime
      runtime.getExecutor(workingDirectory.getIndexScriptContent())
          .setResourceName(workingDirectory.getIndexScriptPath().toAbsolutePath().toString()).setModule(true)
          .executeVoid();
      runtime.await();
    } catch (Exception e) {
      this.plugin.getLogger().severe("Failed to start runtime for path " + path + ": " + e.getMessage());
      e.printStackTrace();
    }

  }

  /**
   * Releases all runtimes managed by this pool.
   * This method iterates through all runtimes, releasing each one and clearing
   * the maps.
   * It also closes the Javet engine pool and clears the Javet engine.
   * After releasing all resources, it performs garbage collection to free up
   * memory.
   * This is typically called when the plugin is disabled or when the application
   * is shutting down.
   * 
   * @throws JavetException if there is an error releasing the runtimes.
   */
  public void releaseAllRuntimes() throws JavetException {
    for (Path path : this.runtimes.keySet()) {
      releaseRuntime(path);
    }

    System.gc();
  }

  public Set<Path> listRuntimes() {
    return this.runtimes.keySet();
  }
}
