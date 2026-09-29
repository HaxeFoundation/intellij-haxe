package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.plugins.haxe.v2.buildtools.server.HaxeCompilationServerManager;
import com.intellij.plugins.haxe.v2.buildtools.info.HaxeLimeProjectInfoService;
import com.intellij.plugins.haxe.v2.buildtools.info.HaxeNmeProjectInfoService;
import com.intellij.ide.trustedProjects.TrustedProjects;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFile;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The trust gates in front of project-code execution. Headless runs
 * auto-trust every project, but an EXPLICIT distrust wins over that
 * default — which is exactly what these tests set (and restore).
 */
@DisplayName("Build tools: project trust gating")
public class HaxeProjectTrustTest extends HaxeCodeInsightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return "/testing/runner/";
  }

  @Test
  @DisplayName("headless projects are trusted by default")
  public void testHeadlessProjectsAreTrustedByDefault() {
    assertTrue(HaxeProjectTrust.isTrusted(getProject()),
               "unit-test projects auto-trust; the suite must not trip the gates");
  }

  @Test
  @DisplayName("untrusted project blocks background evaluation")
  public void testUntrustedProjectBlocksBackgroundEvaluation() {
    withDistrustedProject(() -> {
      assertFalse(HaxeProjectTrust.isTrusted(getProject()));
      assertFalse(HaxeProjectTrust.checkForBackgroundEvaluation(getProject()));
    });
  }

  @Test
  @DisplayName("untrusted project refuses the compilation server")
  public void testUntrustedProjectRefusesTheCompilationServer() {
    withDistrustedProject(() -> {
      int port = HaxeCompilationServerManager.getInstance(getProject()).ensureRunning(null);
      assertEquals(-1, port, "an untrusted project must not start a compilation server");
    });
  }

  @Test
  @DisplayName("untrusted project skips lime and nme evaluation")
  public void testUntrustedProjectSkipsLimeAndNmeEvaluation() {
    Project project = getProject();
    VirtualFile projectXml = myFixture.addFileToProject("project.xml", "<project/>").getVirtualFile();
    HaxeBuildFile buildFile = new HaxeBuildFile(projectXml, HaxeBuildFileType.LIME);

    withDistrustedProject(() -> {
      var lime = HaxeLimeProjectInfoService.getInstance(project)
        .getCachedOrSchedule(buildFile, "html5", null, () -> { });
      assertNull(lime, "lime evaluation executes project code and must not be scheduled");

      var nme = HaxeNmeProjectInfoService.getInstance(project)
        .getCachedOrSchedule(buildFile, "neko", null, () -> { });
      assertNull(nme, "nme prepare executes project code and must not be scheduled");
    });
  }

  /** Runs the body with the project explicitly distrusted, restoring trust after. */
  private void withDistrustedProject(Runnable body) {
    TrustedProjects.setProjectTrusted(getProject(), false);
    try {
      body.run();
    }
    finally {
      TrustedProjects.setProjectTrusted(getProject(), true);
    }
  }
}
