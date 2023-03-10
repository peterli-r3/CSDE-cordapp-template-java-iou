package com.r3.developers.csdetemplate.IOUFlows;

import com.r3.developers.csdetemplate.utxoexample.contracts.IOUContract;
import com.r3.developers.csdetemplate.utxoexample.states.IOUState;
import net.corda.v5.application.flows.CordaInject;
import net.corda.v5.application.flows.FlowEngine;
import net.corda.v5.application.flows.RPCRequestData;
import net.corda.v5.application.flows.RPCStartableFlow;
import net.corda.v5.application.marshalling.JsonMarshallingService;
import net.corda.v5.application.membership.MemberLookup;
import net.corda.v5.base.annotations.Suspendable;
import net.corda.v5.base.exceptions.CordaRuntimeException;
import net.corda.v5.base.types.MemberX500Name;
import net.corda.v5.ledger.common.NotaryLookup;
import net.corda.v5.ledger.common.Party;
import net.corda.v5.ledger.utxo.UtxoLedgerService;
import net.corda.v5.ledger.utxo.transaction.UtxoSignedTransaction;
import net.corda.v5.ledger.utxo.transaction.UtxoTransactionBuilder;
import net.corda.v5.membership.MemberInfo;
import net.corda.v5.membership.NotaryInfo;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.PublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

public class IOUIssueFlow implements RPCStartableFlow {
    private final static Logger log = LoggerFactory.getLogger(IOUIssueFlow.class);


    @CordaInject
    public JsonMarshallingService jsonMarshallingService;
    @CordaInject
    public MemberLookup memberLookup;

    // Injects the UtxoLedgerService to enable the flow to make use of the Ledger API.
    @CordaInject
    public UtxoLedgerService ledgerService;
    @CordaInject
    public NotaryLookup notaryLookup;
    // FlowEngine service is required to run SubFlows.
    @CordaInject
    public FlowEngine flowEngine;

    @NotNull
    @Override
    @Suspendable
    public String call(@NotNull RPCRequestData requestBody) {
        log.info("IOUIssueFlow.call() called");

        try {
            // Obtain the deserialized input arguments to the flow from the requestBody.
            IOUIssueFlowArgs flowArgs = requestBody.getRequestBodyAs(jsonMarshallingService, IOUIssueFlowArgs.class);

            // Get MemberInfos for the Vnode running the flow and the otherMember.
            MemberInfo myInfo = memberLookup.myInfo();
            MemberInfo lenderInfo = requireNonNull(
                    memberLookup.lookup(MemberX500Name.parse(flowArgs.getLender())),
                    "MemberLookup can't find otherMember specified in flow arguments."
            );

            // Create the IOUState from the input arguments and member information.
            IOUState iou = new IOUState(
                    Integer.parseInt(flowArgs.getAmount()),
                    lenderInfo.getName(),
                    myInfo.getName(),
                    Arrays.asList(myInfo.getLedgerKeys().get(0), lenderInfo.getLedgerKeys().get(0))
            );

            // Obtain the Notary name and public key.
            NotaryInfo notary = notaryLookup.getNotaryServices().iterator().next();
            PublicKey notaryKey = null;
            for(MemberInfo memberInfo: memberLookup.lookup()){
                if(Objects.equals(
                        memberInfo.getMemberProvidedContext().get("corda.notary.service.name"),
                        notary.getName().toString())) {
                    notaryKey = memberInfo.getLedgerKeys().get(0);
                    break;
                }
            }
            // Note, in Java CorDapps only unchecked RuntimeExceptions can be thrown not
            // declared checked exceptions as this changes the method signature and breaks override.
            if(notaryKey == null) {
                throw new CordaRuntimeException("No notary PublicKey found");
            }

            // Use UTXOTransactionBuilder to build up the draft transaction.
            UtxoTransactionBuilder txBuilder = ledgerService.getTransactionBuilder()
                    .setNotary(new Party(notary.getName(), notaryKey))
                    .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofDays(1).toMillis()))
                    .addOutputState(iou)
                    .addCommand(new IOUContract.Issue())
                    .addSignatories(iou.getParticipants());

            // Convert the transaction builder to a UTXOSignedTransaction and sign with this Vnode's first Ledger key.
            // Note, toSignedTransaction() is currently a placeholder method, hence being marked as deprecated.
            @SuppressWarnings("DEPRECATION")
            UtxoSignedTransaction signedTransaction = txBuilder.toSignedTransaction(myInfo.getLedgerKeys().get(0));

            // Call FinalizeIOUSubFlow which will finalise the transaction.
            // If successful the flow will return a String of the created transaction id,
            // if not successful it will return an error message.
            return flowEngine.subFlow(new FinalizeIOUFlow.FinalizeIOU(signedTransaction, Arrays.asList(lenderInfo.getName())));
        }
        // Catch any exceptions, log them and rethrow the exception.
        catch (Exception e) {
            log.warn("Failed to process utxo flow for request body " + requestBody + " because: " + e.getMessage());
            throw new CordaRuntimeException(e.getMessage());
        }
    }
}




/*
RequestBody for triggering the flow via http-rpc:
{
    "clientRequestId": "createiou-3",
    "flowClassName": "com.r3.developers.csdetemplate.IOUFlows.IOUIssueFlow",
    "requestData": {
        "amount":"20",
        "lender":"CN=Bob, OU=Test Dept, O=R3, L=London, C=GB"
        }
}
 */
