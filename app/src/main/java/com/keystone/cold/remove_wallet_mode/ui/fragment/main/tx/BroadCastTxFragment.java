package com.keystone.cold.remove_wallet_mode.ui.fragment.main.tx;

import android.os.Bundle;
import android.view.View;

import com.keystone.cold.R;
import com.keystone.cold.databinding.FragmentBroadcastTxBinding;
import com.keystone.cold.remove_wallet_mode.constant.BundleKeys;
import com.keystone.cold.ui.fragment.BaseFragment;

public class BroadCastTxFragment extends BaseFragment<FragmentBroadcastTxBinding> {

    @Override
    protected int setView() {
        return R.layout.fragment_broadcast_tx;
    }

    @Override
    protected void init(View view) {
        Bundle data = requireArguments();
        String signatureURString = data.getString(BundleKeys.SIGNATURE_UR_KEY);
        String coinCode = data.getString(BundleKeys.COIN_CODE_KEY);
        mBinding.qrcodeLayout.qrcode.setData(signatureURString);
        mBinding.setCoinCode(coinCode);
        mBinding.toolbar.setNavigationOnClickListener(v -> popBackStack(R.id.myAssetsFragment, false));
        mBinding.complete.setOnClickListener(v -> popBackStack(R.id.myAssetsFragment, false));

    }

    @Override
    protected void initData(Bundle savedInstanceState) {

    }
}
