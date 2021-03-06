package com.teambrella.android.ui.withdraw;

import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.teambrella.android.R;
import com.teambrella.android.api.TeambrellaModel;
import com.teambrella.android.api.model.json.JsonWrapper;
import com.teambrella.android.data.base.IDataPager;
import com.teambrella.android.ui.base.TeambrellaDataPagerAdapter;
import com.teambrella.android.util.AmountCurrencyUtil;
import com.teambrella.android.util.TeambrellaDateUtils;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Withdrawals Adapter
 */
class WithdrawalsAdapter extends TeambrellaDataPagerAdapter {

    static final int VIEW_TYPE_SUBMIT_WITHDRAWAL = VIEW_TYPE_REGULAR + 1;
    static final int VIEW_TYPE_QUEUED_HEADER = VIEW_TYPE_REGULAR + 2;
    static final int VIEW_TYPE_IN_PROCESS_HEADER = VIEW_TYPE_REGULAR + 3;
    static final int VIEW_TYPE_HISTORY_HEADER = VIEW_TYPE_REGULAR + 4;
    private static final int VIEW_TYPE_WITHDRAWAL = VIEW_TYPE_REGULAR + 5;

    private String mDefaultWithdrawAddress;

    private float mAvailableValue;
    private float mBalanceValue;
    private float mReservedValue;
    private String mCurrency;
    private float mRate;
    private final IWithdrawActivity mWithdrawActivity;

    WithdrawalsAdapter(IDataPager<JsonArray> pager, IWithdrawActivity withdrawActivity) {
        super(pager);
        mWithdrawActivity = withdrawActivity;
    }

    void setDefaultWithdrawAddress(String address) {
        mDefaultWithdrawAddress = address;
        notifyItemChanged(0);
    }

    void setBalanceValue(float balance, float reserved, String currency, float rate) {
        mBalanceValue = balance;
        mReservedValue = reserved;
        mCurrency = currency;
        mRate = rate;
        mAvailableValue = mBalanceValue - mReservedValue;
        notifyItemChanged(0);
    }

