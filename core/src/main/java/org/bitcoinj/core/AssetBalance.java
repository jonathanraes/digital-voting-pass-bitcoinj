package org.bitcoinj.core;

import java.util.ArrayList;

/**
 * Creates everything that is needed to build a new transaction on the blockchain.
 * Balance is returned by Wallet.getAssetBalance
 */
public class AssetBalance extends ArrayList<TransactionOutput> {

    private Asset asset;
    private Address address;
    private float balance = 0;
    private ArrayList<TransactionOutput> unspentOutputs;

    /**
     * Constructs a new asset balance for the given address
     * @param balance
     * @param unspentOutputs
     */
    public AssetBalance(Asset asset, Address address) {
        this.asset = asset;
        this.address = address;
    }

    public void addAmount(float amount) {
        this.balance += amount;
    }

    public void addTxo(TransactionOutput txo) {
        this.add(txo);
    }

    public float getBalance() {
        return this.balance;
    }

    public String toString() {
        return "Asset balance " +asset.getName() + ": \n" + address + ": " + balance + " (" + this.size() + " outputs)";
    }

}
