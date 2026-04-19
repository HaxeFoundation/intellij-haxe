package com.intellij.plugins.haxe.ide.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.plugins.haxe.ide.hierarchy.HaxeHierarchyUtils;
import com.intellij.plugins.haxe.ide.lookup.HaxeMacroLookupElement;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeComponentName;
import com.intellij.plugins.haxe.lang.psi.impl.ComponentNameScopeProcessor;
import com.intellij.plugins.haxe.model.*;
import com.intellij.plugins.haxe.model.type.HaxeGenericResolver;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.ResolveState;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.intellij.plugins.haxe.ide.completion.HaxeCommonCompletionPattern.inReificationOrMacro;
import static com.intellij.plugins.haxe.ide.completion.HaxeMacroCompletionUtil.reificationLookupElement;
import static com.intellij.plugins.haxe.ide.completion.MacroCompletionData.reificationData;
import static com.intellij.plugins.haxe.ide.completion.MacroCompletionData.macroFunctionData;
import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.*;

public class HaxeMacroCompletionContributor extends CompletionContributor {

  public static final Set<MacroCompletionData> REIFICATION_TYPES =
    Set.of(
      // reification expressions
      reificationData(MACRO_ARRAY_REIFICATION, "$a", "Array reification"),
      reificationData(MACRO_BLOCK_REIFICATION, "$b", "Block reification"),
      reificationData(MACRO_IDENTIFIER_REIFICATION, "$i", "Identifier reification"),
      reificationData(MACRO_FIELD_REIFICATION, "$p", "Field reification"),
      reificationData(MACRO_VALUE_REIFICATION, "$v", "Value / Enum Reification"),
      reificationData(MACRO_EXPRESSION_REIFICATION, "$e", "Expression reification"),
      reificationData(MACRO_EXPRESSION_REIFICATION, "$", "Expression reification"),
      // macro functions
      macroFunctionData(MACRO_IDENTIFIER, "$type", "Type tracing")
    );


  public HaxeMacroCompletionContributor() {
    extend(CompletionType.BASIC, inReificationOrMacro,
           new CompletionProvider<CompletionParameters>() {
             @Override
             protected void addCompletions(@NotNull CompletionParameters parameters,
                                           ProcessingContext context,
                                           @NotNull CompletionResultSet result) {
               reificationAndMacroIds(result, parameters.getOriginalPosition(), parameters.getOriginalFile());
             }
           });
  }

  @Override
  public void fillCompletionVariants(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {
    super.fillCompletionVariants(parameters, result);
  }


  private void reificationAndMacroIds(CompletionResultSet result, PsiElement position, PsiFile file) {
    addReificationsAndMacroFunctions(result);
    addMacroIdentifiers(result, position);
  }

  private static void addMacroIdentifiers(CompletionResultSet result, PsiElement position) {
    Set<HaxeComponentName> suggestedVariants = new HashSet<>();
    PsiTreeUtil.treeWalkUp(new ComponentNameScopeProcessor(suggestedVariants), position, null, new ResolveState());

    List<HaxeComponentName> members = HaxeHierarchyUtils.findMembersByWalkingTree(position);
    for (HaxeComponentName name : members) {
      // ignoring type definitions and method/function definitions
      HaxeModel model = HaxeBaseMemberModel.fromPsi(name);
      if ((model instanceof HaxeMethodModel) || (name.getParent() instanceof HaxeClass)) continue;
      result.addElement(HaxeMacroLookupElement.create(name, new HaxeGenericResolver()));

    }
  }

  private static void addReificationsAndMacroFunctions(CompletionResultSet result) {
    List<LookupElement> lookupElements = new ArrayList<>();
    addReificationSuggestions(lookupElements, REIFICATION_TYPES);
    result.addAllElements(lookupElements);
  }

  public static void addReificationSuggestions(List<LookupElement> result, Set<MacroCompletionData> macroCompletionData) {
    for (MacroCompletionData completionData : macroCompletionData) {
      LookupElement lookupElement = reificationLookupElement(completionData);
      LookupElement withPriorityZero = PrioritizedLookupElement.withPriority(lookupElement, 0);
      result.add(withPriorityZero);
    }
  }
}
