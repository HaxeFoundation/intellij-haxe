package com.intellij.plugins.haxe.util;

import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeNameSuggesterUtil {
  private HaxeNameSuggesterUtil() {
  }

  private static String deleteNonLetterFromString(@NotNull final String string) {
    Pattern pattern = Pattern.compile("[^a-zA-Z_]+");
    Matcher matcher = pattern.matcher(string);
    return matcher.replaceAll("_");
  }

  @NotNull
  public static Collection<String> generateNames(@NotNull String name) {
    name = StringUtil.decapitalize(deleteNonLetterFromString(StringUtil.unquoteString(name.replace('.', '_'))));
    if (name.startsWith("get")) {
      name = name.substring(3);
    }
    else if (name.startsWith("is")) {
      name = name.substring(2);
    }
    while (name.startsWith("_")) {
      name = name.substring(1);
    }
    while (name.endsWith("_")) {
      name = name.substring(0, name.length() - 1);
    }
    final int length = name.length();
    final Collection<String> possibleNames = new LinkedHashSet<String>();
    for (int i = 0; i < length; i++) {
      if (Character.isLetter(name.charAt(i)) &&
          (i == 0 || name.charAt(i - 1) == '_' || (Character.isLowerCase(name.charAt(i - 1)) && Character.isUpperCase(name.charAt(i))))) {
        final String candidate = StringUtil.decapitalize(name.substring(i));
        if (candidate.length() < 25) {
          possibleNames.add(candidate);
        }
      }
    }
    // prefer shorter names
    ArrayList<String> reversed = new ArrayList<String>(possibleNames);
    Collections.reverse(reversed);
    return reversed;
  }
}
