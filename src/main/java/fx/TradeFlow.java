package fx;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableSet;
import net.corda.core.contracts.ContractState;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import javax.annotation.Signed;
import java.security.GeneralSecurityException;
import java.security.PublicKey;

import static net.corda.core.contracts.ContractsDSL.requireThat;
import static net.corda.core.crypto.Crypto.generateKeyPair;

public class TradeFlow {
    // ``InitiatorFlow`` is our first flow, and will communicate with
    // ``ResponderFlow``, below.
    // We mark ``InitiatorFlow`` as an ``InitiatingFlow``, allowing it to be
    // started directly by the node.
    @InitiatingFlow
    // We also mark ``InitiatorFlow`` as ``StartableByRPC``, allowing the
    // node's owner to start the flow via RPC.
    @StartableByRPC
    // Every flow must subclass ``FlowLogic``. The generic indicates the
    // flow's return type.
    public static class TradeInitiatorFlow extends FlowLogic<SignedTransaction> {
        private String tradeId;
        private Party counterparty;
        private String status;
        private String boughtCurrency;
        private int boughtAmount;
        private String soldCurrency;
        private int soldAmount;

        public TradeInitiatorFlow(
                String tradeId, Party counterparty, String status, String boughtCurrency, int boughtAmount,
                String soldCurrency, int soldAmount) {
            this.tradeId = tradeId;
            this.counterparty = counterparty;
            this.status = status;
            this.boughtCurrency = boughtCurrency;
            this.boughtAmount = boughtAmount;
            this.soldCurrency = soldCurrency;
            this.soldAmount = soldAmount;
        }

        /*----------------------------------
         * WIRING UP THE PROGRESS TRACKER *
        ----------------------------------*/
        // Giving our flow a progress tracker allows us to see the flow's
        // progress visually in our node's CRaSH shell.
        private static final ProgressTracker.Step ID_OTHER_NODES = new ProgressTracker.Step("Identifying other nodes on the network.");
        private static final ProgressTracker.Step TX_BUILDING = new ProgressTracker.Step("Building a transaction.");
        private static final ProgressTracker.Step TX_VERIFICATION = new ProgressTracker.Step("Verifying a transaction.");
        private static final ProgressTracker.Step TX_SIGNING = new ProgressTracker.Step("Signing a transaction.");
        private static final ProgressTracker.Step SIGS_GATHERING = new ProgressTracker.Step("Gathering a transaction's signatures.") {
            // Wiring up a child progress tracker allows us to see the
            // subflow's progress steps in our flow's progress tracker.
            @Override
            public ProgressTracker childProgressTracker() {
                return CollectSignaturesFlow.tracker();
            }
        };
        private static final ProgressTracker.Step VERIFYING_SIGS = new ProgressTracker.Step("Verifying a transaction's signatures.");
        private static final ProgressTracker.Step FINALISATION = new ProgressTracker.Step("Finalising a transaction.") {
            @Override
            public ProgressTracker childProgressTracker() {
                return FinalityFlow.tracker();
            }
        };

        private final ProgressTracker progressTracker = new ProgressTracker(
                ID_OTHER_NODES,
                TX_BUILDING,
                TX_VERIFICATION,
                TX_SIGNING,
                SIGS_GATHERING,
                VERIFYING_SIGS,
                FINALISATION
        );
        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            progressTracker.setCurrentStep(ID_OTHER_NODES);
            // We choose our transaction's notary (the notary prevents double-spends).
            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

            progressTracker.setCurrentStep(TX_BUILDING);

            // We build our transaction.
            TransactionBuilder transactionBuilder = new TransactionBuilder();

            // Get ourselves as an initiator.
            Party initiator = getOurIdentity();

            TradeState state = new TradeState(
                    tradeId, initiator, counterparty, status, boughtCurrency, boughtAmount, soldCurrency, soldAmount);
            transactionBuilder.addOutputState(state, TradeContract.ID, notary);
            transactionBuilder.addCommand(new TradeContract.Propose(), initiator.getOwningKey());

            progressTracker.setCurrentStep(TX_VERIFICATION);

            // We check our transaction is valid based on its contracts.
            transactionBuilder.verify(getServiceHub());

            progressTracker.setCurrentStep(TX_VERIFICATION);

            // We sign the transaction with our private key, making it immutable.
            SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(transactionBuilder);

            progressTracker.setCurrentStep(SIGS_GATHERING);

            // Send the state to the counterparty, and receive it back with their signature.
            FlowSession counterPartySession = initiateFlow(counterparty);
            final SignedTransaction fullySignedTx = subFlow(
                    new CollectSignaturesFlow(
                            partSignedTx, ImmutableSet.of(counterPartySession), CollectSignaturesFlow.Companion.tracker()));

            progressTracker.setCurrentStep(VERIFYING_SIGS);
            try {
                // We can verify that a transaction has all the required
                // signatures, and that they're all valid, by running:
                fullySignedTx.verifyRequiredSignatures();
            } catch (GeneralSecurityException e) {
                // Handle this as required.
            }

            progressTracker.setCurrentStep(FINALISATION);

            // We get the transaction notarised and recorded automatically by the platform.
            return subFlow(new FinalityFlow(fullySignedTx));
        }
    }

    @InitiatedBy(TradeInitiatorFlow.class)
    public static class TradeResponderFlow extends FlowLogic<SignedTransaction> {

        private final FlowSession counterpartySession;

        public TradeResponderFlow(FlowSession counterpartySession) {
            this.counterpartySession = counterpartySession;
        }

        private static final ProgressTracker.Step SIGNING = new ProgressTracker.Step("Responding to CollectSignaturesFlow.") {
            @Override
            public ProgressTracker childProgressTracker() {
                return CollectSignaturesFlow.tracker();
            }
        };

        private final ProgressTracker progressTracker = new ProgressTracker(
                SIGNING
        );
        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            // The ``ResponderFlow` has all the same APIs available. It looks
            // up network information, sends and receives data, and constructs
            // transactions in exactly the same way.

            /*-----------------------------------------
             * RESPONDING TO COLLECT_SIGNATURES_FLOW *
            -----------------------------------------*/
            progressTracker.setCurrentStep(SIGNING);


            // The responder will often need to respond to a call to
            // ``CollectSignaturesFlow``. It does so my invoking its own ``SignTransactionFlow`` subclass.
            class SignTxFlow extends SignTransactionFlow {
                private SignTxFlow(FlowSession otherSession, ProgressTracker progressTracker) {
                    super(otherSession, progressTracker);
                }

                @Override
                protected void checkTransaction(SignedTransaction stx) {
                    requireThat(require -> {
                        ContractState output = stx.getTx().getOutputs().get(0).getData();
                        require.using("This must be a trade transaction.", output instanceof TradeState);
                        return null;
                    });
                }
            }
            return subFlow(new SignTxFlow(counterpartySession, SignTransactionFlow.tracker()));
        }
    }
}
