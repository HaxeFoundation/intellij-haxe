package com.intellij.plugins.haxe.util;

import com.intellij.codeInsight.template.ExpressionContext;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.lang.psi.HaxeComponentName;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.util.PsiTreeUtil;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Set;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeMacroUtil {
  public static Set<HaxeComponentName> findVariables(@Nullable PsiElement at) {
    if (at == null) {
      return Collections.emptySet();
    }
    final Set<HaxeComponentName> result = new THashSet<HaxeComponentName>();
    PsiTreeUtil.treeWalkUp(new PsiScopeProcessor() {
      @Override
      public boolean execute(@NotNull PsiElement element, ResolveState state) {
        if (element instanceof HaxeNamedComponent) {
          final HaxeNamedComponent haxeNamedComponent = (HaxeNamedComponent)element;
          if (haxeNamedComponent.getComponentName() != null && HaxeComponentType.isVariable(HaxeComponentType.typeOf(haxeNamedComponent))) {
            result.add(haxeNamedComponent.getComponentName());
          }
        }
        return true;
      }

      @Override
      public <T> T getHint(@NotNull Key<T> hintKey) {
        return null;
      }

      @Override
      public void handleEvent(Event event, @Nullable Object associated) {
      }
    }, at, null, ResolveState.initial());
    return result;
  }

  @Nullable
  public static PsiElement findElementAt(ExpressionContext context) {
    final Project project = context.getProject();
    final int templateStartOffset = context.getTemplateStartOffset();
    final int offset = templateStartOffset > 0 ? context.getTemplateStartOffset() - 1 : context.getTemplateStartOffset();

    PsiDocumentManager.getInstance(project).commitAllDocuments();

    final PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(context.getEditor().getDocument());
    return file == null ? null : file.findElementAt(offset);
  }
}
