package com.intellij.plugins.haxe.ide.structure;

import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.util.treeView.smartTree.TreeElement;
import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.NavigationItem;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.plugins.haxe.lang.psi.impl.AbstractHaxeNamedComponent;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeStructureViewElement implements StructureViewTreeElement {
  private final PsiElement myElement;

  public HaxeStructureViewElement(final PsiElement element) {
    myElement = element;
  }

  @Override
  public Object getValue() {
    return myElement;
  }

  @Override
  public void navigate(boolean requestFocus) {
    if(myElement instanceof NavigationItem){
      ((NavigationItem)myElement).navigate(requestFocus);
    }
  }

  @Override
  public boolean canNavigate() {
    return myElement instanceof NavigationItem && ((NavigationItem)myElement).canNavigate();
  }

  @Override
  public boolean canNavigateToSource() {
    return myElement instanceof NavigationItem && ((NavigationItem)myElement).canNavigateToSource();
  }

  @Nullable
  @Override
  public ItemPresentation getPresentation() {
    return myElement instanceof NavigationItem ? ((NavigationItem)myElement).getPresentation() : null;
  }

  @Override
  public TreeElement[] getChildren() {
    final List<TreeElement> result = new ArrayList<TreeElement>();
    if(myElement instanceof HaxeFile){
      for (HaxeNamedComponent subNamedComponent : HaxeResolveUtil.findComponentDeclarations((PsiFile)myElement)){
        result.add(new HaxeStructureViewElement(subNamedComponent));
      }
    } else if(myElement instanceof HaxeClass){
      final HaxeClass haxeClass = (HaxeClass)myElement;
      for (HaxeNamedComponent subNamedComponent : HaxeResolveUtil.findNamedSubComponents(haxeClass)){
        result.add(new HaxeStructureViewElement(subNamedComponent));
      }
    }
    return result.toArray(new TreeElement[result.size()]);
  }
}
