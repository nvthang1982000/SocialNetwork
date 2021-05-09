package com.example.socialnetwork;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class FriendsActivity extends AppCompatActivity {
    private RecyclerView myFriendList;

    private DatabaseReference UsersRef, FriendsRef;
    private FirebaseAuth mAuth;
    private String online_user_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);

        mAuth = FirebaseAuth.getInstance();
        online_user_id = mAuth.getCurrentUser().getUid();
        FriendsRef = FirebaseDatabase.getInstance().getReference().child("Friends").child(online_user_id);
        UsersRef = FirebaseDatabase.getInstance().getReference().child("Users");

        myFriendList = (RecyclerView) findViewById(R.id.friend_list);
        myFriendList.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        myFriendList.setLayoutManager(linearLayoutManager);

        DisplayAllFriends();
    }
    public void updateUserStatus(String state){
        String saveCurrentDate , saveCurrentTime;
        Calendar calFordDate = Calendar.getInstance();
        SimpleDateFormat currentDate = new SimpleDateFormat("dd-MMMM-yyyy");
        saveCurrentDate = currentDate.format(calFordDate.getTime());

        Calendar calFordTime = Calendar.getInstance();
        SimpleDateFormat currentTime = new SimpleDateFormat("HH:mm:s aa");
        saveCurrentTime = currentTime.format(calFordTime.getTime());

        Map currentStateMap = new HashMap();
        currentStateMap.put("date",saveCurrentDate);
        currentStateMap.put("time",saveCurrentTime);
        currentStateMap.put("type",state);

        UsersRef.child(online_user_id).child("userState").updateChildren(currentStateMap);
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateUserStatus("online");
    }

    @Override
    protected void onStop() {
        super.onStop();
        updateUserStatus("offline");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        updateUserStatus("offline");
    }

    public static class FriendsViewHolder extends RecyclerView.ViewHolder
    {
        View mView;
        ImageView onlineStatusView;

        public FriendsViewHolder(View itemView)
        {
            super(itemView);
            mView = itemView;
            onlineStatusView = (ImageView) itemView.findViewById(R.id.all_user_online_icon);

        }
        public void setFullname(String fullname)
        {
            TextView myName = (TextView) mView.findViewById(R.id.all_users_profile_full_name);
            myName.setText(fullname);
        }

        public void setProfileimage(Context ctx, String profileimage)
        {
            CircleImageView myImage = (CircleImageView) mView.findViewById(R.id.all_users_profile_image);
            Picasso.with(ctx).load(profileimage).into(myImage);
        }
        public void setDate(String date) {
            TextView friendsDate = (TextView) mView.findViewById(R.id.all_users_status);
            friendsDate.setText("Friends since: "+date);
        }
    }
    private void DisplayAllFriends(){
        FirebaseRecyclerAdapter<Friends, FriendsViewHolder> firebaseRecyclerAdapter =
                new FirebaseRecyclerAdapter<Friends, FriendsViewHolder>(
                        Friends.class,
                        R.layout.all_user_display_layout,
                        FriendsViewHolder.class,
                        FriendsRef
                ){

                    @Override
                    protected void populateViewHolder(FriendsViewHolder friendsViewHolder, Friends friends, int i) {
                        friendsViewHolder.setDate(friends.getDate());
                        final String usersIDs = getRef(i).getKey();
                        UsersRef.child(usersIDs).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if(snapshot.exists()){
                                    final String userName = snapshot.child("fullname").getValue().toString();
                                    final String profileImage = snapshot.child("profileimage").getValue().toString();
                                    final String type;

                                    if(snapshot.hasChild("userState")){
                                        type = snapshot.child("userState").child("type").getValue().toString();
                                        if(type.equals("online")){
                                            friendsViewHolder.onlineStatusView.setVisibility(View.VISIBLE);
                                        }
                                        else{
                                            friendsViewHolder.onlineStatusView.setVisibility(View.INVISIBLE);
                                        }
                                    }


                                    friendsViewHolder.setFullname(userName);
                                    friendsViewHolder.setProfileimage(getApplication(),profileImage);
                                    friendsViewHolder.mView.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            CharSequence options[] = new CharSequence[]
                                            {
                                                userName + "'s Profile",
                                                    "Send Message"
                                            };
                                            AlertDialog.Builder builder = new AlertDialog.Builder(FriendsActivity.this);
                                            builder.setTitle("Select Option");
                                            builder.setItems(options, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                    if(i == 0){
                                                        Intent profileIntent = new Intent(FriendsActivity.this, PersonProfileActivity.class);
                                                        profileIntent.putExtra("visit_user_id",usersIDs);
                                                        startActivity(profileIntent);
                                                    }
                                                    if( i == 1){
                                                        Intent chatIntent = new Intent(FriendsActivity.this, ChatActivity.class);
                                                        chatIntent.putExtra("visit_user_id",usersIDs);
                                                        chatIntent.putExtra("userName",userName);
                                                        startActivity(chatIntent);
                                                    }
                                                }
                                            });
                                            builder.show();
                                        }
                                    });
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });
                    }
                };
        myFriendList.setAdapter(firebaseRecyclerAdapter);
    }

}