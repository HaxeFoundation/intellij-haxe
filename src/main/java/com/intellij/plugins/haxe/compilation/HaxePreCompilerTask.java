/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
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
package com.intellij.plugins.haxe.compilation;

import com.intellij.compiler.options.CompileStepBeforeRun;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileTask;
import com.intellij.openapi.compiler.FileProcessingCompiler;
import com.intellij.plugins.haxe.tests.runner.HaxeTestsConfiguration;

/**
 * Pre-compile task that builds Haxe modules inside the IDE process.
 *
 * Regular builds are handled by the JPS builder (HaxeModuleLevelBuilder in the jps-plugin
 * module); this task only remains for test-runner configurations, which need a special
 * build (neko target with the test-runner class as -main) that the JPS builder does not
 * know how to produce.
 *
 * Created by as3boyan on 03.08.14.
 */
public class HaxePreCompilerTask implements CompileTask {

  static HaxeCompiler haxeCompiler;

  @Override
  public boolean execute(CompileContext context) {
    final RunConfiguration runConfiguration = CompileStepBeforeRun.getRunConfiguration(context.getCompileScope());
    if (!(runConfiguration instanceof HaxeTestsConfiguration)) {
      // everything else is compiled by the JPS builder
      return true;
    }

    if (haxeCompiler == null) {
      haxeCompiler = new HaxeCompiler();
    }

    FileProcessingCompiler.ProcessingItem[] processingItems = haxeCompiler.getProcessingItems(context);
    haxeCompiler.process(context, processingItems);
    return true;
  }
}
