package com.intellij.plugins.haxe.ide.structure;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.structureView.StructureViewModel;
import com.intellij.ide.structureView.StructureViewModelBase;
import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.structureView.impl.java.VisibilitySorter;
import com.intellij.ide.util.treeView.smartTree.*;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.PlatformIcons;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeStructureViewModel extends StructureViewModelBase implements StructureViewModel.ElementInfoProvider {
  public HaxeStructureViewModel(@NotNull PsiFile psiFile) {
    super(psiFile, new HaxeStructureViewElement(psiFile));
    withSorters(Sorter.ALPHA_SORTER, VisibilitySorter.INSTANCE);
    withSuitableClasses(HaxeNamedComponent.class, HaxeClass.class);
  }

  @Override
  public boolean isAlwaysShowsPlus(StructureViewTreeElement element) {
    return false;
  }

  @NotNull
  @Override
  public Filter[] getFilters() {
    return new Filter[]{ourFieldsFilter};
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


  private static final Filter ourFieldsFilter = new Filter() {
    @NonNls public static final String ID = "SHOW_FIELDS";

    public boolean isVisible(TreeElement treeNode) {
      if (!(treeNode instanceof HaxeStructureViewElement)) return true;
      final PsiElement element = ((HaxeStructureViewElement)treeNode).getRealElement();

      if (HaxeComponentType.typeOf(element) == HaxeComponentType.FIELD) {
        return false;
      }

      return true;
    }

    public boolean isReverted() {
      return true;
    }

    @NotNull
    public ActionPresentation getPresentation() {
      return new ActionPresentationData(
        IdeBundle.message("action.structureview.show.fields"),
        null,
        PlatformIcons.FIELD_ICON
      );
    }

    @NotNull
    public String getName() {
      return ID;
    }
  };
}
