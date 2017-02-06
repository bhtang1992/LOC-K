package com.ss164e.lock.loc_k;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.InputStreamReader;

public class ExistingFile extends AppCompatActivity {

    String internalKey;
    static final int READ_BLOCK_SIZE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_existing_file);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        String hashedPw = extras.getString("hashedPw");
        String cText = extras.getString("cText");

        String pText = "";

        try {
            // Get secret key
            FileInputStream fileIn = openFileInput("secretKey.txt");
            InputStreamReader InputRead = new InputStreamReader(fileIn);

            char[] inputBuffer = new char[READ_BLOCK_SIZE];
            int charRead;

            while ((charRead = InputRead.read(inputBuffer)) > 0) {
                // char to string conversion
                String readstring = String.copyValueOf(inputBuffer, 0, charRead);
                internalKey += readstring;
            }
            InputRead.close();

            Encryption e = Encryption.getDefault(internalKey, hashedPw, new byte[16]);
            pText = e.decrypt(cText);

            TextView messageView = (TextView)findViewById(R.id.showText);
            messageView.setText(pText);

        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, ListFile.class);
        startActivity(intent);
        finish();
    }

}