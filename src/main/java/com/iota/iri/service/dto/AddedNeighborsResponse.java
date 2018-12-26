package com.iota.iri.service.dto;

import com.iota.iri.service.API;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * 
 * Contains information about the result of a successful {@code addNeighbors} API call.
 * See {@link API#} for how this response is created.
 * 
 */
public class AddedNeighborsResponse extends AbstractResponse {



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
     * The amount of temporally added neighbors to this node.
     * Can be 0 or more.
     */
	private int addedNeighbors;
	
	/**
     * Creates a new {@link AddedNeighborsResponse}
     * 
     * @param numberOfAddedNeighbors {@link #addedNeighbors}
     * @return an {@link AddedNeighborsResponse} filled with the number of added neighbors
     */
	public static AbstractResponse create(int numberOfAddedNeighbors) {
		AddedNeighborsResponse res = new AddedNeighborsResponse();
		res.addedNeighbors = numberOfAddedNeighbors;
		return res;
	}

    /**
     * 
     * @return {link #addedNeighbors}
     */
	public int getAddedNeighbors() {
		return addedNeighbors;
	}
	
}
