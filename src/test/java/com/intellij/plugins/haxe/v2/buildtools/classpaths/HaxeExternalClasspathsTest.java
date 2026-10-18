package com.intellij.plugins.haxe.v2.buildtools.classpaths;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.GeneralModuleType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.model.FullyQualifiedInfo;
import com.intellij.plugins.haxe.model.HaxeModel;
import com.intellij.plugins.haxe.model.HaxeProjectModel;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFile;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileType;
import com.intellij.plugins.haxe.v2.buildtools.classpaths.HaxeExternalClasspaths.ExternalClasspath;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiSearchHelper;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.PsiTestUtil;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Build-file classpaths outside the module's own sources: which folders
 * count, and that the module's files resolve and are searched through them
 * without any module dependency. The fixture module's content holds a nested
 * module "shared"; a temporary folder outside every module stands in for a
 * checkout beside the project.
 */
@DisplayName("Build tools: external classpaths")
public class HaxeExternalClasspathsTest extends HaxeCodeInsightFixtureTestCase {

  private static final String SHARED_MODULE_NAME = "shared";
  private static final String SHARED_SOURCES_PATH = SHARED_MODULE_NAME + "/src";
  private static final String PLATFORM_HX_NAME = SHARED_SOURCES_PATH + "/shared/Platform.hx";
  private static final String PLATFORM_HX_SOURCE = """
    package shared;
    class Platform {}
    """;
  private static final String MAIN_HX_NAME = "src/Main.hx";
  private static final String MAIN_HX_SOURCE = """
    import shared.Platform;
    class Main {}
    """;

  private final List<File> outsideDirectories = new ArrayList<>();

  @Override
  protected String getBasePath() {
    return "";
  }

  /** The fixture module's sources sit in src/: a source root holding the nested module would reach its files directly. */
  @Override
  protected boolean toAddSourceRoot() {
    return false;
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    PsiTestUtil.addSourceRoot(myFixture.getModule(), myFixture.getTempDirFixture().findOrCreateDir("src"));
    // the library sync a project open starts would replace the classpaths a test publishes
    awaitLibrarySync();
  }

  @Override
  protected void tearDown() throws Exception {
    try {
      outsideDirectories.forEach(FileUtil::delete);
    }
    finally {
      super.tearDown();
    }
  }

  @Test
  @DisplayName("a classpath in another modules content is external")
  public void testAClasspathInAnotherModulesContentIsExternal() throws IOException {
    addSharedModule();
    HaxeBuildFile buildFile = hxmlBuild("-cp src\n-cp " + SHARED_SOURCES_PATH + "\n-main Main\n");

    List<ExternalClasspath> classpaths = externalClasspathsOf(myFixture.getModule(), buildFile);

    assertEquals(List.of(new ExternalClasspath(sharedSources(), false)), classpaths);
  }

  @Test
  @DisplayName("a classpath outside every module is external to all modules")
  public void testAClasspathOutsideEveryModuleIsExternalToAllModules() throws IOException {
    VirtualFile outside = outsideDirectory("haxe-external-classpath");
    HaxeBuildFile buildFile = hxmlBuild("-cp " + outside.getPath() + "\n-main Main\n");

    List<ExternalClasspath> classpaths = externalClasspathsOf(myFixture.getModule(), buildFile);

    assertEquals(List.of(new ExternalClasspath(outside, true)), classpaths);
  }

  @Test
  @DisplayName("a classpath holding a modules content root is not external")
  public void testAClasspathHoldingAModulesContentRootIsNotExternal() throws IOException {
    VirtualFile outside = outsideDirectory("haxe-external-parent");
    VirtualFile nested = createSubdirectory(outside, "nested");
    addModule("nested", nested, nested);
    HaxeBuildFile buildFile = hxmlBuild("-cp " + outside.getPath() + "\n-main Main\n");

    List<ExternalClasspath> classpaths = externalClasspathsOf(myFixture.getModule(), buildFile);

    assertEquals(List.of(), classpaths);
  }

