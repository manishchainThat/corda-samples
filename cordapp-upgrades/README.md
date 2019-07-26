<p align="center">
  <img src="https://camo.githubusercontent.com/a7b7d659d6e01a9e49ff2d9919f7a66d84aac66e/68747470733a2f2f7777772e636f7264612e6e65742f77702d636f6e74656e742f75706c6f6164732f323031362f31312f66673030355f636f7264615f622e706e67" alt="Corda" width="500">
</p>

# CorDapp Upgrades

This sample show how to upgrade corDapps using implicit and explicit approaches. 
Signature Constraint (Implicit-Upgrades) introduced in Corda 4 is however the recommended approach to perform upgrades in Corda, since it doesn't 
requires the heavy weight process of creating upgrade transactions for every state on the ledger of all parties.


## Contract and Flow Version
This sample has various versions of contracts and flows which will used to demonstrate implicit & explicit upgrades in Corda.

###Version 1 Contracts & Flows
Version 1 contracts and flows will be our initial cordapp implementation. Its a simple cordapp on vehicle registration, which contains two flows, 
one for issuing registration number for the vehicle and other for transferring the vehicle to a new owner. There would be three parties involved - `RTO` 
(The registering authority), `Party A` and `PartyB`. We assume that both the flows would be initiated by the `RTO`.

###Version 2 Flows
Version 2 of flows brings a minor change, while version 1 has the owner and new owner of the vehicle as non-signing parties for vehicle transfer flow, 
this version includes them as signing party.

###Version 2 Contracts and Version 3 Flows
Version 2 of contracts introduces a new feature to issue/ pay challan again traffic violation. `Police` party would be issuing the challans 
and payments against challans could be done by the vehicle owner. Two new state variable are introduced in the Vehicle state (`challanValue` and  
`challanIssuer`) for this purpose. Corresponding Commands and verify logic are added to the `VehicleContract`. The corresponding flows to accommodate 
this feature is implemented in version 3 of the flows.

###Version 3 Contracts
Version 3 contracts updates the transfer verify logic to restrict transfer of vehicle till all challans are paid.

###Version 2 Explicit
This version of the contract would be used to perform explicit upgrades. It is equivant to version 2 of the contract but the contract implements the 
`UpgradedContract` interface which is required for explicit upgrades of contracts using `WhiteListedByZoneAttachmentConstraint`.

###Version 2 Explicit
This version of the contract would be used to perform explicit upgrades for contracts using hash constraint. It is equivant to version 2 of the contract but the contract implements the 
`UpgradedLegacyContract` interface which is required for explicit upgrades of contracts using `HashAttachmentConstraint`.

###Version 2 Legacy Explicit

## Implicit Upgrade Scenarios And Steps

###Scenario 1: Initial Deployment, Vehicle Registration and Transfer
**Step1:** Deploy version1 of contracts and flows by running `./gradlew deployNodes` and then run the nodes using `./build/nodes/runnodes`. 
This would create a network of 4 nodes and a notary all running version 1 of contracts and flows. The `Police` node however would be used once 
the Challan feature is introduced in our cordapp in later versions.

**Step2:** Register two vehicle by running the `RegistrationInitiatorFlow` from `RTO`'s shell. We need to pass the registration number and the party to 
whom the vehicle is being registered, as parameters to the flow.

    start RegistrationInitiatorFlow owner: PartyA, redgNumber: MH01C2321
    start RegistrationInitiatorFlow owner: PartyA, redgNumber: MH01C2322
    
**Step3:** Run vaultQuery to validate the vehicle successfully registered. The state should have been shared with `PartyA` and `RTO`. Run the below 
command in both the party's shell.
    
    run vaultQuery contractStateType: corda.samples.upgrades.states.VehicleState
    
**Step4:** Transfer one of the vehicle to `Party B`.
    
    start TransferInitiatorFlow newOwner: PartyB, redgNumber: MH01C2321
    
**Step5:** Run vaultQuery to validate successful transfer of the vehicle. The state information of the vehicle transferred should now only be available with 
`PartyB` and `RTO`. `PartyA` would not anymore be able to view the state. Run the below command in all the three party's shell and check the result.

    run vaultQuery contractStateType: corda.samples.upgrades.states.VehicleState
    
###Scenario 2: Flow Upgrade to version 2 for RTO and Party A
**Step1:** Shutdown the nodes and upgrade the flows to version 2 for `RTO` and `PartyA` nodes. Upgrade can be done by using the below script, which would copy
 v2-workflows.jar to cordapps directory of both the nodes.

    cd script
    ./upgrade.sh --node=RTO,PartyA --workflow=2
    
**Step2**: Restart the nodes. `RTO` and `PartyA` should now be running version 2 of the flows while `PartyB` would still be running version 1.

**Step3**: After the upgrade we should still be able perform transactions between nodes. Try to register a new vehicle and transfer a 
pre-registered vehicle from the `RTO`'s shell

    start RegistrationInitiatorFlow owner: PartyA, redgNumber: MH01C2323
    start TransferInitiatorFlow newOwner: PartyB, redgNumber: MH01C2322
    
 Note the while transfer of the vehicle, the "Collecting Signature from Counterparty" step would be greyed out which means the step was not executed since `Party B` is 
 still on the older version of the flow. However since our new version of the flow is backward compatible we are still able to transact.

###Scenario 3: Flow Upgrade to version 2 for Party B
**Step1:** Shutdown the nodes and upgrade the flows to version 2 for `PartyB`.

    cd script
    ./upgrade.sh --node=PartyB --workflow=2

**Step2:** Restart the nodes. `PartyB` should not also be running version 2 of the flow.

