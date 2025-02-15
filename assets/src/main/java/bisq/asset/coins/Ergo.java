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

package bisq.asset.coins;

import bisq.asset.AddressValidationResult;
import bisq.asset.AddressValidator;
import bisq.asset.Coin;

import java.util.Arrays;

import org.bitcoinj.core.Base58;
import org.bitcoinj.core.AddressFormatException;
import org.bouncycastle.crypto.digests.Blake2bDigest;

public class Ergo extends Coin {

    public Ergo() {
        super("Ergo", "ERG", new ErgoAddressValidator());
    }

    public static class ErgoAddressValidator implements AddressValidator {

        @Override
        public AddressValidationResult validate(String address) {
            try {
                byte[] decoded  = Base58.decode(address);
                if (decoded.length < 4) {
                    return AddressValidationResult.invalidAddress("Input too short: " + decoded.length);
                }
                if (decoded[0] != 1 && decoded[0] != 2 && decoded[0] != 3) {
                    return AddressValidationResult.invalidAddress("Invalid prefix");
                }
                byte[] data = Arrays.copyOfRange(decoded, 0, decoded.length - 4);
                byte[] checksum = Arrays.copyOfRange(decoded, decoded.length - 4, decoded.length);
                byte[] hashed = new byte[32];
                {
                    Blake2bDigest digest = new Blake2bDigest(256);
                    digest.update(data, 0, data.length);
                    digest.doFinal(hashed, 0);
                }
                byte[] actualChecksum = Arrays.copyOfRange(hashed, 0, 4);
                if (!Arrays.equals(checksum, actualChecksum)) {
                    return AddressValidationResult.invalidAddress("Invalid checksum");
                }
            } catch (AddressFormatException e) {
                return AddressValidationResult.invalidAddress(e);
            }
            return AddressValidationResult.validAddress();
        }
    }
}
