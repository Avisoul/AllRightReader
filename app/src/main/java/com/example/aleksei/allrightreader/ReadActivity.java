package com.example.aleksei.allrightreader;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.StringBuilderPrinter;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;


import com.github.mertakdut.BookSection;
import com.github.mertakdut.CssStatus;
import com.github.mertakdut.Reader;
import com.github.mertakdut.exception.OutOfPagesException;
import com.github.mertakdut.exception.ReadingException;

public class ReadActivity extends AppCompatActivity implements PageFragment.OnFragmentReadyListener {

        private Reader reader;
        private SeekBar pageControl = null;

        private ViewPager mViewPager;

        /**
         * The {@link android.support.v4.view.PagerAdapter} that will provide
         * fragments for each of the sections. We use a
         * (FragmentPagerAdapter} derivative, which will keep every
         * loaded fragment in memory. If this becomes too memory intensive, it
         * may be best to switch to a
         * {@link android.support.v4.app.FragmentStatePagerAdapter}.
         */
        private SectionsPagerAdapter mSectionsPagerAdapter;

        private int pageCount = Integer.MAX_VALUE;
        private int pxScreenWidth;
        private TextView progressTextview;
        private TextView percentageTextview;



        private boolean isSkippedToPage = false;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_read);
//            getActionBar().hide();

            progressTextview = (TextView)findViewById(R.id.progess_textview);
            percentageTextview = (TextView)findViewById(R.id.percentage_textview);

            pxScreenWidth = getResources().getDisplayMetrics().widthPixels;

            mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

            mViewPager = (ViewPager) findViewById(R.id.container);
            mViewPager.setOffscreenPageLimit(0);
            mViewPager.setAdapter(mSectionsPagerAdapter);

            if (getIntent() != null && getIntent().getExtras() != null) {
                String filePath = getIntent().getExtras().getString("filePath");

                try {
                    reader = new Reader();

                    // Setting optionals once per file is enough.
                    reader.setMaxContentPerSection(950);
                    reader.setCssStatus(CssStatus.INCLUDE);
                    reader.setIsIncludingTextContent(true);
                    reader.setIsOmittingTitleTag(true);


                    // This method must be called before readSection.
                    reader.setFullContent(filePath);

//                int lastSavedPage = reader.setFullContentWithProgress(filePath);
                    if (reader.isSavedProgressFound()) {
                        int lastSavedPage = reader.loadProgress();
                        mViewPager.setCurrentItem(lastSavedPage);
                    }

                } catch (ReadingException e) {
                    Toast.makeText(ReadActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                }

                pageControl = (SeekBar) findViewById(R.id.seek_bar);
                pageControl.setMax(getMaxPageNumber());
                try {
                    pageControl.setProgress(reader.loadProgress());
                    progressTextview.setText(getProgress(reader.loadProgress()));
                    percentageTextview.setText(getPercentage(reader.loadProgress()));
                } catch (ReadingException e) {
                    e.printStackTrace();
                }
                pageControl.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    int progressChanged = 0;
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        progressChanged = progress;
                        progressTextview.setText(getProgress(progressChanged));
                        percentageTextview.setText(getPercentage(progressChanged));
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        //Toast.makeText(ReadActivity.this, String.valueOf(progressChanged), Toast.LENGTH_SHORT).show();
                        mViewPager.setCurrentItem(progressChanged);
                        progressTextview.setText(getProgress(mViewPager.getCurrentItem()));
                        percentageTextview.setText(getPercentage(mViewPager.getCurrentItem()));
                        pageControl.setProgress(mViewPager.getCurrentItem());
                        pageControl.setMax(getMaxPageNumber());
                    }
                });
            }
            progressTextview.setText(getProgress(mViewPager.getCurrentItem()));
            percentageTextview.setText(getPercentage(mViewPager.getCurrentItem()));
            pageControl.setProgress(mViewPager.getCurrentItem());
            pageControl.setMax(getMaxPageNumber());
        }

        private String getPercentage(int count) {
            StringBuilder sb = new StringBuilder();
            double percentage = Math.round(((double)count * 1000)/((double)getMaxPageNumber()))/((double)10);
            sb.append(String.valueOf(percentage));
            sb.append(" %");
            return  sb.toString();
        }

        private String getProgress(int count){
            StringBuilder sb = new StringBuilder();
            sb.append(String.valueOf(count));
            sb.append("/");
            sb.append(String.valueOf(getMaxPageNumber()));
            return  sb.toString();
        }

        private int getMaxPageNumber() {
            int maxPageNumber = 100;
            try {
                BookSection bookSection = reader.readSection(Integer.MAX_VALUE);
            } catch (OutOfPagesException e) {
                e.printStackTrace();
                maxPageNumber = e.getPageCount() - 1;
            } catch (Exception e) {
            }
            return maxPageNumber;
        }

        @Override
        public View onFragmentReady(int position) {

            BookSection bookSection = null;

            try {
                bookSection = reader.readSection(position);
                progressTextview.setText(getProgress(mViewPager.getCurrentItem()));
                percentageTextview.setText(getPercentage(mViewPager.getCurrentItem()));
                pageControl.setProgress(mViewPager.getCurrentItem());
            } catch (ReadingException e) {
                e.printStackTrace();
                Toast.makeText(ReadActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
            } catch (OutOfPagesException e) {
                e.printStackTrace();
                this.pageCount = e.getPageCount();

                if (isSkippedToPage) {
                    Toast.makeText(ReadActivity.this, "Max page number is: " + this.pageCount, Toast.LENGTH_LONG).show();
                }

                mSectionsPagerAdapter.notifyDataSetChanged();
            }

            isSkippedToPage = false;

            if (bookSection != null) {
                return setFragmentView(bookSection.getSectionContent(), "text/html", "UTF-8"); // reader.isContentStyled
            }

            return null;
        }

        @Override
        public void onBackPressed() {
            super.onBackPressed();
        }

        @Override
        protected void onStop() {
            super.onStop();
            try {
                reader.saveProgress(mViewPager.getCurrentItem());
                Toast.makeText(ReadActivity.this, "Saved page: " + mViewPager.getCurrentItem() + "...", Toast.LENGTH_LONG).show();
            } catch (ReadingException e) {
                e.printStackTrace();
                Toast.makeText(ReadActivity.this, "Progress is not saved: " + e.getMessage(), Toast.LENGTH_LONG).show();
            } catch (OutOfPagesException e) {
                e.printStackTrace();
                Toast.makeText(ReadActivity.this, "Progress is not saved. Out of Bounds. Page Count: " + e.getPageCount(), Toast.LENGTH_LONG).show();
            }
        }


    private View setFragmentView(String data, String mimeType, String encoding) {

        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        WebView webView = new WebView(ReadActivity.this);
        webView.loadDataWithBaseURL(null, data, mimeType, encoding, null);


        webView.setLayoutParams(layoutParams);

        return webView;
    }


        public class SectionsPagerAdapter extends FragmentStatePagerAdapter {

            SectionsPagerAdapter(FragmentManager fm) {
                super(fm);
            }

            @Override
            public int getCount() {
                return pageCount;
            }

            @Override
            public Fragment getItem(int position) {
                // getItem is called to instantiate the fragment for the given page.
                return PageFragment.newInstance(position);
            }
        }
}