/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
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
package com.intellij.plugins.haxe.lang.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilderFactory;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.parser.HaxeParser;
import com.intellij.plugins.haxe.lang.psi.HaxeExpressionCodeFragment;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.SingleRootFileViewProvider;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.impl.PsiManagerEx;
import com.intellij.psi.impl.file.impl.FileManager;
import com.intellij.psi.impl.source.tree.FileElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.testFramework.LightVirtualFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Set;

import static com.intellij.lang.parser.GeneratedParserUtilBase.*;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeExpressionCodeFragmentImpl extends HaxeFile implements HaxeExpressionCodeFragment {
  // The resolve context (the PSI element at the debugger's source position) is
  // held through a SMART pointer, never as a raw element: the fragment lives as
  // long as the evaluate/watches editor, and the context's file gets reparsed
  // underneath it (a document edit, a VFS refresh racing session start). A raw
  // element dies on the first reparse, and a dead context previously turned
  // isValid() false FOREVER — the platform then threw "Invalid PSI Element ...
  // invalid context: containing file is null" when the editor touched the
  // fragment. The pointer re-anchors across reparses; if truly gone, the
  // fragment degrades to a context-less one instead of becoming invalid.
  private SmartPsiElementPointer<PsiElement> myContext;
  private FileViewProvider myViewProvider;
  private GlobalSearchScope myScope = null;
  // imports added in the evaluate window are held here, not in the fragment text,
  // so they never end up in the evaluated expression (see HaxeExpressionCodeFragment)
  private final Set<String> myImportedTypeNames = new LinkedHashSet<>();

  public HaxeExpressionCodeFragmentImpl(Project project,
                                        @NonNls String name,
                                        CharSequence text,
                                        boolean isPhysical) {
    super(new SingleRootFileViewProvider(PsiManager.getInstance(project),
                                         new LightVirtualFile(name, FileTypeManager.getInstance().getFileTypeByFileName(name), text),
                                         isPhysical) {
      @Override
      public boolean supportsIncrementalReparse(@NotNull Language rootLanguage) {
        return false;
      }
    });

    ((SingleRootFileViewProvider)getViewProvider()).forceCachedPsi(this);
    // the element type MUST be the shared singleton: every IElementType
    // construction registers permanently into a global short-indexed
    // registry, and the debugger creates fragments constantly (variable
    // hover, watches, evaluate) - a per-fragment instance exhausted the
    // registry (~9900 leaked types) and broke ALL Haxe PSI with a
    // TooManyElementTypesException.
    init(HaxeCodeFragmentElementType.INSTANCE, HaxeCodeFragmentElementType.INSTANCE);
  }


  public PsiElement getContext() {
    SmartPsiElementPointer<PsiElement> pointer = myContext;
    if (pointer == null) return null;
    PsiElement element = pointer.getElement();
    return element != null && element.isValid() ? element : null;
  }

  @NotNull
  public FileViewProvider getViewProvider() {
    if (myViewProvider != null) return myViewProvider;
    return super.getViewProvider();
  }

  public boolean isValid() {
    // deliberately independent of the context element: a reparse of the
    // context's file must degrade resolution (getContext() -> null), not
    // permanently invalidate the fragment the debugger UI is editing
    return super.isValid();
  }

  protected HaxeExpressionCodeFragmentImpl clone() {
    final HaxeExpressionCodeFragmentImpl clone = (HaxeExpressionCodeFragmentImpl)cloneImpl((FileElement)calcTreeElement().clone());
    clone.myOriginalFile = this;
    FileManager fileManager = ((PsiManagerEx)getManager()).getFileManager();
    SingleRootFileViewProvider cloneViewProvider =
      (SingleRootFileViewProvider)fileManager.createFileViewProvider(new LightVirtualFile(getName(), getLanguage(), getText()), false);
    clone.myViewProvider = cloneViewProvider;
    cloneViewProvider.forceCachedPsi(clone);
    clone.init(getContentElementType(), getContentElementType());
    clone.myImportedTypeNames.addAll(myImportedTypeNames);
    return clone;
  }


  public void setContext(PsiElement context) {
    if (context == null || !context.isValid()) {
      myContext = null;
      return;
    }
    myContext = ReadAction.compute(
      () -> SmartPointerManager.getInstance(getProject()).createSmartPsiElementPointer(context));
  }

  @Override
  public boolean importClass(String qualifiedName) {
    return myImportedTypeNames.add(qualifiedName);
  }

  @Override
  public Set<String> getImportedTypeNames() {
    return myImportedTypeNames;
  }

  @Override
  public void forceResolveScope(GlobalSearchScope scope) {
    myScope = scope;
  }

  @Override
  public GlobalSearchScope getForcedResolveScope() {
    return myScope;
  }

  private static final class HaxeCodeFragmentElementType extends IFileElementType {
    static final HaxeCodeFragmentElementType INSTANCE = new HaxeCodeFragmentElementType();

    private HaxeCodeFragmentElementType() {
      super("HAXE_CODE_FRAGMENT", HaxeLanguage.INSTANCE);
    }

    @Nullable
    @Override
    public ASTNode parseContents(final ASTNode chameleon) {
      // Initial parse: the chameleon is the fragment's own FileElement with
      // its PSI already bound. REPARSE (typing in the evaluate/watches/set-
      // value editors commits the fragment's document): the platform hands a
      // FRESH element inside a DummyHolder - it has NO psi bound, and asking
      // it would route through HaxeParserDefinition.createElement, which has
      // no case for this type ("AssertionError: Unknown element type:
      // HAXE_CODE_FRAGMENT"). The holder's psi IS bound - prefer it, exactly
      // like the Java fragment parser does.
      ASTNode holder = chameleon.getTreeParent();
      PsiElement psi = holder != null ? holder.getPsi() : chameleon.getPsi();
      return doParseContents(chameleon, psi);
    }

    @Override
    protected ASTNode doParseContents(@NotNull ASTNode chameleon, @NotNull PsiElement psi) {
      final PsiBuilderFactory factory = PsiBuilderFactory.getInstance();
      final PsiBuilder psiBuilder = factory.createBuilder(psi.getProject(), chameleon);
      final PsiBuilder builder = adapt_builder_(HaxeTokenTypes.EXPRESSION, psiBuilder, new HaxeParser(), HaxeParser.EXTENDS_SETS_);

      final PsiBuilder.Marker marker = enter_section_(builder, 0, _NONE_, "<code fragment>");
      HaxeParser.expression(builder, 1, -1);
      while (builder.getTokenType() != null) {
        builder.advanceLexer();
      }
      marker.done(HaxeTokenTypes.EXPRESSION);
      return builder.getTreeBuilt();
    }
  }
}
