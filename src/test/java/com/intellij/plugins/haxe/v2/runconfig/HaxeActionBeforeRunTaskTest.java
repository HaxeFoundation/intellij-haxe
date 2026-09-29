package com.intellij.plugins.haxe.v2.runconfig;

import com.intellij.openapi.util.JDOMUtil;
import com.intellij.plugins.haxe.v2.buildtools.HxmlProjects;
import com.intellij.plugins.haxe.v2.runconfig.HaxeActionBeforeRunTaskProvider.Task;
import com.intellij.util.xmlb.XmlSerializer;
import org.jdom.Element;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The task's stored form: run configurations saved before the state-component migration still load. */
@DisplayName("Run configurations: action before run task")
public class HaxeActionBeforeRunTaskTest {

  @Test
  @DisplayName("loads the option list format")
  public void testLoadsTheOptionListFormat() throws Exception {
    Element stored = JDOMUtil.load("""
      <option name="HaxeActionBeforeRun" enabled="true">
        <option name="buildFile" value="/project/build.hxml" />
        <option name="action" value="test" />
        <option name="arguments" value="-v" />
        <option name="injectDebugArguments" value="false" />
        <option name="sectionScoped" value="true" />
      </option>""");

    Task task = loadedTask(stored);

    assertEquals("/project/build.hxml", task.getBuildFilePath());
    assertEquals("test", task.getActionName());
    assertEquals("-v", task.getExtraArguments());
    assertFalse(task.isInjectDebugArguments());
    assertTrue(task.isSectionScoped());
  }

  @Test
  @DisplayName("absent options keep their defaults")
  public void testAbsentOptionsKeepTheirDefaults() throws Exception {
    Element stored = JDOMUtil.load("""
      <option name="HaxeActionBeforeRun" enabled="true">
        <option name="buildFile" value="/project/build.hxml" />
      </option>""");

    Task task = loadedTask(stored);

    assertEquals(HxmlProjects.BUILD_ACTION, task.getActionName());
    assertEquals("", task.getExtraArguments());
    assertTrue(task.isInjectDebugArguments());
    assertFalse(task.isSectionScoped());
  }

  @Test
  @DisplayName("clone edits leave the original untouched")
  public void testCloneEditsLeaveTheOriginalUntouched() {
    Task original = new Task();
    original.setBuildFilePath("/project/build.hxml");

    Task copy = original.clone();
    copy.setBuildFilePath("/project/other.hxml");

    assertEquals("/project/build.hxml", original.getBuildFilePath());
    assertEquals("/project/other.hxml", copy.getBuildFilePath());
  }

  private static Task loadedTask(Element stored) {
    Task task = new Task();
    task.loadState(XmlSerializer.deserialize(stored, Task.State.class));
    return task;
  }
}
