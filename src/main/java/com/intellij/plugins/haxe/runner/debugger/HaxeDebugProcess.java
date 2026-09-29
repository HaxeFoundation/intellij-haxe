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
package com.intellij.plugins.haxe.runner.debugger;

import com.intellij.lang.javascript.flex.debug.FlexDebugProcess;
import com.intellij.lang.javascript.flex.projectStructure.model.FlexBuildConfiguration;
import com.intellij.lang.javascript.flex.run.BCBasedRunnerParameters;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.runner.debugger.dap.ide.DapSourceScopes;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeDebugProcess extends FlexDebugProcess {

  // the build's classpath roots, scoping name-based breakpoint markers (see resolveFileReference)
  private final List<String> sourceDirectories;

  public HaxeDebugProcess(final XDebugSession session,
                          final FlexBuildConfiguration bc,
                          final BCBasedRunnerParameters params,
                          @NotNull final List<String> sourceDirectories) throws IOException {
    super(session, bc, params);
    this.sourceDirectories = sourceDirectories;
  }

  @NotNull
  @Override
  public XDebuggerEditorsProvider getEditorsProvider() {
    return new HaxeDebuggerEditorsProvider();
  }

  /// fdb matches name-based breakpoint markers (`break Main.hx:6`) against
  /// ANY file with that name in the swf, and flex's out-of-scope breakpoint
  /// filter fails open for Haxe files (its ActionScript resolver cannot see
  /// them) — so a breakpoint in an unrelated build's Main.hx would land in the
  /// debugged build's Main.hx. For files outside the debugged BUILD's
  /// classpaths (the module as the fallback scope - too coarse when one
  /// module holds several builds) the marker becomes the absolute path
  /// instead: it only matches when fdb truly knows that file, and is
  /// reported "not set" (breakpoint shown invalid for this session)
  /// otherwise — the correct outcome for foreign breakpoints.
  @Override
  protected String resolveFileReference(VirtualFile file) {
    String reference = super.resolveFileReference(file);
    if (reference.startsWith("#")) {
      // a real fdb file id - precise, nothing to guard
      return reference;
    }
    if (!sourceDirectories.isEmpty()) {
      return DapSourceScopes.underAny(file.getPath(), sourceDirectories) ? reference : file.getPath();
    }
    Module module = getModule();
    boolean inScope = module == null || ReadAction.computeBlocking(
      () -> module.getModuleWithDependenciesAndLibrariesScope(false).contains(file));
    return inScope ? reference : file.getPath();
  }
}
