package org.thoughtcrime.securesms.util;

import android.content.Context;
import android.text.Spannable;
import android.text.style.URLSpan;
import androidx.annotation.NonNull;
import io.noties.markwon.AbstractMarkwonPlugin;
import io.noties.markwon.Markwon;
import io.noties.markwon.SoftBreakAddsNewLinePlugin;
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin;
import io.noties.markwon.inlineparser.HtmlInlineProcessor;
import io.noties.markwon.inlineparser.MarkwonInlineParserPlugin;
import java.util.Collections;
import java.util.HashSet;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.parser.Parser;

public class MarkdownUtil {
  private static MarkdownUtil instance;
  private final Markwon markwon;

  private MarkdownUtil(final Context context) {
    markwon =
        Markwon.builder(context)
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(SoftBreakAddsNewLinePlugin.create())
            .usePlugin(MarkwonInlineParserPlugin.create())
            .usePlugin(
                new AbstractMarkwonPlugin() {
                  @Override
                  public void configure(@NonNull Registry registry) {
                    registry.require(
                        MarkwonInlineParserPlugin.class,
                        plugin -> {
                          plugin.factoryBuilder().excludeInlineProcessor(HtmlInlineProcessor.class);
                        });
                  }

                  @Override
                  public void configureParser(@NonNull Parser.Builder builder) {
                    builder.enabledBlockTypes(
                        new HashSet<>(Collections.singletonList(FencedCodeBlock.class)));
                  }
                })
            .build();
  }

  private static MarkdownUtil getInstance(Context context) {
    if (instance == null) {
      instance = new MarkdownUtil(context.getApplicationContext());
    }
    return instance;
  }

  public static Spannable toMarkdown(Context context, String text) {
    return toMarkdown(context, text, true);
  }

  public static Spannable toMarkdown(Context context, String text, boolean clickable) {
    Spannable spannable = (Spannable) getInstance(context).markwon.toMarkdown(text);
    if (clickable) return spannable;

    URLSpan[] urlSpans = spannable.getSpans(0, spannable.length(), URLSpan.class);
    for (URLSpan urlSpan : urlSpans) {
      spannable.removeSpan(urlSpan);
    }
    return spannable;
  }
}
