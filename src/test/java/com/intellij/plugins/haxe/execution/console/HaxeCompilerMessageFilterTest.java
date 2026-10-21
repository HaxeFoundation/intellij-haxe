package com.intellij.plugins.haxe.execution.console;

import com.intellij.execution.filters.Filter;
import com.intellij.execution.filters.OpenFileHyperlinkInfo;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Console: compiler message filter")
public class HaxeCompilerMessageFilterTest extends HaxeConsoleFilterTestBase {

  private static final String MESSAGE = "src/Main.hx:5: characters 1-3 : Unexpected token";

  @Test
  @DisplayName("links the message to its file and position")
  public void testLinksTheMessageToItsFileAndPosition() {
    myFixture.addFileToProject("src/Main.hx", "class Main {}");
    filter = new HaxeCompilerMessageFilter(getProject(), GlobalSearchScope.allScope(getProject()));

    Filter.Result result = applyToLine(MESSAGE);

    assertNotNull(result, "message must link");
    OpenFileDescriptor target = ((OpenFileHyperlinkInfo)result.getFirstHyperlinkInfo()).getDescriptor();
    assertEquals("Main.hx", target.getFile().getName());
    assertEquals(4, target.getLine());
    assertEquals("src/Main.hx:5", highlightedSpan(MESSAGE + "\n", result));
  }

  /** The build ran from game/, so its src/Main.hx is the file the message means, not the project root's. */
  @Test
  @DisplayName("a relative path resolves inside the launch directories before the project root")
  public void testARelativePathResolvesInsideTheLaunchDirectoriesBeforeTheProjectRoot() {
    myFixture.addFileToProject("src/Main.hx", "class Main {}");
    VirtualFile gameMain = myFixture.addFileToProject("game/src/Main.hx", "class Main {}").getVirtualFile();
    VirtualFile gameDirectory = gameMain.getParent().getParent();
    filter = new HaxeCompilerMessageFilter(getProject(), new HaxeLaunchSearchScope(getProject(), List.of(gameDirectory)));

    Filter.Result result = applyToLine(MESSAGE);

    assertNotNull(result, "message must link");
    OpenFileDescriptor target = ((OpenFileHyperlinkInfo)result.getFirstHyperlinkInfo()).getDescriptor();
    assertEquals(gameMain, target.getFile());
  }

  @Test
  @DisplayName("a message naming no known file does not link")
  public void testAMessageNamingNoKnownFileDoesNotLink() {
    filter = new HaxeCompilerMessageFilter(getProject(), GlobalSearchScope.allScope(getProject()));

    assertNull(applyToLine("src/Missing.hx:5: characters 1-3 : Unexpected token"));
  }
}
