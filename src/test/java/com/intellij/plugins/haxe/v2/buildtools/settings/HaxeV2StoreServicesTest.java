package com.intellij.plugins.haxe.v2.buildtools.settings;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.v2.buildtools.libraries.HaxeLibraryDefinesStore;
import com.intellij.plugins.haxe.v2.toolwindow.HaxeToolWindowUiState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * The v2 storage classes are {@code @Service} light services with no
 * plugin.xml entries; this pins that each one actually instantiates through
 * the container — the spot a wrong annotation or an extra constructor would
 * break at runtime while everything still compiles.
 */
@DisplayName("Build tools: store services")
public class HaxeV2StoreServicesTest extends HaxeLightFixtureTestCase {

  // every project-level store in the settings and libraries packages belongs here
  static final List<Class<?>> STORE_CLASSES = List.of(
    HaxeTargetSelectionStore.class,
    HaxeSectionSelectionStore.class,
    HaxeActiveBuildFileStore.class,
    HaxeTestsBuildFileStore.class,
    HaxeEnvironmentStore.class,
    HaxeCustomActionsStore.class,
    HaxeCustomToolsStore.class,
    HaxeBuildFilesStore.class,
    HaxeWorkDirectoryStore.class,
    HaxeLibraryDefinesStore.class,
    HaxeToolWindowUiState.class);

  @Override
  protected String getBasePath() {
    return "";
  }

  @Test
  @DisplayName("every store resolves as a project light service")
  public void testEveryStoreResolvesAsAProjectLightService() {
    Project project = myFixture.getProject();
    for (Class<?> store : STORE_CLASSES) {
      assertNotNull(project.getService(store), store.getSimpleName() + " must resolve as a light service");
    }
  }
}
