/*
 * Copyright 2000-2013 JetBrains s.r.o.  (for portions copied from AnnotationHolderImpl.java)
 * Copyright 2019 Eric Bishton
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.plugins.haxe.ide.annotator;

import com.intellij.codeInsight.daemon.impl.AnnotationHolderImpl;
import com.intellij.lang.ASTNode;
import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.AnnotationSession;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.xml.util.XmlStringUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Convenience class over an AnnotationHolder that deals with
 * specific messages.
 *
 */
public class HaxeAnnotationHolder implements AnnotationHolder {
  private static final Logger LOG = Logger.getInstance("#com.intellij.plugins.haxe.ide.annotator.HaxeAnnotationHolder");
  private static final Key<HashMap<Integer, Annotation>> HAXE_ANNOTATION_KEY = new Key<>("HaxeAnnotationsKey");
  private final AnnotationHolderImpl myInternalHolder;

  public HaxeAnnotationHolder(AnnotationHolder underlyingHolder) {
    assert(underlyingHolder instanceof AnnotationHolderImpl || underlyingHolder instanceof HaxeAnnotationHolder);
    myInternalHolder = underlyingHolder instanceof HaxeAnnotationHolder
                        ? ((HaxeAnnotationHolder)underlyingHolder).myInternalHolder
                        : (AnnotationHolderImpl)underlyingHolder;
  }

  public Annotation addAnnotation(HaxeAnnotation haxeAnnotation) {
    if (null == haxeAnnotation) return null;
    Annotation preexisting = findAnnotation(haxeAnnotation.getSeverity(),
                                            haxeAnnotation.getRange(),
                                            haxeAnnotation.getMessage());
    if (null != preexisting) {
      // It would be great if we could coalesce all of the fixes up to the
      // original annotation.  We can't, because the information is all copied/
      // cloned out to other data structures and our original annotations
      // are not kept around in the outer loops (in GeneralHighlightingPass).
      // Duplicating the output is apparently incorrect, too, now.  The test
      // infrastructure will not allow multiple identical markers in the
      // same position. (It won't accept them as input!) As of IDEA 2019.1.
      return preexisting;
    }

    Annotation anno = haxeAnnotation.toAnnotation();
    rememberAnnotation(anno);
    myInternalHolder.add(anno);
    return anno;
  }

  // /////////////////////////////////////////////////////////////////////////
  //
  // All overrides below this line.
  //
  // /////////////////////////////////////////////////////////////////////////

  @Override
  public Annotation createErrorAnnotation(@NotNull PsiElement element, @Nullable String s) {
    assertMyFile(element);
    return createErrorAnnotation(element.getTextRange(), s);
  }

  @Override
  public Annotation createErrorAnnotation(@NotNull ASTNode node, @Nullable String s) {
    assertMyFile(node.getPsi());
    return createErrorAnnotation(node.getTextRange(), s);
  }

  @Override
  public Annotation createErrorAnnotation(@NotNull TextRange range, @Nullable String s) {
    return createAnnotation(HighlightSeverity.ERROR, range, s);
  }

  @Override
  public Annotation createWarningAnnotation(@NotNull PsiElement element, @Nullable String s) {
    assertMyFile(element);
    return createWarningAnnotation(element.getTextRange(), s);
  }

  @Override
  public Annotation createWarningAnnotation(@NotNull ASTNode node, @Nullable String s) {
    assertMyFile(node.getPsi());
    return createWarningAnnotation(node.getTextRange(), s);
  }

  @Override
  public Annotation createWarningAnnotation(@NotNull TextRange range, @Nullable String s) {
    return createAnnotation(HighlightSeverity.WARNING, range, s);
  }

  @Override
  public Annotation createWeakWarningAnnotation(@NotNull PsiElement element, @Nullable String s) {
    assertMyFile(element);
    return createWeakWarningAnnotation(element.getTextRange(), s);
  }

  @Override
  public Annotation createWeakWarningAnnotation(@NotNull ASTNode node, @Nullable String s) {
    assertMyFile(node.getPsi());
    return createWeakWarningAnnotation(node.getTextRange(), s);
  }

  @Override
  public Annotation createWeakWarningAnnotation(@NotNull TextRange range, @Nullable String s) {
    return createAnnotation(HighlightSeverity.WEAK_WARNING, range, s);
  }

