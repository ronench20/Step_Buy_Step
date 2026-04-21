package com.example.stepbuystep.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.stepbuystep.R;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.List;

public class PendingRequestsAdapter extends RecyclerView.Adapter<PendingRequestsAdapter. ViewHolder> {

    public static class PendingRequest {
        public String userId;
        public String name;
        public String email;
        public String city;
        public int age;
        public String gender;
        /** Optional Firebase Storage URL for the trainee's profile picture. */
        public String profileImageUrl;

        public PendingRequest(String userId, String name, String email, String city,
                              int age, String gender) {
            this(userId, name, email, city, age, gender, null);
        }

        public PendingRequest(String userId, String name, String email, String city,
                              int age, String gender, String profileImageUrl) {
            this.userId = userId;
            this.name = name;
            this.email = email;
            this.city = city;
            this.age = age;
            this.gender = gender;
            this.profileImageUrl = profileImageUrl;
        }
    }

    public interface OnRequestActionListener {
        void onApprove(String userId);
        void onReject(String userId);
    }

    private List<PendingRequest> requests = new ArrayList<>();
    private OnRequestActionListener listener;

    public void setRequests(List<PendingRequest> requests) {
        this.requests = requests;
        notifyDataSetChanged();
    }

    public void setListener(OnRequestActionListener listener) {
        this.listener = listener;
    }

    public void removeRequest(String userId) {
        for (int i = 0; i < requests.size(); i++) {
            if (requests.get(i).userId.equals(userId)) {
                requests.remove(i);
                notifyItemRemoved(i);
                break;
            }
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pending_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PendingRequest request = requests. get(position);

        // Initials are always written so they show while Glide is loading AND as a
        // fallback when the trainee has no profileImageUrl.
        String initial = (request.name != null && !request.name.isEmpty())
                ? request.name.substring(0, 1).toUpperCase()
                : "?";
        holder.tvInitial.setText(initial);

        // Flip between image and initial based on whether a URL is available.
        if (!TextUtils.isEmpty(request.profileImageUrl)) {
            holder.ivAvatar.setVisibility(View.VISIBLE);
            Glide.with(holder.itemView.getContext())
                    .load(request.profileImageUrl)
                    .circleCrop()
                    .into(holder.ivAvatar);
        } else {
            holder.ivAvatar.setVisibility(View.GONE);
            Glide.with(holder.itemView.getContext()).clear(holder.ivAvatar);
        }

        holder.tvTraineeName.setText(request.name);
        holder.tvTraineeEmail.setText(request.email);
        holder.tvTraineeCity.setText(request.city);
        holder.tvTraineeAge.setText(String.valueOf(request.age));
        holder.tvTraineeGender.setText(request. gender);

        // Approve button
        holder.btnApprove.setOnClickListener(v -> {
            if (listener != null) {
                listener.onApprove(request.userId);
            }
        });

        // Reject button
        holder. btnReject.setOnClickListener(v -> {
            if (listener != null) {
                listener.onReject(request.userId);
            }
        });
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvInitial;
        ShapeableImageView ivAvatar;
        TextView tvTraineeName;
        TextView tvTraineeEmail;
        TextView tvTraineeCity;
        TextView tvTraineeAge;
        TextView tvTraineeGender;
        LinearLayout btnApprove;
        LinearLayout btnReject;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvInitial = itemView. findViewById(R.id.tvInitial);
            ivAvatar = itemView.findViewById(R.id.ivRequestAvatar);
            tvTraineeName = itemView.findViewById(R. id.tvTraineeName);
            tvTraineeEmail = itemView.findViewById(R.id. tvTraineeEmail);
            tvTraineeCity = itemView.findViewById(R.id.tvTraineeCity);
            tvTraineeAge = itemView. findViewById(R.id.tvTraineeAge);
            tvTraineeGender = itemView. findViewById(R.id.tvTraineeGender);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnReject = itemView.findViewById(R.id.btnReject);
        }
    }
}
