package com.ss164e.lock.loc_k;

import android.app.Dialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;

public class NewFile extends AppCompatActivity {

    //Define a request code to send to Google Play services
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private double currentLatitude;
    private double currentLongitude;
    private double radius;
    private Button button;
    private String locText, outsiderText;
    EditText ed;
    String fileName;
    final Context context = this;
    String internalKey;
    static final int READ_BLOCK_SIZE = 100;
    String path = "";
    String password;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_file);

        ed = (EditText) findViewById(R.id.contentText);

        button = (Button) findViewById(R.id.saveButton);
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        currentLatitude = extras.getDouble("latitude");
        currentLongitude = extras.getDouble("longitude");
        radius = extras.getDouble("radius");
        outsiderText = extras.getString("outsiderText");
        path = extras.getString("path");
        ed.setText(outsiderText, TextView.BufferType.EDITABLE);

        button.setOnClickListener(new View.OnClickListener() {


            @Override
            public void onClick(View arg0) {
                locText = currentLongitude + "," + currentLatitude + "," + radius;

                final Dialog dialog = new Dialog(context);
                dialog.setContentView(R.layout.activity_save_dialog);
                dialog.setTitle("Encrypt");

                Button dialogButton = (Button) dialog.findViewById(R.id.dialogButtonSave);
                final EditText filenameText = (EditText) dialog.findViewById(R.id.filenameTextbox);
                final EditText passwordText = (EditText) dialog.findViewById(R.id.passwordTextbox);
                dialog.show();

                // if button is clicked, close the custom dialog
                dialogButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        try {
                            // Get secret key
                            internalKey = "";
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
                            fileName = filenameText.getText().toString();
                            password = passwordText.getText().toString();
                            if (!fileName.isEmpty() && !password.isEmpty()) {
                                saveFile();
                                dialog.dismiss();
                                Intent intent = new Intent(NewFile.this, ListFile.class);
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(getBaseContext(), "Filename and Password required!", Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }

    public void saveFile(){
        try{
            String hashedPw = SHA1.hash(password);
            String message = ed.getText().toString();
            String locSalt = SHA1.hash(hashedPw + password);
            String textSalt = SHA1.hash(password + locText);

            Encryption locationE = Encryption.getDefault(internalKey, locSalt, new byte[16]);
            String encryptedLoc = locationE.encrypt(locText);

            Encryption textE = Encryption.getDefault(internalKey, textSalt, new byte[16]);
            String encryptedText = textE.encrypt(message);

            // this will create a new name everytime and unique
            ContextWrapper contextWrapper = new ContextWrapper(getApplicationContext());
            File root = contextWrapper.getDir("LOC-K", Context.MODE_PRIVATE);
            File filepath = new File(root, fileName + ".txt");  // file path to save
            FileWriter writer = new FileWriter(filepath);
            writer.append(hashedPw);
            writer.append(":");
            writer.append(encryptedLoc);
            writer.append(":");
            writer.append(encryptedText);
            writer.flush();
            writer.close();
            if (!path.isEmpty()) {
                File file = new File(path);
                file.delete();
            }
        }catch (Exception e) {
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