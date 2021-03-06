package hcmute.spkt.chatappzalo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import hcmute.spkt.chatappzalo.Adapters.MessageAdapter;
import hcmute.spkt.chatappzalo.Model.Chat;
import hcmute.spkt.chatappzalo.Model.Users;

public class MessageActivity extends AppCompatActivity {
    //id ng?????i nh???n, tin nh???n, id ng?????i g???i
    String friendid, message, myid;

    //c??c bi???n d??ng ????? set cho toolbar
    CircleImageView imageViewOnToolbar;
    TextView usernameOnToolbar;
    Toolbar toolbar;

    //bi???n l??u th??ng tin t??i kho???n t??? firebase
    FirebaseUser firebaseUser;

    //c??c bi???n set giao di???n
    EditText et_message;
    Button send;
    ImageButton attachBtn;

    DatabaseReference reference;

    //danh s??ch c??c tin nh???n
    List<Chat> chatList;
    MessageAdapter messageAdapter;
    RecyclerView recyclerView;
    ValueEventListener seenlistener;

    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int STORAGE_REQUEST_CODE = 200;

    private static final int IMAGE_PICK_CAMERA_CODE = 300;
    private static final int IMAGE_PICK_GALLERY_CODE = 400;

    String[] cameraPermissions;
    String[] storagePermissions;

    Uri image_uri = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        //Set toolbar
        toolbar = findViewById(R.id.toolbar_message);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        imageViewOnToolbar = findViewById(R.id.profile_image_toolbar_message);
        usernameOnToolbar = findViewById(R.id.username_ontoolbar_message);

