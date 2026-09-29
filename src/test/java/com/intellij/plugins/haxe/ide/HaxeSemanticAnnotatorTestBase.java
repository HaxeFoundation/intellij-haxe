/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2018 Ilya Malanin
 * Copyright 2018-2020 Eric Bishton
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

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.InspectionProfileEntry;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.plugins.haxe.HaxeToolkitLightFixtureTestCase;
import com.intellij.util.ArrayUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.*;


public abstract class HaxeSemanticAnnotatorTestBase extends HaxeToolkitLightFixtureTestCase {


  @Override
  protected abstract String getBasePath();

  private void doTest(boolean checkWarnings, boolean checkInfos, boolean checkWeakWarnings,
                      @Nullable Set<Class<? extends LocalInspectionTool>> unsetInspections,
                      String... additionalFiles)
    throws Exception {
    doTest(checkWarnings, checkInfos, checkWeakWarnings, unsetInspections, new InspectionProfileEntry[0], additionalFiles);
  }

  /**
   * {@code extraTools} are pre-configured inspection INSTANCES (e.g. with an
   * option flipped); list their classes in {@code unsetInspections} so the
   * default instance does not register alongside.
   */
  private void doTest(boolean checkWarnings, boolean checkInfos, boolean checkWeakWarnings,
                      @Nullable Set<Class<? extends LocalInspectionTool>> unsetInspections,
                      InspectionProfileEntry[] extraTools,
                      String... additionalFiles)
    throws Exception {
    myFixture.configureByFiles(ArrayUtil.mergeArrays(new String[]{getTestName(false) + ".hx"}, additionalFiles));
    myFixture.enableInspections(getAnnotatorBasedInspection());
    myFixture.enableInspections(HaxeInspectionTestTools.semanticInspections(unsetInspections));
    if (extraTools.length > 0) {
      myFixture.enableInspections(extraTools);
    }
    myFixture.testHighlighting(checkWarnings, checkInfos, checkWeakWarnings);
  }

  protected void doTestSkippingInspections(Set<Class<? extends LocalInspectionTool>> unsetInspections) throws Exception {
    doTest(true, false, false, unsetInspections);
  }

  protected void doTestReplacingInspection(InspectionProfileEntry configuredTool) throws Exception {
    Set<Class<? extends LocalInspectionTool>> replacedClass = Set.of(configuredTool.getClass().asSubclass(LocalInspectionTool.class));
    doTest(true, false, false, replacedClass, new InspectionProfileEntry[]{configuredTool});
  }

  protected void doTestNoFixWithWarnings(String... additionalFiles) throws Exception {
    doTest(true, false, false, null, additionalFiles);
  }
  protected void doTestNoFixWithWeakWarnings(String... additionalFiles) throws Exception {
    doTest(true, false, true, null, additionalFiles);
  }

  protected void doTestNoFixWithoutWarnings(String... additionalFiles) throws Exception {
    doTest(false, false, false, null, additionalFiles);
  }

  protected void doTestActions(String... filters) throws Exception {
    doTest(false, false, false, null);

    List<IntentionAction> intentions = myFixture.getAvailableIntentions();
    for (final IntentionAction action : intentions) {
      if (Arrays.asList(filters).contains(action.getText())) {
        System.out.println("Applying intent " + action.getText());
        myFixture.launchAction(action);
      }
      else {
        System.out.println("Ignoring intent " + action.getText() + ", not matching " + StringUtils.join(filters, ","));
      }
    }
    FileDocumentManager.getInstance().saveAllDocuments();
    myFixture.checkResultByFile(getTestName(false) + "_expected.hx");
  }

}
