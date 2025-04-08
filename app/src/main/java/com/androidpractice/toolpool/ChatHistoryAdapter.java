package com.androidpractice.toolpool;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class ChatHistoryAdapter extends RecyclerView.Adapter<ChatHistoryAdapter.ChatPartnerViewHolder> {
    private List<ChatPartner> chatPartners = new ArrayList<>();
    private OnChatClickListener chatListener;
    private OnProfileClickListener profileListener;
    private DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");

    // Listener for chat navigation
    public interface OnChatClickListener {
        void onChatClick(String userId, String username);
    }

    // Listener for profile navigation
    public interface OnProfileClickListener {
        void onProfileClick(String userId);
    }

    public void setChatPartners(List<ChatPartner> chatPartners) {
        this.chatPartners = chatPartners;
        notifyDataSetChanged();
    }

    public void setOnChatClickListener(OnChatClickListener listener) {
        this.chatListener = listener;
    }

    public void setOnProfileClickListener(OnProfileClickListener listener) {
        this.profileListener = listener;
    }

    @NonNull
    @Override
    public ChatPartnerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_history, parent, false);
        return new ChatPartnerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatPartnerViewHolder holder, int position) {
        ChatPartner partner = chatPartners.get(position);
        holder.username.setText(partner.getUsername());

        // Load profile picture
        usersRef.child(partner.getUserId()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String profilePicUrl = snapshot.child("profilePictureUrl").getValue(String.class);
                    if (profilePicUrl != null && !profilePicUrl.isEmpty()) {
                        Picasso.get()
                                .load(profilePicUrl)
                                .placeholder(R.drawable.ic_default_profile)
                                .error(R.drawable.ic_default_profile)
                                .into(holder.profilePicture);
                    } else {
                        holder.profilePicture.setImageResource(R.drawable.ic_default_profile);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                holder.profilePicture.setImageResource(R.drawable.ic_default_profile);
            }
        });

        // Chat click (entire item)
        holder.itemView.setOnClickListener(v -> {
            if (chatListener != null) {
                chatListener.onChatClick(partner.getUserId(), partner.getUsername());
            }
        });

        // Profile click (profile picture only)
        holder.profilePicture.setOnClickListener(v -> {
            if (profileListener != null) {
                profileListener.onProfileClick(partner.getUserId());
            }
        });
    }

    @Override
    public int getItemCount() {
        return chatPartners.size();
    }

    static class ChatPartnerViewHolder extends RecyclerView.ViewHolder {
        ImageView profilePicture;
        TextView username;

        ChatPartnerViewHolder(@NonNull View itemView) {
            super(itemView);
            profilePicture = itemView.findViewById(R.id.partner_profile_picture);
            username = itemView.findViewById(R.id.partner_username);
        }
    }
}