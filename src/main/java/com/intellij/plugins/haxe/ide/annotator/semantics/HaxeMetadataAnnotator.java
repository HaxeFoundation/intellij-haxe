package com.intellij.plugins.haxe.ide.annotator.semantics;

import com.intellij.lang.annotation.AnnotationBuilder;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.project.DumbAware;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.lang.psi.HaxeAbstractTypeDeclaration;
import com.intellij.plugins.haxe.lang.psi.HaxeLocalFunctionDeclaration;
import com.intellij.plugins.haxe.metadata.psi.HaxeMeta;
import com.intellij.plugins.haxe.metadata.util.HaxeMetadataUtils;
import com.intellij.plugins.haxe.model.fixer.HaxeFixer;
import com.intellij.plugins.haxe.v2.compiler.HaxeLanguageLevelUtil;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import static com.intellij.plugins.haxe.model.HaxeCompilerMetadata.OVERLOAD;


public class HaxeMetadataAnnotator implements Annotator, DumbAware {

  @Override
  public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
    if (AnnotatorUtil.shouldSkip(element)) return;

    if (element instanceof HaxeMeta meta) {
      check(meta, holder);
    }

  }

  public void check(HaxeMeta meta, AnnotationHolder holder) {
      if(meta.isType(OVERLOAD)) {
        checkOverloadMeta(meta, holder);
      }
      checkDeprecatedSyntaxMeta(meta, holder);
  }

    private void checkOverloadMeta(HaxeMeta meta, AnnotationHolder holder) {
      PsiElement content = meta.getContent();
      if(content != null ){
        HaxeLocalFunctionDeclaration localFunction = PsiTreeUtil.findChildOfType(content, HaxeLocalFunctionDeclaration.class);
        if(localFunction != null) {
          PsiCodeBlock body = localFunction.getBody();
          if(body.getChildren().length > 0) {
            holder.newAnnotation(HighlightSeverity.ERROR, "Overload must only declare an empty method body {}")
                    .range(body)
                    .create();
          }
        }
      }
    }

    /**
     * The 3.4-era metadata forms with a keyword replacement
     * ({@link HaxeSyntaxMigrationFixes#DEPRECATED_METADATA}). Warned only from
     * the replacement's level: below it the metadata is the only spelling.
     */
    private static void checkDeprecatedSyntaxMeta(HaxeMeta meta, AnnotationHolder holder) {
      String oldForm = deprecatedSyntaxFormOf(meta);
      if (oldForm == null) return;
      HaxeSyntaxMigrationFixes.ApiMigration migration = HaxeSyntaxMigrationFixes.DEPRECATED_METADATA.get(oldForm);
      if (!HaxeLanguageLevelUtil.isAtLeast(meta, migration.level())) return;

      HaxeFixer fix = HaxeSyntaxMigrationFixes.modernizeMetaFix(meta);
      if (fix == null) return;
      String message = HaxeBundle.message("haxe.semantic.deprecated.since.language.level",
                                          oldForm, migration.level().getPresentableText(), migration.replacement());
      AnnotationBuilder annotation = holder.newAnnotation(HighlightSeverity.WARNING, message)
        .range(meta.getContainer())
        .withFix(fix);
      annotation.create();
    }

    /** The presentable old form when this meta is one of the replaced ones, else null. */
    private static String deprecatedSyntaxFormOf(HaxeMeta meta) {
      if (!meta.isCompileTimeMeta()) return null;
      if (meta.isType(HaxeMeta.ENUM)) {
        // @:enum is only the deprecated spelling when it declares an enum abstract
        PsiElement associated = HaxeMetadataUtils.getAssociatedElement(meta);
        return associated instanceof HaxeAbstractTypeDeclaration ? "@:enum abstract" : null;
      }
      if (meta.isType(HaxeMeta.FINAL)) return "@:final";
      if (meta.isType(HaxeMeta.EXTERN)) return "@:extern";
      return null;
    }
}
