package org.thoughtcrime.securesms.util;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import org.junit.Test;

public class LinkifierTest {

  @Test
  public void shortenLink_keepsShortLinks() {
    String link = "https://example.org/path";

    assertEquals(link, Linkifier.shortenLink(link));
  }

  @Test
  public void shortenLink_shortensLongPathAfterDomain() {
    // domainPart = "https://example.org/" (20 chars), available = 12, tailLength = 9
    String link = "https://example.org/some/really/long/path/with/query?value=1234567890";
    String shortened = Linkifier.shortenLink(link);

    assertEquals("https://example.org/...234567890", shortened);
    assertEquals(Linkifier.MAX_DISPLAY_LINK_LENGTH, shortened.length());
  }

  @Test
  public void shortenLink_keepsLinksAtTheLimit() {
    String link = repeat('a', Linkifier.MAX_DISPLAY_LINK_LENGTH);

    assertEquals(link, Linkifier.shortenLink(link));
  }

  @Test
  public void shortenLink_domainAloneTooLong() {
    // domainPart = "https://my.domain.com/" (22), available = 10, tailLength = 7
    // domainPart just fits within budget, so we show domain + ... + tail
    String link = "https://my.domain.com/some/really/long/path/value=1234567890";
    String shortened = Linkifier.shortenLink(link);

    assertEquals("https://my.domain.com/...4567890", shortened);
    assertEquals(Linkifier.MAX_DISPLAY_LINK_LENGTH, shortened.length());
  }

  @Test
  public void shortenLink_domainExceedsBudget() {
    // domainPart = "https://averylongdomain.example.org/" (36 chars) > 32, available < 0
    String link = "https://averylongdomain.example.org/path/to/something";
    String shortened = Linkifier.shortenLink(link);

    assertEquals("https://averylongdomain.example.org/...", shortened);
  }

  @Test
  public void shortenLink_fallbackTruncatesAtEndForLinksWithoutPath() {
    // No "://" — falls back to end truncation
    String link = repeat('b', 256);
    String shortened = Linkifier.shortenLink(link);

    assertEquals(Linkifier.MAX_DISPLAY_LINK_LENGTH, shortened.length());
    assertEquals(repeat('b', 29) + "...", shortened);
  }
}
