package com.tbocek.android.combatmap.about;

import android.app.Dialog;
import android.content.Context;

import com.tbocek.dungeonsketch.R;

/**
 * Dialog that displays info about the application.
 * 
 * @author Tim
 * 
 */
public class AboutDialog extends Dialog {

    /**
     * Constructor.
     * 
     * @param context
     *            Context to construct in.
     */
    public AboutDialog(final Context context) {
        super(context);
        this.setContentView(R.layout.about_box);
        this.setTitle(context.getString(R.string.about));
    }
}
