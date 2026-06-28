package com.intellij.plugins.haxe.ide.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.plugins.haxe.ide.lookup.HaxeSynteticLookupElement;
import com.intellij.plugins.haxe.ide.lookup.HaxeSynteticLookupElements;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.fakes.impl.HaxeFakeComponentTrace;
import com.intellij.plugins.haxe.lang.psi.fakes.impl.HaxeFakeTargetSpecificSyntax;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

import static com.intellij.patterns.PlatformPatterns.psiElement;
import static com.intellij.plugins.haxe.ide.completion.HaxeCommonCompletionPattern.identifierInNewExpression;

public class HaxeSynteticMemberCompletionContributor extends CompletionContributor {

    private static Map<String, String>   targetSpecificSyntaxData = Map.of(
            "__js__", "js.Syntax.code",
            "__php__", "php.Syntax.code",
            "__python__", "python.Syntax.code",
            "__cpp__", "cpp.Syntax.code",
            "__cs__", "cs.Syntax.code",
            "__java__", "java.Syntax.code",
            "__lua__", "lua.Syntax.code"
    );

    private static final PsiElementPattern.Capture<PsiElement> ELEMENT_CAPTURE = psiElement()
            .inside(HaxeIdentifier.class)
            .andNot(psiElement().inside(HaxeType.class))
            // - avoid chained refs (MyClass.startComplet.. / myVar.startComplet... should not show static suggestions)
            // current = ID token
            // parent 1: HaxeIdentifier
            // parent 2: HaxeReference
            // parent 3: should not be a refrence as that would be a chain
            .andNot(psiElement().withSuperParent(3, HaxeReferenceExpression.class));

    public HaxeSynteticMemberCompletionContributor() {
    extend(CompletionType.BASIC, ELEMENT_CAPTURE,
           new CompletionProvider<CompletionParameters>() {
             @Override
             protected void addCompletions(@NotNull CompletionParameters parameters,
                                           ProcessingContext context,
                                           @NotNull CompletionResultSet result) {
               final PsiFile file = parameters.getOriginalFile();
               var position = parameters.getOriginalPosition();
               boolean newExpression = identifierInNewExpression.accepts(position);
               if(!newExpression) {
                 position = position != null ? position : parameters.getPosition();
                 addVariantsFromIndex(result, file,position, position.getText());
               }
             }
           });
  }

  private static void addVariantsFromIndex(final CompletionResultSet resultSet,
                                           final PsiFile targetFile,
                                           PsiElement position,
                                           @NlsSafe String filterText) {
    final Project project = targetFile.getProject();
    final GlobalSearchScope scope = HaxeResolveUtil.getScopeForElement(targetFile);
    final PrefixMatcher matcher = resultSet.getPrefixMatcher();



      if("trace".startsWith(filterText)) {
          HaxeIdentifier identifier = PsiTreeUtil.getParentOfType(position, HaxeIdentifier.class);
          HaxeFakeComponentTrace fake = new HaxeFakeComponentTrace(identifier);
          HaxeSynteticLookupElement trace = HaxeSynteticLookupElements.trace(fake);
          resultSet.addElement(trace);
      }

      HaxeReference reference = position instanceof HaxeReference haxeReference
              ? haxeReference
              : PsiTreeUtil.getParentOfType(position, HaxeReference.class);

      if (reference != null) {
          for (Map.Entry<String, String> data : targetSpecificSyntaxData.entrySet()) {
              HaxeFakeTargetSpecificSyntax fake = HaxeFakePsiUtil.createFakeForSyntax(data.getKey(), data.getValue(), reference);
              HaxeSynteticLookupElement lookupElement = HaxeSynteticLookupElements.targetSpecificSyntax(fake);
              resultSet.addElement(lookupElement);
          }
      }
  }
}
