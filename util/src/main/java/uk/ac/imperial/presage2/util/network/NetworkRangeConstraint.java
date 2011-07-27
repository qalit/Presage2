/**
 * 	Copyright (C) 2011 Sam Macbeth <sm1106 [at] imperial [dot] ac [dot] uk>
 *
 * 	This file is part of Presage2.
 *
 *     Presage2 is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Presage2 is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser Public License
 *     along with Presage2.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.ac.imperial.presage2.util.network;

import java.util.UUID;

import uk.ac.imperial.presage2.core.environment.EnvironmentServiceProvider;
import uk.ac.imperial.presage2.core.environment.ServiceDependencies;
import uk.ac.imperial.presage2.core.environment.SharedStateAccessException;
import uk.ac.imperial.presage2.core.environment.UnavailableServiceException;
import uk.ac.imperial.presage2.core.event.EventBus;
import uk.ac.imperial.presage2.core.network.Message;
import uk.ac.imperial.presage2.core.network.NetworkAddress;
import uk.ac.imperial.presage2.core.network.NetworkConstraint;
import uk.ac.imperial.presage2.util.environment.CommunicationRangeService;
import uk.ac.imperial.presage2.util.location.CannotSeeAgent;
import uk.ac.imperial.presage2.util.location.Location;
import uk.ac.imperial.presage2.util.location.LocationService;

import com.google.inject.Inject;

/**
 * @author Sam Macbeth
 * 
 */
@ServiceDependencies({ LocationService.class, CommunicationRangeService.class })
public class NetworkRangeConstraint implements NetworkConstraint {

	private LocationService locService;

	private CommunicationRangeService commRangeService;

	@Inject
	public NetworkRangeConstraint(EnvironmentServiceProvider serviceProvider,
			EventBus eb) throws UnavailableServiceException {
		locService = serviceProvider
				.getEnvironmentService(LocationService.class);
		commRangeService = serviceProvider
				.getEnvironmentService(CommunicationRangeService.class);
		eb.subscribe(this);
	}

	@Override
	public Message constrainMessage(Message m) {
		// we don't need to modify messages, we just block at point of delivery.
		return m;
	}

	@Override
	public boolean blockMessageDelivery(NetworkAddress to, Message m) {
		final UUID sender = m.getFrom().getId();
		final UUID receiver = to.getId();
		boolean result = areLinked(sender, receiver);
		return result;
	}

	protected boolean areLinked(UUID a1, UUID a2) {
		try {
			// retrieve locations and comms ranges of sender and receiver.
			final Location senderLoc = locService.getAgentLocation(a1);
			final Location receiverLoc = locService.getAgentLocation(a2);
			final double senderRange = commRangeService
					.getAgentCommunicationRange(a1);
			final double receiverRange = commRangeService
					.getAgentCommunicationRange(a2);

			// return true if distance between sender and receiver > the
			// smallest of their comm ranges.
			boolean result = (senderLoc.distanceTo(receiverLoc) > Math.min(
					senderRange, receiverRange));

			return result;
		} catch (CannotSeeAgent e) {
			// this should not happen!
			throw new RuntimeException(
					"LocationService threw CannotSeeAgent for NetworkRangeConstraint",
					e);
		} catch (SharedStateAccessException e) {
			// someone doesn't have location or communication range state, allow
			// in this case
			return false;
		}
	}

}