    @Override
    public int getItemViewType(int position) {
        int viewType;
        if (position == 0) {
            viewType = VIEW_TYPE_SUBMIT_WITHDRAWAL;
        } else {
            viewType = super.getItemViewType(position);
            if (viewType == VIEW_TYPE_REGULAR) {
                JsonObject item = mPager.getLoadedData().get(position - getHeadersCount()).getAsJsonObject();
                switch (item.get(TeambrellaModel.ATTR_DATA_ITEM_TYPE).getAsString()) {
                    case TeambrellaModel.WithdrawlsItemType.ITEM_QUEUED_HEADER:
                        viewType = VIEW_TYPE_QUEUED_HEADER;
                        break;
                    case TeambrellaModel.WithdrawlsItemType.ITEM_IN_PROCESS_HEADER:
                        viewType = VIEW_TYPE_IN_PROCESS_HEADER;
                        break;
                    case TeambrellaModel.WithdrawlsItemType.ITEM_HISTORY_HEADER:
                        viewType = VIEW_TYPE_HISTORY_HEADER;
                        break;
                    case TeambrellaModel.WithdrawlsItemType.ITEM_HISTORY:
                    case TeambrellaModel.WithdrawlsItemType.ITEM_IN_PROCESS:
                    case TeambrellaModel.WithdrawlsItemType.ITEM_QUEDUED:
                        viewType = VIEW_TYPE_WITHDRAWAL;
                        break;
                }
            }
        }
        return viewType;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder viewHolder = super.onCreateViewHolder(parent, viewType);
        if (viewHolder == null) {
            switch (viewType) {
                case VIEW_TYPE_SUBMIT_WITHDRAWAL:
                    return new SubmitWithdrawViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_withdraw_request, parent, false));
                case VIEW_TYPE_QUEUED_HEADER:
                    return new Header(parent, R.string.deferred_withdrawals, R.string.milli_ethereum);
                case VIEW_TYPE_IN_PROCESS_HEADER:
                    return new Header(parent, R.string.withdrawals_in_progress, R.string.milli_ethereum, R.drawable.list_item_header_background_top);
                case VIEW_TYPE_HISTORY_HEADER:
                    return new Header(parent, R.string.history_withdrawals, R.string.milli_ethereum);
                case VIEW_TYPE_WITHDRAWAL:
                    return new WithdrawalViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_withdrawal, parent, false));
            }
        }
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        if (holder instanceof SubmitWithdrawViewHolder) {
            ((SubmitWithdrawViewHolder) holder).setAddress(mDefaultWithdrawAddress);
            ((SubmitWithdrawViewHolder) holder).setBalance(mBalanceValue, mReservedValue, mAvailableValue, mCurrency);
        } else if (holder instanceof WithdrawalViewHolder) {
            ((WithdrawalViewHolder) holder).onBind(new JsonWrapper(mPager.getLoadedData().get(position - getHeadersCount()).getAsJsonObject()));
        } else if (holder instanceof Header && getItemCount() > 2 && getItemViewType(position) != VIEW_TYPE_BOTTOM) {
            ((Header) holder).setBackgroundDrawable(position == 1 ? R.drawable.list_item_header_background_top : R.drawable.list_item_header_background_middle);
            TextView subtitleView = holder.itemView.findViewById(R.id.status_subtitle);
            subtitleView.setAllCaps(false);
        }
    }

    @Override
    protected RecyclerView.ViewHolder createEmptyViewHolder(ViewGroup parent) {
        return new RecyclerView.ViewHolder(new View(parent.getContext())) {
        };
    }

    @Override
    protected int getHeadersCount() {
        return 1;
    }

    class SubmitWithdrawViewHolder extends RecyclerView.ViewHolder {

        private TextView mAddressView;
        private TextView mAmountView;
        private TextView mSubmitView;
        private TextView mCryptoAvailableView;
        private TextView mCurrencyView;
        private TextView mAvailableView;
        private View mInfoView;

        SubmitWithdrawViewHolder(View itemView) {
            super(itemView);
            mAddressView = itemView.findViewById(R.id.eth_address_input);
            mAmountView = itemView.findViewById(R.id.amount_input);
            mSubmitView = itemView.findViewById(R.id.submit);
            mInfoView = itemView.findViewById(R.id.info);
            mCryptoAvailableView = itemView.findViewById(R.id.crypto_available);
            mCurrencyView = itemView.findViewById(R.id.currency);
            mAvailableView = itemView.findViewById(R.id.currency_available);

            mSubmitView.setOnClickListener(v -> {
                String address = mAddressView.getText().toString();
                if (!checkEthereum(address)) {
                    mAddressView.setError(itemView.getContext().getString(R.string.invalid_ethereum_address_error));
                    return;
                }

                String amountString = mAmountView.getText().toString();
                float amount = !TextUtils.isEmpty(amountString) ? Float.parseFloat(mAmountView.getText().toString()) / 1000 : 0;

                if (amount <= 0 || amount > mAvailableValue) {
                    mAmountView.setError(itemView.getContext().getResources().getString(R.string.invalid_withdraw_amount_error, mAvailableValue * 1000));
                    return;
                }

                mWithdrawActivity.requestWithdraw(address, amount);
                mAmountView.setText(null);

            });
            mInfoView.setOnClickListener(v -> mWithdrawActivity.showWithdrawInfo());
        }

        void setAddress(String address) {
            if (TextUtils.isEmpty(mAddressView.getText())) {
                mAddressView.setText(address);
            }
        }

        void setBalance(float cryptoBalance, float reserved, float available, String currency ) {
            mAmountView.setHint(itemView.getContext().getResources().getString(R.string.eth_amount_format_string, available * 1000));
            mSubmitView.setEnabled(mAvailableValue > 0f);

            int stringId = cryptoBalance > 1 ? R.string.ethereum : R.string.milli_ethereum;
            String cryptoCurrency = itemView.getContext().getString(stringId);
            switch (stringId) {
                case R.string.ethereum:
                    mCryptoAvailableView.setText(String.format(Locale.US, "%.2f", available));
                    break;
                case R.string.milli_ethereum:
                    mCryptoAvailableView.setText(String.format(Locale.US, "%d", Math.round(available * 1000)));
                    break;

            }
            mCurrencyView.setText(cryptoCurrency);
            mAvailableView.setText(itemView.getContext().getString(R.string.amount_format_string
                    , AmountCurrencyUtil.getCurrencySign(mCurrency)
                    , Math.round(available * mRate)));
        }

        private boolean checkEthereum(String address) {
            return Pattern.matches("^0x[a-fA-F0-9]{40}$", address);
        }
    }

    class WithdrawalViewHolder extends RecyclerView.ViewHolder {

        private TextView mDateView;
        private TextView mAddressView;
        private TextView mAmount;
        private View mNewIndicator;

        WithdrawalViewHolder(View itemView) {
            super(itemView);
            mDateView = itemView.findViewById(R.id.date);
            mAddressView = itemView.findViewById(R.id.address);
            mAmount = itemView.findViewById(R.id.amount);
            mNewIndicator = itemView.findViewById(R.id.new_indicator);
        }

        void onBind(JsonWrapper item) {
            mDateView.setText(TeambrellaDateUtils.getDatePresentation(mDateView.getContext()
                    , TeambrellaDateUtils.TEAMBRELLA_UI_DATE_SHORT
                    , item.getString(TeambrellaModel.ATTR_DATA_WITHDRAWAL_DATE)));
            mAddressView.setText(item.getString(TeambrellaModel.ATTR_REQUEST_TO_ADDRESS));
            mAmount.setText(mDateView.getContext().getString(R.string.eth_amount_short_format_string, 1000 * item.getFloat(TeambrellaModel.ATTR_DATA_AMOUNT)));
            mNewIndicator.setVisibility(item.getBoolean(TeambrellaModel.ATTR_DATA_IS_NEW, false) ? View.VISIBLE : View.INVISIBLE);
        }
    }
}
