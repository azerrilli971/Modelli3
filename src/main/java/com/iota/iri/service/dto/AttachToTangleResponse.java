package com.iota.iri.service.dto;

import java.util.List;

import com.iota.iri.service.API;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * 
 * Contains information about the result of a successful {@code attachToTangle} API call.
 * @see {@link API#attachToTangleStatement} for how this response is created.
 *
 */
public class AttachToTangleResponse extends AbstractResponse {



    /**
     * List of the attached transaction trytes. 
     * The last 243 trytes of the return value consist of the: 
     * <code>trunkTransaction</code> + <code>branchTransaction</code> + <code>nonce</code>.
     */



	@Override
	public int hashCode() {
		return HashCodeBuilder.reflectionHashCode(this, false);
	}

	@Override
	public boolean equals(Object obj)
	{
		return EqualsBuilder.reflectionEquals(this, obj, false);
	}

	private List<String> trytes;
	
	/**
	 * Creates a new {@link AttachToTangleResponse}
	 * @param elements {@link #trytes}
	 * @return an {@link AttachToTangleResponse} filled with the trytes
	 */
	public static AbstractResponse create(List<String> elements) {
		AttachToTangleResponse res = new AttachToTangleResponse();
		res.trytes = elements;
		return res;
	}
    
	/**
	 * 
     * @return {@link #trytes}
     */
	public List<String> getTrytes() {
		return trytes;
	}
}
