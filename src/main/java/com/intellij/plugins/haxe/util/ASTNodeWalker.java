/*
 * Copyright 2017 Eric Bishton
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
package com.intellij.plugins.haxe.util;

import com.intellij.lang.ASTNode;

/**
 * Depth-first visitor of an ASTNode tree.
 */
public class ASTNodeWalker {

  public ASTNodeWalker() {
  }

  /**
   * Walk the AST tree, calling the given lambda for each node.
   * Parent nodes are processed first, then their children.
   *
   * @param tree Parent AST node that we want to visit.
   * @param lambda Visitor code.  Return true to keep processing; false, to stop.
   * @return the return value of the last lambda processed.
   */
  public boolean walk(ASTNode tree, Lambda<ASTNode> lambda) {
    if (null == tree) return true;

    if (!lambda.process(tree)) {
      return false;
    }

    ASTNode[] children = tree.getChildren(null); // 2016 and later: TokenSet.ANY
    for (ASTNode child : children) {
      if (!walk(child, lambda)) {
        return false;
      }
    }
    return true;
  }
}