**Step3:** Since all the three nodes are now running the upgraded version of the flow, we should be able to see the "Collecting Signature from Counterparty" step working.
Run the below commands from the `RTO`'s shell to validate the same.
    
    // Register Vechile just to make sure its working as well.
    start RegistrationInitiatorFlow owner: PartyA, redgNumber: MH01C2324 
    start TransferInitiatorFlow newOwner: PartyB, redgNumber: MH01C2323
    
###Scenario 4: Contract & Flow Upgrade to Introduce New Feature for RTO, Police and PartyA
**Step1:** Shutdown the nodes and upgrade the flows to version 3 and contracts to version 2 for `RTO`, `Police` and `PartyA` nodes. Upgrade can be done by using the below script, which would copy
 v3-workflows.jar and v2-contracts.jar to cordapps directory of both the nodes.

    cd script
    ./upgrade.sh --node=RTO,PartyA,Police --workflow=3 --contract=2
    
**Step2**: Restart the nodes. `RTO`, `Police` and `PartyA` should now be running version 2 of contracts and version 3 of flows while `PartyB` would still be running older versions.

**Step3**: After the upgrade we should have the challan feature introduced. Lets first try out the register and transfer flow to check and validate they still work. 
Run the below commands from the `RTO`'s shell to validate the same.

    start RegistrationInitiatorFlow owner: PartyA, redgNumber: MH01C2325 
    start TransferInitiatorFlow newOwner: PartyB, redgNumber: MH01C2324
    
You should notice that Register works, but the Transfer fails at the counterparty, that's because `PartyB` is on a different version of the contract and thus cannot validate the transaction. 
In order for the transaction to execute successfully `PartyB` must also upgrade to the latest version of the contract.  You may also notice that the registration 
of vehicle to PartyB works fine, that's because PartyB is not a signing party in the transaction hence does not need to run the contract. 

    start RegistrationInitiatorFlow owner: PartyB, redgNumber: MH01C2326
    
**Step4**: Validate that new feature introduced works fine, Issue Pay a challan by running be below command from `Police`'s shell
    
    start IssueChallanInitiatorFlow redgNumber: MH01C2325, rto: RTO, challanValue: 5000
    
Settle challan dues from `PartyA`'s shell using the below command.
    
    start PayChallanInitiatorFlow value: 5000, redgNumber: MH01C2325
    
Above flows would work fine, since the vehicle is registered to PartyA, however if we trigger the `IssueChallanInitiatorFlow` for a vehicle registered with `PartyB`, the flow would wait
indefinitely for the responder to respond, since there is no responder available at `PartyB` as it has not upgraded to the latest version yet. 
You can terminate the flow by pressing `Ctrl+C` in the rpc shell.

    start IssueChallanInitiatorFlow redgNumber: MH01C2321, rto: RTO, challanValue: 5000

###Scenario 5: Contract & Flow Upgrade to Introduce New Feature for PartyB
**Step1:** Shutdown the nodes and upgrade the flows to version 3 and contracts to version 2 for `PartyB`.

    cd script
    ./upgrade.sh --node=PartyB --workflow=3 --contract=2

**Step2:** Restart the nodes. `PartyB` should now also be running updated version of contract and flows.

**Step3:** Validate Transfer flow works for `PartyB` after the upgrade.
    
    start TransferInitiatorFlow newOwner: PartyB, redgNumber: MH01C2324
    
**Step4:** Validate Issue a challan by running be below command from `Police`'s shell & Settle Challan by it form `PartyB`'s shell.

    start IssueChallanInitiatorFlow redgNumber: MH01C2321, rto: RTO, challanValue: 5000
    start PayChallanInitiatorFlow value: 5000, redgNumber: MH01C2321
        
###Scenario 5: Contract Upgrade for PartyA, RTO and Police to Restrict Vehicle Transfer having pending dues.
**Step1:** Shutdown the nodes and upgrade the contracts to version 3 for `RTO`, `PartyA` and `Police`

    cd script
    ./upgrade.sh --node=RTO,PartyA,Police --contract=3
    
**Step2:** Restart the nodes. `RTO`, `Police` and `PartyA` should now be running version 3 of contracts while `PartyB` would still be running versions 2.

**Step3:** Try to initiate transfer flow, this should fail, since all the signing parties are not on the same version of the contract.

    start TransferInitiatorFlow newOwner: PartyA, redgNumber: MH01C2321
    
**Step4:** Try to issue Challan on vehicles.    
    
    //Vehicle owned by PartyB
    start IssueChallanInitiatorFlow redgNumber: MH01C2321, rto: RTO, challanValue: 5000
    //Vehicle owned by PartyA
    start IssueChallanInitiatorFlow redgNumber: MH01C2325, rto: RTO, challanValue: 5000
    
Notice that both of these passes, although `PartyB` is on a different version of the contract, since both of them are non-signing parties and hence contract
is not executed at their end. However PartyB would not be able to receive the updated state, since he would not be able to validate the validity of the 
transaction, without the latest version of the contract. The flows would be checkpoints at his end, and the updates would be receive once he moves to the updated
contract version.
     
    
###Scenario 6: Contract Upgrade to version 3 for PartyB.  
**Step1:** Shutdown the nodes and upgrade the contracts to version 3 for `PartyB`

    cd script
    ./upgrade.sh --node=PartyB --contract=3
    
**Step2:** Restart the nodes. `PartyB` should now be running version 3 of contract.

**Step3:** Run vaultQuery to check if `PartyB` has received the updated states after the version upgrade.

    run vaultQuery contractStateType: corda.samples.upgrades.states.VehicleState
    
**Step4:** Validate the transfer vehicle flow works after contract version upgrade on all parties

    start TransferInitiatorFlow newOwner: PartyA, redgNumber: MH01C2321

Note that this would fail, because of pending challans, Pay the challans and try again and it should pass.    


## Explicit Upgrade Scenarios And Steps