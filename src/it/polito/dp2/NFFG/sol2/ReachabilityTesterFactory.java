package it.polito.dp2.NFFG.sol2;

import it.polito.dp2.NFFG.lab2.ReachabilityTester;
import it.polito.dp2.NFFG.lab2.ReachabilityTesterException;

public class ReachabilityTesterFactory extends it.polito.dp2.NFFG.lab2.ReachabilityTesterFactory {

	@Override
	public ReachabilityTester newReachabilityTester() throws ReachabilityTesterException {
		// TODO the method newReachabilityTester(), creates an instance of your concrete class that implements the ReachabilityTester interface
		ReachabilityTester myReachabilityTester = null;
		myReachabilityTester = new MyReachabilityTester();
		System.out.println("new MyReachabilityTester() successfully called.");
		return myReachabilityTester;
	}
	

}
