package com.avayaspacesproject.ui.conference.chat;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import com.avayaspacesproject.R;
import static android.text.TextUtils.isEmpty;
import static com.avayaspacesproject.ui.conference.chat.utils.MessageUtil.textToHtml;

public class ChatInput extends LinearLayout implements View.OnClickListener {
    public static final int ATTACH_REQ_FILE = 0;
    public static final int ATTACH_REQ_PHOTO = 1;
    public static final int ATTACH_REQ_VIDEO = 2;
    private ChatInputListener listener;
    private EditText fieldEdit;
    private ImageView fieldAttach;
    private ImageView fieldSend;
    private DocumentFile attachment;
    private TextView attachmentText;
    private View attachmentContainer;
    private View closeAttachmentTypeButton;
    private View attachmentTypeFileButton;
    private View attachmentTypePhotoButton;
    private View attachmentTypeVideoButton;
    private View attachmentTypeContainer;
    private String lastTopicId;

    // Chat Input Preferences
    public static final String PREF_FILE_CHAT_STORE = "ChatInput";
    public static final String CHAT_STORE_PREFIX = "chat.input.s.";
    public static final String CHAT_STORE_PREFIX_TIME = "chat.input.t.";

    public ChatInput(Context context) {
        super(context);
    }

    public ChatInput(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ChatInput(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public static ChatInput inflate(LayoutInflater factory) {
        return (ChatInput) factory.inflate(R.layout.chat_input, null, false);
    }

    public static ChatInput inflate(ViewGroup parent) {
        return (parent == null) ? null : inflate(parent.getContext());
    }

    public static ChatInput inflate(Context context) {
        LayoutInflater factory = LayoutInflater.from(context);
        return inflate(factory);
    }

    public void setListener(ChatInputListener listener) {
        this.listener = listener;
    }

    public void saveUnsentMessage() {
        saveUnsentMessage(lastTopicId);
    }

    private void saveUnsentMessage(String topicId) {
        final SharedPreferences.Editor edit = getContext()
                .getSharedPreferences(PREF_FILE_CHAT_STORE, Context.MODE_PRIVATE)
                .edit();
        if (isEmpty(topicId)) {
            edit
                    .remove(CHAT_STORE_PREFIX + topicId)
                    .remove(CHAT_STORE_PREFIX_TIME + topicId)
                    .apply();
        } else {
            edit
                    .putString(CHAT_STORE_PREFIX + topicId, fieldEdit.getText().toString())
                    .putLong(CHAT_STORE_PREFIX_TIME + topicId, System.currentTimeMillis())
                    .apply();
        }
    }

    private void loadUnsentMessage(@Nullable String topicId) {
        String s = "";

        if (!isEmpty(topicId)) {
            s = getContext()
                    .getSharedPreferences(PREF_FILE_CHAT_STORE, Context.MODE_PRIVATE)
                    .getString(CHAT_STORE_PREFIX + topicId, "");
        }

        fieldEdit.setText(s);
        fieldEdit.setSelection(fieldEdit.getText().length());
    }

    public void prepareForTopic(@Nullable String topicId) {
        if (topicId == null) {
            topicId = "";
        }

        if (lastTopicId == null) {
            lastTopicId = "";
        }

        if (topicId.equals(lastTopicId)) {
            return;
        }

        saveUnsentMessage();

        lastTopicId = topicId;
        loadUnsentMessage(lastTopicId);
    }

    private void updateAttachment() {
        attachmentContainer.setVisibility(attachment == null ? View.GONE : View.VISIBLE);
        if (attachment != null) {
            attachmentText.setText(attachment.getName());
        }
    }

    public void addAttachment(DocumentFile documentFile) {
        if (documentFile == null) {
            return;
        }
        attachment = documentFile;
        updateAttachment();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        bind();
    }

    private void bind() {
        attachmentContainer = findViewById(R.id.attachment_container);

        attachmentText = findViewById(R.id.attachment_text);

        View removeAttachmentButton = findViewById(R.id.attachment_remove);
        removeAttachmentButton.setOnClickListener(view -> onTapRemoveAttachment());

        fieldEdit = findViewById(R.id.edit);
        fieldAttach = findViewById(R.id.attach);
        fieldAttach.setVisibility(View.GONE); //SDK temporary change. Remove it
        fieldSend = findViewById(R.id.send);

        fieldEdit.setHint(R.string.write_a_message);

        fieldAttach.setOnClickListener(this);

        fieldSend.setOnClickListener(this);

        closeAttachmentTypeButton = findViewById(R.id.attachment_type_close);
        closeAttachmentTypeButton.setOnClickListener(this);

        attachmentTypeFileButton = findViewById(R.id.attachment_type_file);
        attachmentTypeFileButton.setOnClickListener(this);

        attachmentTypePhotoButton = findViewById(R.id.attachment_type_photo);
        attachmentTypePhotoButton.setOnClickListener(this);

        attachmentTypeVideoButton = findViewById(R.id.attachment_type_video);
        attachmentTypeVideoButton.setOnClickListener(this);

        attachmentTypeContainer = findViewById(R.id.attachment_type_container);
    }

    private void onTapRemoveAttachment() {
        attachment = null;
        updateAttachment();
    }

    public CharSequence getText() {
        return fieldEdit.getText();
    }

    public void setText(CharSequence text) {
        fieldEdit.setText(text);
    }

    private void attachmentTypeContainerSetVisibility(int visibility) {
        attachmentTypeContainer.setVisibility(visibility);
        findViewById(R.id.edit_container).setAlpha(visibility == VISIBLE ? 0.4f : 1.0f);
    }

    @Override
    public void onClick(View v) {

        if (v == fieldAttach) {
            attachmentTypeContainerSetVisibility(attachmentTypeContainer.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        } else if (v == closeAttachmentTypeButton) {
            attachmentTypeContainerSetVisibility(View.GONE);
        } else if (v == attachmentTypeFileButton) {
//            if (listener != null) {
//                listener.onAttachRequest(ATTACH_REQ_FILE);
//            }
            attachmentTypeContainerSetVisibility(View.GONE);
        } else if (v == attachmentTypePhotoButton) {
//            if (listener != null) {
//                listener.onAttachRequest(ATTACH_REQ_PHOTO);
//            }
            attachmentTypeContainerSetVisibility(View.GONE);
        } else if (v == attachmentTypeVideoButton) {
//            if (listener != null) {
//                listener.onAttachRequest(ATTACH_REQ_VIDEO);
//            }
            attachmentTypeContainerSetVisibility(View.GONE);
        } else if (v == fieldSend) {
            String text = fieldEdit.getText().toString();
            if (!text.isEmpty() || (attachment != null)) {
                if (listener != null) {
                    text = textToHtml(text);
                    listener.onSendWithAttachment(text, attachment);
                }
            }

            clear();
        }
    }

    private void clear() {
        fieldEdit.setText("");
        attachment = null;
        updateAttachment();
        saveUnsentMessage(lastTopicId);
    }

    public interface ChatInputListener {
        void onSendWithAttachment(@NonNull String text, @Nullable DocumentFile documentFile);

        void onAttachRequest(int requestType);
    }
}
