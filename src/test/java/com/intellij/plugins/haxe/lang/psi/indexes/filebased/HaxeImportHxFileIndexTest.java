package com.intellij.plugins.haxe.lang.psi.indexes.filebased;

import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.lang.psi.indexes.filebased.extension.specialized.HaxeImportHxFileIndex;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.testFramework.builders.ModuleFixtureBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The module's content root is no source root; only its {@code src} folder is one. */
@DisplayName("Indexing: import hx file index")
public class HaxeImportHxFileIndexTest extends HaxeCodeInsightFixtureTestCase {

  private static final String SOURCE_FOLDER_NAME = "src";
  // conditional compilation keeps the file out of the stub index, so the file-based index reads it
  private static final String CONDITIONAL_IMPORT_HX_SOURCE = "#if js\nimport haxe.ds.StringMap;\n#end\n";

  @Override
  protected String getBasePath() {
    return "";
  }

  @Override
  protected boolean toAddSourceRoot() {
    return false;
  }

  @Override
  @SuppressWarnings("rawtypes")
  protected void tuneFixture(ModuleFixtureBuilder moduleBuilder) throws Exception {
    Files.createDirectories(Path.of(myFixture.getTempDirPath(), SOURCE_FOLDER_NAME));
    moduleBuilder.addSourceRoot(SOURCE_FOLDER_NAME);
  }

  @Test
  @DisplayName("an import hx without a package statement applies to the package of its folder")
  public void testAnImportHxWithoutAPackageStatementAppliesToThePackageOfItsFolder() {
    PsiFile importHx = myFixture.addFileToProject(SOURCE_FOLDER_NAME + "/app/import.hx", CONDITIONAL_IMPORT_HX_SOURCE);

    List<HaxeFile> appImports = importHxFor("app");

    assertEquals(List.of(importHx), appImports);
  }

  @Test
  @DisplayName("an import hx outside every source root applies to no package")
  public void testAnImportHxOutsideEverySourceRootAppliesToNoPackage() {
    myFixture.addFileToProject("tools/import.hx", CONDITIONAL_IMPORT_HX_SOURCE);

    List<HaxeFile> rootPackageImports = importHxFor("");

    assertEquals(List.of(), rootPackageImports);
  }

  private List<HaxeFile> importHxFor(String packageName) {
    return HaxeImportHxFileIndex.getImportHxForPackage(packageName, getProject(), GlobalSearchScope.allScope(getProject()));
  }
}
