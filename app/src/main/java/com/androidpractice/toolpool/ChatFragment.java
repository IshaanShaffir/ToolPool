package com.androidpractice.toolpool;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
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

public class ChatFragment extends Fragment {
    private static final String ARG_RECEIVER_ID = "receiverId";
    private static final String ARG_RECEIVER_NAME = "receiverName";

    private String receiverId;
    private String receiverName;
    private DatabaseReference messagesRef;
    private String currentUserId;
    private RecyclerView recyclerView;
    private ChatAdapter adapter;
    private EditText messageInput;
    private Button sendButton;
    private TextView chatTitle;

    public ChatFragment() {
        // Required empty public constructor
    }

    public static ChatFragment newInstance(String receiverId, String receiverName) {
        ChatFragment fragment = new ChatFragment();
        Bundle args = new Bundle();
        args.putString(ARG_RECEIVER_ID, receiverId);
        args.putString(ARG_RECEIVER_NAME, receiverName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            receiverId = getArguments().getString(ARG_RECEIVER_ID);
            receiverName = getArguments().getString(ARG_RECEIVER_NAME);
        }
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String chatId = generateChatId(currentUserId, receiverId);
        messagesRef = FirebaseDatabase.getInstance().getReference("messages").child(chatId);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        chatTitle = view.findViewById(R.id.chat_title);
        recyclerView = view.findViewById(R.id.chat_recycler_view);
        messageInput = view.findViewById(R.id.message_input);
        sendButton = view.findViewById(R.id.send_button);

        chatTitle.setText("Chat with " + receiverName);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ChatAdapter(currentUserId);
        recyclerView.setAdapter(adapter);

        loadMessages();

        sendButton.setOnClickListener(v -> sendMessage());

        return view;
    }

    private String generateChatId(String userId1, String userId2) {
        return userId1.compareTo(userId2) < 0 ? userId1 + "_" + userId2 : userId2 + "_" + userId1;
    }

    private void loadMessages() {
        messagesRef.child("messages").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Message> messages = new ArrayList<>();
                for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                    Message message = messageSnapshot.getValue(Message.class);
                    if (message != null) {
                        messages.add(message);
                    }
                }
                adapter.setMessages(messages);
                recyclerView.scrollToPosition(messages.size() - 1);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Error loading messages: " + error.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void sendMessage() {
        String text = messageInput.getText().toString().trim();
        if (text.isEmpty()) {
            return;
        }

        Map<String, Object> messageData = new HashMap<>();
        messageData.put("senderId", currentUserId);
        messageData.put("receiverId", receiverId);
        messageData.put("text", text);
        messageData.put("timestamp", System.currentTimeMillis());

        messagesRef.child("messages").push().setValue(messageData)
                .addOnSuccessListener(aVoid -> {
                    if (isAdded()) {
                        messageInput.setText("");
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Failed to send message: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });

        // Update chat participants
        Map<String, Object> participants = new HashMap<>();
        participants.put("senderId", currentUserId);
        participants.put("receiverId", receiverId);
        messagesRef.updateChildren(participants);
    }
}