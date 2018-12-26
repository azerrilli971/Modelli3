package com.iota.iri.service.dto;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * 
 * This class represents the API error for accessing a command when it is not allowed by this Node.
 * 
 */
public class AccessLimitedResponse extends AbstractResponse {

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this, false);
    }

    @Override
    public boolean equals(Object obj)
    {
        return EqualsBuilder.reflectionEquals(this, obj, false);
    }
    

    /**
     * The error identifies what caused this Response.
     * It is a readable message identifying the command that is limited.
     */
    private String error;

    /**
     * Creates a new {@link AccessLimitedResponse}
     * 
     * @param error {@link #error}
     * @return an {@link AccessLimitedResponse} filled with the error message
     */
    public static AbstractResponse create(String error) {
        AccessLimitedResponse res = new AccessLimitedResponse();
        res.error = error;
        return res;
    }

    /**
     * 
     * @return {@link #error}
     */
    public String getError() {
        return error;
    }
}
