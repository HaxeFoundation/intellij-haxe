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
package com.intellij.plugins.haxe.lang.psi;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.openapi.project.Project;
import com.intellij.psi.impl.AnyPsiChangeListener;
import com.intellij.psi.impl.PsiManagerImpl;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.messages.MessageBus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ConcurrentMap;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeClassResolveCache {
  private final ConcurrentMap<HaxeClass, HaxeClassResolveResult> myMap = ContainerUtil.createConcurrentWeakMap();

  public static HaxeClassResolveCache getInstance(Project project) {
    ProgressIndicatorProvider.checkCanceled(); // We hope this method is being called often enough to cancel daemon processes smoothly
    return ServiceManager.getService(project, HaxeClassResolveCache.class);
  }

  public HaxeClassResolveCache(@NotNull MessageBus messageBus) {
    messageBus.connect().subscribe(PsiManagerImpl.ANY_PSI_CHANGE_TOPIC, new AnyPsiChangeListener() {
      @Override
      public void beforePsiChanged(boolean isPhysical) {
        myMap.clear();
      }

      @Override
      public void afterPsiChanged(boolean isPhysical) {
      }
    });
  }

  public void put(@NotNull HaxeClass haxeClass, @NotNull HaxeClassResolveResult result) {
    myMap.put(haxeClass, result);
  }

  @Nullable
  public HaxeClassResolveResult get(HaxeClass haxeClass) {
    return myMap.get(haxeClass);
  }
}
