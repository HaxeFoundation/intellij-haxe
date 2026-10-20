package com.intellij.plugins.haxe.ide.lookup.indexed;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.ide.lookup.HaxeCompletionPriorityData;
import com.intellij.plugins.haxe.ide.lookup.HaxePsiLookupElement;
import com.intellij.plugins.haxe.ide.lookup.indexed.data.HaxeClassLookupData;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.indexes.unified.HaxeConstructorUnifiedIndex;
import com.intellij.plugins.haxe.model.FullyQualifiedInfo;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.util.PsiTreeUtil;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import static com.intellij.plugins.haxe.ide.lookup.indexed.HaxelookupInsertUtil.insertParentheses;
import static com.intellij.plugins.haxe.ide.lookup.lookupItemImportUtil.*;


public class HaxeIndexedClassLookupElement extends LookupElement implements HaxePsiLookupElement {
  @Getter private final HaxeCompletionPriorityData priority = new HaxeCompletionPriorityData();

  @Getter private FullyQualifiedInfo qualifiedInfo;
  @Getter private final HaxeComponentType type;
  @Getter private final String name;
  private final String tailText;
  private final Icon icon;

  private final boolean strikeout = false;
  private final boolean bold = false;


  private final InsertHandler<HaxeIndexedClassLookupElement> myInsertHandler;
  private final HaxeClassLookupData lookupData;

  public HaxeIndexedClassLookupElement(HaxeClassLookupData classLookupData, InsertHandler<HaxeIndexedClassLookupElement> insertHandler) {
    this.lookupData = classLookupData;

    this.qualifiedInfo = classLookupData.qualifiedInfo;
    this.name = classLookupData.name;
    this.type = classLookupData.type;
    this.icon = classLookupData.icon;

    this.myInsertHandler = insertHandler;

    FullyQualifiedInfo qualifiedInfo = classLookupData.qualifiedInfo;
    String qualifiedName = qualifiedInfo != null ? qualifiedInfo.getQualifiedName(false) : "";
    this.tailText = qualifiedName != null ? HaxeResolveUtil.splitQName(qualifiedName).getFirst() : "";
  }


  @Override
  public void handleInsert(InsertionContext context) {
    if(myInsertHandler != null) {
      myInsertHandler.handleInsert(context, this);
    }else {
      PsiFile file = context.getFile();
      PsiElement element = file.findElementAt(context.getStartOffset());
      boolean packageQualified = isPackageQualified(element);
      if(isNewExpression(element)) {
        HaxeMethod constructor = HaxeConstructorUnifiedIndex.getConstructor(qualifiedInfo, file.getProject(), file.getResolveScope());
        if(constructor != null) {
          context.commitDocument();
          insertParentheses(context, this, constructor.hasParameters(), true);
        }
      }

      if (qualifiedInfo != null) {
        String path = qualifiedInfo.toShortendImportReferenceString();
        if (packageQualified) {
          writePath(context, path);
        }
        else {
          addImportIfNecessary(context, element, path);
        }
      }
    }
  }

  /** Whether the completed name follows a package ({@code pack.sub.Name}); the reference then spells the type's whole path. */
  private static boolean isPackageQualified(@Nullable PsiElement element) {
    HaxeReference reference = PsiTreeUtil.getParentOfType(element, HaxeReference.class);
    HaxeReference qualifier = HaxeResolveUtil.getLeftReference(reference);
    return qualifier != null && qualifier.resolve() instanceof PsiPackage;
  }

  /**
   * Rewrites the whole package-qualified reference, qualifier included, as the
   * type's path. A type declared inside another module is reachable only
   * through that module: {@code pack.Kind} becomes {@code pack.Module.Kind}.
   * Replacing only the completed name would keep the typed qualifier in front
   * of the path. The text is written to the document directly: a PSI replace
   * would reformat the surrounding statement.
   */
  private static void writePath(@NotNull InsertionContext context, @NotNull String path) {
    context.commitDocument();
    PsiElement leaf = context.getFile().findElementAt(context.getStartOffset());
    HaxeReference reference = PsiTreeUtil.getParentOfType(leaf, HaxeReference.class);
    if (reference == null || reference.getText().equals(path)) return;

    TextRange range = reference.getTextRange();
    // a caret past the reference (inside constructor parentheses) moves with the text by itself
    boolean caretAtEnd = context.getEditor().getCaretModel().getOffset() == range.getEndOffset();
    context.getDocument().replaceString(range.getStartOffset(), range.getEndOffset(), path);
    context.commitDocument();
    if (caretAtEnd) {
      context.getEditor().getCaretModel().moveToOffset(range.getStartOffset() + path.length());
    }
  }

  private boolean isNewExpression(PsiElement element) {
    HaxeType parentOfType = PsiTreeUtil.getParentOfType(element, HaxeType.class);
    if (parentOfType != null && parentOfType.getParent() instanceof HaxeNewExpression) {
      return true;
    }
    return false;
  }


  public @Nullable HaxeClassModel getModel() {
    return lookupData.getClassModel();

  }

  @Override
  public @Nullable PsiElement getPsiElement() {
    HaxeClassModel classModel = getModel();
    return classModel != null ?  classModel.getPsi() : null;
  }

  @NotNull
  @Override
  public String getLookupString() {
    return name;
  }


  @NotNull
  @Override
  public String deduplicateKey() {
    if (qualifiedInfo != null) {
      return qualifiedInfo.toString();
    }
    //ideally this shouldn't really happen, but currently it might due to some anonymous types
    return "*Error*";
  }

  @Override
  public void renderElement(LookupElementPresentation presentation) {
    presentation.setItemText(name);
    presentation.setStrikeout(strikeout);
    presentation.setItemTextBold(bold);
    presentation.setIcon(icon);
    presentation.setTailText(" " + tailText, true);
  }
}