  /** Both modules mark shared/src as a source root; the platform hands its files to one of them, the other still owns it. */
  @Test
  @DisplayName("a source root another module marks too stays the modules own")
  public void testASourceRootAnotherModuleMarksTooStaysTheModulesOwn() throws IOException {
    Module shared = addSharedModule();
    VirtualFile sharedSources = sharedSources();
    PsiTestUtil.addSourceRoot(myFixture.getModule(), sharedSources);
    HaxeBuildFile buildFile = hxmlBuild("-cp " + sharedSources.getPath() + "\n-main Main\n");
    Module owner = ReadAction.computeBlocking(() -> ProjectFileIndex.getInstance(getProject()).getModuleForFile(sharedSources));
    Module otherMarker = owner == shared ? myFixture.getModule() : shared;

    List<ExternalClasspath> classpaths = externalClasspathsOf(otherMarker, buildFile);

    assertEquals(List.of(), classpaths);
  }

  @Test
  @DisplayName("an import through an external classpath resolves once published")
  public void testAnImportThroughAnExternalClasspathResolvesOncePublished() throws IOException {
    addSharedModule();
    myFixture.addFileToProject(PLATFORM_HX_NAME, PLATFORM_HX_SOURCE);
    PsiFile main = myFixture.addFileToProject(MAIN_HX_NAME, MAIN_HX_SOURCE);
    HaxeBuildFile buildFile = hxmlBuild("-cp " + SHARED_SOURCES_PATH + "\n-main Main\n");

    assertEquals(List.of(), resolveFrom(main, "shared.Platform"), "the nested module is out of the fixture module's reach");

    publish(myFixture.getModule(), buildFile);
    assertEquals(1, resolveFrom(main, "shared.Platform").size(), "the external classpath brings the shared class into reach");
  }

  @Test
  @DisplayName("an outside classpath is indexed as a library once published")
  public void testAnOutsideClasspathIsIndexedAsALibraryOncePublished() throws IOException {
    VirtualFile outside = outsideDirectory("haxe-external-library");
    HaxeBuildFile buildFile = hxmlBuild("-cp " + outside.getPath() + "\n-main Main\n");

    publish(myFixture.getModule(), buildFile);

    boolean inLibrary = ReadAction.computeBlocking(() -> ProjectFileIndex.getInstance(getProject()).isInLibrarySource(outside));
    assertTrue(inLibrary, "a folder outside every module becomes a synthetic library");
  }

  @Test
  @DisplayName("a package under an outside classpath resolves once published")
  public void testAPackageUnderAnOutsideClasspathResolvesOncePublished() throws IOException {
    VirtualFile outside = outsideDirectory("haxe-external-package");
    createSubdirectory(outside, "extutil");
    HaxeBuildFile buildFile = hxmlBuild("-cp " + outside.getPath() + "\n-main Main\n");
    JavaPsiFacade facade = JavaPsiFacade.getInstance(getProject());

    assertNull(ReadAction.computeBlocking(() -> facade.findPackage("extutil")), "no package before the folder is published");

    publish(myFixture.getModule(), buildFile);
    assertNotNull(ReadAction.computeBlocking(() -> facade.findPackage("extutil")), "the folder's subfolder is a package");
  }

  @Test
  @DisplayName("a declaration under an external classpath is used by the module reaching it")
  public void testADeclarationUnderAnExternalClasspathIsUsedByTheModuleReachingIt() throws IOException {
    addSharedModule();
    PsiFile platform = myFixture.addFileToProject(PLATFORM_HX_NAME, PLATFORM_HX_SOURCE);
    PsiFile main = myFixture.addFileToProject(MAIN_HX_NAME, MAIN_HX_SOURCE);
    publish(myFixture.getModule(), hxmlBuild("-cp " + SHARED_SOURCES_PATH + "\n-main Main\n"));

    HaxeClass platformClass = PsiTreeUtil.findChildOfType(platform, HaxeClass.class);
    assertNotNull(platformClass, "the shared file declares a class");
    SearchScope useScope = ReadAction.computeBlocking(() -> PsiSearchHelper.getInstance(getProject()).getUseScope(platformClass));

    boolean reachesMain = useScope instanceof GlobalSearchScope globalScope && globalScope.contains(main.getVirtualFile());
    assertTrue(reachesMain, "the module reaching the shared folder holds usages of its declarations");
  }

