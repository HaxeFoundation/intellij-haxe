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
package com.intellij.plugins.haxe.ide.inspections;

import com.intellij.codeInspection.*;
import com.intellij.lang.ASTNode;
import com.intellij.lang.FileASTNode;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.tree.LeafElement;
import com.intellij.psi.impl.source.tree.TreeUtil;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ebishton on 2/19/15.
 */
public class HaxePreprocessorInspection extends LocalInspectionTool {
  @NotNull
  public String getGroupDisplayName() {
    return HaxeBundle.message("inspections.group.name");
  }

  @Nls
  @NotNull
  @Override
  public String getDisplayName() {
    return HaxeBundle.message("haxe.inspection.preprocessor.symbol.long.name");
  }

  @Override
  public boolean isEnabledByDefault() {
    return true;
  }

  @NotNull
  @Override
  public String getShortName() {
    return HaxeBundle.message("haxe.inspection.preprocessor.symbol.name");
  }

  @Nullable
  @Override
  public ProblemDescriptor[] checkFile(@NotNull PsiFile file, @NotNull final InspectionManager manager, final boolean isOnTheFly) {
    final List<ProblemDescriptor> result = new ArrayList<ProblemDescriptor>();

    FileASTNode node1 = file.getNode();
    LeafElement firstLeaf = TreeUtil.findFirstLeaf(node1);

    int conditionalCount = 0;

    List<ASTNode> nodes = new ArrayList<ASTNode>();

    ASTNode leaf = firstLeaf;
    while (leaf != null) {
      IElementType leafElementType = leaf.getElementType();

      if (leafElementType.equals(HaxeTokenTypes.CONDITIONAL_STATEMENT_ID)) {
        if (leaf.getText().startsWith(HaxeTokenTypes.PPIF.toString())) {
          conditionalCount++;
        }
        nodes.add(leaf);
      }
      else if (leafElementType.equals(HaxeTokenTypes.PPEND)) {
        conditionalCount--;
        nodes.add(leaf);
      }
      else if (leafElementType.equals(HaxeTokenTypes.PPELSE) || leafElementType.equals(HaxeTokenTypes.PPELSEIF)) {
        nodes.add(leaf);
      }
      leaf = TreeUtil.nextLeaf(leaf);
    }

    if (conditionalCount != 0) {
      int currentLevel = 0;
      for (int i = 0, size = nodes.size(); i < size; i++) {
        ASTNode astNode = nodes.get(i);
        IElementType nodeType = astNode.getElementType();

        if (nodeType.equals(HaxeTokenTypes.CONDITIONAL_STATEMENT_ID)) {
          currentLevel++;
        }

        if (currentLevel <= conditionalCount || currentLevel <= 0) {
          result.add( manager.createProblemDescriptor( astNode.getPsi(),
                                                       "Unbalanced Preprocessing Directive",
                                                       (LocalQuickFix)null,
                                                       ProblemHighlightType.ERROR,
                                                       isOnTheFly));
        }

        if (nodeType.equals(HaxeTokenTypes.PPEND)) {
          currentLevel--;
        }
        //else { // (astNode.getElementType().equals(HaxeTokenTypes.PPELSEIF) || astNode.getElementType().equals(HaxeTokenTypes.PPELSE))
        //}


      }
    }

    return ArrayUtil.toObjectArray(result, ProblemDescriptor.class);
  }

}
