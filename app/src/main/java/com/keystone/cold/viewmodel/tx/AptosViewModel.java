package com.keystone.cold.viewmodel.tx;

import static com.keystone.cold.ui.fragment.main.AssetFragment.HD_PATH;
import static com.keystone.cold.ui.fragment.main.AssetFragment.REQUEST_ID;
import static com.keystone.cold.ui.fragment.main.AssetFragment.SIGN_DATA;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.keystone.coinlib.accounts.ExtendedPublicKey;
import com.keystone.coinlib.utils.Coins;
import com.keystone.cold.DataRepository;
import com.keystone.cold.MainApplication;
import com.keystone.cold.db.entity.AddressEntity;
import com.keystone.coinlib.coins.APTOS.AptosImpl;
import com.keystone.coinlib.coins.SignTxResult;
import com.keystone.coinlib.interfaces.Signer;
import com.keystone.cold.AppExecutors;
import com.keystone.cold.callables.ClearTokenCallable;
import com.keystone.cold.db.entity.TxEntity;
import com.keystone.cold.encryption.ChipSigner;
import com.keystone.cold.ui.fragment.main.aptos.model.AptosTxData;
import com.sparrowwallet.hummingbird.registry.aptos.AptosSignature;

import org.json.JSONException;
import org.json.JSONObject;
import org.spongycastle.util.encoders.Hex;

import java.nio.ByteBuffer;

import java.util.UUID;

public class AptosViewModel extends Base {

    @SuppressLint("StaticFieldLeak")
    private final Context context;

    private String txHex;
    private String messageData;
    private String hdPath;

    private String requestId;
    private String signature;

    private String xPub;

    private final MutableLiveData<JSONObject> parseMessageJsonLiveData;
    private final MutableLiveData<AptosTxData> aptosTxDataMutableLiveData;


    public AptosViewModel(@NonNull Application application) {
        super(application);
        context = application.getApplicationContext();
        coinCode = "APTOS";
        parseMessageJsonLiveData = new MutableLiveData<>();
        aptosTxDataMutableLiveData = new MutableLiveData<>();
    }

    private final SignCallBack signCallBack = new SignCallBack() {
        @Override
        public void startSign() {
            signState.postValue(STATE_SIGNING);
        }

        @Override
        public void onFail() {
            signState.postValue(STATE_SIGN_FAIL);
            new ClearTokenCallable().call();
        }

        @Override
        public void onSignTxSuccess(String signatureHex) {
            signature = signatureHex;
            signState.postValue(STATE_SIGN_SUCCESS);
            new ClearTokenCallable().call();
            insertDB(signature, txHex);
        }

        @Override
        public void onSignMsgSuccess(String signatureHex) {
            signature = signatureHex;
            signState.postValue(STATE_SIGN_SUCCESS);
            new ClearTokenCallable().call();
        }
    };

    private void signTransaction(Signer signer) {
        signCallBack.startSign();
        SignTxResult result = new AptosImpl().signHex(txHex, signer);
        if (result == null) {
            signCallBack.onFail();
        } else {
            signCallBack.onSignTxSuccess(result.signaturHex);
        }
    }

    private void signMessage(Signer signer) {
        signCallBack.startSign();
        String result = new AptosImpl().signMessage(messageData, signer);
        if (result == null) {
            signCallBack.onFail();
        } else {
            signCallBack.onSignMsgSuccess(result);
        }
    }


    private Signer initSigner() {
        String authToken = getAuthToken();
        if (TextUtils.isEmpty(authToken)) {
            Log.w(TAG, "authToken null");
            return null;
        }
        return new ChipSigner(hdPath.toLowerCase(), authToken);
    }

    public void handleSign() {
        AppExecutors.getInstance().diskIO().execute(() -> {
            Signer signer = initSigner();
            signTransaction(signer);
        });
    }

    public MutableLiveData<JSONObject> getParseMessageJsonLiveData() {
        return parseMessageJsonLiveData;
    }

    public MutableLiveData<AptosTxData> getAptosTxDataMutableLiveData() {
        return aptosTxDataMutableLiveData;
    }

