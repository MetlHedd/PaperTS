package dev.metlhedd.paperts;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import com.caoccao.javet.exceptions.JavetException;

/**
 * Main class for the PaperTS plugin.
 */
public class PaperTSPlugin extends JavaPlugin implements Listener {
  /**
   * The pool for managing Javet engines and V8 runtimes.
   */
  private Pool pool;

  @Override
  public void onEnable() {
    try {
      this.pool = new Pool(this);

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
        try {
          Path path = file.toPath();
          pool.initRuntime(path);
          pool.startRuntime(path);

          getLogger().info("Module initialized: " + file.getName());
        } catch (Exception e) {
          getLogger().severe("Failed to initialize module at " + file.getName() + ": " + e.getMessage());
          e.printStackTrace();
        }
      }
    }
  }

  public void loadModule(String moduleName) throws JavetException, IOException {
    Path modulePath = getDataFolder().toPath().resolve(moduleName);

    pool.initRuntime(modulePath);
    pool.startRuntime(modulePath);
  }

  public void unloadModule(String moduleName) throws JavetException {
    Path modulePath = getDataFolder().toPath().resolve(moduleName);

    pool.releaseRuntime(modulePath);
  }

  public void reloadAllModules() throws JavetException, IOException {
    pool.releaseAllRuntimes();
    setupModules();
  }

  public void reloadModule(String moduleName) throws JavetException, IOException {
    unloadModule(moduleName);
    loadModule(moduleName);
  }

  public Set<Path> listModules() {
    return pool.listRuntimes();
  }
}
