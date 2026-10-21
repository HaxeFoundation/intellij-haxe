package com.intellij.plugins.haxe.ide.inspections.duplicates;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;
import com.intellij.plugins.haxe.ide.annotator.HaxeProblemReporter;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.*;
import com.intellij.plugins.haxe.model.fixer.HaxeFixer;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;
import java.util.*;
import static java.util.stream.Collectors.toList;
import com.intellij.plugins.haxe.ide.inspections.HaxeInspection;
import com.intellij.plugins.haxe.ide.inspections.hierarchy.HaxeClassInspectionUtil;

/** Repeated modifiers on one class declaration. */
public class HaxeDuplicateClassModifierInspection extends HaxeInspection {

  @Override
  public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
    return checkVisitor(holder, HaxeClass.class, HaxeDuplicateClassModifierInspection::checkDuplicateModifiers);
  }

  public static void checkDuplicateModifiers(final HaxeClass clazzPsi, final HaxeProblemReporter reporter) {
    HaxeClassModel clazz = HaxeClassInspectionUtil.modelToCheck(clazzPsi);
    if (clazz != null) checkModifiers(clazz, reporter);
  }

  static private void checkModifiers(final HaxeClassModel clazz, final HaxeProblemReporter reporter) {

    HaxeClassModifierList modifiers = clazz.getModifiersList();

    if (null != modifiers) {
      List<HaxeClassModifier> list = modifiers.getClassModifierList();
      checkForDuplicateModifier(reporter, "private",
                                list.stream()
                                  .filter((modifier) -> !Objects.isNull(modifier.getPrivateKeyWord()))
                                  .collect(toList()));
      checkForDuplicateModifier(reporter, "final",
                                list.stream()
                                  .filter((modifier) -> !Objects.isNull(modifier.getFinalKeyWord()))
                                  .collect(toList()));
      if (modifiers instanceof HaxeExternClassModifierList) {
        checkForDuplicateModifier(reporter, "extern", ((HaxeExternClassModifierList)modifiers).getExternKeyWordList());
      }
    }
  }

  private static void checkForDuplicateModifier(@NotNull HaxeProblemReporter reporter,
                                                @NotNull String modifier,
                                                @Nullable List<? extends PsiElement> elements) {
    if (null != elements && elements.size() > 1) {
      for (int i = 1; i < elements.size(); ++i) {
        reportDuplicateModifier(reporter, modifier, elements.get(i));
      }
    }
  }

  private static void reportDuplicateModifier(HaxeProblemReporter reporter, String modifier, final PsiElement element) {
    final HaxeDocumentModel document = HaxeDocumentModel.fromElement(element);
    String message = HaxeBundle.message("haxe.semantic.key.must.not.be.repeated.for.class.declaration", modifier);
    reporter.problem(HighlightSeverity.ERROR, message).range(element)
      .withFix(new HaxeFixer(HaxeBundle.message("haxe.quickfix.remove.duplicate", modifier)) {
        @Override
        public void run() {
          document.replaceElementText(element, "", StripSpaces.AFTER);
        }
      })
      .create();
  }

}
