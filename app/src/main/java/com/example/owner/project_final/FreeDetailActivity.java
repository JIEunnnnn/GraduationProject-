package com.example.owner.project_final;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SnapHelper;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.owner.project_final.adapter.CommentAdapter;
import com.example.owner.project_final.adapter.StartSnapHelper;
import com.example.owner.project_final.firebase.FirebaseApi;
import com.example.owner.project_final.firebase.ImageLoaderHelper;
import com.example.owner.project_final.firebase.MessageUtils;
import com.example.owner.project_final.firebase.PublicVariable;
import com.example.owner.project_final.map.MapFragemnt;
import com.example.owner.project_final.model.Comment;
import com.example.owner.project_final.model.PreferenceHelper;
import com.example.owner.project_final.model.Reply;
import com.example.owner.project_final.model.FreeWrite;
import com.example.owner.project_final.model.UserModel;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class FreeDetailActivity extends AppCompatActivity {
    //[오투잡] 2019.04.13 글쓰기 상세부분 구현

    @BindView(R.id.title_date)
    TextView date;

    @BindView(R.id.title)
    TextView tvTitle;

    @BindView(R.id.User)
    TextView tvUser;

    @BindView(R.id.Contents)
    TextView tvDiscription;
    /* 사진 삽입
        @BindView(R.id.photo_1)
        ImageView ivPhoto1;

        @BindView(R.id.photo_2)
        ImageView ivPhoto2;
    */
    @BindView(R.id.unknown_name)
    CheckBox checkBoxunknownName;

    @BindView(R.id.scroll)
    ScrollView scrollView;

    @BindView(R.id.field_comment_text)
    EditText editComment;

    @BindView(R.id.recycler_comments)
    RecyclerView recyclerView;

    private AlertDialog dialog;
    FreeWrite mFreeWrite;
    CommentAdapter mCommentAdapter;
    /*사진 삽입
        String[] photoUri = {"", ""};
    */
    Intent intent;

    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

    // For Toolbar ---------------------------------------------------------------------------------
    Toolbar toolBar;
    //----------------------------------------------------------------------------------------------

    // For Navigation Drawer -----------------------------------------------------------------------
    NavigationView navigationView;
    DrawerLayout drawerLayout;
    // ---------------------------------------------------------------------------------------------

    String id;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_free_detail);
        ButterKnife.bind(this);

        Intent intent_detail = getIntent();
        Bundle bundle = new Bundle();
        if (intent_detail != null) {
            mFreeWrite = (FreeWrite) intent_detail.getSerializableExtra("FreeWrite");
            L.e("::::::시리얼 라이저블 : " + mFreeWrite.toString());

            id = mFreeWrite.getId();    //키 값 가져오기

            tvTitle.setText(mFreeWrite.getTitle());

            tvUser.setText(mFreeWrite.getWriter());
            date.setText(mFreeWrite.getPostingDate());
            tvDiscription.setText(mFreeWrite.getDescription());


            mCommentAdapter = new CommentAdapter(getApplicationContext()) {
                @Override
                public void selectItem(final Comment item, final int pos) {
                    L.e("::::롱클릭 설정 : " + item.toString());
                    L.e("::::롱클릭 pos : " + pos);
                    if (dialog != null && dialog.isShowing()) {
                        dialog.dismiss();
                    }
                    AlertDialog.Builder builder = new AlertDialog.Builder(FreeDetailActivity.this/*, R.style.Theme_AppCompat_Light_Dialog*/);
                    builder.setTitle("답글 달기");
                    LayoutInflater inflater = LayoutInflater.from(getApplicationContext());
                    View view = inflater.inflate(R.layout.dialog_reply, null);
                    final EditText editContent = view.findViewById(R.id.reply_content);
                    builder.setView(view);
                    builder.setCancelable(false);
                    builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int position) {

                            if (editContent.getText().toString().equalsIgnoreCase("") || TextUtils.isEmpty(editContent.getText().toString())) {
                                MessageUtils.showLongToastMsg(getApplicationContext(), "댓글 내용을 입력해주세요.");
                                return;
                            }

                            DatabaseReference req = FirebaseDatabase.getInstance().getReference().child(PublicVariable.FIREBASE_CHILD_FREES_COMMENT).child(item.getNodeId()).child(item.getChildId());
                            req.runTransaction(new Transaction.Handler() {
                                @NonNull
                                @Override
                                public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                                    Comment comment = mutableData.getValue(Comment.class);
                                    if (comment == null) {
                                        return Transaction.success(mutableData);
                                    }

                                    Map<String, Reply> replyHashMap = comment.getReplyMap();
                                    String key = FirebaseDatabase.getInstance().getReference().child(PublicVariable.FIREBASE_CHILD_FREES_COMMENT).push().getKey();
                                    replyHashMap.put(key, new Reply(FirebaseApi.getCurrentUser().getUid(), false, PreferenceHelper.getNickName(getApplicationContext()), new SimpleDateFormat("yyyy/MM/dd").format(System.currentTimeMillis())
                                            , editContent.getText().toString(), key));
                                    comment.setReplyMap(replyHashMap);
                                    mutableData.setValue(comment);
                                    return Transaction.success(mutableData);
                                }

                                @Override
                                public void onComplete(@Nullable DatabaseError databaseError, boolean complte, @Nullable DataSnapshot dataSnapshot) {
                                    if (databaseError == null && complte && dataSnapshot != null) {
                                        mCommentAdapter.clear();
                                        onLoadComment();
                                    }

                                }
                            });
                        }
                    });
                    builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
                    dialog = builder.create();
                    dialog.show();
                }
            };
            recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
            recyclerView.setAdapter(mCommentAdapter);
            SnapHelper snapHelperStart = new StartSnapHelper();
            snapHelperStart.attachToRecyclerView(recyclerView);
            onLoadComment();

        }

        // For Toolbar -----------------------------------------------------------------------------
        toolBar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolBar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("자유 게시판 글보기");
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.baseline_menu_black_24dp);
        // -----------------------------------------------------------------------------------------

        // For Navigation Drawer -------------------------------------------------------------------
        drawerLayout = (DrawerLayout)findViewById(R.id.activity);  //각 레이아웃의 가장 큰 DrawerLayout 이름
        navigationView = (NavigationView)findViewById(R.id.navigationView);

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                item.setChecked(true);
                drawerLayout.closeDrawers();

                int id = item. getItemId();

                switch (id) {
                    case R.id.navi_tab1:    //오늘 하루
                        //Toast.makeText(getApplicationContext(), item.getTitle(), Toast.LENGTH_LONG).show();
                        intent = new Intent().setClass( getApplicationContext(),Tab1Activity.class );
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        break;
                    case R.id.navi_tab2:    //위치 서비스
                        //Toast.makeText(getApplicationContext(), item.getTitle(), Toast.LENGTH_LONG).show();
                        intent = new Intent().setClass( getApplicationContext(),Tab2Activity.class );
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        break;
                    case R.id.navi_tab3:    //게시판
                        //Toast.makeText(getApplicationContext(), item.getTitle(), Toast.LENGTH_LONG).show();
                        intent = new Intent().setClass( getApplicationContext(),Tab3Activity.class );
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        break;
                    case R.id.navi_tab3_1:    //공동구매 게시판
                        Toast.makeText(getApplicationContext(), item.getTitle(), Toast.LENGTH_LONG).show();
                        intent = new Intent().setClass( getApplicationContext(), PurchaseActivity.class );
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        break;
                    case R.id.navi_tab3_2:    //단기방대여 게시판
                        //Toast.makeText(getApplicationContext(), item.getTitle(), Toast.LENGTH_LONG).show();
                        intent = new Intent().setClass( getApplicationContext(), RoomActivity.class );
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        break;
                    case R.id.navi_tab3_3:    //음식주문 게시판
                        ////Toast.makeText(getApplicationContext(), item.getTitle(), Toast.LENGTH_LONG).show();
                        intent = new Intent().setClass( getApplicationContext(), FoodActivity.class );
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        break;
                    case R.id.navi_tab3_4:    //취미여가 게시판
                        //Toast.makeText(getApplicationContext(), item.getTitle(), Toast.LENGTH_LONG).show();
                        intent = new Intent().setClass( getApplicationContext(), HobbyActivity.class );
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        break;
                    case R.id.navi_tab3_5:    //자유게시판
                        //Toast.makeText(getApplicationContext(), item.getTitle(), Toast.LENGTH_LONG).show();
                        intent = new Intent().setClass( getApplicationContext(), FreeActivity.class );
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        break;
                    case R.id.navi_tab4:    //무드등
                        //Toast.makeText(getApplicationContext(), item.getTitle(), Toast.LENGTH_LONG).show();
                        intent = new Intent().setClass( getApplicationContext(), BluetoothLED.class );
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        break;
                    case R.id.navi_tab5:    //미니게임
                        //Toast.makeText(getApplicationContext(), item.getTitle(), Toast.LENGTH_LONG).show();
                        intent = new Intent().setClass( getApplicationContext(), GameActivity.class );
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        break;
                    case R.id.navi_tab6:    //마이페이지
                        //Toast.makeText(getApplicationContext(), item.getTitle(), Toast.LENGTH_LONG).show();
                        intent = new Intent().setClass( getApplicationContext(), MypageActivity.class );
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        break;
                }

                return true;
            }
        });
        // -----------------------------------------------------------------------------------------
    }

    public void onLoadComment() {

        DatabaseReference req = FirebaseDatabase.getInstance().getReference().child(PublicVariable.FIREBASE_CHILD_FREES_COMMENT).child(mFreeWrite.getId());
        req.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot child : dataSnapshot.getChildren()) {
                        Comment item = child.getValue(Comment.class);
                        mCommentAdapter.insertData(item);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
/* 사진 삽입
        setVaildFirebaseStorage(mFreeWrite.getAddedByUser(), mFreeWrite.getPhotoID());
*/
    }

    @OnClick(R.id.button_post_comment)
    public void postComment(View view) {

        if (TextUtils.isEmpty(editComment.getText()) || editComment.getText().toString().equalsIgnoreCase("")) {
            MessageUtils.showLongToastMsg(getApplicationContext(), "내용을 입력해주세요.");
            return;
        }

        FirebaseDatabase.getInstance().getReference().child(PublicVariable.FIREBASE_CHILD_USERS).child(FirebaseApi.getCurrentUser().getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                UserModel userModel = dataSnapshot.getValue(UserModel.class);
                if (userModel != null) {
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd");
                    DatabaseReference req = FirebaseDatabase.getInstance().getReference().child(PublicVariable.FIREBASE_CHILD_FREES_COMMENT).child(mFreeWrite.getId());
                    String id = req.push().getKey();
                    Comment comment = new Comment(FirebaseApi.getCurrentUser().getUid(), checkBoxunknownName.isChecked(), userModel.getName(), simpleDateFormat.format(System.currentTimeMillis()), editComment.getText().toString(), mFreeWrite.getId(), id, false);
                    req.child(id).setValue(comment);
                    mCommentAdapter.insertData(comment);
                    editComment.setText("");
                    hideKeyboard();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    /* 사진 삽입
        @OnClick(R.id.photo_view_1)
        public void setPhotoClick_01(View view) {
            if (photoUri[0].equalsIgnoreCase("")) {
                return;
            }

            Intent intent = new Intent(this, PhotoZoomActivity.class);
            intent.putExtra("photo", photoUri[0]);
            startActivity(intent);


        }

        @OnClick(R.id.photo_view_2)
        public void setPhotoClick_02(View view) {
            if (photoUri[1].equalsIgnoreCase("")) {
                return;
            }
            Intent intent = new Intent(this, PhotoZoomActivity.class);
            intent.putExtra("photo", photoUri[1]);
            startActivity(intent);
        }
    */
    private void hideKeyboard() {
        //hide keyboard
        View currentFocus = getCurrentFocus();
        if (currentFocus != null) {
            String serviceName = Context.INPUT_METHOD_SERVICE;
            InputMethodManager imm = (InputMethodManager) getSystemService(serviceName);
            int stateHide = InputMethodManager.HIDE_NOT_ALWAYS;
            imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), stateHide);
        }
    }
    /* 사진 삽입
        private void setVaildFirebaseStorage(String key, String storageKey) {
            FirebaseStorage storage = FirebaseStorage.getInstance();
            if (storageKey.equalsIgnoreCase("")) {
                return;
            }

            for (int index = 0; index < 2; index++) {
                StorageReference storageRef = storage.getReferenceFromUrl(PublicVariable.FIREBASE_STORAGE).child(PublicVariable.FIREBASE_STORAGE_FREES).child(key).child("freeImage" + (index + 1) + ".jpg").child(storageKey);
                final int success = index;
                storageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        if (success == 0) {
                            photoUri[0] = uri.toString();
                        } else {
                            photoUri[1] = uri.toString();
                        }
                        ImageLoaderHelper.setProfileImage(getApplicationContext(), uri, success == 0 ? ivPhoto1 : ivPhoto2, "");
                    }
                });
            }
        }
    */
    // For Toolbar ---------------------------------------------------------------------------------
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //return super.onCreateOptionsMenu(menu);
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu, menu);    //게시판 목록 외에서 사용할 툴바 메뉴
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //return super.onOptionsItemSelected(item);

        drawerLayout = (DrawerLayout) findViewById(R.id.activity);

        switch (item.getItemId()) {
            case R.id.MainButton:
                intent = new Intent().setClass( getApplicationContext(), MainActivity.class );  //MainActivity로 이동
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
            case R.id.ChatButton:
                intent = new Intent().setClass( getApplicationContext(), ChattingActivity.class );  //ChattingActivity로 이동
                startActivity(intent);
                return true;
            case android.R.id.home:
                drawerLayout.openDrawer(GravityCompat.START);
                return true;
            case R.id.MyPageButton:
                intent = new Intent().setClass( getApplicationContext(), MypageActivity.class );    //MyㅔageActivity로 이동
                startActivity(intent);
                return true;
/*
            case R.id.action_modify:
                intent = new Intent().setClass( getApplicationContext(), MypageActivity.class );    //글 수정으로 이동
                startActivity(intent);
                return true;
*/
            case R.id.action_erase:
                FirebaseDatabase.getInstance().getReference().child(PublicVariable.FIREBASE_CHILD_FREES).child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child(id).removeValue();
                intent = new Intent().setClass( getApplicationContext(), FreeActivity.class );    //글 목록으로 이동
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
            case R.id.LogOutButton:
                if(user != null){
                    FirebaseAuth.getInstance().signOut();
                    intent = new Intent().setClass( getApplicationContext(), LoginActivity.class ); //로그아웃 후 LoginActivity로 이동
                    startActivity(intent);
                    Toast.makeText(getApplicationContext(), "로그아웃 성공", Toast.LENGTH_LONG).show();
                    overridePendingTransition(0, 0);
                }else{
                    Toast.makeText(getApplicationContext(), "로그아웃 실패", Toast.LENGTH_LONG).show();
                }
                return true;
            default:
                Toast.makeText(getApplicationContext(), "나머지 버튼 클릭됨", Toast.LENGTH_LONG).show();
                return super.onOptionsItemSelected(item);
        }
    }
    //----------------------------------------------------------------------------------------------
}