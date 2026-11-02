package com.intellij.plugins.haxe.runner.debugger.hashlink;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.runner.HaxeRunConfigurationType;
import org.jdom.Element;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("HashLink debugger: run configuration")
public class HashLinkRunConfigurationTest extends HaxeCodeInsightFixtureTestCase {

  @TempDir
  Path folder;

  @Override
  protected String getBasePath() {
    return "";
  }

  @Test
  @DisplayName("the run command line passes the program arguments after the program")
  public void testTheRunCommandLinePassesTheProgramArgumentsAfterTheProgram() throws IOException, ExecutionException {
    Path program = Files.createFile(folder.resolve("app.hl"));
    HashLinkRunConfiguration configuration = configuration();
    configuration.setUseCustomHlBinary(true);
    configuration.setCustomHlBinaryPath(Files.createFile(folder.resolve("hl.exe")).toString());
    configuration.setHlFilePath(program.toString());
    configuration.setProgramArguments("--level 2 \"two words\"");

    GeneralCommandLine commandLine = configuration.createRunCommandLine(null);

    assertEquals(List.of(program.toString(), "--level", "2", "two words"), commandLine.getParametersList().getList());
  }

  @Test
  @DisplayName("the program arguments survive a save and load")
  public void testTheProgramArgumentsSurviveASaveAndLoad() {
    HashLinkRunConfiguration saved = configuration();
    saved.setProgramArguments("--level 2");
    Element element = new Element("configuration");

    saved.writeExternal(element);
    HashLinkRunConfiguration loaded = configuration();
    loaded.readExternal(element);

    assertEquals("--level 2", loaded.getProgramArguments());
  }

  private HashLinkRunConfiguration configuration() {
    HaxeRunConfigurationType type = HaxeRunConfigurationType.getInstance();
    HashLinkRunConfiguration configuration = new HashLinkRunConfiguration("app", getProject(), new HashLinkConfigurationFactory(type));
    configuration.setModule(myFixture.getModule());
    return configuration;
  }
}
