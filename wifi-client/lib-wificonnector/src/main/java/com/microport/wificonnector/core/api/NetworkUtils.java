package com.microport.wificonnector.core.api;

import java.net.InetAddress;
import java.net.UnknownHostException;


class NetworkUtils {
    /**
     * Convert a IPv4 address from an integer to an InetAddress.
     *
     * @param hostAddress an int corresponding to the IPv4 address in network byte order
     */
    public static InetAddress intToInetAddress(int hostAddress) {
        byte[] addressBytes = {(byte) (0xff & hostAddress), (byte) (0xff & (hostAddress >> 8)),
                (byte) (0xff & (hostAddress >> 16)), (byte) (0xff & (hostAddress >> 24))};
        
        try {
            return InetAddress.getByAddress(addressBytes);
        } catch (UnknownHostException e) {
            throw new AssertionError();
        }
    }
    
    /**
     * Convert a IPv4 address from an InetAddress to an integer
     *
     * @param inetAddress is an InetAddress corresponding to the IPv4 address
     * @return the IP address as an integer in network byte order
     */
    public static int inetAddressToInt(InetAddress inetAddress) throws IllegalArgumentException {
        byte[] address = inetAddress.getAddress();
        return ((address[3] & 0xff) << 24) | ((address[2] & 0xff) << 16) | ((address[1] & 0xff) << 8) | (address[0] & 0xff);
    }
}