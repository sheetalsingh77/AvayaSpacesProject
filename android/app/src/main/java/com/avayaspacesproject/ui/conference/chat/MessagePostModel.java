package com.avayaspacesproject.ui.conference.chat;

import android.content.Context;

import androidx.annotation.NonNull;

import com.avaya.spacescsdk.services.user.model.SpacesUserInfo;

import java.util.List;
import java.util.Objects;

import io.zang.spaces.api.LoganFile;
import io.zang.spaces.api.LoganMessage;
import util.DateWorks;

public class MessagePostModel implements MessageModel{
    private final String id;
    private final LoganMessage message;
    private final boolean ours;
    private final String pictureUrl;
    private final boolean showPicture;
    private final CharSequence sender;
    private final boolean showSenderName;
    private final String createdDate;

    private final String title;
    private final String description;
    private final int attachmentCount;
    private final int chatCount;


    public MessagePostModel(@NonNull LoganMessage message, Context context) {
        id = message.iden;
        this.message = message;
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

        title = message.getContentBodyTextAsText();
        description = message.getContentDescriptionAsHtml();

        ours = message.isFromMe();

        final List<LoganFile> attachments = message.attachments;
        attachmentCount = (attachments == null) ? 0 : attachments.size();

        chatCount = message.chatCount;
    }

    @Override
    public int getType() {
        return VIEW_TYPE_POST;
    }

    @Override
    public String getId() {
        return id;
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

    @Override
    public String getCreatedDate() {
        return createdDate;
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

    @Override
    public boolean isOurs() {
        return ours;
    }

    public int getAttachmentCount() {
        return attachmentCount;
    }

    public int getChatCount() {
        return chatCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessagePostModel that = (MessagePostModel) o;
        return id.equals(that.id) &&
                sender.equals(that.sender) &&
                showSenderName == that.showSenderName &&
                pictureUrl.equals(that.pictureUrl) &&
                showPicture == that.showPicture &&
                title.equals(that.title) &&
                description.equals(that.description) &&
                createdDate.equals(that.createdDate) &&
                ours == that.ours &&
                attachmentCount == that.attachmentCount &&
                chatCount == that.chatCount;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, sender, showPicture, pictureUrl, showPicture, sender, title, description, createdDate, ours, attachmentCount, chatCount);
    }
}
