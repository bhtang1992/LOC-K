package com.ss164e.lock.loc_k;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;

public class ExistingFile extends AppCompatActivity {

    String internalKey;
    static final int READ_BLOCK_SIZE = 100;
    final Context context = this;

    EditText editText;
    String filename = "";
    String password = "";
    String hashedPw = "";
    String encryptedLoc = "";
    String cipherText = "";
    String locText = "";
    String plainText = "";

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_option_menu, menu);
        return true;
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_existing_file);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        filename = extras.getString("filename");
        password = extras.getString("password");
        hashedPw = extras.getString("hashedPw");
        encryptedLoc = extras.getString("encryptedLoc");
        cipherText = extras.getString("cipherText");


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

            //TextView messageView = (TextView)findViewById(R.id.showText);
            editText = (EditText)findViewById(R.id.showText);
            editText.setText(plainText);

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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.changePassword:
                changePassword();
                break;
            case R.id.changeAreaLoc:

                break;
            case R.id.saveChanges:
                saveChanges();
                break;
            default:
                break;
        }

        return true;
    }

    public void saveChanges() {
        try {
            editText = (EditText)findViewById(R.id.showText);
            String message = editText.getText().toString();
            String textSalt = SHA1.hash(password + locText);

            Encryption textE = Encryption.getDefault(internalKey, textSalt, new byte[16]);
            String encryptedText = textE.encrypt(message);

            File root = new File(Environment.getExternalStorageDirectory(), "LOC-K");

            File filepath = new File(root, filename);  // file path to save
            FileWriter writer = new FileWriter(filepath);
            writer.append(hashedPw + "\n");
            writer.append(encryptedLoc + "\n");
            writer.append(encryptedText);
            writer.flush();
            writer.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        Intent intent = new Intent(ExistingFile.this, ListFile.class);
        startActivity(intent);
        finish();
    }

    public void changePassword() {
        final Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.activity_password_dialog);
        dialog.setTitle(filename);

        Button dialogButton = (Button) dialog.findViewById(R.id.dialogButtonEnter);
        final EditText passwordText = (EditText) dialog.findViewById(R.id.passwordTextbox);

        dialog.show();

        dialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    String newPassword = passwordText.getText().toString();
                    String newHashedPw = SHA1.hash(newPassword);
                    String locSalt = SHA1.hash(newHashedPw+newPassword);
                    String textSalt = SHA1.hash(newPassword+locText);

                    Encryption locationE = Encryption.getDefault(internalKey, locSalt, new byte[16]);
                    String encryptedLoc = locationE.encrypt(locText);

                    Encryption textE = Encryption.getDefault(internalKey, textSalt, new byte[16]);
                    String encryptedText = textE.encrypt(plainText);

                    File root = new File(Environment.getExternalStorageDirectory(), "LOC-K");

                    File filepath = new File(root, filename);  // file path to save
                    FileWriter writer = new FileWriter(filepath);
                    writer.append(newHashedPw + "\n");
                    writer.append(encryptedLoc + "\n");
                    writer.append(encryptedText);
                    writer.flush();
                    writer.close();

                    Intent intent = new Intent(ExistingFile.this, ListFile.class);
                    startActivity(intent);
                    finish();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

    }


}