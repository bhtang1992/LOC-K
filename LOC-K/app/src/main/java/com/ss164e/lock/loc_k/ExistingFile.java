package com.ss164e.lock.loc_k;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.InputStreamReader;

public class ExistingFile extends AppCompatActivity {

    String internalKey;
    static final int READ_BLOCK_SIZE = 100;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_option_menu, menu);
        return true;
    }

    /*@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.changePassword:

                break;
            case R.id.deleteFile:

                break;
            default:
                break;
        }

        return true;
    }*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_existing_file);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        String password = extras.getString("password");
        String hashedPw = extras.getString("hashedPw");
        String encryptedLoc = extras.getString("encryptedLoc");
        String cipherText = extras.getString("cipherText");

        String locText = "";
        String plainText = "";

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

            String locSalt = SHA1.hash(hashedPw+password);
            Encryption locationE = Encryption.getDefault(internalKey, locSalt, new byte[16]);
            locText = locationE.decrypt(encryptedLoc);

            String textSalt = SHA1.hash(password+locText);
            Encryption textE = Encryption.getDefault(internalKey, textSalt, new byte[16]);
            plainText = textE.decrypt(cipherText);

            TextView messageView = (TextView)findViewById(R.id.showText);
            messageView.setText(plainText);

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