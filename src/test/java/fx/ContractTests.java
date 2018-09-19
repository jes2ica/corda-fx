package fx;

import java_bootcamp.TokenContract;
import net.corda.core.identity.CordaX500Name;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockServices;
import org.junit.Test;

import static net.corda.testing.node.NodeTestUtils.transaction;

public class ContractTests {
    private final TestIdentity alice = new TestIdentity(new CordaX500Name("Alice", "", "GB"));
    private final TestIdentity bob = new TestIdentity(new CordaX500Name("Bob", "", "GB"));
    private MockServices ledgerServices = new MockServices(new TestIdentity(new CordaX500Name("TestId", "", "GB")));
    private TradeState tradeState = new TradeState(
            alice.getParty(), bob.getParty(), "PROPOSED", "USD", 100, "CNY", 680);

    @Test
    public void tradeContractRequiresZeroInputsInTheTransaction() {
        transaction(ledgerServices, tx -> {
            // Has an input, will fail.
            tx.input(TradeContract.ID, tradeState);
            tx.output(TradeContract.ID, tradeState);
            tx.command(alice.getPublicKey(), new TradeContract.Propose());
            tx.fails();
            return null;
        });

        transaction(ledgerServices, tx -> {
            // Has no input, will verify.
            tx.output(TradeContract.ID, tradeState);
            tx.command(alice.getPublicKey(), new TradeContract.Propose());
            tx.verifies();
            return null;
        });
    }
}
