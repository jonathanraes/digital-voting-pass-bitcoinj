/*
 * Copyright 2011 Google Inc.
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

package org.bitcoinj.core;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Arrays;

import com.google.common.base.Objects;
import com.google.common.primitives.Ints;
import com.google.common.primitives.UnsignedBytes;

/**
 * <p>In Bitcoin the following format is often used to represent some type of key:</p>
 * <p/>
 * <pre>[one version byte] [data bytes] [4 addressChecksum bytes]</pre>
 * <p/>
 * <p>and the result is then Base58 encoded. This format is used for addresses, and private keys exported using the
 * dumpprivkey command.</p>
 */
public class VersionedChecksummedBytes implements Serializable, Cloneable, Comparable<VersionedChecksummedBytes> {
    protected final int version;
    protected int addressChecksum;
    protected byte[] bytes;

    protected VersionedChecksummedBytes(String encoded) throws AddressFormatException {
        byte[] versionAndDataBytes = Base58.decodeChecked(encoded);
        if (versionAndDataBytes.length == 20) {
            version = java.nio.ByteBuffer.wrap(new byte[]{versionAndDataBytes[0]}).getInt();
            bytes = new byte[versionAndDataBytes.length - 1];
            System.arraycopy(versionAndDataBytes, 1, bytes, 0, versionAndDataBytes.length - 1);
        } else if (versionAndDataBytes.length == 24) {
            version = java.nio.ByteBuffer.wrap(new byte[]{
                    versionAndDataBytes[0],
                    versionAndDataBytes[6],
                    versionAndDataBytes[12],
                    versionAndDataBytes[18]
            }).getInt();
            bytes = new byte[versionAndDataBytes.length - 4];
            System.arraycopy(versionAndDataBytes, 1, bytes, 0, 5);
            System.arraycopy(versionAndDataBytes, 7, bytes, 5, 5);
            System.arraycopy(versionAndDataBytes, 13, bytes, 10, 5);
            System.arraycopy(versionAndDataBytes, 19, bytes, 15, 5);
        } else {
            version = 0;
        }

        this.addressChecksum = 0;
    }

    /**
     * Call constructor and set addressChecksum from NetworkParameters
     * @param encoded
     * @param addressChecksum
     */
    protected VersionedChecksummedBytes(String encoded, int addressChecksum) {
        this(encoded);
        this.addressChecksum = addressChecksum;
    }

    protected VersionedChecksummedBytes(int version, byte[] bytes) {
        checkArgument(version >= 0 && version < Integer.MAX_VALUE);
        this.version = version;
        this.addressChecksum = 0;
        this.bytes = bytes;
    }

    protected VersionedChecksummedBytes(int version, int addressChecksum, byte[] bytes) {
        checkArgument(version >= 0 && version < Integer.MAX_VALUE);
        this.version = version;
        this.addressChecksum = addressChecksum;
        this.bytes = bytes;
    }

    /**
     * Returns the base-58 encoded String representation of this
     * object, including version and addressChecksum bytes.
     */
    public final String toBase58() {
        // A stringified buffer is:
        //   1 byte version + data bytes + 4 bytes check code (a truncated hash)
        byte[] versionBytes = BigInteger.valueOf(this.version).toByteArray();
        if (this.version < 256) versionBytes = new byte[] {versionBytes[3]};
        byte[] addressBytes = new byte[versionBytes.length + bytes.length + 4];
        for (int i = 0; i < versionBytes.length; i++) {
            addressBytes[i*((bytes.length/4)+1)] = versionBytes[i];
            System.arraycopy(bytes, i*5, addressBytes, ((bytes.length/4)+1)*i+1, (bytes.length/4));
        }

        byte[] checksum = Sha256Hash.hashTwice(addressBytes, 0, bytes.length + 4);
        byte[] addressChecksum = BigInteger.valueOf(this.addressChecksum).toByteArray();
        for (int i = 0; i < 4; i++) {
            checksum[i] = (byte) (checksum[i] ^ addressChecksum[i]);
        }
        System.arraycopy(checksum, 0, addressBytes, bytes.length + 4, 4);
        return Base58.encode(addressBytes);
    }

    @Override
    public String toString() {
        return toBase58();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(version, Arrays.hashCode(bytes));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VersionedChecksummedBytes other = (VersionedChecksummedBytes) o;
        return this.version == other.version && Arrays.equals(this.bytes, other.bytes);
    }

    /**
     * {@inheritDoc}
     *
     * This implementation narrows the return type to <code>VersionedChecksummedBytes</code>
     * and allows subclasses to throw <code>CloneNotSupportedException</code> even though it
     * is never thrown by this implementation.
     */
    @Override
    public VersionedChecksummedBytes clone() throws CloneNotSupportedException {
        return (VersionedChecksummedBytes) super.clone();
    }

    /**
     * {@inheritDoc}
     *
     * This implementation uses an optimized Google Guava method to compare <code>bytes</code>.
     */
    @Override
    public int compareTo(VersionedChecksummedBytes o) {
        int result = Ints.compare(this.version, o.version);
        return result != 0 ? result : UnsignedBytes.lexicographicalComparator().compare(this.bytes, o.bytes);
    }

    /**
     * Returns the "version" or "header" byte: the first byte of the data. This is used to disambiguate what the
     * contents apply to, for example, which network the key or address is valid on.
     *
     * @return A positive number between 0 and 255.
     */
    public int getVersion() {
        return version;
    }
}
