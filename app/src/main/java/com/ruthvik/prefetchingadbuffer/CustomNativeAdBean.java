package com.ruthvik.prefetchingadbuffer;

import com.google.android.gms.ads.formats.NativeAd;

/**
 * Created by admin on 3/16/2018.
 */

public class CustomNativeAdBean {


    public NativeAd nativeAd;
    public boolean booleanStatus;


    public CustomNativeAdBean(NativeAd localNativeAd, boolean isDiplayed) {
        this.nativeAd = localNativeAd;
        this.booleanStatus = isDiplayed;
    }


    public NativeAd getNativeAd() {
        return nativeAd;
    }

    public void setNativeAd(NativeAd nativeAd) {
        this.nativeAd = nativeAd;
    }

    public boolean isBooleanStatus() {
        return booleanStatus;
    }

    public void setBooleanStatus(boolean booleanStatus) {
        this.booleanStatus = booleanStatus;
    }

}