  @Override
  public Annotation createInfoAnnotation(@NotNull PsiElement element, @Nullable String s) {
    assertMyFile(element);
    return createInfoAnnotation(element.getTextRange(), s);
  }

  @Override
  public Annotation createInfoAnnotation(@NotNull ASTNode node, @Nullable String s) {
    assertMyFile(node.getPsi());
    return createInfoAnnotation(node.getTextRange(), s);
  }

  @Override
  public Annotation createInfoAnnotation(@NotNull TextRange range, @Nullable String s) {
    return createAnnotation(HighlightSeverity.INFORMATION, range, s);
  }

  @Override
  public Annotation createAnnotation(@NotNull HighlightSeverity severity, @NotNull TextRange range, @Nullable String s) {
    @NonNls String tooltip = s == null ? null : XmlStringUtil.wrapInHtml(XmlStringUtil.escapeString(s));
    return createAnnotation(severity, range, s, tooltip);
  }

  @Override
  public Annotation createAnnotation(@NotNull HighlightSeverity severity,
                                     @NotNull TextRange range,
                                     @Nullable String message,
                                     @Nullable String tooltip) {
    Annotation annotation = findAnnotation(severity, range, message);
    if (null != annotation) {
      return annotation; // Don't add a duplicate.
    }
    annotation =  myInternalHolder.createAnnotation(severity, range, message, tooltip);
    rememberAnnotation(annotation);
    return annotation;
  }

  @Nullable
  private Annotation findAnnotation(@NotNull HighlightSeverity severity,
                                   @NotNull TextRange range,
                                   @Nullable String message) {
    HashMap<Integer, Annotation> remembered = getRememberedAnnotations();
    if (null != remembered) {
      int hashcode = generateAnnotationHash(range.getStartOffset(), range.getEndOffset(), severity, message);
      if (remembered.containsKey(hashcode)) {
        return remembered.get(hashcode);
      }
    }
    return null;
  }

  // Generates a hash code useful for us storing and finding similar annotations.
  private int generateAnnotationHash(Annotation annotation) {
    if (null == annotation) return 0;
    return generateAnnotationHash(annotation.getStartOffset(), annotation.getEndOffset(), annotation.getSeverity(), annotation.getMessage());
  }

  private int generateAnnotationHash(int startOffset, int endOffset, HighlightSeverity severity, String message) {
    return Arrays.hashCode(new Object[]{startOffset, endOffset, severity, message});
  }

  @Nullable
  private HashMap<Integer, Annotation> getRememberedAnnotations() {
    return getCurrentAnnotationSession().getUserData(HAXE_ANNOTATION_KEY);
  }

  // We stash annotations on the session in order to not duplicate them when
  // the same annotation occurs for a PsiElement and sub-element.  The
  // annotation holders are cleared (in GeneralHighlightingPass) after each
  // element is visited, so we have to track where we've been in a separate
  // data structure.
  private void rememberAnnotation(@NotNull Annotation annotation) {
    HashMap<Integer, Annotation> remembered = getRememberedAnnotations();
    if (null == remembered) {
      remembered = new HashMap<>();
      getCurrentAnnotationSession().putUserData(HAXE_ANNOTATION_KEY, remembered);
    }
    remembered.put(generateAnnotationHash(annotation), annotation);
  }

  @NotNull
  @Override
  public AnnotationSession getCurrentAnnotationSession() {
    return myInternalHolder.getCurrentAnnotationSession();
  }

  @Override
  public boolean isBatchMode() {
    return myInternalHolder.isBatchMode();
  }

  // From AnnotationHolderImpl.java
  private void assertMyFile(PsiElement node) {
    if (node == null) return;
    PsiFile myFile = getCurrentAnnotationSession().getFile();
    PsiFile containingFile = node.getContainingFile();
    LOG.assertTrue(containingFile != null, node);
    VirtualFile containingVFile = containingFile.getVirtualFile();
    VirtualFile myVFile = myFile.getVirtualFile();
    if (!Comparing.equal(containingVFile, myVFile)) {
      LOG.error(
        "Annotation must be registered for an element inside '" + myFile + "' which is in '" + myVFile + "'.\n" +
        "Element passed: '" + node + "' is inside the '" + containingFile + "' which is in '" + containingVFile + "'");
    }
  }
}
