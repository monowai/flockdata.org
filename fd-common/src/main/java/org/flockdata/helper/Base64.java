/*
 * Copyright (c) 2012-2014 "FlockData LLC"
 *
 * This file is part of FlockData.
 *
 * FlockData is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FlockData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.helper;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * https://github.com/RestExpress/RepoExpress/blob/master/common/src/java/com/strategicgains/repoexpress/util/UuidConverter.java
 */
public class Base64 {
    static final char[] C64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_".toCharArray();
    static final int[] I256 = new int[256];

    static {
        for (int i = 0; i < Base64.C64.length; i++) {
            Base64.I256[Base64.C64[i]] = i;
        }
    }

    /**
     * Given a UUID instance, return a short (22-character) string
     * representation of it.
     *
     * @param uuid a UUID instance.
     * @return a short string representation of the UUID.
     * @throws NullPointerException     if the UUID instance is null.
     * @throws IllegalArgumentException if the underlying UUID implementation is not 16 bytes.
     */
    public static String format(UUID uuid) {
        if (uuid == null) throw new NullPointerException("Null UUID");

        byte[] bytes = toByteArray(uuid);
        return encodeBase64(bytes);
    }

    /**
     * Given a UUID representation (either a short or long form), return a
     * UUID from it.
     * <p/>
     * If the uuidString is longer than our short, 22-character form (or 24 with padding),
     * it is assumed to be a full-length 36-character UUID string.
     *
     * @param uuidString a string representation of a UUID.
     * @return a UUID instance
     * @throws IllegalArgumentException if the uuidString is not a valid UUID representation.
     * @throws NullPointerException     if the uuidString is null.
     */
    public static UUID parse(String uuidString) {
        if (uuidString == null) throw new NullPointerException("Null UUID string");

        if (uuidString.length() > 24) {
            return UUID.fromString(uuidString);
        }

        if (uuidString.length() < 22) {
            throw new IllegalArgumentException("Short UUID must be 22 characters: " + uuidString);
        }

        byte[] bytes = decodeBase64(uuidString);
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.put(bytes, 0, 16);
        bb.clear();
        return new UUID(bb.getLong(), bb.getLong());
    }

    /**
     * Extracts the bytes from a UUID instance in MSB, LSB order.
     *
     * @param uuid a UUID instance.
     * @return the bytes from the UUID instance.
     */
    static byte[] toByteArray(UUID uuid) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }

    /**
     * Accepts a UUID byte array (of exactly 16 bytes) and base64 encodes it, using a URL-safe
     * encoding scheme.  The resulting string will be 22 characters in length with no extra
     * padding on the end (e.g. no "==" on the end).
     * <p/>
     * Base64 encoding essentially takes each three bytes from the array and converts them into
     * four characters.  This implementation, not using padding, converts the last byte into two
     * characters.
     *
     * @param bytes a UUID byte array.
     * @return a URL-safe base64-encoded string.
     */
    static String encodeBase64(byte[] bytes) {
        if (bytes == null) throw new NullPointerException("Null UUID byte array");
        if (bytes.length != 16) throw new IllegalArgumentException("UUID must be 16 bytes");

        // Output is always 22 characters.
        char[] chars = new char[22];

        int i = 0;
        int j = 0;

        while (i < 15) {
            // Get the next three bytes.
            int d = (bytes[i++] & 0xff) << 16 | (bytes[i++] & 0xff) << 8 | (bytes[i++] & 0xff);

            // Put them in these four characters
            chars[j++] = C64[(d >>> 18) & 0x3f];
            chars[j++] = C64[(d >>> 12) & 0x3f];
            chars[j++] = C64[(d >>> 6) & 0x3f];
            chars[j++] = C64[d & 0x3f];
        }

        // The last byte of the input gets put into two characters at the end of the string.
        int d = (bytes[i] & 0xff) << 10;
        chars[j++] = C64[d >> 12];
        chars[j++] = C64[(d >>> 6) & 0x3f];
        return new String(chars);
    }

    /**
     * Base64 decodes a short, 22-character UUID string (or 24-characters with padding)
     * into a byte array. The resulting byte array contains 16 bytes.
     * <p/>
     * Base64 decoding essentially takes each four characters from the string and converts
     * them into three bytes. This implementation, not using padding, converts the final
     * two characters into one byte.
     *
     * @param s
     * @return
     */
    static byte[] decodeBase64(String s) {
        if (s == null) throw new NullPointerException("Cannot decode null string");
        if (s.isEmpty() || (s.length() > 24)) throw new IllegalArgumentException("Invalid short UUID");

        // Output is always 16 bytes (UUID).
        byte[] bytes = new byte[16];
        int i = 0;
        int j = 0;

        while (i < 15) {
            // Get the next four characters.
            int d = I256[s.charAt(j++)] << 18 | I256[s.charAt(j++)] << 12 | I256[s.charAt(j++)] << 6 | I256[s.charAt(j++)];

            // Put them in these three bytes.
            bytes[i++] = (byte) (d >> 16);
            bytes[i++] = (byte) (d >> 8);
            bytes[i++] = (byte) d;
        }

        // Add the last two characters from the string into the last byte.
        bytes[i] = (byte) ((I256[s.charAt(j++)] << 18 | I256[s.charAt(j++)] << 12) >> 16);
        return bytes;
    }
}