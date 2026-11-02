package com.intellij.plugins.haxe.model;

import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.metadata.HaxeMetadataList;
import com.intellij.plugins.haxe.metadata.psi.HaxeMeta;
import com.intellij.plugins.haxe.metadata.psi.HaxeMetadataCompileTimeMeta;
import com.intellij.plugins.haxe.metadata.psi.impl.HaxeMetadataTypeName;
import com.intellij.plugins.haxe.metadata.util.HaxeMetadataUtils;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.intellij.plugins.haxe.metadata.psi.HaxeMeta.*;

/// The metadata that lets code reach a private member from outside the
/// member's class and its subclasses:
///
/// - `@:privateAccess` on the accessing expression or on one enclosing it
///   inside the same method;
/// - `@:allow(target)` on the member or its class, where `target` names the
///   accessing package (or a parent package), module, class (or a superclass
///   of it) or method;
/// - `@:access(target)` on the accessing class or member, where `target` names
///   the member's package, module, class (or a class related to it by
///   inheritance) or the member itself.
///
/// A target may leave out the module name, as in `@:access(pack.Helper)` for
/// a class `Helper` declared in module `pack.Tools`.
public final class HaxeAccessRules {

  private HaxeAccessRules() {
  }

  /** Whether metadata grants access to the private {@code member} at {@code element}. */
  public static boolean isGrantedByMetadata(@NotNull HaxeMemberModel member, @NotNull PsiElement element) {
    if (hasPrivateAccessMeta(element)) return true;
    AccessSite site = AccessSite.of(element);
    HaxeClass memberClass = declaringClass(member);
    return isAllowedBy(member, memberClass, site) || isAccessedFrom(site, member, memberClass);
  }

  /** The class or abstract the element sits in; an anonymous structure type is no access scope of its own. */
  @Nullable
  public static HaxeClass enclosingNamedClass(@NotNull PsiElement element) {
    HaxeClass haxeClass = PsiTreeUtil.getParentOfType(element, HaxeClass.class);
    while (haxeClass != null && haxeClass.isAnonymousType()) {
      haxeClass = PsiTreeUtil.getParentOfType(haxeClass, HaxeClass.class);
    }
    return haxeClass;
  }

  /** The reference an {@code @:allow} or {@code @:access} meta names as its target. */
  @Nullable
  public static HaxeReferenceExpression accessMetaReference(@NotNull HaxeMeta metadata) {
    List<HaxeExpression> expressions = HaxeMetadataUtils.getCompileTimeExpressions(metadata.getContent());
    if (!expressions.isEmpty() && expressions.getFirst() instanceof HaxeReferenceExpression expression) {
      return expression;
    }
    return null;
  }

  private static boolean hasPrivateAccessMeta(@NotNull PsiElement element) {
    PsiElement expression = element;
    while (expression != null) {
      if (HaxeMetadataUtils.hasMeta(expression, HaxeMetadataCompileTimeMeta.class, PRIVATE_ACCESS)) return true;
      expression = expression.getParent();
      // the meta only reaches as far as the enclosing method
      if (expression instanceof HaxeMethodDeclaration || expression instanceof HaxeClass || expression instanceof HaxeModule) break;
    }
    return false;
  }

  /** {@code @:allow} on the member or its class naming the access site. */
  private static boolean isAllowedBy(@NotNull HaxeMemberModel member, @Nullable HaxeClass memberClass, @NotNull AccessSite site) {
    for (HaxeMeta metadata : collectMetadata(memberClass, member, ALLOW)) {
      for (PsiElement target : accessMetaTargets(metadata)) {
        if (isAllowTargetOf(target, site)) return true;
      }
    }
    return false;
  }

  private static boolean isAllowTargetOf(@NotNull PsiElement target, @NotNull AccessSite site) {
    return switch (target) {
      case PsiPackage aPackage -> isInPackageOrSubpackage(site.file(), aPackage.getQualifiedName());
      case HaxeModule module -> site.file() == module.getContainingFile();
      case HaxeClass aClass -> isClassOrModuleOf(site.haxeClass(), aClass);
      case HaxeMethod method -> site.member() != null && site.member().getMemberPsi() == method;
      default -> false;
    };
  }

  private static boolean isInPackageOrSubpackage(@NotNull PsiFile file, @NotNull String packageName) {
    String filePackage = HaxeResolveUtil.getPackageName(file);
    return filePackage.equals(packageName) || filePackage.startsWith(packageName + ".");
  }

  /** The accessing class is the target class or a subclass of it, or of a class in the module the target names. */
  private static boolean isClassOrModuleOf(@Nullable HaxeClass accessingClass, @NotNull HaxeClass target) {
    if (accessingClass == null) return false;
    if (accessingClass == target) return true;
    HaxeClassModel model = accessingClass.getModel();
    if (model.inheritsFrom(target)) return true;
    // a target named like its module stands for the whole module
    HaxeModule module = target.getModule();
    if (!module.getModel().getName().equals(target.getName())) return false;
    for (HaxeClass moduleClass : module.getClassDeclarationList()) {
      if (accessingClass == moduleClass || model.inheritsFrom(moduleClass)) return true;
    }
    return false;
  }

  /** {@code @:access} on the accessing class or member naming the member. */
  private static boolean isAccessedFrom(@NotNull AccessSite site, @NotNull HaxeMemberModel member, @Nullable HaxeClass memberClass) {
    for (HaxeMeta metadata : collectMetadata(site.haxeClass(), site.member(), ACCESS)) {
      for (PsiElement target : accessMetaTargets(metadata)) {
        if (isAccessTargetOf(target, member, memberClass)) return true;
      }
    }
    return false;
  }

