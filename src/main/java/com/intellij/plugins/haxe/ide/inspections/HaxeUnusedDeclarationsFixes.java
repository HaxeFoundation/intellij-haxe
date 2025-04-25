package com.intellij.plugins.haxe.ide.inspections;

import com.intellij.codeInspection.*;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.ide.annotator.HaxeAnnotatingVisitor;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.metadata.psi.HaxeMetadataCompileTimeMeta;
import com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluatorSearchUtil;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiParserFacade;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.intellij.plugins.haxe.metadata.psi.HaxeMeta.KEEP;

public class HaxeUnusedDeclarationsFixes  {


  public static LocalQuickFix createRemoveVarFix(String text) {
    return new LocalQuickFix() {
      @NotNull
      @Override
      public String getName() {
        return HaxeBundle.message("haxe.inspections.unused.var.remove", text);
      }

      @NotNull
      public String getFamilyName() {
        return getName();
      }

      @Override
      public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
        PsiElement element = descriptor.getStartElement();
        if (element.getParent() instanceof  HaxeLocalVarDeclaration varDeclaration) {
          HaxeVarInit init = varDeclaration.getVarInit();
          if (varDeclaration.getParent() instanceof  HaxeLocalVarDeclarationList declarationList) {
            if(declarationList.getLocalVarDeclarationList().size() == 1) {
              //if exact 1 we can remove var, else
              if (init != null && init.getExpression() != null) {
                Collection<HaxeReference> references = PsiTreeUtil.findChildrenOfType(init, HaxeReferenceExpression.class);
                if (references.isEmpty()) {
                  declarationList.delete();
                }else {
                  PsiElement replaced = declarationList.replace(init.getExpression());
                  replaced.add(HaxeElementGenerator.createSemi(replaced.getProject()));
                }
              } else {
                declarationList.delete();
              }
            }else {
              if (init != null && init.getExpression() != null) {

                PsiElement nextLeaf = PsiTreeUtil.nextVisibleLeaf(varDeclaration);
                PsiElement prevLeaf = PsiTreeUtil.prevVisibleLeaf(varDeclaration);
                if (nextLeaf != null &&nextLeaf.textMatches(",")){
                  nextLeaf.delete();
                }else if (prevLeaf != null &&prevLeaf.textMatches(",")){
                  prevLeaf.delete();
                }

                Collection<HaxeReference> references = PsiTreeUtil.findChildrenOfType(init, HaxeReferenceExpression.class);
                if (references.isEmpty()) {
                  varDeclaration.delete();
                }else {
                  PsiElement parent = declarationList.getContainingFile();
                  PsiElement added = parent.addBefore(init.getExpression().copy(), declarationList );
                  added = parent.addAfter(HaxeElementGenerator.createSemi(init.getProject()), added);
                  parent.addAfter(PsiParserFacade.getInstance(init.getProject()).createWhiteSpaceFromText("\n"), added);
                  varDeclaration.delete();
                }
              } else {
                varDeclaration.delete();
              }
            }
          }
        }
      }
    };
  }
  public static LocalQuickFix createRemoveFieldFix(String text) {
    return new LocalQuickFix() {
      @NotNull
      @Override
      public String getName() {
        return HaxeBundle.message("haxe.inspections.unused.field.remove", text);
      }

      @NotNull
      public String getFamilyName() {
        return getName();
      }

      @Override
      public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
        PsiElement element = descriptor.getStartElement();
        if (element.getParent() instanceof HaxeFieldDeclaration fieldDeclaration) {
          fieldDeclaration.delete();
        }
      }
    };
  }
  public static LocalQuickFix createAddKeepMetaFix(String text) {
    return new LocalQuickFix() {
    @NotNull
    @Override
    public String getName() {
      return HaxeBundle.message("haxe.inspections.unused.field.keep.meta", text);
    }

    @NotNull
    public String getFamilyName() {
      return getName();
    }

      @Override
      public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
        PsiElement element = descriptor.getStartElement();
        if (element.getParent() instanceof HaxeFieldDeclaration fieldDeclaration) {
          PsiElement meta = HaxeElementGenerator.createMeta(element.getProject(), KEEP, true);
          PsiElement newLine = HaxeElementGenerator.createNewLine(element.getProject());
          fieldDeclaration.getParent().addBefore(meta, fieldDeclaration);
          fieldDeclaration.getParent().addBefore(newLine, fieldDeclaration);
        }
        else if (element.getParent() instanceof HaxeMethodDeclaration methodDeclaration) {
          PsiElement meta = HaxeElementGenerator.createMeta(element.getProject(), KEEP, true);
          PsiElement newLine = HaxeElementGenerator.createNewLine(element.getProject());
          methodDeclaration.getParent().addBefore(meta, methodDeclaration);
          methodDeclaration.getParent().addBefore(newLine, methodDeclaration);
        }
      }
    };
  }

  public static LocalQuickFix createRemoveMethodFix(String text) {
    return new LocalQuickFix() {
      @NotNull
      @Override
      public String getName() {
        return HaxeBundle.message("haxe.inspections.unused.method.remove", text);
      }

      @NotNull
      public String getFamilyName() {
        return getName();
      }

      @Override
      public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
        PsiElement element = descriptor.getStartElement();
        if (element.getParent() instanceof HaxeMethodDeclaration fieldDeclaration) {
          fieldDeclaration.delete();
        }
        if (element.getParent() instanceof HaxeLocalFunctionDeclaration fieldDeclaration) {
          fieldDeclaration.delete();
        }
      }
    };
  }
  public static LocalQuickFix createRemoveFunctionFix(String text) {
    return new LocalQuickFix() {
      @NotNull
      @Override
      public String getName() {
        return HaxeBundle.message("haxe.inspections.unused.function.remove", text);
      }

      @NotNull
      public String getFamilyName() {
        return getName();
      }

      @Override
      public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
        PsiElement element = descriptor.getStartElement();
        if (element.getParent() instanceof HaxeLocalFunctionDeclaration fieldDeclaration) {
          fieldDeclaration.delete();
        }
      }
    };
  }

}
