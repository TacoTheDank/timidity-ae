/*******************************************************************************
 * Copyright (C) 2017 xperia64 <xperiancedapps@gmail.com>
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package com.xperia64.timidityae.gui.fragments.preferences;

import android.content.Intent;
import android.content.UriPermission;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;

import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.xperia64.timidityae.R;
import com.xperia64.timidityae.SettingsActivity;
import com.xperia64.timidityae.gui.dialogs.FileBrowserDialog;
import com.xperia64.timidityae.util.ObjectSerializer;
import com.xperia64.timidityae.util.SettingsStorage;

import java.io.IOException;
import java.util.List;

public class SettingsDisplayFragment extends PreferenceFragmentCompat {

    private SettingsActivity activity;

    private EditTextPreference manHomeFolderPref; // Enter the default folder manually

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_display);

        activity = (SettingsActivity) getActivity();

        final ListPreference themePref = findPreference("fbTheme"); // Theme selection
        final Preference defaultFolderPref = findPreference("defFold"); // Browse for the default folder
        manHomeFolderPref = findPreference("defaultPath");
        final Preference extStoragePref = findPreference("lolWrite"); // Select external storage to write to (API 21+)
        final SwitchPreferenceCompat askStorageAccessPref = findPreference("shouldLolNag");

        themePref.setOnPreferenceChangeListener((preference, newValue) -> {
            SettingsStorage.theme = Integer.parseInt((String) newValue);
            // Just to be safe
            try {
                activity.prefs.edit()
                        .putString("tplusSoundfonts", ObjectSerializer.serialize(activity.tmpSounds))
                        .commit();
            } catch (IOException e) {
                e.printStackTrace();
            }
            final Intent intent = activity.getIntent();
            intent.putExtra("returnToDisp", true);
            activity.finish();
            startActivity(intent);
            return true;
        });

        defaultFolderPref.setOnPreferenceClickListener(preference -> {
            activity.tmpItemEdit = manHomeFolderPref;
            new FileBrowserDialog().create(3, null, activity, activity,
                    activity.getLayoutInflater(), true,
                    activity.prefs.getString("defaultPath",
                            Environment.getExternalStorageDirectory().getAbsolutePath()
                    ), getResources().getString(R.string.fb_add));
            return true;
        });

        // Show these preferences if Android version is Lollipop 5.0 (API 21) or above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            extStoragePref.setVisible(true);
            extStoragePref.setOnPreferenceClickListener(preference -> {
                // dialog code here
                final List<UriPermission> permissions =
                        activity.getContentResolver().getPersistedUriPermissions();
                if (!permissions.isEmpty()) {
                    for (UriPermission p : permissions) {
                        activity.getContentResolver()
                                .releasePersistableUriPermission(p.getUri(),
                                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    }
                }
                final Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                activity.startActivityForResult(intent, 42);
                return true;
            });

            askStorageAccessPref.setVisible(true);
        }
    }
}
