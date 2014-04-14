package com.tbocek.android.combatmap;

import java.util.HashMap;
import java.util.Map;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import com.tbocek.dungeonsketch.R;

/**
 * Interface that allows management of a tabbing system without caring whether
 * the tabs are implemented in the ActionBar or in a TabWidget. Tab actions are
 * forwarded to a listener regardless of the tab implementation.
 * 
 * @author Tim
 * 
 */
public class TabManager {

    /**
     * Action bar that provides the tabs.
     */
    private ActionBar mActionBar;

    /**
     * The Android context this tab manager is operating in.
     */
    private Context mContext;

    /**
     * The currently selected tab mode. A value < 0 is undefined (and will be
     * treated as a GM-selected tab for the purpose of GM screen confirmation).
     */
    private int mLastSelectedMode = -1;

    /**
     * Reverse lookup so we know what tab to select when forced into an
     * interaction mode.
     */
    private Map<Integer, ActionBar.Tab> mManipulationModeTabs =
            new HashMap<Integer, ActionBar.Tab>();

    /**
     * Whether each tab mode (identified by the integer code) requires GM screen
     * confirmation.
     */
    private HashMap<Integer, Boolean> modesForGm =
            new HashMap<Integer, Boolean>();

    /**
     * Listener that fires when a tab is selected.
     */
    private TabSelectedListener mTabSelectedListener;

    /**
     * Constructor.
     * 
     * @param actionBar
     *            The action bar that will provide the tabs.
     * @param context
     *            The application context managing these tabs.
     */
    public TabManager(ActionBar actionBar, Context context) {
        this.mActionBar = actionBar;
        this.mContext = context;
    }

    /**
     * Adds a new tab to the tab manager.
     * 
     * @param description
     *            Description of the tab. This string is what will be shown in
     *            the UI.
     * @param mode
     *            Numerical code identifying the tab.
     * @param forGm
     *            Whether this tab will contain GM-only information.
     */
    public final void addTab(String description, final int mode, boolean forGm) {
        ActionBar.Tab tab = this.mActionBar.newTab();
        tab.setText(description);
        tab.setTabListener(new ActionBar.TabListener() {
            @Override
            public void onTabReselected(ActionBar.Tab arg0, FragmentTransaction arg1) {

            }

            @Override
            public void onTabSelected(ActionBar.Tab arg0, FragmentTransaction arg1) {
                TabManager.this.onTabSelected(mode);
            }

            @Override
            public void onTabUnselected(ActionBar.Tab arg0, FragmentTransaction arg1) {
                // TODO Auto-generated method stub

            }
        });
        this.mActionBar.addTab(tab);
        this.mManipulationModeTabs.put(mode, tab);
        this.modesForGm.put(mode, forGm);
    }

    /**
     * Opens the GM Screen dialog. If the dialog is confirmed, will switch to
     * the requested tab. Otherwise, the previous tab will be used.
     * 
     * @param destinationMode
     *            The requested tab. Will be the mode entered if the GM screen
     *            dialog is confirmed.
     */
    private void confirmGmScreenBeforeSwitchingTabs(final int destinationMode) {
        int switchBackMode = TabManager.this.mLastSelectedMode;
        TabManager.this.mLastSelectedMode = -1;
        this.mManipulationModeTabs.get(switchBackMode).select();

        new AlertDialog.Builder(this.mContext)
                .setCancelable(true)
                .setMessage(R.string.gm_screen_spoiler_warning)
                .setPositiveButton(R.string.gm_screen_mistchief_managed,
                        new OnClickListener() {

                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {
                                // Make sure we don't get kicked into the GM
                                // Screen dialog again!
                                TabManager.this.mLastSelectedMode = -1;
                                TabManager.this.mManipulationModeTabs.get(
                                        destinationMode).select();

                            }

                        })
                .setNegativeButton(R.string.gm_screen_cancel,
                        new OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {

                            }
                        }).create().show();
    }

    /**
     * Checks whether selecting the given mode requires GM Screen confirmation.
     * 
     * @param mode
     *            The mode to enter.
     * @return True if GM Screen confirmation needed.
     */
    protected boolean needGmScreenConfirmation(int mode) {
        if (this.mLastSelectedMode == -1) {
            return false;
        } // Do not need confirmation for first selection.
        if (!PreferenceManager.getDefaultSharedPreferences(this.mContext)
                .getBoolean("gmscreen", false)) {
            return false;
        }
        return this.modesForGm.get(mode).booleanValue()
                && !this.modesForGm.get(this.mLastSelectedMode).booleanValue();
    }

    /**
     * Subclasses can call this to fire the tab selected listener.
     * 
     * @param mode
     *            The integer identifier for the mode that was selected.
     */
    protected void onTabSelected(int mode) {
        if (this.needGmScreenConfirmation(mode)) {
            this.confirmGmScreenBeforeSwitchingTabs(mode);
        } else {
            if (this.mTabSelectedListener != null) {
                this.mTabSelectedListener.onTabSelected(mode);
            }
            this.mLastSelectedMode = mode;
        }
    }

    /**
     * Selects the given tab. Guaranteed to not open the GM Screen dialog.
     * 
     * @param mode
     *            The new tab mode to select.
     */
    public void pickTab(int mode) {
        this.mManipulationModeTabs.get(mode).select();
        this.mLastSelectedMode = mode;
    }

    /**
     * Sets the listener that implements the tab selection action.
     * 
     * @param listener
     *            The new tab selected listener.
     */
    public void setTabSelectedListener(TabSelectedListener listener) {
        this.mTabSelectedListener = listener;
    }

    /**
     * Listener interface for when a new tab is selected.
     * 
     * @author Tim
     * 
     */
    public interface TabSelectedListener {
        /**
         * Fires when a new tab is selected.
         * 
         * @param tab
         *            Integer identifier for the tab that was selected.
         */
        void onTabSelected(int tab);
    }
}
