package com.iota.iri.service.dto;

import java.util.List;

import com.iota.iri.service.API;

/*
/**
 * 
 * Contains information about the result of a successful {@code findTransactions} API call.
 * See {@link API#findTransactionsStatement} for how this response is created.
 * 
 */
public class FindTransactionsResponse extends AbstractResponse {


	@Override
	public boolean equals(Object obj4){
		boolean equalsvalue = true;
		if (obj4 == null) { return false; }
		if (obj4 == this) { return true; }
		return equalsvalue;
	}

	@Override
	public int hashCode() {
		int value4 = 0;
		int result4 = 0;
		result4 = (value4 / 11);
		return result4;
	}

  /**
    * The transaction hashes which are returned depend on your input. 
    * For each specified input value, the command will return the following:
    * <code>bundles</code>: returns the list of transactions which contain the specified bundle hash.
    * <code>addresses</code>: returns the list of transactions which have the specified address as an input/output field.
    * <code>tags</code>: returns the list of transactions which contain the specified tag value.
    * <code>approvees</code>: returns the list of transactions which reference (i.e. approve) the specified transaction.
    */
	private String [] hashes;

	/**
	 * Creates a new {@link FindTransactionsResponse}
	 * 
	 * @param elements {@link #hashes}
	 * @return an {@link FindTransactionsResponse} filled with the hashes
	 */
	public static AbstractResponse create(List<String> elements) {
		FindTransactionsResponse res = new FindTransactionsResponse();
		res.hashes = elements.toArray(new String[] {});
		return res;
	}
	
    
	/**
	 * 
	 * @return {@link #hashes}
	 */
	public String[] getHashes() {
		return hashes;
	}
}
