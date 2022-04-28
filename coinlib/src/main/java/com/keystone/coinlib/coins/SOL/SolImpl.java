package com.keystone.coinlib.coins.SOL;

import android.util.Log;

import androidx.annotation.NonNull;

import com.keystone.coinlib.coins.AbsTx;
import com.keystone.coinlib.coins.SignTxResult;
import com.keystone.coinlib.interfaces.Coin;
import com.keystone.coinlib.interfaces.SignCallback;
import com.keystone.coinlib.interfaces.Signer;
import com.keystone.coinlib.utils.Coins;

import org.json.JSONException;
import org.json.JSONObject;


public class SolImpl implements Coin {
    protected static native Void nativeParseMessage(final String message, final ParseMessageCallback callback);

    static {
        System.loadLibrary("CryptoCoinLib_v0_1_2");
    }

    @Override
    public String coinCode() {
        return Coins.SOL.coinCode();
    }

    @Override
    public void generateTransaction(@NonNull AbsTx tx, SignCallback callback, Signer... signers) {

    }

    @Override
    public String signMessage(@NonNull String message, Signer signer) {
        return null;
    }

    @Override
    public String generateAddress(@NonNull String publicKey) {
        return null;
    }

    @Override
    public boolean isAddressValid(@NonNull String address) {
        return false;
    }

    public SignTxResult signHex(String hex, Signer signer) {
        String signature = signer.sign(hex);
        if (signature != null) {
            return new SignTxResult(signature, hex, signature);
        } else {
            return null;
        }
    }

    public static void parseMessage(String message, ParseMessageCallback parseMessageCallback) {
        SolImpl.nativeParseMessage(message, parseMessageCallback);
    }


    public interface ParseMessageCallback {

        void onSuccess(String json);

        void onFailed();

    }
}