package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.plugins.haxe.util.HaxeSdkUtilBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Build tools: tool environment")
public class HaxeToolEnvironmentTest extends HaxeRegisteredSdkTestBase {

  @Test
  @DisplayName("the SDK folders go in front of the effective PATH")
  public void testTheSdkFoldersGoInFrontOfTheEffectivePath() throws IOException {
    Path haxelib = touch(temporaryDirectory("haxelib-home").resolve(HaxeSdkUtilBase.getExecutableName("haxelib")));
    Sdk sdk = registerSdk("env-sdk", data -> data.setHaxelibPath(haxelib.toString()));
    String before = pathOf(new GeneralCommandLine("haxe"));

    String path = pathOf(HaxeToolEnvironment.withSdkOnPath(new GeneralCommandLine("haxe"), sdk));

    assertTrue(path.endsWith(File.pathSeparator + before), "the existing PATH stays, got: " + path);
    List<String> expected = List.of(Path.of(sdk.getHomePath()).toString(), haxelib.getParent().toString());
    assertEquals(expected, foldersInFront(path, before));
  }

  @Test
  @DisplayName("a folder holding several SDK tools is listed once")
  public void testAFolderHoldingSeveralSdkToolsIsListedOnce() throws IOException {
    Sdk sdk = registerSdk("shared-folder-sdk", data -> { });
    touch(Path.of(sdk.getHomePath()).resolve(HaxeSdkUtilBase.getExecutableName("haxelib")));
    String before = pathOf(new GeneralCommandLine("haxe"));

    String path = pathOf(HaxeToolEnvironment.withSdkOnPath(new GeneralCommandLine("haxe"), sdk));

    assertEquals(List.of(Path.of(sdk.getHomePath()).toString()), foldersInFront(path, before));
  }

  @Test
  @DisplayName("without an SDK the environment stays as it is")
  public void testWithoutAnSdkTheEnvironmentStaysAsItIs() {
    GeneralCommandLine commandLine = new GeneralCommandLine("haxe");
    String before = pathOf(commandLine);

    assertEquals(before, pathOf(HaxeToolEnvironment.withSdkOnPath(commandLine, null)));
  }

  private static String pathOf(GeneralCommandLine commandLine) {
    return commandLine.getEffectiveEnvironment().get("PATH");
  }

  /** The entries the patched PATH carries before the original one. */
  private static List<String> foldersInFront(String path, String before) {
    String prefix = path.substring(0, path.length() - before.length() - File.pathSeparator.length());
    return Arrays.asList(prefix.split(Pattern.quote(File.pathSeparator)));
  }
}
