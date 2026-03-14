/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.lifecycle;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.example.android.lifecycle.util.Counter;
import com.example.android.lifecycle.util.StatusTracker;
import com.example.android.lifecycle.util.Utils;

/**
 * Activity B — Increases the Thread Counter by 5 each time it is created.
 * Also tracks onPause() and onDestroy() call counts.
 */
public class ActivityB extends Activity {

    private String mActivityName;
    private TextView mStatusView;
    private TextView mStatusAllView;
    private TextView mPauseCountView;
    private TextView mDestroyCountView;
    private TextView mThreadCountView;

    private StatusTracker mStatusTracker = StatusTracker.getInstance();
    private Counter mThreadCounter = Counter.getInstance();

    private int mOnPauseCount = 0;
    private int mOnDestroyCount = 0;

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        mOnPauseCount   = savedInstanceState.getInt("OnPauseCount", 0);
        mOnDestroyCount = savedInstanceState.getInt("OnDestroyCount", 0);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putInt("OnPauseCount",   mOnPauseCount);
        savedInstanceState.putInt("OnDestroyCount", mOnDestroyCount + 1);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mOnPauseCount   = savedInstanceState.getInt("OnPauseCount", 0);
            mOnDestroyCount = savedInstanceState.getInt("OnDestroyCount", 0);
        }

        setContentView(R.layout.activity_b);
        mActivityName     = getString(R.string.activity_b_label);
        mStatusView       =  findViewById(R.id.status_view_b);
        mStatusAllView    =  findViewById(R.id.status_view_all_b);
        mPauseCountView   =  findViewById(R.id.pause_counter_view_b);
        mDestroyCountView =  findViewById(R.id.destroy_counter_view_b);
        mThreadCountView  =  findViewById(R.id.thread_counter_view_b);

        // Activity B increases Thread Counter by 5 on creation
        mThreadCounter.incrementBy(5);

        mStatusTracker.setStatus(mActivityName, getString(R.string.on_create));
        Utils.printStatus(mStatusView, mStatusAllView);
        updateCounterViews();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mStatusTracker.setStatus(mActivityName, getString(R.string.on_start));
        Utils.printStatus(mStatusView, mStatusAllView);
        updateCounterViews();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        mStatusTracker.setStatus(mActivityName, getString(R.string.on_restart));
        Utils.printStatus(mStatusView, mStatusAllView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mStatusTracker.setStatus(mActivityName, getString(R.string.on_resume));
        Utils.printStatus(mStatusView, mStatusAllView);
        updateCounterViews();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mOnPauseCount++;
        mStatusTracker.setStatus(mActivityName, getString(R.string.on_pause));
        Utils.printStatus(mStatusView, mStatusAllView);
        updateCounterViews();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mStatusTracker.setStatus(mActivityName, getString(R.string.on_stop));
    }

    @Override
    protected void onDestroy() {
        mOnDestroyCount++;
        mStatusTracker.setStatus(mActivityName, getString(R.string.on_destroy));
        super.onDestroy();
    }

    /** Refreshes the onPause, onDestroy, and Thread counter TextViews. */
    private void updateCounterViews() {
        if (mPauseCountView   != null) mPauseCountView.setText(String.valueOf(mOnPauseCount));
        if (mDestroyCountView != null) mDestroyCountView.setText(String.valueOf(mOnDestroyCount));
        if (mThreadCountView  != null) mThreadCountView.setText(String.valueOf(mThreadCounter.getCount()));
    }

    public void startDialog(View v) {
        Intent intent = new Intent(ActivityB.this, DialogActivity.class);
        startActivity(intent);
    }

    public void startActivityA(View v) {
        Intent intent = new Intent(ActivityB.this, ActivityA.class);
        startActivity(intent);
    }

    public void finishActivityB(View v) {
        ActivityB.this.finish();
    }
}
