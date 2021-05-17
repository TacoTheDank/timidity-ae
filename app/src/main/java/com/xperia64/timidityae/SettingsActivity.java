/*******************************************************************************
 * Copyright (C) 2017 xperia64 <xperiancedapps@gmail.com>
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package com.xperia64.timidityae;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.xperia64.timidityae.gui.dialogs.FileBrowserDialog.FileBrowserDialogListener;
import com.xperia64.timidityae.gui.dialogs.SoundfontDialog.SoundfontDialogListener;
import com.xperia64.timidityae.gui.fragments.preferences.SettingsDisplayFragment;
import com.xperia64.timidityae.gui.fragments.preferences.SettingsFragment;
import com.xperia64.timidityae.gui.fragments.preferences.SettingsTimidityFragment;
import com.xperia64.timidityae.util.Constants;
import com.xperia64.timidityae.util.DocumentFileUtils;
import com.xperia64.timidityae.util.ObjectSerializer;
import com.xperia64.timidityae.util.SettingsStorage;

import java.io.IOException;
import java.util.ArrayList;

public class SettingsActivity extends AppCompatActivity
        implements FileBrowserDialogListener, SoundfontDialogListener {

    private final static int prefDisplay = R.xml.preferences_display;
    private final static int prefTimidity = R.xml.preferences_timidity;
    public ArrayList<String> tmpSounds;
    public boolean needRestart = false;
    public boolean needUpdateSf = false;
    public SharedPreferences prefs;
    public EditTextPreference tmpItemEdit;
    public boolean loadDispSettings = false;

    private static int getTitleOfPage(int preferences) {
        switch (preferences) {
            case prefDisplay:
                return R.string.sett_ds;
            case prefTimidity:
                return R.string.sett_plus;
            default:
                return R.string.action_settings;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(SettingsStorage.theme == 1 ? R.style.AppLightTheme : R.style.AppDarkTheme);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);
        final Toolbar toolbar = findViewById(R.id.settings_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Display the fragment as the main content.
        prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.settings_container, new SettingsFragment())
                .commit();

        loadDispSettings = getIntent().getBooleanExtra("returnToDisp", false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // Store the soundfonts
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
            return;
        }
        try {
            prefs.edit().putString("tplusSoundfonts", ObjectSerializer.serialize(tmpSounds)).commit();
        } catch (IOException e) {
            e.printStackTrace();
        }
        SettingsStorage.reloadSettings(this);
        if (needUpdateSf) {
            SettingsStorage.writeCfg(this,
                    SettingsStorage.dataFolder + "/timidity/timidity.cfg", tmpSounds); // TODO
        }

        if (needRestart) {
            final Intent restartIntent = new Intent();
            restartIntent.setAction(Constants.msrv_rec);
            restartIntent.putExtra(Constants.msrv_cmd, Constants.msrv_cmd_reload_libs);
            sendBroadcast(restartIntent);
        }

        final Intent returnIntent = new Intent();
        setResult(3, returnIntent);
        this.finish();
    }

    @Override
    public void setItem(String path, int type) {
        if (path != null) {
            if (!path.isEmpty()) {
                switch (type) {
                    case 3:
                        prefs.edit().putString("defaultPath", path).commit();
                        tmpItemEdit.setText(path);
                        SettingsStorage.homeFolder = path;
                        //((BaseAdapter) tmpItemScreen.getRootAdapter()).notifyDataSetChanged();
                        break;
                    case 4:
                        prefs.edit().putString("dataDir", path).commit();
                        tmpItemEdit.setText(path);
                        //((BaseAdapter) tmpItemScreen.notifyChanged().getRootAdapter()).notifyDataSetChanged();
                        break;
                    case 5:
                        // soundfont fun
                        break;
                }
                return;
            }
        }
        Toast.makeText(this, getString(R.string.invalidfold), Toast.LENGTH_SHORT).show();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Check which request we're responding to
        if (requestCode == 42) {
            if (resultCode == RESULT_OK) {
                Uri treeUri = data.getData();
                getContentResolver().takePersistableUriPermission(treeUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                DocumentFileUtils.docFileDevice = treeUri;
            } else {
                DocumentFileUtils.docFileDevice = null;
            }
        }
    }

    @Override
    public void write() {
    }

    @Override
    public void ignore() {
    }

    public void openSettingsScreen(final int screen) {
        final PreferenceFragmentCompat fragment = getSettingsScreen(screen);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.settings_container, fragment)
                .addToBackStack(getString(getTitleOfPage(screen)))
                .commit();
    }

    private PreferenceFragmentCompat getSettingsScreen(final int screen) {
        PreferenceFragmentCompat prefFragment = null;

        switch (screen) {
            case prefDisplay:
                prefFragment = new SettingsDisplayFragment();
                break;
            case prefTimidity:
                prefFragment = new SettingsTimidityFragment();
                break;
        }
        return prefFragment;
    }

    @Override
    public void writeSoundfonts(ArrayList<String> l) {
        if (l.size() == tmpSounds.size()) {
            for (int i = 0; i < l.size(); i++) {
                if (!l.get(i).equals(tmpSounds.get(i))) {
                    needRestart = true;
                    needUpdateSf = true;
                    break;
                }
            }
        } else {
            needRestart = true;
            needUpdateSf = true;
        }
        if (needUpdateSf) {
            tmpSounds.clear();
            tmpSounds.addAll(l);
        }
    }
}
