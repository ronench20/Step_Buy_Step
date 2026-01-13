package com.example.stepbuystep.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.stepbuystep.R;

import java.util.ArrayList;
import java.util.List;

public class TeamMembersAdapter extends RecyclerView. Adapter<TeamMembersAdapter. ViewHolder> {

    public static class TeamMember {
        public String userId;
        public String name;
        public String email;
        public String city;

        public TeamMember(String userId, String name, String email, String city) {
            this.userId = userId;
            this.name = name;
            this.email = email;
            this.city = city;
        }
    }

    public interface OnRemoveMemberListener {
        void onRemove(String userId, String name);
    }

    private List<TeamMember> members = new ArrayList<>();
    private OnRemoveMemberListener listener;

    public void setMembers(List<TeamMember> members) {
        this.members = members;
        notifyDataSetChanged();
    }

    public void setListener(OnRemoveMemberListener listener) {
        this.listener = listener;
    }

    public int getMemberCount() {
        return members.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_team_member, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TeamMember member = members.get(position);

        // Set initial (first letter of name)
        String initial = member.name.length() > 0 ? member.name.substring(0, 1).toUpperCase() : "? ";
        holder.tvInitial.setText(initial);

        holder.tvTraineeName.setText(member.name);
        holder.tvTraineeEmail.setText(member.email);
        holder.tvTraineeCity.setText(member.city);

        // Remove button
        holder.btnRemove.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRemove(member.userId, member. name);
            }
        });
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvInitial;
        TextView tvTraineeName;
        TextView tvTraineeEmail;
        TextView tvTraineeCity;
        LinearLayout btnRemove;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvInitial = itemView.findViewById(R.id.tvInitial);
            tvTraineeName = itemView.findViewById(R.id.tvTraineeName);
            tvTraineeEmail = itemView.findViewById(R.id.tvTraineeEmail);
            tvTraineeCity = itemView.findViewById(R.id. tvTraineeCity);
            btnRemove = itemView.findViewById(R.id.btnRemove);
        }
    }
}