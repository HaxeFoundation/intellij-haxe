package com.intellij.plugins.haxe.ide.lookup;

import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.completion.JavaCompletionUtil;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.navigation.ItemPresentation;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeComponentName;
import com.intellij.plugins.haxe.lang.psi.HaxeReferenceExpression;
import com.intellij.plugins.haxe.lang.psi.impl.AbstractHaxePsiClass;
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeClassStub;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import icons.HaxeIcons;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.intellij.plugins.haxe.ide.lookup.lookupItemImportUtil.addImportIfNecessary;

public class HaxeClassLookupElement extends LookupElement implements HaxePsiLookupElement {
  @Getter private final HaxeCompletionPriorityData priority = new HaxeCompletionPriorityData();
  private final HaxeComponentName myComponentName;
  private final HaxeClass haxeClass;
  @Getter private HaxeClassModel model;
  @Getter private final HaxeComponentType type;


  private String presentableText;
  private String tailText;
  private boolean strikeout = false;
  private boolean bold = false;
  private boolean presentationCalculated = false;
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
  }

  @NotNull
  @Override
  public String getLookupString() {
    return haxeClass.getName();
  }

  @Override
  public void renderElement(LookupElementPresentation presentation) {
    if (!presentationCalculated) {
      calculatePresentation();
      presentationCalculated = true;
    }
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
    if(!isPartOfChain(context)){
      addImportIfNecessary(context, haxeClass, haxeClass.getQualifiedName());
    }
    //TODO verify we do not need this anymore (test with module member classes)
    //JavaCompletionUtil.insertClassReference(haxeClass, context.getFile(), context.getStartOffset());
  }

  private static boolean isPartOfChain(InsertionContext context) {
    PsiFile file = context.getFile();
    PsiElement element = file.findElementAt(context.getStartOffset());
    if(element == null) return false;
    return PsiTreeUtil.getParentOfType(element, HaxeReferenceExpression.class) != null;
  }

  @NotNull
  @Override
  public String deduplicateKey() {
    return haxeClass.getQualifiedName();
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

}
