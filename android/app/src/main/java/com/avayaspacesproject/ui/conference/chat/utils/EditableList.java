package com.avayaspacesproject.ui.conference.chat.utils;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;


import com.avayaspacesproject.R;

import java.util.ArrayList;
import java.util.List;

public class EditableList extends LinearLayout {
    private TextView empty;
    private List<EditableListItem> items;
    private List<View> itemsDeleted;
    private boolean editing = false;

    public EditableList(Context context) {
        super(context);
        ctor();
    }

    public EditableList(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        ctor();
    }

    public EditableList(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        ctor();
    }

    private void askDelete(EditableListItem item) {
        item.halfDeleted = true;
        item.collapse(true);
    }

    private void ctor() {
        setOrientation(VERTICAL);
        items = new ArrayList<>();

        empty = new TextView(getContext());
        empty.setText(R.string.empty);
        empty.setTextColor(Color.LTGRAY);
        empty.setGravity(Gravity.CENTER);

        addEmpty();
    }

    private void addEmpty() {
        addView(empty, ViewGroup.LayoutParams.MATCH_PARENT, (int) (getResources().getDisplayMetrics().density * 50));
    }

    public void reset() {
        removeAllViews();
        addEmpty();

        items.clear();
        resetDeleted();
    }

    public void addItem(View view) {
        addItem(view, false);
    }

    public void addItem(View view, boolean immutable) {
        if (items.size() == 0)
            removeAllViews(); // remove "Empty"

        EditableListItem item = new EditableListItem(view);
        item.immutable = immutable;
        if (editing)
            item.edit(true);
        items.add(item);
        addView(item, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    public List<View> items() {
        ArrayList<View> res = new ArrayList(items.size());
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).deleted)
                continue;
            res.add(items.get(i).nestedView);
        }
        return res;
    }

    private void edit(boolean edit) {
        if (editing == edit)
            return;
        editing = edit;

        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).deleted)
                continue;
            items.get(i).edit(edit);
        }
    }

    public List<View> itemsDeleted() {
        return itemsDeleted;
    }

    public void resetDeleted() {
        itemsDeleted = null;
    }

    public void startEdit() {
        edit(true);
    }

    public void commit() {
        if (!editing)
            return;

        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).halfDeleted) {
                items.get(i).halfDeleted = false;
                items.get(i).deleted = true;
                if (itemsDeleted == null)
                    itemsDeleted = new ArrayList<>();
                itemsDeleted.add(items.get(i).nestedView);
            }
        }
        edit(false);
    }

    public void rollback() {
        if (!editing)
            return;

        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).halfDeleted) {
                items.get(i).halfDeleted = false;
                items.get(i).collapse(false);
            }
        }
        edit(false);
    }

    private class EditableListItem extends LinearLayout implements OnClickListener {
        public boolean deleted;
        public boolean halfDeleted;
        public boolean immutable;
        public View nestedView;
        private ImageView buttonDelete;


        public EditableListItem(View nested) {
            super(nested.getContext());
            setOrientation(HORIZONTAL);
            setGravity(Gravity.CENTER_VERTICAL);

            LayoutParams lp = new LayoutParams(0, LayoutParams.WRAP_CONTENT);
            lp.weight = 1;
            addView(nested, lp);
            nestedView = nested;
        }

        public void edit(boolean edit) {
            if (immutable)
                return;

            if (buttonDelete == null) {
                if (!edit)
                    return;

                buttonDelete = new ImageView(getContext());
                buttonDelete.setImageResource(R.drawable.ic_close_24);
                buttonDelete.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                buttonDelete.setOnClickListener(this);
                int sizeButton = getResources().getDimensionPixelSize(R.dimen.btn_height_round);
                addView(buttonDelete, sizeButton, sizeButton);
            }

            buttonDelete.setVisibility(edit ? View.VISIBLE : View.GONE);
        }

        public void collapse(boolean collapse) {
            setVisibility(!collapse ? View.VISIBLE : View.GONE);
        }

        @Override
        public void onClick(View view) {
            if (immutable)
                return;

            askDelete(this);
        }
    }

}
