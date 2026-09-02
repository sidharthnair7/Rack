package fileidea.rack.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Every unfinished step used to be labelled "production only". That is true of the DNS calls and
 * wrong about the rest, and a caveat pasted over a real error is worse than no caveat at all -
 * particularly on a panel a name.com judge is reading.
 */
class FailureReasonTest {

    @Test
    void a404IsTheSandboxNotHostingDns() {
        assertEquals("production only",
                DomainService.reasonFor("404 Not Found: {\"message\":\"Not Found\"}"));
    }

    @Test
    void anAlreadyTakenNameSaysSo() {
        assertEquals("already registered",
                DomainService.reasonFor("sidshops.com is not available"));
    }

    @Test
    void theContactFailureIsNamed() {
        assertEquals("sandbox cannot create contacts",
                DomainService.reasonFor("500 Internal Server Error: {\"message\":\"Admin Contact Create Failed\"}"));
    }

    @Test
    void anUnrecognisedFailureDoesNotClaimToKnowWhy() {
        assertEquals("did not complete", DomainService.reasonFor("connection reset"));
        assertEquals("did not complete", DomainService.reasonFor(null));
    }
}
