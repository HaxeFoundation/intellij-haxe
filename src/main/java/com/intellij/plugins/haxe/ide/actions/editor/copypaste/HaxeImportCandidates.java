package com.intellij.plugins.haxe.ide.actions.editor.copypaste;

import com.intellij.plugins.haxe.lang.psi.HaxeCallExpression;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.lang.psi.HaxeReferenceExpression;
import com.intellij.plugins.haxe.model.FullyQualifiedInfo;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.HaxeImportModel;
import com.intellij.plugins.haxe.model.HaxeImportableModel;
import com.intellij.plugins.haxe.model.HaxeMethodModel;
import com.intellij.plugins.haxe.model.HaxeUsingModel;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

/**
 * Which references in a copied range can need an import or using once
 * pasted elsewhere — the others are never resolved. Resolving every
 * sub-expression of a large selection (long member chains, generated
 * fields that never resolve and whose failures the resolver does not
 * cache) takes long enough for the copy to show a progress dialog. Haxe fixes the
 * shapes: type names start uppercase, so only an UNQUALIFIED uppercase
 * name can be an import candidate (a qualified one is a module path,
 * whose leftmost part is the candidate); an unqualified lowercase name is
 * a static-import usage only when the file has member imports; a
 * qualified lowercase callee is an extension-method usage only when a
 * {@code using} in scope exposes that name.
 */
final class HaxeImportCandidates {

  private final boolean staticImportsPossible;
  private final Set<String> usingMethodNames;

  private HaxeImportCandidates(boolean staticImportsPossible, Set<String> usingMethodNames) {
    this.staticImportsPossible = staticImportsPossible;
    this.usingMethodNames = usingMethodNames;
  }

  /** One scan of the file's imports and usings (import.hx included) - a handful of resolutions per copy. */
  @NotNull
  static HaxeImportCandidates of(@NotNull HaxeFile file) {
    boolean staticImports = false;
    Set<String> usingMethods = new HashSet<>();
    for (HaxeImportableModel importable : file.getModel().getOrderedImportAndUsingModels()) {
      if (importable instanceof HaxeUsingModel using) {
        addStaticMethodNames(using, usingMethods);
      }
      else if (importable instanceof HaxeImportModel importModel) {
        staticImports |= importsMembers(importModel);
      }
    }
    return new HaxeImportCandidates(staticImports, usingMethods);
  }

  /** The names a {@code using} exposes as extension methods: the static methods of the classes it names. */
  private static void addStaticMethodNames(HaxeUsingModel using, Set<String> into) {
    for (HaxeClassModel classModel : using.getClassModels()) {
      for (HaxeMethodModel method : classModel.getMethodsSelf(null)) {
        if (method.isStatic()) into.add(method.getName());
      }
    }
  }

  boolean mayNeedImport(@NotNull HaxeReferenceExpression reference) {
    String name = reference.getReferenceName();
    if (name == null || name.isEmpty()) return false;
    boolean typeName = Character.isUpperCase(name.charAt(0));
    if (!isQualified(reference)) {
      return typeName || staticImportsPossible;
    }
    return !typeName && isCallee(reference) && usingMethodNames.contains(name);
  }

  /** Anything before the name element qualifies it - a literal or call too, which getQualifier() (references only) would miss. */
  private static boolean isQualified(HaxeReferenceExpression reference) {
    return reference.getFirstChild() != reference.getReferenceNameElement();
  }

  /** {@code import pack.Class.member} and {@code import pack.Class.*} bring static members into scope; {@code import pack.Class} and {@code import pack.*} only types. */
  private static boolean importsMembers(HaxeImportModel importModel) {
    FullyQualifiedInfo info = importModel.getQualifiedInfo();
    if (info == null) return false;
    return info.memberName != null || (importModel.hasWildcard() && info.className != null);
  }

  private static boolean isCallee(HaxeReferenceExpression reference) {
    return reference.getParent() instanceof HaxeCallExpression call && call.getExpression() == reference;
  }
}
