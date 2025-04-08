package com.androidpractice.toolpool;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class ToolRequestsFragment extends Fragment {
    private RecyclerView recyclerView;
    private ToolRequestsAdapter adapter;
    private DatabaseReference toolRequestsRef;
    private DatabaseReference usersRef;
    private DatabaseReference listingsRef;
    private DatabaseReference messagesRef;

    public ToolRequestsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        toolRequestsRef = FirebaseDatabase.getInstance().getReference("tool_requests");
        usersRef = FirebaseDatabase.getInstance().getReference("users");
        listingsRef = FirebaseDatabase.getInstance().getReference("listings");
        messagesRef = FirebaseDatabase.getInstance().getReference("messages");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tool_requests, container, false);

        recyclerView = view.findViewById(R.id.tool_requests_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ToolRequestsAdapter();
        recyclerView.setAdapter(adapter);

        loadToolRequests();
        return view;
    }

    private void loadToolRequests() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            if (isAdded()) {
                Toast.makeText(requireContext(), "Please log in to view tool requests", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        String currentUserId = currentUser.getUid();
        toolRequestsRef.orderByChild("listerId").equalTo(currentUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<ToolRequest> requests = new ArrayList<>();
                for (DataSnapshot requestSnapshot : snapshot.getChildren()) {
                    String requestId = requestSnapshot.getKey();
                    String requesterId = requestSnapshot.child("requesterId").getValue(String.class);
                    String listingId = requestSnapshot.child("listingId").getValue(String.class);
                    String status = requestSnapshot.child("status").getValue(String.class);

                    if (requesterId != null && listingId != null && "pending".equals(status)) {
                        fetchRequestDetails(requestId, requesterId, listingId, requests);
                    }
                }
                adapter.setRequests(requests);
                if (requests.isEmpty() && isAdded()) {
                    Toast.makeText(requireContext(), "No pending tool requests", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Error loading requests: " + error.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void fetchRequestDetails(String requestId, String requesterId, String listingId, List<ToolRequest> requests) {
        usersRef.child(requesterId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                if (userSnapshot.exists() && isAdded()) {
                    String username = userSnapshot.child("name").getValue(String.class);
                    String profilePicUrl = userSnapshot.child("profilePictureUrl").getValue(String.class);

                    listingsRef.child(listingId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot listingSnapshot) {
                            if (listingSnapshot.exists() && isAdded()) {
                                Listing listing = listingSnapshot.getValue(Listing.class);
                                if (listing != null) {
                                    listing.setListingId(listingId);
                                    ToolRequest request = new ToolRequest(requestId, requesterId, username, profilePicUrl, listing);
                                    requests.add(request);
                                    adapter.notifyDataSetChanged();
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            if (isAdded()) {
                                Toast.makeText(requireContext(), "Error loading listing: " + error.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Error loading requester: " + error.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}

class ToolRequest {
    String requestId;
    String requesterId;
    String username;
    String profilePicUrl;
    Listing listing;

    ToolRequest(String requestId, String requesterId, String username, String profilePicUrl, Listing listing) {
        this.requestId = requestId;
        this.requesterId = requesterId;
        this.username = username;
        this.profilePicUrl = profilePicUrl;
        this.listing = listing;
    }
}

class ToolRequestsAdapter extends RecyclerView.Adapter<ToolRequestsAdapter.ViewHolder> {
    private List<ToolRequest> requests = new ArrayList<>();
    private DatabaseReference toolRequestsRef = FirebaseDatabase.getInstance().getReference("tool_requests");
    private DatabaseReference listingsRef = FirebaseDatabase.getInstance().getReference("listings");
    private DatabaseReference messagesRef = FirebaseDatabase.getInstance().getReference("messages");

    void setRequests(List<ToolRequest> requests) {
        this.requests = requests; // Reverted to direct assignment
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tool_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ToolRequest request = requests.get(position);

        holder.usernameText.setText(request.username);
        holder.toolTitleText.setText(request.listing.getTitle());
        holder.toolCategoryText.setText("Category: " + request.listing.getCategory());

        if (request.profilePicUrl != null && !request.profilePicUrl.isEmpty()) {
            Picasso.get()
                    .load(request.profilePicUrl)
                    .placeholder(R.drawable.ic_default_profile)
                    .error(R.drawable.ic_default_profile)
                    .into(holder.profilePicture);
        }

        holder.acceptButton.setOnClickListener(v -> {
            String listingId = request.listing.getListingId();
            toolRequestsRef.child(request.requestId).child("status").setValue("accepted")
                    .addOnSuccessListener(aVoid -> {
                        listingsRef.child(listingId).child("booked").setValue(true)
                                .addOnSuccessListener(aVoid2 -> {
                                    if (isActivityAlive(holder)) {
                                        Toast.makeText(holder.itemView.getContext(), "Request accepted and listing marked as booked", Toast.LENGTH_SHORT).show();
                                        removeRequestByListingId(listingId);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    if (isActivityAlive(holder)) {
                                        Toast.makeText(holder.itemView.getContext(), "Failed to update listing: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    })
                    .addOnFailureListener(e -> {
                        if (isActivityAlive(holder)) {
                            Toast.makeText(holder.itemView.getContext(), "Failed to accept request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        holder.denyButton.setOnClickListener(v -> {
            String listingId = request.listing.getListingId();
            toolRequestsRef.child(request.requestId).child("status").setValue("denied")
                    .addOnSuccessListener(aVoid -> {
                        if (isActivityAlive(holder)) {
                            Toast.makeText(holder.itemView.getContext(), "Request denied", Toast.LENGTH_SHORT).show();
                            removeRequestByListingId(listingId);
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (isActivityAlive(holder)) {
                            Toast.makeText(holder.itemView.getContext(), "Failed to deny: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        holder.chatButton.setOnClickListener(v -> {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser == null) {
                if (isActivityAlive(holder)) {
                    Toast.makeText(holder.itemView.getContext(), "Please log in to chat", Toast.LENGTH_SHORT).show();
                }
                return;
            }

            String currentUserId = currentUser.getUid();
            messagesRef.orderByChild("senderId").equalTo(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    boolean chatExists = false;
                    for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                        String receiverId = messageSnapshot.child("receiverId").getValue(String.class);
                        if (request.requesterId.equals(receiverId)) {
                            chatExists = true;
                            break;
                        }
                    }
                    if (isActivityAlive(holder)) {
                        Fragment fragment = chatExists ? new ChatHistoryFragment() : ChatFragment.newInstance(request.requesterId, request.username);
                        ((homeActivity) holder.itemView.getContext()).replaceFragment(fragment);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    if (isActivityAlive(holder)) {
                        Toast.makeText(holder.itemView.getContext(), "Error checking chat history: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        });
    }

    // Helper method to remove request by listingId
    private void removeRequestByListingId(String listingId) {
        for (int i = 0; i < requests.size(); i++) {
            if (requests.get(i).listing.getListingId().equals(listingId)) {
                requests.remove(i);
                notifyItemRemoved(i);
                break; // Assuming only one request per listingId
            }
        }
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    // Helper method to check if the activity is still alive
    private boolean isActivityAlive(ViewHolder holder) {
        return holder.itemView.getContext() != null && !((homeActivity) holder.itemView.getContext()).isFinishing();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView profilePicture;
        TextView usernameText;
        TextView toolTitleText;
        TextView toolCategoryText;
        ImageButton acceptButton;
        ImageButton denyButton;
        ImageButton chatButton;

        ViewHolder(View itemView) {
            super(itemView);
            profilePicture = itemView.findViewById(R.id.requester_profile_picture);
            usernameText = itemView.findViewById(R.id.requester_username);
            toolTitleText = itemView.findViewById(R.id.tool_title);
            toolCategoryText = itemView.findViewById(R.id.tool_category);
            acceptButton = itemView.findViewById(R.id.accept_button);
            denyButton = itemView.findViewById(R.id.deny_button);
            chatButton = itemView.findViewById(R.id.chat_button);
        }
    }
}