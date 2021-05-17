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
import android.content.DialogInterface;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.xperia64.timidityae.R;
import com.xperia64.timidityae.util.FileComparator;
import com.xperia64.timidityae.util.Globals;
import com.xperia64.timidityae.util.SettingsStorage;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class FileBrowserDialog implements OnItemClickListener {

    private ListView fbdList;
    private String currPath;
    private ArrayList<String> fname;
    private ArrayList<String> path;
    private String extensions;
    private Activity context;
    private int type;
    private String msg;
    private FileBrowserDialogListener onSelectedCallback;
    private AlertDialog alertDialog;
    private boolean closeImmediately; // Should the dialog be closed immediately after selecting the file?

    @SuppressLint("InflateParams")
    public void create(int type, String extensions, FileBrowserDialogListener onSelectedCallback,
                       Activity context, LayoutInflater layoutInflater, boolean closeImmediately,
                       String path, String msg) {
        this.onSelectedCallback = onSelectedCallback;
        this.msg = msg;
        this.context = context;
        this.extensions = extensions;
        this.type = type; // A command for later reference. 0 is files, otherwise
        // folders
        this.closeImmediately = closeImmediately; // Close immediately after selecting a file/folder

        LinearLayout fbdLayout = (LinearLayout) layoutInflater.inflate(R.layout.list, null);
        fbdList = fbdLayout.findViewById(android.R.id.list);
        fbdList.setOnItemClickListener(this);

        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(context)
                .setView(fbdLayout)
                .setCancelable(false)
                .setTitle(this.context.getString(type == 0 ? R.string.fb_chfi : R.string.fb_chfo));

        if (!closeImmediately) {
            alertDialog.setPositiveButton(R.string.done, (dialog, which) ->
                    FileBrowserDialog.this.onSelectedCallback.write());
        }
        alertDialog.setNegativeButton(android.R.string.cancel, (dialog, which) ->
                FileBrowserDialog.this.onSelectedCallback.ignore());
        if (type != 0) {
            alertDialog.setNeutralButton(R.string.fb_fold, null);
        }

        if (path == null)
            path = Environment.getExternalStorageDirectory().getAbsolutePath();
        else if (!new File(path).exists())
            path = Environment.getExternalStorageDirectory().getAbsolutePath();
        getDir(path);
        this.alertDialog = alertDialog.create();
        this.alertDialog.show();
        final Button theButton = this.alertDialog.getButton(DialogInterface.BUTTON_NEUTRAL);
        if (theButton != null)
            theButton.setOnClickListener(v -> {
                FileBrowserDialog.this.onSelectedCallback.setItem(currPath, FileBrowserDialog.this.type);
                if (FileBrowserDialog.this.closeImmediately) {
                    this.alertDialog.dismiss();
                }
            });
    }

    private void getDir(String dirPath) {
        currPath = dirPath;
        fname = new ArrayList<>();
        path = new ArrayList<>();
        if (currPath != null) {
            File f = new File(currPath);
            if (f.exists()) {
                File[] files = f.listFiles();
                if (files.length > 0) {
                    Arrays.sort(files, new FileComparator());
                    if (!currPath.matches(Globals.repeatedSeparatorString)
                            && !(currPath.equals(File.separator + "storage" + File.separator)
                            && !new File(File.separator).canRead())
                    ) {
                        fname.add(Globals.parentString);
                        // Thank you Marshmallow.
                        // Disallowing access to /storage/emulated has now prevent billions of hacking attempts daily.
                        if (new File(f.getParent()).canRead()) {
                            path.add(f.getParent() + File.separator);
                        } else if (new File(File.separator).canRead()) { // N seems to block reading /
                            path.add(File.separator);
                        } else {
                            path.add(File.separator + "storage" + File.separator);
                        }
                    }
                    for (File file : files) {
                        if (!file.getName().startsWith(".") || SettingsStorage.showHiddenFiles) {
                            if (file.isFile() && type == 0) {
                                String extension = Globals.getFileExtension(file);
                                if (extension != null) {
                                    if (extensions.contains("*" + extension + "*")) {
                                        path.add(file.getAbsolutePath());
                                        fname.add(file.getName());
                                    }
                                } else if (file.getName().endsWith(File.separator)) {
                                    path.add(file.getAbsolutePath() + File.separator);
                                    fname.add(file.getName() + File.separator);
                                }
                            } else if (file.isDirectory()) {
                                path.add(file.getAbsolutePath() + File.separator);
                                fname.add(file.getName() + File.separator);
                            }
                        }
                    }
                } else {
                    if (!currPath.matches(Globals.repeatedSeparatorString)
                            && !(currPath.equals(File.separator + "storage" + File.separator)
                            && !new File(File.separator).canRead())
                    ) {
                        fname.add(Globals.parentString);
                        // Thank you Marshmallow.
                        // Disallowing access to /storage/emulated has now prevent billions of hacking attempts daily.
                        if (new File(f.getParent()).canRead()) {
                            path.add(f.getParent() + File.separator);
                        } else if (new File(File.separator).canRead()) { // N seems to block reading /
                            path.add(File.separator);
                        } else {
                            path.add(File.separator + "storage" + File.separator);
                        }
                    }
                }

                ArrayAdapter<String> fileList = new ArrayAdapter<>(context, R.layout.row, fname);
                fbdList.setFastScrollEnabled(true);
                fbdList.setAdapter(fileList);
            }
        }
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        final String emulated0Str = "/storage/emulated/0";
        final String emulatedLegacyStr = "/storage/emulated/legacy";
        final String selfPrimaryStr = "/storage/self/primary";
        final File emulated0 = new File(emulated0Str);
        final File emulatedLegacy = new File(emulatedLegacyStr);
        final File selfPrimary = new File(selfPrimaryStr);
        File file = new File(path.get(arg2));
        if (file.isDirectory()) {
            if (file.canRead()) {
                getDir(path.get(arg2));
            } else if (file.getAbsolutePath().equals("/storage/emulated")) {
                if (emulated0.exists() && emulated0.canRead()) {
                    getDir(emulated0Str);
                } else if (emulatedLegacy.exists() && emulatedLegacy.canRead()) {
                    getDir(emulatedLegacyStr);
                } else if (selfPrimary.exists() && selfPrimary.canRead()) {
                    getDir(selfPrimaryStr);
                }
            } else {
                new AlertDialog.Builder(context)
                        .setIcon(R.drawable.ic_launcher)
                        .setTitle(String.format("[%1$s] %2$s", file.getName(), R.string.fb_cread))
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            }
        } else {
            if (file.canRead()) {
                Toast.makeText(context,
                        String.format("%1$s '%2$s'", msg, fname.get(arg2)), Toast.LENGTH_SHORT)
                        .show();
                onSelectedCallback.setItem(file.getAbsolutePath(), type);
                if (closeImmediately)
                    alertDialog.dismiss();
            }
        }
    }

    public interface FileBrowserDialogListener {
        void setItem(String path, int type);

        void write();

        void ignore();
    }
}
