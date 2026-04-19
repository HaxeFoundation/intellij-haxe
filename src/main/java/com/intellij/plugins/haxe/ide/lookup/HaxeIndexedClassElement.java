package com.intellij.plugins.haxe.ide.lookup;

import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.model.FullyQualifiedInfo;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import static com.intellij.plugins.haxe.ide.lookup.lookupItemImportUtil.*;


public class HaxeIndexedClassElement extends LookupElement implements HaxePsiLookupElement {
  @Getter private final HaxeCompletionPriorityData priority = new HaxeCompletionPriorityData();

  @Getter private final String name;
  @Getter private final HaxeComponentType type;
  private final String tailText;
  private final Icon icon;

  private final boolean strikeout = false;
  private final boolean bold = false;


  private final HaxeClassModel model;

  public HaxeIndexedClassElement(HaxeClassModel model) {
    this.model = model;
    this.name = model.getName();
    this.type = model.haxeClass.getComponentType();
    this.icon = this.type != null ? type.getCompletionIcon() : null;

    FullyQualifiedInfo qualifiedInfo = model.getQualifiedInfo();
    String qualifiedName = qualifiedInfo != null ? qualifiedInfo.getQualifiedName(false) : "";
    this.tailText = qualifiedName != null ? HaxeResolveUtil.splitQName(qualifiedName).getFirst() : "";
  }


  @Override
  public void handleInsert(InsertionContext context) {
    PsiFile file = context.getFile();
    PsiElement element = file.findElementAt(context.getStartOffset());

    FullyQualifiedInfo qualifiedInfo = model.getQualifiedInfo();
    if (qualifiedInfo != null) {
      addImportIfNecessary(context, element, qualifiedInfo.toShortendImportReferenceString());
    }
  }


  @Override
  public @Nullable PsiElement getPsiElement() {
    return model.getPsi();
  }

  @NotNull
  @Override
  public String getLookupString() {
    return name;
  }


  @NotNull
  @Override
  public String deduplicateKey() {
    FullyQualifiedInfo qualifiedInfo = model.getQualifiedInfo();
    if (qualifiedInfo != null) {
      return qualifiedInfo.toString();
    }
    return "*Error*"; //ideally this shouldn't really happen, but currently it might due to some anonymous types
  }

  @Override
  public void renderElement(LookupElementPresentation presentation) {
    presentation.setItemText(name);
    presentation.setStrikeout(strikeout);
    presentation.setItemTextBold(bold);
    presentation.setIcon(icon);
    presentation.setTailText(tailText, true);
  }
}
