# Foreign Exchange in Corda
Implement a Foreign Exchange (FX) system in Corda.

## Links to useful resources

* Key Concepts docs (`docs.corda.net/key-concepts.html`)
* API docs (`docs.corda.net/api-index.html`)
* Cheat sheet (`docs.corda.net/cheat-sheet.html`)
* Sample CorDapps (`www.corda.net/samples`)
* Stack Overflow (`www.stackoverflow.com/questions/tagged/corda`)

## What we'll be building

Our FX system will have three parts:

### The TradeState

States define shared facts on the ledger. Our state, TradeState, will define a
fx trade. It will have the following structure:

    ----------------------
    |                    |
    |   TradeState       |
    |                    |
    |   - tradeId        |
    |   - initiator      |
    |   - counterparty   |
    |   - status         |
    |   - boughtCurrency |
    |   - boughtAmount   |
    |   - soldCurrency   | 
    |   - soldAmount     |
    |                    |
    ----------------------

### The TradeContract

Contracts govern how states evolve over time. Our contract, TradeContract,
will define how TradeState evolve. It will only allow the following type of
TokenState transaction:

    -------------------------------------------------------------------------------------
    |                                                                                   |
    |    - - - - - - - - - -                                     -------------------    |
    |                                              ▲             |                 |    |
    |    |                 |                       | -►          |   TradeState    |    |
    |            NO             -------------------     -►       |                 |    |
    |    |                 |    |    Propose command       -►    |   - initiator   |    |
    |          INPUTS           | signed by initiator      -►    |   - recipient   |    |
    |    |                 |    -------------------     -►       |                 |    |
    |                                              | -►          |                 |    |
    |    - - - - - - - - - -                       ▼             -------------------    |
    |                                                                                   |
    -------------------------------------------------------------------------------------

              No inputs             One issue command,                One output,
                                 issuer is a required signer       issuer must sign

To do so, TradeContract will impose the following constraints on transactions
involving TokenStates:

* The transaction has one output state
* The transaction has one command
* The output state is a TokenState
* The command is an Propose command
* The command lists the TokenState's initiator as a required signer

### The TradeFlow

Flows automate the process of updating the ledger. Our flow, TradeFlow, will
automate the following steps:

            Issuer                Recipient                Notary
              |                       |                       |
       Chooses a notary
              |                       |                       |
        Starts building
         a transaction                |                       |
              |
        Adds the output               |                       |
          TradeState
              |                       |                       |
           Adds the
         Popose command               |                       |
              |
          Signs the                   |                       |
         transaction                  
              |                       |                       |
              
              |.                 Verifies the                 |
                                transaction                  
              |                       |                       |
              
              |----------------------------------------------►|
              |                       |                       |
                                                         Notarises the
              |                       |                   transaction
                                                              |
              |◀----------------------------------------------|
              |                       |                       |
         Records the
         transaction                  |                       |
              |
              |----------------------►|                       |
                                      |
              |                  Records the                  |
                                 transaction
              |                       |                       |
              ▼                       ▼                       ▼

## Running our CorDapp

Normally, you'd interact with a CorDapp via a client or webserver. So we can
focus on our CorDapp, we'll be running it via the node shell instead.

Once you've finished the CorDapp's code, run it with the following steps:

* Build a test network of nodes by opening a terminal window at the root of
  your project and running the following command:

    * Windows:   `gradlew.bat deployNodesJava -Poffline=true`
    * macOS:     `./gradlew deployNodesJava -Poffline=true`

* Start the nodes by running the following command:

    * Windows:   `build\nodes\runnodes.bat`
    * macOS:     `build/nodes/runnodes`

* Open the nodes are started, go to the terminal of Party A (not the notary!)
  and run the following command to intiate a trade with Party B:

    `flow start fx.TradeFlow$TradeInitiatorFlow tradeId: "123", counterparty: PartyB, status: "Proposed", boughtCurrency: "USD", boughtAmount: 100, soldCurrency: "CNY", soldAmount: 680`
    
* You can see the following logs:

- ✅   Identifying other nodes on the network.
- ✅   Building a transaction.
- ✅   Verifying a transaction.
- ✅   Signing a transaction.
- ✅   Gathering a transaction's signatures.
    - ✅   Collecting signatures from counterparties.
    - ✅   Verifying collected signatures.
- ✅   Verifying a transaction's signatures.
- ✅   Finalising a transaction.
    - ✅   Requesting signature by notary service
        - ✅   Requesting signature by Notary service
        - ✅   Validating response from Notary service
    - ✅   Broadcasting transaction to participants
- ✅   Done

* You can now see the tokens in both nodes' vaults by running the following
  command in their respective terminals:

    `run vaultQuery contractStateType: fx.TradeState`
