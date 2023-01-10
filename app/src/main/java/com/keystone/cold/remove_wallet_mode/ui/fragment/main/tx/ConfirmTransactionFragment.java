package com.keystone.cold.remove_wallet_mode.ui.fragment.main.tx;

import static androidx.fragment.app.FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT;

import static com.keystone.cold.ui.fragment.setup.PreImportFragment.ACTION;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;

import com.keystone.cold.R;
import com.keystone.cold.databinding.FragmentConfirmTransactionBinding;
import com.keystone.cold.remove_wallet_mode.constant.UIConstants;
import com.keystone.cold.remove_wallet_mode.viewmodel.tx.BaseTxViewModel;
import com.keystone.cold.ui.fragment.BaseFragment;
import com.keystone.cold.ui.fragment.setup.PreImportFragment;
import com.keystone.cold.ui.modal.SigningDialog;
import com.keystone.cold.viewmodel.tx.AptosViewModel;

public abstract class ConfirmTransactionFragment<V extends BaseTxViewModel> extends BaseFragment<FragmentConfirmTransactionBinding> {

    private SigningDialog signingDialog;
    protected V viewModel;

    protected final Runnable forgetPassword = () -> {
        Bundle bundle = new Bundle();
        bundle.putString(ACTION, PreImportFragment.ACTION_RESET_PWD);
        navigate(R.id.action_to_preImportFragment, bundle);
    };

    @Override
    protected int setView() {
        return R.layout.fragment_confirm_transaction;
    }

    @Override
    protected void init(View view) {
        initViewModel();
        setupView();
        setupViewPager();
    }

    protected abstract void initViewModel();

    protected abstract TabLayoutConfig[] getTabLayouts();

    @Override
    protected void initData(Bundle savedInstanceState) {

    }

    protected abstract void setupView();

    private void setupViewPager() {
        TabLayoutConfig[] configs = getTabLayouts();
        mBinding.transaction.viewPager.setOffscreenPageLimit(configs.length);
        mBinding.transaction.viewPager.setAdapter(new FragmentPagerAdapter(getChildFragmentManager(),
                BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
            @NonNull
            @Override
            public Fragment getItem(int position) {
                return configs[position].fragment;
            }

            @Override
            public int getCount() {
                return configs.length;
            }

            @Override
            public CharSequence getPageTitle(int position) {
                return configs[position].name;
            }
        });
        mBinding.transaction.tab.setupWithViewPager(mBinding.transaction.viewPager);
    }


    protected void subscribeSignState() {
        viewModel.getSignState().observe(this, s -> {
            if (AptosViewModel.STATE_SIGNING.equals(s)) {
                signingDialog = SigningDialog.newInstance();
                signingDialog.show(mActivity.getSupportFragmentManager(), "");
            } else if (AptosViewModel.STATE_SIGN_SUCCESS.equals(s)) {
                if (signingDialog != null) {
                    signingDialog.setState(SigningDialog.STATE_SUCCESS);
                }
                new Handler().postDelayed(() -> {
                    if (signingDialog != null) {
                        signingDialog.dismiss();
                    }
                    signingDialog = null;
                    onSignSuccess();
                }, UIConstants.SIGN_DIALOG_SUCCESS_DELAY);
            } else if (AptosViewModel.STATE_SIGN_FAIL.equals(s)) {
                new Handler().postDelayed(() -> {
                    if (signingDialog != null) {
                        signingDialog.setState(SigningDialog.STATE_FAIL);
                    }
                }, UIConstants.SIGN_DIALOG_FAIL_DELAY);
                new Handler().postDelayed(() -> {
                    if (signingDialog != null) {
                        signingDialog.dismiss();
                    }
                    signingDialog = null;
                    viewModel.getSignState().removeObservers(this);
                }, UIConstants.SIGN_DIALOG_REMOVE_OBSERVERS_DELAY);
            }
        });
    }

    protected abstract void onSignSuccess();


    protected static class TabLayoutConfig {
        private final String name;
        private final Fragment fragment;

        public TabLayoutConfig(String name, Fragment fragment) {
            this.name = name;
            this.fragment = fragment;
        }

        public String getName() {
            return name;
        }

        public Fragment getFragment() {
            return fragment;
        }
    }
}
