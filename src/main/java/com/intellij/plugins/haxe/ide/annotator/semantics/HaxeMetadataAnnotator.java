package com.intellij.plugins.haxe.ide.annotator.semantics;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.project.DumbAware;
import com.intellij.plugins.haxe.lang.psi.HaxeLocalFunctionDeclaration;
import com.intellij.plugins.haxe.metadata.psi.HaxeMeta;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import static com.intellij.plugins.haxe.model.HaxeCompilerMetadata.OVERLOAD;


public class HaxeMetadataAnnotator implements Annotator, DumbAware {



  @Override
  public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
    if(!element.isValid()) return;

    if (element instanceof HaxeMeta meta) {
      check(meta, holder);
    }

  }


  public void check(HaxeMeta meta, AnnotationHolder holder) {
      if(meta.isType(OVERLOAD)) {
        checkOverloadMeta(meta, holder);
      }

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
}
