package com.intellij.plugins.haxe.ide.refactoring.rename;

import com.intellij.codeInsight.template.Expression;
import com.intellij.openapi.command.impl.StartMarkAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.lang.psi.HaxeFieldDeclaration;
import com.intellij.plugins.haxe.lang.psi.HaxeMethod;
import com.intellij.plugins.haxe.model.HaxePropertyFamily;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.refactoring.rename.RenameProcessor;
import com.intellij.refactoring.rename.inplace.MemberInplaceRenamer;
import com.intellij.refactoring.rename.inplace.MyLookupExpression;
import com.intellij.refactoring.rename.inplace.VariableInplaceRenamer;
import com.intellij.refactoring.rename.naming.AutomaticRenamerFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * The member renamer behind {@link HaxePropertyInplaceRenameHandler}. When
 * the popup chose the whole family, the template edits the property name
 * wherever the family spells it in the current file: in full in a property,
 * and after the {@code get_}/{@code set_} prefix in an accessor and its
 * calls. A rename started on an accessor works the same way: the template
 * edits only the part after its prefix, and the prefix is put back on the
 * committed name. On commit, the template's edits are rolled back and every
 * member is renamed for real, other files included. The family's own
 * automatic renamer is left out of that commit, since the popup has already
 * answered its question.
 */
class HaxePropertyInplaceRenamer extends MemberInplaceRenamer {
  /** Whether the popup chose the whole family. */
  private final boolean withAccessors;
  /** The property of the family renamed along; null when the member is renamed alone. */
  private final @Nullable HaxeFieldDeclaration property;
  /** The {@code get_}/{@code set_} prefix the template leaves alone when the rename starts on an accessor; empty otherwise. */
  private final String editedPrefix;
  /**
   * The edited member's whole old name. The inherited old name holds only
   * the edited part: the platform writes it back into every template segment
   * on revert, and each segment holds only the property name.
   */
  private final @Nullable String fullOldName;

  HaxePropertyInplaceRenamer(@NotNull PsiNameIdentifierOwner element, @NotNull Editor editor, boolean withAccessors) {
    this(element, element, editor, element.getName(), editedNameOf(element, withAccessors), withAccessors);
  }

  private HaxePropertyInplaceRenamer(@NotNull PsiNamedElement element,
                                     @Nullable PsiElement substituted,
                                     @NotNull Editor editor,
                                     @Nullable String initialName,
                                     @Nullable String oldEditedName,
                                     boolean withAccessors) {
    super(element, substituted, editor, initialName, oldEditedName);
    this.withAccessors = withAccessors;
    this.property = withAccessors ? HaxePropertyFamily.propertyOf(element) : null;
    this.editedPrefix = editedPrefixOf(element, withAccessors);
    this.fullOldName = element.getName();
  }

  String initialName() {
    return myInitialName;
  }

  @Override
  protected @NotNull VariableInplaceRenamer createInplaceRenamerToRestart(PsiNamedElement variable, Editor editor, String initialName) {
    return new HaxePropertyInplaceRenamer(variable, getSubstituted(), editor, initialName, myOldName, withAccessors);
  }

  /** The part of the edited member's name after its prefix, at the declaration. */
  @Override
  protected @NotNull TextRange getRangeToRename(@NotNull PsiElement element) {
    return new TextRange(editedPrefix.length(), element.getTextLength());
  }

  /** The part of the edited member's name after its prefix, at a reference. */
  @Override
  protected @NotNull TextRange getRangeToRename(@NotNull PsiReference reference) {
    TextRange range = super.getRangeToRename(reference);
    return new TextRange(range.getStartOffset() + editedPrefix.length(), range.getEndOffset());
  }

  /** A reference joins the template when its text after the prefix is the edited part of the name. */
  @Override
  protected boolean acceptReference(PsiReference reference) {
    String referenceText = getRangeToRename(reference).substring(reference.getElement().getText());
    return referenceText.equals(editedName());
  }

  /** The lookup at the template stop offers names for the property, since the property name is what the stop edits. */
  @Override
  protected Expression createTemplateExpression(PsiElement selectedElement) {
    if (property == null) return super.createTemplateExpression(selectedElement);
    return new MyLookupExpression(property.getName(), myNameSuggestions, property, selectedElement, shouldSelectAll(), myAdvertisementText);
  }

