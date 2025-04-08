package com.androidpractice.toolpool;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatHistoryFragment extends Fragment {
    private RecyclerView recyclerView;
    private ChatHistoryAdapter adapter;
    private DatabaseReference messagesRef;
    private DatabaseReference usersRef;

    public ChatHistoryFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        messagesRef = FirebaseDatabase.getInstance().getReference("messages");
        usersRef = FirebaseDatabase.getInstance().getReference("users");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat_history, container, false);

        recyclerView = view.findViewById(R.id.chat_history_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ChatHistoryAdapter();
        recyclerView.setAdapter(adapter);

        // Set chat click listener
        adapter.setOnChatClickListener((userId, username) -> {
            if (isAdded()) {
                ChatFragment chatFragment = ChatFragment.newInstance(userId, username);
                ((homeActivity) requireActivity()).replaceFragment(chatFragment);
            }
        });

        // Set profile click listener
        adapter.setOnProfileClickListener(userId -> {
            if (isAdded()) {
                PublicProfileFragment publicProfileFragment = PublicProfileFragment.newInstance(userId);
                ((homeActivity) requireActivity()).replaceFragment(publicProfileFragment);
            }
        });

        loadChatHistory();
        return view;
    }

    private void loadChatHistory() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        messagesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, String> chatPartners = new HashMap<>();

                for (DataSnapshot chatSnapshot : snapshot.getChildren()) {
                    String senderId = chatSnapshot.child("senderId").getValue(String.class);
                    String receiverId = chatSnapshot.child("receiverId").getValue(String.class);

                    if (senderId != null && receiverId != null) {
                        String otherUserId = senderId.equals(userId) ? receiverId : senderId;
                        if (!chatPartners.containsKey(otherUserId)) {
                            usersRef.child(otherUserId).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                                    if (userSnapshot.exists() && isAdded()) {
                                        String username = userSnapshot.child("name").getValue(String.class);
                                        if (username != null) {
                                            chatPartners.put(otherUserId, username);
                                            updateChatHistory(chatPartners);
                                        }
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    if (isAdded()) {
                                        Toast.makeText(requireContext(), "Error loading chat partner: " + error.getMessage(),
                                                Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                        }
                    }
                }

                if (chatPartners.isEmpty() && isAdded()) {
                    adapter.setChatPartners(new ArrayList<>());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Error loading chat history: " + error.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateChatHistory(Map<String, String> chatPartners) {
        List<ChatPartner> partners = new ArrayList<>();
        for (Map.Entry<String, String> entry : chatPartners.entrySet()) {
            partners.add(new ChatPartner(entry.getKey(), entry.getValue()));
        }
        adapter.setChatPartners(partners);
    }
}