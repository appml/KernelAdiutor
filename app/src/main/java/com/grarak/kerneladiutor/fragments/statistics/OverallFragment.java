/*
 * Copyright (C) 2015-2016 Willi Ye <williye97@gmail.com>
 *
 * This file is part of Kernel Adiutor.
 *
 * Kernel Adiutor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Kernel Adiutor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Kernel Adiutor.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.grarak.kerneladiutor.fragments.statistics;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bvalosek.cpuspy.CpuSpyApp;
import com.bvalosek.cpuspy.CpuStateMonitor;
import com.grarak.kerneladiutor.fragments.BaseControlFragment;
import com.grarak.kerneladiutor.fragments.BaseFragment;
import com.grarak.kerneladiutor.utils.Utils;
import com.grarak.kerneladiutor.utils.kernel.cpu.CPUFreq;
import com.grarak.kerneladiutor.utils.kernel.cpu.Temperature;
import com.grarak.kerneladiutor.views.XYGraph;
import com.grarak.kerneladiutor.views.recyclerview.CardView;
import com.grarak.kerneladiutor.views.recyclerview.CircularText;
import com.grarak.kerneladiutor.views.recyclerview.DescriptionView;
import com.grarak.kerneladiutor.views.recyclerview.FrequencyButtonView;
import com.grarak.kerneladiutor.views.recyclerview.FrequencyTableView;
import com.grarak.kerneladiutor.views.recyclerview.RecyclerViewItem;
import com.grarak.kerneladiutor.views.recyclerview.TitleView;
import com.grarak.kerneladiutordonate.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by willi on 19.04.16.
 */
public class OverallFragment extends BaseControlFragment {

    private static final String TAG = OverallFragment.class.getSimpleName();

    private CPUUsageFragment mCPUUsageFragment;

    private CircularText mCPUTemp;
    private CircularText mGPUTemp;
    private CircularText mBatteryTemp;

    private CardView mFreqBig;
    private CardView mFreqLITTLE;
    private CpuSpyApp mCpuSpyBig;
    private CpuSpyApp mCpuSpyLITTLE;

    private double mBatteryRaw;

    private boolean mUpdateFrequency;

    @Override
    protected void init() {
        super.init();
        addViewPagerFragment(mCPUUsageFragment = new CPUUsageFragment());
        if (getSavedInstanceState() != null) {
            mUpdateFrequency = getSavedInstanceState().getBoolean("updateFrequency");
        }
    }

    @Override
    protected List<RecyclerViewItem> addItems(List<RecyclerViewItem> items) {
        temperatureInit(items);
        frequenciesInit(items);
        return items;
    }

    private void temperatureInit(List<RecyclerViewItem> parent) {
        CardView tempCard = new CardView();
        tempCard.setTitle(getString(R.string.temperature));

        if (Temperature.hasCPU()) {
            mCPUTemp = new CircularText();
            mCPUTemp.setTitle(getString(R.string.cpu));
            mCPUTemp.setMessage(Temperature.getCPU(getActivity()));
            mCPUTemp.setColor(Temperature.getCPUColor(getActivity()));
            tempCard.addItem(mCPUTemp);
        }

        if (Temperature.hasGPU()) {
            mGPUTemp = new CircularText();
            mGPUTemp.setTitle(getString(R.string.gpu));
            mGPUTemp.setMessage(Temperature.getGPU(getActivity()));
            mGPUTemp.setColor(Temperature.getGPUColor(getActivity()));
            tempCard.addItem(mGPUTemp);
        }

        mBatteryTemp = new CircularText();
        mBatteryTemp.setTitle(getString(R.string.battery));
        tempCard.addItem(mBatteryTemp);

        parent.add(tempCard);
    }

