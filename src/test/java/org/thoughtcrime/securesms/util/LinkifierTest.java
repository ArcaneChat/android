package org.thoughtcrime.securesms.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class LinkifierTest {

  @Test
  public void shortenMiddle_keepsShortLinks() {
    String link = "https://example.org/path";

    assertEquals(link, Linkifier.shortenMiddle(link));
  }

  @Test
  public void shortenMiddle_shortensLongLinksInTheMiddle() {
    String link = "https://example.org/some/really/long/path/with/query?value=1234567890";

    assertEquals(
        "https://example...ry?value=1234567890",
        Linkifier.shortenMiddle(link));
  }
}
