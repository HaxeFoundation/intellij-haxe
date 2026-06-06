package com.intellij.plugins.haxe.ide.lookup.indexed;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.ide.lookup.HaxeCompletionPriorityData;
import com.intellij.plugins.haxe.ide.lookup.HaxePsiLookupElement;
import com.intellij.plugins.haxe.ide.lookup.indexed.data.HaxeClassLookupData;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.indexes.unified.HaxeConstrcutorUnifiedIndex;
import com.intellij.plugins.haxe.model.FullyQualifiedInfo;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
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
      if(isNewExpression(element)) {
        HaxeMethod constructor = HaxeConstrcutorUnifiedIndex.getConstructor(qualifiedInfo, file.getProject(), file.getResolveScope());
        if(constructor != null) {
          context.commitDocument();
          insertParentheses(context, this, constructor.hasParameters(), true);
        }
      }

      if (qualifiedInfo != null) {
        addImportIfNecessary(context, element, qualifiedInfo.toShortendImportReferenceString());
      }
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
