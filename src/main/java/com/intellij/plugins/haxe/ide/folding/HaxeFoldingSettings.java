/*
 * Copyright 2018 Ilya Malanin
 * Copyright 2018 Eric Bishton
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
package com.intellij.plugins.haxe.ide.folding;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;

@State(name = "HaxeFoldingSettings", storages = @Storage("editor.codeinsight.xml"))
public class HaxeFoldingSettings implements PersistentStateComponent<HaxeFoldingSettings> {
  /**
   * This is a stub, which could be used later to add haxe-specific folding settings
   * <pre><code>
   *
   * public boolean isCollapseSpecialRegions() {
   *  return myCollapseSpecialRegions;
   * }
   *
   * public void setCollapseSpecialRegions(final boolean value) {
   *  myCollapseSpecialRegions = value;
   * }
   * </code></pre>
   *
   * @see HaxeFoldingOptionsProvider
   **/

  private boolean myCollapseFlashDevelopStyleRegions;
  private boolean myCollapseHaxePluginStyleRegions;
  private boolean myCollapseCSharpStyleRegions;
  private boolean myCollapseUnusedConditionallyCompiledCode;

  public static HaxeFoldingSettings getInstance() {
    return ServiceManager.getService(HaxeFoldingSettings.class);
  }

  @Override
  public HaxeFoldingSettings getState() {
    return this;
  }

  @Override
  public void loadState(@NotNull final HaxeFoldingSettings state) {
    XmlSerializerUtil.copyBean(state, this);
  }

  public boolean isCollapseFlashDevelopStyleRegions() {
    return myCollapseFlashDevelopStyleRegions;
  }
  public void setCollapseFlashDevelopStyleRegions(final boolean value) {
    myCollapseFlashDevelopStyleRegions = value;
  }

  public boolean isCollapseHaxePluginStyleRegions() {
    return myCollapseHaxePluginStyleRegions;
  }
  public void setCollapseHaxePluginStyleRegions(final boolean value) {
    myCollapseHaxePluginStyleRegions = value;
  }

  public boolean isCollapseCSharpStyleRegions() {
    return myCollapseCSharpStyleRegions;
  }
  public void setCollapseCSharpStyleRegions(final boolean value) {
    myCollapseCSharpStyleRegions = value;
  }

  public boolean isCollapseUnusedConditionallyCompiledCode() {
    return myCollapseUnusedConditionallyCompiledCode;
  }
  public void setCollapseUnusedConditionallyCompiledCode(final boolean value) {
    myCollapseUnusedConditionallyCompiledCode = value;
  }
}