        //set giao di???n khi nh???n tin
        recyclerView = findViewById(R.id.recyclerview_messages);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);

        et_message = findViewById(R.id.edit_message_text);
        send = findViewById(R.id.send_messsage_btn);
        attachBtn = findViewById(R.id.camera_gallery_btn);

        cameraPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        //l???y t??i kho???n ??ang ????ng nh???p
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        myid = firebaseUser.getUid();

        friendid = getIntent().getStringExtra("friendid"); //l???y id khi click v??o item c???a ng?????i d??ng ????
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        //ch??? ???????ng ?????n child c???n d??ng trong firebase
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users").child(friendid);

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //l???y d??? li???u t??? firebase g??n v??o ?????i t?????ng users ch???a t???t c??? th??ng tin c???a t??i kho???n receiver
                Users users = snapshot.getValue(Users.class);
                //g??n username c???a t??i kho???n l??n toolbar
                usernameOnToolbar.setText(users.getUsername());
                //g??n h??nh ???nh, n???u kh??ng c?? ???nh s??? set ???nh m???c ?????nh
                if(users.getImageURL().equals("default")){
                    imageViewOnToolbar.setImageResource(R.drawable.user);
                }else{
                    Glide.with(getApplicationContext()).load(users.getImageURL()).into(imageViewOnToolbar);
                }

                //g???i h??m ?????c tin nh???n ????? hi???n th??? t???t c??? tin nh???n
                readMessage(myid, friendid, users.getImageURL());

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        seenMessage(friendid);

        //x??? l?? c??c s??? ki???n ??? ?? nh???p tin nh???n
        et_message.addTextChangedListener(new TextWatcher() {
            //set b???t/t???t n??t g???i khi tin nh???n tr???ng
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if(s.toString().length()>0){
                    send.setEnabled(true);
                }else{
                    send.setEnabled(false);
                }
            }

            //x??? l?? ??o???n tin nh???n
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String text = et_message.getText().toString();
                if(!text.startsWith("")){
                    et_message.getText().insert(0,"");
                }

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });


        //x??? l?? s??? ki???n khi b???m n??t g???i
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                message = et_message.getText().toString();
                sendMessage(myid, friendid, message);
                et_message.setText(" ");
            }
        });

        attachBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                showImagePickDialog();
            }
        });

    }

    private void showImagePickDialog() {
        String[] options = {"Camera", "Gallery"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Ch???n h??nh ???nh t???");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0){
                    //ch???n camera
                    if(!checkCameraPermission()){
                        requestCameraPermission();
                    }
                    else{
                        pickFromCamera();
                    }
                }

                if(which == 1){
                    //Ch???n th?? vi???n ???nh
                    if(!checkStoragePermission()){
                        requestStoragePermission();
                    }
                    else{
                        pickFromGallery();
                    }
                }
            }
        });

        builder.create().show();
    }

    private void pickFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_PICK_GALLERY_CODE);
    }

    private void pickFromCamera() {
        ContentValues cv = new ContentValues();
        cv.put(MediaStore.Images.Media.TITLE, "Temp Pick");
        cv.put(MediaStore.Images.Media.DESCRIPTION, "Temp Descr");
        image_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        startActivityForResult(intent, IMAGE_PICK_CAMERA_CODE);
    }

    private boolean checkCameraPermission(){
        boolean result = ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED);
        boolean result1 = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        return result && result1;
    }

    private void requestCameraPermission(){
        ActivityCompat.requestPermissions(this, cameraPermissions, CAMERA_REQUEST_CODE);
    }

    private boolean checkStoragePermission(){
        boolean result = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        return result;
    }

    private void requestStoragePermission(){
        ActivityCompat.requestPermissions(this, storagePermissions, STORAGE_REQUEST_CODE);
    }

    //H??m set trang th??i tin nh???n ???? ?????c
    private void seenMessage(String friendid){
        reference = FirebaseDatabase.getInstance().getReference("Chat");

        seenlistener = reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds: snapshot.getChildren()){
                    Chat chat = ds.getValue(Chat.class);

                    if(chat.getReceiver().equals(myid) && chat.getSender().equals(friendid)){
                        HashMap<String, Object> hashMap = new HashMap<>();
                        hashMap.put("isseen", true);
                        ds.getRef().updateChildren(hashMap);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    //h??m ?????c tin nh???n
    private void readMessage(String myid, String friendid, String imageURL) {
        chatList = new ArrayList<>();

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Chat");

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                chatList.clear();

                for (DataSnapshot ds: snapshot.getChildren()) {

                    Chat chat = ds.getValue(Chat.class);

                    if (chat.getSender().equals(myid) && chat.getReceiver().equals(friendid) ||
                            chat.getSender().equals(friendid) && chat.getReceiver().equals(myid)) {

                        chatList.add(chat);
                    }

                    messageAdapter = new MessageAdapter(MessageActivity.this, chatList, imageURL);
                    recyclerView.setAdapter(messageAdapter);

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    //h??m g???i tin nh???n
    private void sendMessage(String myid, String friendid, String message) {

        final DatabaseReference reference = FirebaseDatabase.getInstance().getReference();

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("sender", myid);
        hashMap.put("receiver", friendid);
        hashMap.put("message", message);
        hashMap.put("isseen", false);
        hashMap.put("type", "text");

        reference.child("Chat").push().setValue(hashMap);

        final DatabaseReference reference1 = FirebaseDatabase.getInstance().getReference("ChatList").child(myid).child(friendid);
        reference1.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(!snapshot.exists()) {
                    reference1.child("id").setValue(friendid);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    //h??m g???i h??nh ???nh
    private void sendImageMessage(Uri image_uri) throws IOException {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("??ang g???i ???nh...");
        progressDialog.show();

        String timeStamp = "" + System.currentTimeMillis();
        String fileNameAndPath = "ChatImages/" + "post_" + timeStamp;

        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), image_uri);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] data = baos.toByteArray();
        StorageReference ref = FirebaseStorage.getInstance().getReference().child(fileNameAndPath);
        ref.putBytes(data)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        progressDialog.dismiss();

                        Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                        while (!uriTask.isSuccessful());
                        String downloadUri = uriTask.getResult().toString();

                        if(uriTask.isSuccessful()){
                            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();

                            HashMap<String, Object> hashMap = new HashMap<>();
                            hashMap.put("sender", myid);
                            hashMap.put("receiver", friendid);
                            hashMap.put("message", downloadUri);
                            hashMap.put("type", "image");
                            hashMap.put("isseen", false);

                            databaseReference.child("Chat").push().setValue(hashMap);

                            final DatabaseReference reference1 = FirebaseDatabase.getInstance().getReference("ChatList").child(myid).child(friendid);
                            reference1.addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    if(!snapshot.exists()) {
                                        reference1.child("id").setValue(friendid);

                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                    }
                });
    }

    //h??m set tr???ng th??i online/offline cho t??i kho???n
    private void Status (String status){
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users").child(firebaseUser.getUid());

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("status", status);

        reference.updateChildren(hashMap);
    }

    //set online khi t??i kho???n ???????c ????ng nh???p v?? ??ang s??i app
    @Override
    protected void onResume() {
        super.onResume();
        Status("online");
    }
    //set offline khi tho??t ???ng d???ng, v?? kh??ng hi???n th??? ???? xem khi c?? tin nh???n ?????n
    @Override
    protected void onPause() {
        super.onPause();
        Status("offline");
        reference.removeEventListener(seenlistener);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode){
            case CAMERA_REQUEST_CODE:{
                if(grantResults.length > 0){
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;

                    if(cameraAccepted && storageAccepted){
                        pickFromCamera();

                    }
                    else{
                        Toast.makeText(this, "M??y ???nh v?? b??? nh??? ch??a ???????c cho ph??p!", Toast.LENGTH_SHORT).show();

                    }
                }
                else {

                }

            }
            break;
            case STORAGE_REQUEST_CODE:{
                if(grantResults.length>0){
                    boolean storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if(storageAccepted){
                        pickFromGallery();

                    }
                    else{
                        Toast.makeText(this, "B??? nh??? ch??a ???????c cho ph??p!", Toast.LENGTH_SHORT).show();

                    }
                }
                else {

                }

            }
            break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK){
            if(requestCode == IMAGE_PICK_GALLERY_CODE){
                image_uri = data.getData();

                try {
                    sendImageMessage(image_uri);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
            else if(requestCode == IMAGE_PICK_CAMERA_CODE){
                try {
                    sendImageMessage(image_uri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

}