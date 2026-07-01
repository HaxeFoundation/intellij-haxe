package com.intellij.plugins.haxe.ide.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.HaxeBaseMemberModel;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

import static com.intellij.patterns.PlatformPatterns.psiElement;
import static com.intellij.plugins.haxe.ide.completion.HaxeCommonCompletionPattern.identifierInNewExpression;
import static com.intellij.plugins.haxe.ide.completion.HaxeCompletionUtil.isInMetadataOfType;
import static com.intellij.plugins.haxe.ide.completion.HaxeCompletionUtil.isInReferenceChain;
import static com.intellij.plugins.haxe.ide.lookup.HaxeMemberLookupElement.createSynteticMember;
import static com.intellij.plugins.haxe.metadata.psi.HaxeMeta.NULL_SAFETY;
import static com.intellij.plugins.haxe.util.HaxeResolveUtil.findClassByQName;

public class HaxeMetadataCompletionContributor extends CompletionContributor {



    private static final PsiElementPattern.Capture<PsiElement> ELEMENT_CAPTURE = psiElement()
            .inside(HaxeIdentifier.class)
            .andNot(psiElement().inside(HaxeType.class))
            // current = ID token
            // parent 1: HaxeIdentifier
            // parent 2: HaxeReference
            .and(psiElement().withSuperParent(2, HaxeReferenceExpression.class));


    public HaxeMetadataCompletionContributor() {
    extend(CompletionType.BASIC, ELEMENT_CAPTURE,
           new CompletionProvider<CompletionParameters>() {
             @Override
             protected void addCompletions(@NotNull CompletionParameters parameters,
                                           ProcessingContext context,
                                           @NotNull CompletionResultSet result) {
               final PsiFile file = parameters.getOriginalFile();
               var position = parameters.getOriginalPosition();
               boolean inChain = isInReferenceChain(parameters.getPosition());
               boolean inMetadata = isInMetadataOfType(parameters.getPosition(), NULL_SAFETY);
               if(inMetadata && !inChain) {
                 position = position != null ? position : parameters.getPosition();
                 addNullSafetyCompletion(result, file,position, position.getText());
               }
             }
           });
  }

    private static void addNullSafetyCompletion(final CompletionResultSet resultSet,
                                                final PsiFile targetFile,
                                                PsiElement position,
                                                @NlsSafe String filterText) {
        final Project project = targetFile.getProject();
        final GlobalSearchScope scope = HaxeResolveUtil.getScopeForElement(targetFile);
        final PrefixMatcher matcher = resultSet.getPrefixMatcher();

        HaxeClass NullSafetyMode = findClassByQName("haxe.macro.Compiler.NullSafetyMode", position);
        if (NullSafetyMode != null) {
            List<HaxeBaseMemberModel> members = NullSafetyMode.getModel().getMembersSelf();
            for (HaxeBaseMemberModel member : members) {
                LookupElementBuilder lookup = HaxeLookupElementFactory.create(member);
                resultSet.addElement(lookup);
            }
            resultSet.stopHere();
        }
    }
}
