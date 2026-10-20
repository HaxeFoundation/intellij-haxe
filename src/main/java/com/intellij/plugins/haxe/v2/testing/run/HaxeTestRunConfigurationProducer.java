package com.intellij.plugins.haxe.v2.testing.run;

import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.LazyRunConfigurationProducer;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.lang.psi.HaxeMethod;
import com.intellij.plugins.haxe.v2.testing.HaxeTestClasses;
import com.intellij.plugins.haxe.v2.testing.HaxeTestContext;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * The context-menu "Run 'Tests in …'" entries: a directory, a file, a class
 * or a test method under a tests build becomes a configuration running the
 * suite classes found there (several selected files/directories are one
 * run). A location no tests build owns, or without a suite class the
 * build's framework accepts, offers nothing.
 */
public final class HaxeTestRunConfigurationProducer extends LazyRunConfigurationProducer<HaxeTestRunConfiguration> {

  /**
   * What a context selects: the owning build, the suites, an optional single
   * method, and the configuration's name - null for the generated one (the
   * configuration's own suggestedName), set where the location carries a
   * name the stored state does not (a directory, a multi-suite file).
   */
  private record Selection(@NotNull HaxeTestContext testContext,
                           @NotNull List<String> testClasses,
                           @Nullable String testMethod,
                           @Nullable String name) {
  }

  @Override
  public @NotNull ConfigurationFactory getConfigurationFactory() {
    return HaxeTestRunConfigurationType.getInstance().getFactory();
  }

  @Override
  protected boolean setupConfigurationFromContext(@NotNull HaxeTestRunConfiguration configuration,
                                                  @NotNull ConfigurationContext context,
                                                  @NotNull Ref<PsiElement> sourceElement) {
    Selection selection = select(context);
    if (selection == null) return false;
    configuration.setBuildFilePath(selection.testContext().testsBuildPath());
    if (selection.testMethod() != null) {
      configuration.setSingleRun(selection.testClasses().getFirst(), selection.testMethod());
    }
    else {
      configuration.setSingleRun(selection.testClasses());
    }
    if (selection.name() != null) {
      configuration.setName(selection.name());
    }
    else {
      configuration.setGeneratedName();
    }
    configuration.syncCompileStep();
    sourceElement.set(context.getPsiLocation());
    return true;
  }

  @Override
  public boolean isConfigurationFromContext(@NotNull HaxeTestRunConfiguration configuration, @NotNull ConfigurationContext context) {
    Selection selection = select(context);
    return selection != null
           && configuration.getBuildFilePath().equals(selection.testContext().testsBuildPath())
           && configuration.getTestClasses().equals(selection.testClasses())
           && configuration.getTestMethod().equals(Objects.requireNonNullElse(selection.testMethod(), ""));
  }

  @Nullable
  private static Selection select(@NotNull ConfigurationContext context) {
    Project project = context.getProject();
    PsiElement[] selected = PlatformCoreDataKeys.PSI_ELEMENT_ARRAY.getData(context.getDataContext());
    if (selected != null && selected.length > 1) return selectMany(project, selected);
    PsiElement location = context.getPsiLocation();
    if (location == null) return null;
    if (location instanceof PsiDirectory directory) return selectDirectory(project, directory);
    PsiFile file = location.getContainingFile();
    if (!(file instanceof HaxeFile)) return null;
    HaxeTestContext testContext = HaxeTestContext.forFile(file);
    if (testContext == null) return null;
    HaxeClass haxeClass = PsiTreeUtil.getParentOfType(location, HaxeClass.class, false);
    if (haxeClass != null && testContext.framework().isTestClass(haxeClass)) {
      return selectClass(testContext, haxeClass, location);
    }
    return selectFile(testContext, file);
  }

  @Nullable
  private static Selection selectClass(@NotNull HaxeTestContext testContext, @NotNull HaxeClass haxeClass, @NotNull PsiElement location) {
    String reference = HaxeTestClasses.templateReference(haxeClass);
    if (reference == null) return null;
    HaxeMethod method = PsiTreeUtil.getParentOfType(location, HaxeMethod.class, false);
    boolean singleTest = method != null
                         && testContext.framework().isTestMethod(method)
                         && testContext.framework().singleRunTemplate(true) != null;
    if (singleTest) {
      return new Selection(testContext, List.of(reference), method.getName(), null);
    }
    return new Selection(testContext, List.of(reference), null, null);
  }

  @Nullable
  private static Selection selectFile(@NotNull HaxeTestContext testContext, @NotNull PsiFile file) {
    List<String> suites = HaxeTestClasses.inFile(file, testContext.framework());
    if (suites.isEmpty()) return null;
    String name = suites.size() == 1 ? null : HaxeBundle.message("haxe.test.run.tests.in", file.getName());
    return new Selection(testContext, suites, null, name);
  }

  @Nullable
  private static Selection selectDirectory(@NotNull Project project, @NotNull PsiDirectory directory) {
    VirtualFile virtualDirectory = directory.getVirtualFile();
    HaxeTestContext testContext = HaxeTestContext.owning(project, virtualDirectory);
    if (testContext == null) return null;
    List<String> suites = HaxeTestClasses.underDirectory(project, virtualDirectory, testContext.framework());
    if (suites.isEmpty()) return null;
    return new Selection(testContext, suites, null, HaxeBundle.message("haxe.test.run.tests.in", virtualDirectory.getName()));
  }

  /** Several selected files/directories: every suite under any of them, all owned by the same tests build. */
  @Nullable
  private static Selection selectMany(@NotNull Project project, @NotNull PsiElement[] selected) {
    HaxeTestContext testContext = null;
    Set<String> suites = new LinkedHashSet<>();
    for (PsiElement element : selected) {
      Selection part = element instanceof PsiDirectory directory ? selectDirectory(project, directory)
                                                                  : selectFileElement(element);
      if (part == null) continue;
      if (testContext == null) testContext = part.testContext();
      if (!testContext.equals(part.testContext())) return null;
      suites.addAll(part.testClasses());
    }
    if (testContext == null) return null;
    List<String> sorted = new ArrayList<>(suites);
    sorted.sort(null);
    return new Selection(testContext, sorted, null, null);
  }

  @Nullable
  private static Selection selectFileElement(@NotNull PsiElement element) {
    if (!(element instanceof HaxeFile file)) return null;
    HaxeTestContext testContext = HaxeTestContext.forFile(file);
    return testContext == null ? null : selectFile(testContext, file);
  }
}
