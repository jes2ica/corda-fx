package java_bootcamp;

import net.corda.core.contracts.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.LedgerTransaction;

import java.security.PublicKey;
import java.util.List;

import static net.corda.core.contracts.ContractsDSL.requireThat;

/* Our contract, governing how our state will evolve over time.
 * See src/main/kotlin/examples/ExampleContract.java for an example. */
public class TokenContract implements Contract {
    public static String ID = "java_bootcamp.TokenContract";

    @Override
    public void verify(LedgerTransaction tx) throws IllegalArgumentException {
        List<CommandWithParties<CommandData>> commands = tx.getCommands();
        if (commands.size() != 1) {
            throw new IllegalArgumentException("Must have one command.");
        }

        CommandWithParties<CommandData> command = commands.get(0);
        if (!(command.getValue() instanceof Issue)) {
            throw new IllegalArgumentException("Command type must be Issue");
        }

        List<ContractState> inputs = tx.getInputStates();
        List<ContractState> outputs = tx.getOutputStates();

        if (inputs.size() != 0) throw new IllegalArgumentException("Must have no inputs.");
        if (outputs.size() != 1) throw new IllegalArgumentException("Must have one output.");

        ContractState output = outputs.get(0);
        if (!(output instanceof TokenState)) {
            throw new IllegalArgumentException("Output must be a token");
        }

        TokenState outputState = (TokenState) output;
        if (outputState.getAmount() < 0) {
            throw new IllegalArgumentException("Purchase price must be positive");
        }

        Party issuer = outputState.getIssuer();
        PublicKey issuerKey = issuer.getOwningKey();
        List<PublicKey> requiredSigners = command.getSigners();
        if (!(requiredSigners.contains(issuerKey))) {
            throw new IllegalArgumentException("Issuer must sign.");
        }
    }

    public static class Issue implements CommandData {}
}