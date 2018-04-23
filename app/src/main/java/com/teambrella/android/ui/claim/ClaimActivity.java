package com.teambrella.android.ui.claim;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.google.gson.JsonObject;
import com.teambrella.android.R;
import com.teambrella.android.api.TeambrellaModel;
import com.teambrella.android.api.model.json.JsonWrapper;
import com.teambrella.android.api.server.TeambrellaUris;
import com.teambrella.android.data.base.TeambrellaDataFragment;
import com.teambrella.android.data.base.TeambrellaDataPagerFragment;
import com.teambrella.android.image.glide.GlideApp;
import com.teambrella.android.services.TeambrellaNotificationServiceClient;
import com.teambrella.android.services.push.INotificationMessage;
import com.teambrella.android.ui.base.ADataProgressFragment;
import com.teambrella.android.ui.base.ATeambrellaActivity;
import com.teambrella.android.ui.chat.ChatBroadCastManager;
import com.teambrella.android.ui.chat.ChatBroadCastReceiver;
import com.teambrella.android.ui.teammate.TeammateActivity;
import com.teambrella.android.util.StatisticHelper;

import org.jetbrains.annotations.NotNull;

import io.reactivex.Notification;
import io.reactivex.disposables.Disposable;

/**
 * Claim Activity
 */
public class ClaimActivity extends ATeambrellaActivity implements IClaimActivity {

    private static final String CLAIM_DATA_TAG = "claim_data_tag";
    private static final String VOTE_DATA_TAG = "vote_data_tag";
    private static final String UI_TAG = "ui";
    private static final String EXTRA_URI = "uri";
    private static final String EXTRA_MODEL = "model";
    private static final String EXTRA_TEAM_ID = "team_id";
    private static final String EXTRA_CLAIM_ID = "claimId";

    private static final int DEFAULT_REQUEST_CODE = 4;


    private Disposable mDisposal;


    private int mClaimId;
    private int mTeamId;
    private TextView mTitle;
    private TextView mSubtitle;
    private ImageView mIcon;
    private Snackbar mSnackBar;
    private String mTopicId;
    private ClaimNotificationClient mNotificationClient;
    private ChatBroadCastManager mChatBroadCastManager;


    public static Intent getLaunchIntent(Context context, int id, String model, int teamId) {
        return new Intent(context, ClaimActivity.class)
                .putExtra(EXTRA_URI, TeambrellaUris.getClaimUri(id))
                .putExtra(EXTRA_MODEL, model)
                .putExtra(EXTRA_CLAIM_ID, id)
                .putExtra(EXTRA_TEAM_ID, teamId);
    }

