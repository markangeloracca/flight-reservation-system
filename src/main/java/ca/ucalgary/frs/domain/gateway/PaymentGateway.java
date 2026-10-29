package ca.ucalgary.frs.domain.gateway;

import ca.ucalgary.frs.domain.model.CardDetails;
import ca.ucalgary.frs.domain.model.Money;

/**
 * Boundary to a card processor. The project uses a simulated implementation; a real processor
 * could be plugged in later behind this interface without touching business logic.
 */
public interface PaymentGateway {

    GatewayResult authorize(CardDetails card, Money amount);

    GatewayResult capture(String authorizationRef, Money amount);

    GatewayResult refund(String originalRef, Money amount);

    record GatewayResult(boolean approved, String reference, String declineReason) {
        public static GatewayResult approved(String reference) {
            return new GatewayResult(true, reference, null);
        }

        public static GatewayResult declined(String reason) {
            return new GatewayResult(false, null, reason);
        }
    }
}
