package com.intellij.plugins.haxe.ide.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.plugins.haxe.ide.lookup.HaxeLookupElement;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.metadata.psi.HaxeMetadataCompileTimeMeta;
import com.intellij.plugins.haxe.metadata.util.HaxeMetadataArguments;
import com.intellij.plugins.haxe.util.HaxeAbstractForwardUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static com.intellij.patterns.PlatformPatterns.psiElement;
import static com.intellij.plugins.haxe.ide.completion.HaxeCompletionUtil.isInMetadataOfType;
import static com.intellij.plugins.haxe.ide.completion.HaxeCompletionUtil.isInReferenceChain;
import static com.intellij.plugins.haxe.lang.psi.impl.HaxeReferenceSuggestionUtil.addAbstractUnderlyingClassSuggestions;
import static com.intellij.plugins.haxe.metadata.psi.HaxeMeta.*;

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
                 PsiElement position = parameters.getPosition();
                 if (!isInReferenceChain(position)) {
                     HaxeMetadataCompileTimeMeta meta = PsiTreeUtil.getParentOfType(position, HaxeMetadataCompileTimeMeta.class);
                     if (meta != null) {
                         addArgumentCandidates(result, meta, position);
                     }
                     if (isInMetadataOfType(position, FORWARD)) {
                         addForwardMemberCompletion(result, parameters);
                     }
                 }
             }
           });
  }

    /** The metadata's argument values ({@link HaxeMetadataArguments}); analyzer options show bare, they carry no type. */
    private static void addArgumentCandidates(CompletionResultSet resultSet, HaxeMetadataCompileTimeMeta meta, PsiElement position) {
        List<HaxeNamedComponent> candidates = HaxeMetadataArguments.candidates(meta, position);
        if (candidates.isEmpty()) return;
        boolean bare = meta.isType(ANALYZER);
        for (HaxeNamedComponent candidate : candidates) {
            LookupElementBuilder lookup = HaxeLookupElementFactory.create(candidate, null);
            resultSet.addElement(bare ? lookup.withTailText(null).withTypeText(null) : lookup);
        }
        resultSet.stopHere();
    }

    private static void addForwardMemberCompletion(final CompletionResultSet resultSet,
                                                   CompletionParameters parameters) {

        PsiElement position = parameters.getPosition();
        if (HaxeAbstractForwardUtil.isElementInForwardMeta(position)) {
            HaxeReference haxeReference = PsiTreeUtil.getParentOfType(position, HaxeReference.class);
            if(haxeReference != null) {
                List<HaxeLookupElement> members = new ArrayList<>();
                addAbstractUnderlyingClassSuggestions(members, haxeReference);
                List<LookupElement> lookupElements = members.stream().map(LookupElement.class::cast).toList();
                resultSet.addAllElements(lookupElements);
                resultSet.stopHere();
            }
        }
    }
}
