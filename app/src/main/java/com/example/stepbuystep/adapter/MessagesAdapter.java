package com.example.stepbuystep.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.stepbuystep.R;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.ViewHolder> {

    public static class MessageItem {
        public String messageId;
        public String coachName;
        public String messageText;
        public Timestamp timestamp;
        public boolean isRead;

        public MessageItem(String messageId, String coachName, String messageText,
                           Timestamp timestamp, boolean isRead) {
            this.messageId = messageId;
            this.coachName = coachName;
            this.messageText = messageText;
            this.timestamp = timestamp;
            this.isRead = isRead;
        }
    }

    public interface OnMessageActionListener {
        void onDeleteMessage(String messageId);
    }

    private List<MessageItem> messages = new ArrayList<>();
    private OnMessageActionListener listener;

    public void setMessages(List<MessageItem> messages) {
        this.messages = messages;
        notifyDataSetChanged();
    }

    public void setListener(OnMessageActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MessageItem message = messages.get(position);

        holder.tvCoachName.setText(message.coachName);
        holder.tvMessageText.setText(message.messageText);

        // Show "NEW" badge if unread
        if (! message.isRead) {
            holder.tvNewBadge. setVisibility(View.VISIBLE);
        } else {
            holder.tvNewBadge. setVisibility(View.GONE);
        }

        // Format timestamp
        if (message.timestamp != null) {
            holder.tvTimestamp.setText(formatTimestamp(message.timestamp));
        } else {
            holder.tvTimestamp.setText("");
        }

        // Delete button
        holder.btnDeleteMessage.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteMessage(message.messageId);
            }
        });
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    private String formatTimestamp(Timestamp timestamp) {
        Date date = timestamp.toDate();
        Date now = new Date();

        long diffInMillis = now.getTime() - date.getTime();
        long diffInMinutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis);
        long diffInHours = TimeUnit.MILLISECONDS.toHours(diffInMillis);
        long diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis);

        if (diffInMinutes < 1) {
            return "Just now";
        } else if (diffInMinutes < 60) {
            return diffInMinutes + " min ago";
        } else if (diffInHours < 24) {
            return diffInHours + " hour" + (diffInHours > 1 ? "s" : "") + " ago";
        } else if (diffInDays < 7) {
            return diffInDays + " day" + (diffInDays > 1 ?  "s" : "") + " ago";
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            return sdf.format(date);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout messageCard;
        TextView tvNewBadge;
        TextView tvCoachName;
        TextView tvTimestamp;
        TextView tvMessageText;
        ImageView btnDeleteMessage;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            messageCard = itemView.findViewById(R.id.messageCard);
            tvNewBadge = itemView. findViewById(R.id.tvNewBadge);
            tvCoachName = itemView.findViewById(R.id.tvCoachName);
            tvTimestamp = itemView.findViewById(R.id. tvTimestamp);
            tvMessageText = itemView.findViewById(R.id.tvMessageText);
            btnDeleteMessage = itemView.findViewById(R.id.btnDeleteMessage);
        }
    }
    public void removeMessage(String messageId) {
        for (int i = 0; i < messages.size(); i++) {
            if (messages.get(i).messageId.equals(messageId)) {
                messages.remove(i);
                notifyItemRemoved(i);
                break;
            }
        }
    }
}