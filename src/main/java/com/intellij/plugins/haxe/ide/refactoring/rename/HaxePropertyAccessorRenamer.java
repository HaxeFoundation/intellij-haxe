package com.intellij.plugins.haxe.ide.refactoring.rename;

import com.intellij.plugins.haxe.HaxeRefactoringBundle;
import com.intellij.plugins.haxe.model.HaxePropertyFamily;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.refactoring.rename.naming.AutomaticRenamer;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/** The other members of a property family and the names they take when one member is renamed; see {@link HaxePropertyAccessorRenamerFactory}. */
class HaxePropertyAccessorRenamer extends AutomaticRenamer {

  HaxePropertyAccessorRenamer(@NotNull PsiElement element, @NotNull String newName) {
    HaxePropertyFamily family = HaxePropertyFamily.of(element);
    Map<PsiNamedElement, String> renames = family == null ? Map.of() : family.renamesFor(element, newName);
    for (Map.Entry<PsiNamedElement, String> rename : renames.entrySet()) {
      String oldName = rename.getKey().getName();
      if (oldName == null) continue;
      myElements.add(rename.getKey());
      suggestAllNames(oldName, rename.getValue());
    }
  }

  @Override
  public boolean isSelectedByDefault() {
    return true;
  }

  @Override
  public boolean allowChangeSuggestedName() {
    return false;
  }

  @Override
  public String getDialogTitle() {
    return HaxeRefactoringBundle.message("rename.property.accessors.title");
  }

  @Override
  public String getDialogDescription() {
    return HaxeRefactoringBundle.message("rename.property.accessors.description");
  }

  @Override
  public String entityName() {
    return HaxeRefactoringBundle.message("rename.property.accessors.entity");
  }
}
