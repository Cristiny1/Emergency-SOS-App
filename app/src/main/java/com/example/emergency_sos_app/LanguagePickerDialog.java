package com.example.emergency_sos_app;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

public final class LanguagePickerDialog {

    private LanguagePickerDialog() {
    }

    public static void show(Activity activity) {
        View view = LayoutInflater.from(activity)
                .inflate(R.layout.dialog_language_picker, null);

        ImageView checkEnglish = view.findViewById(R.id.check_english);
        ImageView checkKhmer = view.findViewById(R.id.check_khmer);

        boolean isKhmer = LanguageManager.isKhmer(activity);
        checkEnglish.setVisibility(isKhmer ? View.GONE : View.VISIBLE);
        checkKhmer.setVisibility(isKhmer ? View.VISIBLE : View.GONE);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.select_language)
                .setView(view)
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        view.findViewById(R.id.option_english).setOnClickListener(v -> {
            LanguageManager.setLanguage(activity, LanguageManager.LANG_ENGLISH);
            dialog.dismiss();
            // We use manual smooth refresh because setApplicationLocales 
            // can sometimes reset the activity stack on certain Android versions.
            if (activity instanceof BaseActivity) {
                ((BaseActivity) activity).smoothRefresh();
            } else {
                activity.recreate();
            }
        });

        view.findViewById(R.id.option_khmer).setOnClickListener(v -> {
            LanguageManager.setLanguage(activity, LanguageManager.LANG_KHMER);
            dialog.dismiss();
            if (activity instanceof BaseActivity) {
                ((BaseActivity) activity).smoothRefresh();
            } else {
                activity.recreate();
            }
        });

        dialog.show();
    }
}
