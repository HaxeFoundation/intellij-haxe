/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.plugins.haxe.ide;

import com.intellij.ide.util.importProject.RootDetectionProcessor;
import com.intellij.ide.util.projectWizard.importSources.DetectedProjectRoot;
import com.intellij.ide.util.projectWizard.importSources.ProjectStructureDetector;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.plugins.haxe.ide.projectStructure.detection.HaxeProjectStructureDetector;
import com.intellij.plugins.haxe.util.HaxeTestUtils;
import com.intellij.testFramework.PlatformTestCase;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeSourceRootDetectionTest extends PlatformTestCase {

  @Override
  protected void tearDown() throws Exception {
    HaxeTestUtils.cleanupUnexpiredAppleUITimers(this::addSuppressedException);
    super.tearDown();
  }


  private void doTest(String... expected) {
    final String dirPath = FileUtil.toSystemDependentName(HaxeTestUtils.BASE_TEST_DATA_PATH + "/rootDetection/") + getTestName(true);
    final File dir = new File(dirPath);
    assertTrue(dir.isDirectory());
    final HaxeProjectStructureDetector haxeProjectStructureDetector = new HaxeProjectStructureDetector();
    final ProjectStructureDetector[] detector = new ProjectStructureDetector[]{haxeProjectStructureDetector};
    final RootDetectionProcessor detectionProcessor = new RootDetectionProcessor(
      dir, detector
    );
    // TODO:
    final List<DetectedProjectRoot> detected;//= detectionProcessor.findRoots().get(haxeProjectStructureDetector);
    Map<ProjectStructureDetector, List<DetectedProjectRoot>> detectorListMap = detectionProcessor.runDetectors();
    detected = detectorListMap.get(haxeProjectStructureDetector);
    assertNotNull(detected);
    final Set<String> actual = new THashSet<String>();
    for (DetectedProjectRoot projectRoot : detected) {
      final String relativePath = FileUtil.getRelativePath(dir, projectRoot.getDirectory());
      assertNotNull(relativePath);
      actual.add(FileUtil.toSystemIndependentName(relativePath));
    }
    assertSameElements(actual, expected);
  }

  @Test
  public void testSimple() throws Throwable {
    doTest("src");
  }

  @Test
  public void testModules() throws Throwable {
    doTest("src", "module1/src", "module2/src");
  }
}
