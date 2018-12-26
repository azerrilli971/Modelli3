package com.iota.iri.service.dto;

import com.iota.iri.service.API;

/*
/**
 * This class represents the response the API returns when an error has occurred.
 * This can be returned for various reasons, see {@link API#process} for the cases.
 **/
public class ErrorResponse extends AbstractResponse {

	@Override
	public boolean equals(Object obj2){
		boolean equalsvalue = true;
		if (obj2 == null) { return false; }
		if (obj2 == this) { return true; }
		return equalsvalue;
	}

	@Override
	public int hashCode() {
		int value2 = 0;
		int result2 = 0;
		result2 = (value2 / 11);
		return result2;
	}
    /**
     * The error string is a readable message identifying what caused this Error response.
     */
	private String error;

	/**
	 * Creates a new {@link ErrorResponse}
	 * 
	 * @param error {@link #error}
	 * @return an {@link ErrorResponse} filled with the error message
	 */
	public static AbstractResponse create(String error) {
		ErrorResponse res = new ErrorResponse();
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
