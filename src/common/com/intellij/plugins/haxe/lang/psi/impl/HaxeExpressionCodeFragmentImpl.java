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
import com.intellij.psi.impl.PsiManagerEx;
import com.intellij.psi.impl.file.impl.FileManager;
import com.intellij.psi.impl.source.tree.FileElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.testFramework.LightVirtualFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.lang.parser.GeneratedParserUtilBase.*;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeExpressionCodeFragmentImpl extends HaxeFile implements HaxeExpressionCodeFragment {
  private PsiElement myContext;
  private boolean myPhysical;
  private FileViewProvider myViewProvider;
  private GlobalSearchScope myScope = null;

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

    myPhysical = isPhysical;
    ((SingleRootFileViewProvider)getViewProvider()).forceCachedPsi(this);
    final MyHaxeFileElementType type = new MyHaxeFileElementType();
    init(type, type);
  }


  public PsiElement getContext() {
    return myContext;
  }

  @NotNull
  public FileViewProvider getViewProvider() {
    if (myViewProvider != null) return myViewProvider;
    return super.getViewProvider();
  }

  public boolean isValid() {
    if (!super.isValid()) return false;
    if (myContext != null && !myContext.isValid()) return false;
    return true;
  }

  protected HaxeExpressionCodeFragmentImpl clone() {
    final HaxeExpressionCodeFragmentImpl clone = (HaxeExpressionCodeFragmentImpl)cloneImpl((FileElement)calcTreeElement().clone());
    clone.myPhysical = myPhysical;
    clone.myOriginalFile = this;
    FileManager fileManager = ((PsiManagerEx)getManager()).getFileManager();
    SingleRootFileViewProvider cloneViewProvider =
      (SingleRootFileViewProvider)fileManager.createFileViewProvider(new LightVirtualFile(getName(), getLanguage(), getText()), myPhysical);
    clone.myViewProvider = cloneViewProvider;
    cloneViewProvider.forceCachedPsi(clone);
    clone.init(getContentElementType(), getContentElementType());
    return clone;
  }

  public boolean isPhysical() {
    return myPhysical;
  }

  public void setContext(PsiElement context) {
    myContext = context;
  }

  @Override
  public void forceResolveScope(GlobalSearchScope scope) {
    myScope = scope;
  }

  @Override
  public GlobalSearchScope getForcedResolveScope() {
    return myScope;
  }

  private class MyHaxeFileElementType extends IFileElementType {
    public MyHaxeFileElementType() {
      super(HaxeLanguage.INSTANCE);
    }

    @Nullable
    @Override
    public ASTNode parseContents(final ASTNode chameleon) {
      final PsiElement psi = new HaxePsiCompositeElementImpl(chameleon);
      return doParseContents(chameleon, psi);
    }

    @Override
    protected ASTNode doParseContents(@NotNull ASTNode chameleon, @NotNull PsiElement psi) {
      final PsiBuilderFactory factory = PsiBuilderFactory.getInstance();
      final PsiBuilder psiBuilder = factory.createBuilder(getProject(), chameleon);
      final PsiBuilder builder = adapt_builder_(HaxeTokenTypes.EXPRESSION, psiBuilder, new HaxeParser());

      final PsiBuilder.Marker marker = builder.mark();
      enter_section_(builder, 0, _NONE_, "<code fragment>");
      HaxeParser.expression(builder, 1);
      while (builder.getTokenType() != null) {
        builder.advanceLexer();
      }
      marker.done(HaxeTokenTypes.EXPRESSION);
      return builder.getTreeBuilt();
    }
  }
}
