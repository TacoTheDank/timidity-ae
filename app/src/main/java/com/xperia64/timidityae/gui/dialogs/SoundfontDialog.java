/*******************************************************************************
 * Copyright (C) 2017 xperia64 <xperiancedapps@gmail.com>
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package com.xperia64.timidityae.gui.dialogs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.xperia64.timidityae.JNIHandler;
import com.xperia64.timidityae.R;
import com.xperia64.timidityae.gui.dialogs.FileBrowserDialog.FileBrowserDialogListener;
import com.xperia64.timidityae.util.Globals;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

public class SoundfontDialog implements OnItemLongClickListener, FileBrowserDialogListener,
        SoundfontArrayAdapter.SoundfontArrayAdapterListener {

    private ArrayList<String> sfList;
    private ArrayList<String> tmpList;
    private Context context;
    private ListView mList;
    private SoundfontDialogListener mCallback;

    public void create(ArrayList<String> currList, SoundfontDialogListener sl, final Activity c,
                       final LayoutInflater f, final String path) {
        sfList = new ArrayList<>(currList.size());
        sfList.addAll(currList);
        context = c;
        mCallback = sl;
        LinearLayout mLayout = (LinearLayout) f.inflate(R.layout.list, null);
        mList = mLayout.findViewById(android.R.id.list);

        SoundfontArrayAdapter fileList = new SoundfontArrayAdapter(this, context, sfList);
        mList.setAdapter(fileList);
        mList.setOnItemLongClickListener(this);

        new AlertDialog.Builder(context)
                .setView(mLayout)
                .setCancelable(false)
                .setTitle(R.string.sf_man)
                .setPositiveButton(R.string.done, (dialog, which) ->
                        mCallback.writeSoundfonts(sfList))
                .setNeutralButton(R.string.addcon, (dialog, which) -> {
                    tmpList = new ArrayList<>();
                    new FileBrowserDialog().create(
                            0, Globals.fontFiles, SoundfontDialog.this,
                            c, f, false, path, c.getString(R.string.fb_add));
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) ->
                        dialog.dismiss())
                .create()
                .show();
    }

    @Override
    public void setItem(final String path, int type) {
        if (path.toLowerCase(Locale.US).endsWith(".sfark")
                || path.toLowerCase(Locale.US).endsWith(".sfark.exe")) {

            new AlertDialog.Builder(context)
                    .setTitle("Extract sfArk?")
                    .setCancelable(false)
                    .setMessage(String.format("%s must be extracted. Extract to %s?",
                            path.substring(path.lastIndexOf('/') + 1),
                            path.substring(
                                    path.lastIndexOf('/') + 1,
                                    path.lastIndexOf('.')) + ".sf2"
                            )
                    )
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        @SuppressLint("StaticFieldLeak")
                        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
                            ProgressDialog pd;

                            @Override
                            protected void onPreExecute() {
                                pd = new ProgressDialog(context);
                                pd.setTitle(R.string.extract);
                                pd.setMessage("Extracting");
                                pd.setCancelable(false);
                                pd.setIndeterminate(true);
                                pd.show();
                            }

                            @Override
                            protected Void doInBackground(Void... arg0) {
                                JNIHandler.decompressSFArk(
                                        path, path.substring(
                                                path.lastIndexOf('/') + 1,
                                                path.lastIndexOf('.')) + ".sf2"
                                );
                                return null;
                            }

                            @Override
                            protected void onPostExecute(Void result) {
                                if (pd != null)
                                    pd.dismiss();
                                if (new File(path.substring(0, path.lastIndexOf('.')) + ".sf2").exists()) {
                                    tmpList.add(path.substring(0, path.lastIndexOf('.')) + ".sf2");

                                    new AlertDialog.Builder(context)
                                            .setTitle("Delete sfArk?")
                                            .setCancelable(false)
                                            .setMessage(String.format("Delete %s?", path.substring(path.lastIndexOf('/') + 1)))
                                            .setNegativeButton(android.R.string.cancel, null)
                                            .setPositiveButton(android.R.string.ok, (dialog, which) ->
                                                    new File(path).delete());
                                } else
                                    Toast.makeText(context, "Error extracting sfArk", Toast.LENGTH_SHORT).show();
                                // b.setEnabled(true);
                            }

                        };
                        task.execute((Void[]) null);
                    })
                    .show();
        } else {
            tmpList.add(path);
        }
    }

    @Override
    public void write() {
        sfList.addAll(tmpList);

        SoundfontArrayAdapter fileList = new SoundfontArrayAdapter(this, context, sfList);
        mList.setAdapter(fileList);
        mList.setOnItemLongClickListener(this);
    }

    @Override
    public void ignore() {
        tmpList = null;
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> arg0, View arg1, final int arg2, long arg3) {
        new AlertDialog.Builder(context)
                .setIcon(R.drawable.ic_launcher)
                .setTitle(R.string.sf_rem)
                .setCancelable(false)
                .setNegativeButton(android.R.string.cancel, (dialog, which) ->
                        dialog.dismiss())
                .setPositiveButton(R.string.sf_rem2, (dialog, which) ->
                        onItemLongClickPositive(arg2))
                .show();
        return true;
    }

    private void onItemLongClickPositive(final int arg2) {
        new AlertDialog.Builder(context)
                .setIcon(R.drawable.ic_launcher)
                .setTitle(String.format(context.getString(R.string.sf_com),
                        sfList.get(arg2).substring(sfList.get(arg2).lastIndexOf('/') + 1)))
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    sfList.remove(arg2);
                    SoundfontArrayAdapter fileList
                            = new SoundfontArrayAdapter(SoundfontDialog.this, context, sfList);
                    mList.setAdapter(fileList);
                    mList.setOnItemLongClickListener(SoundfontDialog.this);
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) ->
                        dialog.dismiss())
                .show();
    }

    @Override
    public void setSFEnabled(int position, boolean enable) {
        String pos = sfList.get(position);
        if (pos.startsWith("#") && enable) {
            pos = pos.substring(1);
            sfList.set(position, pos);
        } else if (!pos.startsWith("#") && !enable) {
            sfList.set(position, "#" + pos);
        }
    }

    public interface SoundfontDialogListener {
        void writeSoundfonts(ArrayList<String> l);
    }
}
