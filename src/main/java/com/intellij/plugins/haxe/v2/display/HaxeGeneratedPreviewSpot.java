package com.intellij.plugins.haxe.v2.display;

import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeClassBody;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Where a generated-code preview opens: the module whose dump is rendered,
 * the type to show in it, the member to put the caret on, and the name of a
 * local declared inside that member when the caret belongs on one, such as
 * the parameter a build macro wraps around an expression.
 */
public record HaxeGeneratedPreviewSpot(@NotNull String moduleDotPath,
                                       @NotNull String typeDotPath,
                                       @Nullable String memberName,
                                       @Nullable String localName) {

  /** A type by dot path, as the blueprint resolver names it; its module is looked up by the same path. */
  @NotNull
  public static HaxeGeneratedPreviewSpot ofType(@NotNull String typeDotPath, @Nullable String memberName) {
    return new HaxeGeneratedPreviewSpot(typeDotPath, typeDotPath, memberName, null);
  }

  /** A source class, with the member and local to land on; null when the class has no name. */
  @Nullable
  public static HaxeGeneratedPreviewSpot of(@NotNull HaxeClass haxeClass,
                                            @Nullable HaxeNamedComponent member,
                                            @Nullable String localName) {
    String className = haxeClass.getName();
    PsiFile file = haxeClass.getContainingFile();
    if (className == null || file == null) return null;
    String packageName = HaxeResolveUtil.getPackageName(file);
    String moduleDotPath = qualified(packageName, FileUtilRt.getNameWithoutExtension(file.getName()));
    String memberName = member == null ? null : member.getName();
    return new HaxeGeneratedPreviewSpot(moduleDotPath, qualified(packageName, className), memberName, localName);
  }

  /** The spot around a source element: its class and, when it sits in one, its member; null outside a class. */
  @Nullable
  public static HaxeGeneratedPreviewSpot around(@NotNull PsiElement element, @Nullable String localName) {
    HaxeClass haxeClass = PsiTreeUtil.getParentOfType(element, HaxeClass.class, false);
    return haxeClass == null ? null : of(haxeClass, enclosingMember(element), localName);
  }

  /** The class member (field, method, constructor) the element sits in; null for the class itself or outside its body. */
  @Nullable
  public static HaxeNamedComponent enclosingMember(@NotNull PsiElement element) {
    PsiElement member = element;
    while (member != null && !(member.getParent() instanceof HaxeClassBody)) {
      member = member.getParent();
    }
    return member instanceof HaxeNamedComponent named ? named : null;
  }

  /** What the preview is about, for popups: the local, else the member, else the type. */
  @NotNull
  public String title() {
    if (localName != null) return localName;
    return memberName != null ? memberName : typeDotPath;
  }

  @NotNull
  private static String qualified(@NotNull String packageName, @NotNull String name) {
    return packageName.isEmpty() ? name : packageName + "." + name;
  }
}