    public void parseTxData(Bundle bundle) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            txHex = bundle.getString(SIGN_DATA);
            hdPath = bundle.getString(HD_PATH);
            requestId = bundle.getString(REQUEST_ID);
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("raw", txHex);
            } catch (JSONException exception) {
                exception.printStackTrace();
            }
            parseMessageJsonLiveData.postValue(jsonObject);
            xPub = getXpubByPath(hdPath);
        });
    }


    public String getSignatureJson() {
        JSONObject signed = new JSONObject();
        try {
            signed.put("signature", signature);
            signed.put("requestId", requestId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return signed.toString();
    }

    @Override
    public String getTxId() {
        return signature;
    }


    public String getSignatureUR() {
        if (TextUtils.isEmpty(signature) || TextUtils.isEmpty(requestId)) {
            return "";
        }
        byte[] signatureByte = Hex.decode(signature);
        UUID uuid = UUID.fromString(requestId);
        ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[16]);
        byteBuffer.putLong(uuid.getMostSignificantBits());
        byteBuffer.putLong(uuid.getLeastSignificantBits());
        byte[] requestId = byteBuffer.array();
        byte[] publicKey = null;
        if (xPub != null) {
            publicKey = getPublicKey(xPub);
        }
        AptosSignature aptosSignature = new AptosSignature(signatureByte, requestId, publicKey);
        return aptosSignature.toUR().toString();
    }


    private String getXpubByPath(String path) {
        DataRepository repository = MainApplication.getApplication().getRepository();
        AddressEntity addressEntity = repository.loadAddressBypath(path);
        if (addressEntity != null) {
            String addition = addressEntity.getAddition();
            try {
                JSONObject rootJson = new JSONObject(addition);
                JSONObject additionJson = rootJson.getJSONObject("addition");
                return additionJson.getString("xPub");
            } catch (JSONException exception) {
                exception.printStackTrace();
            }
        }
        return null;
    }

    private byte[] getPublicKey(String xPub) {
        ExtendedPublicKey extendedPublicKey = new ExtendedPublicKey(xPub);
        byte[] key = extendedPublicKey.getKey();
        byte[] publicKey = new byte[32];
        System.arraycopy(key, 1, publicKey, 0, 32);
        return publicKey;
    }

    private void insertDB(String signature, String rawMessage) {
        TxEntity txEntity = generateAptosTxEntity();
        txEntity.setTxId(signature);
        String additionsString = null;
        try {
            JSONObject addition = new JSONObject();
            addition.put("signature", signature);
            addition.put("raw_message", rawMessage);
            JSONObject additions = new JSONObject();
            additions.put("coin", Coins.APTOS.coinId());
            additions.put("addition", addition);
            JSONObject root = new JSONObject();
            root.put("additions", additions);
            additionsString = root.toString();
        } catch (JSONException exception) {
            exception.printStackTrace();
        }
        if (!TextUtils.isEmpty(additionsString)) {
            //addition结构详见 com.keystone.cold.db.entity.TxEntity addition字段
            txEntity.setAddition(additionsString);
            mRepository.insertTx(txEntity);
        }
    }

    private TxEntity generateAptosTxEntity() {
        TxEntity txEntity = new TxEntity();
        txEntity.setCoinId(Coins.APTOS.coinId());
        txEntity.setSignId(watchWallet.getSignId());
        txEntity.setCoinCode(Coins.APTOS.coinCode());
        txEntity.setTimeStamp(getUniversalSignIndex(getApplication()));
        txEntity.setBelongTo(mRepository.getBelongTo());
        txEntity.setSignedHex(getSignatureUR());
        return txEntity;
    }

    public void parseAptosTxEntity(TxEntity txEntity) {
        String addition = txEntity.getAddition();
        if (TextUtils.isEmpty(addition)) {
            return;
        }
        try {
            JSONObject root = new JSONObject(addition);
            JSONObject additions = root.getJSONObject("additions");
            String coin = additions.getString("coin");
            if (!TextUtils.isEmpty(coin) && coin.equals(Coins.APTOS.coinId())) {
                String signature = additions.getJSONObject("addition").getString("signature");
                String rawMessage = additions.getJSONObject("addition").getString("raw_message");
                AptosTxData aptosTxData = new AptosTxData();
                aptosTxData.setSignature(signature);
                JSONObject rawData = new JSONObject();
                rawData.put("raw", rawMessage);
                aptosTxData.setRawMessage(rawData.toString(2));
                aptosTxData.setSignatureUR(txEntity.getSignedHex());
                aptosTxDataMutableLiveData.postValue(aptosTxData);
            }
        } catch (JSONException exception) {
            exception.printStackTrace();
        }
    }

    interface SignCallBack {
        void startSign();

        void onFail();

        void onSignTxSuccess(String signatureHex);

        void onSignMsgSuccess(String signatureHex);
    }

}