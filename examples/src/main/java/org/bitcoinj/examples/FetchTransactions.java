/*
 * Copyright 2012 Google Inc.
 * Copyright 2014 Andreas Schildbach
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
import org.bitcoinj.params.MultiChainParams;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.store.MemoryBlockStore;
import org.bitcoinj.utils.BriefLogFormatter;
import com.google.common.util.concurrent.ListenableFuture;

import java.net.InetAddress;
import java.util.List;

/**
 * Downloads the given transaction and its dependencies from a peers memory pool then prints them out.
 */
public class FetchTransactions {
    public static void main(String[] args) throws Exception {
        System.out.println("Connecting to node");

        final NetworkParameters params = MultiChainParams.get(
                "0072c94584c54627a7d9421e12a68e2318305311feacf70b69eae653e9d197c6",
                "0100000000000000000000000000000000000000000000000000000000000000000000009ce9a108bcae325057ff5f5e763f5fff48ce85281d436e5ad98934eca7d4d66573e81e59ffff0020d40000000101000000010000000000000000000000000000000000000000000000000000000000000000ffffffff1704ffff002001040f4d756c7469436861696e20766f7465ffffffff0200000000000000002f76a914169aa3da3bbc8253d9c164a761ee10a08bc8e49c88ac1473706b703731000000000000ffffffff73e81e59750000000000000000131073706b6e0200040101000104726f6f74756a00000000"
        );

        BlockStore blockStore = new MemoryBlockStore(params);
        BlockChain chain = new BlockChain(params, blockStore);
        PeerGroup peerGroup = new PeerGroup(params, chain);
        peerGroup.start();
        peerGroup.addAddress(new PeerAddress(params, InetAddress.getByName("188.226.149.56")));
        peerGroup.waitForPeers(1).get();
        Peer peer = peerGroup.getConnectedPeers().get(0);

        Sha256Hash txHash = Sha256Hash.wrap("");
        ListenableFuture<Transaction> future = peer.getPeerMempoolTransaction(txHash);
        System.out.println("Waiting for node to send us the requested transaction: " + txHash);
        Transaction tx = future.get();
        System.out.println(tx);

        System.out.println("Waiting for node to send us the dependencies ...");
        List<Transaction> deps = peer.downloadDependencies(tx).get();
        for (Transaction dep : deps) {
            System.out.println("Got dependency " + dep.getHashAsString());
        }

        System.out.println("Done.");
        peerGroup.stop();
    }
}
