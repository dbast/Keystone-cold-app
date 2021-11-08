package com.keystone.coinlib.accounts;

import java.util.regex.Pattern;

public enum ETHAccount {
    LEDGER_LIVE("M/44'/60'", 0x3c, "Ledger Live", "ledger_live"),
    LEDGER_LEGACY("M/44'/60'/0'", 0x3c, "Ledger Legacy", "ledger_legacy"),
    BIP44_STANDARD("M/44'/60'/0'", 0x3c, "BIP44 Standard", "standard");

    private String path;
    private int type;
    private String name;
    private String code;

    ETHAccount(String path, int type, String name, String code) {
        this.path = path;
        this.type = type;
        this.name = name;
        this.code = code;
    }

    public static ETHAccount ofCode(String code) {
        if(code.equals(LEDGER_LIVE.code)) return LEDGER_LIVE;
        if(code.equals(LEDGER_LEGACY.code)) return LEDGER_LEGACY;
        if(code.equals(BIP44_STANDARD.code)) return BIP44_STANDARD;
        throw new RuntimeException("invalid eth account code: " + code);
    }

    public String getPath() {
        return path;
    }

    public int getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    private boolean isLedgerLiveChildren(String path) {
        return Pattern.matches("^M/44'/60'/\\d+'/0/0", path);
    }

    private boolean isLedgerLegacyChildren(String path) {
        return Pattern.matches("^M/44'/60'/0'/\\d+", path);
    }

    private boolean isStandardChildren(String path) {
        return Pattern.matches("^M/44'/60'/0'/0/\\d+", path);
    }


    public boolean isChildrenPath(String path) {
        if (!path.toUpperCase().startsWith("M/")) {
            path = "M/" + path;
        }
        switch (this) {
            case LEDGER_LIVE:
                return isLedgerLiveChildren(path);
            case LEDGER_LEGACY:
                return isLedgerLegacyChildren(path);
            default:
                return isStandardChildren(path);
        }
    }
}
