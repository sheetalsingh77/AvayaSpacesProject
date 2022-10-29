package com.avayaspacesproject.ui.conference.members;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import com.avayaspacesproject.ui.conference.members.MemberModel;

public class MembersDiffCallback extends DiffUtil.ItemCallback<MemberModel> {
    @Override
    public boolean areItemsTheSame(
            @NonNull MemberModel oldMember, @NonNull MemberModel newMember) {
        return oldMember.getId().equals(newMember.getId());
    }

    @Override
    public boolean areContentsTheSame(@NonNull MemberModel oldMember, @NonNull MemberModel newMember) {
        return oldMember.equals(newMember);
    }
}

