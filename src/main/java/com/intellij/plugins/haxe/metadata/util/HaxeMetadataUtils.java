/*
 * Copyright 2020 Eric Bishton
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
package com.intellij.plugins.haxe.metadata.util;

import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.HaxeCompileTimeMetadata;
import com.intellij.plugins.haxe.lang.psi.HaxeExpression;
import com.intellij.plugins.haxe.lang.psi.HaxeMetadataContent;
import com.intellij.plugins.haxe.metadata.HaxeMetadataList;
import com.intellij.plugins.haxe.metadata.psi.HaxeMeta;
import com.intellij.plugins.haxe.metadata.psi.impl.HaxeMetadataTypeName;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.METADATA_DECLARATION;


@CustomLog
public class HaxeMetadataUtils {


  private HaxeMetadataUtils() {
  }

  /**
   * Retrieve metadata from the element, if any.  Runtime metadata can be accessed at run-time via reflection or DynamicAccess.
   *
   * @return a single entry of metadata for the element, if any matched metadata exist; null, if none present.
   */
  @Nullable
  public static HaxeMeta getMetadata(@Nullable PsiElement element,
                                     @Nullable Class<? extends HaxeMeta> clazz,
                                     @NotNull HaxeMetadataTypeName metaName) {
    if (null == element) return null;
    HaxeMetadataList list = getMetadataList(element, clazz, metaName);
    return list.isEmpty() ? null : list.get(0);
  }

  /**
   * Retrieve metadata from the element, if any.  Runtime metadata can be accessed at run-time via reflection or DynamicAccess.
   *
   * @return a list of metadata for the element, if any; empty list if none present.
   */
  @NotNull
  public static HaxeMetadataList getMetadataList(@Nullable PsiElement element) {
    return getMetadataList(element, null, null);
  }

  /**
   * Retrieve metadata from the element, if any.  Runtime metadata can be accessed at run-time via reflection or DynamicAccess.
   *
   * @return a list of metadata for the element, if any; empty list if none present.
   */
  @NotNull
  public static HaxeMetadataList getMetadataList(@Nullable PsiElement element, @Nullable Class<? extends HaxeMeta> clazz) {
    return getMetadataList(element, clazz, null);
  }

  /**
   * Retrieve metadata from the element, if any.  Runtime metadata can be accessed at run-time via reflection or DynamicAccess.
   *
   * @return a list of metadata for the element, if any; empty list if none present.
   */
  @NotNull
  public static HaxeMetadataList getMetadataList(@Nullable PsiElement element, @Nullable HaxeMetadataTypeName meta) {
    return getMetadataList(element, null, meta);
  }

  /**
   * Retrieve metadata from the element, if any.  Runtime metadata can be accessed at run-time via reflection or DynamicAccess.
   *
   * @return a list of metadata for the element, if any; empty list if none present.
   */
  @NotNull
  public static HaxeMetadataList getMetadataList(@Nullable PsiElement element,
                                                 @Nullable Class<? extends HaxeMeta> clazz,
                                                 @Nullable HaxeMetadataTypeName metaName) {
    // Runtime metadata is associated with the element that follows it, typically.  However, we don't left associate
    // it (e.g. add the meta as a child element in the PSI) because that means that *everything* then needs a getRuntimeMeta() method.
    // Instead, we will check for previous siblings that might be meta and return them.

    // Note that we want them in the list in order of appearance, but we are walking backward, so we use the stack to help us, instead
    // of reversing the array when finished, or pushing elements into the beginning of the array.
    HaxeMetadataList list = new HaxeMetadataList();
    if (null == clazz && null == metaName) {
      findPrevMeta(element, list::add);
    } else {
      findPrevMeta(element, (HaxeMeta foundMeta) -> {
        if (metaClassMatches(clazz, foundMeta)
        &&  metaTypeMatches(metaName, foundMeta)) {
          list.add(foundMeta);
        }
      } );
    }
    return list;
  }

  private static boolean metaClassMatches(@Nullable Class<? extends HaxeMeta> clazz, @Nullable HaxeMeta meta) {
    return null == clazz || clazz.isInstance(meta);
  }

  private static boolean metaTypeMatches(@Nullable HaxeMetadataTypeName meta, @Nullable HaxeMeta foundMeta) {
    return null != foundMeta && (null == meta || foundMeta.isType(meta));
  }

  @Nullable
  private static void findPrevMeta(PsiElement self, Consumer<HaxeMeta> lambda) {
    if (null == self || null == lambda) return;
    PsiElement prev = UsefulPsiTreeUtil.getPrevSiblingSkipWhiteSpacesAndComments(self, true);
    if (null != prev && METADATA_DECLARATION == prev.getNode().getElementType()) {
      findPrevMeta(prev, lambda);
      PsiElement metaElement = prev.getFirstChild();
      if (metaElement instanceof HaxeMeta) {
        lambda.accept((HaxeMeta)metaElement);
      }
    }
  }


  /**
   * Tells whether an element has *any* metadata associated with it.
   *
   * @param element which might have metadata associated with it.
   * @return true if there is metadata associated.
   */
  public static boolean hasMeta(PsiElement element) {
    PsiElement prev = UsefulPsiTreeUtil.getPrevSiblingSkipWhiteSpacesAndComments(element, true);
    return prev != null && METADATA_DECLARATION == prev.getNode().getElementType();
  }

  /**
   * Tells whether an element has a specific type of metadata associated with it, regardless of
   * the metadata type (run-time or compile-time).
   *
   * @param element which might have metadata associated with it.
   * @param meta specific metadata value to look for.
   * @return true if there is metadata of the given type associated.
   */
  public static boolean hasMeta(PsiElement element, HaxeMetadataTypeName meta) {
    return hasMeta(element, null, meta);
  }

  /**
   * Tells whether an element has a specific type of metadata associated with it.
   *
   * @param element which might have metadata associated with it.
   * @param metadataType the type of metadata to look for (runtime, compile-time, or null for either)
   * @param meta specific metadata value to look for.
   * @return true if there is metadata of the given type associated.
   */
  public static boolean hasMeta(PsiElement element, Class<? extends HaxeMeta> metadataType, HaxeMetadataTypeName meta) {
    if (null == element) return false;
    if (null == meta) return false;
    final AtomicBoolean hasMeta = new AtomicBoolean(false);
    if (null == metadataType) {
      findPrevMeta(element, (foundMeta) -> {
        if (foundMeta.isType(meta)) {
          hasMeta.set(true);
        }
      });
    } else {
      findPrevMeta(element, (foundMeta) -> {
        if (metadataType.isInstance(foundMeta) && foundMeta.isType(meta)) {
          hasMeta.set(true);
        }
      });
    }
    return hasMeta.get();
  }

  @NotNull
  public static List<HaxeExpression> getCompileTimeExpressions(@Nullable HaxeMetadataContent content) {
    List<HaxeExpression> expressions = new ArrayList<>();
    if (null != content) {
      PsiElement metaArgs = UsefulPsiTreeUtil.getChild(content, HaxeTokenTypes.METADATA_PARENTHESIS_LEFT);
      if (null != metaArgs) {
        for (PsiElement arg : metaArgs.getChildren()) {
          //TODO mlo : verify
          if (arg instanceof HaxeCompileTimeMetadata meta) {
            expressions.addAll(meta.getExpressionList());
          }
        }
      }
    }
    return null != expressions ? expressions : Collections.emptyList();
  }

  @Nullable
  public static PsiElement getAssociatedElement(HaxeMeta meta) {
    if (null == meta) return null;
    PsiElement holder = UsefulPsiTreeUtil.getParent(meta, METADATA_DECLARATION);
    return findAssociatedElement(holder);
  }

  @Nullable
  public static PsiElement getAssociatedElement(PsiElement element) {
    if (null == element) return null;
    if (element.getNode().getElementType() != METADATA_DECLARATION) {
      element = UsefulPsiTreeUtil.getParent(element, METADATA_DECLARATION);
      if (null == element) return null;
    }
    return findAssociatedElement(element);
  }

  @Nullable
  private static PsiElement findAssociatedElement(PsiElement element) {
    if (null == element) return null;
    if (element.getNode().getElementType() != METADATA_DECLARATION) {
      log.error("Internal error: Not an embedded meta.");
    }
    PsiElement next = element;
    while (null != next && next.getNode().getElementType() == METADATA_DECLARATION) {
      next = UsefulPsiTreeUtil.getNextSiblingSkipWhiteSpacesAndComments(next);
      if (next instanceof PsiFile) {
        next = null;
      }
    }
    return next;
  }

  /**
   * Get's the immediately enclosing metadata (NOT the metadata associated with this element), if any.
   *
   * @param element - the element potentially inside of a metadata content.
   * @return A metadata element for which this element is part of its content.
   */
  @Nullable
  public static HaxeMeta getEnclosingMeta(@Nullable PsiElement element) {
    return UsefulPsiTreeUtil.getParentOfType(element, HaxeMeta.class);
  }

  /**
   * Copies metadata associated with the source element to the target element.
   * Does NOT delete old metadata or remove duplicates. Does copy intermediate whitespace and comments.
   *
   * @param source PsiElement with
   * @param target
   * @return the first element that was added, or its copy.
   */
  @Nullable
  public static PsiElement copyMetadata(PsiElement source, PsiElement target) {
    final AtomicReference<PsiElement> foundFirst = new AtomicReference<>(null);
    findPrevMeta(source, meta->{if (null == foundFirst.get()) foundFirst.set(meta);});
    if (null != foundFirst.get()) {
      PsiElement first = ((HaxeMeta)foundFirst.get()).getContainer();
      PsiElement last = source.getPrevSibling();
      if (first != null && last != null) {
        return target.getParent().addRangeBefore(first, last, target);
      }
    }
    return null;
  }

}
