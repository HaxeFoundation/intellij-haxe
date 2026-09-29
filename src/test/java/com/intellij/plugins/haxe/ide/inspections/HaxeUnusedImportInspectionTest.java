/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2016 AS3Boyan
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
package com.intellij.plugins.haxe.ide.inspections;

import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.v2.compiler.settings.HaxeCompilerSettings;

import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.intellij.plugins.haxe.ide.inspections.unused.HaxeUnusedImportInspection;

/**
 * Test for the HaxeUnusedImportInspection.
 * <p/>
 * Created by Usievaład Kimajeŭ on 27.05.2016.
 */
@DisplayName("Inspection: unused import")
public class HaxeUnusedImportInspectionTest extends HaxeLightFixtureTestCase {
  @Test
  @DisplayName("unused alias typedef")
  public void testUnusedAliasTypedef() {
    doTest("UnusedAliasTypedef.hx");
  }

  @Test
  @DisplayName("unused class")
  public void testUnusedClass() {
    doTest("UnusedClass.hx");
  }

  @Test
  @DisplayName("unused interface")
  public void testUnusedInterface() {
    doTest("UnusedInterface.hx");
  }

  @Test
  @DisplayName("unused typedef")
  public void testUnusedTypedef() {
    doTest("UnusedTypedef.hx");
  }

  @Test
  @DisplayName("used alias typedef")
  public void testUsedAliasTypedef() {
    doTest("UsedAliasTypedef.hx");
  }

  @Test
  @DisplayName("used class")
  public void testUsedClass() {
    doTest("UsedClass.hx");
  }

  @Test
  @DisplayName("used interface")
  public void testUsedInterface() {
    doTest("UsedInterface.hx");
  }

  @Test
  @DisplayName("used typedef")
  public void testUsedTypedef() {
    doTest("UsedTypedef.hx");
  }

  @Test
  @DisplayName("mixed use")
  public void testMixedUse() {
    doTest("MixedUse.hx");
  }

  @Override
  protected String getBasePath() {
    return "/imports/unused/";
  }

  /// The compiler-diagnostics unused-import toggle REPLACES this inspection: with both
  /// the master and the feature toggle on, the static analysis must report nothing.
  @Test
  @DisplayName("compiler diagnostics toggle displaces the static inspection")
  public void testCompilerDiagnosticsToggleDisplacesTheStaticInspection() {
    HaxeCompilerSettings settings = HaxeCompilerSettings.getInstance(getProject());
    settings.setCompilerDiagnosticsEnabled(true);
    settings.setDiagnosticsUnusedImportsEnabled(true);
    try {
      myFixture.configureByFiles("UnusedClass.hx", "helper/Bar.hx", "helper/Foo.hx", "helper/IFoo.hx", "helper/Typedefs.hx");
      myFixture.enableInspections(new HaxeUnusedImportInspection());

      boolean unusedImportReported = myFixture.doHighlighting().stream()
        .anyMatch(info -> info.getDescription() != null && info.getDescription().contains("nused import"));
      assertFalse(unusedImportReported, "the static inspection must stand down while the compiler owns unused imports");
    } finally {
      // the shared light project remembers project-level settings
      settings.setCompilerDiagnosticsEnabled(false);
      settings.setDiagnosticsUnusedImportsEnabled(false);
    }
  }

  private void doTest(String fileName) {
    myFixture.configureByFiles(fileName, "helper/Bar.hx", "helper/Foo.hx", "helper/IFoo.hx", "helper/Typedefs.hx");
    myFixture.setTestDataPath(getTestDataPath());
    myFixture.enableInspections(new HaxeUnusedImportInspection());
    myFixture.testHighlighting(true, true, true, myFixture.getFile().getVirtualFile());
  }
}
