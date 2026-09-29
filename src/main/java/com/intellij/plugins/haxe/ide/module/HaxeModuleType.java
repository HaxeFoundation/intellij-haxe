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
package com.intellij.plugins.haxe.ide.module;

import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.ModuleTypeManager;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.wizard.HaxeModuleBuilder;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * The legacy HAXE_MODULE type, kept registered so old projects still open.
 * Creating a module through it yields a v2 PLAIN-module setup (the v2 wizard
 * builder) - the v1 builder and its settings surface are gone.
 */
public class HaxeModuleType extends ModuleType<HaxeModuleBuilder> {
  private static final String MODULE_TYPE_ID = "HAXE_MODULE";

  public HaxeModuleType() {
    super(MODULE_TYPE_ID);
  }

  public static HaxeModuleType getInstance() {
    return (HaxeModuleType)ModuleTypeManager.getInstance().findByID(MODULE_TYPE_ID);
  }

  @Override
  public @NotNull String getName() {
    return HaxeBundle.message("haxe.module.type.name");
  }

  @Override
  public @NotNull String getDescription() {
    return HaxeBundle.message("haxe.module.type.description");
  }

  @Override
  public @NotNull Icon getNodeIcon(boolean isOpened) {
    return icons.HaxeIcons.HAXE_LOGO;
  }

  @Override
  public @NotNull HaxeModuleBuilder createModuleBuilder() {
    return new HaxeModuleBuilder();
  }
}
