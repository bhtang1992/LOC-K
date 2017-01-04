package com.ss164e.lock.loc_k;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

public class ExistingFile extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_existing_file);

        Intent intent = getIntent();
        String messageText = intent.getStringExtra(ListFile.EXTRA_MESSAGE);
        TextView messageView = (TextView)findViewById(R.id.showText);
        messageView.setText(messageText);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, ListFile.class);
        startActivity(intent);
        finish();
    }

}