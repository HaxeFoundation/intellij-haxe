package com.intellij.plugins.haxe.util;

import com.intellij.psi.codeStyle.SuggestedNameInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Suggested names, best first, together with the description of the value
 * they are for: the kind of declaration, the property its initializer is
 * about, and its type. The chosen name is remembered under that description
 * ({@link HaxeNameStatistics}).
 */
public record HaxeSuggestedNames(@NotNull List<String> names,
                                 @NotNull HaxeNameKind kind,
                                 @Nullable String propertyName,
                                 @Nullable String typeText) {

  public static final HaxeSuggestedNames NONE = new HaxeSuggestedNames(List.of(), HaxeNameKind.VARIABLE, null, null);

  public boolean isEmpty() {
    return names.isEmpty();
  }

  @NotNull
  public String first() {
    return names.getFirst();
  }

  /** Remembers the chosen name so it ranks first the next time a similar value is named. */
  public void recordChosen(@NotNull String name) {
    HaxeNameStatistics.recordChosen(kind, propertyName, typeText, name);
  }

  /** The names in the form the platform's rename and introduce refactorings take; they report the chosen name back through it. */
  @NotNull
  public SuggestedNameInfo asInfo() {
    return new SuggestedNameInfo(names.toArray(String[]::new)) {
      @Override
      public void nameChosen(String name) {
        recordChosen(name);
      }
    };
  }
}
