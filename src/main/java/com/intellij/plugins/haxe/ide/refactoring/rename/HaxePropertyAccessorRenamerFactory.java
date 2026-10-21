package com.intellij.plugins.haxe.ide.refactoring.rename;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.plugins.haxe.HaxeRefactoringBundle;
import com.intellij.plugins.haxe.model.HaxePropertyFamily;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.rename.naming.AutomaticRenamer;
import com.intellij.refactoring.rename.naming.AutomaticRenamerFactory;
import com.intellij.usageView.UsageInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * The rename dialog's counterpart of {@link HaxePropertyInplaceRenameHandler}:
 * a "Rename property accessors" checkbox in the dialog. Once the new name is
 * confirmed, the platform lists the family's other members
 * ({@link HaxePropertyFamily}) with the names they will take.
 */
public class HaxePropertyAccessorRenamerFactory implements AutomaticRenamerFactory {
  private static final String ENABLED_KEY = "haxe.rename.property.accessors";

  @Override
  public boolean isApplicable(@NotNull PsiElement element) {
    return HaxePropertyFamily.isPropertyOrAccessor(element);
  }

  @Override
  public String getOptionName() {
    return HaxeRefactoringBundle.message("rename.property.accessors.option");
  }

  @Override
  public boolean isEnabled() {
    return PropertiesComponent.getInstance().getBoolean(ENABLED_KEY, true);
  }

  @Override
  public void setEnabled(boolean enabled) {
    PropertiesComponent.getInstance().setValue(ENABLED_KEY, enabled, true);
  }

  @Override
  public @NotNull AutomaticRenamer createRenamer(PsiElement element, String newName, Collection<UsageInfo> usages) {
    return new HaxePropertyAccessorRenamer(element, newName);
  }
}
