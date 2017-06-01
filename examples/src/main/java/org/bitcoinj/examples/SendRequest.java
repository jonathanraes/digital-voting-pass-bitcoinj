/*
 * Copyright by the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.bitcoinj.examples;

import org.bitcoinj.core.*;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.MultiChainParams;
import org.bitcoinj.params.RegTestParams;
import org.bitcoinj.script.Script;
import org.bitcoinj.signers.LocalTransactionSigner;
import org.bitcoinj.signers.TransactionSigner;
import org.bitcoinj.utils.BriefLogFormatter;
import org.bitcoinj.wallet.KeyBag;
import org.bitcoinj.wallet.RedeemData;
import org.bitcoinj.wallet.Wallet;

import java.io.File;
import java.math.BigInteger;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/**
 * The following example shows you how to create a SendRequest to send coins from a wallet to a given address.
 */
public class SendRequest {
    private static Address forwardingAddress;
    private static WalletAppKit kit;

    public static void main(String[] args) throws Exception {

        args = new String[]{"17UGUPmLNtPRzT7DUHvWmQyLHcoKDUoH99DeEm"};

        // This line makes the log output more compact and easily read, especially when using the JDK log adapter.
        BriefLogFormatter.init();
        if (args.length < 1) {
            System.err.println("Usage: address-to-send-back-to [regtest|testnet]");
            return;
        }

        // Figure out which network we should connect to. Each one gets its own set of files.

        final NetworkParameters params = MultiChainParams.get(
                "00a9b1b476c6909ac1c8b6393a8721052a435e10367aedbda4b92899ec8d6a8b",
                "010000000000000000000000000000000000000000000000000000000000000000000000b2e938f89a844a23ca2c1a7f5c1b20f83b4c92c279f48fdc55960c4bf5020cbe19482459ffff0020750100000101000000010000000000000000000000000000000000000000000000000000000000000000ffffffff1704ffff002001040f4d756c7469436861696e20766f7465ffffffff0200000000000000002f76a9142fdf35a8cac6bb3dc4fa216303fe312b8ed40b8488ac1473706b703731000000000000ffffffff19482459750000000000000000131073706b6e0200040101000104726f6f74756a00000000"
        );

        String filePrefix = "forwarding-service21" + Math.round(Math.random() * 100);

        // Parse the address given as the first parameter.
        forwardingAddress = Address.fromBase58(params, args[0]);

        // Start up a basic app using a class that automates some boilerplate.
        kit = new WalletAppKit(params, new File("."), filePrefix);

        if (params == RegTestParams.get()) {
            // Regression test mode is designed for testing and development only, so there's no public network for it.
            // If you pick this mode, you're expected to be running a local "bitcoind -regtest" instance.
            kit.connectToLocalHost();
        }

        PeerAddress peer = new PeerAddress(params, InetAddress.getByName("188.226.149.56"));


        kit.setPeerNodes(peer);

        //  Download the block chain and wait until it's done.
        kit.startAsync();
        kit.awaitRunning();

        final ECKey passportKey = ECKey.fromPrivate(new BigInteger("19d8d4c341564e5acc3cb0dd6bb58bb65169d8ec4672dd039d039501b51a2bc422a053af847b7ff3", 16));
        Address from = Address.fromBase58(params, "13KuMB2ToFLiP1gZwhRwA8nHQFqKjAZKT1QpUh");
        Address to   = Address.fromBase58(params, "1BNgsh93GLtqbNaN78yU5BnXwnonhkGrvwzMjZ");

        ArrayList<ECKey> passportKeys = new ArrayList<ECKey>();
        passportKeys.add(passportKey);

        Wallet wallet = new Wallet(params);
        wallet.importKeys(passportKeys);


        ArrayList<Asset> assets = kit.wallet().getAvailableAssets();
        AssetBalance abc = kit.wallet().getAssetBalance(assets.get(2), from);

        Transaction transaction = new Transaction(params);
        TransactionOutput input = abc.get(0);
        transaction.addInput(input.getParentTransactionHash(), 0, new Script(input.getScriptPubKey().getChunks().get(2).data));
        transaction.addOutput(Coin.ZERO, to);

//        Coin votingToken = Coin.parseCoin("1");
//        Address to = Address.fromBase58(params, "132GWwDzwfsohncVme4tBJPebMVz41KNCzruQL");
//
//        TransactionOutput output = new TransactionOutput(params, null, votingToken, from);



//        for (Transaction tx : kit.wallet().getTransactions(true)) {
//            System.out.println(tx);
//        }

//        ECKey destination = ECKey.fromPrivate(new BigInteger("1a9d8ac27bc6229cc0dc62afc216d64b67d7241305980a4ce7ca699722be93df2eae9204c64b2c31", 16));
//
//

        KeyBag bag = new KeyBag() {
            @Override
            public ECKey findKeyFromPubHash(byte[] pubkeyHash) {
                return passportKey;
            }

            @Override
            public ECKey findKeyFromPubKey(byte[] pubkey) {
                return passportKey;
            }

            @Override
            public RedeemData findRedeemDataFromScriptHash(byte[] scriptHash) {
                return null;
            }

        };


        TransactionSigner.ProposedTransaction proposal = new TransactionSigner.ProposedTransaction(transaction);
        TransactionSigner signer = new LocalTransactionSigner();
        signer.signInputs(proposal, bag);


        Wallet.SendResult result = new Wallet.SendResult();
        result.tx = transaction;
        // The tx has been committed to the pending pool by this point (via sendCoinsOffline -> commitTx), so it has
        // a txConfidenceListener registered. Once the tx is broadcast the peers will update the memory pool with the
        // count of seen peers, the memory pool will update the transaction confidence object, that will invoke the
        // txConfidenceListener which will in turn invoke the wallets event listener onTransactionConfidenceChanged
        // method.
        result.broadcast = kit.peerGroup().broadcastTransaction(transaction);
        result.broadcastComplete = result.broadcast.future();

        System.out.println(result);

        TimeUnit.SECONDS.sleep(60);

    }
}
