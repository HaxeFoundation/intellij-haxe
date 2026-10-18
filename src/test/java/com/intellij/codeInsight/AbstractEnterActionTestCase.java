/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2023 AS3Boyan
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
//
// Lifted this file from IDEA 2016.2 for backward-compatibility with IDEA v14, v14.1.
//
package com.intellij.codeInsight;

import com.intellij.plugins.haxe.HaxeLightCodeInsightTestBase;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import org.jetbrains.annotations.NonNls;

public abstract class AbstractEnterActionTestCase extends HaxeLightCodeInsightTestBase {

  @Override
  protected void tearDown() throws Exception {
    CodeStyleSettingsManager.getInstance(getProject()).dropTemporarySettings();
    super.tearDown();
  }

  protected void doTextTest(@NonNls String ext, @NonNls String before, @NonNls String after) {
    configureFromFileText("a." + ext, before);
    performAction();
    checkResultByText(null, after, false);
  }

  protected void performAction() {
    type('\n');
  }
}
