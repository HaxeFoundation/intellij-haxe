package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkModificator;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.config.sdk.HaxeSdkData;
import com.intellij.plugins.haxe.config.sdk.HaxeSdkType;
import com.intellij.plugins.haxe.util.HaxeSdkUtilBase;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Tests over registered Haxe SDKs whose homes are real directories holding executables. */
abstract class HaxeRegisteredSdkTestBase extends HaxeLightFixtureTestCase {

  private final List<Sdk> registered = new ArrayList<>();
  private final List<Path> temporaryDirectories = new ArrayList<>();

  @Override
  protected String getBasePath() {
    return "";
  }

  /** The SDK table is application-wide; tearDown removes what a test registers. */
  @Override
  protected void tearDown() throws Exception {
    try {
      WriteAction.run(() -> registered.forEach(ProjectJdkTable.getInstance()::removeJdk));
      for (Path directory : temporaryDirectories) {
        FileUtil.delete(directory.toFile());
      }
    }
    finally {
      super.tearDown();
    }
  }

  /** A Haxe SDK whose home holds a compiler executable; {@code configure} adjusts its data before registration. */
  Sdk registerSdk(String name, Consumer<HaxeSdkData> configure) throws IOException {
    Path home = temporaryDirectory("haxe-sdk");
    touch(home.resolve(HaxeSdkUtilBase.getExecutableName("haxe")));
    HaxeSdkData data = new HaxeSdkData(home.toString(), "4.3.7");
    configure.accept(data);
    ProjectJdkTable table = ProjectJdkTable.getInstance();
    Sdk sdk = table.createSdk(name, HaxeSdkType.getInstance());
    SdkModificator modificator = sdk.getSdkModificator();
    modificator.setHomePath(home.toString());
    modificator.setSdkAdditionalData(data);
    WriteAction.run(() -> {
      modificator.commitChanges();
      table.addJdk(sdk);
    });
    registered.add(sdk);
    return sdk;
  }

  Path temporaryDirectory(String prefix) throws IOException {
    Path directory = Files.createTempDirectory(prefix);
    temporaryDirectories.add(directory);
    return directory;
  }

  /** An empty file at the path, its folders created. */
  static Path touch(Path file) throws IOException {
    Files.createDirectories(file.getParent());
    Files.writeString(file, "");
    return file;
  }
}
