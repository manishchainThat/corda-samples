package corda.samples.upgrades.contracts;

import corda.samples.upgrades.states.VehicleState;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;

// ************
// * VehicleContract *
// ************
public class VehicleContract implements Contract {
    // This is used to identify our contract when building a transaction.
    public static final String ID = "corda.samples.upgrades.contracts.VehicleContract";

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    @Override
    public void verify(LedgerTransaction tx) {
        Command command = tx.getCommand(0);
        if (command.getValue() instanceof Commands.Register){
            VehicleState output = (VehicleState) tx.getOutput(0);
            if(output.getRedgNumber() == null){
                throw new IllegalArgumentException("Registration Number should be issued");
            }
            if(!(command.getSigners().contains(output.getRto().getOwningKey()))){
                throw new IllegalArgumentException("RTO must sign");
            }
        }else if(command.getValue() instanceof Commands.Transfer){
            VehicleState output = (VehicleState) tx.getOutput(0);
            VehicleState input = (VehicleState) tx.getInput(0);
            if(output.getRedgNumber() == null){
                throw new IllegalArgumentException("Vehicle must be registered");
            }
            if(!(command.getSigners().contains(output.getRto().getOwningKey()))){
                throw new IllegalArgumentException("RTO & Owner must sign");
            }
        }else{
            throw new IllegalArgumentException("Invalid Command");
        }
    }

    // Used to indicate the transaction's intent.
    public interface Commands extends CommandData {
        class Register implements Commands {}
        class Transfer implements Commands {}
    }
}