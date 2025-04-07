package com.androidpractice.toolpool;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MyListingsAdapter extends RecyclerView.Adapter<MyListingsAdapter.ListingViewHolder> {
    private List<Listing> listings = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Listing listing);
    }

    public void setListings(List<Listing> listings) {
        this.listings = listings;
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ListingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_listing_card, parent, false);
        return new ListingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ListingViewHolder holder, int position) {
        Listing listing = listings.get(position);
        holder.title.setText(listing.getTitle());
        holder.category.setText(listing.getCategory());
        holder.description.setText(listing.getDescription());
        holder.address.setText(listing.getAddress());
        holder.deposit.setText(String.format(Locale.getDefault(), "Deposit: $%.2f", listing.getDeposit()));
        holder.condition.setText("Condition: " + listing.getCondition());

        if (listing.getPhotoUrls() != null && !listing.getPhotoUrls().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(listing.getPhotoUrls().get(0))
                    .apply(new RequestOptions()
                            .override(100, 100)
                            .placeholder(android.R.drawable.ic_menu_gallery)
                            .error(android.R.drawable.ic_menu_gallery))
                    .into(holder.image);
        } else {
            holder.image.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(listing);
            }
        });
    }

    @Override
    public int getItemCount() {
        return listings.size();
    }

    static class ListingViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView title, category, description, address, deposit, condition;

        ListingViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.listing_image);
            title = itemView.findViewById(R.id.listing_title);
            category = itemView.findViewById(R.id.listing_category);
            description = itemView.findViewById(R.id.listing_description);
            address = itemView.findViewById(R.id.listing_address);
            deposit = itemView.findViewById(R.id.listing_deposit);
            condition = itemView.findViewById(R.id.listing_condition);
        }
    }
}