  /** The nested module "shared": content root {@code shared/}, source root {@code shared/src}. */
  @NotNull
  private Module addSharedModule() throws IOException {
    VirtualFile root = myFixture.getTempDirFixture().findOrCreateDir(SHARED_MODULE_NAME);
    return addModule(SHARED_MODULE_NAME, root, sharedSources());
  }

  @NotNull
  private VirtualFile sharedSources() throws IOException {
    return myFixture.getTempDirFixture().findOrCreateDir(SHARED_SOURCES_PATH);
  }

  @NotNull
  private Module addModule(@NotNull String name, @NotNull VirtualFile root, @NotNull VirtualFile sources) {
    String moduleFilePath = root.getPath() + "/" + name + ".iml";
    ModuleManager moduleManager = ModuleManager.getInstance(getProject());
    Module module = WriteAction.compute(() -> moduleManager.newModule(moduleFilePath, GeneralModuleType.TYPE_ID));
    PsiTestUtil.addContentRoot(module, root);
    PsiTestUtil.addSourceRoot(module, sources);
    return module;
  }

  @NotNull
  private HaxeBuildFile hxmlBuild(@NotNull String text) {
    VirtualFile file = myFixture.addFileToProject("build.hxml", text).getVirtualFile();
    return new HaxeBuildFile(file, HaxeBuildFileType.HXML);
  }

  @NotNull
  private List<ExternalClasspath> externalClasspathsOf(@NotNull Module module, @NotNull HaxeBuildFile buildFile) {
    return ReadAction.computeBlocking(() -> HaxeExternalClasspaths.of(getProject(), module, List.of(buildFile)));
  }

  /** Publishes the module's external classpaths as a library sync does, and runs the refresh that posts. */
  private void publish(@NotNull Module module, @NotNull HaxeBuildFile buildFile) {
    List<ExternalClasspath> classpaths = externalClasspathsOf(module, buildFile);
    HaxeExternalClasspathStore.getInstance(getProject()).replaceAll(Map.of(module.getName(), classpaths));
    PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
  }

  @NotNull
  private List<HaxeModel> resolveFrom(@NotNull PsiFile file, @NotNull String qualifiedName) {
    FullyQualifiedInfo info = new FullyQualifiedInfo(qualifiedName);
    List<HaxeModel> resolved = ReadAction.computeBlocking(() -> HaxeProjectModel.fromElement(file).resolve(info, file.getResolveScope()));
    return resolved == null ? List.of() : resolved;
  }

  /** A folder outside every module, readable through the VFS for this test and deleted after it. */
  @NotNull
  private VirtualFile outsideDirectory(@NotNull String prefix) throws IOException {
    File directory = FileUtil.createTempDirectory(prefix, null);
    outsideDirectories.add(directory);
    VfsRootAccess.allowRootAccess(getTestRootDisposable(), directory.getPath());
    VirtualFile file = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(directory.toPath());
    assertNotNull(file, "the outside folder must be visible to the VFS");
    return file;
  }

  /** A subfolder created on disk and found through a VFS refresh: the VFS may still list it from an earlier run of the same temp folder. */
  @NotNull
  private static VirtualFile createSubdirectory(@NotNull VirtualFile parent, @NotNull String name) throws IOException {
    Path directory = Files.createDirectories(Path.of(parent.getPath(), name));
    VirtualFile file = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(directory);
    assertNotNull(file, "the subfolder must be visible to the VFS");
    return file;
  }
}
