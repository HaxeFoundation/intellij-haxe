package com.intellij.plugins.haxe.ide.inspections.duplicates;

import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;
import com.intellij.plugins.haxe.ide.annotator.HaxeProblemReporter;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.*;
import java.util.*;
import com.intellij.plugins.haxe.ide.inspections.HaxeInspection;
import com.intellij.plugins.haxe.ide.inspections.hierarchy.HaxeClassInspectionUtil;

/** Members declared more than once in one class. */
public class HaxeDuplicateFieldInspection extends HaxeInspection {

  @Override
  public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
    return checkVisitor(holder, HaxeClass.class, HaxeDuplicateFieldInspection::checkDuplicateFields);
  }
  public static void checkDuplicateFields(final HaxeClass clazzPsi, final HaxeProblemReporter reporter) {
    HaxeClassModel clazz = HaxeClassInspectionUtil.modelToCheck(clazzPsi);
    if (clazz != null) checkDuplicatedFields(clazz, reporter);
  }
  static private void checkDuplicatedFields(final HaxeClassModel clazz, final HaxeProblemReporter reporter) {

    Map<String, HaxeBaseMemberModel> map = new HashMap<>();
    Set<HaxeBaseMemberModel> repeatedMembers = new HashSet<>();
    for (HaxeBaseMemberModel member : clazz.getMembersSelf()) {
      final String memberName = member.getName();
      HaxeBaseMemberModel repeatedMember = map.get(memberName);
      if (repeatedMember instanceof  HaxeMemberModel memberModel) {
        if (!memberModel.isOverload()) {
          repeatedMembers.add(member);
          repeatedMembers.add(repeatedMember);
        }
      }
      else {
        map.put(memberName, member);
      }
    }

    for (HaxeBaseMemberModel member : repeatedMembers) {
      reporter.problem(HighlightSeverity.ERROR, HaxeBundle.message("haxe.semantic.duplicate.class.field.declaration", member.getName()))
        .range(member.getNameOrBasePsi())
        .create();
    }
  }

}
