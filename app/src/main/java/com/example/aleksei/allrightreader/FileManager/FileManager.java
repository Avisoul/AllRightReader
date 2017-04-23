package com.example.aleksei.allrightreader.FileManager;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;

import com.example.aleksei.allrightreader.BookManager.BookInfo;
import com.example.aleksei.allrightreader.MenuActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by asmadek on 22.04.17.
 */

public class FileManager {
    Activity mActivity;

    public FileManager(Activity activity){
        this.mActivity = activity;
    }

    public List<BookInfo> searchForEpubFiles() throws IOException {
        boolean isSDPresent = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);

        List<BookInfo> bookInfoList = null;

        if (isSDPresent) {
            bookInfoList = new ArrayList<>();

            List<File> files = getListFiles(new File(Environment.getExternalStorageDirectory().getAbsolutePath()));
            String[] asset_files = mActivity.getAssets().list("");

            if (asset_files != null) {
                for (String file : asset_files) {
                    String extention = file.substring(file.lastIndexOf(".") + 1, file.length());
                    if(extention.equals("epub")) {
                        files.add(getFileFromAssets(file));
                    }
                }
            }

            for (File file : files) {
                BookInfo bookInfo = new BookInfo();

                bookInfo.setTitle(file.getName());
                bookInfo.setFilePath(file.getPath());

                bookInfoList.add(bookInfo);
            }
        }

        return bookInfoList;
    }

    private File getFileFromAssets(String fileName) {

        File file = new File(mActivity.getCacheDir() + "/" + fileName);

        if (!file.exists()) try {

            InputStream is = mActivity.getAssets().open(fileName);
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

        File[] files = parentDir.listFiles();
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
