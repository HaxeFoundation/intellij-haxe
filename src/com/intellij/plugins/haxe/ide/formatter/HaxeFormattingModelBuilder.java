package com.intellij.plugins.haxe.ide.formatter;

import com.intellij.formatting.Alignment;
import com.intellij.formatting.Wrap;
import com.intellij.formatting.templateLanguages.DataLanguageBlockWrapper;
import com.intellij.formatting.templateLanguages.TemplateLanguageBlock;
import com.intellij.formatting.templateLanguages.TemplateLanguageFormattingModelBuilder;
import com.intellij.lang.ASTNode;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author fedor.korotkov
 */
public class HaxeFormattingModelBuilder extends TemplateLanguageFormattingModelBuilder {
  @Override
  public TemplateLanguageBlock createTemplateLanguageBlock(@NotNull ASTNode node,
                                                           @Nullable Wrap wrap,
                                                           @Nullable Alignment alignment,
                                                           @Nullable List<DataLanguageBlockWrapper> foreignChildren,
                                                           @NotNull CodeStyleSettings codeStyleSettings) {
    return new HaxeBlock(node, wrap, alignment, codeStyleSettings, this, foreignChildren);
  }
}
