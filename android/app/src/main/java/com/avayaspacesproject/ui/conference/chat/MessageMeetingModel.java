package com.avayaspacesproject.ui.conference.chat;

import android.content.Context;
import android.os.Build;
import android.text.format.DateUtils;

import androidx.annotation.NonNull;

import com.avaya.spacescsdk.services.messaging.enums.MeetingStatus;
import com.avaya.spacescsdk.services.messaging.model.Meeting;
import com.avaya.spacescsdk.services.spaces.model.SpacesTopic;
import com.avayaspacesproject.R;
import com.avayaspacesproject.ui.conference.chat.ChatViewModel;


import java.util.Objects;
import java.util.concurrent.TimeUnit;

import io.zang.spaces.api.LoganMessage;

import static android.text.format.DateUtils.FORMAT_SHOW_DATE;
import static android.text.format.DateUtils.FORMAT_SHOW_TIME;

public class MessageMeetingModel implements MessageModel {
    private final String id;
    private final LoganMessage message;

    private final String title;
    private final String started;

    private final String ended;
    private final CharSequence duration;
    private final int attendees;
    private final boolean isActive;


    public MessageMeetingModel(Meeting meeting, @NonNull LoganMessage message, @NonNull Context context, @NonNull ChatViewModel chatViewModel) {
        id = message.iden;
        this.message = message;

        final MeetingStatus meetingStatus = meeting.getStatus();
        if (meetingStatus == MeetingStatus.ACTIVE) {
            title = context.getString(R.string.live_meeting);
            isActive = true;
        } else {
            title = context.getString(R.string.meeting);
            isActive = false;
        }

        final Long startedTime =message.meeting.getStartTime()==null? null:meeting.getStartTime().toEpochMilli();

        started = DateUtils.formatDateTime(context, startedTime, FORMAT_SHOW_DATE | FORMAT_SHOW_TIME);

        final Long endTime = message.meeting.getEndTime()==null?null:message.meeting.getEndTime().toEpochMilli();
        if (endTime == null) {
            ended = "";
        } else {
            ended = DateUtils.formatDateTime(context, endTime, FORMAT_SHOW_DATE | FORMAT_SHOW_TIME);
        }
        if ((meetingStatus == MeetingStatus.ACTIVE) || (endTime == null)) {
            duration = context.getString(R.string.active);
        } else {
            final long durationTime = Math.abs(endTime - startedTime);
            duration = Long.toString(TimeUnit.MILLISECONDS.toSeconds(durationTime));
        }

        int attendeeCount = 0;
        if (meetingStatus == MeetingStatus.ACTIVE) {
            final SpacesTopic topic = chatViewModel.topicById(message.topicId);
            if (topic != null) {
                attendeeCount = topic.numConnectedMembers();
            }
        } else {
            attendeeCount = meeting.getAttendees().size();
        }
        attendees = attendeeCount;
    }

    @Override
    public int getType() {
        return VIEW_TYPE_MEETING;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getCreatedDate() {
        return started;
    }

    public String getTitle() {
        return title;
    }

    public LoganMessage getMessage() {
        return message;
    }

    @Override
    public boolean isOurs() {
        return false;
    }

    public String getEnded() {
        return ended;
    }

    public CharSequence getDuration() {
        return duration;
    }

    public int getAttendees() {
        return attendees;
    }

    public boolean isActive() {
        return isActive;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageMeetingModel that = (MessageMeetingModel) o;
        return id.equals(that.getId()) &&
                title.equals(that.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title);
    }

}

