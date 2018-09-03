package fx;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.Party;
import net.corda.core.transactions.LedgerTransaction;

import java.security.PublicKey;
import java.util.List;

/* Trade contract, governing how trade state will evolve over time. */
public class TradeConstract implements Contract {
    public static String ID = "java_bootcamp.TokenContract";

    @Override
    public void verify(LedgerTransaction tx) throws IllegalArgumentException {
        List<CommandWithParties<CommandData>> commands = tx.getCommands();
        if (commands.size() != 1) {
            throw new IllegalArgumentException("Must have one command.");
        }

        CommandWithParties<CommandData> command = commands.get(0);
        if (!(command.getValue() instanceof java_bootcamp.TokenContract.Issue)) {
            throw new IllegalArgumentException("Command type must be Issue");
        }

        List<ContractState> inputs = tx.getInputStates();
        List<ContractState> outputs = tx.getOutputStates();

        if (inputs.size() != 0) throw new IllegalArgumentException("Must have no inputs.");
        if (outputs.size() != 1) throw new IllegalArgumentException("Must have one output.");

        ContractState output = outputs.get(0);
        if (!(output instanceof TradeState)) {
            throw new IllegalArgumentException("Output must be a token");
        }

        TradeState outputState = (TradeState) output;
        if (outputState.getAmount() < 0) {
            throw new IllegalArgumentException("Purchase price must be positive");
        }

        Party initiator = outputState.getInitiator();
        PublicKey initiatorKey = initiator.getOwningKey();
        List<PublicKey> requiredSigners = command.getSigners();
        if (!(requiredSigners.contains(initiatorKey))) {
            throw new IllegalArgumentException("Initiator must sign.");
        }
    }

    public static class Propose implements CommandData {}
}
