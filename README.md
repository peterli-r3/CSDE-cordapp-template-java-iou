# IOU Cordapp with Corda 5


## IOU(I owe you) app
We had built a iou app to demo some functionalities of the next gen Corda platform.

In this app you can:
1. Create a new IOU between self and a counterparty (Borrow some amount from the counterparty). `IOUIssueFlow`
2. List out the IOU entries you had. `ListIOUFlow`
3. Settle the IOU with the counterparty (pay back the borrowed amount). `IOUSettleFlow`
4. Lender can transfer the IOU to a new lender. `IOUTransferFlow`

### Setting up

1. We will begin our test deployment with clicking the `startCorda`. This task will load up the combined Corda workers in docker.
   A successful deployment will allow you to open the HTTP RPC at: https://localhost:8888/api/v1/swagger#. You can test out some of the
   functions to check connectivity. (GET /cpi function call should return an empty list as for now.)
2. We will now deploy the cordapp with a click of `quickDeployCordapp` task. Upon successful deployment of the CPI, the GET /cpi function call should now return the meta data of the cpi you just upload



### Running the IOU app

In Corda 5, flows will be triggered via `POST /flow/{holdingidentityshorthash}` and flow result will need to be view at `GET /flow/{holdingidentityshorthash}/{clientrequestid}`
* holdingidentityshorthash: the id of the network participants, ie Bob, Alice, Charlie. You can view all the short hashes of the network member with another gradle task called `ListVNodes`
* clientrequestid: the id you specify in the flow requestBody when you trigger a flow.

#### Step 1: Create IOU between Alice and Bob
Pick a VNode identity to initiate the IOU, and get its short hash. (Let's pick Alice. Dont pick Bob because Bob is the person who we will borrow money from).

Go to `POST /flow/{holdingidentityshorthash}`, enter the identity short hash(Alice's hash) and request body:
```
{
    "clientRequestId": "createiou-1",
    "flowClassName": "com.r3.developers.csdetemplate.IOUFlows.IOUIssueFlow",
    "requestData": {
        "amount":"20",
        "lender":"CN=Bob, OU=Test Dept, O=R3, L=London, C=GB"
        }
}
```

After trigger the create-iou flow, hop to `GET /flow/{holdingidentityshorthash}/{clientrequestid}` and enter the short hash(Alice's hash) and clientrequestid to view the flow result

#### Step 2: List the IOU
In order to continue opereating the IOU, we would need the IOU ID. This step will bring out all the IOU entries this entity (Alice) has.
Go to `POST /flow/{holdingidentityshorthash}`, enter the identity short hash(Alice's hash) and request body:
```
{
    "clientRequestId": "list-1",
    "flowClassName": "com.r3.developers.csdetemplate.IOUFlows.ListIOUFlow",
    "requestData": {}
}
```
After trigger the list-iou flow, again, we need to hop to `GET /flow/{holdingidentityshorthash}/{clientrequestid}` and check the result. As the screenshot shows, in the response body,
we will see a list of iou entries, but it currently only has one entry. And we can see the id of the iou entry. Lets record that id.


#### Step 3: Alice partially settle some amount `IOUSettleFlow`
Goto `POST /flow/{holdingidentityshorthash}`, enter Alice the identity short hash and request body. 
```
{
    "clientRequestId": "settleiou-1",
    "flowClassName": "com.r3.developers.csdetemplate.IOUFlows.IOUSettleFlow",
    "requestData": {
        "amountSettle":"10",
        "iouID":"< -- iou ID -- >"  
        }
}
```
And as for the result of this flow, go to `GET /flow/{holdingidentityshorthash}/{clientrequestid}` and enter the required fields.

#### Option step: 

If you would like to see the IOU result, you can repeat Step 2 again. (Note: make sure you change the client request ID)

#### Step 4: Lender (Bob) transfer the IOU to Charlie
In this step, we will have the IOU lender transfer the IOU to a new lender. We will again need to use the `POST /flow/{holdingidentityshorthash}` API, but we need to 
pay attention that we need to change the short hash from Alice's short hash to Bob's short hash. 
```
{
    "clientRequestId": "transferiou-1",
    "flowClassName": "com.r3.developers.csdetemplate.IOUFlows.IOUTransferFlow",
    "requestData": {
        "newLender":"CN=Charlie, OU=Test Dept, O=R3, L=London, C=GB",
        "iouID":"< -- iou ID -- >"
        }
}
```
And as for the result, you need to go to the `GET /flow/{holdingidentityshorthash}/{clientrequestid}` API again and enter the short hash and client request ID.

#### Option step:

If you would like to see the IOU result, you can repeat Step 2 again. (Note: make sure you change the client request ID)

#### Step 5: Alice partially settle some amount `IOUSettleFlow` again, but to new lender Charlie
Goto `POST /flow/{holdingidentityshorthash}`, enter Alice the identity short hash and request body.
```
{
    "clientRequestId": "settleiou-2",
    "flowClassName": "com.r3.developers.csdetemplate.IOUFlows.IOUSettleFlow",
    "requestData": {
        "amountSettle":"7",
        "iouID":"< -- iou ID -- >"  
        }
}
```
And as for the result of this flow, go to `GET /flow/{holdingidentityshorthash}/{clientrequestid}` and enter the required fields.

#### Option step:

Lastly, to ensure all your flows were successful, perform Step 2 again to see the latest IOU. (Note: make sure you change the client request ID)

Thus, we have concluded a full run through of the IOU app. 