    public static void start(Context context, int id, String model, int teamId) {
        context.startActivity(getLaunchIntent(context, id, model, teamId));
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        final Intent intent = getIntent();
        mClaimId = intent.getIntExtra(EXTRA_CLAIM_ID, -1);
        mTeamId = intent.getIntExtra(EXTRA_TEAM_ID, -1);
        mChatBroadCastManager = new ChatBroadCastManager(this);
        mChatBroadCastManager.registerReceiver(mChatBroadCastReceiver);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_claim);
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager.findFragmentByTag(UI_TAG) == null) {
            fragmentManager.beginTransaction()
                    .add(R.id.container, ADataProgressFragment.getInstance(new String[]{CLAIM_DATA_TAG, VOTE_DATA_TAG}, ClaimFragment.class), UI_TAG)
                    .commit();
        }

        mNotificationClient = new ClaimNotificationClient(this);
        mNotificationClient.connect();

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back_vector);
            actionBar.setDisplayOptions(actionBar.getDisplayOptions() | ActionBar.DISPLAY_SHOW_CUSTOM);
            actionBar.setCustomView(R.layout.claim_toolbar_view);
            View view = actionBar.getCustomView();
            mTitle = view.findViewById(R.id.title);
            mSubtitle = view.findViewById(R.id.subtitle);
            mIcon = view.findViewById(R.id.icon);
            Toolbar parent = (Toolbar) view.getParent();
            parent.setPadding(0, 0, 0, 0);
            parent.setContentInsetsAbsolute(0, 0);
        }
        setTitle(getIntent().getStringExtra(EXTRA_MODEL));
        getComponent().inject(this);
    }


    @Override
    protected void onStart() {
        super.onStart();
        mDisposal = getObservable(CLAIM_DATA_TAG).subscribe(this::onDataUpdated);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mDisposal != null && !mDisposal.isDisposed()) {
            mDisposal.dispose();
        }
    }

    @Override
    protected String[] getDataTags() {
        return new String[]{CLAIM_DATA_TAG, VOTE_DATA_TAG};
    }

    @Override
    protected TeambrellaDataFragment getDataFragment(String tag) {
        switch (tag) {
            case CLAIM_DATA_TAG:
                return TeambrellaDataFragment.getInstance(getIntent().getParcelableExtra(EXTRA_URI));
            case VOTE_DATA_TAG:
                return TeambrellaDataFragment.getInstance(null);
        }

        return null;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected String[] getPagerTags() {
        return new String[]{};
    }

    @Override
    protected TeambrellaDataPagerFragment getDataPagerFragment(String tag) {
        return null;
    }

    @Override
    public void setTitle(String title) {
        if (mTitle != null) {
            mTitle.setText(title);
        }
    }

    @Override
    public void setSubtitle(String subtitle) {
        if (mSubtitle != null) {
            mSubtitle.setText(subtitle);
        }
    }


    @Override
    public void postVote(int vote) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        TeambrellaDataFragment dataFragment = (TeambrellaDataFragment) fragmentManager.findFragmentByTag(VOTE_DATA_TAG);
        if (dataFragment != null) {
            dataFragment.load(TeambrellaUris.getClaimVoteUri(mClaimId, vote));
        }
        StatisticHelper.onClaimVote(this, getTeamId(), getClaimId(), vote);
    }


    @Override
    public void showSnackBar(@StringRes int text) {
        if (mSnackBar == null) {
            mSnackBar = Snackbar.make(findViewById(R.id.container), text, Snackbar.LENGTH_LONG);

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

    @Override
    protected void onResume() {
        super.onResume();
        if (mNotificationClient != null) {
            mNotificationClient.onResume();
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        if (mNotificationClient != null) {
            mNotificationClient.onPause();
        }
    }

    @Override
    public int getClaimId() {
        return mClaimId;
    }

    @Override
    public int getTeamId() {
        return mTeamId;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        load(CLAIM_DATA_TAG);
        super.onActivityResult(requestCode, resultCode, data);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mNotificationClient != null) {
            mNotificationClient.disconnect();
        }
        mChatBroadCastManager.unregisterReceiver(mChatBroadCastReceiver);
    }

    @Override
    public void launchActivity(Intent intent) {
        startActivityForResult(intent, DEFAULT_REQUEST_CODE);
    }

    protected void onDataUpdated(Notification<JsonObject> notification) {
        if (notification.isOnNext()) {
            JsonWrapper response = new JsonWrapper(notification.getValue());
            JsonWrapper data = response.getObject(TeambrellaModel.ATTR_DATA);
            JsonWrapper basic = data.getObject(TeambrellaModel.ATTR_DATA_ONE_BASIC);
            JsonWrapper discussion = data.getObject(TeambrellaModel.ATTR_DATA_ONE_DISCUSSION);
            if (basic != null) {
                String pictureUri = basic.getString(TeambrellaModel.ATTR_DATA_AVATAR);
                if (pictureUri != null) {
                    GlideApp.with(this).load(getImageLoader().getImageUrl(pictureUri))
                            .apply(new RequestOptions().transforms(new CenterCrop(), new CircleCrop())).into(mIcon);
                    mIcon.setOnClickListener(v ->
                            TeammateActivity.start(ClaimActivity.this
                                    , getIntent().getIntExtra(EXTRA_TEAM_ID, 0)
                                    , basic.getString(TeambrellaModel.ATTR_DATA_USER_ID)
                                    , basic.getString(TeambrellaModel.ATTR_DATA_NAME)
                                    , pictureUri));
                }

                setTitle(basic.getString(TeambrellaModel.ATTR_DATA_MODEL));
            }


            if (discussion != null) {
                mTopicId = discussion.getString(TeambrellaModel.ATTR_DATA_TOPIC_ID);
            }


            mClaimId = data.getInt(TeambrellaModel.ATTR_DATA_ID, 0);

        }
    }

    private class ClaimNotificationClient extends TeambrellaNotificationServiceClient {

        private boolean mResumed;
        private boolean mUpdateOnResume;

        ClaimNotificationClient(Context context) {
            super(context);
        }

        @Override
        public boolean onPushMessage(INotificationMessage message) {
            String topicId = message.getTopicId();
            if (topicId != null && topicId.equals(mTopicId)) {
                if (mResumed) {
                    load(CLAIM_DATA_TAG);
                } else {
                    mUpdateOnResume = true;
                }
            }
            return false;
        }

        void onPause() {
            mResumed = false;
        }

        void onResume() {
            mResumed = true;
            if (mUpdateOnResume) {
                load(CLAIM_DATA_TAG);
                mUpdateOnResume = false;
            }
        }
    }

    private ChatBroadCastReceiver mChatBroadCastReceiver = new ChatBroadCastReceiver() {
        @Override
        protected void onTopicRead(@NotNull String topicId) {
            super.onTopicRead(topicId);
            if (topicId.equals(mTopicId)) {
                load(CLAIM_DATA_TAG);
            }
        }
    };
}
