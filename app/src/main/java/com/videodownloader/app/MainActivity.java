package com.videodownloader.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.media.MediaExtractor;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import com.naman14.androidlame.AndroidLame;
import com.naman14.androidlame.LameBuilder;
import dev.ffmpegkit_maintained.ytdlp.YtDlp;
import dev.ffmpegkit_maintained.ytdlp.YtDlpException;
import dev.ffmpegkit_maintained.ytdlp.YtDlpRequest;
import dev.ffmpegkit_maintained.ytdlp.YtDlpResponse;
import dev.ffmpegkit_maintained.ytdlp.DownloadProgressCallback;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.DownloadListener;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private static volatile boolean ytDlpInitialized=false;
    private static final int BG=Color.rgb(7,10,29), PANEL=Color.rgb(17,23,51);
    private static final int CYAN=Color.rgb(0,220,255), PURPLE=Color.rgb(190,55,255);
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ConcurrentHashMap<String, Task> tasks=new ConcurrentHashMap<>();
    private static final Semaphore DOWNLOAD_SLOTS=new Semaphore(2,true);
    private final ArrayList<File> completed=new ArrayList<>();
    private LinearLayout root, taskList, fileList;
    private EditText urlInput;
    private TextView status, libraryTitle;
    private WebView browser;
    private ScrollView pageScroll;
    private String language="en";
    private int nextNotification=2000;
    private static final int NOTIFICATION_PERMISSION_REQUEST=7001;

    private String tr(String key) {
        String[][] values={
          {"en","Video Downloader","Paste a link","Open","Download","Downloads","Ready","Advertisement","Pause","Resume","Cancel","Play","Share","Delete","No downloaded files yet","Enter a URL first","Download started","Download complete","Download failed","Choose language","Download only content you have permission to save.","Browser","Files","English","Русский","ქართული","Waiting","Paused","Downloading","Extract MP3","Audio saved","Audio extraction failed"},
          {"ru","Video Downloader","Вставь ссылку","Открыть","Скачать","Загрузки","Готово","Реклама","Пауза","Продолжить","Отмена","Открыть","Поделиться","Удалить","Пока нет загруженных файлов","Сначала введи ссылку","Загрузка началась","Загрузка завершена","Ошибка загрузки","Выбери язык","Скачивай только материалы, которые разрешено сохранять.","Браузер","Файлы","English","Русский","ქართული","Ожидание","На паузе","Загрузка","Извлечь MP3","Аудио сохранено","Не удалось извлечь аудио"},
          {"ka","Video Downloader","ჩასვი ბმული","გახსნა","ჩამოტვირთვა","ჩამოტვირთვები","მზადაა","რეკლამა","პაუზა","გაგრძელება","გაუქმება","გახსნა","გაზიარება","წაშლა","ჩამოტვირთული ფაილები ჯერ არ არის","ჯერ შეიყვანე ბმული","ჩამოტვირთვა დაიწყო","ჩამოტვირთვა დასრულდა","ჩამოტვირთვა ვერ მოხერხდა","აირჩიე ენა","ჩამოტვირთე მხოლოდ ის მასალა, რომლის შენახვაც ნებადართულია.","ბრაუზერი","ფაილები","English","Русский","ქართული","მოლოდინი","შეჩერებულია","იტვირთება","MP3-ის მიღება","აუდიო შენახულია","აუდიოს ამოღება ვერ მოხერხდა"},
          {"es","Video Downloader","Pega un enlace","Abrir","Descargar","Descargas","Listo","Publicidad","Pausar","Reanudar","Cancelar","Reproducir","Compartir","Eliminar","Aún no hay archivos descargados","Introduce un enlace primero","Descarga iniciada","Descarga completada","Error de descarga","Elegir idioma","Descarga solo contenido que tengas permiso para guardar.","Navegador","Archivos","English","Русский","ქართული","En espera","En pausa","Descargando","Extraer MP3","Audio guardado","Error al extraer audio"},
          {"de","Video Downloader","Link einfügen","Öffnen","Herunterladen","Downloads","Bereit","Werbung","Pause","Fortsetzen","Abbrechen","Abspielen","Teilen","Löschen","Noch keine Dateien heruntergeladen","Gib zuerst einen Link ein","Download gestartet","Download abgeschlossen","Download fehlgeschlagen","Sprache wählen","Lade nur Inhalte herunter, die du speichern darfst.","Browser","Dateien","English","Русский","ქართული","Wartend","Pausiert","Wird heruntergeladen","MP3 erstellen","Audio gespeichert","Audioextraktion fehlgeschlagen"},
          {"fr","Video Downloader","Coller un lien","Ouvrir","Télécharger","Téléchargements","Prêt","Publicité","Pause","Reprendre","Annuler","Lire","Partager","Supprimer","Aucun fichier téléchargé","Saisis d’abord un lien","Téléchargement démarré","Téléchargement terminé","Échec du téléchargement","Choisir la langue","Télécharge uniquement les contenus que tu es autorisé à enregistrer.","Navigateur","Fichiers","English","Русский","ქართული","En attente","En pause","Téléchargement","Extraire MP3","Audio enregistré","Échec de l’extraction audio"},
          {"tr","Video Downloader","Bağlantı yapıştır","Aç","İndir","İndirilenler","Hazır","Reklam","Duraklat","Sürdür","İptal","Oynat","Paylaş","Sil","Henüz indirilen dosya yok","Önce bir bağlantı gir","İndirme başladı","İndirme tamamlandı","İndirme başarısız","Dil seç","Yalnızca kaydetme iznin olan içerikleri indir.","Tarayıcı","Dosyalar","English","Русский","ქართული","Bekliyor","Duraklatıldı","İndiriliyor","MP3 oluştur","Ses kaydedildi","Ses çıkarılamadı"}
        };
        int idx=0; for(int i=0;i<values[0].length;i++) if(values[0][i].equals(key)){idx=i;break;}
        int row=language.equals("ru")?1:language.equals("ka")?2:language.equals("es")?3:language.equals("de")?4:language.equals("fr")?5:language.equals("tr")?6:0;
        return values[row][idx];
    }

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        language=getSharedPreferences("settings",MODE_PRIVATE).getString("language","en");
        getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);
        File d=downloadDir(); if(!d.exists()) d.mkdirs();
        createNotificationChannel();
        requestNotificationPermission();
        buildUi();
        refreshLibrary();
        handleIncomingIntent(getIntent());
        restorePendingDownloads();
    }

    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
    private TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);return t;}
    private Button button(String s,boolean primary){
        Button b=new Button(this); b.setText(s); b.setTextColor(Color.WHITE); b.setTextSize(13); b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(primary?Color.rgb(0,105,150):Color.rgb(95,30,135)));
        return b;
    }
    private void buildUi(){
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);root.setPadding(dp(14),dp(20),dp(14),0);
        LinearLayout top=new LinearLayout(this);top.setGravity(android.view.Gravity.CENTER_VERTICAL);
        TextView logo=text("▶ ↓  Video Downloader",23,CYAN);logo.setTypeface(null,Typeface.BOLD);top.addView(logo,new LinearLayout.LayoutParams(0,dp(52),1));
        Button lang=button("文",false);top.addView(lang,new LinearLayout.LayoutParams(dp(48),dp(44)));lang.setOnClickListener(v->chooseLanguage());root.addView(top);
        urlInput=new EditText(this);urlInput.setSingleLine(true);urlInput.setTextColor(Color.WHITE);urlInput.setHintTextColor(Color.GRAY);urlInput.setHint(tr("Paste a link"));
        urlInput.setTextSize(14);urlInput.setPadding(dp(12),0,dp(12),0);urlInput.setBackgroundColor(PANEL);
        root.addView(urlInput,new LinearLayout.LayoutParams(-1,dp(48)));
        LinearLayout actions=new LinearLayout(this);Button open=button(tr("Open"),true),download=button(tr("Download"),false);
        actions.addView(open,new LinearLayout.LayoutParams(0,dp(46),1));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(46),1);p.leftMargin=dp(7);actions.addView(download,p);root.addView(actions);
        open.setOnClickListener(v->openAddress());download.setOnClickListener(v->downloadAddress());
        LinearLayout nav=new LinearLayout(this);Button browserTab=button(tr("Browser"),true),libraryTab=button(tr("Files"),false);
        nav.addView(browserTab,new LinearLayout.LayoutParams(0,dp(42),1));nav.addView(libraryTab,new LinearLayout.LayoutParams(0,dp(42),1));root.addView(nav);
        status=text(tr("Ready"),12,Color.LTGRAY);status.setPadding(0,dp(6),0,dp(6));root.addView(status);
        pageScroll=new ScrollView(this);LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);pageScroll.addView(content);
        browser=new WebView(this);browser.setBackgroundColor(BG);browser.getSettings().setJavaScriptEnabled(true);browser.getSettings().setDomStorageEnabled(true);browser.getSettings().setMediaPlaybackRequiresUserGesture(true);
        browser.setWebChromeClient(new WebChromeClient());browser.setWebViewClient(new WebViewClient());
        browser.setDownloadListener((url,ua,disp,mime,len)->startDownload(url,disp,mime,ua));
        content.addView(browser,new LinearLayout.LayoutParams(-1,dp(420)));browser.setVisibility(View.GONE);
        TextView qTitle=text(tr("Downloads"),17,CYAN);qTitle.setTypeface(null,Typeface.BOLD);qTitle.setPadding(0,dp(12),0,dp(6));content.addView(qTitle);qTitle.setVisibility(View.GONE);
        taskList=new LinearLayout(this);taskList.setOrientation(LinearLayout.VERTICAL);content.addView(taskList);taskList.setVisibility(View.GONE);
        libraryTitle=text(tr("Files"),17,CYAN);libraryTitle.setTypeface(null,Typeface.BOLD);libraryTitle.setPadding(0,dp(12),0,dp(6));content.addView(libraryTitle);libraryTitle.setVisibility(View.GONE);
        fileList=new LinearLayout(this);fileList.setOrientation(LinearLayout.VERTICAL);content.addView(fileList);fileList.setVisibility(View.GONE);
        root.addView(pageScroll,new LinearLayout.LayoutParams(-1,0,1));
        TextView ad=text(tr("Advertisement"),10,Color.GRAY);ad.setGravity(android.view.Gravity.CENTER);ad.setBackgroundColor(Color.rgb(12,15,34));root.addView(ad,new LinearLayout.LayoutParams(-1,dp(30)));
        setContentView(root);
        browserTab.setOnClickListener(v->{boolean show=browser.getVisibility()!=View.VISIBLE;browser.setVisibility(show?View.VISIBLE:View.GONE);qTitle.setVisibility(show?View.VISIBLE:View.GONE);taskList.setVisibility(show?View.VISIBLE:View.GONE);if(show){if(browser.getUrl()==null||browser.getUrl().isEmpty()||"about:blank".equals(browser.getUrl()))browser.loadUrl("https://www.google.com");pageScroll.smoothScrollTo(0,0);}});
        libraryTab.setOnClickListener(v->{boolean show=fileList.getVisibility()!=View.VISIBLE;libraryTitle.setVisibility(show?View.VISIBLE:View.GONE);fileList.setVisibility(show?View.VISIBLE:View.GONE);if(show){refreshLibrary();pageScroll.post(()->pageScroll.smoothScrollTo(0,libraryTitle.getTop()));}});
        browser.loadUrl("https://www.google.com");
    }
    private void chooseLanguage(){
        new AlertDialog.Builder(this).setTitle(tr("Choose language")).setItems(new String[]{"English","Русский","ქართული","Español","Deutsch","Français","Türkçe"},(d,w)->{
            language=w==1?"ru":w==2?"ka":w==3?"es":w==4?"de":w==5?"fr":w==6?"tr":"en";
            getSharedPreferences("settings",MODE_PRIVATE).edit().putString("language",language).apply();
            String currentUrl=browser==null?null:browser.getUrl();
            buildUi();
            if(currentUrl!=null&&!currentUrl.isEmpty())browser.loadUrl(currentUrl);
            for(Task task:tasks.values())addTaskView(task);
            refreshLibrary();
        }).show();
    }
    private String normalized(String raw){
        raw=raw.trim();if(raw.length()==0)return "";if(!raw.matches("(?i)^https?://.*"))raw="https://"+raw;return raw;
    }
    private void openAddress(){
        String raw=normalized(urlInput.getText().toString());if(raw.isEmpty()){toast(tr("Enter a URL first"));return;}
        try{Uri u=Uri.parse(raw);if(u.getHost()==null)throw new Exception();
            ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(urlInput.getWindowToken(),0);
            browser.loadUrl(raw);status.setText(u.getHost());
        }catch(Exception e){toast("Invalid URL");}
    }
    private void downloadAddress(){String raw=normalized(urlInput.getText().toString());if(raw.isEmpty()){toast(tr("Enter a URL first"));return;}new AlertDialog.Builder(this).setTitle("Choose download format").setItems(new String[]{"Video (best available, up to 720p)","Audio only (MP3)"},(d,which)->startDownload(raw,null,null,null,null,which==1)).setNegativeButton("Cancel",null).show();}
    private boolean needsExtractor(String raw){
        try{
            String host=Uri.parse(raw).getHost();
            if(host==null)return false;
            host=host.toLowerCase(Locale.ROOT);
            return host.equals("youtu.be")||host.endsWith(".youtu.be")||
                   host.equals("youtube.com")||host.endsWith(".youtube.com")||
                   host.equals("youtube-nocookie.com")||host.endsWith(".youtube-nocookie.com")||
                   host.equals("fb.watch")||host.endsWith(".fb.watch")||
                   host.equals("facebook.com")||host.endsWith(".facebook.com");
        }catch(Exception e){return false;}
    }
    private void startYtDlpDownload(String raw,String userAgent,boolean audioOnly){
        if(tasks.containsKey(raw)){toast("Already in download queue");return;}
        String ua=userAgent;
        if(ua==null||ua.trim().isEmpty())ua=browser!=null?browser.getSettings().getUserAgentString():"VideoDownloader/1.1";
        String displayName=audioOnly?"Audio MP3":"Online video";
        persistPending(raw,displayName,false);
        Task t=new Task(raw,displayName,ua,true,audioOnly);tasks.put(raw,t);DownloadKeepAliveService.markActive(raw);
        try{Intent keepAlive=new Intent(this,DownloadKeepAliveService.class);keepAlive.setAction(DownloadKeepAliveService.ACTION_START);if(Build.VERSION.SDK_INT>=26)startForegroundService(keepAlive);else startService(keepAlive);}catch(Exception ignored){}
        addTaskView(t);status.setText(tr("Download started"));t.start();
    }
    private File downloadDir(){File base=getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS);return base!=null?base:new File(getFilesDir(),"Download");}
    // Copy completed media into Android's shared media library so music/video players
    // and file managers can discover it, while keeping the app's own library intact.
    private void publishToMediaLibrary(File source){
        if(source==null||!source.isFile()||source.length()==0)return;
        android.net.Uri inserted=null;
        try{
            String ext=android.webkit.MimeTypeMap.getFileExtensionFromUrl(source.getName()).toLowerCase(Locale.ROOT);
            String mime=android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
            if(mime==null){
                if(ext.equals("ts"))mime="video/mp2t";
                else if(ext.equals("mkv"))mime="video/x-matroska";
                else if(ext.equals("flac"))mime="audio/flac";
                else mime="application/octet-stream";
            }
            boolean audio=mime.startsWith("audio/");
            boolean video=mime.startsWith("video/");
            if(Build.VERSION.SDK_INT>=29){
                android.net.Uri collection=audio?android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI:
                    video?android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI:
                    android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI;
                String relative=audio?android.os.Environment.DIRECTORY_MUSIC+"/Video Downloader":
                    video?android.os.Environment.DIRECTORY_MOVIES+"/Video Downloader":
                    android.os.Environment.DIRECTORY_DOWNLOADS+"/Video Downloader";
                android.content.ContentValues values=new android.content.ContentValues();
                String displayName=source.getName();
                values.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME,displayName);
                values.put(android.provider.MediaStore.MediaColumns.MIME_TYPE,mime);
                values.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH,relative);
                values.put(android.provider.MediaStore.MediaColumns.IS_PENDING,1);
                inserted=getContentResolver().insert(collection,values);
                if(inserted==null)throw new java.io.IOException("Android could not create a public media entry");
                try(java.io.InputStream in=new java.io.FileInputStream(source);
                    java.io.OutputStream out=getContentResolver().openOutputStream(inserted,"w")){
                    if(out==null)throw new java.io.IOException("Could not open public media file");
                    byte[] buffer=new byte[32768];int count;
                    while((count=in.read(buffer))!=-1)out.write(buffer,0,count);
                    out.flush();
                }
                android.content.ContentValues ready=new android.content.ContentValues();
                ready.put(android.provider.MediaStore.MediaColumns.IS_PENDING,0);
                getContentResolver().update(inserted,ready,null,null);
            }else{
                java.io.File publicDir=new java.io.File(android.os.Environment.getExternalStoragePublicDirectory(
                    audio?android.os.Environment.DIRECTORY_MUSIC:video?android.os.Environment.DIRECTORY_MOVIES:android.os.Environment.DIRECTORY_DOWNLOADS),
                    "Video Downloader");
                if(!publicDir.exists()&&!publicDir.mkdirs())throw new java.io.IOException("Could not create public media folder");
                java.io.File dest=new java.io.File(publicDir,source.getName());
                if(dest.exists())dest=new java.io.File(publicDir,System.currentTimeMillis()+"_"+source.getName());
                try(java.io.InputStream in=new java.io.FileInputStream(source);java.io.OutputStream out=new java.io.FileOutputStream(dest)){
                    byte[] buffer=new byte[32768];int count;while((count=in.read(buffer))!=-1)out.write(buffer,0,count);out.flush();
                }
                android.media.MediaScannerConnection.scanFile(this,new String[]{dest.getAbsolutePath()},new String[]{mime},null);
            }
        }catch(Exception e){
            if(inserted!=null)try{getContentResolver().delete(inserted,null,null);}catch(Exception ignored){}
            android.util.Log.w("VideoDownloader","Could not publish media to phone library: "+e.getMessage());
        }
    }
    private String safeName(String name){name=name==null?"download":name.replaceAll("[\\/:*?\"<>|]","_").trim();if(name.isEmpty())name="download";return name.length()>100?name.substring(0,100):name;}
    private void startDownload(String raw,String disposition,String mime){ startDownload(raw,disposition,mime,null,null); }
    private void startDownload(String raw,String disposition,String mime,String userAgent){ startDownload(raw,disposition,mime,null,userAgent); }
    private boolean isHlsUrl(String raw){return raw!=null&&raw.toLowerCase(Locale.ROOT).matches("(?s).*\\.m3u8(?:[?#].*)?$");}
    private String hlsOutputName(String raw,String requested){
        String base=requested;
        if(base==null||base.trim().isEmpty()){
            try{String path=Uri.parse(raw).getLastPathSegment();base=(path==null||path.isEmpty())?"stream":path.replaceAll("(?i)\\.m3u8$","");}catch(Exception e){base="stream";}
        }else base=base.replaceAll("(?i)\\.m3u8$","");
        base=safeName(base);
        if(!base.toLowerCase(Locale.ROOT).endsWith(".ts"))base+=".ts";
        return base;
    }
    private void startDownload(String raw,String disposition,String mime,String savedName,String userAgent){startDownload(raw,disposition,mime,savedName,userAgent,false);}
    private boolean isDirectMediaUrl(String raw){
        try{
            String path=Uri.parse(raw).getPath();
            if(path==null)return false;
            return path.toLowerCase(Locale.ROOT).matches("(?s).*\\.(mp4|m4v|mkv|webm|mov|avi|mpg|mpeg|3gp|ts|mp3|m4a|aac|wav|flac|ogg|opus|wma|m3u8)$");
        }catch(Exception e){return false;}
    }
    private boolean shouldUseExtractor(String raw,String disposition,String mime){
        if(needsExtractor(raw))return true;
        if(disposition!=null&&!disposition.trim().isEmpty())return false;
        if(mime!=null){
            String type=mime.toLowerCase(Locale.ROOT);
            if(type.startsWith("video/")||type.startsWith("audio/")||type.equals("application/octet-stream"))return false;
        }
        return !isDirectMediaUrl(raw);
    }
    private void startDownload(String raw,String disposition,String mime,String savedName,String userAgent,boolean audioOnly){
        if(isHlsUrl(raw)){if(audioOnly){toast("This stream link needs to be downloaded as video first; then use Extract MP3 in the library.");return;}inspectHlsAndStart(raw,disposition,mime,savedName,userAgent);return;}
        if(shouldUseExtractor(raw,disposition,mime)){startYtDlpDownload(raw,userAgent,audioOnly);return;}
        startDownloadRaw(raw,disposition,mime,savedName,userAgent,audioOnly);
    }
    private void startDownloadRaw(String raw,String disposition,String mime,String savedName,String userAgent){startDownloadRaw(raw,disposition,mime,savedName,userAgent,false);}
    private void startDownloadRaw(String raw,String disposition,String mime,String savedName,String userAgent,boolean audioOnly){
        final String url=raw;
        final String name=safeName(savedName==null?android.webkit.URLUtil.guessFileName(url,disposition,mime):savedName);
        if(tasks.containsKey(url)){toast("Already in download queue");return;}
        if(!url.matches("(?i)^https?://.+")){toast("Only direct HTTP/HTTPS links are supported");return;}
        persistPending(url,name,false);
        String ua=userAgent;if(ua==null||ua.trim().isEmpty())ua=browser!=null?browser.getSettings().getUserAgentString():"VideoDownloader/1.1";
        Task t=new Task(url,name,ua,false,audioOnly);tasks.put(url,t);DownloadKeepAliveService.markActive(url);
        try{Intent keepAlive=new Intent(this,DownloadKeepAliveService.class);keepAlive.setAction(DownloadKeepAliveService.ACTION_START);if(Build.VERSION.SDK_INT>=26)startForegroundService(keepAlive);else startService(keepAlive);}catch(Exception ignored){}
        addTaskView(t);status.setText(tr("Download started"));t.start();
    }
    private static class HlsVariant{
        final String url,label;
        HlsVariant(String u,String l){url=u;label=l;}
    }
    private String requestText(String target,String ua)throws Exception{
        HttpURLConnection cn=(HttpURLConnection)new URL(target).openConnection();
        cn.setConnectTimeout(15000);cn.setReadTimeout(20000);cn.setInstanceFollowRedirects(true);
        cn.setRequestProperty("User-Agent",ua==null||ua.isEmpty()?"VideoDownloader/1.1":ua);
        String cookie=CookieManager.getInstance().getCookie(target);if(cookie!=null&&!cookie.isEmpty())cn.setRequestProperty("Cookie",cookie);
        int code=cn.getResponseCode();if(code<200||code>=300){cn.disconnect();throw new java.io.IOException("HTTP "+code);}
        try(InputStream in=cn.getInputStream();java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream()){
            byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);
            return new String(out.toByteArray(),java.nio.charset.StandardCharsets.UTF_8);
        }finally{cn.disconnect();}
    }
    private void inspectHlsAndStart(String raw,String disposition,String mime,String savedName,String userAgent){
        if(tasks.containsKey(raw)){toast("Already in download queue");return;}
        String ua=userAgent;
        if(ua==null||ua.trim().isEmpty())ua=browser!=null?browser.getSettings().getUserAgentString():"VideoDownloader/1.1";
        final String agent=ua;
        status.setText("Checking stream qualities…");
        new Thread(()->{
            try{
                String manifest=requestText(raw,agent);
                if(!manifest.trim().startsWith("#EXTM3U"))throw new Exception("Not a valid HLS playlist");
                ArrayList<HlsVariant> variants=new ArrayList<>();
                String[] lines=manifest.split("\\r?\n");
                String pendingInfo=null;
                for(String line:lines){
                    line=line.trim();if(line.isEmpty())continue;
                    if(line.startsWith("#EXT-X-STREAM-INF:")){pendingInfo=line.substring(line.indexOf(':')+1);continue;}
                    if(pendingInfo!=null&&!line.startsWith("#")){
                        String resolution="";java.util.regex.Matcher rm=java.util.regex.Pattern.compile("RESOLUTION=(\\d+x\\d+)",java.util.regex.Pattern.CASE_INSENSITIVE).matcher(pendingInfo);
                        if(rm.find())resolution=rm.group(1);
                        String bandwidth="";java.util.regex.Matcher bm=java.util.regex.Pattern.compile("(?:AVERAGE-BANDWIDTH|BANDWIDTH)=(\\d+)",java.util.regex.Pattern.CASE_INSENSITIVE).matcher(pendingInfo);
                        if(bm.find())try{bandwidth=String.format(Locale.US,"%.1f Mbps",Integer.parseInt(bm.group(1))/1000000.0);}catch(Exception ignored){}
                        String label=!resolution.isEmpty()?resolution:(!bandwidth.isEmpty()?bandwidth:"Available quality");
                        if(!bandwidth.isEmpty()&&!resolution.isEmpty())label+=" · "+bandwidth;
                        variants.add(new HlsVariant(new URL(new URL(raw),line).toString(),label));pendingInfo=null;
                    }
                }
                if(variants.isEmpty()){
                    String output=hlsOutputName(raw,savedName);
                    main.post(()->startDownloadRaw(raw,disposition,"video/mp2t",output,agent));
                }else{
                    String[] labels=new String[variants.size()];for(int i=0;i<variants.size();i++)labels[i]=variants.get(i).label;
                    main.post(()->new AlertDialog.Builder(MainActivity.this).setTitle("Choose video quality").setItems(labels,(dialog,which)->{
                        HlsVariant chosen=variants.get(which);
                        String base=savedName;
                        if(base==null||base.trim().isEmpty())base=hlsOutputName(raw,null);
                        base=base.replaceAll("(?i)\\.ts$","").replaceAll("(?i)\\.m3u8$","")+"_"+chosen.label.replaceAll("[^A-Za-z0-9]+","_")+".ts";
                        startDownloadRaw(chosen.url,null,"video/mp2t",safeName(base),agent);
                    }).setNegativeButton("Cancel",null).show());
                }
            }catch(Exception e){String msg=e.getMessage()==null?"Could not read stream":e.getMessage();main.post(()->{status.setText("Stream check failed");toast(msg);});}
        },"hls-inspect").start();
    }
    private synchronized void persistPending(String url,String name,boolean remove){
        try{
            android.content.SharedPreferences prefs=getSharedPreferences("downloads",MODE_PRIVATE);
            JSONArray old=new JSONArray(prefs.getString("pending","[]"));JSONArray next=new JSONArray();
            for(int i=0;i<old.length();i++){
                JSONObject item=old.optJSONObject(i);
                if(item==null||url.equals(item.optString("url")))continue;
                next.put(item);
            }
            if(!remove){JSONObject item=new JSONObject();item.put("url",url);item.put("name",name);next.put(item);}
            prefs.edit().putString("pending",next.toString()).apply();
        }catch(Exception ignored){}
    }
    private void restorePendingDownloads(){
        try{
            JSONArray pending=new JSONArray(getSharedPreferences("downloads",MODE_PRIVATE).getString("pending","[]"));
            for(int i=0;i<pending.length();i++){
                JSONObject item=pending.optJSONObject(i);if(item==null)continue;
                String url=item.optString("url","");String name=item.optString("name","download");
                if(!url.isEmpty()&&!DownloadKeepAliveService.isActive(url))startDownload(url,null,null,name,null);
            }
        }catch(Exception ignored){}
    }
    private void addTaskView(Task t){
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(8),dp(6),dp(8),dp(6));card.setBackgroundColor(PANEL);
        TextView name=text(t.name,13,Color.WHITE);card.addView(name);
        TextView state=text(tr("Waiting"),11,Color.LTGRAY);card.addView(state);
        ProgressBar bar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);bar.setMax(100);card.addView(bar,new LinearLayout.LayoutParams(-1,dp(5)));
        LinearLayout row=new LinearLayout(this);Button pause=button(tr("Pause"),false),cancel=button(tr("Cancel"),false);
        row.addView(pause,new LinearLayout.LayoutParams(0,dp(38),1));row.addView(cancel,new LinearLayout.LayoutParams(0,dp(38),1));card.addView(row);taskList.addView(card,0);
        pause.setOnClickListener(v->{if(t.done)return;if(t.paused){t.paused=false;synchronized(t){t.notifyAll();}pause.setText(tr("Pause"));}else{t.paused=true;pause.setText(tr("Resume"));}});
        cancel.setOnClickListener(v->{t.cancelled=true;synchronized(t){t.notifyAll();}state.setText(tr("Cancel"));});
        t.viewUpdate=(pct,msg)->main.post(()->{bar.setProgress(pct);state.setText(msg);if(t.done){pause.setEnabled(false);cancel.setText(tr("Delete"));cancel.setOnClickListener(v->{card.setVisibility(View.GONE);});}});
    }
    private class Task extends Thread{
        final String url,name,userAgent;final boolean extractor,audioOnly;volatile boolean paused=false,cancelled=false,done=false,preservePartial=false;volatile Update viewUpdate;
        Task(String u,String n,String ua){this(u,n,ua,false,false);}
        Task(String u,String n,String ua,boolean useExtractor){this(u,n,ua,useExtractor,false);}
        Task(String u,String n,String ua,boolean useExtractor,boolean audio){url=u;name=n;userAgent=ua;extractor=useExtractor;audioOnly=audio;}
        @Override public void run(){
            File dest=new File(downloadDir(),name);File part=new File(downloadDir(),name+".part");HttpURLConnection conn=null;boolean slotAcquired=false;
            try{
                while(!cancelled&&!DOWNLOAD_SLOTS.tryAcquire(500,TimeUnit.MILLISECONDS)){if(viewUpdate!=null)viewUpdate.set(0,tr("Waiting"));}
                if(cancelled)throw new InterruptedException("Cancelled");
                slotAcquired=true;
                if(!downloadDir().exists())downloadDir().mkdirs();
                if(extractor){runYtDlp();return;}
                if(isHlsUrl(url)){downloadHlsStream(dest,part);return;}
                long offset=part.exists()?part.length():0;
                conn=(HttpURLConnection)new URL(url).openConnection();conn.setConnectTimeout(15000);conn.setReadTimeout(20000);conn.setInstanceFollowRedirects(true);conn.setRequestProperty("User-Agent",userAgent);
                String cookies=CookieManager.getInstance().getCookie(url);if(cookies!=null&&!cookies.isEmpty())conn.setRequestProperty("Cookie",cookies);
                if(offset>0)conn.setRequestProperty("Range","bytes="+offset+"-");
                int code=conn.getResponseCode();
                if(code==416&&offset>0){
                    conn.disconnect();part.delete();offset=0;
                    conn=(HttpURLConnection)new URL(url).openConnection();
                    conn.setConnectTimeout(15000);conn.setReadTimeout(20000);conn.setInstanceFollowRedirects(true);
                    conn.setRequestProperty("User-Agent",userAgent);
                    String retryCookies=CookieManager.getInstance().getCookie(url);if(retryCookies!=null&&!retryCookies.isEmpty())conn.setRequestProperty("Cookie",retryCookies);
                    code=conn.getResponseCode();
                }
                if(offset>0&&code==206){
                    String rangeHeader=conn.getHeaderField("Content-Range");
                    java.util.regex.Matcher rangeMatcher=java.util.regex.Pattern.compile("(?i)^bytes\\s+(\\d+)-(\\d+)/(\\d+|\\*)$").matcher(rangeHeader==null?"":rangeHeader.trim());
                    boolean validRange=rangeMatcher.matches()&&Long.parseLong(rangeMatcher.group(1))==offset;
                    if(!validRange){
                        conn.disconnect();part.delete();offset=0;
                        conn=(HttpURLConnection)new URL(url).openConnection();
                        conn.setConnectTimeout(15000);conn.setReadTimeout(20000);conn.setInstanceFollowRedirects(true);
                        conn.setRequestProperty("User-Agent",userAgent);
                        String retryCookies=CookieManager.getInstance().getCookie(url);if(retryCookies!=null&&!retryCookies.isEmpty())conn.setRequestProperty("Cookie",retryCookies);
                        code=conn.getResponseCode();
                    }
                }
                if(code<200||code>=300)throw new Exception("HTTP "+code);
                String responseType=conn.getContentType();
                if(responseType!=null&&responseType.toLowerCase(Locale.ROOT).contains("text/html")){
                    part.delete();persistPending(url,name,true);
                    throw new Exception("This is a web page, not a direct file link");
                }
                if(offset>0&&code!=206){offset=0;part.delete();}
                long responseLength; if (Build.VERSION.SDK_INT >= 24) responseLength=conn.getContentLengthLong(); else responseLength=conn.getContentLength();
                long total=responseLength;
                String contentRange=conn.getHeaderField("Content-Range");
                if(contentRange!=null){
                    java.util.regex.Matcher totalMatcher=java.util.regex.Pattern.compile("(?i)^bytes\\s+\\d+-\\d+/(\\d+)$").matcher(contentRange.trim());
                    if(totalMatcher.matches())try{total=Long.parseLong(totalMatcher.group(1));}catch(NumberFormatException ignored){}
                }else if(total>0&&offset>0)total+=offset;
                InputStream in=new BufferedInputStream(conn.getInputStream());FileOutputStream out=new FileOutputStream(part,offset>0);
                byte[] buf=new byte[32768];long count=offset;int n;
                long startedAt=System.currentTimeMillis(),lastUiUpdate=0,lastNotificationUpdate=0;
                while((n=in.read(buf))!=-1){
                    synchronized(this){while(paused&&!cancelled)wait();}
                    if(cancelled)throw new InterruptedException("Cancelled");
                    out.write(buf,0,n);count+=n;
                    final int pct=total>0?(int)Math.min(99,count*100/total):0;
                    long now=System.currentTimeMillis();
                    if(viewUpdate!=null&&(now-lastUiUpdate>=500)){
                        long transferred=count-offset;
                        long bytesPerSecond=transferred*1000/Math.max(1,now-startedAt);
                        String detail=tr("Downloading")+(total>0?" "+pct+"%":"")+" · "+android.text.format.Formatter.formatFileSize(MainActivity.this,transferred);
                        if(total>0)detail+=" / "+android.text.format.Formatter.formatFileSize(MainActivity.this,total);
                        if(bytesPerSecond>0)detail+=" · "+android.text.format.Formatter.formatFileSize(MainActivity.this,bytesPerSecond)+"/s";
                        viewUpdate.set(pct,paused?tr("Paused"):detail);
                        lastUiUpdate=now;
                    }
                    if(now-lastNotificationUpdate>=1000){
                        postNotification(2000 + (Math.abs(url.hashCode()) % 500),name,pct,false);
                        lastNotificationUpdate=now;
                    }
                }
                out.flush();out.close();in.close();
                if(total>0&&count<total)throw new java.io.IOException("Incomplete download: received "+count+" of "+total+" bytes");
                if(cancelled){part.delete();throw new InterruptedException("Cancelled");}
                if(dest.exists())dest=new File(downloadDir(),System.currentTimeMillis()+"_"+name);
                if(!part.renameTo(dest))throw new Exception("Could not save file");
                final File saved=dest;publishToMediaLibrary(saved);done=true;tasks.remove(url);persistPending(url,name,true);if(viewUpdate!=null)viewUpdate.set(100,tr("Download complete"));
                postNotification(2000 + (Math.abs(url.hashCode()) % 500),name,100,true);main.post(()->{status.setText(tr("Download complete")+": "+saved.getName());refreshLibrary();if(audioOnly&&!saved.getName().toLowerCase(Locale.ROOT).endsWith(".mp3"))convertAudioToMp3(saved,null,192);});
            }catch(InterruptedException e){if(!preservePartial){part.delete();persistPending(url,name,true);}done=true;tasks.remove(url);if(viewUpdate!=null)viewUpdate.set(0,preservePartial?tr("Waiting"):tr("Cancel"));}
            catch(Exception e){done=true;tasks.remove(url);if(!part.exists()||part.length()==0)persistPending(url,name,true);String error=e.getMessage()==null?e.getClass().getSimpleName():e.getMessage();if(error.length()>900)error=error.substring(error.length()-900);final String visibleError=error;if(viewUpdate!=null)viewUpdate.set(0,tr("Download failed")+": "+visibleError);main.post(()->{status.setText(tr("Download failed")+": "+visibleError);Toast.makeText(MainActivity.this,"Download failed: "+visibleError,Toast.LENGTH_LONG).show();});}
            finally{
                if(conn!=null)conn.disconnect();
                if(slotAcquired)DOWNLOAD_SLOTS.release();
                DownloadKeepAliveService.markInactive(url);
                if(!DownloadKeepAliveService.hasActiveDownloads()){
                    try{stopService(new Intent(MainActivity.this,DownloadKeepAliveService.class));}catch(Exception ignored){}
                }
            }
        }

        private void runYtDlp()throws Exception{
            long startedAt=System.currentTimeMillis();
            if(viewUpdate!=null)viewUpdate.set(0,"Preparing media extractor…");
            synchronized(MainActivity.class){
                if(!ytDlpInitialized){
                    try{YtDlp.init(getApplicationContext());ytDlpInitialized=true;}
                    catch(YtDlpException e){throw new Exception("Downloader initialization failed: "+e.getMessage(),e);}
                }
            }

            // Give every attempt its own output prefix so existing files never make a
            // successful extraction look like a failed download.
            String output=new File(downloadDir(),"vd_"+startedAt+"_%(title)s_%(id)s.%(ext)s").getAbsolutePath();
            String hostForExtractor=Uri.parse(url).getHost();
            String normalizedHost=hostForExtractor==null?"":hostForExtractor.toLowerCase(Locale.ROOT);
            boolean youtube=normalizedHost.equals("youtu.be")||normalizedHost.endsWith(".youtu.be")||
                normalizedHost.equals("youtube.com")||normalizedHost.endsWith(".youtube.com")||
                normalizedHost.equals("youtube-nocookie.com")||normalizedHost.endsWith(".youtube-nocookie.com");
            String[] clients=youtube?new String[]{"android","web_safari",""}:new String[]{""};

            // Export cookies from the embedded browser for the current URL and, for
            // Facebook share links, the destination domains they commonly redirect to.
            // Cookies stay in this app's cache and are passed only to yt-dlp for this URL.
            File cookieFile=new File(getCacheDir(),"ytdlp-cookies.txt");
            boolean haveCookies=false;
            try{
                ArrayList<String> cookieSources=new ArrayList<>();
                cookieSources.add(url);
                boolean facebook=normalizedHost.equals("facebook.com")||normalizedHost.endsWith(".facebook.com")||
                    normalizedHost.equals("fb.watch")||normalizedHost.endsWith(".fb.watch");
                if(facebook){
                    cookieSources.add("https://www.facebook.com/");
                    cookieSources.add("https://facebook.com/");
                    cookieSources.add("https://m.facebook.com/");
                    cookieSources.add("https://web.facebook.com/");
                    cookieSources.add("https://fb.watch/");
                }
                java.util.LinkedHashMap<String,String> cookieLines=new java.util.LinkedHashMap<>();
                for(String source:cookieSources){
                    String header=CookieManager.getInstance().getCookie(source);
                    if(header==null||header.trim().isEmpty())continue;
                    String sourceHost=Uri.parse(source).getHost();
                    if(sourceHost==null)continue;
                    sourceHost=sourceHost.toLowerCase(Locale.ROOT);
                    String cookieDomain=sourceHost;
                    boolean subdomains=false;
                    if(sourceHost.equals("youtube.com")||sourceHost.endsWith(".youtube.com")){
                        cookieDomain=".youtube.com";subdomains=true;
                    }else if(sourceHost.equals("facebook.com")||sourceHost.endsWith(".facebook.com")){
                        cookieDomain=".facebook.com";subdomains=true;
                    }else if(sourceHost.equals("fb.watch")||sourceHost.endsWith(".fb.watch")){
                        cookieDomain=".fb.watch";subdomains=true;
                    }
                    boolean secure=source.toLowerCase(Locale.ROOT).startsWith("https://");
                    for(String item:header.split(";\\s*")){
                        int eq=item.indexOf('=');
                        if(eq<=0)continue;
                        String cookieName=item.substring(0,eq).trim();
                        String cookieValue=item.substring(eq+1).trim();
                        if(cookieName.isEmpty()||cookieName.indexOf('\n')>=0||cookieValue.indexOf('\n')>=0||
                            cookieName.indexOf('\t')>=0||cookieValue.indexOf('\t')>=0)continue;
                        String key=cookieDomain+"\t"+cookieName;
                        String line=cookieDomain+"\t"+(subdomains?"TRUE":"FALSE")+"\t/\t"+(secure?"TRUE":"FALSE")+
                            "\t0\t"+cookieName+"\t"+cookieValue+"\n";
                        cookieLines.put(key,line);
                    }
                }
                if(!cookieLines.isEmpty()){
                    StringBuilder netscape=new StringBuilder("# Netscape HTTP Cookie File\n");
                    for(String line:cookieLines.values())netscape.append(line);
                    try(FileOutputStream cookieOut=new FileOutputStream(cookieFile,false)){
                        cookieOut.write(netscape.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    }
                    haveCookies=true;
                }
            }catch(Exception ignored){
                // Public videos should still be attempted when no browser cookies exist.
            }
            if(!haveCookies&&cookieFile.exists())cookieFile.delete();

            DownloadProgressCallback callback=new DownloadProgressCallback(){
                @Override public void onProgressUpdate(float progress,long etaInSeconds,String line){
                    final int pct=(int)Math.max(0,Math.min(99,progress));
                    final String detail=tr("Downloading")+" "+pct+"%"+(etaInSeconds>0?" · ETA "+etaInSeconds+"s":"");
                    if(viewUpdate!=null)viewUpdate.set(pct,detail);
                    postNotification(2000+(Math.abs(url.hashCode())%500),name,pct,false);
                }
            };

            YtDlpResponse response=null;
            String lastDetail="";
            for(String client:clients){
                if(cancelled)throw new InterruptedException("Cancelled");
                YtDlpRequest request=new YtDlpRequest(url).setOutputTemplate(output)
                    .addOption("--no-playlist")
                    .addOption("--verbose")
                    .addOption("--socket-timeout","20")
                    .addOption("--retries","5")
                    .addOption("--fragment-retries","5")
                    .addOption("--extractor-retries","3")
                    .addOption("--user-agent",userAgent);
                if(!client.isEmpty())request.addOption("--extractor-args","youtube:player_client="+client);
                boolean facebookSource=normalizedHost.equals("facebook.com")||normalizedHost.endsWith(".facebook.com")||normalizedHost.equals("fb.watch")||normalizedHost.endsWith(".fb.watch");
                if(facebookSource){
                    request.addOption("--referer","https://www.facebook.com/");
                    request.addOption("--user-agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36");
                }
                if(haveCookies)request.addOption("--cookies",cookieFile.getAbsolutePath());
                if(audioOnly){
                    // Produce MP3 directly instead of leaving the source audio/video beside it.
                    request.addOption("-f","bestaudio/best")
                        .addOption("-x")
                        .addOption("--audio-format","mp3")
                        .addOption("--audio-quality","192K");
                }else{
                    request.addOption("-f","best[height<=720]/best");
                }

                response=YtDlp.execute(request,callback);
                if(response!=null&&response.isSuccess())break;
                if(response==null){
                    lastDetail="Downloader returned no result. Check the internet connection and try again.";
                }else{
                    lastDetail=response.getErrorOutput();
                    if(lastDetail==null||lastDetail.trim().isEmpty())lastDetail=response.getOutput();
                    if(lastDetail==null||lastDetail.trim().isEmpty())lastDetail="yt-dlp exit code "+response.getExitCode();
                    lastDetail=lastDetail.replaceAll("(?m)^.*(?:WARNING:|DEBUG:).*$","").trim();
                    if(lastDetail.length()>900)lastDetail=lastDetail.substring(lastDetail.length()-900);
                }
                // Retry public YouTube extraction with another supported player client;
                // do not loop on other sites or on a user-cancelled task.
                if(!youtube)break;
                if(viewUpdate!=null)viewUpdate.set(0,"Retrying YouTube extraction…");
            }

            if(response==null)throw new Exception(lastDetail.isEmpty()?"Downloader returned no result. Check your internet connection and try again.":lastDetail);
            if(!response.isSuccess()){
                if(lastDetail.toLowerCase(Locale.ROOT).contains("403")||lastDetail.toLowerCase(Locale.ROOT).contains("forbidden")){
                    String sourceName=(normalizedHost.contains("facebook")||normalizedHost.equals("fb.watch")||normalizedHost.endsWith(".fb.watch"))?"Facebook":(youtube?"YouTube":"The source");
                    if(sourceName.equals("Facebook")){
                        throw new Exception("Facebook returned HTTP 403 (access denied). Open the exact video in the in-app browser and sign in if required, then retry. Check that the post is visible to your account. Private, restricted, or expired share links may not be downloadable.");
                    }
                    throw new Exception(sourceName+" returned HTTP 403. Open the source in the in-app browser, sign in if needed, then retry. Some videos are restricted or blocked by the platform and may not be downloadable.");
                }
                throw new Exception("Source download failed: "+(lastDetail.isEmpty()?"yt-dlp exit code "+response.getExitCode():lastDetail));
            }

            File[] files=downloadDir().listFiles();
            File saved=null;
            if(files!=null)for(File f:files){
                String n=f.getName().toLowerCase(Locale.ROOT);
                boolean temporary=n.endsWith(".part")||n.endsWith(".ytdl")||n.endsWith(".temp")||n.endsWith(".tmp");
                boolean requestedFormat=!audioOnly||n.endsWith(".mp3");
                if(f.isFile()&&f.lastModified()>=startedAt-2000&&!temporary&&requestedFormat&&!n.equals(name.toLowerCase(Locale.ROOT))){
                    if(saved==null||f.lastModified()>saved.lastModified())saved=f;
                }
            }
            if(saved==null){
                if(audioOnly)throw new Exception("Audio extraction finished without an MP3 file. The source may not provide audio or the extractor could not convert it.");
                throw new Exception("The extractor reported success, but no new output file was found. Try again and check available storage.");
            }
            done=true;tasks.remove(url);persistPending(url,name,true);
            if(viewUpdate!=null)viewUpdate.set(100,tr("Download complete"));
            postNotification(2000+(Math.abs(url.hashCode())%500),saved.getName(),100,true);
            final File completedFile=saved;
            // Publish only the requested final output. yt-dlp has already made MP3 for audio-only mode.
            publishToMediaLibrary(completedFile);
            main.post(()->{status.setText(tr("Download complete")+": "+completedFile.getName());refreshLibrary();});
        }

        private void downloadHlsStream(File dest,File part)throws Exception{
            String manifest=requestText(url,userAgent);
            if(!manifest.trim().startsWith("#EXTM3U"))throw new Exception("Not a valid HLS playlist");
            if(manifest.contains("#EXT-X-STREAM-INF:"))throw new Exception("Choose a quality level again by adding the master playlist");
            if(manifest.matches("(?s).*#EXT-X-KEY:(?!.*METHOD=NONE).*"))throw new Exception("Encrypted HLS streams are not supported");
            if(manifest.contains("#EXT-X-MAP:"))throw new Exception("Fragmented MP4 HLS is not supported yet; TS segments are required");
            ArrayList<String> segments=new ArrayList<>();
            for(String line:manifest.split("\\r?\n")){
                line=line.trim();if(!line.isEmpty()&&!line.startsWith("#"))segments.add(new URL(new URL(url),line).toString());
            }
            if(segments.isEmpty())throw new Exception("Playlist contains no media segments");
            long transferred=0;long started=System.currentTimeMillis();
            try(FileOutputStream out=new FileOutputStream(part,false)){
                byte[] buffer=new byte[32768];
                for(int i=0;i<segments.size();i++){
                    if(cancelled)throw new InterruptedException("Cancelled");
                    HttpURLConnection seg=(HttpURLConnection)new URL(segments.get(i)).openConnection();
                    seg.setConnectTimeout(15000);seg.setReadTimeout(20000);seg.setInstanceFollowRedirects(true);
                    seg.setRequestProperty("User-Agent",userAgent);
                    String cookie=CookieManager.getInstance().getCookie(segments.get(i));if(cookie!=null&&!cookie.isEmpty())seg.setRequestProperty("Cookie",cookie);
                    int code=seg.getResponseCode();if(code<200||code>=300){seg.disconnect();throw new java.io.IOException("Segment HTTP "+code);}
                    try(InputStream in=new BufferedInputStream(seg.getInputStream())){
                        int n;while((n=in.read(buffer))!=-1){
                            synchronized(this){while(paused&&!cancelled)wait();}
                            if(cancelled)throw new InterruptedException("Cancelled");
                            out.write(buffer,0,n);transferred+=n;
                            int pct=(int)Math.min(99,((long)i*100/segments.size()));
                            long now=System.currentTimeMillis();
                            if(viewUpdate!=null&&(now-started>0))viewUpdate.set(pct,"HLS "+(i+1)+"/"+segments.size()+" · "+android.text.format.Formatter.formatFileSize(MainActivity.this,transferred));
                            if(now-started>=0)postNotification(2000+(Math.abs(url.hashCode())%500),name,pct,false);
                        }
                    }finally{seg.disconnect();}
                }
                out.flush();
            }
            if(cancelled){part.delete();throw new InterruptedException("Cancelled");}
            if(part.length()==0)throw new Exception("No media data received");
            if(dest.exists())dest=new File(downloadDir(),System.currentTimeMillis()+"_"+name);
            if(!part.renameTo(dest))throw new Exception("Could not save HLS stream");
            File saved=dest;publishToMediaLibrary(saved);done=true;tasks.remove(url);persistPending(url,name,true);
            if(viewUpdate!=null)viewUpdate.set(100,tr("Download complete"));
            postNotification(2000+(Math.abs(url.hashCode())%500),name,100,true);
            main.post(()->{status.setText(tr("Download complete")+": "+saved.getName());refreshLibrary();});
        }
    }
    private interface Update{void set(int pct,String msg);}
    private void refreshLibrary(){
        if(fileList==null)return;fileList.removeAllViews();File[] fs=downloadDir().listFiles();
        if(fs==null||fs.length==0){fileList.addView(text(tr("No downloaded files yet"),12,Color.LTGRAY));return;}
        for(File f:fs){if(!f.isFile()||f.getName().endsWith(".part"))continue;
            LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(8),dp(5),dp(8),dp(5));card.setBackgroundColor(PANEL);
            card.addView(text(f.getName(),13,Color.WHITE));card.addView(text(android.text.format.Formatter.formatFileSize(this,f.length()),11,Color.LTGRAY));
            LinearLayout row=new LinearLayout(this);Button play=button(tr("Play"),true),share=button(tr("Share"),false),del=button(tr("Delete"),false);
            row.addView(play,new LinearLayout.LayoutParams(0,dp(38),1));row.addView(share,new LinearLayout.LayoutParams(0,dp(38),1));row.addView(del,new LinearLayout.LayoutParams(0,dp(38),1));card.addView(row);
            if(isMediaFile(f)){Button audio=button(tr("Extract MP3"),false);LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,dp(36));ap.topMargin=dp(4);card.addView(audio,ap);audio.setOnClickListener(v->extractAudio(f,audio));}
            fileList.addView(card);
            play.setOnClickListener(v->openFile(f));share.setOnClickListener(v->shareFile(f));del.setOnClickListener(v->{if(f.delete()){refreshLibrary();toast(tr("Delete"));}});
        }
    }
    private boolean isMediaFile(File f){
        String ext=android.webkit.MimeTypeMap.getFileExtensionFromUrl(f.getName()).toLowerCase(Locale.ROOT);
        String mime=android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
        return mime!=null&&(mime.startsWith("video/")||mime.startsWith("audio/"));
    }
    private void extractAudio(File source,Button action){
        new AlertDialog.Builder(this).setTitle("MP3 quality")
            .setItems(new String[]{"128 kbps · smaller file","192 kbps · recommended","320 kbps · highest bitrate"},(dialog,which)->convertAudioToMp3(source,action,new int[]{128,192,320}[which]))
            .setNegativeButton("Cancel",null).show();
    }
    private void convertAudioToMp3(File source,Button action,int bitrate){
        if(action!=null){action.setEnabled(false);action.setText(tr("Waiting"));}
        new Thread(()->{
            File output=null;MediaExtractor extractor=null;MediaCodec decoder=null;AndroidLame lame=null;FileOutputStream out=null;
            boolean decoderStarted=false;boolean inputDone=false;boolean outputDone=false;int samples=0;
            try{
                extractor=new MediaExtractor();extractor.setDataSource(source.getAbsolutePath());
                int audioTrack=-1;MediaFormat inputFormat=null;
                for(int i=0;i<extractor.getTrackCount();i++){
                    MediaFormat format=extractor.getTrackFormat(i);
                    String mime=format.getString(MediaFormat.KEY_MIME);
                    if(mime!=null&&mime.startsWith("audio/")){audioTrack=i;inputFormat=format;break;}
                }
                if(audioTrack<0)throw new Exception("No audio track found");
                int fallbackRate=inputFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)?inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE):44100;
                int fallbackChannels=inputFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)?inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT):2;
                String mime=inputFormat.getString(MediaFormat.KEY_MIME);
                decoder=MediaCodec.createDecoderByType(mime);decoder.configure(inputFormat,null,null,0);decoder.start();decoderStarted=true;extractor.selectTrack(audioTrack);
                String base=source.getName();int dot=base.lastIndexOf('.');if(dot>0)base=base.substring(0,dot);
                output=new File(downloadDir(),safeName(base+"_audio.mp3"));
                if(output.exists())output=new File(downloadDir(),safeName(base+"_audio_"+System.currentTimeMillis()+".mp3"));
                out=new FileOutputStream(output);
                MediaCodec.BufferInfo info=new MediaCodec.BufferInfo();byte[] encoded=new byte[256*1024];
                int sampleRate=fallbackRate,channels=fallbackChannels;
                while(!outputDone){
                    if(!inputDone){
                        int inputIndex=decoder.dequeueInputBuffer(10000);
                        if(inputIndex>=0){
                            ByteBuffer input=decoder.getInputBuffer(inputIndex);
                            if(input==null)throw new Exception("Audio decoder input unavailable");
                            input.clear();int size=extractor.readSampleData(input,0);
                            if(size<0){decoder.queueInputBuffer(inputIndex,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM);inputDone=true;}
                            else{long pts=extractor.getSampleTime();decoder.queueInputBuffer(inputIndex,0,size,Math.max(0,pts),0);extractor.advance();}
                        }
                    }
                    int outputIndex=decoder.dequeueOutputBuffer(info,10000);
                    if(outputIndex==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
                        MediaFormat decoded=decoder.getOutputFormat();
                        if(decoded.containsKey(MediaFormat.KEY_SAMPLE_RATE))sampleRate=decoded.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                        if(decoded.containsKey(MediaFormat.KEY_CHANNEL_COUNT))channels=decoded.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                    }else if(outputIndex>=0){
                        if(info.size>0&&(info.flags&MediaCodec.BUFFER_FLAG_CODEC_CONFIG)==0){
                            if(lame==null){
                                if(sampleRate<8000||sampleRate>48000)throw new Exception("This audio sample rate is not supported for MP3 conversion");
                                if(channels<1||channels>2)throw new Exception("MP3 conversion supports mono or stereo audio");
                                LameBuilder builder=new LameBuilder().setInSampleRate(sampleRate).setOutSampleRate(sampleRate).setOutChannels(channels).setOutBitrate(bitrate).setQuality(5);
                                if(channels==1)builder.setMode(LameBuilder.Mode.MONO);else builder.setMode(LameBuilder.Mode.JSTEREO);
                                lame=builder.build();
                            }
                            ByteBuffer pcm=decoder.getOutputBuffer(outputIndex);
                            if(pcm==null)throw new Exception("Audio decoder output unavailable");
                            pcm.position(info.offset);pcm.limit(info.offset+info.size);pcm=pcm.slice().order(ByteOrder.LITTLE_ENDIAN);
                            boolean floatPcm=false;
                            if(Build.VERSION.SDK_INT>=24){
                                MediaFormat decoded=decoder.getOutputFormat();
                                floatPcm=decoded.containsKey(MediaFormat.KEY_PCM_ENCODING)&&decoded.getInteger(MediaFormat.KEY_PCM_ENCODING)==android.media.AudioFormat.ENCODING_PCM_FLOAT;
                            }
                            short[] pcmSamples;
                            if(floatPcm){
                                int count=pcm.remaining()/4;pcmSamples=new short[count];
                                for(int i=0;i<count;i++){float value=pcm.getFloat();value=Math.max(-1f,Math.min(1f,value));pcmSamples[i]=(short)(value*32767f);}
                            }else{
                                int count=pcm.remaining()/2;pcmSamples=new short[count];
                                for(int i=0;i<count;i++)pcmSamples[i]=pcm.getShort();
                            }
                            int produced=lame.encodeBufferInterLeaved(pcmSamples,pcmSamples.length/channels,encoded);
                            if(produced>0)out.write(encoded,0,produced);
                            samples+=pcmSamples.length;
                        }
                        boolean eos=(info.flags&MediaCodec.BUFFER_FLAG_END_OF_STREAM)!=0;
                        decoder.releaseOutputBuffer(outputIndex,false);if(eos)outputDone=true;
                    }
                }
                if(lame==null)throw new Exception("No decoded audio samples");
                int tail=lame.flush(encoded);if(tail>0)out.write(encoded,0,tail);out.flush();out.close();out=null;
                if(!output.exists()||output.length()==0)throw new Exception("MP3 output is empty");
                File saved=output;publishToMediaLibrary(saved);
                main.post(()->{if(action!=null){action.setEnabled(true);action.setText(tr("Extract MP3"));}toast("MP3 saved: "+saved.getName());refreshLibrary();});
            }catch(Exception e){
                if(output!=null)output.delete();
                String error=e.getMessage()==null?"":e.getMessage();
                main.post(()->{if(action!=null){action.setEnabled(true);action.setText(tr("Extract MP3"));}toast(tr("Audio extraction failed")+(error.isEmpty()?"":": "+error));});
            }finally{
                if(out!=null)try{out.close();}catch(Exception ignored){}
                if(lame!=null)try{lame.close();}catch(Exception ignored){}
                if(decoder!=null){if(decoderStarted)try{decoder.stop();}catch(Exception ignored){}try{decoder.release();}catch(Exception ignored){}}
                if(extractor!=null)try{extractor.release();}catch(Exception ignored){}
            }
        },"mp3-converter").start();
    }
    private void handleIncomingIntent(Intent intent){
        if(intent==null||!Intent.ACTION_SEND.equals(intent.getAction()))return;
        if("text/plain".equals(intent.getType())){
            String shared=intent.getStringExtra(Intent.EXTRA_TEXT);
            if(shared!=null){java.util.regex.Matcher m=java.util.regex.Pattern.compile("https?://\\S+",java.util.regex.Pattern.CASE_INSENSITIVE).matcher(shared);if(m.find()){String link=m.group().replaceAll("[),.]+$","");urlInput.setText(link);browser.loadUrl(link);status.setText(link);}}
        }
    }
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);handleIncomingIntent(intent);}
    private void openFile(File f){
        try{
            Uri u=FileProvider.getUriForFile(this,"com.videodownloader.app.fileprovider",f);
            String mime=android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(android.webkit.MimeTypeMap.getFileExtensionFromUrl(f.getName()));
            Intent i=new Intent(Intent.ACTION_VIEW);i.setDataAndType(u,mime==null?"*/*":mime);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(i);
        }
        catch(Exception e){shareFile(f);}
    }
    private void shareFile(File f){try{Uri u=FileProvider.getUriForFile(this,"com.videodownloader.app.fileprovider",f);Intent i=new Intent(Intent.ACTION_SEND);i.setType("*/*");i.putExtra(Intent.EXTRA_STREAM,u);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(Intent.createChooser(i,tr("Share")));}catch(Exception e){toast(e.getMessage());}}
    private void requestNotificationPermission(){
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},NOTIFICATION_PERMISSION_REQUEST);
        }
    }
    private void createNotificationChannel(){if(Build.VERSION.SDK_INT>=26){NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);nm.createNotificationChannel(new NotificationChannel("downloads","Downloads",NotificationManager.IMPORTANCE_LOW));}}
    private void postNotification(int id,String name,int pct,boolean done){
        try{Notification n=new NotificationCompat.Builder(this,"downloads").setSmallIcon(android.R.drawable.stat_sys_download_done).setContentTitle(name).setContentText(done?tr("Download complete"):tr("Downloading")+" "+pct+"%").setProgress(100,pct,!done).setOngoing(!done).build();((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(id,n);}catch(Exception ignored){}
    }
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    @Override public void onBackPressed(){if(browser!=null&&browser.canGoBack())browser.goBack();else super.onBackPressed();}
    @Override protected void onDestroy(){
        // Transfers run under DownloadKeepAliveService; keep them alive when the Activity closes.
        if(browser!=null)browser.destroy();
        super.onDestroy();
    }
}
