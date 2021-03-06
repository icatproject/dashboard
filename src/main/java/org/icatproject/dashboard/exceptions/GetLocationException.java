/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.icatproject.dashboard.exceptions;

/**
 *
 * @author tip22963
 */
public class GetLocationException extends Exception {
    private static final long serialVersionUID = 1L;
    
    private final String message;
    
    private final String ipAddress;
    
    public GetLocationException(String message, String ipAddress) {
        this.message = message;
        this.ipAddress = ipAddress;
    }

    public String getShortMessage() {
        return message;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
}
