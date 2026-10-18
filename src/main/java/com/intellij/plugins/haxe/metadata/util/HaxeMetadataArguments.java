package com.intellij.plugins.haxe.metadata.util;

import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.fakes.HaxeSyntheticDeclarations;
import com.intellij.plugins.haxe.metadata.psi.HaxeMeta;
import com.intellij.plugins.haxe.metadata.psi.HaxeMetadataCompileTimeMeta;
import com.intellij.plugins.haxe.model.HaxeBaseMemberModel;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The declarations a compile-time metadata's arguments may name, decided once
 * for the resolver (find by name) and completion (list): {@code @:nullSafety}
 * modes, {@code @:analyzer} options and {@code @:multiType} type parameters.
 */
public final class HaxeMetadataArguments {

  /** The enum {@code @:nullSafety(...)} modes come from; the compiler needs no import for them. */
  public static final String NULL_SAFETY_MODE_QNAME = "haxe.macro.Compiler.NullSafetyMode";

  private HaxeMetadataArguments() {
  }

  /** The declarations the metadata's arguments may name; empty for metadata without a fixed value domain. */
  @NotNull
  public static List<HaxeNamedComponent> candidates(@NotNull HaxeMetadataCompileTimeMeta meta, @NotNull PsiElement context) {
    if (meta.isType(HaxeMeta.NULL_SAFETY)) {
      HaxeClass modes = HaxeResolveUtil.findClassByQName(NULL_SAFETY_MODE_QNAME, context);
      return modes == null ? List.of() : membersOf(modes.getModel());
    }
    if (meta.isType(HaxeMeta.ANALYZER)) {
      return membersOf(HaxeSyntheticDeclarations.getAnalyzerOptions(context.getProject()));
    }
    if (meta.isType(HaxeMeta.MULTI_TYPE)) {
      return typeParametersOf(annotatedAbstract(meta));
    }
    return List.of();
  }

  /** The candidate declared under {@code name}, or null. */
  @Nullable
  public static HaxeNamedComponent candidateNamed(@NotNull HaxeMetadataCompileTimeMeta meta, @NotNull PsiElement context, @NotNull String name) {
    for (HaxeNamedComponent candidate : candidates(meta, context)) {
      if (name.equals(candidate.getName())) return candidate;
    }
    return null;
  }

  @NotNull
  private static List<HaxeNamedComponent> membersOf(@Nullable HaxeClassModel model) {
    if (model == null) return List.of();
    List<HaxeNamedComponent> members = new ArrayList<>();
    for (HaxeBaseMemberModel member : model.getMembersSelf()) {
      if (member.getBasePsi() instanceof HaxeNamedComponent component) members.add(component);
    }
    return members;
  }

  /** A {@code @:multiType(T)} argument names a type parameter of the abstract the metadata annotates. */
  @NotNull
  private static List<HaxeNamedComponent> typeParametersOf(@Nullable HaxeAbstractTypeDeclaration declaration) {
    HaxeGenericParam param = declaration == null ? null : declaration.getGenericParam();
    return param == null ? List.of() : List.copyOf(param.getGenericListPartList());
  }

  /**
   * Metadata before a module's first member parses outside the module, so
   * the annotated abstract is either inside the next module or the next
   * sibling itself.
   */
  @Nullable
  private static HaxeAbstractTypeDeclaration annotatedAbstract(@NotNull HaxeMetadataCompileTimeMeta meta) {
    PsiElement metaParent = meta.getParent();
    HaxeModule module = PsiTreeUtil.getNextSiblingOfType(metaParent, HaxeModule.class);
    if (module != null) {
      return PsiTreeUtil.getChildOfType(module, HaxeAbstractTypeDeclaration.class);
    }
    return PsiTreeUtil.getNextSiblingOfType(metaParent, HaxeAbstractTypeDeclaration.class);
  }
}
