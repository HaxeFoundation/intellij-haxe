package com.intellij.plugins.haxe.ide.refactoring.rename;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.impl.StartMarkAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.plugins.haxe.HaxeRefactoringBundle;
import com.intellij.plugins.haxe.lang.psi.HaxeFieldDeclaration;
import com.intellij.plugins.haxe.model.HaxePropertyFamily;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.refactoring.rename.PsiElementRenameHandler;
import com.intellij.refactoring.rename.inplace.InplaceRefactoring;
import com.intellij.refactoring.rename.inplace.MemberInplaceRenameHandler;
import com.intellij.ui.dsl.listCellRenderer.BuilderKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/// In-place rename of a property or one of its accessors. A property declared
/// `var width(get, set)` binds the methods `get_width` and `set_width` by
/// name, so renaming one of the three alone leaves the compiler without its
/// accessors. Before the template starts, a popup asks how far the rename
/// reaches: the whole family ([HaxePropertyFamily]), overrides included, or
/// the member alone. With the family chosen, the members in the current file
/// follow the name while it is typed, and those in other files are renamed
/// when the template is committed ([HaxePropertyInplaceRenamer]). The rename
/// dialog offers the same choice through [HaxePropertyAccessorRenamerFactory].
public class HaxePropertyInplaceRenameHandler extends MemberInplaceRenameHandler {
  /** The popup's answer in tests, where no popup opens: true renames the whole family. Left unset, the rename fails rather than pick an answer silently. */
  @TestOnly
  public static Boolean renameAccessorsInTests;

  @Override
  protected boolean isAvailable(@Nullable PsiElement element, @NotNull Editor editor, @NotNull PsiFile file) {
    return editor.getSettings().isVariableInplaceRenameEnabled()
           && element instanceof PsiNameIdentifierOwner
           && HaxeRenameProcessor.canBeRenamed(element)
           && HaxePropertyFamily.isPropertyOrAccessor(element);
  }

  @Override
  public InplaceRefactoring doRename(@NotNull PsiElement elementToRename, @NotNull Editor editor, @Nullable DataContext dataContext) {
    HaxeFieldDeclaration property = HaxePropertyFamily.propertyOf(elementToRename);
    // a template already running: the platform ends it and opens the dialog
    if (property == null || StartMarkAction.canStart(editor) != null) return super.doRename(elementToRename, editor, dataContext);

    DataContext context = dataContext != null ? dataContext : DataManager.getInstance().getDataContext(editor.getComponent());
    boolean onProperty = HaxePropertyFamily.declarationOf(elementToRename) == property;
    askRenameScope(editor, property.getName(), onProperty, withAccessors -> startRename(elementToRename, editor, context, withAccessors));
    return null;
  }

  private static void startRename(@NotNull PsiElement element, @NotNull Editor editor, @NotNull DataContext context, boolean withAccessors) {
    HaxePropertyInplaceRenamer renamer = new HaxePropertyInplaceRenamer((PsiNameIdentifierOwner)element, editor, withAccessors);
    List<String> names = PsiElementRenameHandler.NAME_SUGGESTIONS.getData(context);
    if (!renamer.performInplaceRename(names)) performDialogRename(element, editor, context, renamer.initialName());
  }

  /** Asks whether the rename takes the whole family along. Tests answer through {@link #renameAccessorsInTests} instead of the popup. */
  private static void askRenameScope(@NotNull Editor editor, String propertyName, boolean onProperty, @NotNull Consumer<Boolean> onAnswer) {
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      onAnswer.accept(Objects.requireNonNull(renameAccessorsInTests, "renameAccessorsInTests answers in place of the popup"));
      return;
    }
    String titleKey = onProperty ? "rename.accessors.of.property.found" : "rename.property.of.accessor.found";
    String memberOnly = HaxeRefactoringBundle.message(onProperty ? "rename.property.only" : "rename.accessor.only");
    String withAccessors = HaxeRefactoringBundle.message("rename.property.and.accessors");
    JBPopupFactory.getInstance().createPopupChooserBuilder(List.of(withAccessors, memberOnly))
      .setTitle(HaxeRefactoringBundle.message(titleKey, propertyName))
      .setRenderer(BuilderKt.textListCellRenderer("", choice -> choice))
      .setMovable(false)
      .setResizable(false)
      .setRequestFocus(true)
      .setItemChosenCallback(choice -> onAnswer.accept(choice.equals(withAccessors)))
      .createPopup()
      .showInBestPositionFor(editor);
  }
}
