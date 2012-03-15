package com.intellij.plugins.haxe.util;

import java.util.Iterator;
import java.util.List;

/**
 * @author: Fedor.Korotkov
 */
public class CollectionUtil {
  public static void filterListByClass(List list, Class clazz) {
    final Iterator iterator = list.iterator();
    while (iterator.hasNext()) {
      if (clazz.isInstance(iterator.next())) {
        iterator.remove();
      }
    }
  }
}
