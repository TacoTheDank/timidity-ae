/*******************************************************************************
 * Copyright (C) 2017 xperia64 <xperiancedapps@gmail.com>
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package com.xperia64.timidityae.util;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.text.InputFilter;
import android.text.InputType;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.xperia64.timidityae.JNIHandler;
import com.xperia64.timidityae.R;
import com.xperia64.timidityae.TimidityActivity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;

public class WavSaver implements TimidityActivity.SpecialAction {
    private final Activity context;
    private final String currSongName;
    private final boolean playingExport; // export while playing
    private boolean localfinished;
    private AlertDialog alertDialog;

    public WavSaver(Activity context, String currSongName, boolean playingExport) {
        this.context = context;
        this.currSongName = currSongName;
        this.playingExport = playingExport;
    }

    public void dynExport() {
        localfinished = false;
        if (Globals.isMidi(currSongName) && (JNIHandler.isActive() || !playingExport)) {
            final EditText input = new EditText(context);
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            input.setFilters(new InputFilter[]{Globals.fileNameInputFilter});

            final AlertDialog.Builder alertDialog = new AlertDialog.Builder(context)
                    .setTitle(R.string.dynex_alert1)
                    .setMessage(R.string.dynex_alert1_msg)
                    .setView(input)
                    .setPositiveButton(android.R.string.ok, (dialog, whichButton) -> {
                        String value = input.getText().toString();
                        if (!value.toLowerCase(Locale.US).endsWith(".wav"))
                            value += ".wav";
                        String parent = currSongName.substring(0, currSongName.lastIndexOf('/') + 1);
                        boolean alreadyExists = new File(parent + value).exists();
                        boolean normalWrite = true;
                        String needRename = null;
                        String probablyTheRoot = "";
                        String probablyTheDirectory = "";
                        try {
                            new FileOutputStream(parent + value, true).close();
                        } catch (FileNotFoundException e) {
                            normalWrite = false;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        if (normalWrite && !alreadyExists)
                            new File(parent + value).delete();

                        if (normalWrite && new File(parent).canWrite()) {
                            value = parent + value;
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                                && DocumentFileUtils.docFileDevice != null
                        ) {
                            String[] tmp = DocumentFileUtils.getExternalFilePaths(context, parent);
                            probablyTheDirectory = tmp[0];
                            probablyTheRoot = tmp[1];

                            if (probablyTheDirectory.length() > 1) {
                                needRename = parent.substring(parent.indexOf(probablyTheRoot)
                                        + probablyTheRoot.length()) + value;
                                value = probablyTheDirectory + '/' + value;
                            } else {
                                value = Environment.getExternalStorageDirectory().getAbsolutePath() + '/' + value;
                            }
                        } else {
                            value = Environment.getExternalStorageDirectory().getAbsolutePath() + '/' + value;
                        }
                        final String finalval = value;
                        final boolean canWrite = normalWrite;
                        final String needToRename = needRename;
                        final String probRoot = probablyTheRoot;
                        if (new File(finalval).exists()
                                || new File(probRoot + needRename).exists() && needToRename != null
                        ) {
                            new AlertDialog.Builder(context)
                                    .setTitle(R.string.warning)
                                    .setMessage(R.string.dynex_alert2_msg)
                                    .setCancelable(false)
                                    .setPositiveButton(android.R.string.ok, (dialog1, buttonId) -> {
                                        if (!canWrite && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                            if (needToRename != null) {
                                                DocumentFileUtils.tryToDeleteFile(
                                                        context, probRoot + needToRename);
                                            }
                                            DocumentFileUtils.tryToDeleteFile(context, finalval);
                                        } else {
                                            new File(finalval).delete();
                                        }
                                        saveWavPart2(finalval, needToRename);
                                    })
                                    .setNegativeButton(android.R.string.no, null)
                                    .create()
                                    .show();
                        } else {
                            saveWavPart2(finalval, needToRename);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null);

            this.alertDialog = alertDialog.show();

        }
    }

    private void saveWavPart2(final String finalval, final String needToRename) {
        Intent new_intent = new Intent();
        new_intent.setAction(Constants.msrv_rec);
        new_intent.putExtra(Constants.msrv_cmd,
                playingExport ? Constants.msrv_cmd_write_curr : Constants.msrv_cmd_write_new);
        if (!playingExport) {
            new_intent.putExtra(Constants.msrv_infile, currSongName);
        }
        new_intent.putExtra(Constants.msrv_outfile, finalval);
        context.sendBroadcast(new_intent);
        final ProgressDialog prog = new ProgressDialog(context);
        prog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        prog.setTitle("Converting to WAV");
        prog.setMessage("Converting...");
        prog.setIndeterminate(false);
        prog.setCancelable(false);
        prog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        prog.show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (!localfinished && prog.isShowing()) {
                    prog.setMax(JNIHandler.maxTime);
                    prog.setProgress(JNIHandler.currTime);
                    try {

                        Thread.sleep(25);
                    } catch (InterruptedException ignored) {
                    }
                }
                if (!localfinished) {
                    JNIHandler.stop();
                    context.runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(context, "Conversion canceled", Toast.LENGTH_SHORT).show();
                            if (!SettingsStorage.keepPartialWav) {
                                if (new File(finalval).exists())
                                    new File(finalval).delete();
                            } else {
                                Intent outgoingIntent = new Intent();
                                outgoingIntent.setAction(Constants.ta_rec);
                                outgoingIntent.putExtra(Constants.ta_cmd, Constants.ta_cmd_refresh_filebrowser);
                                context.sendBroadcast(outgoingIntent);
                            }
                        }
                    });

                } else {
                    context.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                TextView messageView = prog.findViewById(android.R.id.message);
                                messageView.setText("Copying... Please wait...");
                                messageView.invalidate();
                            }
                        }
                    });
                    String trueName = finalval;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                            && DocumentFileUtils.docFileDevice != null && needToRename != null
                    ) {
                        if (DocumentFileUtils.renameDocumentFile(context, finalval, needToRename)) {
                            trueName = needToRename;
                        } else {
                            trueName = "Error";
                        }
                    }
                    final String tn = trueName;
                    context.runOnUiThread(new Runnable() {
                        public void run() {
                            prog.dismiss();
                            Toast.makeText(context, "Wrote " + tn, Toast.LENGTH_SHORT).show();
                            Intent outgoingIntent = new Intent();
                            outgoingIntent.setAction(Constants.ta_rec);
                            outgoingIntent.putExtra(Constants.ta_cmd, Constants.ta_cmd_refresh_filebrowser);
                            context.sendBroadcast(outgoingIntent);
                        }
                    });
                }
            }
        }).start();
    }

    @Override
    public AlertDialog getAlertDialog() {
        // TODO Auto-generated method stub
        return alertDialog;
    }

    @Override
    public void setLocalFinished(boolean localfinished) {
        // TODO Auto-generated method stub
        this.localfinished = localfinished;
    }
}
