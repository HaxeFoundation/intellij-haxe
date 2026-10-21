package com.intellij.plugins.haxe.util;

import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.indexes.unified.HaxeClassNameUnifiedIndex;
import com.intellij.plugins.haxe.lang.psi.indexes.unified.fqn.HaxeFullyQualifiedClassNameUnifiedIndex;
import com.intellij.plugins.haxe.model.FullyQualifiedInfo;
import com.intellij.plugins.haxe.model.HaxeBaseMemberModel;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Qualified-name-as-TEXT → PSI element lookups, shared by features that meet
 * dotted names outside code: debugger value links, string-literal navigation.
 */
public final class HaxeQnameResolveUtil {

  private HaxeQnameResolveUtil() {
  }

  /**
   * The class or member a dotted name denotes: the source-level spelling
   * first, then the RUNTIME spelling (runtime names carry no module segment —
   * modules are a compile-time concept). Call in a read action.
   */
  @Nullable
  public static PsiElement findClassOrMember(@NotNull String qname, @NotNull Project project) {
    PsiElement element = HaxeResolveUtil.findClassOrMemberByQName(qname, project);
    if (element != null) return element;
    return resolveRuntimeName(project, qname);
  }

  /**
   * Whether a dotted prefix names something the project knows: a resolvable
   * class/member, or a PACKAGE some class FQN lives under. Drives the
   * "auto-popup only once the typed prefix is real" rule for qualified-name
   * completion in strings. Call in a read action; false while dumb.
   */
  public static boolean isKnownQnamePrefix(@NotNull String prefix, @NotNull Project project) {
    if (DumbService.isDumb(project)) return false;
    if (findClassOrMember(prefix, project) != null) return true;
    String packagePrefix = prefix + ".";
    for (String fqn : HaxeFullyQualifiedClassNameUnifiedIndex.getAllKeys(project)) {
      if (fqn.startsWith(packagePrefix)) return true;
    }
    return false;
  }

  /**
   * Every class a dotted RUNTIME name denotes ({@code pkg.Cls} or
   * {@code pkg.Cls.member}: package and class, without the module that
   * declares the class), one per copy of the class the project knows, so a
   * caller can pick by location. Empty for a name that qualifies no class
   * ({@code fn@12}, {@code ?}). Call in a read action.
   */
  @NotNull
  public static List<HaxeClass> findClassesByRuntimeName(@NotNull String qname, @NotNull Project project) {
    FullyQualifiedInfo info = getRuntimeQualifiedInfo(qname);
    return info == null ? List.of() : classesNamed(project, info);
  }

  @Nullable
  private static PsiElement resolveRuntimeName(@NotNull Project project, @NotNull String value) {
    FullyQualifiedInfo info = getRuntimeQualifiedInfo(value);
    if (info == null) return null;
    List<HaxeClass> haxeClasses = classesNamed(project, info);
    if (haxeClasses.isEmpty()) return null;
    HaxeClass haxeClass = haxeClasses.getFirst();
    if (!info.hasMemberName()) {
      return haxeClass;
    }
    HaxeBaseMemberModel member = haxeClass.getModel().getMember(info.getMemberName(), null);
    return member != null ? member.getBasePsi() : null;
  }

  /** The indexed classes whose qualified name, module dropped, is the one the info names. */
  @NotNull
  private static List<HaxeClass> classesNamed(@NotNull Project project, @NotNull FullyQualifiedInfo info) {
    GlobalSearchScope scope = GlobalSearchScope.allScope(project);
    FullyQualifiedInfo wanted = info.toClassQualifiedName();
    return HaxeClassNameUnifiedIndex.getByNameFiltered(info.className, project, scope)
      .stream()
      .filter(aClass -> isRuntimeNameOf(aClass, wanted))
      .toList();
  }

  private static boolean isRuntimeNameOf(@NotNull HaxeClass aClass, @NotNull FullyQualifiedInfo wanted) {
    return wanted.getQualifiedName(false).equals(runtimeClassName(aClass));
  }

  /**
   * The class's RUNTIME name: package plus class name, without the module
   * segment PSI's qualified name inserts for a secondary class
   * ({@code pack.FileName.ClassName} is {@code pack.ClassName} to the runtime,
   * and that is what frames and type names carry). Null for a nameless class.
   */
  @Nullable
  public static String runtimeClassName(@NotNull HaxeClass haxeClass) {
    String name = haxeClass.getName();
    if (name == null || name.isEmpty()) return null;
    PsiFile file = haxeClass.getContainingFile();
    String packageName = file != null ? HaxeResolveUtil.getPackageName(file) : null;
    return packageName == null || packageName.isEmpty() ? name : packageName + "." + name;
  }

  /**
   * The classes a RUNTIME type name denotes ({@code Array<Circle>},
   * {@code openfl.display.DefaultPreloader}): type parameters dropped, a bare
   * short name matching every class of that name, a qualified one only the
   * classes whose runtime name it is. Call in a read action.
   */
  @NotNull
  public static List<HaxeClass> findRuntimeTypes(@NotNull String typeName, @NotNull Project project) {
    int typeParameters = typeName.indexOf('<');
    String bare = (typeParameters >= 0 ? typeName.substring(0, typeParameters) : typeName).trim();
    if (bare.isBlank()) return List.of();
    if (bare.indexOf('.') >= 0) return findClassesByRuntimeName(bare, project);
    return List.copyOf(HaxeClassNameUnifiedIndex.getByNameFiltered(bare, project, GlobalSearchScope.allScope(project)));
  }

  @Nullable
  private static FullyQualifiedInfo getRuntimeQualifiedInfo(@NotNull String value) {
    // Runtime does not contain module info, so it is dropped and only the class is used
    FullyQualifiedInfo info = new FullyQualifiedInfo(value);
    if (!info.hasModuleName()) return null;
    if (!info.hasClassName()) {
      info = info.withModuleName(null).withClassName(info.moduleName);
    }
    return info;
  }
}
