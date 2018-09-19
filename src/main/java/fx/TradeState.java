package fx;

import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;

import java.util.List;

public class TradeState implements ContractState {
    private Party initiator;
    private Party counterparty;
    private String status;
    private String boughtCurrency;
    private int boughtAmount;
    private String soldCurrency;
    private int soldAmount;

    public TradeState(Party initiator, Party counterParty, String status, String boughtCurrency, int boughtAmount, String soldCurrency, int soldAmount) {
        this.initiator = initiator;
        this.counterparty = counterParty;
        this.status = status;
        this.boughtCurrency = boughtCurrency;
        this.boughtAmount = boughtAmount;
        this.soldCurrency = soldCurrency;
        this.soldAmount = soldAmount;
    }

    public Party getInitiator() {
        return initiator;
    }
    public Party getCounterParty() {
        return counterparty;
    }
    public String getBoughtCurrency() { return boughtCurrency; }
    public int getBoughtAmount() { return boughtAmount; }
    public String getSoldCurrency() { return soldCurrency; }
    public int getSoldAmount() { return soldAmount; }
    public String getStatus() { return status; }

    @Override
    public List<AbstractParty> getParticipants() {
        return ImmutableList.of(initiator, counterparty);
    }

}
