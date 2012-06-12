package com.intellij.plugins.haxe.ide.template;

import com.intellij.codeInsight.template.TemplateContextType;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeTemplateContextType extends TemplateContextType {
  protected HaxeTemplateContextType() {
    super("HAXE", HaxeBundle.message("haxe.language.id"));
  }

  @Override
  public boolean isInContext(@NotNull PsiFile file, int offset) {
    return file.getLanguage() instanceof HaxeLanguage;
  }
}
