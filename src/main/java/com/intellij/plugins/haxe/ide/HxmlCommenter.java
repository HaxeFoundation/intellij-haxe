package com.intellij.plugins.haxe.ide;

import com.intellij.lang.Commenter;
/**
 * @author: Fedor.Korotkov
 */
public class HxmlCommenter implements Commenter {

  public static final String SINGLE_LINE_PREFIX = "#";

  @Override
  public String getLineCommentPrefix() {
    return SINGLE_LINE_PREFIX;
  }

  @Override
  public String getBlockCommentPrefix() {
    return null;
  }

  @Override
  public String getBlockCommentSuffix() {
    return null;
  }

  @Override
  public String getCommentedBlockCommentPrefix() {
    return null;
  }

  @Override
  public String getCommentedBlockCommentSuffix() {
    return null;
  }

}
