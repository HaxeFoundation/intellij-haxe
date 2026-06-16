package com.intellij.plugins.haxe.ide.lookup;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.plugins.haxe.lang.psi.indexes.filebased.data.HaxeComponentIndexData;
import com.intellij.plugins.haxe.model.FullyQualifiedInfo;
import icons.HaxeIcons;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;


public class HaxeModuleLookupElement extends LookupElement implements HaxeLookupElement {
  @Getter private final HaxeCompletionPriorityData priority = new HaxeCompletionPriorityData();

  private final String packageName;
  private final String moduleName;


  @NotNull
  public static List<HaxeModuleLookupElement>  convert(Collection<HaxeComponentIndexData> modules) {
    final List<HaxeModuleLookupElement> result = new ArrayList<>();
    for (HaxeComponentIndexData module : modules) {
      result.add(new HaxeModuleLookupElement(module.getFqn()));
    }
    return result;
  }

  public HaxeModuleLookupElement(FullyQualifiedInfo moduleFqn) {
    this.moduleName = moduleFqn.getModuleName();
    this.packageName = moduleFqn.getPackageName();
  }

  @Override
  public void renderElement(LookupElementPresentation presentation) {
    presentation.setItemText(moduleName);
    presentation.setIcon(HaxeIcons.Module);
    presentation.setTypeText(packageName);
  }


  @Override
  public @NotNull String getLookupString() {
    return moduleName;
  }

  @NotNull
  @Override
  public Object getObject() {
    return moduleName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o instanceof HaxeModuleLookupElement element){
      return packageName.equals(element.packageName) && moduleName.equals(element.moduleName);
    }
    return false;
  }


  @Override
  public int hashCode() {
    return Objects.hash(packageName, moduleName);
  }
}
