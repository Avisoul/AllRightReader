package com.example.aleksei.allrightreader;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.aleksei.allrightreader.FileManager.DownloadFileActivity;
import com.github.mertakdut.Reader;
import com.github.mertakdut.exception.ReadingException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Evgenia on 22.04.2017.
 */

public class MenuActivity extends AppCompatActivity {

    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

//        ((GridView) findViewById(R.id.grid_book_info)).setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
//                String clickedItemFilePath = ((BookInfo) adapterView.getAdapter().getItem(i)).getFilePath();
//                final Intent intent = new Intent(MenuActivity.this, ReadActivity.class);
//                intent.putExtra("filePath", clickedItemFilePath);
//                intent.putExtra("isWebView", true);
//                startActivity(intent);
//            }
//        });

        ((Button) findViewById(R.id.download_from_cloud)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent intent = new Intent(MenuActivity.this, DownloadFileActivity.class);
                startActivity(intent);
            }
        });


        progressBar = (ProgressBar) findViewById(R.id.progressbar);

        new ListBookInfoTask().execute();
    }

    final String[] EXTERNAL_PERMS = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE
    };

    final int EXTERNAL_REQUEST = 138;
    @Override
    protected void onResume() {
        super.onResume();

        requestForPermission();
    }

    @TargetApi(Build.VERSION_CODES.M)
    public boolean requestForPermission() {

        boolean isPermissionOn = true;
        final int version = Build.VERSION.SDK_INT;
        if (version >= 23) {
            if (!canAccessExternalSd()) {
                isPermissionOn = false;
                requestPermissions(EXTERNAL_PERMS, EXTERNAL_REQUEST);
            }
        }

        return isPermissionOn;
    }

    public boolean canAccessExternalSd() {
        return (hasPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE));
    }

    private boolean hasPermission(String perm) {
        return (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this, perm));

    }

    private class ListBookInfoTask extends AsyncTask<Object, Object, List<BookInfo>> {

        private Exception occuredException;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected List<BookInfo> doInBackground(Object... params) {
            List<BookInfo> bookInfoList = searchForPdfFiles();

            Reader reader = new Reader();
            for (BookInfo bookInfo : bookInfoList) {
                try {
                    reader.setInfoContent(bookInfo.getFilePath());

                    String title = reader.getInfoPackage().getMetadata().getTitle();
                    if (title != null && !title.equals("")) {
                        bookInfo.setTitle(reader.getInfoPackage().getMetadata().getTitle());
                    } else { // If title doesn't exist, use fileName instead.
                        int dotIndex = bookInfo.getTitle().lastIndexOf('.');
                        bookInfo.setTitle(bookInfo.getTitle().substring(0, dotIndex));
                    }

                    bookInfo.setCoverImage(reader.getCoverImage());
                } catch (ReadingException e) {
                    occuredException = e;
                    e.printStackTrace();
                }
            }

            return bookInfoList;
        }

        @Override
        protected void onPostExecute(List<BookInfo> bookInfoList) {
            super.onPostExecute(bookInfoList);
            progressBar.setVisibility(View.GONE);

            if (bookInfoList != null) {
                BookInfoGridAdapter adapter = new BookInfoGridAdapter(MenuActivity.this, bookInfoList);
                ((GridView) findViewById(R.id.grid_book_info)).setAdapter(adapter);
            }

            if (occuredException != null) {
                Toast.makeText(MenuActivity.this, occuredException.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private List<BookInfo> searchForPdfFiles() {
        boolean isSDPresent = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);

        List<BookInfo> bookInfoList = null;

        if (isSDPresent) {
            bookInfoList = new ArrayList<>();

            List<File> files = getListFiles(new File(Environment.getExternalStorageDirectory().getAbsolutePath()));

            File sampleFile = getFileFromAssets("pg28885-images_new.epub");
            files.add(0, sampleFile);
            File sampleFile2 = getFileFromAssets("PhysicsSyllabus.epub");
            files.add(1, sampleFile2);
            File sampleFile3 = getFileFromAssets("The Silver Chair.epub");
            files.add(2,sampleFile3);

            for (File file : files) {
                BookInfo bookInfo = new BookInfo();

                bookInfo.setTitle(file.getName());
                bookInfo.setFilePath(file.getPath());

                bookInfoList.add(bookInfo);
            }
        }

        return bookInfoList;
    }

    public File getFileFromAssets(String fileName) {

        File file = new File(getCacheDir() + "/" + fileName);

        if (!file.exists()) try {

            InputStream is = getAssets().open(fileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            FileOutputStream fos = new FileOutputStream(file);
            fos.write(buffer);
            fos.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return file;
    }

    private List<File> getListFiles(File parentDir) {
        ArrayList<File> inFiles = new ArrayList<>();
        File[] files = parentDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".epub");
            }
        });
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    inFiles.addAll(getListFiles(file));
                } else {
                    if (file.getName().endsWith(".epub")) {
                        inFiles.add(file);
                    }
                }
            }
        }
        return inFiles;
    }
}
