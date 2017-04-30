package com.pinetask.app;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.webkit.WebView;
import android.widget.TextView;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiActivity;

import butterknife.BindView;
import butterknife.ButterKnife;

/** Shows version, copyright, and license attribution info. **/
public class AboutActivity extends PineTaskActivity
{
    @BindView(R.id.appNameAndVersionTextView) TextView mAppNameAndVersionTextView;
    @BindView(R.id.licenseInfoTextView) TextView mLicenseInfoTextView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about_activity);
        ButterKnife.bind(this);
        String versionName = BuildConfig.VERSION_NAME;
        mAppNameAndVersionTextView.setText(String.format("%s %s %s", getString(R.string.app_name), getString(R.string.version), versionName));
    }
}
