package pl.bejc.android;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

public class MainActivity extends Activity {

    private static final int CONTACT_PICKER_RESULT = 1001;

    public List<String> affiliates;
    private Integer netActivity = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_main);

        loadAccessToken();
    }

    public void accessTokenReadyCallback() {
        new LoadAffiliatesTask(this).execute();
    }

    public void loadAffiliatesListCallback(List<String> affiliates) {
        this.affiliates = affiliates;
        ((LinearLayout) findViewById(R.id.affiliates)).removeAllViews();
        for (String affiliate : affiliates) {
            new DrawAffiliateTask(this).execute(affiliate);
        }
    }

//    private void prepareAccountName() {
//        AccountManager accountManager = AccountManager.get(this);
//        assert accountManager != null;
//        Account[] accounts = accountManager.getAccountsByType("com.google");
//        TextView view = (TextView) findViewById(R.id.textView2);
//        String name = accounts.length == 0 ? "brak" : accounts[0].name;
//        view.setText(name);
//    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    public void doLaunchContactPicker(View view) {
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        startActivityForResult(intent, CONTACT_PICKER_RESULT);
    }

    public void doAddDebt(View view) throws IOException, JSONException {

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        JSONObject json = new JSONObject();
        json.put("access_token", getAccessToken());
        json.put("amount", ((EditText) findViewById(R.id.amountTextArea)).getText().toString());
        json.put("affiliate", ((EditText) findViewById(R.id.affiliateTextArea)).getText().toString());
        json.put("description", ((EditText) findViewById(R.id.descriptionTextArea)).getText().toString());
        switch (view.getId()) {
            case R.id.heOwesMeButton:
                json.put("heOwesMe", 1);
                break;
            case R.id.iOweHimButton:
                json.put("iOweHim", 1);
                break;
        }

        new AddDebtTask(this).execute(json);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case CONTACT_PICKER_RESULT:

                    Uri result = data.getData();
                    String id = result.getLastPathSegment();
                    Cursor cursor = getContentResolver().query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, null, ContactsContract.CommonDataKinds.Email.CONTACT_ID + "=?", new String[]{id}, null);
                    String email = "";
                    if (cursor.moveToFirst()) {
                        int emailIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA);
                        email = cursor.getString(emailIdx);
                        Log.v("debug", "Got email: " + email);
                    }
                    EditText emailEntry = (EditText) findViewById(R.id.affiliateTextArea);
                    emailEntry.setText(email);
                    if (email.length() == 0) {
                        Toast.makeText(this, "Wybrany kontakt nie posiada adresu email.", Toast.LENGTH_LONG).show();
                    }

                    break;
                case AccessTokenTask.USER_RECOVERABLE_AUTH_EXCEPTION:
                    loadAccessToken();
                    break;
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.renew_access_token:
                renewToken();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void markNetActivity() {
        netActivity++;
        if (netActivity == 1) {
            setProgressBarIndeterminateVisibility(true);
        }
    }

    public void unmarkNetActivity() {
        netActivity--;
        if (netActivity == 0) {
            setProgressBarIndeterminateVisibility(false);
        }
    }

    public void lockInterface() {
        findViewById(R.id.iOweHimButton).setClickable(false);
        findViewById(R.id.iOweHimButton).setBackgroundColor(0xFF333333);
        findViewById(R.id.heOwesMeButton).setClickable(false);
        findViewById(R.id.heOwesMeButton).setBackgroundColor(0xFF333333);
        findViewById(R.id.affiliateTextArea).setEnabled(false);
        findViewById(R.id.amountTextArea).setEnabled(false);
        findViewById(R.id.descriptionTextArea).setEnabled(false);
    }

    public void unlockInterface() {
        findViewById(R.id.iOweHimButton).setClickable(true);
        findViewById(R.id.iOweHimButton).setBackgroundColor(0xFFD44944);
        findViewById(R.id.heOwesMeButton).setClickable(true);
        findViewById(R.id.heOwesMeButton).setBackgroundColor(0xFF5AB45A);
        findViewById(R.id.affiliateTextArea).setEnabled(true);
        findViewById(R.id.amountTextArea).setEnabled(true);
        findViewById(R.id.descriptionTextArea).setEnabled(true);
    }

    public void renewToken() {
        AccountManager.get(this).invalidateAuthToken("com.google", getAccessToken());
        new AccessTokenTask(this).execute();
    }

    private void loadAccessToken() {
        if (getAccessToken() == "null") {
            new AccessTokenTask(this).execute();
        } else {
            accessTokenReadyCallback();
        }
    }

    public String getAccessToken() {
        return getSharedPreferences("main", MODE_MULTI_PROCESS).getString("access_token", "null");
    }

    public void resetTextAreas() {
        ((TextView) findViewById(R.id.affiliateTextArea)).setText("");
        ((TextView) findViewById(R.id.amountTextArea)).setText("");
        ((TextView) findViewById(R.id.descriptionTextArea)).setText("");
        findViewById(R.id.affiliateTextArea).setFocusableInTouchMode(true);
        findViewById(R.id.affiliateTextArea).requestFocus();
    }

}
