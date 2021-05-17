/*******************************************************************************
 * Copyright (C) 2017 xperia64 <xperiancedapps@gmail.com>
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package com.xperia64.timidityae.gui.fragments.preferences;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.media.AudioTrack;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.util.SparseIntArray;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.xperia64.timidityae.R;
import com.xperia64.timidityae.SettingsActivity;
import com.xperia64.timidityae.gui.dialogs.FileBrowserDialog;
import com.xperia64.timidityae.gui.dialogs.SoundfontDialog;
import com.xperia64.timidityae.util.Globals;
import com.xperia64.timidityae.util.SettingsStorage;

/**
 * Created by xperia64 on 1/2/17.
 */

public class SettingsTimidityFragment extends PreferenceFragmentCompat {
    SettingsActivity activity;

    // TiMidity++ Settings
    private SwitchPreferenceCompat manTcfg; // Use manual timidity.cfg?
    private Preference sfPref; // Open soundfont manager
    private SwitchPreferenceCompat psilence; // Preserve silence and beginning of midi
    private SwitchPreferenceCompat unload; // Unload instruments
    private ListPreference resampMode; // Default resampling algorithm
    private ListPreference stereoMode; // Synth Mono, Downmixed Mono, or Stereo
    private ListPreference rates; // Audio rates
    private EditTextPreference volume; // Amplification. Default 70, max 800
    private EditTextPreference bufferSize; // Buffer size, in something. I use 192000 by default.
    private ListPreference verbosity;

