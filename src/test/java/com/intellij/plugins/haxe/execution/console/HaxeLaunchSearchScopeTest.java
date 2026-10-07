package com.intellij.plugins.haxe.execution.console;

import com.intellij.execution.filters.Filter;
import com.intellij.execution.filters.OpenFileHyperlinkInfo;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.DelegatingGlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Two sibling projects each hold a {@code Main.hx}; the launch's classpaths
 * decide which one a classpath-relative trace position opens.
 */
@DisplayName("Console: launch search scope")
public class HaxeLaunchSearchScopeTest extends HaxeConsoleFilterTestBase {

  private VirtualFile rootSource;
  private VirtualFile limeSource;

  @BeforeEach
  public void addTwoMains() {
    rootSource = myFixture.addFileToProject("src/Main.hx", "class Main {}").getVirtualFile().getParent();
    limeSource = myFixture.addFileToProject("lime/src/Main.hx", "class Main {}").getVirtualFile().getParent();
    myFixture.addFileToProject("tools/Helper.hx", "class Helper {}");
  }

  @Test
  @DisplayName("a trace position opens the file under the launchs classpath")
  public void testATracePositionOpensTheFileUnderTheLaunchsClasspath() {
    filter = new HaxeUtestFailureFilter(getProject(), new HaxeLaunchSearchScope(getProject(), List.of(limeSource)));

    VirtualFile opened = openedFile(applyToLine("Main.hx:3: hello from lime"));

    assertEquals(limeSource.findChild("Main.hx"), opened);
  }

  @Test
  @DisplayName("a position relative to the launchs working directory beats the project root")
  public void testAPositionRelativeToTheLaunchsWorkingDirectoryBeatsTheProjectRoot() {
    // lime runs haxe from lime/ with -cp src, so the position reads src/Main.hx; the
    // project root holds a src/Main.hx of its own
    filter = new HaxeUtestFailureFilter(getProject(), new HaxeLaunchSearchScope(getProject(), List.of(limeSource)));

    VirtualFile opened = openedFile(applyToLine("src/Main.hx:21: lime_flag"));

    assertEquals(limeSource.findChild("Main.hx"), opened);
  }

  @Test
  @DisplayName("the other launch opens the other main")
  public void testTheOtherLaunchOpensTheOtherMain() {
    filter = new HaxeUtestFailureFilter(getProject(), new HaxeLaunchSearchScope(getProject(), List.of(rootSource)));

    VirtualFile opened = openedFile(applyToLine("Main.hx:3: hello from root"));

    assertEquals(rootSource.findChild("Main.hx"), opened);
  }

  @Test
  @DisplayName("a file outside the classpaths still resolves through the project")
  public void testAFileOutsideTheClasspathsStillResolvesThroughTheProject() {
    filter = new HaxeUtestFailureFilter(getProject(), new HaxeLaunchSearchScope(getProject(), List.of(limeSource)));

    VirtualFile opened = openedFile(applyToLine("Helper.hx:1: outside"));

    assertEquals("Helper.hx", opened.getName());
  }

  @Test
  @DisplayName("the preferred directories survive the platforms delegating wrapper")
  public void testThePreferredDirectoriesSurviveThePlatformsDelegatingWrapper() {
    // a console gets the run profile's scope behind a lazy DelegatingGlobalSearchScope
    GlobalSearchScope wrapped = new DelegatingGlobalSearchScope(new HaxeLaunchSearchScope(getProject(), List.of(limeSource)));
    filter = new HaxeUtestFailureFilter(getProject(), wrapped);

    VirtualFile opened = openedFile(applyToLine("src/Main.hx:21: lime_flag"));

    assertEquals(List.of(limeSource), HaxeLaunchSearchScope.preferredDirectoriesOf(wrapped));
    assertEquals(limeSource.findChild("Main.hx"), opened);
  }

  @Test
  @DisplayName("a plain scope has no preferred directories")
  public void testAPlainScopeHasNoPreferredDirectories() {
    GlobalSearchScope plain = GlobalSearchScope.allScope(getProject());

    assertTrue(HaxeLaunchSearchScope.preferredDirectoriesOf(plain).isEmpty());
    assertEquals(List.of(limeSource), HaxeLaunchSearchScope.preferredDirectoriesOf(new HaxeLaunchSearchScope(getProject(), List.of(limeSource))));
  }

  private static VirtualFile openedFile(Filter.Result result) {
    assertNotNull(result, "the position must link");
    return ((OpenFileHyperlinkInfo)result.getFirstHyperlinkInfo()).getDescriptor().getFile();
  }
}
