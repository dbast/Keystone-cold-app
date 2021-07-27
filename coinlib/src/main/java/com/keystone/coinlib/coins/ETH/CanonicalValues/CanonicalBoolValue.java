package com.keystone.coinlib.coins.ETH.CanonicalValues;

import com.esaulpaugh.headlong.abi.ABIType;

import org.json.JSONException;
import org.json.JSONObject;

public class CanonicalBoolValue extends CanonicalValue {
    CanonicalBoolValue(ABIType canonicalType) {
        super(canonicalType);
    }

    @Override
    public void resolveValueToJSONObject(Object value, JSONObject jsonObject) throws JSONException {
        jsonObject.put("value", value);
    }
}