    // Timidity AE Data Settings
    private Preference reinstallSoundfont;
    private Preference dataFoldPreference;
    private EditTextPreference manDataFolder;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_timidity);

        activity = (SettingsActivity) getActivity();

        manTcfg = findPreference("manualConfig");
        sfPref = findPreference("sfConfig");
        psilence = findPreference("tplusSilKey");
        unload = findPreference("tplusUnload");
        resampMode = findPreference("tplusResamp");
        stereoMode = findPreference("sdlChanValue");
        rates = findPreference("tplusRate");
        volume = findPreference("tplusVol");
        bufferSize = findPreference("tplusBuff");
        verbosity = findPreference("timidityVerbosity");

        reinstallSoundfont = findPreference("reSF");
        dataFoldPreference = findPreference("defData");
        manDataFolder = findPreference("dataDir");

        SettingsStorage.updateBuffers(SettingsStorage.updateRates());
        int[] values = SettingsStorage.updateRates();
        if (values != null) {
            CharSequence[] hz = new CharSequence[values.length];
            CharSequence[] hzItems = new CharSequence[values.length];
            for (int i = 0; i < values.length; i++) {
                hz[i] = values[i] + "Hz";
                hzItems[i] = Integer.toString(values[i]);
            }
            rates.setEntries(hz);
            rates.setEntryValues(hzItems);
            rates.setDefaultValue(Integer.toString(AudioTrack.getNativeOutputSampleRate(AudioTrack.MODE_STREAM)));
            rates.setValue(activity.prefs.getString("tplusRate",
                    Integer.toString(AudioTrack.getNativeOutputSampleRate(AudioTrack.MODE_STREAM))));
        }

        manTcfg.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference arg0, Object arg1) {
                boolean manual = (Boolean) arg1;
                sfPref.setEnabled(!manual);
                activity.needRestart = true;
                activity.needUpdateSf = !manual;
                if (!manual) {
                    activity.needUpdateSf = true;
                }
                return true;
            }
        });

        sfPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new SoundfontDialog().create(activity.tmpSounds, activity, activity,
                        activity.getLayoutInflater(), activity.prefs.getString(
                                "defaultPath",
                                Environment.getExternalStorageDirectory().getPath()
                        ));
                return true;
            }
        });

        psilence.setOnPreferenceChangeListener((arg0, arg1) -> {
            activity.needRestart = true;
            return true;
        });

        unload.setOnPreferenceChangeListener((arg0, arg1) -> {
            activity.needRestart = true;
            return true;
        });

        resampMode.setOnPreferenceChangeListener((preference, newValue) -> {
            if (!resampMode.getValue().equals(newValue)) {
                activity.needRestart = true;
            }
            return true;
        });

        stereoMode.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (!stereoMode.getValue().equals(newValue)) {
                    activity.needRestart = true;
                    String stereo = (String) newValue;
                    String sixteen = "16"; // s.bitMode.getValue();
                    boolean sb = stereo == null || stereo.equals("2");
                    boolean sxb = sixteen.equals("16");
                    SparseIntArray mmm = SettingsStorage.validBuffers(
                            SettingsStorage.validRates(sb, sxb), sb, sxb);
                    if (mmm != null) {
                        int minBuff = mmm.get(Integer.parseInt(rates.getValue()));

                        int buff = Integer.parseInt(bufferSize.getText());
                        if (buff < minBuff) {
                            activity.prefs.edit().putString("tplusBuff", Integer.toString(minBuff)).commit();
                            bufferSize.setText(Integer.toString(minBuff));
                            Toast.makeText(activity,
                                    getResources().getString(R.string.invalidbuff),
                                    Toast.LENGTH_SHORT).show();
                            //((BaseAdapter) SettingsTimidityFragment.this.getPreferenceScreen()
                            // .getRootAdapter()).notifyDataSetChanged();
                            //((BaseAdapter) SettingsTimidityFragment.this.getPreferenceScreen()
                            // .getRootAdapter()).notifyDataSetInvalidated();
                        }
                    }
                }
                return true;
            }
        });

        rates.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (!rates.getValue().equals(newValue)) {
                    activity.needRestart = true;
                    String stereo = stereoMode.getValue();
                    String sixteen = "16";// s.bitMode.getValue();
                    boolean sb = stereo == null || stereo.equals("2");
                    boolean sxb = sixteen.equals("16");
                    SparseIntArray mmm = SettingsStorage.validBuffers(
                            SettingsStorage.validRates(sb, sxb), sb, sxb);
                    if (mmm != null) {
                        int minBuff = mmm.get(Integer.parseInt((String) newValue));

                        int buff = Integer.parseInt(bufferSize.getText());
                        if (buff < minBuff) {
                            activity.prefs.edit().putString("tplusBuff", Integer.toString(minBuff)).commit();
                            bufferSize.setText(Integer.toString(minBuff));
                            Toast.makeText(activity,
                                    getResources().getString(R.string.invalidbuff),
                                    Toast.LENGTH_SHORT).show();
                            //((BaseAdapter) SettingsTimidityFragment.this.getPreferenceScreen()
                            // .getRootAdapter()).notifyDataSetChanged();
                            //((BaseAdapter) SettingsTimidityFragment.this.getPreferenceScreen()
                            // .getRootAdapter()).notifyDataSetInvalidated();
                        }
                    }
                }
                return true;
            }
        });

        volume.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER));
        volume.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (!volume.getText().equals(newValue)) {
                    activity.needRestart = true;
                    String txt = (String) newValue;
                    if (txt != null) {
                        if (!txt.isEmpty()) {
                            int volume = Integer.parseInt(txt);
                            if (volume < 0 || volume > 800) {
                                Toast.makeText(activity,
                                        "Invalid volume. Must be between 0 and 800",
                                        Toast.LENGTH_SHORT).show();
                                return false;
                            }
                            return true;
                        }
                    }
                    return false;
                }
                return true;
            }
        });

        bufferSize.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER));
        bufferSize.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, final Object newValue) {
                if (!bufferSize.getText().equals(newValue)) {
                    activity.needRestart = true;
                    String txt = (String) newValue;
                    if (txt != null) {
                        if (!txt.isEmpty()) {
                            String stereo = stereoMode.getValue();
                            String sixteen = "16"; // s.bitMode.getValue();
                            boolean sb = stereo == null || stereo.equals("2");
                            boolean sxb = sixteen.equals("16");
                            SparseIntArray mmm = SettingsStorage.validBuffers(
                                    SettingsStorage.validRates(sb, sxb), sb, sxb);
                            if (mmm != null) {
                                int minBuff = mmm.get(Integer.parseInt(rates.getValue()));

                                int buff = Integer.parseInt(txt);
                                if (buff < minBuff) {
                                    activity.prefs.edit().putString("tplusBuff", Integer.toString(minBuff)).commit();
                                    ((EditTextPreference) preference).setText(Integer.toString(minBuff));
                                    Toast.makeText(activity,
                                            getResources().getString(R.string.invalidbuff),
                                            Toast.LENGTH_SHORT).show();
                                    //((BaseAdapter) SettingsTimidityFragment.this.getPreferenceScreen()
                                    // .getRootAdapter()).notifyDataSetChanged();
                                    //((BaseAdapter) SettingsTimidityFragment.this.getPreferenceScreen()
                                    // .getRootAdapter()).notifyDataSetInvalidated();
                                    return false;
                                }
                            }
                            return true;
                        }
                    }
                    return false;
                }
                return true;
            }

        });

        verbosity.setOnPreferenceChangeListener((arg0, arg1) -> {
            activity.needRestart = true;
            return true;
        });

        reinstallSoundfont.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference arg0) {
                new AlertDialog.Builder(activity)
                        .setTitle(getResources().getString(R.string.sett_resf_q))
                        .setMessage(getResources().getString(R.string.sett_resf_q_sum))
                        .setCancelable(true)
                        .setPositiveButton(android.R.string.ok, (dialog, buttonId) -> {
                            @SuppressLint("StaticFieldLeak")
                            AsyncTask<Void, Void, Integer> task = new AsyncTask<Void, Void, Integer>() {
                                ProgressDialog pd;

                                @Override
                                protected void onPreExecute() {
                                    pd = new ProgressDialog(activity);
                                    pd.setTitle(getResources().getString(R.string.extract));
                                    pd.setMessage(getResources().getString(R.string.extract_sum));
                                    pd.setCancelable(false);
                                    pd.setIndeterminate(true);
                                    pd.show();
                                }

                                @Override
                                protected Integer doInBackground(Void... arg01) {
                                    return Globals.extract8Rock(activity);
                                }

                                @Override
                                protected void onPostExecute(Integer result) {
                                    if (pd != null) {
                                        pd.dismiss();
                                        if (result != 777) {
                                            Toast.makeText(activity,
                                                    getResources().getString(R.string.sett_resf_err),
                                                    Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(activity,
                                                    getResources().getString(R.string.extract_def),
                                                    Toast.LENGTH_LONG).show();
                                        }
                                    }
                                }
                            };
                            task.execute((Void[]) null);
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .create()
                        .show();
                return true;
            }
        });

        dataFoldPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                activity.needRestart = true;
                activity.tmpItemEdit = manDataFolder;
                new FileBrowserDialog().create(4, null, activity, activity,
                        activity.getLayoutInflater(), true,
                        activity.prefs.getString("dataDir",
                                Environment.getExternalStorageDirectory().getAbsolutePath()
                        ), getResources().getString(R.string.fb_add));
                return true;
            }
        });

        manDataFolder.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_TEXT));
        manDataFolder.setOnPreferenceChangeListener((preference, newValue) -> {
            if (!manDataFolder.getText().equals(newValue)) {
                activity.needRestart = true;
            }
            return true;
        });
    }
}
