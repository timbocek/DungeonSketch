package com.tbocek.android.combatmap;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.tbocek.dungeonsketch.R;

/**
 * An Android dialog that allows the user to enter text. Provides a text entry
 * field, and a confirm button. The system back button is used to cancel. Allows
 * customization of the dialog title and the text displayed on the confirmation
 * button.
 * 
 * @author Tim Bocek
 * 
 */
public class TextPromptDialog extends Dialog {

    /**
     * Listener that is called when the user clicks the confirm button.
     */
    private final OnTextConfirmedListener mListener;

    /**
     * Text entry field.
     */
    private final TextView mNameText;

    /**
     * Constructor.
     * 
     * @param context
     *            Context to create the dialog in.
     * @param listener
     *            Listener that specifies the action to take when the user
     *            confirms the text entered.
     * @param title
     *            Title to display in the dialog.
     * @param confirmText
     *            Text to display on the confirmation button.
     */
    public TextPromptDialog(final Context context,
            final OnTextConfirmedListener listener, final String title,
            final String confirmText) {
        super(context);
        this.setContentView(R.layout.save);
        this.setTitle(title);
        this.mListener = listener;

        this.mNameText = (TextView) this.findViewById(R.id.save_file_name);
        this.mNameText.requestFocus();
        this.mNameText.setText("");

        Button confirmButton = (Button) this.findViewById(R.id.button_save);
        confirmButton.setText(confirmText);
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                String name =
                        TextPromptDialog.this.mNameText.getText().toString();
                if (!"".equals(name)) {
                    TextPromptDialog.this.dismiss();
                    TextPromptDialog.this.mListener.onTextConfirmed(name);
                }
            }
        });
    }

    /**
     * Sets the text currently filled in the dialog.
     * 
     * @param text
     *            The new text.
     */
    public final void fillText(String text) {
        this.mNameText.setText(text);
    }

    /**
     * Listener used to specify the action to take when the user confirms text
     * entry in a TextPromptDialog.
     * 
     * @author Tim Bocek
     * 
     */
    public interface OnTextConfirmedListener {
        /**
         * Called when the user confirms the text entered.
         * 
         * @param text
         *            The text entered by the user.
         */
        void onTextConfirmed(String text);
    }

}
