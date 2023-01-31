package com.keystone.cold.remove_wallet_mode.ui.fragment.main.tx.bitcoin;

import android.os.Bundle;

import androidx.lifecycle.ViewModelProviders;

import com.keystone.coinlib.utils.Coins;
import com.keystone.cold.R;
import com.keystone.cold.remove_wallet_mode.constant.BundleKeys;
import com.keystone.cold.remove_wallet_mode.exceptions.BaseException;
import com.keystone.cold.remove_wallet_mode.ui.fragment.main.tx.ConfirmTransactionFragment;
import com.keystone.cold.remove_wallet_mode.ui.fragment.main.tx.RawTxFragment;
import com.keystone.cold.remove_wallet_mode.viewmodel.tx.BitcoinTxViewModel;
import com.keystone.cold.ui.modal.ProgressModalDialog;

import java.util.Objects;

public class BitcoinConfirmTransactionFragment extends ConfirmTransactionFragment<BitcoinTxViewModel> {
    ProgressModalDialog dialog = ProgressModalDialog.newInstance();

    @Override
    protected void initViewModel() {
        viewModel = ViewModelProviders.of(this).get(BitcoinTxViewModel.class);
        viewModel.reset();
        viewModel.parseTxException().observe(this, this::handleParseException);
        viewModel.parseTxData(requireArguments());
    }

    @Override
    protected TabLayoutConfig[] getTabLayouts() {
        TabLayoutConfig[] configs = new TabLayoutConfig[2];
        configs[0] = new TabLayoutConfig(getString(R.string.overview), BitcoinTransactionDetailsFragment.newInstance(requireArguments(), viewModel.getObservablePsbt()));
        configs[1] = new TabLayoutConfig(getString(R.string.raw_data), RawTxFragment.newInstance(requireArguments(), viewModel.getRawFormatTx()));
        return configs;
    }

    @Override
    protected void setupView() {
        mBinding.toolbar.setNavigationOnClickListener((v) -> navigateUp());
        dialog.show(Objects.requireNonNull(mActivity.getSupportFragmentManager()), "");
        mBinding.sign.setOnClickListener(v -> {
            handleSign();
        });
        viewModel.getObservablePsbt().observe(this, (v) -> {
            if (v == null) return;
            dialog.dismiss();
        });
    }

    private void handleParseException(BaseException ex) {
        if (ex != null) {
            dialog.dismiss();
            ex.printStackTrace();
            alertException(ex, () -> {
                popBackStack(R.id.myAssetsFragment, false);
            });
            viewModel.parseTxException().setValue(null);
        }
    }

    @Override
    protected void onSignSuccess() {
        String signatureURString = viewModel.getSignatureUR();
        Bundle data = new Bundle();
        data.putString(BundleKeys.SIGNATURE_UR_KEY, signatureURString);
        data.putString(BundleKeys.COIN_CODE_KEY, viewModel.getCoinCode());
        navigate(R.id.action_to_broadCastTxFragment, data);
        viewModel.getSignState().setValue("");
        viewModel.getSignState().removeObservers(this);
    }
}