package fx;

import java_bootcamp.TokenState;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.testing.core.TestIdentity;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StateTests {
    private final Party alice = new TestIdentity(new CordaX500Name("Alice", "", "GB")).getParty();
    private final Party bob = new TestIdentity(new CordaX500Name("Bob", "", "GB")).getParty();

    @Test
    public void tokenStateHasIssuerRecipientAndAmountFieldsOfTheCorrectType() {
        new TokenState(alice, bob, 1);
    }

    @Test
    public void tokenStateHasGettersForIssuerRecipient() {
        TradeState tradeState = new TradeState(
                "12345", alice, bob, "PROPOSED", "USD", 100,
                "CNY", 680);
        assertEquals(alice, tradeState.getInitiator());
        assertEquals(bob, tradeState.getCounterParty());
        assertEquals(2, tradeState.getParticipants().size());
    }

    @Test
    public void tokenStateImplementsContractState() {
        assert(new TokenState(alice, bob, 1) instanceof ContractState);
    }
}
