package com.intellij.plugins.haxe.v2.testing;

import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.lang.psi.HaxeMethod;
import com.intellij.plugins.haxe.metadata.psi.HaxeMeta;
import com.intellij.plugins.haxe.model.HaxeMethodModel;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The tink_unittest framework: a test class carries {@code @:asserts}
 * metadata (the build macro that threads the asserts collector through); a
 * test is any public instance method of such a class.
 *
 * tink has no TeamCity reporter of its own, so the shipped
 * {@code intellij_tink} reporter IS the IDE's result channel: its build macro
 * makes the reporter the DEFAULT of {@code tink.testrunner.Runner.run}'s
 * optional reporter argument — an unmodified TestMain gets it, one passing
 * its own reporter keeps it.
 */
public final class TinkFramework implements HaxeTestFramework {

  @Override
  public @NotNull String libraryName() {
    return "tink_unittest";
  }

  @Override
  public boolean supportsInterp() {
    return true;
  }

  @Override
  public boolean isTestClass(@NotNull HaxeClass haxeClass) {
    return haxeClass.getModel().isClass()
           && haxeClass.getMetadataList(HaxeMeta.COMPILE_TIME).stream()
             .anyMatch(meta -> meta.isType("asserts"));
  }

  @Override
  public boolean isTestMethod(@NotNull HaxeMethod method) {
    HaxeMethodModel model = method.getModel();
    if (model.isConstructor() || model.isStatic() || !model.isPublic()) return false;
    HaxeClassModel declaringClass = model.getDeclaringClass();
    return declaringClass != null && isTestClass(declaringClass.haxeClass);
  }

  /** Or null when extraction fails - the run then reports to the console only. */
  @Override
  public @Nullable String reporterClasspath() {
    return HaxeTestReporterFiles.classpath(
      "/testing/tinkLiveReporter/", "tink-live-reporter",
      List.of("intellij_tink/Macro.hx", "intellij_tink/TcReporter.hx"));
  }

  @Override
  public @NotNull List<String> reportingArgs(@Nullable String suiteName,
                                             @Nullable String reporterClasspath,
                                             boolean liveReporting) {
    // the injected reporter is tink's only result channel - liveReporting cannot opt out
    return HaxeTestReporterArgs.macroReporterArgs(suiteName, reporterClasspath, "intellij_tink.Macro.init()");
  }

  @Override
  public @NotNull List<String> filterArgs(@Nullable String pattern) {
    // whole-build filtering has no tink mechanism (include/exclude are
    // per-case runtime flags); single runs go through the templates instead
    return List.of();
  }

  @Override
  public @Nullable String singleRunTemplate(boolean singleTest) {
    // the test template flips the matching case's include flag, which puts
    // the runner into include mode - everything else is skipped
    return singleTest ? "singleTest.hx" : "singleSuite.hx";
  }

  /**
   * The reporter's hints are {@code haxe:tink://<file>::<Class>.<method>}
   * with the REAL method name from PosInfos but only the SIMPLE class name
   * (tink's builder loses the package). Name resolution runs first — it
   * covers default-package and unambiguous classes — and the source file
   * pins the class down otherwise.
   */
  @Override
  public @Nullable PsiElement resolveTestLocation(@NotNull Project project,
                                                  @NotNull GlobalSearchScope scope,
                                                  @NotNull String protocol,
                                                  @NotNull String path) {
    if (!"haxe:tink".equals(protocol)) {
      return HaxeTestFramework.super.resolveTestLocation(project, scope, protocol, path);
    }
    int separator = path.indexOf("::");
    if (separator <= 0) return null;
    String fileName = path.substring(0, separator);
    String classAndMethod = path.substring(separator + 2);

    PsiElement byName = HaxeTestNameLocation.resolve(classAndMethod, project, scope);
    if (byName != null) return byName;
    return resolveThroughFile(project, scope, fileName, classAndMethod);
  }

  @Nullable
  private static PsiElement resolveThroughFile(@NotNull Project project,
                                               @NotNull GlobalSearchScope scope,
                                               @NotNull String fileName,
                                               @NotNull String classAndMethod) {
    if (!(HaxeTestFileLocation.find(project, scope, fileName) instanceof HaxeFile haxeFile)) return null;
    String className = StringUtil.getPackageName(classAndMethod);
    String methodName = StringUtil.getShortName(classAndMethod);
    for (HaxeClass haxeClass : haxeFile.getClassList()) {
      if (!className.equals(haxeClass.getName())) continue;
      for (PsiMethod method : haxeClass.getMethods()) {
        if (methodName.equals(method.getName())) return method;
      }
      return haxeClass;
    }
    return null;
  }
}
