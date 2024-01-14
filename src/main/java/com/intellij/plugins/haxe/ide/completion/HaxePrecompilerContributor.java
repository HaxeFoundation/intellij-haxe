package com.intellij.plugins.haxe.ide.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.plugins.haxe.haxelib.definitions.HaxeDefineDetectionManager;
import com.intellij.psi.PsiFile;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;

import static com.intellij.patterns.PsiJavaPatterns.psiElement;
import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets.PPEXPRESSION;

public class HaxePrecompilerContributor extends CompletionContributor {
  public HaxePrecompilerContributor() {
    extend(CompletionType.BASIC,
           psiElement().withElementType(PPEXPRESSION),
           new CompletionProvider<>() {
             @Override
             protected void addCompletions(@NotNull CompletionParameters parameters,
                                           @NotNull ProcessingContext context,
                                           @NotNull CompletionResultSet result) {
               addDefinitions(result, parameters.getOriginalFile());
             }
           });
  }

  private static void addDefinitions(CompletionResultSet resultSet, PsiFile targetFile) {
    Map<String, String> allDefinitions = HaxeDefineDetectionManager.getInstance(targetFile.getProject()).getAllDefinitions();
    Set<String> names = allDefinitions.keySet();
    for (String name : names) {
      if(!name.trim().isEmpty()) {
        String value = allDefinitions.get(name);
        if (value.trim().isEmpty()) {
          LookupElement lookupElement = LookupElementBuilder.create(name);
          resultSet.addElement(lookupElement);
        }else {
          LookupElement lookupElement = LookupElementBuilder.create(name).appendTailText(" (" + value + ")", true);
          resultSet.addElement(lookupElement);
        }
      }
    }
  }
}
