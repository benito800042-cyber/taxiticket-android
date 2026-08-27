package com.taxiticket.app;
import android.Manifest;
import android.app.Activity;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.webkit.*;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
  private static final int FILE_REQUEST=41, CAMERA_PERMISSION=42;
  private WebView webView; private ValueCallback<Uri[]> fileCallback; private Uri cameraUri; private boolean captureRequested;
  @Override public void onCreate(Bundle state) {
    super.onCreate(state); webView=new WebView(this); WebSettings s=webView.getSettings();
    s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true); s.setAllowFileAccess(true); s.setAllowContentAccess(true);
    webView.setWebChromeClient(new WebChromeClient(){ @Override public boolean onShowFileChooser(WebView v,ValueCallback<Uri[]> cb,FileChooserParams p){
      if(fileCallback!=null)fileCallback.onReceiveValue(null); fileCallback=cb; captureRequested=p.isCaptureEnabled();
      if(captureRequested && Build.VERSION.SDK_INT>=23 && checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED) requestPermissions(new String[]{Manifest.permission.CAMERA},CAMERA_PERMISSION); else launchSource(); return true; }});
    setContentView(webView); webView.loadUrl("file:///android_asset/index.html");
  }
  private void launchSource(){
    if(captureRequested){ Intent i=new Intent(MediaStore.ACTION_IMAGE_CAPTURE); if(i.resolveActivity(getPackageManager())!=null) try{
      File d=new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES),"tickets"); if(!d.exists()&&!d.mkdirs())throw new IOException();
      File f=File.createTempFile("ticket_"+new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.US).format(new Date()),".jpg",d); cameraUri=FileProvider.getUriForFile(this,getPackageName()+".fileprovider",f);
      i.putExtra(MediaStore.EXTRA_OUTPUT,cameraUri); i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION); startActivityForResult(i,FILE_REQUEST); return;
    }catch(IOException|IllegalArgumentException e){ Toast.makeText(this,"No se pudo preparar la cámara. Puedes elegir una imagen.",Toast.LENGTH_LONG).show(); }
      Toast.makeText(this,"No hay una aplicación de cámara disponible. Puedes elegir una imagen.",Toast.LENGTH_LONG).show();
    }
    Intent p=new Intent(Intent.ACTION_OPEN_DOCUMENT); p.addCategory(Intent.CATEGORY_OPENABLE); p.setType("image/*"); p.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
    try{startActivityForResult(p,FILE_REQUEST);}catch(ActivityNotFoundException e){finishSelection(null);Toast.makeText(this,"No se encontró una aplicación para seleccionar imágenes.",Toast.LENGTH_LONG).show();}
  }
  @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==CAMERA_PERMISSION)if(g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED)launchSource();else{finishSelection(null);Toast.makeText(this,"Permiso de cámara denegado. Actívalo en Ajustes para fotografiar el ticket; no se ha enviado ninguna foto.",Toast.LENGTH_LONG).show();}}
  @Override protected void onActivityResult(int r,int code,Intent data){super.onActivityResult(r,code,data);if(r!=FILE_REQUEST)return;Uri u=code==RESULT_OK?(cameraUri!=null?cameraUri:(data==null?null:data.getData())):null;finishSelection(u==null?null:new Uri[]{u});cameraUri=null;}
  private void finishSelection(Uri[] u){if(fileCallback!=null){fileCallback.onReceiveValue(u);fileCallback=null;}}
  @Override protected void onDestroy(){finishSelection(null);if(webView!=null)webView.destroy();super.onDestroy();}
}
