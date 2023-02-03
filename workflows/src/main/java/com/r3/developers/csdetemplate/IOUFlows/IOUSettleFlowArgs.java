package com.r3.developers.csdetemplate.IOUFlows;

import java.util.UUID;

public class IOUSettleFlowArgs {
    private String amountSettle;
    private UUID iouID;

    public IOUSettleFlowArgs() {
    }

    public IOUSettleFlowArgs(String amountSettle, UUID iouID) {
        this.amountSettle = amountSettle;
        this.iouID = iouID;
    }

    public String getAmountSettle() {
        return amountSettle;
    }

    public UUID getIouID() {
        return iouID;
    }
}