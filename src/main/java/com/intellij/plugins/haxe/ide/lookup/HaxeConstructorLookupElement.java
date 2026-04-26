package com.intellij.plugins.haxe.ide.lookup;

import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.codeInsight.TailType;
import com.intellij.codeInsight.TailTypes;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.completion.PrioritizedLookupElement;
import com.intellij.codeInsight.completion.util.CompletionStyleUtil;
import com.intellij.codeInsight.completion.util.ParenthesesInsertHandler;
import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.codeInsight.lookup.LookupItem;
import com.intellij.openapi.editor.Editor;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.HaxeMemberModel;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilCore;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import static com.intellij.plugins.haxe.ide.lookup.lookupItemImportUtil.*;

@EqualsAndHashCode
public class HaxeConstructorLookupElement extends LookupElement implements HaxePsiLookupElement {
  @Getter private final HaxeCompletionPriorityData priority = new HaxeCompletionPriorityData();
  @Getter private final HaxeComponentType type;
  @Getter private final String className;
  @Getter private final String packageName;
  @Getter private final String qname;
  @Getter private final boolean hasParameters;

  private final Icon icon;
  private final String presentableText;
  private HaxeMethod construtor;


  public HaxeConstructorLookupElement(HaxeMethod constructor,
                                      String className,
                                      String packageName,
                                      boolean hasParameters,
                                      HaxeComponentType type)
  {
    this.presentableText = className + "()";
    this.className = className;
    this.packageName = packageName;
    this.hasParameters = hasParameters;
    this.type = type;
    this.icon = type.getIcon();
    this.construtor = constructor;
    this.qname = createQname(className, packageName);
  }

  @NotNull
  @Override
  public String getLookupString() {
    return className;
  }

  /** Returns the fully-qualified class name for use as the dedup key. */
  @NotNull
  @Override
  public String deduplicateKey() {
    return qname;
  }

  @Override
  public void renderElement(LookupElementPresentation presentation) {
    presentation.setItemText(presentableText);
    presentation.setTypeText(packageName);
    presentation.setIcon(icon);
  }


  @Override
  public void handleInsert(InsertionContext context) {
    PsiElement element = insertParentheses(context, this,  hasParameters, true);
    context.commitDocument();
    addImportIfNecessary(context, element, qname);
  }



  @Override
  public @Nullable PsiElement getPsiElement() {
    return construtor;
  }


  static PsiElement insertParentheses(@NotNull InsertionContext context,
                                      @NotNull LookupElement item,
                                      boolean hasParams,
                                      boolean forceClosingParenthesis) {
    Editor editor = context.getEditor();
    char completionChar = context.getCompletionChar();
    PsiFile file = context.getFile();

    TailType tailType = completionChar == '(' ? TailTypes.noneType() :
                        completionChar == ':' ? TailTypes.conditionalExpressionColonType() :
                        LookupItem.handleCompletionChar(context.getEditor(), item, completionChar);
    boolean hasTail = tailType != TailTypes.noneType() && tailType != TailTypes.unknownType();
    boolean smart = completionChar == Lookup.COMPLETE_STATEMENT_SELECT_CHAR;

    if (completionChar == '(' || completionChar == '.' || completionChar == ',' || completionChar == ';' || completionChar == ':' || completionChar == ' ') {
      context.setAddCompletionChar(false);
    }

    if (hasTail) {
      hasParams = false;
    }
    boolean needRightParenth = forceClosingParenthesis ||
                               !smart && (CodeInsightSettings.getInstance().AUTOINSERT_PAIR_BRACKET ||
                                          !hasParams && completionChar != '(');

    context.commitDocument();

    CommonCodeStyleSettings styleSettings = CompletionStyleUtil.getCodeStyleSettings(context);
    PsiElement elementAt = file.findElementAt(context.getStartOffset());
    if (elementAt == null || !(elementAt.getParent() instanceof PsiMethodReferenceExpression)) {
      boolean hasParameters = hasParams;
      boolean spaceBetweenParentheses = hasParams && styleSettings.SPACE_WITHIN_METHOD_CALL_PARENTHESES;
      new ParenthesesInsertHandler<>(styleSettings.SPACE_BEFORE_METHOD_CALL_PARENTHESES, spaceBetweenParentheses,
                                     needRightParenth, styleSettings.METHOD_PARAMETERS_LPAREN_ON_NEXT_LINE) {
        @Override
        protected boolean placeCaretInsideParentheses(InsertionContext context1, LookupElement item1) {
          return hasParameters;
        }

        @Override
        protected PsiElement findExistingLeftParenthesis(@NotNull InsertionContext context) {
          PsiElement token = super.findExistingLeftParenthesis(context);
          return isPartOfLambda(token) ? null : token;
        }

        private static boolean isPartOfLambda(PsiElement token) {
          return token != null && token.getParent() instanceof PsiExpressionList &&
                 PsiUtilCore.getElementType(PsiTreeUtil.nextVisibleLeaf(token.getParent())) == JavaTokenType.ARROW;
        }
      }.handleInsert(context, item);
    }
    return elementAt;
  }
}
