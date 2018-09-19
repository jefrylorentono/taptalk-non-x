package com.moselo.HomingPigeon.Helper;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import com.moselo.HomingPigeon.R;

public class HomingPigeonDialog extends Dialog implements View.OnClickListener{

    private static final String TAG = HomingPigeonDialog.class.getSimpleName();
    public Context context;
    public Dialog dialog;
    public TextView title, message, secondary, primary;
    private String dialogTitle = "", dialogMessage = "", textSecondary = "", textPrimary = "";
    int layout;

    //listener
    View.OnClickListener primaryListener = null;
    View.OnClickListener secondaryListener = null;

    public HomingPigeonDialog(Context context, String dialogTitle, String dialogMessage,
                              String textPrimary, View.OnClickListener primaryListener,
                              String textSecondary, View.OnClickListener secondaryListener) {
        super(context);
        this.context = context;
        this.dialogTitle = dialogTitle;
        this.dialogMessage = dialogMessage;
        this.textPrimary = textPrimary;
        this.primaryListener = primaryListener;
        this.textSecondary = textSecondary;
        this.secondaryListener = secondaryListener;
        layout = 1;
    }

    public HomingPigeonDialog(Context context, String dialogTitle, String dialogMessage,
                              String textPrimary, View.OnClickListener primaryListener) {
        super(context);
        this.context = context;
        this.dialogTitle = dialogTitle;
        this.dialogMessage = dialogMessage;
        this.textPrimary = textPrimary;
        this.primaryListener = primaryListener;
        layout = 2;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        try {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        } catch (Exception e) {
            Log.e(TAG, "onCreate: ", e);
        }

        setContentView(R.layout.dialog_homing_pigeon);
        title = findViewById(R.id.tv_dialog_title);
        message = findViewById(R.id.tv_dialog_message);
        primary = findViewById(R.id.tv_primary_btn);
        secondary = findViewById(R.id.tv_secondary_btn);

        setTextInTextView(title, dialogTitle);
        setTextInTextView(message, dialogMessage);

        switch (layout) {
            case 1:
                secondary.setVisibility(View.VISIBLE);
                initiateButton(R.id.tv_secondary_btn, textSecondary);
                initiateButton(R.id.tv_primary_btn, textPrimary);
                break;
            case 2:
                secondary.setVisibility(View.GONE);
                initiateButton(R.id.tv_primary_btn, textPrimary);
                break;
        }
    }

    private void initiateButton(@IdRes int id, String text) {
        TextView view = findViewById(id);
        if (!text.equals("")) {
            view.setText(text);
            view.setOnClickListener(this);
        }
    }

    private void setTextInTextView(TextView view, String text) {
        if (text.trim().equals("")) view.setVisibility(View.GONE);
        else view.setText(text);
    }

    @Override
    public void show() {
        super.show();
    }

    @Override
    public void onClick(View v) {
        if (R.id.tv_primary_btn == v.getId() && null != primaryListener) {
            primaryListener.onClick(v);
        } else if (R.id.tv_secondary_btn == v.getId() && null != secondaryListener) {
            secondaryListener.onClick(v);
        }
    }
}
