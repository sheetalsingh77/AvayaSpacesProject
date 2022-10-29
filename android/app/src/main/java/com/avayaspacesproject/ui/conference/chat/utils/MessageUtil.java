package com.avayaspacesproject.ui.conference.chat.utils;

import android.content.Context;
import android.os.Build;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.Menu;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.text.HtmlCompat;

import com.avaya.spacescsdk.services.spaces.model.SpacesTopic;
import com.avayaspacesproject.R;
import com.avayaspacesproject.ui.conference.TaskPostViewModel;
import com.avayaspacesproject.ui.conference.chat.ChatViewModel;
import com.avayaspacesproject.ui.conference.tasks.TaskViewModel;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import io.zang.spaces.api.LoganMessage;
import util.DateUtil;
import util.HtmlTagHandler;

import static androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT;
import static java.lang.Math.abs;
import static java.util.Collections.emptyList;
import static java.util.concurrent.TimeUnit.MINUTES;

public final class MessageUtil {

    private static final long MESSAGE_COALESCE_INTERVAL_MS = MINUTES.toMillis(5);

    public static final Comparator<LoganMessage> MESSAGE_COMPARATOR = (lhs, rhs) -> {
        final Date lhsDate = lhs.getCreated();
        final Date rhsDate = rhs.getCreated();

        if ((lhsDate == null) || (rhsDate == null)) {
            if ((lhsDate == null) && (rhsDate == null)) {
                return 0;
            }
            return (lhsDate == null) ? -1 : 1;
        }
        return lhsDate.compareTo(rhsDate);
    };

    public static final Comparator<LoganMessage> TASK_COMPARATOR = (lhs, rhs) -> {
        final Date lhsDate = lhs.contentDueDate;
        final Date rhsDate = rhs.contentDueDate;

        if ((lhsDate == null) || (rhsDate == null)) {
            if ((lhsDate == null) && (rhsDate == null)) return 0;
            return (lhsDate == null) ? -1 : 1;
        }
        return lhsDate.compareTo(rhsDate);
    };

    private MessageUtil() {
    }

    public static void onMessageContextMenu(@NonNull SpacesTopic topic,
                                            @NonNull LoganMessage message,
                                            @NonNull View view,
                                            @NonNull TaskPostViewModel viewModel) {
        final Context context = new ContextThemeWrapper(view.getContext(), R.style.AppTheme);
        final PopupMenu popupMenu = new PopupMenu(context, view);
        final Menu menu = popupMenu.getMenu();

        if (!TextUtils.isEmpty(message.getContentBody())) {
            menu.add(Menu.NONE, 0, 0, R.string.str_copy)
                    .setOnMenuItemClickListener(menuItem -> {
                        viewModel.copyMessageToClipboard(context, message);
                        return true;
                    });
        }

        if (viewModel.topicCanRemoveMessage(topic, message)) {
            menu.add(Menu.NONE, 0, 0, R.string.remove)
                    .setOnMenuItemClickListener(menuItem -> {
                        viewModel.deleteMessage(message);
                        return true;
                    });
        }

        if (menu.size() == 0) {
            menu.add(Menu.NONE, 0, 0, R.string.no_actions)
                    .setOnMenuItemClickListener(menuItem -> true)
                    .setEnabled(false);
        }

        if (menu.size() > 0) {
            popupMenu.show();
        }
    }

