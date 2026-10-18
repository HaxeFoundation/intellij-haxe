package com.intellij.plugins.haxe.v2.buildtools.context;

import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.v2.buildtools.HaxeBuildEventListener;
import com.intellij.plugins.haxe.v2.buildtools.HaxeContainers;
import com.intellij.plugins.haxe.v2.buildtools.libraries.HaxeLibraryGraph;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeActiveBuildFileStore;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeEnvironmentStore;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeResolveContextStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("Resolve context: ownership")
public class HaxeResolveContextsTest extends HaxeCodeInsightFixtureTestCase {
  private static final String WEB_HXML_SOURCE = """
    -cp src
    -main WebMain
    -js bin/web.js
    """;
  private static final String NODE_HXML_SOURCE = """
    -cp src
    -main NodeMain
    -js bin/node.js
    """;
  private static final String SHARED_HXML_SOURCE = """
    -cp src
    --interp
    """;
  private static final String TOOLS_HXML_SOURCE = """
    -cp src/tools
    -main Tool
    --interp
    """;
  private static final String LIB_USER_HXML_SOURCE = """
    -cp src
    -lib fake
    -main NodeMain
    --interp
    """;

  @TempDir
  Path libraryDirectory;

  @Override
  protected String getBasePath() {
    return "";
  }

