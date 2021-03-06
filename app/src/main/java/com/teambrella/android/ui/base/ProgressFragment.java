package com.teambrella.android.ui.base;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.teambrella.android.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * Progress Fragment
 */
public abstract class ProgressFragment extends TeambrellaFragment {

    @BindView(R.id.content)
    ViewGroup mContent;
    @BindView(R.id.data)
    ViewGroup mData;
    @BindView(R.id.error)
    ViewGroup mError;
    @BindView(R.id.refreshable)
    SwipeRefreshLayout mRefreshable;


    private Unbinder mUnbinder;


    private Handler mHandler = new Handler();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_progress, container, false);
        mUnbinder = ButterKnife.bind(this, view);
        mData.addView(onCreateContentView(inflater, container, savedInstanceState));
        mHandler.postDelayed(mPostponedRefreshing, 1000);
        return view;
    }

    protected abstract View onCreateContentView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState);


    protected void setContentShown(boolean shown, boolean error) {
        if (!shown) {
            mHandler.postDelayed(mPostponedRefreshing, 1000);
        } else {
            mHandler.removeCallbacks(mPostponedRefreshing);
            mRefreshable.setRefreshing(false);
        }

        mContent.setVisibility(shown ? View.VISIBLE : View.GONE);
        mData.setVisibility(error ? View.GONE : View.VISIBLE);
        mError.setVisibility(error ? View.VISIBLE : View.GONE);
    }

    protected void setContentShown(boolean shown) {
        setContentShown(shown, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRefreshable.setColorSchemeColors(getResources().getColor(R.color.colorPrimary));
        mRefreshable.setOnRefreshListener(this::onReload);
    }

    protected void setRefreshable(@SuppressWarnings("SameParameterValue") boolean refreshable) {
        if (mRefreshable != null) {
            mRefreshable.setEnabled(refreshable);
        }
    }

    public void setRefreshing(@SuppressWarnings("SameParameterValue") boolean refreshing) {
        if (mRefreshable != null) {
            mRefreshable.setRefreshing(refreshing);
        }
    }


    protected abstract void onReload();

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mHandler.removeCallbacks(mPostponedRefreshing);
        mUnbinder.unbind();
    }


    private Runnable mPostponedRefreshing = () -> mRefreshable.setRefreshing(true);
}
