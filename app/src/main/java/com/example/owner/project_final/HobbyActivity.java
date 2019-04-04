package com.example.owner.project_final;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class HobbyActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hobby);

        Button preButton = (Button)findViewById(R.id.hButton);
        preButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){

                //이전 페이지로 화면전환
                Intent intent = new Intent (HobbyActivity.this, Tab3Activity.class);

                startActivity(intent);
            }

        });

    }
}
