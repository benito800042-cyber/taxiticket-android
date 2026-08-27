package com.taxiticket.app;

import android.Manifest;
import android.app.Activity;
import android.os.Message;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.*;
import android.webkit.*;
import android.widget.FrameLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity {
 private static final int PICK_FILE = 4101, REQ_CAMERA = 4102;
 private WebView web; private FrameLayout root; private ValueCallback<Uri[]> fileCallback;
 private boolean captureRequested; private Uri cameraUri; private WebView child;
 @Override public void onCreate(Bundle state) { super.onCreate(state);
 root = new FrameLayout(this); web = createWebView(); root.addView(web); setContentView(root);
 web.loadUrl("file:///android_asset/index.html"); }
 private WebView createWebView() {
 WebView v = new WebView(this); v.setBackgroundColor(0xfff5f7fb);
 WebSettings s=v.getSettings(); s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true);
 s.setAllowFileAccess(true); s.setAllowContentAccess(true); s.setMediaPlaybackRequiresUserGesture(false);
 v.setWebViewClient(new WebViewClient() { @Override public void onPageFinished(WebView view, String url) { view.evaluateJavascript("window.print=function(){TaxiTicketAndroid.print()};", null); } });
 v.addJavascriptInterface(new PrintBridge(v), "TaxiTicketAndroid");
 v.setWebChromeClient(new WebChromeClient() {
 @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> cb, FileChooserParams params) {
 if (fileCallback != null) fileCallback.onReceiveValue(null); fileCallback=cb; captureRequested=params.isCaptureEnabled();
 if (captureRequested && ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
 new android.app.AlertDialog.Builder(MainActivity.this).setTitle("Permiso de cámara").setMessage("TaxiTicket necesita la cámara únicamente para fotografiar el ticket ahora. No se enviará la foto a ningún servidor.").setNegativeButton("Cancelar", (d,w) -> { if(fileCallback!=null){fileCallback.onReceiveValue(null);fileCallback=null;} }).setPositiveButton("Continuar", (d,w) -> ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.CAMERA},REQ_CAMERA)).show();
 } else launchPicker(); return true; }
 @Override public boolean onCreateWindow(WebView view, boolean dialog, boolean userGesture, Message resultMsg) { child=createWebView(); root.addView(child,new FrameLayout.LayoutParams(-1,-1)); WebView.WebViewTransport t=(WebView.WebViewTransport)resultMsg.obj; t.setWebView(child); resultMsg.sendToTarget(); return true; }
 }); return v; }
 private void launchPicker() { Intent docs=new Intent(Intent.ACTION_OPEN_DOCUMENT); docs.addCategory(Intent.CATEGORY_OPENABLE); docs.setType("image/*"); Intent chooser=Intent.createChooser(docs,"Selecciona o haz una foto del ticket"); if (captureRequested) { Intent camera=new Intent(MediaStore.ACTION_IMAGE_CAPTURE); try { File dir=new File(getCacheDir(),"images"); if(!dir.exists())dir.mkdirs(); File f=File.createTempFile("ticket_", ".jpg", dir); cameraUri=FileProvider.getUriForFile(this,"com.taxiticket.app.fileprovider",f); camera.putExtra(MediaStore.EXTRA_OUTPUT,cameraUri); camera.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_READ_URI_PERMISSION); chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS,new Intent[]{camera}); } catch(IOException ignored) {} } startActivityForResult(chooser,PICK_FILE); captureRequested=false; }
 @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){super.onRequestPermissionsResult(request,permissions,results); if(request==REQ_CAMERA){if(results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED)launchPicker();else if(fileCallback!=null){fileCallback.onReceiveValue(null);fileCallback=null;}}}
 @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(req==PICK_FILE){Uri[] out=null;if(result==RESULT_OK){Uri u=data!=null?data.getData():null;if(u==null)u=cameraUri;if(u!=null)out=new Uri[]{u};}if(fileCallback!=null){fileCallback.onReceiveValue(out);fileCallback=null;}cameraUri=null;}}
 @Override public void onBackPressed(){if(child!=null){root.removeView(child);child.destroy();child=null;}else if(web.canGoBack())web.goBack();else super.onBackPressed();}
 private static class PrintBridge { private final WebView view; PrintBridge(WebView v){view=v;} @android.webkit.JavascriptInterface public void print(){ PrintManager pm=(PrintManager)view.getContext().getSystemService(Context.PRINT_SERVICE); if(pm!=null) pm.print("TaxiTicket", view.createPrintDocumentAdapter("TaxiTicket"), new PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build()); } }
}
