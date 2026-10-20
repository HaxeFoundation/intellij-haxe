package com.intellij.plugins.haxe.v2.testing;

import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeMethod;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The buddy framework: a suite transitively extends {@code buddy.BuddySuite}
 * ({@code buddy.SingleSuite} chains through it) or implements the
 * {@code buddy.Buddy} main marker. Individual specs are describe/it CLOSURES
 * — there is no method PSI to detect, so {@link #isTestMethod} is always
 * false.
 *
 * buddy selects its reporter from {@code -D reporter=<fqcn>}, so the shipped
 * {@code intellij_buddy.TcReporter} rides in without macro patching. It
 * reports one TeamCity BATCH from the finished tree: buddy's per-spec
 * callback carries no suite context, and only the tree has the
 * describe-nesting, durations and per-spec captured traces. buddy has no
 * TeamCity output of its own, so the reporter is the result channel, not an
 * optional enhancement.
 */
public final class BuddyFramework implements HaxeTestFramework {

  private static final Set<String> SUITE_MARKERS = Set.of("buddy.BuddySuite", "buddy.Buddy");

  @Override
  public @NotNull String libraryName() {
    return "buddy";
  }

  @Override
  public boolean supportsInterp() {
    return true;
  }

  @Override
  public boolean supportsFlash() {
    // buddy's reporter has no flash shims - nothing would end the adl host
    return false;
  }

  @Override
  public boolean isTestClass(@NotNull HaxeClass haxeClass) {
    if (DumbService.isDumb(haxeClass.getProject())) return false;
    try {
      return haxeClass.getModel().isClass()
             && HaxeTestSupertypes.inheritsAny(haxeClass.getModel(), SUITE_MARKERS);
    }
    catch (IndexNotReadyException e) {
      // dumb mode can begin mid-walk; detection degrades to "not a test" rather than throwing
      return false;
    }
  }

  @Override
  public boolean isTestMethod(@NotNull HaxeMethod method) {
    return false;
  }

  /** Or null when extraction fails - the run then reports to the console only. */
  @Override
  public @Nullable String reporterClasspath() {
    return HaxeTestReporterFiles.classpath(
      "/testing/buddyLiveReporter/", "buddy-live-reporter",
      List.of("intellij_buddy/TcReporter.hx", "intellij_buddy/SuiteName.hx"));
  }

  @Override
  public @NotNull List<String> reportingArgs(@Nullable String suiteName,
                                             @Nullable String reporterClasspath,
                                             boolean liveReporting) {
    // without the extracted reporter there is nothing to report through -
    // the run still executes with buddy's console reporter
    if (reporterClasspath == null) return List.of();
    List<String> arguments = new ArrayList<>(HaxeTestReporterArgs.suiteNameDefine(suiteName));
    // buddy selects the reporter class itself from this define - no macro patching
    arguments.add("-D");
    arguments.add("reporter=intellij_buddy.TcReporter");
    arguments.add("-cp");
    arguments.add(reporterClasspath);
    return arguments;
  }

  @Override
  public @NotNull List<String> filterArgs(@Nullable String pattern) {
    // TODO: single-spec buddy runs - buddy filters via @include metadata in
    //  code, not a define, so they need a source-level strategy
    return List.of();
  }

  /**
   * Specs are prose-named closures with no method PSI, so the reporter's
   * hints carry the {@code it()} call site's file plus the description:
   * {@code haxe:buddy://<compile-relative file>::<description>}. Navigation
   * lands on the description STRING LITERAL in that file — the closest
   * addressable thing buddy's runtime model exposes (it drops the call
   * site's line number when building its Spec).
   */
  @Override
  public @Nullable PsiElement resolveTestLocation(@NotNull Project project,
                                                  @NotNull GlobalSearchScope scope,
                                                  @NotNull String protocol,
                                                  @NotNull String path) {
    if (!"haxe:buddy".equals(protocol)) return null;
    int separator = path.indexOf("::");
    if (separator <= 0) return null;
    String fileName = path.substring(0, separator);
    String description = path.substring(separator + 2);

    PsiFile psiFile = HaxeTestFileLocation.find(project, scope, fileName);
    if (psiFile == null) return null;
    return descriptionLiteral(psiFile, description);
  }

  @Nullable
  private static PsiElement descriptionLiteral(@NotNull PsiFile psiFile, @NotNull String description) {
    String text = psiFile.getText();
    int offset = text.indexOf('"' + description + '"');
    if (offset < 0) {
      offset = text.indexOf("'" + description + "'");
    }
    return offset >= 0 ? psiFile.findElementAt(offset + 1) : psiFile;
  }
}
