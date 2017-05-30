package org.bitcoinj.core;

import java.util.Arrays;

/**
 * @author Daan Middendorp
 */
public class Asset {

    private String name;
    private byte[] id;

    /**
     * Used to keep track of the assets in the blockchain. Constructor parses the issuanceTxId
     *
     * @param name
     * @param issuanceTxId
     */
    public Asset(String name, Sha256Hash issuanceTxId) {
        this.name = name;

        // Assets are identified by the first 16 bytes of asset’s first issuance txid in reverse order.
        id = issuanceTxId.toBigInteger().toByteArray();
        id = Arrays.copyOfRange(id, 0, 16);
        reverse(id);
    }

    /**
     * Reverses a byte array
     * @param array
     */
    public static void reverse(byte[] array) {
        if (array == null) {
            return;
        }
        int i = 0;
        int j = array.length - 1;
        byte tmp;
        while (j > i) {
            tmp = array[j];
            array[j] = array[i];
            array[i] = tmp;
            j--;
            i++;
        }
    }

    public String getName() {
        return this.name;
    }

    public String getId() {
        return Utils.HEX.encode(this.id);
    }

    public String toString() {
        return this.getId() + ": " + this.getName();
    }

}
