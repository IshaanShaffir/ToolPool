package com.androidpractice.toolpool;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class PhotosAdapter extends RecyclerView.Adapter<PhotosAdapter.PhotoViewHolder> {
    private List<Uri> photoUris;

    public PhotosAdapter(List<Uri> photoUris) {
        this.photoUris = photoUris;
    }

    @Override
    public PhotoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.photo_item, parent, false);
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(PhotoViewHolder holder, int position) {
        Uri uri = photoUris.get(position);
        Glide.with(holder.itemView.getContext())
                .load(uri)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.placeholder_image)
                .into(holder.imageView);
    }

    @Override
    public int getItemCount() {
        return photoUris.size();
    }

    static class PhotoViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        PhotoViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.photo_image);
        }
    }
}