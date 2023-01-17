package com.keystone.cold.remove_wallet_mode.helper.address_generators;

import com.keystone.coinlib.coins.AbsDeriver;
import com.keystone.cold.callables.GetExtendedPublicKeyCallable;
import com.keystone.cold.db.entity.AddressEntity;
import com.keystone.cold.remove_wallet_mode.viewmodel.tx.BitcoinTxViewModel;

public class BitcoinNativeSegwitAddressGenerator extends BaseAddressGenerator {
    @Override
    protected String deriveAddress(int index, AddressEntity addressEntity, AbsDeriver deriver) {
        String path = BitcoinTxViewModel.BTCNativeSegwitPath + "/0/" + index;
        String xPub = new GetExtendedPublicKeyCallable(path).call();
        String address = deriver.derive(xPub);
        addressEntity.setPath(path);
        return address;
    }
}