    @NonNull
    public static List<LoganMessage> refreshChat(@NonNull SpacesTopic topic) {
//        if ((topic.xChat == null) || (topic.xChat.length == 0)) {
//            return emptyList();
//        }
        final LoganMessage[] loganMessages = topic.copyChatMessages();
        if ((loganMessages == null) || (loganMessages.length == 0)) {
            return emptyList();
        }

        // Setup

        boolean sameDayWithPrev = false;
        boolean sameDayWithNext = false;
        boolean sameTimeWithPrev = false;
        boolean sameTimeWithNext = false;
        boolean samePersonWithPrev = false;
        boolean samePersonWithNext = false;
        @Nullable LoganMessage msgNext = loganMessages[0];
        final List<LoganMessage> cells = new ArrayList<>(loganMessages.length);

        for (int i = 0; i < loganMessages.length; i++) {
            final LoganMessage msg = msgNext;
            final boolean first = (i == 0) || msg.zPageBreaker;
            final boolean last = i == (loganMessages.length - 1);
            msgNext = last ? null : loganMessages[i + 1];

            sameTimeWithPrev = sameTimeWithNext;
            samePersonWithPrev = samePersonWithNext;

            sameDayWithPrev = sameDayWithNext;
            sameDayWithNext = !last && DateUtil.sameDay(msg.createdSafe(), msgNext.createdSafe());
            sameTimeWithNext = sameDayWithNext && (timeDifference(msg, msgNext) < MESSAGE_COALESCE_INTERVAL_MS);
            samePersonWithNext = (msgNext != null) && (msg.sender != null) && (msgNext.sender != null) && msg.sender.isEqualTo(msgNext.sender);

            // Date title
            msg.zShowDateTitle = first || !sameDayWithPrev;

            // Time on bottom
            msg.zShowTimeOnBottom = last || !samePersonWithNext || !sameTimeWithNext;

            // New bunch
            final boolean newBunch = first || msg.hasAttachment() || !samePersonWithPrev || !sameTimeWithPrev;
            msg.zShowBaloonTriangle = newBunch;
            msg.zShowUserpic = newBunch && !msg.isFromMe();
            msg.zShowUsername = newBunch && !msg.isFromMe();

            msg.zLinked = null;
            msg.zLinkedBack = null;

            cells.add(msg);
        }
        return cells;
    }

    private static long timeDifference(@NonNull LoganMessage msg1, @NonNull LoganMessage msg2) {
        return abs(msg1.createdSafe().getTime() - msg2.createdSafe().getTime());
    }

    public static String textToHtml(String text) {
        final String html = HtmlCompat.toHtml(new SpannableString(text), HtmlCompat.TO_HTML_PARAGRAPH_LINES_INDIVIDUAL);
        return cleanHtml(html);
    }

    public static Spanned getMessageForDisplay(String messageText) {
        final String cleanedText = cleanHtml(messageText);
        final String text = codeBlockEmptyLineToNbsp(cleanedText);
        final Spanned spanned;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            spanned = Html.fromHtml(text, FROM_HTML_MODE_COMPACT, null, new HtmlTagHandler());
        } else {
            spanned = Html.fromHtml(text, null, new HtmlTagHandler());
        }
        return (Spanned) trimTrailingWhitespace(spanned);
    }

    /**
     * We need NO-BREAK SPACE symbol instead of <br /> to use LineBackgroundSpan for empty lines inside code blocks.
     */
    private static String codeBlockEmptyLineToNbsp(String text) {
        return text.replaceAll("<pre><code><br /></code></pre>", "<pre><code>&nbsp;</code></pre>");
    }

    /**
     * From https://stackoverflow.com/a/10187511
     */
    private static CharSequence trimTrailingWhitespace(@Nullable CharSequence source) {
        if (source == null) {
            return "";
        }

        int length = source.length();

        while (--length > 0) {
            if (!Character.isWhitespace(source.charAt(length))) {
                break;
            }
        }

        return source.subSequence(0, length + 1);
    }

    public static String cleanHtml(String html) {
        final Document.OutputSettings outputSettings = new Document.OutputSettings();
        outputSettings.prettyPrint(false);

        final String cleanedHtml = Jsoup.clean(html, "", messageWhitelist(), outputSettings);
        final Document doc = Jsoup.parse(cleanedHtml, "", Parser.xmlParser());

        final Elements boldElements = doc.select("b");
        boldElements.tagName("strong");

        final Elements italicsElements = doc.select("i");
        italicsElements.tagName("em");

        final Elements strikeElements = doc.select("strike");
        strikeElements.tagName("del");

        return doc.toString();
    }

    /**
     * This is partially based on the Draft.js block render map being used by the web client:
     * https://draftjs.org/docs/advanced-topics-custom-block-render-map/#draft-default-block-render-map
     * as well as including some other historically supported tags.
     */
    private static Whitelist messageWhitelist() {
        return new Whitelist()
                .addTags(
                        "b", "blockquote", "br", "code", "del", "em",
                        "h1", "h2", "h3", "h4", "h5", "h6",
                        "i", "li", "ol", "p", "pre", "strike", "strong",
                        "sub", "sup", "u", "ul");
    }

}
