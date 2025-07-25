package dev.metlhedd.paperts;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import com.caoccao.javet.exceptions.JavetException;
import com.caoccao.javet.interop.options.NodeRuntimeOptions;

/**
 * Main class for the PaperTS plugin.
 */
public class PaperTSPlugin extends JavaPlugin implements Listener {
  /**
   * The pool for managing Javet engines and V8 runtimes.
   */
  private Pool pool;

  /**
   * Enables Node.js with internationalization (i18n) support.
   * This method checks if the ICU data directory exists in the plugin's data
   * folder and sets the Node.js flags accordingly.
   * If the directory does not exist, it returns false, indicating that i18n
   * support cannot be enabled.
   * @return true if i18n support is enabled, false otherwise.
   */
  private boolean enableNodeI18n() {
    File icuDataDir = getDataFolder().toPath().resolve("node-icu").toFile();

    if (!icuDataDir.exists()) {
      return false;
    }

    NodeRuntimeOptions.NODE_FLAGS.setIcuDataDir(icuDataDir.getAbsolutePath());

    return true;
  }
  /**
   * Called when the plugin is enabled.
   * This method initializes the pool and sets up modules by scanning the server
   * root folder for directories containing a package.json file.
   * It also registers event listeners and commands.
   */
  @Override
  public void onEnable() {
    try {
      this.pool = new Pool(this, enableNodeI18n());

      setupModules();
    } catch (Exception e) {
      getLogger().severe("Failed to initialize PaperTS: " + e.getMessage());
      e.printStackTrace();
      Bukkit.getPluginManager().disablePlugin(this);

      return;
    }

    Bukkit.getPluginManager().registerEvents(this, this);
    getServer().getCommandMap().register("paperts", new Command("paperts", this));
  }

  /**
   * Called when the plugin is disabled.
   * This method releases all runtimes managed by the pool and performs cleanup.
   */
  @Override
  public void onDisable() {
    try {
      this.pool.releaseAllRuntimes();
    } catch (Exception e) {
      getLogger().severe("Failed to release runtimes: " + e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Sets up modules by scanning the server root folder for directories
   * containing a package.json file.
   * For each module found, it initializes and starts the runtime.
   * This method is called during plugin enable.
   */
  private void setupModules() {
    File serverRootFolder = getDataFolder();

    if (!serverRootFolder.exists()) {
      serverRootFolder.mkdirs();
    }

    // Loop through directories in the data folder, and enable it as a module
    for (File file : serverRootFolder.listFiles()) {
      if (file.isDirectory() && new File(file, "package.json").exists()) {
        new Thread(() -> {
          Path path = file.toPath();

          try {
            this.pool.initRuntime(path);
          } catch (IOException | JavetException | InterruptedException e) {
            getLogger().severe("Failed to initialize module at " + path + ": " + e.getMessage());
            e.printStackTrace();
          }

        }).start();
      }
    }
  }

  /**
   * Loads a module by its name.
   * This method starts a new thread to initialize the runtime for the specified
   * module.
   * @param moduleName The name of the module to load.
   * It constructs the module path from the plugin's data folder and calls the
   * pool's initRuntime method.
   */
  public void loadModule(String moduleName) {
    new Thread(() -> {
      try {
        Path modulePath = getDataFolder().toPath().resolve(moduleName);

        this.pool.initRuntime(modulePath);
      } catch (Exception e) {
        getLogger().severe("Failed to load module " + moduleName + ": " + e.getMessage());
        e.printStackTrace();
      }

    }).start();
  }

  /**
   * Unloads a module by its name.
   * This method releases the runtime for the specified module and removes it from
   * the pool.
   * @param moduleName The name of the module to unload.
   * It constructs the module path from the plugin's data folder and calls the
   * pool's releaseRuntime method.
   * @throws JavetException if there is an error releasing the runtime.
   * @throws InterruptedException if the thread is interrupted while waiting for
   *                              the runtime to be ready.
   */
  public void unloadModule(String moduleName) throws JavetException, InterruptedException {
    Path modulePath = getDataFolder().toPath().resolve(moduleName);

    this.pool.releaseRuntime(modulePath);
  }

  /**
   * Reloads all modules managed by the pool.
   * This method iterates through all modules, unloading and then loading each one
   * to refresh their state.
   * @throws JavetException if there is an error reloading the modules.
   * @throws IOException if there is an error reading the module files.
   * @throws InterruptedException if the thread is interrupted while waiting for
   *                              the runtime to be ready.
   */
  public void reloadAllModules() throws JavetException, IOException, InterruptedException {
    for (Path path : this.listModules()) {
      this.unloadModule(path.getFileName().toString());
      this.loadModule(path.getFileName().toString());
    }
  }

  /**
   * Reloads a specific module by its name.
   * This method first unloads the module and then loads it again to refresh its
   * state.
   * @param moduleName The name of the module to reload.
   * @throws JavetException if there is an error reloading the module.
   * @throws IOException if there is an error reading the module files.
   * @throws InterruptedException if the thread is interrupted while waiting for
   *                              the runtime to be ready.
   */
  public void reloadModule(String moduleName) throws JavetException, IOException, InterruptedException {
    this.unloadModule(moduleName);
    this.loadModule(moduleName);
  }

  /**
   * Lists all modules managed by the pool.
   * This method returns a set of paths representing the modules currently
   * managed by the pool.
   * @return A set of paths representing the modules managed by the pool.
   */
  public Set<Path> listModules() {
    return this.pool.getRuntimes();
  }
}
