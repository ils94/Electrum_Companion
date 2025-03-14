package com.droidev.electrumcompanion;

import static com.droidev.electrumcompanion.PSBTChecker.checkPSBT;
import static com.droidev.electrumcompanion.PSBTChecker.isPSBT;
import static com.droidev.electrumcompanion.PSBTChecker.isSignedPSBT;
import static com.droidev.electrumcompanion.PSBTChecker.isSignedTransaction;

import android.bluetooth.BluetoothAdapter;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class MainActivity extends AppCompatActivity {

    private String sharedText;
    private Uri savedFileUri;
    private static final int CREATE_FILE_REQUEST_CODE = 1;
    private TextView textView;
    private String fileContent = "";
    private BluetoothAdapter bluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        textView = findViewById(R.id.textView);

        Intent intent = getIntent();

        String action = intent.getAction();

        Uri uri = intent.getData();

        if (uri != null) {
            String fileType = getContentResolver().getType(uri);

            if (fileType != null) {
                readTextFile(uri);
            }

        } else if (Intent.ACTION_SEND.equals(action) && intent.hasExtra(Intent.EXTRA_TEXT)) {
            handleSendIntent(intent);
        }
    }

    private void readTextFile(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            StringBuilder stringBuilder = new StringBuilder();

            String line;

            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }

            reader.close();

            fileContent = removeHtmlTags(stringBuilder.toString());

            if (!checkPSBT(fileContent)) {
                Toast.makeText(this, "This is not a valid PSBT or SBT.", Toast.LENGTH_SHORT).show();
                return;
            }

            textView.setText(fileContent);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to open file.", Toast.LENGTH_SHORT).show();
        }
    }

    private String removeHtmlTags(String input) {
        Document doc = Jsoup.parse(input);
        return doc.text();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_copy) {
            if (!textView.getText().toString().isEmpty()) {
                copyToClipboard();
            } else {
                Toast.makeText(this, "There is nothing to copy.", Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void copyToClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Text File Content", fileContent);
        clipboard.setPrimaryClip(clip);

        Toast.makeText(this, "Content copied!", Toast.LENGTH_SHORT).show();

        openElectrumApp();
    }

    private void openElectrumApp() {
        try {
            Intent launchIntent = new Intent();
            launchIntent.setClassName("org.electrum.electrum", "org.kivy.android.PythonActivity");

            Toast.makeText(this, "Opening Electrum Bitcoin Wallet...", Toast.LENGTH_SHORT).show();

            startActivity(launchIntent);

        } catch (Exception e) {
            Toast.makeText(this, "Failed to open Electrum: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void createFile() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");

        String timeStamp = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.getDefault()).format(new Date());
        String fileName;

        if (isPSBT(sharedText) || isSignedPSBT(sharedText)) {
            fileName = "Electrum-PSBT_" + timeStamp + ".txt";
        } else if (isSignedTransaction(sharedText)) {
            fileName = "Electrum-SBT_" + timeStamp + ".txt";
        } else {
            fileName = "Electrum-Unknown_" + timeStamp + ".txt";
        }

        intent.putExtra(Intent.EXTRA_TITLE, fileName);

        startActivityForResult(intent, CREATE_FILE_REQUEST_CODE);
    }

    private void saveTextToFile() {
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(getContentResolver().openOutputStream(savedFileUri)));

            writer.write(sharedText);
            writer.close();

            Toast.makeText(this, "File saved successfully!", Toast.LENGTH_SHORT).show();

            checkBluetoothAndSendFile();

        } catch (Exception e) {
            Toast.makeText(this, "Error saving file.", Toast.LENGTH_SHORT).show();
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    private void checkBluetoothAndSendFile() {
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
            showSendFileDialog();
        } else {
            MainActivity.this.finish();
        }
    }

    private void showSendFileDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Send via Bluetooth")
                .setCancelable(false)
                .setMessage("Bluetooth is enabled. Do you want to send the file?")
                .setPositiveButton("Yes", (dialog, which) -> sendFileViaBluetooth())
                .setNegativeButton("No", (dialog, which) -> MainActivity.this.finish())
                .show();
    }

    private void sendFileViaBluetooth() {
        if (savedFileUri == null) {
            Toast.makeText(this, "File URI is null.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_STREAM, savedFileUri);
        intent.setPackage("com.android.bluetooth");

        startActivity(Intent.createChooser(intent, "Send file via Bluetooth"));

        Toast.makeText(this, "Sending file via Bluetooth...", Toast.LENGTH_SHORT).show();

        MainActivity.this.finish();
    }

    private void handleSendIntent(Intent intent) {
        sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);

        if (sharedText != null) {
            if (!checkPSBT(sharedText)) {
                Toast.makeText(this, "This is not a valid PSBT or SBT.", Toast.LENGTH_SHORT).show();
                return;
            }

            createFile();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CREATE_FILE_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            savedFileUri = data.getData();
            saveTextToFile();
        }
    }
}