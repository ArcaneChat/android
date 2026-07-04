package org.thoughtcrime.securesms.util;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
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
    String shortened = Linkifier.shortenMiddle(link);

    assertEquals(
        "https://example...lue=1234567890",
        shortened);
    assertEquals(Linkifier.MAX_DISPLAY_LINK_LENGTH, shortened.length());
  }

  @Test
  public void shortenMiddle_keepsLinksAtTheLimit() {
    String link = repeat('a', Linkifier.MAX_DISPLAY_LINK_LENGTH);

    assertEquals(link, Linkifier.shortenMiddle(link));
  }

  @Test
  public void shortenMiddle_shortensLinksJustOverTheLimit() {
    String link = "123456789012345678901234567890123";

    assertEquals("123456789012345...01234567890123", Linkifier.shortenMiddle(link));
  }

  @Test
  public void shortenMiddle_handlesVeryLongLinks() {
    String link = repeat('b', 256);
    String shortened = Linkifier.shortenMiddle(link);

    assertEquals(Linkifier.MAX_DISPLAY_LINK_LENGTH, shortened.length());
    assertEquals(repeat('b', 15) + "..." + repeat('b', 14), shortened);
  }

  private static String repeat(char value, int count) {
    char[] chars = new char[count];
    Arrays.fill(chars, value);
    return new String(chars);
  }
}
