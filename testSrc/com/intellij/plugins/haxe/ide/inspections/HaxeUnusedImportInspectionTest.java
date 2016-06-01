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

import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;

/**
 * Test for the HaxeUnusedImportInspection.
 * <p/>
 * Created by Usievaład Kimajeŭ on 27.05.2016.
 */
public class HaxeUnusedImportInspectionTest extends HaxeCodeInsightFixtureTestCase {
  public void testUnusedAliasTypedef() {
    doTest("UnusedAliasTypedef.hx");
  }

  public void testUnusedClass() {
    doTest("UnusedClass.hx");
  }

  public void testUnusedInterface() {
    doTest("UnusedInterface.hx");
  }

  public void testUnusedTypedef() {
    doTest("UnusedTypedef.hx");
  }

  public void testUsedAliasTypedef() {
    doTest("UsedAliasTypedef.hx");
  }

  public void testUsedClass() {
    doTest("UsedClass.hx");
  }

  public void testUsedInterface() {
    doTest("UsedInterface.hx");
  }

  public void testUsedTypedef() {
    doTest("UsedTypedef.hx");
  }

  @Override
  protected String getBasePath() {
    return "/imports/unused/";
  }

  private void doTest(String fileName) {
    myFixture.configureByFiles(fileName, "helper/Bar.hx", "helper/Foo.hx", "helper/IFoo.hx", "helper/Typedefs.hx");
    myFixture.setTestDataPath(getTestDataPath());
    myFixture.enableInspections(new HaxeUnusedImportInspection());
    myFixture.testHighlighting(true, true, true, myFixture.getFile().getVirtualFile());
  }
}
