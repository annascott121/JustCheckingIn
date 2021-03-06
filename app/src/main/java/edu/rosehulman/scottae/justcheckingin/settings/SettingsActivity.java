package edu.rosehulman.scottae.justcheckingin.settings;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.support.v7.app.ActionBar;
import android.util.Log;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Locale;

import edu.rosehulman.scottae.justcheckingin.R;
import edu.rosehulman.scottae.justcheckingin.utils.Constants;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity {

    private static String contact = "None";
    private static DatabaseReference mRef;

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();

        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new AllPreferencesFragment())
                .commit();
        String firebasePath = getIntent().getStringExtra(Constants.USER_TAG);
        if (firebasePath == null || firebasePath.isEmpty()) {
            mRef = FirebaseDatabase.getInstance().getReference();
        } else {
            mRef = FirebaseDatabase.getInstance().getReference().child(firebasePath).child("settings");
        }

        sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object value) {

                if (preference instanceof ListPreference) {
                    // For list preferences, look up the correct display value in
                    // the preference's 'entries' list.
                    ListPreference listPreference = (ListPreference) preference;
                    int index = listPreference.findIndexOfValue(value.toString());

                    // Set the summary to reflect the new value.
                    preference.setSummary(
                            index >= 0
                                    ? listPreference.getEntries()[index]
                                    : null);
                } else if (preference instanceof EditTextPreference) {
                    preference.setSummary(value.toString());
                } else if (preference instanceof NumberPickerPreference) {
                    preference.setSummary(String.format(Locale.getDefault(),
                            "%d minutes after Check-In", (int) value));
                } else if (preference instanceof TimePreference) {
                    preference.setSummary(String.valueOf(value));
                }
                mRef.child(preference.getKey()).setValue(value.toString());
                return true;
            }
        };
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }


    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(final Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        mRef.child(preference.getKey()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (preference instanceof NumberPickerPreference)
                    return;
                else
                    if  (dataSnapshot.getValue() != null)
                        preference.setSummary(dataSnapshot.getValue().toString());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // do nothing
            }
        });
    }


    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return AllPreferencesFragment.class.getName().equals(fragmentName);
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class AllPreferencesFragment extends PreferenceFragment {
        private final static int SELECT_PHONE_NUMBER = 1;
        private Preference mPreference;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference(getString(R.string.emergency_contact)));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.language)));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.default_message)));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.check_in_frequency_label)));

            Preference contactPref = findPreference(getString(R.string.emergency_contact));
            contactPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(Intent.ACTION_PICK);
                    intent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
                    mPreference = preference;
                    startActivityForResult(intent, SELECT_PHONE_NUMBER);
                    return true;
                }
            });

            bindPreferenceSummaryToValue(findPreference(getString(R.string.check_in_reminder)));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.unresponse_limit)));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.default_reminder)));
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            sBindPreferenceSummaryToValueListener = null;
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (requestCode == SELECT_PHONE_NUMBER && resultCode == RESULT_OK) {
                Uri contactUri = data.getData();

                String[] projectionName = new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME};
                Cursor cursorName = getActivity().getContentResolver().query(contactUri, projectionName, null, null, null);
                if (cursorName != null && cursorName.moveToFirst()) {
                    int nameIndex = cursorName.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                    SettingsActivity.contact = cursorName.getString(nameIndex);
                }

                String[] projectionNumber = new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER};
                Cursor cursorNumber = getActivity().getContentResolver().query(contactUri, projectionNumber, null, null, null);
                if (cursorNumber != null && cursorNumber.moveToFirst()) {
                    int numberIndex = cursorNumber.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                    String number = cursorNumber.getString(numberIndex);
                    SettingsActivity.contact += ": " + number;
                }

                mPreference.setSummary(contact);
                mRef.child(mPreference.getKey()).setValue(contact);
                assert cursorNumber != null;
                cursorNumber.close();
                assert cursorName != null;
                cursorName.close();
            }
        }
    }
}