  /** The library sync a project open starts would replace the haxelib entries the tests attach. */
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    awaitLibrarySync();
  }

  @Test
  @DisplayName("the most specific source directory owns a file")
  public void testTheMostSpecificSourceDirectoryOwnsAFile() {
    VirtualFile web = hxml("web.hxml", WEB_HXML_SOURCE);
    VirtualFile tools = hxml("tools.hxml", TOOLS_HXML_SOURCE);
    VirtualFile tool = source("src/tools/Tool.hx", "class Tool {}");
    VirtualFile shared = source("src/Shared.hx", "class Shared {}");

    assertEquals(tools.getPath(), contextPath(tool), "src/tools is more specific than src");
    assertEquals(web.getPath(), contextPath(shared), "only the web build's -cp src contains it");
  }

  @Test
  @DisplayName("shared sources go to the build whose main class the file is")
  public void testSharedSourcesGoToTheBuildWhoseMainClassTheFileIs() {
    VirtualFile web = hxml("web.hxml", WEB_HXML_SOURCE);
    VirtualFile node = hxml("node.hxml", NODE_HXML_SOURCE);
    VirtualFile webMain = source("src/WebMain.hx", "class WebMain {}");
    VirtualFile nodeMain = source("src/NodeMain.hx", "class NodeMain {}");
    HaxeActiveBuildFileStore.getInstance(getProject()).setActiveFile(web.getPath());

    assertEquals(node.getPath(), contextPath(nodeMain), "the -main tie-break beats the active build file");
    assertEquals(web.getPath(), contextPath(webMain));
  }

  @Test
  @DisplayName("shared sources fall to the active build file, then to the first known build")
  public void testSharedSourcesFallToTheActiveBuildFileThenToTheFirstKnownBuild() {
    VirtualFile web = hxml("web.hxml", WEB_HXML_SOURCE);
    VirtualFile node = hxml("node.hxml", NODE_HXML_SOURCE);
    VirtualFile shared = source("src/Shared.hx", "class Shared {}");
    HaxeActiveBuildFileStore store = HaxeActiveBuildFileStore.getInstance(getProject());

    store.setActiveFile(node.getPath());
    assertEquals(node.getPath(), contextPath(shared));

    store.setActiveFile(web.getPath());
    assertEquals(web.getPath(), contextPath(shared));

    store.loadState(new HaxeActiveBuildFileStore.State());
    List<HaxeResolveContext> reaching = contexts().contextsReaching(shared);
    assertEquals(2, reaching.size(), "both builds contain the file");
    assertEquals(reaching.getFirst().path(), contextPath(shared), "without an active build the first known one wins");
  }

  @Test
  @DisplayName("a pin wins over the automatic choice while its build contains the file")
  public void testAPinWinsOverTheAutomaticChoiceWhileItsBuildContainsTheFile() {
    VirtualFile web = hxml("web.hxml", WEB_HXML_SOURCE);
    hxml("node.hxml", NODE_HXML_SOURCE);
    VirtualFile nodeMain = source("src/NodeMain.hx", "class NodeMain {}");
    HaxeResolveContextStore store = HaxeResolveContextStore.getInstance(getProject());

    store.pin(nodeMain, web.getPath());
    assertEquals(web.getPath(), contextPath(nodeMain), "the pin beats the -main rule");
    assertTrue(contexts().isPinned(nodeMain));

    store.pin(nodeMain, "/elsewhere/gone.hxml");
    assertEquals(hxmlPath("node.hxml"), contextPath(nodeMain), "a pin to a build that does not contain the file is ignored");
    assertFalse(contexts().isPinned(nodeMain));

    store.unpin(nodeMain);
    assertFalse(contexts().isPinned(nodeMain));
  }

  @Test
  @DisplayName("a navigation gives a shared file the context it was opened from, a pin still wins")
  public void testANavigationGivesASharedFileTheContextItWasOpenedFromAPinStillWins() {
    VirtualFile web = hxml("web.hxml", WEB_HXML_SOURCE);
    VirtualFile node = hxml("node.hxml", NODE_HXML_SOURCE);
    VirtualFile nodeMain = source("src/NodeMain.hx", "class NodeMain {}");
    VirtualFile shared = source("src/Shared.hx", "class Shared {}");
    HaxeActiveBuildFileStore.getInstance(getProject()).setActiveFile(web.getPath());
    assertEquals(web.getPath(), contextPath(shared), "the active build before any navigation");

    assertTrue(contexts().follow(nodeMain, shared), "node.hxml reaches Shared.hx and differs from its current context");
    assertEquals(node.getPath(), contextPath(shared));
    assertTrue(contexts().isFollowed(shared));
    assertFalse(contexts().isPinned(shared));

    assertFalse(contexts().follow(shared, nodeMain), "nothing changes when the two already agree");

    VirtualFile tools = hxml("tools.hxml", TOOLS_HXML_SOURCE);
    VirtualFile tool = source("src/tools/Tool.hx", "class Tool {}");
    assertFalse(contexts().follow(nodeMain, tool), "a file with a closer owner keeps it, although node.hxml reaches it too");
    assertEquals(tools.getPath(), contextPath(tool));

    HaxeResolveContextStore store = HaxeResolveContextStore.getInstance(getProject());
    store.pin(shared, web.getPath());
    assertFalse(contexts().follow(nodeMain, shared), "a pinned file never follows");
    assertEquals(web.getPath(), contextPath(shared));

    store.unpin(shared);
    assertEquals(web.getPath(), contextPath(shared), "select automatically forgets the followed context too");
    assertFalse(contexts().isFollowed(shared));
  }

  @Test
  @DisplayName("among equal owners the containers build command wins before the first known build")
  public void testAmongEqualOwnersTheContainersBuildCommandWinsBeforeTheFirstKnownBuild() {
    hxml("a.hxml", SHARED_HXML_SOURCE);
    VirtualFile b = hxml("b.hxml", SHARED_HXML_SOURCE);
    VirtualFile shared = source("src/Shared.hx", "class Shared {}");
    String containerId = HaxeContainers.containerIdFor(getProject(), shared);
    HaxeEnvironmentStore.CompileCommand command = new HaxeEnvironmentStore.CompileCommand(b.getPath(), null, "");
    HaxeEnvironmentStore.getInstance(getProject()).setCompileCommand(containerId, command);

    assertEquals(b.getPath(), contextPath(shared), "no -main, no active build: the Build command's file beats tool-window order");
  }

  @Test
  @DisplayName("contexts reaching a file list every owner, the most specific first")
  public void testContextsReachingAFileListEveryOwnerTheMostSpecificFirst() {
    VirtualFile web = hxml("web.hxml", WEB_HXML_SOURCE);
    VirtualFile tools = hxml("tools.hxml", TOOLS_HXML_SOURCE);
    VirtualFile tool = source("src/tools/Tool.hx", "class Tool {}");

    List<String> reaching = contexts().contextsReaching(tool).stream()
      .map(HaxeResolveContext::path)
      .toList();

    assertEquals(List.of(tools.getPath(), web.getPath()), reaching);
  }

  @Test
  @DisplayName("a library file is reached by every build, the active one first")
  public void testALibraryFileIsReachedByEveryBuildTheActiveOneFirst() throws IOException {
    VirtualFile web = hxml("web.hxml", WEB_HXML_SOURCE);
    VirtualFile node = hxml("node.hxml", NODE_HXML_SOURCE);
    VirtualFile libraryFile = libraryFile("Lib.hx", "haxelib: fake");
    HaxeActiveBuildFileStore.getInstance(getProject()).setActiveFile(node.getPath());

    List<String> reaching = contexts().contextsReaching(libraryFile).stream()
      .map(HaxeResolveContext::path)
      .toList();

    assertEquals(Set.of(web.getPath(), node.getPath()), Set.copyOf(reaching), "no build's sources contain it, so every build reaches it");
    assertEquals(node.getPath(), contextPath(libraryFile), "the active build is the default for a library file");
  }

  @Test
  @DisplayName("a haxelib file goes to the builds declaring its library")
  public void testAHaxelibFileGoesToTheBuildsDeclaringItsLibrary() throws IOException {
    VirtualFile web = hxml("web.hxml", WEB_HXML_SOURCE);
    VirtualFile user = hxml("user.hxml", LIB_USER_HXML_SOURCE);
    VirtualFile libraryFile = libraryFile("Lib.hx", "haxelib: fake");
    HaxeActiveBuildFileStore.getInstance(getProject()).setActiveFile(web.getPath());

    List<String> reaching = contexts().contextsReaching(libraryFile).stream()
      .map(HaxeResolveContext::path)
      .toList();

    assertEquals(List.of(user.getPath()), reaching, "only the build declaring -lib fake reaches a file of haxelib fake");
    assertEquals(user.getPath(), contextPath(libraryFile), "the declaring build beats the active build file");
  }

  @Test
  @DisplayName("a file of a library reached only through another library goes to the builds declaring that library")
  public void testAFileOfALibraryReachedOnlyThroughAnotherLibraryGoesToTheBuildsDeclaringThatLibrary() throws IOException {
    VirtualFile web = hxml("web.hxml", WEB_HXML_SOURCE);
    VirtualFile user = hxml("user.hxml", LIB_USER_HXML_SOURCE);
    VirtualFile libraryFile = libraryFile("Deep.hx", "haxelib: transitive 1.0.0");
    HaxeActiveBuildFileStore.getInstance(getProject()).setActiveFile(web.getPath());
    // what a library sync records for -lib fake: fake itself brings transitive in
    String containerId = myFixture.getModule().getName();
    HaxeLibraryGraph.getInstance(getProject()).replaceAll(Map.of(), Map.of(containerId, Map.of("fake", List.of("fake", "transitive"))));
    HaxeBuildEventListener.publisher(getProject()).librariesSynced();

    List<String> reaching = contexts().contextsReaching(libraryFile).stream()
      .map(HaxeResolveContext::path)
      .toList();

    assertEquals(List.of(user.getPath()), reaching, "the build declaring fake reaches a file of transitive");
    assertEquals(user.getPath(), contextPath(libraryFile));
  }

  @Test
  @DisplayName("a build file created on disk owns its sources on the next ask")
  public void testABuildFileCreatedOnDiskOwnsItsSourcesOnTheNextAsk() {
    VirtualFile web = hxml("web.hxml", WEB_HXML_SOURCE);
    VirtualFile tool = source("src/tools/Tool.hx", "class Tool {}");
    assertEquals(web.getPath(), contextPath(tool), "only the web build reaches src/tools before tools.hxml exists");

    VirtualFile tools = hxml("tools.hxml", TOOLS_HXML_SOURCE);

    assertEquals(tools.getPath(), contextPath(tool), "the created build file is an owner without a reload");
  }

  @Test
  @DisplayName("a file of a project without build files has no context")
  public void testAFileOfAProjectWithoutBuildFilesHasNoContext() {
    VirtualFile lone = source("src/Lone.hx", "class Lone {}");

    assertNull(contexts().contextFor(lone));
  }

  /** A file under a library root OUTSIDE the content root: in library scope, in no build's sources; {@code entryName} is the synced entry. */
  private VirtualFile libraryFile(String name, String entryName) throws IOException {
    Path root = Files.createDirectories(libraryDirectory.resolve("lib"));
    Path file = Files.writeString(root.resolve(name), "class Lib {}");
    VirtualFile rootFile = Objects.requireNonNull(LocalFileSystem.getInstance().refreshAndFindFileByNioFile(root));
    ModuleRootModificationUtil.addModuleLibrary(myFixture.getModule(), entryName, List.of(rootFile.getUrl()), List.of());
    return Objects.requireNonNull(LocalFileSystem.getInstance().refreshAndFindFileByNioFile(file));
  }

  private VirtualFile hxml(String name, String content) {
    return myFixture.addFileToProject(name, content).getVirtualFile();
  }

  private String hxmlPath(String name) {
    return myFixture.findFileInTempDir(name).getPath();
  }

  private VirtualFile source(String relativePath, String content) {
    return myFixture.addFileToProject(relativePath, content).getVirtualFile();
  }

  private HaxeResolveContexts contexts() {
    return HaxeResolveContexts.getInstance(getProject());
  }

  private String contextPath(VirtualFile file) {
    HaxeResolveContext context = contexts().contextFor(file);
    assertNotNull(context, "a build contains " + file.getName());
    return context.path();
  }
}
