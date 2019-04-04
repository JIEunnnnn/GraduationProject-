package com.example.owner.project_final;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.example.owner.project_final.firebase.FirebaseApi;
import com.example.owner.project_final.firebase.ImageLoaderHelper;
import com.example.owner.project_final.firebase.OnPhotoResultListener;
import com.example.owner.project_final.firebase.PublicVariable;
import com.example.owner.project_final.location.LocationProvider;
import com.example.owner.project_final.map.MapFragemnt;
import com.example.owner.project_final.model.PreferenceHelper;
import com.example.owner.project_final.model.RoomWrite;
import com.example.owner.project_final.volley.VolleyResult;
import com.example.owner.project_final.volley.VolleyService;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class RoomWriteActivity extends AppCompatActivity {
    //[오투잡] 2019.04.12 글쓰기 부분 진입 부분 버터나이프 기능적용
    //[오투잡] 2019.04.13 글쓰기 부분 데이터 및 구글맵 셋팅 Firebase 서버로 데이터 add 완료
    //조언  public static Activity roomWriteActivity;  방식으로 Static 을 너무 쓰면 리로스를 많이잡아먹습니다 가급적 satic은 자제해주세요.
    Intent intent;

    @BindView(R.id.room_title_date)
    TextView date;

    @BindView(R.id.room_title)
    EditText editTitle;
    @BindView(R.id.room_User)
    EditText editUser;
    @BindView(R.id.editStartDay)
    EditText editStartDay;
    @BindView(R.id.editEndDay)
    EditText editEndDay;

    @BindView(R.id.daily_rental)
    EditText editEDailyRental;

    @BindView(R.id.room_address)
    EditText editRoomAddress;

    @BindView(R.id.detail_address)
    EditText editRoomDetailAddress;

    @BindView(R.id.room_Contents)
    EditText editDiscription;

    @BindView(R.id.option_room_01)
    CheckBox checkBoxRoomOption01;

    @BindView(R.id.option_room_02)
    CheckBox checkBoxRoomOption02;

    @BindView(R.id.option_room_03)
    CheckBox checkBoxRoomOption03;

    @BindView(R.id.option_room_04)
    CheckBox checkBoxRoomOption04;

    @BindView(R.id.option_room_05)
    CheckBox checkBoxRoomOption05;


    @BindView(R.id.option_limit_01)
    CheckBox checkBoxRoomLimitOption01;

    @BindView(R.id.option_limit_02)
    CheckBox checkBoxRoomLimitOption02;

    @BindView(R.id.option_limit_03)
    CheckBox checkBoxRoomLimitOption03;

    @BindView(R.id.option_limit_04)
    CheckBox checkBoxRoomLimitOption04;

    @BindView(R.id.photo_1)
    ImageView roomPhoto1;

    @BindView(R.id.default_photo_1)
    ImageView roomPhotodefalut1;

    @BindView(R.id.photo_2)
    ImageView roomPhoto2;

    @BindView(R.id.default_photo_2)
    ImageView roomPhotodefalut2;

    @BindView(R.id.scroll)
    ScrollView scrollView;

    public final static int REQUEST_PICK_IMAGE = 0x20;

    private String[] mRoomOption = {"", "", "", "", ""};
    private String[] mRoomLimitOption = {"", "", "", ""};
    private String[] photoUri = new String[]{"", ""};

    private String latitude;
    private String longitude;
    private String postingDate;

    public int number = 1;
    private MapFragemnt mMapFragemnt;
    public ProgressDialog mProgressDialog;

    public int mPhotoPosition = -1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        L.e("::::RoomWriteActivity");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_write);
        ButterKnife.bind(this);

        mMapFragemnt = MapFragemnt.getInstance();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.add(R.id.fragment_placeholder, mMapFragemnt);
        transaction.commitAllowingStateLoss();

        mMapFragemnt.setScrollView(scrollView);

        //최신 날짜 셋팅
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd");
        postingDate = simpleDateFormat.format(System.currentTimeMillis());
        date.setText(postingDate);
        editUser.setText(PreferenceHelper.getNickName(getApplicationContext()));

    }


    @OnClick(R.id.saveButton_room)
    public void save(View view) {

        if (!validateForm()) {
            return;
        }

        if (checkBoxRoomOption01.isChecked()) {
            mRoomOption[0] = "냉방/히터";
        }

        if (checkBoxRoomOption02.isChecked()) {
            mRoomOption[1] = "냉장고";
        }

        if (checkBoxRoomOption03.isChecked()) {
            mRoomOption[2] = "세탁기";
        }

        if (checkBoxRoomOption04.isChecked()) {
            mRoomOption[3] = "인터넷";
        }

        if (checkBoxRoomOption05.isChecked()) {
            mRoomOption[4] = "조리대";
        }

        if (checkBoxRoomLimitOption01.isChecked()) {
            mRoomLimitOption[0] = "남자가능";
        }

        if (checkBoxRoomLimitOption02.isChecked()) {
            mRoomLimitOption[1] = "여자가능";
        }

        if (checkBoxRoomLimitOption03.isChecked()) {
            mRoomLimitOption[2] = "흡연불가";
        }

        if (checkBoxRoomLimitOption04.isChecked()) {
            mRoomLimitOption[3] = "애완동물 불가";
        }

        final ArrayList<Uri> storePhotoArray = new ArrayList<>();
        int i = 0;
        while (i < photoUri.length) {
            if (!TextUtils.isEmpty(photoUri[i]) && !photoUri[i].equalsIgnoreCase("")) {
                storePhotoArray.add(Uri.parse(photoUri[i]));
            }
            i++;
        }

        showProgressDialog("업로드 중입니다.");
        String autoKey = FirebaseDatabase.getInstance().getReference().child(PublicVariable.FIREBASE_CHILD_ROOMS).push().getKey();
        final String storageKey = FirebaseDatabase.getInstance().getReference().child(PublicVariable.FIREBASE_CHILD_ROOMS).push().getKey();
        RoomWrite roomWrite = new RoomWrite(FirebaseApi.getCurrentUser().getUid(), editTitle.getText().toString(), editUser.getText().toString(),
                editStartDay.getText().toString(), editEndDay.getText().toString(), editEDailyRental.getText().toString(),
                mRoomOption[0], mRoomOption[1], mRoomOption[2], mRoomOption[3], mRoomOption[4],
                mRoomLimitOption[0], mRoomLimitOption[1], mRoomLimitOption[2], mRoomLimitOption[3],
                editRoomAddress.getText().toString(), editRoomDetailAddress.getText().toString(), storageKey, editDiscription.getText().toString(), latitude, longitude, postingDate, autoKey);

        L.e("::::::auto key : " + autoKey);
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        databaseReference.child(PublicVariable.FIREBASE_CHILD_ROOMS).child(FirebaseApi.getCurrentUser().getUid()).child(autoKey).setValue(roomWrite).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    for (int i = 0; i < storePhotoArray.size(); i++) {
                        FirebaseApi.sendFirebaseStorage(storageKey, storePhotoArray.get(i), i, storePhotoArray.size(), new OnPhotoResultListener() {
                            @Override
                            public void onComplete() {
                                hideProgressDialog();
                                int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.95);
                                int height = WindowManager.LayoutParams.WRAP_CONTENT;
                                new AlertDialog.Builder(RoomWriteActivity.this, R.style.Theme_AppCompat_Light_Dialog)
                                        .setTitle("알림")
                                        .setMessage("업로드가 완료 되었습니다. ")
                                        .setCancelable(false)
                                        .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                finish();
                                            }
                                        }).setOnDismissListener(new DialogInterface.OnDismissListener() {
                                    @Override
                                    public void onDismiss(DialogInterface dialog) {

                                    }
                                }).show().getWindow().setLayout(width, height);
                            }

                            @Override
                            public void onFailure(Exception e) {

                            }
                        });
                    }
                }
            }
        });
    }

    @OnClick(R.id.cancelButton_room)
    public void cancle(View view) {

    }


    @OnClick(R.id.request_gps)
    public void requestAddress(View view) {
        if (!gpsEnabled(getApplicationContext())) {
            showSnakbar("GPS 장치를 활성화 해주세요.\n활성화 후 다시 시도해주세요.", 1500);
            return;
        }
        Dexter.withActivity(RoomWriteActivity.this).withPermissions(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION).withListener(new MultiplePermissionsListener() {
            @Override
            public void onPermissionsChecked(MultiplePermissionsReport report) {
                if (report.areAllPermissionsGranted()) {
                    showProgressDialog("위치정보를 요청중입니다.. 잠시만 기다려주세요.");
                    new LocationProvider().getLocation(getApplicationContext(), new LocationProvider.LocationResultCallback() {
                        @Override
                        public void gotLocation(final Location location) {
                            if (location != null) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        hideProgressDialog();

                                        latitude = String.valueOf(location.getLatitude());
                                        longitude = String.valueOf(location.getLongitude());

                                        VolleyService volleyService = new VolleyService(new VolleyResult() {
                                            @Override
                                            public void notifySuccess(String type, JSONObject response) {
                                                try {
                                                    JSONArray ja = response.getJSONArray("results");
                                                    String address = "";
                                                    for (int i = 0; i < ja.length(); i++) {
                                                        JSONObject c = ja.getJSONObject(i);
                                                        address = c.getString("formatted_address");
                                                        L.e("::::address : " + address);
                                                        break;
                                                    }
                                                    editRoomAddress.setText(address);
                                                    mMapFragemnt.onMapUpdate();
                                                } catch (Exception e) {

                                                }
                                            }

                                            @Override
                                            public void notifyError(VolleyError error) {
                                                L.e(":::Server Internal Error  : " + error.getMessage());
                                            }
                                        }, getApplicationContext());
                                        volleyService.getGeocoder(location);
                                    }
                                });

                            } else {
                                //위치정보 Time Out.
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        showSnakbar("연결상태가 일시적으로 불안정합니다\n다시 시도해주세요", 1500);
                                        hideProgressDialog();
                                    }
                                });

                            }

                        }
                    });
                }
            }

            @Override
            public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                token.continuePermissionRequest();
            }
        }).check();
    }

    @OnClick(R.id.editStartDay)
    public void setStartDay(View view) {
        L.e("::::setStartDay");
        if (!isFinishing()) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);
            DatePickerDialog dialog = new DatePickerDialog(RoomWriteActivity.this, new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                    editStartDay.setText(getResultDay(year, month, dayOfMonth));
                }
            }, year, month, day);

            dialog.show();
        }


    }


    @OnClick(R.id.editEndDay)
    public void setEndDay(View view) {
        L.e("::::setEndDay");
        if (!isFinishing()) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);
            DatePickerDialog dialog = new DatePickerDialog(RoomWriteActivity.this, new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                    editEndDay.setText(getResultDay(year, month, dayOfMonth));
                }
            }, year, month, day);

            dialog.show();
        }
    }

    public String getResultDay(int year, int month, int dayOfMonth) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd");
        return simpleDateFormat.format(calendar.getTimeInMillis());

    }


    public void showProgressDialog(String title) {
        L.e(":::title : " + title);
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setMessage(title);
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setCanceledOnTouchOutside(false);
        }

        mProgressDialog.show();
    }


    public void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }

    public void showSnakbar(String text, int priod) {

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
            Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), text, priod);
            snackbar.show();
        } else {
            Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();
        }
    }

    public boolean gpsEnabled(Context context) {
        final LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }


    private boolean validateForm() {
        boolean result = true;
        if (TextUtils.isEmpty(editTitle.getText().toString())) {
            editTitle.setError("Required");
            result = false;
        } else {
            editTitle.setError(null);
        }

        if (TextUtils.isEmpty(editUser.getText().toString())) {
            editUser.setError("Required");
            result = false;
        } else {
            editUser.setError(null);
        }

        if (TextUtils.isEmpty(editStartDay.getText().toString())) {
            editStartDay.setError("Required");
            result = false;
        } else {
            editStartDay.setError(null);
        }

        if (TextUtils.isEmpty(editEndDay.getText().toString())) {
            editEndDay.setError("Required");
            result = false;
        } else {
            editEndDay.setError(null);
        }


        if (TextUtils.isEmpty(editRoomAddress.getText().toString())) {
            editRoomAddress.setError("Required");
            result = false;
        } else {
            editRoomAddress.setError(null);
        }

        if (TextUtils.isEmpty(editRoomDetailAddress.getText().toString())) {
            editRoomDetailAddress.setError("Required");
            result = false;
        } else {
            editRoomDetailAddress.setError(null);
        }

        if (TextUtils.isEmpty(editEDailyRental.getText().toString())) {
            editEDailyRental.setError("Required");
            result = false;
        } else {
            editEDailyRental.setError(null);
        }

        if (TextUtils.isEmpty(editDiscription.getText().toString())) {
            editDiscription.setError("Required");
            result = false;
        } else {
            editDiscription.setError(null);
        }


        return result;
    }


    @OnClick(R.id.photo_view_1)
    public void setPhotoClick_01(View view) {
        Dexter.withActivity(RoomWriteActivity.this).withPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE).withListener(new MultiplePermissionsListener() {
            @Override
            public void onPermissionsChecked(MultiplePermissionsReport report) {
                if (report.areAllPermissionsGranted()) {
                    mPhotoPosition = 0;
                    Intent intent = new Intent(Intent.ACTION_PICK);
                    intent.setData(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(intent, REQUEST_PICK_IMAGE);
                }
            }

            @Override
            public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                token.continuePermissionRequest();
            }
        }).check();
    }

    @OnClick(R.id.photo_view_2)
    public void setPhotoClick_02(View view) {
        Dexter.withActivity(RoomWriteActivity.this).withPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE).withListener(new MultiplePermissionsListener() {
            @Override
            public void onPermissionsChecked(MultiplePermissionsReport report) {
                if (report.areAllPermissionsGranted()) {
                    mPhotoPosition = 1;
                    Intent intent = new Intent(Intent.ACTION_PICK);
                    intent.setData(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(intent, REQUEST_PICK_IMAGE);
                }
            }

            @Override
            public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                token.continuePermissionRequest();
            }
        }).check();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_PICK_IMAGE:
                //[오투잡]이미지를 갤러리에서 Callback Url 을 콜백하는 부분;
                L.i(":::::: OPEN_IMAGE_REQUEST_CODE");
                try {
                    Uri uri;

                    if (data == null) {
                        return;
                    }
                    uri = data.getData();

                    L.e("::::uri : " + uri);
                    photoUri[mPhotoPosition] = uri.toString();

                    if (mPhotoPosition == 0) {
                        roomPhoto1.setVisibility(View.VISIBLE);
                        roomPhotodefalut1.setVisibility(View.GONE);
                        ImageLoaderHelper.setProfileImage(getApplicationContext(), uri, roomPhoto1, "");
                    } else {
                        roomPhoto2.setVisibility(View.VISIBLE);
                        roomPhotodefalut2.setVisibility(View.GONE);
                        ImageLoaderHelper.setProfileImage(getApplicationContext(), uri, roomPhoto2, "");
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }
    }
}
