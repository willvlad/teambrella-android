package com.teambrella.android.ui.chat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.google.gson.JsonObject;
import com.teambrella.android.R;
import com.teambrella.android.api.server.TeambrellaUris;
import com.teambrella.android.ui.base.AppCompatRequestActivity;
import com.teambrella.android.ui.base.TeambrellaBroadcastManager;
import com.teambrella.android.ui.dialog.ProgressDialogFragment;
import com.teambrella.android.util.ConnectivityUtils;

import io.reactivex.Notification;

/**
 * Activity to start a new discussion
 */
public class StartNewChatActivity extends AppCompatRequestActivity implements TextWatcher {


    private static final String EXTRA_TEAM_ID = "teamId";

    private static final String PROGRESS_DIALOG_TAG = "progress_dialog_tag";

    public static void startForResult(Activity activity, int teamId, int requestCode) {
        activity.startActivityForResult(new Intent(activity, StartNewChatActivity.class).putExtra(EXTRA_TEAM_ID, teamId), requestCode);
    }

    private int mTeamId;
    private TextView mTitleView;
    private TextView mPostView;
    private Snackbar mSnackBar;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        mTeamId = getIntent().getIntExtra(EXTRA_TEAM_ID, 0);
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_discussion);
        setTitle(R.string.new_discussion);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_close);
        }

        mTitleView = findViewById(R.id.title);
        mPostView = findViewById(R.id.post);
        mTitleView.addTextChangedListener(this);
        mPostView.addTextChangedListener(this);
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        String title = mTitleView.getText().toString().trim();
        String post = mPostView.getText().toString().trim();
        menu.findItem(R.id.submit).setEnabled(!TextUtils.isEmpty(title) && !TextUtils.isEmpty(post));
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.submit_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                setResult(AppCompatActivity.RESULT_CANCELED);
                finish();
                return true;
            case R.id.submit:
                hideKeyboard();
                showProgressDialog();
                item.setEnabled(false);
                request(TeambrellaUris.getNewChatUri(mTeamId, mTitleView.getText().toString(), mPostView.getText().toString()));
                return true;

        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onRequestResult(Notification<JsonObject> response) {
        super.onRequestResult(response);
        if (response.isOnNext()) {
            new TeambrellaBroadcastManager(this).notifyNewChatStarted();
            setResult(RESULT_OK);
            finish();
        } else {
            showSnackBar(ConnectivityUtils.isNetworkAvailable(this) ? R.string.something_went_wrong_error : R.string.no_internet_connection);
        }
        hideProgressDialog();
        invalidateOptionsMenu();
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence charSequence, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable editable) {
        invalidateOptionsMenu();
    }

    private void showProgressDialog() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager.findFragmentByTag(PROGRESS_DIALOG_TAG) == null) {
            new ProgressDialogFragment().show(fragmentManager, PROGRESS_DIALOG_TAG);
        }
    }


    private void hideProgressDialog() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentByTag(PROGRESS_DIALOG_TAG);
        if (fragment != null) {
            fragmentManager.beginTransaction().remove(fragment).commit();
        }
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    private void showSnackBar(@StringRes int text) {
        if (mSnackBar == null) {
            mSnackBar = Snackbar.make(mPostView, text, Snackbar.LENGTH_LONG);

            mSnackBar.addCallback(new Snackbar.Callback() {
                @Override
                public void onShown(Snackbar sb) {
                    super.onShown(sb);
                }

                @Override
                public void onDismissed(Snackbar transientBottomBar, int event) {
                    super.onDismissed(transientBottomBar, event);
                    mSnackBar = null;
                }
            });
            mSnackBar.show();
        }
    }
}
