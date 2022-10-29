package com.avayaspacesproject.ui.conference.chat;

import android.content.Context;
import android.text.format.DateFormat;

import androidx.annotation.NonNull;

import java.util.Date;

public class MessageUnreadLineModel implements MessageModel {

    private final String dateLabel;

    public MessageUnreadLineModel(@NonNull Context context, Date date) {
        dateLabel = DateFormat.getLongDateFormat(context).format(date);
    }

    @Override
    public int getType() {
        return MessageModel.VIEW_TYPE_UNREAD_LINE;
    }

    @Override
    public String getId() {
        return "";
    }

    @Override
    public String getCreatedDate() {
        return dateLabel;
    }

    @Override
    public boolean isOurs() {
        return false;
    }


}
