package ca.ucalgary.frs.data.gateway;

import ca.ucalgary.frs.domain.gateway.PaymentGateway;
import ca.ucalgary.frs.domain.model.CardDetails;
import ca.ucalgary.frs.domain.model.Money;

import java.util.UUID;

/**
 * Stand-in for a real card processor. Approves every valid card except a few documented test numbers:
 * <ul>
 *   <li>card ending in {@code 0002}: declined, insufficient funds</li>
 *   <li>card ending in {@code 0069}: declined, card reported lost or stolen</li>
 *   <li>amounts over $10,000: declined, over limit</li>
 * </ul>
 * An optional delay imitates network latency so the UI's progress state is visible.
 */
public final class SimulatedPaymentGateway implements PaymentGateway {
    private static final Money LIMIT = Money.of("10000.00");

    private final long latencyMillis;

    public SimulatedPaymentGateway(long latencyMillis) {
        this.latencyMillis = latencyMillis;
    }

    @Override
    public GatewayResult authorize(CardDetails card, Money amount) {
        pause();
        if (card.number().endsWith("0002")) {
            return GatewayResult.declined("insufficient funds");
        }
        if (card.number().endsWith("0069")) {
            return GatewayResult.declined("card reported lost or stolen");
        }
        if (amount.compareTo(LIMIT) > 0) {
            return GatewayResult.declined("amount exceeds card limit");
        }
        return GatewayResult.approved("AUTH-" + shortId());
    }

    @Override
    public GatewayResult capture(String authorizationRef, Money amount) {
        return GatewayResult.approved(authorizationRef.replace("AUTH-", "TXN-"));
    }

    @Override
    public GatewayResult refund(String originalRef, Money amount) {
        pause();
        return GatewayResult.approved("RFD-" + shortId());
    }

    private static String shortId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }

    private void pause() {
        if (latencyMillis <= 0) {
            return;
        }
        try {
            Thread.sleep(latencyMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
