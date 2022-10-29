package com.avayaspacesproject.ui.conference.chat;

import android.content.Context;
import android.text.format.DateFormat;

import androidx.annotation.NonNull;

import com.avaya.spacescsdk.services.user.model.SpacesUserInfo;
import com.avayaspacesproject.ui.conference.chat.ChatViewModel;

import java.util.Date;
import java.util.List;
import java.util.Objects;

import io.zang.spaces.api.LoganFile;
import io.zang.spaces.api.LoganMessage;
import util.DateWorks;



public class MessageTaskModel implements MessageModel{
    private final String id;
    private final LoganMessage message;
    private final boolean ours;
    private final String pictureUrl;
    private final boolean showPicture;
    private final CharSequence sender;
    private final boolean showSenderName;
    private final String createdDate;

    private final CharSequence title;
    private final String dueDate;

    private final String description;
    private final boolean completed;
    private final boolean today;
    private final boolean overdue;
    private final int attachmentCount;
    private final int chatCount;


    public MessageTaskModel(@NonNull LoganMessage message, @NonNull Context context, @NonNull ChatViewModel chatViewModel) {
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

        final Date contentDueDate = message.contentDueDate;
        if (contentDueDate == null) {
            dueDate = "";
        } else {
            dueDate = DateFormat.getDateFormat(context).format(contentDueDate);
        }

        //SDK Changes temporary to remove compilation error. Change it back to original
        completed = false; // isTaskCompleted(message);
        today = false; // isTaskDueToday(message);
        overdue = false; // isTaskOverdue(message, today);

        final List<LoganFile> attachments = message.attachments;
        attachmentCount = (attachments == null) ? 0 : attachments.size();

        chatCount = message.chatCount;
    }

    @Override
    public int getType() {
        return VIEW_TYPE_TASK;
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

    public CharSequence getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getDueDate() {
        return dueDate;
    }

    public LoganMessage getMessage() {
        return message;
    }

    public boolean isCompleted() {
        return completed;
    }

    public boolean isToday() {
        return today;
    }

    public boolean isOverdue() {
        return overdue;
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
        MessageTaskModel that = (MessageTaskModel) o;
        return id.equals(that.getId()) &&
                sender.equals(that.sender) &&
                showSenderName == that.showSenderName &&
                pictureUrl.equals(that.pictureUrl) &&
                showPicture == that.showPicture &&
                title.equals(that.title) &&
                description.equals(that.description) &&
                createdDate.equals(that.createdDate) &&
                dueDate.equals(that.dueDate) &&
                completed == that.completed &&
                today == that.today &&
                overdue == that.overdue &&
                ours == that.ours &&
                attachmentCount == that.attachmentCount &&
                chatCount == that.chatCount;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, sender, showSenderName, pictureUrl, showPicture, showSenderName, title, createdDate, dueDate, completed, today, overdue, ours, attachmentCount, chatCount);
    }
}