    private void frequenciesInit(List<RecyclerViewItem> items) {
        TitleView frequencyTitle = new TitleView();
        frequencyTitle.setText(getString(R.string.frequencies));
        items.add(frequencyTitle);

        FrequencyButtonView frequencyButtonView = new FrequencyButtonView();
        frequencyButtonView.setRefreshListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateFrequency();
            }
        });
        frequencyButtonView.setResetListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CpuStateMonitor cpuStateMonitor = mCpuSpyBig.getCpuStateMonitor();
                CpuStateMonitor cpuStateMonitorLITTLE = null;
                if (mCpuSpyLITTLE != null) {
                    cpuStateMonitorLITTLE = mCpuSpyLITTLE.getCpuStateMonitor();
                }
                try {
                    cpuStateMonitor.setOffsets();
                    if (cpuStateMonitorLITTLE != null) {
                        cpuStateMonitorLITTLE.setOffsets();
                    }
                } catch (CpuStateMonitor.CpuStateMonitorException ignored) {
                }
                mCpuSpyBig.saveOffsets(getActivity());
                if (mCpuSpyLITTLE != null) {
                    mCpuSpyLITTLE.saveOffsets(getActivity());
                }
                updateView(cpuStateMonitor, mFreqBig);
                if (cpuStateMonitorLITTLE != null) {
                    updateView(cpuStateMonitorLITTLE, mFreqLITTLE);
                }
                scroll(500);
            }
        });
        frequencyButtonView.setRestoreListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CpuStateMonitor cpuStateMonitor = mCpuSpyBig.getCpuStateMonitor();
                CpuStateMonitor cpuStateMonitorLITTLE = null;
                if (mCpuSpyLITTLE != null) {
                    cpuStateMonitorLITTLE = mCpuSpyLITTLE.getCpuStateMonitor();
                }
                cpuStateMonitor.removeOffsets();
                if (cpuStateMonitorLITTLE != null)
                    cpuStateMonitorLITTLE.removeOffsets();
                mCpuSpyBig.saveOffsets(getActivity());
                if (mCpuSpyLITTLE != null) {
                    mCpuSpyLITTLE.saveOffsets(getActivity());
                }
                updateView(cpuStateMonitor, mFreqBig);
                if (mCpuSpyLITTLE != null) {
                    updateView(cpuStateMonitorLITTLE, mFreqLITTLE);
                }
                scroll(500);
            }
        });
        items.add(frequencyButtonView);

        mFreqBig = new CardView();
        if (CPUFreq.isBigLITTLE()) {
            mFreqBig.setTitle(getString(R.string.cluster_big));
        }
        items.add(mFreqBig);

        if (CPUFreq.isBigLITTLE()) {
            mFreqLITTLE = new CardView();
            mFreqLITTLE.setTitle(getString(R.string.cluster_little));
            items.add(mFreqLITTLE);
        }

        mCpuSpyBig = new CpuSpyApp(CPUFreq.getBigCpu());
        if (CPUFreq.isBigLITTLE()) {
            mCpuSpyLITTLE = new CpuSpyApp(CPUFreq.getLITTLECpu());
        }

        updateFrequency();
    }

    private void updateFrequency() {
        if (!mUpdateFrequency) {
            new FrequencyTask().execute();
        }
    }

    private class FrequencyTask extends AsyncTask<Void, Void, Void> {
        private CpuStateMonitor mBigMonitor;
        private CpuStateMonitor mLITTLEMonitor;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mUpdateFrequency = true;
        }

        @Override
        protected Void doInBackground(Void... params) {
            mBigMonitor = mCpuSpyBig.getCpuStateMonitor();
            if (CPUFreq.isBigLITTLE()) {
                mLITTLEMonitor = mCpuSpyLITTLE.getCpuStateMonitor();
            }
            try {
                mBigMonitor.updateStates();
                if (CPUFreq.isBigLITTLE()) {
                    mLITTLEMonitor.updateStates();
                }
            } catch (CpuStateMonitor.CpuStateMonitorException e) {
                Log.e(TAG, "Problem getting CPU states");
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            updateView(mBigMonitor, mFreqBig);
            if (CPUFreq.isBigLITTLE()) {
                updateView(mLITTLEMonitor, mFreqLITTLE);
            }
            mUpdateFrequency = false;
            scroll(500);
        }
    }

    private void updateView(CpuStateMonitor monitor, CardView card) {
        card.clearItems();

        // update the total state time
        DescriptionView totalTime = new DescriptionView();
        totalTime.setTitle(getString(R.string.uptime));
        totalTime.setSummary(sToString(monitor.getTotalStateTime() / 100L));
        card.addItem(totalTime);

        /** Get the CpuStateMonitor from the app, and iterate over all states,
         * creating a row if the duration is > 0 or otherwise marking it in
         * extraStates (missing) */
        List<String> extraStates = new ArrayList<>();
        for (CpuStateMonitor.CpuState state : monitor.getStates()) {
            if (state.duration > 0) {
                generateStateRow(monitor, state, card);
            } else {
                if (state.freq == 0) {
                    extraStates.add(getString(R.string.deep_sleep));
                } else {
                    extraStates.add(state.freq / 1000 + getString(R.string.mhz));
                }
            }
        }

        if (monitor.getStates().size() == 0) {
            card.clearItems();
            TitleView errorTitle = new TitleView();
            errorTitle.setText(getString(R.string.error_frequencies));
            card.addItem(errorTitle);
            return;
        }

        // for all the 0 duration states, add the the Unused State area
        if (extraStates.size() > 0) {
            int n = 0;
            String str = "";

            for (String s : extraStates) {
                if (n++ > 0)
                    str += ", ";
                str += s;
            }

            DescriptionView unusedText = new DescriptionView();
            unusedText.setTitle(getString(R.string.unused_frequencies));
            unusedText.setSummary(str);
            card.addItem(unusedText);
        }
    }

    /**
     * @return A nicely formatted String representing tSec seconds
     */
    private String sToString(long tSec) {
        long h = (long) Math.floor(tSec / (60 * 60));
        long m = (long) Math.floor((tSec - h * 60 * 60) / 60);
        long s = tSec % 60;
        String sDur;
        sDur = h + ":";
        if (m < 10)
            sDur += "0";
        sDur += m + ":";
        if (s < 10)
            sDur += "0";
        sDur += s;

        return sDur;
    }

    /**
     * Creates a View that correpsonds to a CPU freq state row as specified
     * by the state parameter
     */
    private void generateStateRow(CpuStateMonitor monitor, CpuStateMonitor.CpuState state,
                                  CardView frequencyCard) {
        // what percentage we've got
        float per = (float) state.duration * 100 / monitor.getTotalStateTime();

        String sFreq;
        if (state.freq == 0) {
            sFreq = getString(R.string.deep_sleep);
        } else {
            sFreq = state.freq / 1000 + getString(R.string.mhz);
        }

        // duration
        long tSec = state.duration / 100;
        String sDur = sToString(tSec);

        FrequencyTableView frequencyState = new FrequencyTableView();
        frequencyState.setFrequency(sFreq);
        frequencyState.setPercentage((int) per);
        frequencyState.setDuration(sDur);

        frequencyCard.addItem(frequencyState);
    }

    @Override
    protected void refresh() {
        super.refresh();
        mCPUUsageFragment.refresh();

        if (mCPUTemp != null) {
            mCPUTemp.setMessage(Temperature.getCPU(getActivity()));
            mCPUTemp.setColor(Temperature.getCPUColor(getActivity()));
        }
        if (mGPUTemp != null) {
            mGPUTemp.setMessage(Temperature.getGPU(getActivity()));
            mGPUTemp.setColor(Temperature.getGPUColor(getActivity()));
        }
        if (mBatteryTemp != null) {
            double temp = mBatteryRaw;
            mBatteryTemp.setColor(ContextCompat.getColor(getActivity(), temp <= 36 ?
                    R.color.green : temp <= 50 ?
                    R.color.orange : R.color.red));
            boolean useFahrenheit = Utils.useFahrenheit(getActivity());
            if (useFahrenheit) temp = Utils.celsiusToFahrenheit(temp);
            mBatteryTemp.setMessage(temp + getActivity().getString(useFahrenheit ?
                    R.string.fahrenheit : R.string.celsius));
        }
    }

    private BroadcastReceiver mBatteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mBatteryRaw = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10D;
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(mBatteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(mBatteryReceiver);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("updateFrequency", mUpdateFrequency);
    }

    public static class CPUUsageFragment extends BaseFragment {

        private List<View> mUsages = new ArrayList<>();
        private float[] mCPUUsages;
        private int[] mFreqs;

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            LinearLayout rootView = new LinearLayout(getActivity());
            rootView.setOrientation(LinearLayout.VERTICAL);

            int cpus = CPUFreq.getCpuCount();
            LinearLayout[] subViews = new LinearLayout[cpus > 1 ? CPUFreq.getCpuCount() / 2 : 1];
            for (int i = 0; i < subViews.length; i++) {
                rootView.addView(subViews[i] = new LinearLayout(getActivity()));
                LinearLayout.LayoutParams params = new LinearLayout
                        .LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT);
                params.weight = 1;
                subViews[i].setLayoutParams(params);
            }

            for (int i = 0; i < cpus; i++) {
                if (i > 0 && CPUFreq.getCpuCount() == 1) break;
                View view = inflater.inflate(R.layout.fragment_usage_view, subViews[i / 2], false);
                subViews[i / 2].addView(view);
                LinearLayout.LayoutParams params = new LinearLayout
                        .LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT);
                params.weight = 1;
                view.setLayoutParams(params);
                ((TextView) view.findViewById(R.id.usage_core_text)).setText(getString(R.string.core, i + 1));
                mUsages.add(view);
            }

            return rootView;
        }

        private Runnable mCPUUsageRun = new Runnable() {
            @Override
            public void run() {
                mCPUUsages = CPUFreq.getCpuUsage();
                if (mFreqs == null) {
                    mFreqs = new int[CPUFreq.getCpuCount()];
                }
                for (int i = 0; i < mFreqs.length; i++) {
                    mFreqs[i] = CPUFreq.getCurFreq(i);
                }
            }
        };

        public void refresh() {
            new Thread(mCPUUsageRun).start();
            if (mFreqs == null || mCPUUsages == null) return;
            for (int i = 0; i < mUsages.size(); i++) {
                View usageView = mUsages.get(i);
                TextView usageOfflineText = (TextView) usageView.findViewById(R.id.usage_offline_text);
                TextView usageLoadText = (TextView) usageView.findViewById(R.id.usage_load_text);
                TextView usageFreqText = (TextView) usageView.findViewById(R.id.usage_freq_text);
                XYGraph usageGraph = (XYGraph) usageView.findViewById(R.id.usage_graph);
                if (mFreqs[i] == 0) {
                    usageOfflineText.setVisibility(View.VISIBLE);
                    usageLoadText.setVisibility(View.GONE);
                    usageFreqText.setVisibility(View.GONE);
                } else {
                    usageOfflineText.setVisibility(View.GONE);
                    usageLoadText.setVisibility(View.VISIBLE);
                    usageFreqText.setVisibility(View.VISIBLE);
                    usageFreqText.setText(Utils.strFormat("%d" + getString(R.string.mhz), mFreqs[i] / 1000));
                    usageLoadText.setText(Utils.strFormat("%d%%", Math.round(mCPUUsages[i + 1])));
                    usageGraph.addPercentage(Math.round(mCPUUsages[i + 1]));
                }
            }
        }

        @Override
        protected boolean retainInstance() {
            return false;
        }
    }

}
