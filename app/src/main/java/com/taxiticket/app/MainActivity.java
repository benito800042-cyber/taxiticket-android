package com.taxiticket.app;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Local-only WebView host. Photos are handed to the HTML input and never uploaded. */
public class MainActivity extends Activity {
    private static final int FILE_REQUEST = 41;
    private static final int CAMERA_PERMISSION = 42;
    private WebView webView;
    private ValueCallback<Uri[]> fileCallback;
    private Uri cameraUri;
    private boolean captureRequested;
    private TextRecognizer recognizer;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                    FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                captureRequested = params.isCaptureEnabled();
                if (captureRequested && android.os.Build.VERSION.SDK_INT >= 23
                        && checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION);
                } else {
                    launchFileSource();
                }
                return true;
            }
        });
        setContentView(webView);
        webView.loadUrl("file:///android_asset/index.html");
    }

    private void launchFileSource() {
        if (captureRequested) {
            Intent camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (camera.resolveActivity(getPackageManager()) != null) {
                try {
                    File dir = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "tickets");
                    if (!dir.exists() && !dir.mkdirs()) throw new IOException("directory");
                    File image = File.createTempFile("ticket_" +
                            new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()), ".jpg", dir);
                    cameraUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", image);
                    camera.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri);
                    camera.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    startActivityForResult(camera, FILE_REQUEST);
                    return;
                } catch (IOException | IllegalArgumentException e) {
                    Toast.makeText(this, "No se pudo preparar la cámara. Puedes elegir una imagen.", Toast.LENGTH_LONG).show();
                }
            }
            Toast.makeText(this, "No hay una aplicación de cámara disponible. Puedes elegir una imagen.", Toast.LENGTH_LONG).show();
        }
        // ACTION_OPEN_DOCUMENT grants a temporary URI permission; no broad gallery permission is needed.
        Intent picker = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        picker.addCategory(Intent.CATEGORY_OPENABLE);
        picker.setType("image/*");
        picker.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try { startActivityForResult(picker, FILE_REQUEST); }
        catch (ActivityNotFoundException e) {
            finishFileSelection(null);
            Toast.makeText(this, "No se encontró una aplicación para seleccionar imágenes.", Toast.LENGTH_LONG).show();
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode != CAMERA_PERMISSION) return;
        if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            launchFileSource();
        } else {
            finishFileSelection(null);
            Toast.makeText(this, "Permiso de cámara denegado. Actívalo en Ajustes para fotografiar el ticket; no se ha enviado ninguna foto.", Toast.LENGTH_LONG).show();
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != FILE_REQUEST) return;
        Uri result = resultCode == RESULT_OK ? (cameraUri != null ? cameraUri : (data == null ? null : data.getData())) : null;
        finishFileSelection(result == null ? null : new Uri[]{result});
        cameraUri = null;
        if (result != null) runOcr(result);
    }

    private void finishFileSelection(Uri[] result) {
        if (fileCallback != null) { fileCallback.onReceiveValue(result); fileCallback = null; }
    }

    /** Runs bundled ML Kit OCR after WebView receives the image. The image is never uploaded. */
    private void runOcr(Uri uri) {
        try {
            InputImage image = InputImage.fromFilePath(this, uri);
            if (recognizer == null) recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
            recognizer.process(image)
                    .addOnSuccessListener(result -> deliverOcr(result.getText()))
                    .addOnFailureListener(error -> deliverOcr(""));
        } catch (IOException | RuntimeException error) {
            deliverOcr("");
        }
    }

    private void deliverOcr(String text) {
        if (webView == null) return;
        final String payload = org.json.JSONObject.quote(text == null ? "" : text);
        runOnUiThread(() -> webView.evaluateJavascript("window.onNativeOcrText && window.onNativeOcrText(" + payload + ");", null));
    }

    @Override protected void onDestroy() {
        finishFileSelection(null);
        if (recognizer != null) recognizer.close();
        if (webView != null) webView.destroy();
        super.onDestroy();
    }
}
