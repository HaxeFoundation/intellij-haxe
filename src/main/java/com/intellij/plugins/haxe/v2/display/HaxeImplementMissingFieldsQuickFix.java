package com.intellij.plugins.haxe.v2.display;

import com.intellij.codeInsight.FileModificationService;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.display.protocol.MissingFields;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiParserFacade;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Adds the members a MISSING_FIELDS entry lists to the end of the class the
 * diagnostic names, with the types the compiler reported
 * ({@link HaxeMissingMemberSource}). The class is looked up by name when the
 * fix runs, so the fix survives edits above it and does nothing once the
 * class is gone. Implements both fix interfaces, because Inspect Code keeps
 * an annotation's fix only when it is a {@link LocalQuickFix}.
 */
final class HaxeImplementMissingFieldsQuickFix implements IntentionAction, LocalQuickFix {
  private final String text;
  private final String typeName;
  private final MissingFields.Entry entry;

  HaxeImplementMissingFieldsQuickFix(@NotNull String typeName, @NotNull MissingFields.Entry entry) {
    this.text = labelOf(entry);
    this.typeName = typeName;
    this.entry = entry;
  }

  @NotNull
  private static String labelOf(@NotNull MissingFields.Entry entry) {
    if (entry.isFinalFields()) return HaxeBundle.message("haxe.diagnostics.fix.create.constructor");
    if (entry.isFieldAccess() && !entry.fields().isEmpty()) {
      MissingFields.MissingField field = entry.fields().getFirst();
      String key = field.isMethod() ? "haxe.diagnostics.fix.create.function" : "haxe.diagnostics.fix.create.field";
      return HaxeBundle.message(key, field.name());
    }
    return HaxeBundle.message("haxe.diagnostics.fix.implement.members");
  }

  @Override
  public @NotNull String getText() {
    return text;
  }

  @Override
  public @NotNull String getName() {
    return text;
  }

  @Override
  public @NotNull String getFamilyName() {
    return HaxeBundle.message("haxe.diagnostics.fix.family");
  }

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
    return targetClass(file) != null;
  }

  @Override
  public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    addMembers(project, file);
  }

  @Override
  public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
    PsiElement element = descriptor.getPsiElement();
    if (element != null) {
      addMembers(project, element.getContainingFile());
    }
  }

  @Override
  public boolean startInWriteAction() {
    return true;
  }

  private void addMembers(@NotNull Project project, @NotNull PsiFile file) {
    HaxeClass target = targetClass(file);
    PsiElement body = target == null ? null : target.getModel().getBodyPsi();
    PsiElement closingBrace = body == null ? null : closingBrace(body);
    if (closingBrace == null) return;
    if (!FileModificationService.getInstance().prepareFileForWrite(file)) return;

    int firstInserted = -1;
    for (String declaration : HaxeMissingMemberSource.declarationsOf(entry)) {
      for (HaxeNamedComponent component : HaxeElementGenerator.createNamedSubComponentsFromText(project, declaration)) {
        PsiElement separator = PsiParserFacade.getInstance(project).createWhiteSpaceFromText("\n\n");
        body.addBefore(separator, closingBrace);
        PsiElement inserted = body.addBefore(component, closingBrace);
        if (firstInserted < 0) firstInserted = inserted.getTextRange().getStartOffset();
      }
    }
    if (firstInserted >= 0) {
      CodeStyleManager.getInstance(project).reformatText(file, firstInserted, closingBrace.getTextRange().getEndOffset());
    }
  }

  @Nullable
  private HaxeClass targetClass(@Nullable PsiFile file) {
    if (file == null) return null;
    for (HaxeClass candidate : PsiTreeUtil.findChildrenOfType(file, HaxeClass.class)) {
      if (typeName.equals(candidate.getName())) return candidate;
    }
    return null;
  }

  @Nullable
  private static PsiElement closingBrace(@NotNull PsiElement body) {
    ASTNode last = body.getNode().getLastChildNode();
    return last != null && last.getElementType() == HaxeTokenTypes.PRCURLY ? last.getPsi() : null;
  }
}
