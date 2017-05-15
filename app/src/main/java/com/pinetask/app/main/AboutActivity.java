package com.pinetask.app.main;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.TextView;

import com.pinetask.app.BuildConfig;
import com.pinetask.app.R;
import com.pinetask.app.common.PineTaskActivity;

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
