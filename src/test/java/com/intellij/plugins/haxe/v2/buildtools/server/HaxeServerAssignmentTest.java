package com.intellij.plugins.haxe.v2.buildtools.server;

import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.v2.buildtools.HaxeContainers;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeEnvironmentStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A container's server instance is its SDK's shared one until it asks for a dedicated one, whose id and name carry the container. */
@DisplayName("Build tools: server assignment")
public class HaxeServerAssignmentTest extends HaxeCodeInsightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return "";
  }

  @Test
  @DisplayName("a container shares its sdk's server unless it asks for a dedicated one")
  public void testAContainerSharesItsSdksServerUnlessItAsksForADedicatedOne() {
    String containerId = myFixture.getModule().getName();
    HaxeServerAssignment shared = HaxeServerAssignment.shared(getProject(), null);
    assertEquals(shared, HaxeServerAssignment.forContainer(getProject(), containerId));

    HaxeEnvironmentStore.getInstance(getProject()).setUsingDedicatedServer(containerId, true);
    HaxeServerAssignment dedicated = HaxeServerAssignment.forContainer(getProject(), containerId);

    assertEquals(shared.id() + "#" + containerId, dedicated.id());
    assertEquals(shared.executablePath(), dedicated.executablePath());
    String containerName = HaxeContainers.displayName(getProject(), containerId);
    assertTrue(dedicated.displayName().contains(containerName), "named after the container, got: " + dedicated.displayName());
    assertEquals(dedicated.id() + "#display", HaxeDisplayProcessManager.idFor(dedicated));
  }
}
