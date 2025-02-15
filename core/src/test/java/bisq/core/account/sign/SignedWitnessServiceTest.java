/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.core.account.sign;


import bisq.core.account.witness.AccountAgeWitness;
import bisq.core.arbitration.ArbitratorManager;
import bisq.core.arbitration.DisputeManager;

import bisq.network.p2p.storage.persistence.AppendOnlyDataStoreService;

import bisq.common.crypto.Sig;
import bisq.common.util.Utilities;

import org.bitcoinj.core.ECKey;

import com.google.common.base.Charsets;

import java.security.KeyPair;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SignedWitnessServiceTest {
    private SignedWitnessService signedWitnessService;
    private byte[] account1DataHash;
    private byte[] account2DataHash;
    private byte[] account3DataHash;
    private AccountAgeWitness aew1;
    private AccountAgeWitness aew2;
    private AccountAgeWitness aew3;
    private byte[] signature1;
    private byte[] signature2;
    private byte[] signature3;
    private byte[] signer1PubKey;
    private byte[] signer2PubKey;
    private byte[] signer3PubKey;
    private byte[] witnessOwner1PubKey;
    private byte[] witnessOwner2PubKey;
    private byte[] witnessOwner3PubKey;
    private long date1;
    private long date2;
    private long date3;
    private long tradeAmount1;
    private long tradeAmount2;
    private long tradeAmount3;

    @Before
    public void setup() throws Exception {
        AppendOnlyDataStoreService appendOnlyDataStoreService = mock(AppendOnlyDataStoreService.class);
        ArbitratorManager arbitratorManager = mock(ArbitratorManager.class);
        DisputeManager disputeManager = mock(DisputeManager.class);
        when(arbitratorManager.isPublicKeyInList(any())).thenReturn(true);
        signedWitnessService = new SignedWitnessService(null, null, null, arbitratorManager, null, appendOnlyDataStoreService, disputeManager, null);
        account1DataHash = org.bitcoinj.core.Utils.sha256hash160(new byte[]{1});
        account2DataHash = org.bitcoinj.core.Utils.sha256hash160(new byte[]{2});
        account3DataHash = org.bitcoinj.core.Utils.sha256hash160(new byte[]{3});
        long account1CreationTime = getTodayMinusNDays(96);
        long account2CreationTime = getTodayMinusNDays(66);
        long account3CreationTime = getTodayMinusNDays(36);
        aew1 = new AccountAgeWitness(account1DataHash, account1CreationTime);
        aew2 = new AccountAgeWitness(account2DataHash, account2CreationTime);
        aew3 = new AccountAgeWitness(account3DataHash, account3CreationTime);
        ECKey arbitrator1Key = new ECKey();
        KeyPair peer1KeyPair = Sig.generateKeyPair();
        KeyPair peer2KeyPair = Sig.generateKeyPair();
        KeyPair peer3KeyPair = Sig.generateKeyPair();
        signature1 = arbitrator1Key.signMessage(Utilities.encodeToHex(account1DataHash)).getBytes(Charsets.UTF_8);
        signature2 = Sig.sign(peer1KeyPair.getPrivate(), Utilities.encodeToHex(account2DataHash).getBytes(Charsets.UTF_8));
        signature3 = Sig.sign(peer2KeyPair.getPrivate(), Utilities.encodeToHex(account3DataHash).getBytes(Charsets.UTF_8));
        date1 = getTodayMinusNDays(95);
        date2 = getTodayMinusNDays(64);
        date3 = getTodayMinusNDays(33);
        signer1PubKey = arbitrator1Key.getPubKey();
        signer2PubKey = Sig.getPublicKeyBytes(peer1KeyPair.getPublic());
        signer3PubKey = Sig.getPublicKeyBytes(peer2KeyPair.getPublic());
        witnessOwner1PubKey = Sig.getPublicKeyBytes(peer1KeyPair.getPublic());
        witnessOwner2PubKey = Sig.getPublicKeyBytes(peer2KeyPair.getPublic());
        witnessOwner3PubKey = Sig.getPublicKeyBytes(peer3KeyPair.getPublic());
        tradeAmount1 = 1000;
        tradeAmount2 = 1001;
        tradeAmount3 = 1001;
    }

    @Test
    public void testIsValidAccountAgeWitnessOk() {
        SignedWitness sw1 = new SignedWitness(true, account1DataHash, signature1, signer1PubKey, witnessOwner1PubKey, date1, tradeAmount1);
        SignedWitness sw2 = new SignedWitness(false, account2DataHash, signature2, signer2PubKey, witnessOwner2PubKey, date2, tradeAmount2);
        SignedWitness sw3 = new SignedWitness(false, account3DataHash, signature3, signer3PubKey, witnessOwner3PubKey, date3, tradeAmount3);

        signedWitnessService.addToMap(sw1);
        signedWitnessService.addToMap(sw2);
        signedWitnessService.addToMap(sw3);

        assertTrue(signedWitnessService.isValidAccountAgeWitness(aew1));
        assertTrue(signedWitnessService.isValidAccountAgeWitness(aew2));
        assertTrue(signedWitnessService.isValidAccountAgeWitness(aew3));
    }

    @Test
    public void testIsValidAccountAgeWitnessArbitratorSignatureProblem() {
        signature1 = new byte[]{1, 2, 3};

        SignedWitness sw1 = new SignedWitness(true, account1DataHash, signature1, signer1PubKey, witnessOwner1PubKey, date1, tradeAmount1);
        SignedWitness sw2 = new SignedWitness(false, account2DataHash, signature2, signer2PubKey, witnessOwner2PubKey, date2, tradeAmount2);
        SignedWitness sw3 = new SignedWitness(false, account3DataHash, signature3, signer3PubKey, witnessOwner3PubKey, date3, tradeAmount3);

        signedWitnessService.addToMap(sw1);
        signedWitnessService.addToMap(sw2);
        signedWitnessService.addToMap(sw3);

        assertFalse(signedWitnessService.isValidAccountAgeWitness(aew1));
        assertFalse(signedWitnessService.isValidAccountAgeWitness(aew2));
        assertFalse(signedWitnessService.isValidAccountAgeWitness(aew3));
    }

    @Test
    public void testIsValidAccountAgeWitnessPeerSignatureProblem() {
        signature2 = new byte[]{1, 2, 3};

        SignedWitness sw1 = new SignedWitness(true, account1DataHash, signature1, signer1PubKey, witnessOwner1PubKey, date1, tradeAmount1);
        SignedWitness sw2 = new SignedWitness(false, account2DataHash, signature2, signer2PubKey, witnessOwner2PubKey, date2, tradeAmount2);
        SignedWitness sw3 = new SignedWitness(false, account3DataHash, signature3, signer3PubKey, witnessOwner3PubKey, date3, tradeAmount3);

        signedWitnessService.addToMap(sw1);
        signedWitnessService.addToMap(sw2);
        signedWitnessService.addToMap(sw3);

        assertTrue(signedWitnessService.isValidAccountAgeWitness(aew1));
        assertFalse(signedWitnessService.isValidAccountAgeWitness(aew2));
        assertFalse(signedWitnessService.isValidAccountAgeWitness(aew3));
    }

    @Test
    public void testIsValidAccountAgeWitnessDateTooSoonProblem() {
        date3 = getTodayMinusNDays(63);

        SignedWitness sw1 = new SignedWitness(true, account1DataHash, signature1, signer1PubKey, witnessOwner1PubKey, date1, tradeAmount1);
        SignedWitness sw2 = new SignedWitness(false, account2DataHash, signature2, signer2PubKey, witnessOwner2PubKey, date2, tradeAmount2);
        SignedWitness sw3 = new SignedWitness(false, account3DataHash, signature3, signer3PubKey, witnessOwner3PubKey, date3, tradeAmount3);

        signedWitnessService.addToMap(sw1);
        signedWitnessService.addToMap(sw2);
        signedWitnessService.addToMap(sw3);

        assertTrue(signedWitnessService.isValidAccountAgeWitness(aew1));
        assertTrue(signedWitnessService.isValidAccountAgeWitness(aew2));
        assertFalse(signedWitnessService.isValidAccountAgeWitness(aew3));
    }

    @Test
    public void testIsValidAccountAgeWitnessDateTooLateProblem() {
        date3 = getTodayMinusNDays(3);

        SignedWitness sw1 = new SignedWitness(true, account1DataHash, signature1, signer1PubKey, witnessOwner1PubKey, date1, tradeAmount1);
        SignedWitness sw2 = new SignedWitness(false, account2DataHash, signature2, signer2PubKey, witnessOwner2PubKey, date2, tradeAmount2);
        SignedWitness sw3 = new SignedWitness(false, account3DataHash, signature3, signer3PubKey, witnessOwner3PubKey, date3, tradeAmount3);

        signedWitnessService.addToMap(sw1);
        signedWitnessService.addToMap(sw2);
        signedWitnessService.addToMap(sw3);

        assertTrue(signedWitnessService.isValidAccountAgeWitness(aew1));
        assertTrue(signedWitnessService.isValidAccountAgeWitness(aew2));
        assertFalse(signedWitnessService.isValidAccountAgeWitness(aew3));
    }


    @Test
    public void testIsValidAccountAgeWitnessEndlessLoop() throws Exception {
        byte[] account1DataHash = org.bitcoinj.core.Utils.sha256hash160(new byte[]{1});
        byte[] account2DataHash = org.bitcoinj.core.Utils.sha256hash160(new byte[]{2});
        byte[] account3DataHash = org.bitcoinj.core.Utils.sha256hash160(new byte[]{3});
        long account1CreationTime = getTodayMinusNDays(96);
        long account2CreationTime = getTodayMinusNDays(66);
        long account3CreationTime = getTodayMinusNDays(36);
        AccountAgeWitness aew1 = new AccountAgeWitness(account1DataHash, account1CreationTime);
        AccountAgeWitness aew2 = new AccountAgeWitness(account2DataHash, account2CreationTime);
        AccountAgeWitness aew3 = new AccountAgeWitness(account3DataHash, account3CreationTime);

        KeyPair peer1KeyPair = Sig.generateKeyPair();
        KeyPair peer2KeyPair = Sig.generateKeyPair();
        KeyPair peer3KeyPair = Sig.generateKeyPair();


        String account1DataHashAsHexString = Utilities.encodeToHex(account1DataHash);
        String account2DataHashAsHexString = Utilities.encodeToHex(account2DataHash);
        String account3DataHashAsHexString = Utilities.encodeToHex(account3DataHash);

        byte[] signature1 = Sig.sign(peer3KeyPair.getPrivate(), account1DataHashAsHexString.getBytes(Charsets.UTF_8));
        byte[] signature2 = Sig.sign(peer1KeyPair.getPrivate(), account2DataHashAsHexString.getBytes(Charsets.UTF_8));
        byte[] signature3 = Sig.sign(peer2KeyPair.getPrivate(), account3DataHashAsHexString.getBytes(Charsets.UTF_8));

        byte[] signer1PubKey = Sig.getPublicKeyBytes(peer3KeyPair.getPublic());
        byte[] signer2PubKey = Sig.getPublicKeyBytes(peer1KeyPair.getPublic());
        byte[] signer3PubKey = Sig.getPublicKeyBytes(peer2KeyPair.getPublic());
        byte[] witnessOwner1PubKey = Sig.getPublicKeyBytes(peer1KeyPair.getPublic());
        byte[] witnessOwner2PubKey = Sig.getPublicKeyBytes(peer2KeyPair.getPublic());
        byte[] witnessOwner3PubKey = Sig.getPublicKeyBytes(peer3KeyPair.getPublic());
        long date1 = getTodayMinusNDays(95);
        long date2 = getTodayMinusNDays(64);
        long date3 = getTodayMinusNDays(33);

        long tradeAmount1 = 1000;
        long tradeAmount2 = 1001;
        long tradeAmount3 = 1001;

        SignedWitness sw1 = new SignedWitness(false, account1DataHash, signature1, signer1PubKey, witnessOwner1PubKey, date1, tradeAmount1);
        SignedWitness sw2 = new SignedWitness(false, account2DataHash, signature2, signer2PubKey, witnessOwner2PubKey, date2, tradeAmount2);
        SignedWitness sw3 = new SignedWitness(false, account3DataHash, signature3, signer3PubKey, witnessOwner3PubKey, date3, tradeAmount3);

        signedWitnessService.addToMap(sw1);
        signedWitnessService.addToMap(sw2);
        signedWitnessService.addToMap(sw3);

        assertFalse(signedWitnessService.isValidAccountAgeWitness(aew3));
    }


    @Test
    public void testIsValidAccountAgeWitnessLongLoop() throws Exception {
        AccountAgeWitness aew = null;
        KeyPair signerKeyPair;
        KeyPair signedKeyPair = Sig.generateKeyPair();
        int iterations = 1002;
        for (int i = 0; i < iterations; i++) {
            byte[] accountDataHash = org.bitcoinj.core.Utils.sha256hash160(String.valueOf(i).getBytes(Charsets.UTF_8));
            long accountCreationTime = getTodayMinusNDays((iterations - i) * (SignedWitnessService.CHARGEBACK_SAFETY_DAYS + 1));
            aew = new AccountAgeWitness(accountDataHash, accountCreationTime);
            String accountDataHashAsHexString = Utilities.encodeToHex(accountDataHash);
            byte[] signature;
            byte[] signerPubKey;
            if (i == 0) {
                // use arbitrator key
                ECKey arbitratorKey = new ECKey();
                signedKeyPair = Sig.generateKeyPair();
                String signature1String = arbitratorKey.signMessage(accountDataHashAsHexString);
                signature = signature1String.getBytes(Charsets.UTF_8);
                signerPubKey = arbitratorKey.getPubKey();
            } else {
                signerKeyPair = signedKeyPair;
                signedKeyPair = Sig.generateKeyPair();
                signature = Sig.sign(signedKeyPair.getPrivate(), accountDataHashAsHexString.getBytes(Charsets.UTF_8));
                signerPubKey = Sig.getPublicKeyBytes(signerKeyPair.getPublic());
            }
            byte[] witnessOwnerPubKey = Sig.getPublicKeyBytes(signedKeyPair.getPublic());
            long date = getTodayMinusNDays((iterations - i) * (SignedWitnessService.CHARGEBACK_SAFETY_DAYS + 1));
            SignedWitness sw = new SignedWitness(i == 0, accountDataHash, signature, signerPubKey, witnessOwnerPubKey, date, tradeAmount1);
            signedWitnessService.addToMap(sw);
        }
        assertFalse(signedWitnessService.isValidAccountAgeWitness(aew));
    }


    private long getTodayMinusNDays(long days) {
        return Instant.ofEpochMilli(new Date().getTime()).minus(days, ChronoUnit.DAYS).toEpochMilli();
    }

}

