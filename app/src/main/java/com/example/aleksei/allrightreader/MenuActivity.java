package com.example.aleksei.allrightreader;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ProgressBar;

import com.example.aleksei.allrightreader.BookManager.BookInfo;
import com.example.aleksei.allrightreader.BookManager.BookInfoGridAdapter;
import com.example.aleksei.allrightreader.FileManager.DownloadFileActivity;
import com.github.mertakdut.Reader;
import com.github.mertakdut.exception.ReadingException;

import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Evgenia on 22.04.2017.
 */

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
public class MenuActivity extends AppCompatActivity {

    //Permissions
    final String[] EXTERNAL_PERMS = {Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE};
    final int EXTERNAL_REQUEST = 138;

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
        private FileManager fileManager = new FileManager(MenuActivity.this);

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected List<BookInfo> doInBackground(Object... params) {
            List<BookInfo> bookInfoList = null;
            try {
                bookInfoList = fileManager.searchForEpubFiles();
            } catch (IOException e) {
                e.printStackTrace();
            }

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
                System.out.println(occuredException.getMessage());
            }
        }
    }


}
