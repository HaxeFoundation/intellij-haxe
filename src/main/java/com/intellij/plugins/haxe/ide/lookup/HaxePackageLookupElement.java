package com.intellij.plugins.haxe.ide.lookup;

import com.intellij.codeInsight.completion.PrioritizedLookupElement;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.PackageLookupItem;
import com.intellij.psi.PsiPackage;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class HaxePackageLookupElement extends PackageLookupItem implements HaxeLookupElement {
  @Getter private final HaxeCompletionPriorityData priority = new HaxeCompletionPriorityData();
  private final PsiPackage aPackage;

  @NotNull
  public static Collection<HaxePackageLookupElement>  convert(PsiPackage[] packages) {
    final List<HaxePackageLookupElement> result = new ArrayList<>();
    for (PsiPackage aPackage : packages) {
        result.add(new HaxePackageLookupElement(aPackage));
    }
    return result;
  }


  public HaxePackageLookupElement(PsiPackage aPackage) {
    super(aPackage);
    this.aPackage = aPackage;
  }

  @NotNull
  @Override
  public Object getObject() {
    return aPackage;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof HaxePackageLookupElement)) return false;

    return aPackage.equals(((HaxePackageLookupElement)o).aPackage);
  }

  @Override
  public int hashCode() {
    return aPackage.hashCode();
  }
}
