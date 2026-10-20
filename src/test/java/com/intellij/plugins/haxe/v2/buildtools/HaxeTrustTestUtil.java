package com.intellij.plugins.haxe.v2.buildtools;

import com.intellij.ide.trustedProjects.TrustedProjects;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.PlatformTestUtil;
import org.jetbrains.annotations.NotNull;

/**
 * Distrusts a test project. Headless runs auto-trust every project while
 * {@code idea.trust.headless.disabled} is not false, an explicit distrust
 * included, so the trust check is switched on for the body as well.
 */
public final class HaxeTrustTestUtil {

  private static final String HEADLESS_TRUST_DISABLED_PROPERTY = "idea.trust.headless.disabled";

  private HaxeTrustTestUtil() {
  }

  /** Runs the body with the trust check on and the project explicitly distrusted, restoring both after. */
  public static void withDistrustedProject(@NotNull Project project, @NotNull Runnable body) {
    PlatformTestUtil.withSystemProperty(HEADLESS_TRUST_DISABLED_PROPERTY, "false", () -> {
      TrustedProjects.setProjectTrusted(project, false);
      try {
        body.run();
      }
      finally {
        TrustedProjects.setProjectTrusted(project, true);
      }
    });
  }
}
