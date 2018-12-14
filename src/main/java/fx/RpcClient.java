package fx;

import net.corda.client.rpc.CordaRPCClient;
import net.corda.client.rpc.CordaRPCClientConfiguration;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.messaging.DataFeed;
import net.corda.core.node.services.Vault;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.utilities.NetworkHostAndPort;
import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.util.concurrent.ExecutionException;

public class RpcClient {
    private static final Logger logger = LoggerFactory.getLogger(RpcClient.class);

    private static void logState(StateAndRef<TradeState> state) {
        System.out.println(String.format("{%s}", state.getState().getData()));
    }

    public static void main(String[] args) throws ActiveMQException, InterruptedException, ExecutionException {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: ExampleClientRPC <node address>");
        }

        final NetworkHostAndPort nodeAddress = NetworkHostAndPort.parse(args[0]);
        final CordaRPCClient client = new CordaRPCClient(nodeAddress, CordaRPCClientConfiguration.DEFAULT);

        // Can be amended in the com.example.Main file.
        final CordaRPCOps proxy = client.start("user1", "test").getProxy();
        final Party otherParty = proxy.wellKnownPartyFromX500Name(CordaX500Name.parse("O=PartyB,L=New York,C=US"));

        final SignedTransaction signedTx = proxy
                .startTrackedFlowDynamic(TradeFlow.TradeInitiatorFlow.class, "23456", otherParty, "PROPOSED",
                        "USD", 100, "CNY", 680)
                .getReturnValue()
                .get();

        final String msg = String.format("Transaction id %s committed to ledger.\n", signedTx);
        System.out.println(msg);

        // Grab all existing and future IOU states in the vault.
        final DataFeed<Vault.Page<TradeState>, Vault.Update<TradeState>> dataFeed = proxy.vaultTrack(TradeState.class);
        final Vault.Page<TradeState> snapshot = dataFeed.getSnapshot();
        final Observable<Vault.Update<TradeState>> updates = dataFeed.getUpdates();

        // Log the 'placed' IOUs and listen for new ones.
        snapshot.getStates().forEach(RpcClient::logState);
        updates.toBlocking().subscribe(update -> update.getProduced().forEach(RpcClient::logState));
    }

}
