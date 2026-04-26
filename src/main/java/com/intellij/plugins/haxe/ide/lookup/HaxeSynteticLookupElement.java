package com.intellij.plugins.haxe.ide.lookup;

import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.completion.JavaCompletionUtil;
import com.intellij.codeInsight.completion.PrioritizedLookupElement;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.fakes.HaxeFakePsiElement;
import com.intellij.plugins.haxe.model.*;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class HaxeSynteticLookupElement extends LookupElement  implements HaxeLookupElement {
  @Getter private final HaxeCompletionPriorityData priority = new HaxeCompletionPriorityData();

  private HaxeFakePsiElement psiElement;

  private String presentableText;
  private String tailText;
  private String typeText;

  private boolean insertParentheses = false;
  private boolean strikeout = false;
  private boolean bold = false;
  private Icon icon;



  public HaxeSynteticLookupElement(HaxeFakePsiElement psiElement, String presentableText, String tailText, String typeText, Icon icon, boolean insertParentheses) {
    this.psiElement = psiElement;

    this.insertParentheses = insertParentheses;
    this.presentableText = presentableText;
    this.tailText = tailText;
    this.typeText = typeText;
    this.icon = icon;
  }


  @NotNull
  @Override
  public String getLookupString() {
    return presentableText;
  }

  @Override
  public void renderElement(LookupElementPresentation presentation) {
    presentation.setItemText(presentableText);
    presentation.setStrikeout(strikeout);
    presentation.setItemTextBold(bold);
    presentation.setIcon(icon);
    presentation.setTypeText(typeText);

    if (tailText != null) presentation.setTailText(tailText, true);

  }


  @Override
  public void handleInsert(@NotNull InsertionContext context) {
      if(insertParentheses) {
        JavaCompletionUtil.insertParentheses(context, this, false, true);
      }
  }


  @NotNull
  @Override
  public Object getObject() {
    return psiElement;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o instanceof HaxeSynteticLookupElement lookupElement) {
      return psiElement.equals(lookupElement.psiElement);
    }else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return psiElement.hashCode();
  }
}
