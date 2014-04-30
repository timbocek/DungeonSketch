package com.tbocek.android.combatmap;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import com.tbocek.android.combatmap.model.primitives.Information;
import com.tbocek.android.combatmap.model.primitives.Util;
import com.tbocek.dungeonsketch.R;

/**
 * An Android dialog that allows the user to enter multi-line text for info points.
 *
 * @author Tim Bocek
 *
 */
public class InfoPointDialog extends Dialog {
    private int[] mIconIdToRadio = new int[] {
        R.id.radio_info,
        R.id.radio_combat,
        R.id.radio_treasure
    };

    /**
     * Button that the user clicks to confirm the text entered.
     */
    private Button mConfirmButton;

    /**
     * Listener that is called when the user clicks the confirm button.
     */
    private OnTextConfirmedListener mListener;

    /**
     * Text entry field.
     */
    private TextView mNameText;

    private RadioGroup mIconRadioGroup;

    /**
     * Constructor.
     *
     * @param context
     *            Context to create the dialog in.
     * @param listener
     *            Listener that specifies the action to take when the user
     *            confirms the text entered.
     */
    public InfoPointDialog(final Context context,
                      final OnTextConfirmedListener listener) {
        super(context);
        mIconIdToRadio = new int[] {
                R.id.radio_info,
                R.id.radio_combat,
                R.id.radio_treasure
        };

        this.setContentView(R.layout.info_point);
        this.setTitle(context.getString(R.string.info_point));
        this.mListener = listener;

        this.mConfirmButton = (Button) this.findViewById(R.id.button_save);
        this.mNameText = (TextView) this.findViewById(R.id.entered_text);
        this.mNameText.requestFocus();
        this.mNameText.setText("");
        this.mIconRadioGroup = (RadioGroup) this.findViewById(R.id.info_point_type);
        this.mConfirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                int iconId = 0;
                for (int i = 0; i < mIconIdToRadio.length; ++i) {
                    if (mIconIdToRadio[i] == mIconRadioGroup.getCheckedRadioButtonId()) {
                        iconId = i;
                        break;
                    }
                }
                String name =
                        (String) InfoPointDialog.this.mNameText.getText().toString();
                InfoPointDialog.this.dismiss();
                InfoPointDialog.this.mListener.onTextConfirmed(name, iconId);
            }
        });
    }

    /**
     * Removes any currently entered text from the dialog.
     */
    public void clearText() {
        this.mNameText.setText("");
        this.mIconRadioGroup.check(R.id.radio_info);
    }

    /**
     * Sets the value of the text and font size fields, for when this dialog is
     * used to edit instead of create text objects.
     *
     * @param text
     *            The current text of the object being edited.W
     * @param icon
     */
    public void populateFields(String text, int icon) {

        this.mIconRadioGroup.check(mIconIdToRadio[icon]);
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
         * @param text
         *            The text entered by the user.
         * @param iconId
         *
         */
        void onTextConfirmed(String text, int iconId);
    }
}
