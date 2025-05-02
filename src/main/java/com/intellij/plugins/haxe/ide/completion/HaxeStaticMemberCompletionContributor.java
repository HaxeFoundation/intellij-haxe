package com.intellij.plugins.haxe.ide.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.util.Pair;
import com.intellij.plugins.haxe.ide.index.HaxeStaticMemberIndex;
import com.intellij.plugins.haxe.ide.index.HaxeStaticMemberInfo;
import com.intellij.plugins.haxe.ide.lookup.HaxeStaticMemberLookupElement;
import com.intellij.plugins.haxe.lang.psi.HaxeIdentifier;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.ProcessingContext;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.patterns.PlatformPatterns.psiElement;

public class HaxeStaticMemberCompletionContributor extends CompletionContributor {
  public HaxeStaticMemberCompletionContributor() {
    extend(CompletionType.BASIC, psiElement().inside(HaxeIdentifier.class),
           new CompletionProvider<CompletionParameters>() {
             @Override
             protected void addCompletions(@NotNull CompletionParameters parameters,
                                           ProcessingContext context,
                                           @NotNull CompletionResultSet result) {
               final PsiFile file = parameters.getOriginalFile();
               PsiElement position = parameters.getOriginalPosition();
               position = position != null ? position : parameters.getPosition();
               addVariantsFromIndex(result, file, position.getText());
             }
           });
  }

  private static void addVariantsFromIndex(final CompletionResultSet resultSet, final PsiFile targetFile, @NlsSafe String text) {

    final Project project = targetFile.getProject();
    final GlobalSearchScope scope = HaxeResolveUtil.getScopeForElement(targetFile);
    final MyProcessor processor = new MyProcessor(resultSet, targetFile);
    HaxeStaticMemberIndex.processAll(project, processor, scope, text);
  }


  private static class MyProcessor implements Processor<Pair<String, HaxeStaticMemberInfo>> {

    private final PsiElement element;
    private final CompletionResultSet myResultSet;

    private MyProcessor(CompletionResultSet resultSet, @Nullable PsiElement element) {
      myResultSet = resultSet;
      this.element = element;
    }

    @Override
    public boolean process(Pair<String, HaxeStaticMemberInfo> pair) {
      HaxeStaticMemberInfo info = pair.getSecond();
      myResultSet.addElement(new HaxeStaticMemberLookupElement(
        info.getPackageName(),
        info.getModuleName(),
        info.getClassName(),
        info.getMemberName(),
        info.getType(),
        info.getTypeValue(),
        info.toFullyQualifiedInfo(),
        element));
      return true;
    }
  }
}
