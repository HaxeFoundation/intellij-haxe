package com.intellij.plugins.haxe.resolve;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkModificator;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.config.sdk.HaxeSdkType;
import com.intellij.plugins.haxe.ide.module.HaxeModuleType;
import com.intellij.plugins.haxe.lang.psi.HaxeReference;
import com.intellij.plugins.haxe.lang.psi.HaxeUsingStatement;
import com.intellij.plugins.haxe.model.HaxeProjectModel;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A file of an SDK resolves under the module that uses that SDK. The platform
 * attributes a library file to its module only when asked about the file
 * itself; an element inside it answers no module, and the project-wide view
 * that stands in holds the std of ONE SDK - with a second module on another
 * SDK, every lookup from inside the first SDK's files comes back empty.
 */
@DisplayName("Resolve: library file module")
public class HaxeLibraryFileModuleResolveTest extends HaxeCodeInsightFixtureTestCase {

  private static final String STD_TYPES_HX_SOURCE = """
    @:coreType abstract Void {}
    @:coreType abstract Int {}
    @:coreType abstract Bool {}
    """;

  private static final String STRING_HX_SOURCE = """
    extern class String {
      var length(default, null):Int;
    }
    """;

  private static final String STRING_TOOLS_HX_SOURCE = """
    class StringTools {
      public static function shout(s:String, mark:String):String {
        return s;
      }
    }
    """;

  /** An SDK file that reads like haxe.macro.Printer: a using statement and an extension chain on a parameter. */
  private static final String PRINTER_HX_SOURCE = """
    package tools;
    using StringTools;
    class Printer {
      function escape(s:String):String {
        return s.shout("a").shout("b");
      }
    }
    """;

  private final List<Sdk> registeredSdks = new ArrayList<>();
  private final List<File> stdDirectories = new ArrayList<>();
  private Path firstStd;

  @Override
  protected String getBasePath() {
    return "";
  }

  @Override
  protected void setUp() throws Exception {
    firstStd = createStd("haxe-std-first");
    Path secondStd = createStd("haxe-std-second");
    super.setUp();
    ModuleRootModificationUtil.setModuleSdk(myFixture.getModule(), registerSdk("first std", firstStd));
    Module second = WriteAction.compute(() -> ModuleManager.getInstance(getProject()).newModule(myFixture.getTempDirPath() + "/second/second.iml", new HaxeModuleType().getId()));
    ModuleRootModificationUtil.setModuleSdk(second, registerSdk("second std", secondStd));
  }

  @Override
  protected void tearDown() throws Exception {
    try {
      WriteAction.run(() -> registeredSdks.forEach(ProjectJdkTable.getInstance()::removeJdk));
      stdDirectories.forEach(FileUtil::delete);
    }
    finally {
      super.tearDown();
    }
  }

  @Test
  @DisplayName("a using statement inside an sdk file resolves in that sdk")
  public void testAUsingStatementInsideAnSdkFileResolvesInThatSdk() {
    PsiFile printer = sdkFile(firstStd, "tools/Printer.hx");
    HaxeReference usingReference = referenceIn(printer, r -> r.getParent() instanceof HaxeUsingStatement);

    PsiElement resolved = usingReference.resolve();

    assertEquals(myFixture.getModule(), HaxeProjectModel.moduleOf(usingReference), "the element resolves under the module using its sdk");
    assertInStd(resolved, firstStd);
  }

  @Test
  @DisplayName("an extension chain inside an sdk file resolves in that sdk")
  public void testAnExtensionChainInsideAnSdkFileResolvesInThatSdk() {
    PsiFile printer = sdkFile(firstStd, "tools/Printer.hx");
    HaxeReference lastLink = referenceIn(printer, r -> r.getText().startsWith("s.shout(") && r.getText().endsWith(".shout"));

    PsiElement resolved = lastLink.resolve();

    assertInStd(resolved, firstStd);
  }

  private PsiFile sdkFile(Path std, String relativePath) {
    VirtualFile file = LocalFileSystem.getInstance().refreshAndFindFileByPath(std.resolve(relativePath).toString().replace('\\', '/'));
    assertNotNull(file, relativePath + " in " + std);
    PsiFile psiFile = myFixture.getPsiManager().findFile(file);
    assertNotNull(psiFile, "psi for " + relativePath);
    return psiFile;
  }

  private static HaxeReference referenceIn(PsiFile file, Predicate<HaxeReference> which) {
    HaxeReference reference = PsiTreeUtil.findChildrenOfType(file, HaxeReference.class).stream()
      .filter(which)
      .findFirst()
      .orElse(null);
    assertNotNull(reference, "reference in " + file.getName());
    return reference;
  }

  private static void assertInStd(PsiElement resolved, Path std) {
    assertNotNull(resolved, "resolved");
    String path = resolved.getContainingFile().getVirtualFile().getPath();
    assertTrue(path.startsWith(std.toString().replace('\\', '/')), "resolved into " + path + ", expected the std at " + std);
  }

  /** A minimal std: the primitives, String, a using class and one library file that uses it. */
  private Path createStd(String name) throws IOException {
    File directory = FileUtil.createTempDirectory(name, null);
    stdDirectories.add(directory);
    VfsRootAccess.allowRootAccess(getTestRootDisposable(), directory.getPath());
    Path std = directory.toPath();
    Files.writeString(std.resolve("StdTypes.hx"), STD_TYPES_HX_SOURCE);
    Files.writeString(std.resolve("String.hx"), STRING_HX_SOURCE);
    Files.writeString(std.resolve("StringTools.hx"), STRING_TOOLS_HX_SOURCE);
    Files.createDirectories(std.resolve("tools"));
    Files.writeString(std.resolve("tools/Printer.hx"), PRINTER_HX_SOURCE);
    return std;
  }

  /** The SDK table is application-wide; tearDown removes what this registers. */
  private Sdk registerSdk(String name, Path std) {
    ProjectJdkTable table = ProjectJdkTable.getInstance();
    Sdk sdk = table.createSdk(name, HaxeSdkType.getInstance());
    VirtualFile root = LocalFileSystem.getInstance().refreshAndFindFileByPath(std.toString().replace('\\', '/'));
    assertNotNull(root, "std root " + std);
    SdkModificator modificator = sdk.getSdkModificator();
    modificator.setHomePath(root.getPath());
    modificator.addRoot(root, OrderRootType.SOURCES);
    modificator.addRoot(root, OrderRootType.CLASSES);
    WriteAction.run(() -> {
      modificator.commitChanges();
      table.addJdk(sdk);
    });
    registeredSdks.add(sdk);
    return sdk;
  }
}
