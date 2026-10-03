package com.intellij.plugins.haxe.config.sdk.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.intellij.plugins.haxe.util.HaxeSdkUtilBase;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

/**
 * The decisions behind the inline path warnings of the runtime/tool path
 * fields. Entries name a fixture under the temp folder; null stands for an
 * empty field.
 */
@DisplayName("Build tools: haxe runtime settings controls")
public class HaxeRuntimeSettingsControlsTest {
  private static final String HL_EXECUTABLE_NAME = HaxeSdkUtilBase.getExecutableName("hl");
  private static final String TOOL_FILE_NAME = "tool.bin";
  private static final String HL_DIRECTORY_NAME = "hldir";
  private static final String EMPTY_DIRECTORY_NAME = "emptydir";
  private static final String MISSING_NAME = "missing";

  @TempDir
  Path temp;

  @BeforeEach
  public void createFixtures() throws IOException {
    Files.createFile(temp.resolve(TOOL_FILE_NAME));
    Path hlDirectory = Files.createDirectories(temp.resolve(HL_DIRECTORY_NAME));
    Files.createFile(hlDirectory.resolve(HL_EXECUTABLE_NAME));
    Files.createDirectories(temp.resolve(EMPTY_DIRECTORY_NAME));
  }

  /** (fixture entry, executable name, expected warning with %s for the field text, or null). */
  static final List<Arguments> RUNNABLE_PATHS = List.of(
    arguments(TOOL_FILE_NAME, "hl", null),
    arguments(HL_DIRECTORY_NAME, "hl", null),
    arguments(EMPTY_DIRECTORY_NAME, "hl", "%s does not contain " + HL_EXECUTABLE_NAME),
    // no executable name (the Flash player): any folder may be an app bundle
    arguments(EMPTY_DIRECTORY_NAME, null, null),
    arguments(MISSING_NAME, "hl", "Not found: %s"),
    arguments(MISSING_NAME, null, "Not found: %s"),
    arguments(null, "hl", null));

  @ParameterizedTest(name = "{0} / {1}")
  @FieldSource("RUNNABLE_PATHS")
  @DisplayName("reports runnable path problems")
  public void reportsRunnablePathProblems(String entry, String executableName, String expectedTemplate) {
    String text = textFor(entry);

    String problem = HaxeRuntimeSettingsControls.runnablePathProblem(text, executableName);

    assertEquals(expectedFor(expectedTemplate, text), problem);
  }

  /** (fixture entry, expected warning with %s for the field text, or null). */
  static final List<Arguments> FILE_PATHS = List.of(
    arguments(TOOL_FILE_NAME, null),
    arguments(HL_DIRECTORY_NAME, "Not a file: %s"),
    arguments(MISSING_NAME, "Not found: %s"),
    arguments(null, null));

  @ParameterizedTest(name = "{0}")
  @FieldSource("FILE_PATHS")
  @DisplayName("reports file path problems")
  public void reportsFilePathProblems(String entry, String expectedTemplate) {
    String text = textFor(entry);

    String problem = HaxeRuntimeSettingsControls.filePathProblem(text);

    assertEquals(expectedFor(expectedTemplate, text), problem);
  }

  private String textFor(String entry) {
    return entry == null ? "" : temp.resolve(entry).toString();
  }

  private static String expectedFor(String template, String text) {
    return template == null ? null : template.formatted(text);
  }
}