  /** Adds the family's other members in the current file, each edited from the end of its prefix to the end of its name. */
  @Override
  protected void collectAdditionalElementsToRename(@NotNull List<? super Pair<PsiElement, TextRange>> stringUsages) {
    super.collectAdditionalElementsToRename(stringUsages);
    HaxePropertyFamily family = property == null ? null : HaxePropertyFamily.of(property);
    PsiFile currentFile = PsiDocumentManager.getInstance(myProject).getPsiFile(myEditor.getDocument());
    if (family == null || currentFile == null) return;
    PsiElement renamed = HaxePropertyFamily.declarationOf(myElementToRename);
    for (HaxeFieldDeclaration member : family.properties()) {
      if (member != renamed) addOccurrences(stringUsages, member, 0, currentFile);
    }
    for (HaxeMethod member : family.getters()) {
      if (member != renamed) addOccurrences(stringUsages, member, HaxePropertyFamily.GETTER_PREFIX.length(), currentFile);
    }
    for (HaxeMethod member : family.setters()) {
      if (member != renamed) addOccurrences(stringUsages, member, HaxePropertyFamily.SETTER_PREFIX.length(), currentFile);
    }
  }

  /**
   * Puts the prefix back on the typed name. An unchanged name is passed on as
   * the edited part, because the platform skips a rename whose new name
   * equals its old one.
   */
  @Override
  protected void performRefactoringRename(String newName, StartMarkAction markAction) {
    String fullName = editedPrefix + newName;
    super.performRefactoringRename(fullName.equals(fullOldName) ? myOldName : fullName, markAction);
  }

  /** Renames the member with the family's other members added, and without the family's own automatic renamer. */
  @Override
  protected void performRenameInner(PsiElement element, String newName) {
    RenameProcessor processor = createRenameProcessor(element, newName);
    for (AutomaticRenamerFactory factory : AutomaticRenamerFactory.EP_NAME.getExtensionList()) {
      if (factory instanceof HaxePropertyAccessorRenamerFactory) continue;
      if (factory.getOptionName() != null && factory.isEnabled() && factory.isApplicable(element)) processor.addRenamerFactory(factory);
    }
    if (withAccessors) familyRenames(element, newName).forEach(processor::addElement);
    processor.run();
  }

  /** The member's name identifier and its references in the file, each from {@code prefixLength} to the end of the name. */
  private static void addOccurrences(@NotNull List<? super Pair<PsiElement, TextRange>> stringUsages,
                                     @NotNull PsiNameIdentifierOwner member,
                                     int prefixLength,
                                     @NotNull PsiFile file) {
    PsiElement identifier = member.getNameIdentifier();
    if (identifier != null && identifier.getContainingFile() == file) {
      stringUsages.add(Pair.create(identifier, new TextRange(prefixLength, identifier.getTextLength())));
    }
    for (PsiReference reference : ReferencesSearch.search(member, new LocalSearchScope(file)).findAll()) {
      TextRange range = reference.getRangeInElement();
      TextRange afterPrefix = new TextRange(range.getStartOffset() + prefixLength, range.getEndOffset());
      stringUsages.add(Pair.create(reference.getElement(), afterPrefix));
    }
  }

  /** The edited part of the old name: the whole old name after the edited prefix. */
  private String editedName() {
    return fullOldName == null ? "" : fullOldName.substring(editedPrefix.length());
  }

  /** The part of the member's name the template edits: after the prefix of an accessor renamed with its family, otherwise the whole name. */
  @Nullable
  private static String editedNameOf(@NotNull PsiNamedElement element, boolean withAccessors) {
    String name = element.getName();
    return name == null ? null : name.substring(editedPrefixOf(element, withAccessors).length());
  }

  @NotNull
  private static String editedPrefixOf(@NotNull PsiNamedElement element, boolean withAccessors) {
    if (!withAccessors || HaxePropertyFamily.propertyOf(element) == null) return "";
    return prefixOf(HaxePropertyFamily.declarationOf(element));
  }

  @NotNull
  private static String prefixOf(@Nullable PsiElement declaration) {
    if (!(declaration instanceof HaxeMethod accessor) || accessor.getName() == null) return "";
    if (accessor.getName().startsWith(HaxePropertyFamily.GETTER_PREFIX)) return HaxePropertyFamily.GETTER_PREFIX;
    return accessor.getName().startsWith(HaxePropertyFamily.SETTER_PREFIX) ? HaxePropertyFamily.SETTER_PREFIX : "";
  }

  @NotNull
  private static Map<PsiNamedElement, String> familyRenames(@NotNull PsiElement element, @NotNull String newName) {
    HaxePropertyFamily family = HaxePropertyFamily.of(element);
    return family == null ? Map.of() : family.renamesFor(element, newName);
  }
}
