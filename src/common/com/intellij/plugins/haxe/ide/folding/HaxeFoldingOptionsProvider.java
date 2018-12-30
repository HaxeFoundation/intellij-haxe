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

import com.intellij.application.options.editor.CodeFoldingOptionsProvider;
import com.intellij.openapi.options.BeanConfigurable;
import com.intellij.plugins.haxe.HaxeBundle;

public class HaxeFoldingOptionsProvider extends BeanConfigurable<HaxeFoldingSettings> implements CodeFoldingOptionsProvider {
  /**
   * This is a stub, which could be used later to add haxe-specific folding settings
   * <pre><code>
   *  final HaxeFoldingSettings settings = getInstance();
   *  checkBox(
   *    HaxeBundle.message("collapse.special.region"),
   *    settings::isCollapseSpecialRegion,
   *    settings::setCollapseSpecialRegion
   *    );
   * </code></pre>
   *
   * @see HaxeFoldingSettings
   **/
  public HaxeFoldingOptionsProvider() {
    super(HaxeFoldingSettings.getInstance());
    final HaxeFoldingSettings settings = getInstance();

    checkBox(HaxeBundle.message("haxe.idea.style.regions"), settings::isCollapseHaxePluginStyleRegions, settings::setCollapseHaxePluginStyleRegions);
    checkBox(HaxeBundle.message("haxe.flashdevelop.style.regions"), settings::isCollapseFlashDevelopStyleRegions, settings::setCollapseFlashDevelopStyleRegions);
    checkBox(HaxeBundle.message("haxe.c.as3.style.regions"), settings::isCollapseCSharpStyleRegions, settings::setCollapseCSharpStyleRegions);
    checkBox(HaxeBundle.message("haxe.unused.conditionally.compiled.code"), settings::isCollapseUnusedConditionallyCompiledCode, settings::setCollapseUnusedConditionallyCompiledCode);
  }
}
