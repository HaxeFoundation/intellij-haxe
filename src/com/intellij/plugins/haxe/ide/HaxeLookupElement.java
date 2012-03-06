package com.intellij.plugins.haxe.ide;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.haxe.lang.psi.HaxeComponentName;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeLookupElement extends LookupElement {
  private final HaxeComponentName myComponentName;

  public static Collection<HaxeLookupElement> convert(@NotNull Collection<HaxeComponentName> componentNames) {
    final List<HaxeLookupElement> result = new ArrayList<HaxeLookupElement>(componentNames.size());
    for (HaxeComponentName componentName : componentNames) {
      result.add(new HaxeLookupElement(componentName));
    }
    return result;
  }

  public HaxeLookupElement(HaxeComponentName name) {
    myComponentName = name;
  }

  @NotNull
  @Override
  public String getLookupString() {
    final String result = myComponentName.getName();
    if (result != null) {
      return result;
    }
    return myComponentName.getIdentifier().getText();
  }

  @Override
  public void renderElement(LookupElementPresentation presentation) {
    presentation.setItemText(getLookupString());
    final ItemPresentation myComponentNamePresentation = myComponentName.getPresentation();
    if (myComponentNamePresentation == null) {
      return;
    }
    presentation.setIcon(myComponentNamePresentation.getIcon(true));
    final String pkg = myComponentNamePresentation.getLocationString();
    if (StringUtil.isNotEmpty(pkg)) {
      presentation.setTailText(" (" + pkg + ")", true);
    }
  }

  @NotNull
  @Override
  public Object getObject() {
    return myComponentName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof HaxeLookupElement)) return false;

    return myComponentName.equals(((HaxeLookupElement)o).myComponentName);
  }

  @Override
  public int hashCode() {
    return myComponentName.hashCode();
  }
}
