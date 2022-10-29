package com.avayaspacesproject.ui.conference.chat;

public interface MessageModel {
    int VIEW_TYPE_SEPARATOR = 0;
    int VIEW_TYPE_UNREAD_LINE = 1;
    int VIEW_TYPE_MESSAGE = 2;
    int VIEW_TYPE_POST = 3;
    int VIEW_TYPE_POST_CHAT = 4;
    int VIEW_TYPE_TASK = 5;
    int VIEW_TYPE_TASK_CHAT = 6;
    int VIEW_TYPE_MEETING = 7;

    int getType();

    String getId();

    String getCreatedDate();

    boolean isOurs();
}
