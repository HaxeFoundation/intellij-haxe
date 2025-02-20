package com.intellij.plugins.haxe.ide.lookup;

import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.completion.JavaCompletionUtil;
import com.intellij.codeInsight.completion.PrioritizedLookupElement;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.navigation.ItemPresentation;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeComponentName;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.psi.PsiClass;
import icons.HaxeIcons;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class HaxeClassLookupElement extends LookupElement implements HaxePsiLookupElement {
  @Getter private final HaxeCompletionPriorityData priority = new HaxeCompletionPriorityData();
  private final HaxeComponentName myComponentName;
  private final HaxeClass haxeClass;
  @Getter private HaxeClassModel model;
  @Getter private HaxeComponentType type;


  private String presentableText;
  private String tailText;
  private boolean strikeout = false;
  private boolean bold = false;
  private Icon icon = null;

  @NotNull
  public static Collection<HaxeClassLookupElement> convert(PsiClass[] classes) {
    final List<HaxeClassLookupElement> result = new ArrayList<>();
    for (PsiClass aClass : classes) {
      if (aClass instanceof HaxeClass haxeClass)
        result.add(new HaxeClassLookupElement(haxeClass, haxeClass.getComponentName()));
    }

    return result;
  }

  public HaxeClassLookupElement(HaxeClass haxeClass, HaxeComponentName name) {
    this.myComponentName = name;
    this.haxeClass = haxeClass;
    this.type = haxeClass.getComponentType();
    calculatePresentation();
  }

  @NotNull
  @Override
  public String getLookupString() {
    return myComponentName.getIdentifier().getText();
  }

  @Override
  public void renderElement(LookupElementPresentation presentation) {
    presentation.setItemText(presentableText);
    presentation.setStrikeout(strikeout);
    presentation.setItemTextBold(bold);
    presentation.setIcon(icon);
    //presentation.setTailText(tailText, true);
  }

  private void calculatePresentation() {
    final ItemPresentation myComponentNamePresentation = myComponentName.getPresentation();
    if (myComponentNamePresentation == null) {
      presentableText = getLookupString();
      return;
    }

    model = haxeClass.getModel();
    if (model == null) {
      presentableText = myComponentNamePresentation.getPresentableText();
    }
    else {
      presentableText = model.getName();
      if (model.isEnum()) icon = HaxeIcons.Enum;
      else if (model.isTypedef()) icon = HaxeIcons.Typedef;
      else if (model.isInterface()) icon = HaxeIcons.Interface;
      else  icon = HaxeIcons.Class;
    }
  }

  @Override
  public void handleInsert(InsertionContext context) {
    JavaCompletionUtil.insertClassReference(haxeClass, context.getFile(), context.getStartOffset());
  }


  @NotNull
  @Override
  public Object getObject() {
    return myComponentName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof HaxeClassLookupElement)) return false;

    return myComponentName.equals(((HaxeClassLookupElement)o).myComponentName);
  }

  @Override
  public int hashCode() {
    return myComponentName.hashCode();
  }

  @Override
  public PrioritizedLookupElement<LookupElement> toPrioritized() {
    return (PrioritizedLookupElement<LookupElement>)PrioritizedLookupElement.withPriority(this,priority.calculate());
  }
}
