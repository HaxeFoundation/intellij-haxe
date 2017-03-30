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
import java.util.Stack;

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
    final ProblemReporter reporter = new ProblemReporter() {
      @Override
      public void reportProblem(ASTNode node, String message) {
        result.add( manager.createProblemDescriptor( node.getPsi(),
                                                     message,
                                                     (LocalQuickFix)null,
                                                     ProblemHighlightType.ERROR,
                                                     isOnTheFly));
      }
    };

    FileASTNode node1 = file.getNode();
    LeafElement firstLeaf = TreeUtil.findFirstLeaf(node1);

    int conditionalCount = 0;

    Stack<List<ASTNode>> levels = new Stack<List<ASTNode>>();
    List<ASTNode> nodes = new ArrayList<ASTNode>();
    // Push the root node, just in case there is no #if to start it off.
    levels.push(nodes);

    ASTNode leaf = firstLeaf;
    while (leaf != null) {
      IElementType leafElementType = leaf.getElementType();

      if (leafElementType.equals(HaxeTokenTypes.PPIF)) {
        conditionalCount++;
        nodes = new ArrayList<ASTNode>();
        levels.push(nodes);
        nodes.add(leaf);
      }
      else if (leafElementType.equals(HaxeTokenTypes.PPEND)) {
        conditionalCount--;
        nodes.add(leaf);
        // Leave the base level in place, even if there are extra ends.
        if (levels.size() > 1) {
          validateLevel(nodes, reporter);
          levels.pop();
          nodes = levels.peek();
        }
      }
      else if (leafElementType.equals(HaxeTokenTypes.PPELSE) || leafElementType.equals(HaxeTokenTypes.PPELSEIF)) {
        nodes.add(leaf);
      }
      leaf = TreeUtil.nextLeaf(leaf);
    }

    // Any levels that are still left need to be validated.
    for (List<ASTNode> level : levels) {
      validateLevel(level, reporter);
    }

    return ArrayUtil.toObjectArray(result, ProblemDescriptor.class);
  }

  private void validateLevel(List<ASTNode> nodes, @NotNull ProblemReporter reporter) {

    if (nodes.isEmpty()) {
      return;
    }

    // #if better be first.  #end better be last.
    ASTNode node = nodes.get(0);
    if (node.getElementType() != HaxeTokenTypes.PPIF) {
      markAllNodes(nodes, "Missing #\bif for this conditional compiler directive.", reporter);
    }
    node = nodes.get(nodes.size()-1);
    if (node.getElementType() != HaxeTokenTypes.PPEND) {
      markAllNodes(nodes, "Missing #\bend for this conditional compiler directive.", reporter);
    }

    node = null;
    boolean sawElse = false;
    boolean sawEnd = false;
    for (ASTNode current : nodes) {
      if (current.getElementType() == HaxeTokenTypes.PPELSEIF) {
        if (sawElse) {
          reporter.reportProblem(current, "#\belseif follows #\belse.  This conditional section cannot be reached.");
        }
        // Could also detect multiple elseif blocks with the same or similar conditions.
      } else if (current.getElementType() == HaxeTokenTypes.PPELSE) {
        if (sawElse) {
          reporter.reportProblem(current, "Multiple #\belse sections for this compiler conditional.  This section cannot be reached.");
        }
        sawElse = true;
      } else if (current.getElementType() == HaxeTokenTypes.PPEND) {
        if (sawEnd) {
          reporter.reportProblem(current, "Duplicate #\bend. Missing #\bif for this conditional compiler directive?");
        }
        sawEnd = true;
      }
      node = current;
    }
  }

  private void markAllNodes(@NotNull List<ASTNode> nodes, @NotNull String message, @NotNull ProblemReporter reporter) {
    for (ASTNode node : nodes) {
      reporter.reportProblem(node, message);
    }
  }


  private abstract static class ProblemReporter {
    public abstract void reportProblem(ASTNode node, String message);
  }

}
