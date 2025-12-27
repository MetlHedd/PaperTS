package dev.metlhedd.paperts;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

/**
 * Represents a working directory for a module.
 * This class is responsible for managing the path and index script of the
 * module.
 */
public class WorkingDirectory {
  /**
   * The path to the working directory.
   */
  private Path path;
  /**
   * The index script of the module, typically defined in package.json.
   */
  private String indexScript;
  /**
   * The run type of the module, defined in package.json.
   */
  private RunType runType;

  /**
   * The name of the package file, typically package.json.
   */
  private final String packageFileName = "package.json";

  /**
   * Constructor for the WorkingDirectory class.
   * Initializes the working directory with the given path and sets the index
   * script
   * from the package file.
   *
   * @param path The path to the working directory.
   * @throws RuntimeException    if the package file is not found or if there is
   *                             an
   *                             error reading it.
   * @throws IOException         if there is an error reading the package file.
   * @throws JsonSyntaxException if the package file is not a valid JSON.
   */
  public WorkingDirectory(Path path) throws RuntimeException, IOException, JsonSyntaxException {
    this.path = path;

    this.setupFromPackageFile();
  }

  /**
   * Sets up the working directory from the package file.
   * 
   * @throws IOException         if there is an error reading the package file.
   * @throws JsonSyntaxException if the package file is not a valid JSON.
   */
  private void setupFromPackageFile() throws IOException, JsonSyntaxException {
    File packageFile = path.resolve(packageFileName).toFile();

    if (!packageFile.exists()) {
      throw new RuntimeException("Package file not found at " + packageFile.getAbsolutePath());
    }

    String fileContent = Files.readString(path.resolve(packageFileName));
    var jsonObject = JsonParser.parseString(fileContent).getAsJsonObject();

    if (jsonObject.has("main")) {
      indexScript = jsonObject.get("main").getAsString();
    } else {
      throw new RuntimeException("No 'main' field found in package.json");
    }

    if (jsonObject.has("runType")) {
      String runTypeString = jsonObject.get("runType").getAsString();

      try {
        this.runType = RunType.valueOf(runTypeString);
      } catch (IllegalArgumentException e) {
        throw new RuntimeException("Invalid 'runType' value in package.json: " + runTypeString);
      }
    } else {
      this.runType = RunType.Synchronous; // Default run type
    }
  }

  /**
   * Gets the index script content.
   * This method reads the index script file and returns its content as a string.
   *
   * @return The content of the index script.
   * @throws IOException if there is an error reading the index script file.
   */
  public String getIndexScriptContent() throws IOException {
    String fileContent = Files.readString(path.resolve(indexScript));

    return fileContent;
  }

  /**
   * Gets the path of the index script.
   * 
   * @return The path of the index script.
   */
  public Path getIndexScriptPath() {
    return path.resolve(indexScript);
  }

  /**
   * Gets the path of the working directory.
   * 
   * @return The path of the working directory.
   */
  public Path getPath() {
    return path;
  }

  /**
   * Gets the run type of the module.
   * 
   * @return The run type of the module.
   */
  public RunType getRunType() {
    return runType;
  }
}
