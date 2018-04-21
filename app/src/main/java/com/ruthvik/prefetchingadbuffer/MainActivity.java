package com.ruthvik.prefetchingadbuffer;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.google.android.gms.ads.formats.NativeAd;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

         /*Initialising AdMobAdBuffer*/
        AdmobAdBuffer.getInstance().setUpAdMobAds(this);
        /*Fetchs AdMobAds based on the mentioned size*/
        AdmobAdBuffer.getInstance().fetchAdmonAd();

        /*getAdmobAdObject returns the AdObject*/
        NativeAd sampleNativeAd = AdmobAdBuffer.getInstance().getAdmonAdObject();

        /*For recyclerViews Its better to use getAdForIndex() Method*/
        int position = 0;
        NativeAd sampleAd = AdmobAdBuffer.getInstance().getAdForIndex(position);


        /*After placing the Ad update the Boolean status for the AdObject. This helps to remove the correct AdObject after getting the ADImpression*/
        setAdDisplayedStatus(sampleAd);
    }

    private void setAdDisplayedStatus(com.google.android.gms.ads.formats.NativeAd localNativeAd) {
        try {
            if (AdmobAdBuffer.getInstance().mPrefetchedAdmobAdList != null && AdmobAdBuffer.getInstance().mPrefetchedAdmobAdList.get(0) != null) {
                AdmobAdBuffer.getInstance().mPrefetchedAdmobAdList.get(0).setBooleanStatus(true);
            }
        } catch (Exception e) {

        }
    }
}
