package com.avayaspacesproject.ui.conference.chat;

import android.content.Context;

import androidx.annotation.NonNull;

import com.avaya.spacescsdk.services.user.model.SpacesUserInfo;

import java.util.Objects;

import io.zang.spaces.api.LoganMessage;
import util.DateWorks;

public class MessagePostChatModel implements MessageModel {
    private final String id;
    private final LoganMessage message;
    private final LoganMessage parentMessage;
    private final boolean ours;
    private final String pictureUrl;
    private final boolean showPicture;
    private final CharSequence sender;
    private final boolean showSenderName;
    private final String createdDate;

    private final String title;
    private final String description;


    public MessagePostChatModel(@NonNull LoganMessage message, Context context) {
        id = message.iden;
        this.message = message;
        parentMessage = message.getParentMsg();

        final SpacesUserInfo user = message.sender;
        if (user != null) {
            pictureUrl = user.getPictureUrl();
        } else {
            pictureUrl = "";
        }
        showPicture = message.zShowUserpic;
        sender = message.senderNameSafe();
        showSenderName = message.zShowUsername;
        createdDate = DateWorks.formatTime(context, message.createdSafe().getTime(), false);

        title = message.getParentMessageTitle();
        description = message.getContentBodyTextAsHtml();

        ours = message.isFromMe();
    }

    @Override
    public int getType() {
        return VIEW_TYPE_POST_CHAT;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getCreatedDate() {
        return createdDate;
    }

    @Override
    public boolean isOurs() {
        return ours;
    }

    public CharSequence getSenderName() {
        return sender;
    }

    public boolean canShowSenderName() {
        return showSenderName;
    }

    public String getPictureUrl() {
        return pictureUrl;
    }

    public boolean canShowPicture() {
        return showPicture;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public LoganMessage getMessage() {
        return message;
    }

    public LoganMessage getParentMessage() {
        return parentMessage;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessagePostChatModel that = (MessagePostChatModel) o;
        return id.equals(that.id) &&
                sender.equals(that.sender) &&
                showSenderName == that.showSenderName &&
                pictureUrl.equals(that.pictureUrl) &&
                showPicture == that.showPicture &&
                title.equals(that.title) &&
                description.equals(that.description) &&
                createdDate.equals(that.createdDate) &&
                ours == that.ours;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, sender, showPicture, pictureUrl, showPicture, sender, title, description, createdDate, ours);
    }
}
