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

import com.intellij.lang.javascript.flex.projectStructure.model.Dependencies;
import com.intellij.lang.javascript.flex.projectStructure.model.DependencyEntry;
import com.intellij.lang.javascript.flex.projectStructure.model.SdkEntry;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by todd on 7/10/13.
 */

public class FakeFlexDependencies implements Dependencies {

  private static final Logger LOG = Logger.getInstance("#com.intellij.plugins.haxe.config.sdk.HaxeSdkUtil");
  private static final DependencyEntry[] NO_DEPENDENCIES = {};

  @Nullable
  public SdkEntry getSdkEntry() {
    LOG.info("FakeFlexDependencies::getSdkEntry");
    return null;
  }

  public DependencyEntry[] getEntries() {
    LOG.info("FakeFlexDependencies::getEntries");
    return NO_DEPENDENCIES;
  }

  @NotNull
  public com.intellij.flex.model.bc.LinkageType getFrameworkLinkage()
  {
    LOG.info("FakeFlexDependencies::getFrameworkLinkage");
    return null;
  }

  @NotNull
  public java.lang.String getTargetPlayer() {
    LOG.info("FakeFlexDependencies::getTargetPlayer");
    return null;
  }

  @NotNull
  public com.intellij.flex.model.bc.ComponentSet getComponentSet()
  {
    LOG.info("FakeFlexDependencies::getComponentSet");
    return null;
  }

}
