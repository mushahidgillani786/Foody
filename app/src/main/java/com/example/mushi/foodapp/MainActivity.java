package com.example.mushi.foodapp;

import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.firebase.ui.database.SnapshotParser;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;


public class MainActivity extends AppCompatActivity {

    private DatabaseReference mFirebaseDatabaseReference;
    private FirebaseRecyclerAdapter<Items, MessageViewHolder>
            mFirebaseAdapter;
//////////////////////////////////////



    ////////////////////////////////////
    private static final String TAG = "MainActivity";
    public static final String MESSAGES_CHILD = "Friday";
    private RecyclerView mMessageRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    private ProgressBar mProgressBar;



    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView price;
        ImageView photo;
        TextView item;
CardView cv;


        public MessageViewHolder(View v) {
            super(v);
            price = (TextView) itemView.findViewById(R.id.price);
            photo = (ImageView) itemView.findViewById(R.id.photo);
            item = (TextView) itemView.findViewById(R.id.item);
            cv=(CardView) itemView.findViewById(R.id.cv);
        }
    }







    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mMessageRecyclerView = (RecyclerView) findViewById(R.id.messageRecyclerView);
        mLinearLayoutManager = new LinearLayoutManager(this);
        mLinearLayoutManager.setStackFromEnd(false);
        mMessageRecyclerView.setLayoutManager(mLinearLayoutManager);
        // New child entries
        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
        SnapshotParser<Items> parser = new SnapshotParser<Items>() {
            @Override
            public Items parseSnapshot(DataSnapshot dataSnapshot) {
                Items items = dataSnapshot.getValue(Items.class);
                if (items != null) {
                    items.setId(dataSnapshot.getKey());
                }
                return items;
            }
        };


        DatabaseReference messagesRef = mFirebaseDatabaseReference.child(MESSAGES_CHILD);
        FirebaseRecyclerOptions<Items> options =
                new FirebaseRecyclerOptions.Builder<Items>()
                        .setQuery(messagesRef, parser)
                        .build();
        mFirebaseAdapter = new FirebaseRecyclerAdapter<Items, MessageViewHolder>(options) {
            @Override
            public MessageViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
                LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
                return new MessageViewHolder(inflater.inflate(R.layout.text_view_layout, viewGroup, false));
            }

            @Override
            protected void onBindViewHolder(final MessageViewHolder viewHolder,
                                            int position,
                                            Items items) {
                mProgressBar.setVisibility(ProgressBar.INVISIBLE);
                if (items.getItem() != null) {
                    viewHolder.item.setText(items.getItem());
                    viewHolder.item.setVisibility(TextView.VISIBLE);
                   // viewHolder.photo.setVisibility(ImageView.GONE);
                } else {
                    String imageUrl = items.getPhotoUrl();
                    if (imageUrl.startsWith("gs://")) {
                        StorageReference storageReference = FirebaseStorage.getInstance()
                                .getReferenceFromUrl(imageUrl);
                        storageReference.getDownloadUrl().addOnCompleteListener(
                                new OnCompleteListener<Uri>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Uri> task) {
                                        if (task.isSuccessful()) {
                                            String downloadUrl = task.getResult().toString();
                                            Glide.with(viewHolder.photo.getContext())
                                                    .load(downloadUrl)
                                                    .into(viewHolder.photo);
                                        } else {
                                            Log.w(TAG, "Getting download url was not successful.",
                                                    task.getException());
                                        }
                                    }
                                });
                    } else {
                        Glide.with(viewHolder.photo.getContext())
                                .load(items.getPhotoUrl())
                                .into(viewHolder.photo);
                    }
                    viewHolder.photo.setVisibility(ImageView.VISIBLE);
                    viewHolder.item.setVisibility(TextView.GONE);
                }


                viewHolder.price.setText(items.getPrice());
                if (items.getPhotoUrl() == null) {
                  //  viewHolder.messengerImageView.setImageDrawable(ContextCompat.getDrawable(MainActivity.this,
                    //        R.drawable.ic_account_circle_black_36dp));
                } else {
                    Glide.with(MainActivity.this)
                            .load(items.getPhotoUrl())
                            .into(viewHolder.photo);
                }

            }
        };

        mFirebaseAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                int friendlyMessageCount = mFirebaseAdapter.getItemCount();
                int lastVisiblePosition =
                        mLinearLayoutManager.findLastCompletelyVisibleItemPosition();
                // If the recycler view is initially being loaded or the
                // user is at the bottom of the list, scroll to the bottom
                // of the list to show the newly added message.
                if (lastVisiblePosition == -1 ||
                        (positionStart >= (friendlyMessageCount - 1) &&
                                lastVisiblePosition == (positionStart - 1))) {
                    mMessageRecyclerView.scrollToPosition(positionStart);
                }
            }
        });

        mMessageRecyclerView.setAdapter(mFirebaseAdapter);




    }
    @Override
    public void onPause() {
        mFirebaseAdapter.stopListening();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        mFirebaseAdapter.startListening();
    }

}
