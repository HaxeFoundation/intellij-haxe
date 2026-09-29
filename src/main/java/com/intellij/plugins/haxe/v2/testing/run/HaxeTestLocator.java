package com.intellij.plugins.haxe.v2.testing.run;

import com.intellij.execution.Location;
import com.intellij.execution.PsiLocation;
import com.intellij.execution.testframework.sm.runner.SMTestLocator;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.v2.testing.HaxeTestFramework;
import com.intellij.plugins.haxe.v2.testing.HaxeTestFrameworks;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Routes result-row location URLs to the frameworks: each framework resolves
 * the hints its own reporting emitted (see
 * {@link HaxeTestFramework#resolveTestLocation}) — identifier-shaped
 * {@code haxe:test} names for utest/munit/tink, file-plus-description
 * {@code haxe:buddy} hints for buddy's closure specs.
 */
public final class HaxeTestLocator implements SMTestLocator {

  public static final HaxeTestLocator INSTANCE = new HaxeTestLocator();

  private HaxeTestLocator() {
  }

  @Override
  public @NotNull List<Location> getLocation(@NotNull String protocol,
                                             @NotNull String path,
                                             @NotNull Project project,
                                             @NotNull GlobalSearchScope scope) {
    for (HaxeTestFramework framework : HaxeTestFrameworks.ALL) {
      PsiElement resolved = framework.resolveTestLocation(project, scope, protocol, path);
      if (resolved != null) {
        return List.of(PsiLocation.fromPsiElement(resolved));
      }
    }
    return List.of();
  }
}
