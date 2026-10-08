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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.DownloadListener;
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

public class MainActivity extends Activity {
    private static final int BG=Color.rgb(7,10,29), PANEL=Color.rgb(17,23,51);
    private static final int CYAN=Color.rgb(0,220,255), PURPLE=Color.rgb(190,55,255);
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ConcurrentHashMap<String, Task> tasks=new ConcurrentHashMap<>();
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
          {"en","Video Downloader","Paste a link","Open","Download","Downloads","Ready","Advertisement","Pause","Resume","Cancel","Play","Share","Delete","No downloaded files yet","Enter a URL first","Download started","Download complete","Download failed","Choose language","Download only content you have permission to save.","Browser","Library","English","Русский","ქართული","Waiting","Paused","Downloading"},
          {"ru","Video Downloader","Вставь ссылку","Открыть","Скачать","Загрузки","Готово","Реклама","Пауза","Продолжить","Отмена","Открыть","Поделиться","Удалить","Пока нет загруженных файлов","Сначала введи ссылку","Загрузка началась","Загрузка завершена","Ошибка загрузки","Выбери язык","Скачивай только материалы, которые разрешено сохранять.","Браузер","Файлы","English","Русский","ქართული","Ожидание","На паузе","Загрузка"},
          {"ka","Video Downloader","ჩასვი ბმული","გახსნა","ჩამოტვირთვა","ჩამოტვირთვები","მზადაა","რეკლამა","პაუზა","გაგრძელება","გაუქმება","გახსნა","გაზიარება","წაშლა","ჩამოტვირთული ფაილები ჯერ არ არის","ჯერ შეიყვანე ბმული","ჩამოტვირთვა დაიწყო","ჩამოტვირთვა დასრულდა","ჩამოტვირთვა ვერ მოხერხდა","აირჩიე ენა","ჩამოტვირთე მხოლოდ ის მასალა, რომლის შენახვაც ნებადართულია.","ბრაუზერი","ფაილები","English","Русский","ქართული","მოლოდინი","შეჩერებულია","იტვირთება"},
          {"es","Video Downloader","Pega un enlace","Abrir","Descargar","Descargas","Listo","Publicidad","Pausar","Reanudar","Cancelar","Reproducir","Compartir","Eliminar","Aún no hay archivos descargados","Introduce un enlace primero","Descarga iniciada","Descarga completada","Error de descarga","Elegir idioma","Descarga solo contenido que tengas permiso para guardar.","Navegador","Biblioteca","English","Русский","ქართული","En espera","En pausa","Descargando"},
          {"de","Video Downloader","Link einfügen","Öffnen","Herunterladen","Downloads","Bereit","Werbung","Pause","Fortsetzen","Abbrechen","Abspielen","Teilen","Löschen","Noch keine Dateien heruntergeladen","Gib zuerst einen Link ein","Download gestartet","Download abgeschlossen","Download fehlgeschlagen","Sprache wählen","Lade nur Inhalte herunter, die du speichern darfst.","Browser","Mediathek","English","Русский","ქართული","Wartend","Pausiert","Wird heruntergeladen"},
          {"fr","Video Downloader","Coller un lien","Ouvrir","Télécharger","Téléchargements","Prêt","Publicité","Pause","Reprendre","Annuler","Lire","Partager","Supprimer","Aucun fichier téléchargé","Saisis d’abord un lien","Téléchargement démarré","Téléchargement terminé","Échec du téléchargement","Choisir la langue","Télécharge uniquement les contenus que tu es autorisé à enregistrer.","Navigateur","Bibliothèque","English","Русский","ქართული","En attente","En pause","Téléchargement"},
          {"tr","Video Downloader","Bağlantı yapıştır","Aç","İndir","İndirilenler","Hazır","Reklam","Duraklat","Sürdür","İptal","Oynat","Paylaş","Sil","Henüz indirilen dosya yok","Önce bir bağlantı gir","İndirme başladı","İndirme tamamlandı","İndirme başarısız","Dil seç","Yalnızca kaydetme iznin olan içerikleri indir.","Tarayıcı","Kitaplık","English","Русский","ქართული","Bekliyor","Duraklatıldı","İndiriliyor"}
        };
        int idx=0; for(int i=0;i<values[0].length;i++) if(values[0][i].equals(key)){idx=i;break;}
        int row=language.equals("ru")?1:language.equals("ka")?2:language.equals("es")?3:language.equals("de")?4:language.equals("fr")?5:language.equals("tr")?6:0;
        return values[row][idx];
    }

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);
        File d=downloadDir(); if(!d.exists()) d.mkdirs();
        createNotificationChannel();
        requestNotificationPermission();
        buildUi();
        refreshLibrary();
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
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);root.setPadding(dp(14),dp(8),dp(14),0);
        LinearLayout top=new LinearLayout(this);top.setGravity(android.view.Gravity.CENTER_VERTICAL);
        TextView logo=text("▶ ↓  Video Downloader",23,CYAN);logo.setTypeface(null,Typeface.BOLD);top.addView(logo,new LinearLayout.LayoutParams(0,dp(52),1));
        Button lang=button("文",false);top.addView(lang,new LinearLayout.LayoutParams(dp(48),dp(44)));lang.setOnClickListener(v->chooseLanguage());root.addView(top);
        urlInput=new EditText(this);urlInput.setSingleLine(true);urlInput.setTextColor(Color.WHITE);urlInput.setHintTextColor(Color.GRAY);urlInput.setHint(tr("Paste a link"));
        urlInput.setTextSize(14);urlInput.setPadding(dp(12),0,dp(12),0);urlInput.setBackgroundColor(PANEL);
        root.addView(urlInput,new LinearLayout.LayoutParams(-1,dp(48)));
        LinearLayout actions=new LinearLayout(this);Button open=button(tr("Open"),true),download=button(tr("Download"),false);
        actions.addView(open,new LinearLayout.LayoutParams(0,dp(46),1));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(46),1);p.leftMargin=dp(7);actions.addView(download,p);root.addView(actions);
        open.setOnClickListener(v->openAddress());download.setOnClickListener(v->downloadAddress());
        LinearLayout nav=new LinearLayout(this);Button browserTab=button(tr("Browser"),true),libraryTab=button(tr("Library"),false);
        nav.addView(browserTab,new LinearLayout.LayoutParams(0,dp(42),1));nav.addView(libraryTab,new LinearLayout.LayoutParams(0,dp(42),1));root.addView(nav);
        status=text(tr("Ready"),12,Color.LTGRAY);status.setPadding(0,dp(6),0,dp(6));root.addView(status);
        pageScroll=new ScrollView(this);LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);pageScroll.addView(content);
        browser=new WebView(this);browser.setBackgroundColor(BG);browser.getSettings().setJavaScriptEnabled(true);browser.getSettings().setDomStorageEnabled(true);browser.getSettings().setMediaPlaybackRequiresUserGesture(true);
        browser.setWebChromeClient(new WebChromeClient());browser.setWebViewClient(new WebViewClient());
        browser.setDownloadListener((url,ua,disp,mime,len)->startDownload(url,disp,mime));
        content.addView(browser,new LinearLayout.LayoutParams(-1,dp(420)));
        TextView qTitle=text(tr("Downloads"),17,CYAN);qTitle.setTypeface(null,Typeface.BOLD);qTitle.setPadding(0,dp(12),0,dp(6));content.addView(qTitle);
        taskList=new LinearLayout(this);taskList.setOrientation(LinearLayout.VERTICAL);content.addView(taskList);
        libraryTitle=text(tr("Library"),17,CYAN);libraryTitle.setTypeface(null,Typeface.BOLD);libraryTitle.setPadding(0,dp(12),0,dp(6));content.addView(libraryTitle);
        fileList=new LinearLayout(this);fileList.setOrientation(LinearLayout.VERTICAL);content.addView(fileList);
        root.addView(pageScroll,new LinearLayout.LayoutParams(-1,0,1));
        TextView ad=text(tr("Advertisement"),10,Color.GRAY);ad.setGravity(android.view.Gravity.CENTER);ad.setBackgroundColor(Color.rgb(12,15,34));root.addView(ad,new LinearLayout.LayoutParams(-1,dp(30)));
        setContentView(root);
        browserTab.setOnClickListener(v->{browser.setVisibility(View.VISIBLE);pageScroll.smoothScrollTo(0,0);});
        libraryTab.setOnClickListener(v->{refreshLibrary();pageScroll.smoothScrollTo(0,dp(600));});
        browser.loadUrl("https://www.google.com");
    }
    private void chooseLanguage(){
        new AlertDialog.Builder(this).setTitle(tr("Choose language")).setItems(new String[]{"English","Русский","ქართული","Español","Deutsch","Français","Türkçe"},(d,w)->{
            language=w==1?"ru":w==2?"ka":w==3?"es":w==4?"de":w==5?"fr":w==6?"tr":"en";buildUi();refreshLibrary();
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
    private void downloadAddress(){String raw=normalized(urlInput.getText().toString());if(raw.isEmpty()){toast(tr("Enter a URL first"));return;}startDownload(raw,null,null);}
    private File downloadDir(){File base=getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS);return base!=null?base:new File(getFilesDir(),"Download");}
    private String safeName(String name){name=name==null?"download":name.replaceAll("[\\/:*?\"<>|]","_").trim();if(name.isEmpty())name="download";return name.length()>100?name.substring(0,100):name;}
    private void startDownload(String raw,String disposition,String mime){
        final String url=raw;final String name=safeName(android.webkit.URLUtil.guessFileName(url,disposition,mime));
        if(tasks.containsKey(url)){toast("Already in download queue");return;}
        Task t=new Task(url,name);tasks.put(url,t);addTaskView(t);status.setText(tr("Download started"));t.start();
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
        final String url,name;volatile boolean paused=false,cancelled=false,done=false;volatile Update viewUpdate;
        Task(String u,String n){url=u;name=n;}
        @Override public void run(){
            File dest=new File(downloadDir(),name);File part=new File(downloadDir(),name+".part");HttpURLConnection conn=null;
            try{
                if(!downloadDir().exists())downloadDir().mkdirs();
                long offset=part.exists()?part.length():0;
                conn=(HttpURLConnection)new URL(url).openConnection();conn.setConnectTimeout(15000);conn.setReadTimeout(20000);conn.setInstanceFollowRedirects(true);conn.setRequestProperty("User-Agent","VideoDownloader/1.1");
                if(offset>0)conn.setRequestProperty("Range","bytes="+offset+"-");
                int code=conn.getResponseCode();
                if(code<200||code>=300)throw new Exception("HTTP "+code);
                if(offset>0&&code!=206){offset=0;part.delete();}
                long responseLength; if (Build.VERSION.SDK_INT >= 24) responseLength=conn.getContentLengthLong(); else responseLength=conn.getContentLength();
                long total=responseLength;if(total>0)total+=offset;
                InputStream in=new BufferedInputStream(conn.getInputStream());FileOutputStream out=new FileOutputStream(part,offset>0);
                byte[] buf=new byte[32768];long count=offset;int n;
                while((n=in.read(buf))!=-1){
                    synchronized(this){while(paused&&!cancelled)wait();}
                    if(cancelled)throw new InterruptedException("Cancelled");
                    out.write(buf,0,n);count+=n;
                    final int pct=total>0?(int)Math.min(99,count*100/total):0;
                    if(viewUpdate!=null)viewUpdate.set(pct,paused?tr("Paused"):tr("Downloading")+(total>0?" "+pct+"%":""));
                    postNotification(2000 + (Math.abs(url.hashCode()) % 500),name,pct,false);
                }
                out.flush();out.close();in.close();
                if(cancelled){part.delete();throw new InterruptedException("Cancelled");}
                if(dest.exists())dest=new File(downloadDir(),System.currentTimeMillis()+"_"+name);
                if(!part.renameTo(dest))throw new Exception("Could not save file");
                final File saved=dest;done=true;tasks.remove(url);if(viewUpdate!=null)viewUpdate.set(100,tr("Download complete"));
                postNotification(2000 + (Math.abs(url.hashCode()) % 500),name,100,true);main.post(()->{status.setText(tr("Download complete")+": "+saved.getName());refreshLibrary();});
            }catch(InterruptedException e){part.delete();done=true;tasks.remove(url);if(viewUpdate!=null)viewUpdate.set(0,tr("Cancel"));}
            catch(Exception e){done=true;tasks.remove(url);if(viewUpdate!=null)viewUpdate.set(0,tr("Download failed")+": "+e.getMessage());main.post(()->status.setText(tr("Download failed")));}
            finally{if(conn!=null)conn.disconnect();}
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
            row.addView(play,new LinearLayout.LayoutParams(0,dp(38),1));row.addView(share,new LinearLayout.LayoutParams(0,dp(38),1));row.addView(del,new LinearLayout.LayoutParams(0,dp(38),1));card.addView(row);fileList.addView(card);
            play.setOnClickListener(v->openFile(f));share.setOnClickListener(v->shareFile(f));del.setOnClickListener(v->{if(f.delete()){refreshLibrary();toast(tr("Delete"));}});
        }
    }
    private void openFile(File f){
        try{Uri u=FileProvider.getUriForFile(this,"com.videodownloader.app.fileprovider",f);Intent i=new Intent(Intent.ACTION_VIEW);i.setDataAndType(u,getContentResolver().getType(u));i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(i);}
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
    @Override protected void onDestroy(){for(Task t:tasks.values()){t.cancelled=true;synchronized(t){t.notifyAll();}}if(browser!=null)browser.destroy();super.onDestroy();}
}
