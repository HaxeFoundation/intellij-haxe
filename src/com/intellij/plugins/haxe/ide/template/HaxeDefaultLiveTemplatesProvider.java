package com.intellij.plugins.haxe.ide.template;

import com.intellij.codeInsight.template.impl.DefaultLiveTemplatesProvider;
import org.jetbrains.annotations.NonNls;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeDefaultLiveTemplatesProvider implements DefaultLiveTemplatesProvider {
  private static final @NonNls String[] DEFAULT_TEMPLATES = new String[]{
    "/liveTemplates/haxe_miscellaneous",
    "/liveTemplates/haxe_iterations"
  };

  public String[] getDefaultLiveTemplateFiles() {
    return DEFAULT_TEMPLATES;
  }

  @Override
  public String[] getHiddenLiveTemplateFiles() {
    return null;
  }
}