  private static boolean isAccessTargetOf(@NotNull PsiElement target, @NotNull HaxeMemberModel member, @Nullable HaxeClass memberClass) {
    return switch (target) {
      case PsiPackage aPackage -> member.getPackage() == aPackage;
      case HaxeModule module -> member.getModule() == module;
      case HaxeClass aClass -> isRelatedClass(memberClass, aClass);
      case HaxeMethod method -> member.getMemberPsi() == method;
      case HaxePsiField field -> member.getMemberPsi() == field;
      default -> false;
    };
  }

  /** The same class, or one inheriting from the other in either direction. */
  private static boolean isRelatedClass(@Nullable HaxeClass memberClass, @NotNull HaxeClass target) {
    if (memberClass == null) return false;
    return memberClass == target || memberClass.getModel().inheritsFrom(target) || target.getModel().inheritsFrom(memberClass);
  }

  @NotNull
  private static List<PsiElement> accessMetaTargets(@NotNull HaxeMeta metadata) {
    HaxeReferenceExpression reference = accessMetaReference(metadata);
    if (reference == null) return List.of();
    PsiElement resolved = reference.resolve();
    if (resolved != null) return List.of(resolved);
    // access metas may leave the module out of a qualified name, which the resolver requires
    return resolveWithoutModule(metadata, reference, false);
  }

  @NotNull
  private static List<PsiElement> resolveWithoutModule(@NotNull HaxeMeta metadata, @NotNull HaxeReferenceExpression reference,
                                                       boolean secondPass) {
    List<PsiElement> classesWithName = new ArrayList<>();
    if (!(reference.getFirstChild() instanceof HaxeReferenceExpression packageReference)) return classesWithName;

    PsiElement packageResolved = packageReference.resolve();
    if (packageResolved instanceof PsiPackage aPackage) {
      String className = reference.getLastChild().getText();
      for (PsiFile packageFile : aPackage.getFiles(GlobalSearchScope.allScope(metadata.getProject()))) {
        if (packageFile instanceof HaxeFile haxeFile) {
          HaxeClassModel classModel = haxeFile.getModel().getClassModel(className);
          if (classModel != null) classesWithName.add(classModel.haxeClass);
        }
      }
    }
    // a member of a class not named like its module, referenced without the module
    if (!secondPass && packageResolved == null) {
      classesWithName.addAll(resolveWithoutModule(metadata, packageReference, true));
    }
    return classesWithName;
  }

  @NotNull
  private static HaxeMetadataList collectMetadata(@Nullable HaxeClass haxeClass, @Nullable HaxeMemberModel member,
                                                  @NotNull HaxeMetadataTypeName metadataType) {
    HaxeMetadataList metadata = HaxeMetadataUtils.getMetadataList(haxeClass, HaxeMetadataCompileTimeMeta.class, metadataType);
    if (member != null) {
      metadata.addAll(HaxeMetadataUtils.getMetadataList(member.getMemberPsi(), HaxeMetadataCompileTimeMeta.class, metadataType));
    }
    return metadata;
  }

  @Nullable
  private static HaxeClass declaringClass(@NotNull HaxeMemberModel member) {
    HaxeClassModel declaringClass = member.getDeclaringClass();
    return declaringClass == null ? null : declaringClass.haxeClass;
  }

  /**
   * Where an access happens: the file, and the class and member around the
   * accessing element. Completion asks from a copy of the file; the site then
   * holds the original file's class and member, which metadata targets
   * resolve to.
   */
  private record AccessSite(@NotNull PsiFile file, @Nullable HaxeClass haxeClass, @Nullable HaxeMemberModel member) {

    static AccessSite of(@NotNull PsiElement element) {
      PsiFile file = element.getContainingFile();
      HaxeClass haxeClass = enclosingNamedClass(element);
      HaxeMemberModel member = enclosingMember(element);
      PsiFile original = file.getOriginalFile();
      if (original == file) return new AccessSite(file, haxeClass, member);

      HaxeClass originalClass = haxeClass == null ? null : originalClass(original, haxeClass);
      HaxeMemberModel originalMember = member == null || originalClass == null ? null : originalMember(originalClass, member);
      return new AccessSite(original, originalClass, originalMember);
    }

    @Nullable
    private static HaxeMemberModel enclosingMember(@NotNull PsiElement element) {
      HaxeMethodDeclaration method = PsiTreeUtil.getParentOfType(element, HaxeMethodDeclaration.class);
      if (method != null) return method.getModel();
      HaxeFieldDeclaration field = PsiTreeUtil.getParentOfType(element, HaxeFieldDeclaration.class);
      return field != null && field.getModel() instanceof HaxeMemberModel model ? model : null;
    }

    @Nullable
    private static HaxeClass originalClass(@NotNull PsiFile original, @NotNull HaxeClass copy) {
      if (!(original instanceof HaxeFile haxeFile)) return null;
      HaxeClassModel model = haxeFile.getModel().getClassModel(copy.getName());
      return model == null ? null : model.haxeClass;
    }

    @Nullable
    private static HaxeMemberModel originalMember(@NotNull HaxeClass originalClass, @NotNull HaxeMemberModel copy) {
      HaxeBaseMemberModel member = originalClass.getModel().getMember(copy.getName(), null);
      return member instanceof HaxeMemberModel model ? model : null;
    }
  }
}
