package com.intellij.plugins.haxe.ide.structure;

import com.intellij.ide.structureView.StructureViewModel;
import com.intellij.ide.structureView.StructureViewModelBase;
import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeStructureViewModel extends StructureViewModelBase implements StructureViewModel.ElementInfoProvider {
  public HaxeStructureViewModel(@NotNull PsiFile psiFile) {
      super(psiFile, new HaxeStructureViewElement(psiFile));

      withSuitableClasses(HaxeNamedComponent.class, HaxeClass.class);
    }

    @Override
    public boolean isAlwaysShowsPlus(StructureViewTreeElement element) {
      return false;
    }

    @Override
    public boolean isAlwaysLeaf(StructureViewTreeElement element) {
      final Object value = element.getValue();
      return value instanceof HaxeNamedComponent && !(value instanceof HaxeClass);
    }

    @Override
    public boolean shouldEnterElement(Object element) {
      return element instanceof HaxeClass;
    }
}
