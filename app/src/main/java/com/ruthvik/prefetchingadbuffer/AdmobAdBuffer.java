/*
 * Copyright (c) 2017 Yahoo Inc. All rights reserved.
 * Copyright (c) 2017 Clockbyte LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ruthvik.prefetchingadbuffer;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.formats.NativeAd;
import com.google.android.gms.ads.formats.NativeAdOptions;
import com.google.android.gms.ads.formats.NativeAppInstallAd;
import com.google.android.gms.ads.formats.NativeContentAd;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;


public class AdmobAdBuffer extends CustomAdmobFetcherBase {

    private final String TAG = AdmobAdBuffer.class.getCanonicalName();
    private static AdmobAdBuffer admobFetcher;

    /*Number of Ads that need to be in the buffer*/
    public static final int PREFETCH_ADS_LIMIT_SIZE = 2;
    /*Maximum number of times to try fetching Ads after failing attempts*/
    public static final int MAX_PREFETCH_ATTEMPT_SIZE = 4;
    public Context mContext;
    public int numberOfFetchedAds;
    private SparseArray adMapAtIndex = new SparseArray();


    private AdLoader mAdLoader;
    private int mFetchingAdsCnt = 0;
    public List<CustomNativeAdBean> mPrefetchedAdmobAdList = new ArrayList<>();


    private EnumSet<AdmobAdType> adType = EnumSet.allOf(AdmobAdType.class);

    public EnumSet<AdmobAdType> getAdType() {
        return adType;
    }

    public void setAdType(EnumSet<AdmobAdType> adType) {
        this.adType = adType;
    }

    public AdmobAdBuffer() {
    }

    @Override
    public int getFetchingAdsCount() {
        return 0;
    }

    public static AdmobAdBuffer getInstance() {
        if (admobFetcher == null) {
            admobFetcher = new AdmobAdBuffer();
        }
        return admobFetcher;
    }

// Method to return Ad based on AdType

    public synchronized NativeAd getAdmonAdObject() {
        NativeAd nativeAd = null;
        if (mPrefetchedAdmobAdList != null && mPrefetchedAdmobAdList.size() > 0) {
            nativeAd = mPrefetchedAdmobAdList.get(0).getNativeAd();
        }
        return nativeAd;
    }


    public synchronized NativeAd getAdForIndex(final int index) {
        NativeAd adNative = null;
        if (index >= 0)
            adNative = (NativeAd) adMapAtIndex.get(index);

        if (adNative == null && mPrefetchedAdmobAdList.size() > 0) {
            if (mPrefetchedAdmobAdList.get(0) != null)
                adNative = mPrefetchedAdmobAdList.get(0).getNativeAd();

            if (adNative != null) {
                adMapAtIndex.put(index, adNative);
            }
        }

        ensurePrefetchAdCount(); // Make sure we have enough pre-fetched ads
        return adNative;
    }

    public synchronized void removeDisplayedAd() {
        if (mPrefetchedAdmobAdList != null && mPrefetchedAdmobAdList.size() > 0) {
            if (mPrefetchedAdmobAdList.get(0) != null && mPrefetchedAdmobAdList.get(0).isBooleanStatus()) {
                mPrefetchedAdmobAdList.remove(0);
            }
        }
        ensurePrefetchAdCount();
    }

    private void ensurePrefetchAdCount() {
        if (mPrefetchedAdmobAdList.size() < PREFETCH_ADS_LIMIT_SIZE && mFetchFailCount < MAX_PREFETCH_ATTEMPT_SIZE) {
            fetchAdmonAd();
        }
    }

    public void fetchAdmonAd() {
        if (mContext != null) {
            Log.i(TAG, "Fetching Ad now");
            if (lockFetch.getAndSet(true))
                return;
            mFetchingAdsCnt++;
            mAdLoader.loadAd(getAdRequest()); //Fetching the ads item
        } else {
            mFetchFailCount++;
            Log.i(TAG, "Context is null, not fetching Ad");
        }
    }

    public String getDefaultUnitId() {
        return "ADMOB ADUNIT ID";
    }

    public synchronized void setUpAdMobAds(Context mContext) {
        this.mContext = mContext;
        final String unitId = getDefaultUnitId();
        MobileAds.initialize(mContext, "Place your AdmobNativeAdId");
        final AdLoader.Builder adloaderBuilder = new AdLoader.Builder(mContext, unitId)
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(int errorCode) {
                        // Handle the failure by logging, altering the UI, etc.
                        lockFetch.set(false);
                        mFetchFailCount++;
                        mFetchingAdsCnt++;
                        ensurePrefetchAdCount();
                    }

                    @Override
                    public void onAdClicked() {
                        super.onAdClicked();
                    }

                    @Override
                    public void onAdImpression() {
                        super.onAdImpression();
                        // Removing the Ad only after getting the impression, So that Impression rate would be higher
                        removeDisplayedAd();
                    }
                })
                .withNativeAdOptions(new NativeAdOptions.Builder()
                        // Methods in the NativeAdOptions.Builder class can be
                        // used here to specify individual options settings.
                        .build());
        adloaderBuilder.forAppInstallAd(new NativeAppInstallAd.OnAppInstallAdLoadedListener() {
            @Override
            public void onAppInstallAdLoaded(NativeAppInstallAd appInstallAd) {
                onAdFetched(appInstallAd);
            }
        });
        adloaderBuilder.forContentAd(new NativeContentAd.OnContentAdLoadedListener() {
            @Override
            public void onContentAdLoaded(NativeContentAd contentAd) {
                onAdFetched(contentAd);
            }
        });

        mAdLoader = adloaderBuilder.build();
    }

    private synchronized void onAdFetched(NativeAd nativeAd) {
        int index = -1;
        // checking if the FetchedAd can be used
        if (canUseThisAd(nativeAd)) {
            CustomNativeAdBean customNativeAdBean = new CustomNativeAdBean(nativeAd, false);
            mPrefetchedAdmobAdList.add(customNativeAdBean);
            index = mPrefetchedAdmobAdList.size() - 1;
            mNoOfFetchedAds++;
            numberOfFetchedAds++;
        }
        lockFetch.set(false);
        mFetchFailCount = 0;
        ensurePrefetchAdCount();
        notifyObserversOfAdSizeChange(index);
    }

    private boolean canUseThisAd(NativeAd adNative) {
        if (adNative != null) {
            NativeAd.Image logoImage = null;
            CharSequence header = null, body = null;
            if (adNative instanceof NativeContentAd) {
                NativeContentAd ad = (NativeContentAd) adNative;
                logoImage = ad.getLogo();
                header = ad.getHeadline();
                body = ad.getBody();
            } else if (adNative instanceof NativeAppInstallAd) {
                NativeAppInstallAd ad = (NativeAppInstallAd) adNative;
                logoImage = ad.getIcon();
                header = ad.getHeadline();
                body = ad.getBody();
            }

            if (!TextUtils.isEmpty(header)
                    && !TextUtils.isEmpty(body)) {
                return true;
            }
        }
        return false;
    }


    public synchronized void destroyAllAds() {
        mFetchingAdsCnt = 0;
        adMapAtIndex.clear();
        super.destroyAllAds();
    }

    public synchronized void clearMapAds() {
        adMapAtIndex.clear();
        mFetchingAdsCnt = mPrefetchedAdmobAdList.size();
    }

}
