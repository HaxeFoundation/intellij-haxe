package com.intellij.plugins.haxe.haxelib;

import com.intellij.plugins.haxe.haxelib.HaxelibLocalDocs.GitCheckout;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("Haxelib: git checkout state")
public class HaxelibGitCheckoutTest {

  private static final String COMMIT = "559b24c9a36533281ba7a2eed8aab83ed6b872b4";

  @Test
  @DisplayName("display combines branch and abbreviated commit")
  public void displayCombinesBranchAndAbbreviatedCommit() {
    assertEquals("main @ 559b24c9a3", new GitCheckout("main", COMMIT, null).display());
    assertEquals("559b24c9a3", new GitCheckout(null, COMMIT, null).display());
    assertEquals("main", new GitCheckout("main", null, null).display());
  }

  @Test
  @DisplayName("web url prefers the commit page")
  public void webUrlPrefersTheCommitPage() {
    String remote = "https://github.com/libowner/repo.git";
    assertEquals("https://github.com/libowner/repo/commit/" + COMMIT,
                 new GitCheckout("main", COMMIT, remote).webUrl());
    assertEquals("https://github.com/libowner/repo/tree/main",
                 new GitCheckout("main", null, remote).webUrl());
  }

  @Test
  @DisplayName("web url converts the scp like ssh remote form")
  public void webUrlConvertsTheScpLikeSshRemoteForm() {
    GitCheckout checkout = new GitCheckout(null, COMMIT, "git@github.com:libowner/repo.git");
    assertEquals("https://github.com/libowner/repo/commit/" + COMMIT, checkout.webUrl());
  }

  @Test
  @DisplayName("web url is null without a browsable remote")
  public void webUrlIsNullWithoutABrowsableRemote() {
    assertNull(new GitCheckout("main", COMMIT, null).webUrl());
    assertNull(new GitCheckout("main", COMMIT, "file:///local/mirror.git").webUrl());
  }
}
