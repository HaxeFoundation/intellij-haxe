/*
 * Copyright 2017 Eric Bishton
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
package com.intellij.plugins.haxe.util;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.module.Module;
import com.intellij.plugins.haxe.config.sdk.HaxeSdkAdditionalDataBase;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A GeneralCommandLine replacement that maps the SDK paths into the environment.
 *
 */
public class HaxeCommandLine extends GeneralCommandLine {

  public HaxeCommandLine(@NotNull Module module) {
    super();
    addEnvironmentVars();
    patchPath(module);
  }

  public HaxeCommandLine(@NotNull Module module, @NotNull String... command) {
    super(command);
    addEnvironmentVars();
    patchPath(module);
  }

  public HaxeCommandLine(@NotNull Module module, @NotNull List<String> command) {
    super(command);
    addEnvironmentVars();
    patchPath(module);
  }

  /**
   * Adds environment variables as they seem to not be included by default
   */
  private void addEnvironmentVars() {
    getEnvironment().putAll(System.getenv());
  }

  /**
   * Patches the path with SDK path values so that commands (like lime) that
   * require haxelib and neko will have those directories mapped in.
   */
  public void patchPath(Module module) {
    HaxeSdkAdditionalDataBase sdk = HaxeSdkUtilBase.getSdkData(module);
    HaxeSdkUtilBase.patchEnvironment(getEnvironment(), sdk);
  }
}
