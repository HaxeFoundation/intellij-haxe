package com.intellij.plugins.haxe.ide.refactoring;

import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets;
import com.intellij.plugins.haxe.metadata.psi.HaxeMeta;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMember;
import com.intellij.psi.impl.source.JavaDummyHolder;
import com.intellij.psi.impl.source.tree.LazyParseablePsiElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MoveUpDownUtil {


  public static void addMetadataAndDocs(List<PsiElement> list, PsiMember element, boolean deleteOriginal) {
    Collections.reverse(list);
    // insert after new member
    for (PsiElement psiElement : list) {
      element.getParent().addBefore(psiElement.copy(), element);
    }
    // remove from old member
    if (deleteOriginal) {
      for (PsiElement psiElement : list) {
        if (psiElement.getParent() != null
            && !(psiElement.getParent() instanceof JavaDummyHolder)) {
          psiElement.delete();
        }
      }
    }
  }

  public static List<PsiElement> collectRelatedDocsAndMetadata(PsiMember member) {
    List<PsiElement> psiElements = new ArrayList<PsiElement>();
    List<PsiElement> tempList = new ArrayList<PsiElement>();
    PsiElement sibling = member.getPrevSibling();
    // we want to copy all relevant metadata, comments and docs when moving a member
    // but we dont want to copy comments that might not belong so we only collect
    // elements until we dont get any  more relevant elements or we reach a different member.
    while (sibling != null && !(sibling instanceof PsiMember)) {
      tempList.add(sibling);
      if (sibling instanceof LazyParseablePsiElement lazy) {
        for (PsiElement child : lazy.getChildren()) {
          if (child instanceof HaxeMeta) {
            psiElements.addAll(tempList);
            tempList.clear();
          }
        }
      }
      if (sibling instanceof HaxeMeta) {
        psiElements.addAll(tempList);
        tempList.clear();
      }
      if (sibling instanceof PsiComment comment && comment.getTokenType() == HaxeTokenTypeSets.DOC_COMMENT) {
        psiElements.addAll(tempList);
        tempList.clear();
      }

      sibling = sibling.getPrevSibling();
    }
    return psiElements;
  }
}