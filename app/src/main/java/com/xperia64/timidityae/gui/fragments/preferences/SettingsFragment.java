/*******************************************************************************
 * Copyright (C) 2017 xperia64 <xperiancedapps@gmail.com>
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package com.xperia64.timidityae.gui.fragments.preferences;

import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.xperia64.timidityae.R;
import com.xperia64.timidityae.SettingsActivity;
import com.xperia64.timidityae.gui.dialogs.SoxEffectsDialog;
import com.xperia64.timidityae.util.ObjectSerializer;

import java.io.IOException;
import java.util.ArrayList;

public class SettingsFragment extends PreferenceFragmentCompat {

    private SettingsActivity activity;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences);

        activity = (SettingsActivity) getActivity();

        final Preference displayPref = findPreference("dsKey");
        final Preference timidityPref = findPreference("tplusKey");
        final Preference soxPref = findPreference("soxKey");

        displayPref.setOnPreferenceClickListener(preference -> {
            activity.openSettingsScreen(R.xml.preferences_display);
            return true;
        });

        timidityPref.setOnPreferenceClickListener(preference -> {
            activity.openSettingsScreen(R.xml.preferences_timidity);
            return true;
        });

        soxPref.setOnPreferenceClickListener(preference -> {
            //activity.openSettingsScreen(R.xml.preferences_sox);
            new SoxEffectsDialog().create(activity, activity.getLayoutInflater());
            return true;
        });

        try {
            activity.tmpSounds = (ArrayList<String>) ObjectSerializer.deserialize(
                    activity.prefs.getString("tplusSoundfonts",
                            ObjectSerializer.serialize(new ArrayList<String>())));
            System.out.println("We have tmpSounds of size: " + activity.tmpSounds.size());
            for (int i = 0; i < activity.tmpSounds.size(); i++) {
                if (activity.tmpSounds.get(i) == null)
                    activity.tmpSounds.remove(i);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (activity.tmpSounds == null)
            activity.tmpSounds = new ArrayList<>();

        if (activity.loadDispSettings) {
            activity.loadDispSettings = false;
            activity.openSettingsScreen(R.xml.preferences_display);
        }
    }
}
