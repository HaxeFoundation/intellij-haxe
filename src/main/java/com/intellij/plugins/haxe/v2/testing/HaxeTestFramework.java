package com.intellij.plugins.haxe.v2.testing;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeMethod;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A Haxe test framework the plugin can detect and run: it recognizes test
 * classes/methods in source, and supplies the compiler arguments that make a
 * tests build report TeamCity service messages on stdout — every framework's
 * results ride that one channel, through the framework's own reporter when it
 * ships none (see the reporter sources under {@code resources/testing/}).
 * One implementation per framework; a tests build's framework is picked from
 * its {@code -lib} declarations by {@link #libraryName}.
 */
public interface HaxeTestFramework {

  /** The haxelib whose presence in the tests build declares this framework. */
  @NotNull
  String libraryName();

  /** Whether the framework runs on the eval interpreter — munit predates it and hangs there. */
  boolean supportsInterp();

  /**
   * Whether the framework runs on the flash target under the IDE's adl host.
   * Needs the shipped reporter's flash shims (native-trace output plus the
   * exit call ending the adl process) — a framework without them hangs the
   * run instead of finishing.
   */
  default boolean supportsFlash() {
    return true;
  }

  /** Whether the class is a runnable test case of this framework. Detection only - false in dumb mode. */
  boolean isTestClass(@NotNull HaxeClass haxeClass);

  /** Whether the method is an individual test of this framework. Detection only - false in dumb mode. */
  boolean isTestMethod(@NotNull HaxeMethod method);

  /** The framework's extracted shipped-reporter classpath root (the value {@link #reportingArgs} takes), or null when extraction failed. */
  @Nullable
  String reporterClasspath();

  /**
   * Compiler arguments making the run report TeamCity service messages the
   * IDE's test console can parse. {@code suiteName} labels the run's root
   * suite (null for none); {@code reporterClasspath} is the extracted shipped
   * reporter's {@code -cp} root, null when extraction failed;
   * {@code liveReporting} is the Build Tools toggle — frameworks whose ONLY
   * result channel is the shipped reporter ignore it.
   */
  @NotNull
  List<String> reportingArgs(@Nullable String suiteName, @Nullable String reporterClasspath, boolean liveReporting);

  /**
   * Compiler arguments narrowing the run to tests matching the pattern
   * (utest: `-D UTEST_PATTERN=Class.method`); empty when the pattern is null
   * or the framework cannot filter.
   */
  @NotNull
  List<String> filterArgs(@Nullable String pattern);

  /**
   * The template resource (under {@code resources/testFrameworks/<lib>/})
   * whose generated main runs one suite class ({@code singleTest} false) or
   * one test method (true) — what the gutter run markers compile in place of
   * the build's own main. Null = that granularity has no gutter support:
   * class markers need the suite template, method markers the test one.
   * utest serves both from the suite template (the method rides
   * {@link #singleRunFilterArgs}); buddy has none — its specs are strings,
   * not classes/methods.
   */
  @Nullable
  default String singleRunTemplate(boolean singleTest) {
    return null;
  }

  /** Extra compiler arguments a single-TEST run adds beyond its template (utest's UTEST_PATTERN); empty by default. */
  @NotNull
  default List<String> singleRunFilterArgs(@NotNull String methodName) {
    return List.of();
  }

  /**
   * Resolves a location URL this framework's reporting emitted to the PSI
   * element a result double-click navigates to; null when the URL is not this
   * framework's or nothing matches. The default covers reporters naming tests
   * as {@code Class.method} identifiers on the {@code haxe:test} protocol
   * (utest, munit), whose hints may carry the run's tests build file as a
   * {@code ?build=} suffix to break same-name ties between sibling projects;
   * buddy and tink override with file-carrying protocols of their own.
   * Call in a read action.
   */
  @Nullable
  default PsiElement resolveTestLocation(@NotNull Project project,
                                         @NotNull GlobalSearchScope scope,
                                         @NotNull String protocol,
                                         @NotNull String path) {
    if (!HaxeTestNameLocation.PROTOCOL.equals(protocol)) return null;
    return HaxeTestNameLocation.resolveHint(path, project, scope);
  }
}
