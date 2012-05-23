package com.intellij.plugins.haxe.ide;

import com.intellij.ide.util.importProject.RootDetectionProcessor;
import com.intellij.ide.util.projectWizard.importSources.DetectedProjectRoot;
import com.intellij.ide.util.projectWizard.importSources.ProjectStructureDetector;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.plugins.haxe.ide.projectStructure.detection.HaxeProjectStructureDetector;
import com.intellij.testFramework.PlatformTestCase;
import gnu.trove.THashSet;

import java.io.File;
import java.util.List;
import java.util.Set;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeSourceRootDetectionTest extends PlatformTestCase {
  private void doTest(String... expected) {
    final String dirPath =
      PathManager.getHomePath() + FileUtil.toSystemDependentName("/plugins/haxe/testData/rootDetection/") + getTestName(true);
    final File dir = new File(dirPath);
    assertTrue(dir.isDirectory());
    final HaxeProjectStructureDetector haxeProjectStructureDetector = new HaxeProjectStructureDetector();
    final RootDetectionProcessor detectionProcessor = new RootDetectionProcessor(
      dir, new ProjectStructureDetector[]{haxeProjectStructureDetector}
    );
    final List<DetectedProjectRoot> detected = detectionProcessor.findRoots().get(haxeProjectStructureDetector);
    assertNotNull(detected);
    final Set<String> actual = new THashSet<String>();
    for (DetectedProjectRoot projectRoot : detected) {
      final String relativePath = FileUtil.getRelativePath(dir, projectRoot.getDirectory());
      assertNotNull(relativePath);
      actual.add(FileUtil.toSystemIndependentName(relativePath));
    }
    assertSameElements(actual, expected);
  }

  public void testSimple() throws Throwable {
    doTest("src");
  }

  public void testModules() throws Throwable {
    doTest("src", "module1/src", "module2/src");
  }
}
