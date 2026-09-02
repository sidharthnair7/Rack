package fileidea.rack.integration.stripe;

import java.math.BigDecimal;

/**
 * Turns a listing into something a buyer can actually pay for.
 *
 * <p>Without this the storefront is a catalog, and a domain that hosts a catalog is decoration.
 * A checkout is what makes the registered domain the address of a business rather than a link.
 */
public interface StripeClient {

    /**
     * @param imageUrl absolute, publicly reachable photo of the garment, or {@code null}. Stripe
     *                 fetches it server-side to render on the checkout page, so a path that only
     *                 resolves on the seller's own machine has to be passed as {@code null} rather
     *                 than sent and left to fail.
     * @return a hosted checkout URL, or {@code null} when Stripe is not configured — callers must
     *         treat a missing link as "show contact details instead", never as an error.
     */
    String createCheckoutLink(String productName, String description, BigDecimal amount, String imageUrl);
}
