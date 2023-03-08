/*
 * Copyright (c) 2021 Keystone
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * in the file COPYING.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.keystone.cold.remove_wallet_mode.ui.fragment.setting;

import static com.keystone.cold.ui.fragment.setup.SetPasswordFragment.PASSWORD;
import static com.keystone.cold.ui.fragment.setup.SetPasswordFragment.SIGNATURE;
import static com.keystone.cold.viewmodel.SetupVaultViewModel.VAULT_STATE_CREATED;
import static com.keystone.cold.viewmodel.SetupVaultViewModel.VAULT_STATE_CREATING;
import static com.keystone.cold.viewmodel.SetupVaultViewModel.VAULT_STATE_CREATING_FAILED;
import static com.keystone.cold.viewmodel.SetupVaultViewModel.VAULT_STATE_NOT_CREATE;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.text.method.TransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.databinding.DataBindingUtil;
import androidx.databinding.Observable;
import androidx.databinding.ObservableField;
import androidx.lifecycle.MutableLiveData;
import androidx.navigation.Navigation;

import com.keystone.cold.AppExecutors;
import com.keystone.cold.R;
import com.keystone.cold.Utilities;
import com.keystone.cold.databinding.ConfirmModalBinding;
import com.keystone.cold.databinding.CreateVaultModalBinding;
import com.keystone.cold.databinding.PassphraseBinding;
import com.keystone.cold.remove_wallet_mode.constant.BundleKeys;
import com.keystone.cold.remove_wallet_mode.helper.CoinConfigHelper;
import com.keystone.cold.remove_wallet_mode.helper.SetupManager;
import com.keystone.cold.remove_wallet_mode.ui.SetupVaultActivity;
import com.keystone.cold.remove_wallet_mode.ui.fragment.setup.SetupVaultBaseFragment;
import com.keystone.cold.remove_wallet_mode.ui.MainActivity;
import com.keystone.cold.ui.modal.ModalDialog;
import com.keystone.cold.util.Keyboard;


public class PassphraseFragment extends SetupVaultBaseFragment<PassphraseBinding> {

    private static final String SPACE = " ";
    private static final int MAX_LENGTH = 128;
    private final ObservableField<String> passphrase1 = new ObservableField<>("");
    private final ObservableField<String> passphrase2 = new ObservableField<>("");
    private ModalDialog dialog;

    @Override
    protected int setView() {
        return R.layout.passphrase;
    }

    @Override
    protected void init(View view) {
        super.init(view);
        mBinding.toolbar.setNavigationOnClickListener(v -> {
            if (mActivity instanceof SetupVaultActivity) {
                mActivity.finish();
            } else {
                Keyboard.hide(mActivity, mBinding.input1);
                navigateUp();
            }
        });
        mBinding.setPassphrase1(passphrase1);
        mBinding.setPassphrase2(passphrase2);
        setFilterSpace(mBinding.input1);
        setFilterSpace(mBinding.input2);
        mBinding.confirm.setOnClickListener(v -> confirmInput());
        Observable.OnPropertyChangedCallback callback = new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable sender, int propertyId) {
                String input1 = passphrase1.get();
                String input2 = passphrase2.get();
                if (input2.length() >= input1.length() && !input1.equals(input2)) {
                    mBinding.inputHint.setText(R.string.passphrase_not_match);
                    mBinding.inputHint.setTextColor(Color.RED);
                } else {
                    mBinding.inputHint.setText(R.string.passphrase_hint3);
                    mBinding.inputHint.setTextColor(mActivity.getResources().getColor(R.color.white40));
                }
            }
        };
        passphrase1.addOnPropertyChangedCallback(callback);
        passphrase2.addOnPropertyChangedCallback(callback);

        mBinding.clickHelper1.setOnClickListener(v -> {
            int selectionPosition = -1;
            if (mBinding.input1.hasFocus()) {
                selectionPosition = mBinding.input1.getSelectionStart();
            }
            TransformationMethod method = mBinding.input1.getTransformationMethod();
            if (method == HideReturnsTransformationMethod.getInstance()) {
                mBinding.ivPassword.setBackgroundResource(R.drawable.ic_hide_password);
                mBinding.input1.setTransformationMethod(PasswordTransformationMethod.getInstance());
            } else {
                mBinding.ivPassword.setBackgroundResource(R.drawable.ic_show_password);
                mBinding.input1.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            }
            if (selectionPosition != -1) {
                mBinding.input1.setSelection(selectionPosition);
            }
        });
        mBinding.clickHelper2.setOnClickListener(v -> {
            int selectionPosition = -1;
            if (mBinding.input2.hasFocus()) {
                selectionPosition = mBinding.input2.getSelectionStart();
            }
            TransformationMethod method = mBinding.input2.getTransformationMethod();
            if (method == HideReturnsTransformationMethod.getInstance()) {
                mBinding.ivPasswordAgagin.setBackgroundResource(R.drawable.ic_hide_password);
                mBinding.input2.setTransformationMethod(PasswordTransformationMethod.getInstance());
            } else {
                mBinding.ivPasswordAgagin.setBackgroundResource(R.drawable.ic_show_password);
                mBinding.input2.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            }
            if (selectionPosition != -1) {
                mBinding.input2.setSelection(selectionPosition);
            }
        });
    }

    private void confirmInput() {
        ModalDialog dialog = new ModalDialog();
        ConfirmModalBinding binding = DataBindingUtil.inflate(LayoutInflater.from(mActivity),
                R.layout.confirm_modal, null, false);
        binding.text.setText(R.string.passphrase_confirm_modal_title);
        binding.text2.setText(R.string.passphrase_confirm_modal_hint);
        binding.close.setOnClickListener(v -> dialog.dismiss());
        binding.confirm.setText(R.string.confirm);
        dialog.setBinding(binding);
        binding.confirm.setOnClickListener(v -> {
            Keyboard.hide(mActivity, mBinding.input1);
            dialog.dismiss();
            updatePassphrase();
        });
        dialog.show(mActivity.getSupportFragmentManager(), "");
    }

    private void updatePassphrase() {
        viewModel.setPassword(getArguments().getString(PASSWORD));
        viewModel.setSignature(getArguments().getString(SIGNATURE));
        subscribeVaultState(viewModel.getVaultCreateState());
        viewModel.updatePassphrase(passphrase1.get());
    }

    private void setFilterSpace(EditText editText) {
        InputFilter lengthFilter = new InputFilter.LengthFilter(MAX_LENGTH);
        editText.setFilters(new InputFilter[]{lengthFilter});
    }

    @Override
    protected void initData(Bundle savedInstanceState) {

    }

    private void subscribeVaultState(MutableLiveData<Integer> stateLiveData) {
        stateLiveData.observe(this, state -> {
            if (state == VAULT_STATE_CREATING) {
                showModal();
            } else if (state == VAULT_STATE_CREATED) {
                Utilities.setVaultCreated(mActivity);
                Utilities.setVaultId(mActivity, viewModel.getVaultId());
                Utilities.setCurrentBelongTo(mActivity,
                        TextUtils.isEmpty(passphrase1.get()) ? "main" : "hidden");
                stateLiveData.setValue(VAULT_STATE_NOT_CREATE);
                viewModel.getVaultCreateState().removeObservers(this);
                AppExecutors.getInstance().diskIO().execute(() -> {
                    SetupManager.setup(() -> {
                        CoinConfigHelper.initCoinConfig();
                        if (dialog != null && dialog.getDialog() != null
                                && dialog.getDialog().isShowing()) {
                            dialog.dismiss();
                        }
                        startActivity(new Intent(mActivity, MainActivity.class));
                        mActivity.finish();
                    });
                });
            } else if (state == VAULT_STATE_CREATING_FAILED) {
                stateLiveData.setValue(VAULT_STATE_NOT_CREATE);
                viewModel.getVaultCreateState().removeObservers(this);
                if (dialog != null && dialog.getDialog() != null
                        && dialog.getDialog().isShowing()) {
                    dialog.dismiss();
                }
            }
        });
    }

    private void showModal() {
        CreateVaultModalBinding binding = DataBindingUtil.inflate(LayoutInflater.from(mActivity),
                R.layout.create_vault_modal, null, false);
        dialog = ModalDialog.newInstance();
        dialog.setBinding(binding);
        dialog.show(mActivity.getSupportFragmentManager(), "");
        String[] steps = mActivity.getResources().getStringArray(R.array.create_vault_step);
        binding.step.setText(steps[0]);
        Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            int i = 0;

            @Override
            public void run() {
                try {
                    handler.postDelayed(this, 8000);
                    binding.step.setText(steps[i]);
                    i++;
                    if (i > 4) {
                        handler.removeCallbacks(this);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("exception...");
                }
            }
        };
        handler.post(runnable);
    }
}