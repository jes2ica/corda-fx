package fx;

import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;

import java.util.List;

/* Definition of a Trade */
public class TradeState implements ContractState {
    private Party initiator;
    private Party counterparty;
    private int amount;

    public TradeState(Party issuer, Party recipient, int amount) {
        this.initiator = issuer;
        this.counterparty = recipient;
        this.amount = amount;
    }

    @Override
    public List<AbstractParty> getParticipants() {
        return ImmutableList.of(initiator, counterparty);
    }

    public Party getInitiator() {
        return initiator;
    }

    public Party getCounterparty() {
        return counterparty;
    }

    public int getAmount() {
        return amount;
    }
}
