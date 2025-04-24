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

public class HaxeUnusedVarInspection extends LocalInspectionTool {
  @NotNull
  public String getGroupDisplayName() {
    return HaxeBundle.message("inspections.group.name");
  }

  @Nls
  @NotNull
  @Override
  public String getDisplayName() {
    return HaxeBundle.message("haxe.inspections.unused.var.name");
  }

  @Override
  public boolean isEnabledByDefault() {
    return true;
  }

  @NotNull
  @Override
  public String getShortName() {
    return "HaxeUnusedVar";
  }

  @Nullable
  @Override
  public ProblemDescriptor[] checkFile(@NotNull PsiFile file, @NotNull InspectionManager manager, boolean isOnTheFly) {
    if (!(file instanceof HaxeFile)) return null;
    List<HaxeLocalVarDeclaration> unusedVarDeclarations = new ArrayList<>();
    List<HaxeFieldDeclaration> unusedFieldDeclarations = new ArrayList<>();
    new HaxeAnnotatingVisitor() {
      @Override
      public void visitFieldDeclaration(@NotNull HaxeFieldDeclaration fieldDeclaration) {
        //TODO make check for fields
        // note on fields: check if interface declaration, check if overrides parent, check references?
        // also check "@:keep" (only affects on types and members?)

        //Skipping  fields that are public or have  keep metadata
        if(fieldDeclaration.isPublic()) return;
        if(fieldDeclaration.hasMetadata(KEEP, HaxeMetadataCompileTimeMeta.class)) return;

          SearchScope searchScope = GlobalSearchScope.projectScope(fieldDeclaration.getProject());
          Collection<PsiReference> references = ReferencesSearch.search(fieldDeclaration, searchScope, false).findAll();
          if (references.isEmpty()) {
            unusedFieldDeclarations.add(fieldDeclaration);
          }
      }

      @Override
      public void visitLocalVarDeclaration(@NotNull HaxeLocalVarDeclaration varDeclaration) {
          SearchScope searchScope = HaxeExpressionEvaluatorSearchUtil.getSmallestPossibleSearchScope(varDeclaration, null);
          Collection<PsiReference> references = ReferencesSearch.search(varDeclaration, searchScope, false).findAll();
          if (references.isEmpty()) {
            unusedVarDeclarations.add(varDeclaration);
          }
      }
    }.visitFile(file);

    if (unusedVarDeclarations.isEmpty() && unusedFieldDeclarations.isEmpty()) {
      return ProblemDescriptor.EMPTY_ARRAY;
    }

    final List<ProblemDescriptor> result = new ArrayList<>();
    for (HaxeLocalVarDeclaration unusedVar : unusedVarDeclarations) {
      HaxeComponentName componentName = unusedVar.getComponentName();
      result.add(manager.createProblemDescriptor(
        componentName,
        getDisplayName(),
        new LocalQuickFix[]{createRemoveVarFix(componentName.getText())},
        ProblemHighlightType.LIKE_UNUSED_SYMBOL,
        isOnTheFly,
        false
      ));
    }

    for (HaxeFieldDeclaration unusedField : unusedFieldDeclarations) {
      HaxeComponentName componentName = unusedField.getComponentName();
      String nameText = componentName.getText();
      result.add(manager.createProblemDescriptor(
        componentName,
        getDisplayName(),
        new LocalQuickFix[]{
                createAddKeepMetaFix(nameText),
                createRemoveFieldFix(nameText)
        },
        ProblemHighlightType.LIKE_UNUSED_SYMBOL,
        isOnTheFly,
        false
      ));
    }

    return ArrayUtil.toObjectArray(result, ProblemDescriptor.class);
  }

  private LocalQuickFix createRemoveVarFix(String text) {
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
  private LocalQuickFix createRemoveFieldFix(String text) {
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
  private LocalQuickFix createAddKeepMetaFix(String text) {
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
      }
    };
  }

}
