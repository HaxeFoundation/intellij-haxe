package com.intellij.plugins.haxe.util;

import com.intellij.openapi.util.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeStringUtil {
  /**
   * @see com.intellij.openapi.util.text.StringUtil#getWordsWithOffset
   */
  public static List<Pair<String, Integer>> getWordsWithOffset(String s) {
    List<Pair<String, Integer>> res = new ArrayList<Pair<String, Integer>>();
    s += " ";
    StringBuilder name = new StringBuilder();
    int startInd = -1;
    for (int i = 0; i < s.length(); i++) {
      // fix
      if (!Character.isJavaIdentifierPart(s.charAt(i))) {
        if (name.length() > 0) {
          res.add(Pair.create(name.toString(), startInd));
          name.setLength(0);
          startInd = -1;
        }
      }
      else {
        if (startInd == -1) {
          startInd = i;
        }
        name.append(s.charAt(i));
      }
    }
    return res;
  }
}
