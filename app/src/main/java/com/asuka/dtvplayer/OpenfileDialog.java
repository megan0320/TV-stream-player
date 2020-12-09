package com.asuka.dtvplayer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by Wyvern on 2015/6/29.
 */
public class OpenfileDialog extends DialogFragment {
    private TextView mTxtvCurrentPath;
    private File mCurrentPath;
    private ListView mLstvFiles;
    private ArrayAdapter<String> mAdptFiles;
    private ArrayList<String> mListFiles = new ArrayList<String>();
    private Button mButnConfirm;
    private String mSelectedFileName;
    private int mSelectedFilePos = -1;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View contentView = inflater.inflate(R.layout.dialog_openfile, null);

        mTxtvCurrentPath = (TextView) contentView.findViewById(R.id.txtv_openfdlg_current_path);
        mLstvFiles = (ListView) contentView.findViewById(R.id.lstv_openfdlg_files);
        mButnConfirm = (Button) contentView.findViewById(R.id.butn_openfdlg_confirm);

        mAdptFiles = new ArrayAdapter<String>(getActivity(),
                R.layout.listitem_file,
                R.id.txtv_file_list_item,
                mListFiles);
        mLstvFiles.setAdapter(mAdptFiles);

        if (isExternalStorageReadable()) {
            mCurrentPath = Environment.getExternalStorageDirectory();
        }
        else {
            mCurrentPath = Environment.getDataDirectory();
        }
        updateFileList(-1, mCurrentPath);

        mLstvFiles.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position < 0 || position >= mListFiles.size()) {
                    return;
                }
                String newPathName = mListFiles.get(position);
                File newPath;
                if (newPathName.equals("..")) {
                    newPath = mCurrentPath.getParentFile();
                } else {
                    newPath = new File(mCurrentPath, newPathName);
                }
                if (newPath.isFile()) {
                    selectFile(position, newPath);
                }
                else {
                    updateFileList(position, newPath);
                }
            }
        });

        mButnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mConfirmListener != null) {
                    Bundle arg = new Bundle();
                    arg.putString("file path", mSelectedFileName);
                    mConfirmListener.onDialogConfirm(OpenfileDialog.this, arg);
                }
                dismiss();
            }
        });

        builder.setView(contentView)
                .setTitle(R.string.openfdlg_title_choose_file);
        return builder.create();
    }

    private boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    private void selectFile(int position, File file) {
        mSelectedFilePos = position;
        mSelectedFileName = file.getAbsolutePath();
        mButnConfirm.setEnabled(true);
        mAdptFiles.notifyDataSetChanged();
    }

    private void updateFileList(int position, File path) {
        if (position >= 0) {
            mLstvFiles.setItemChecked(position, false);
        }
        if (mSelectedFilePos >= 0) {
            mSelectedFilePos = -1;
            mSelectedFileName = null;
        }

        if (path.canRead()) {
            mButnConfirm.setEnabled(false);
            mSelectedFilePos = -1;
            mSelectedFileName = null;
            mListFiles.clear();
            mListFiles.add("..");
            String[] fileList = path.list();
            if (fileList != null) {
                for (String fileName : fileList) {
                    if (!fileName.startsWith(".")) {
                        mListFiles.add(fileName);
                    }
                }
            }
            mCurrentPath = path;
            mAdptFiles.notifyDataSetChanged();
            mTxtvCurrentPath.setText(mCurrentPath.getAbsolutePath());
        }
        else {
            Toast.makeText(getActivity(),
                    "Cannot access this path",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private DialogConfirmCallback mConfirmListener;
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mConfirmListener = (DialogConfirmCallback)activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement ScanDialogConfirmCallback");
        }
    }
}
