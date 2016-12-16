package it.polito.dp2.NFFG.sol2;

import it.polito.dp2.NFFG.lab2.NoGraphException;
import it.polito.dp2.NFFG.lab2.ServiceException;
import it.polito.dp2.NFFG.lab2.UnknownNameException;

public class MainTest {
	
	public static void main(String args[]){
		MyReachabilityTester tester = new MyReachabilityTester();
		try {
			tester.loadNFFG("Nffg0");
			boolean test;
			test = tester.testReachability("nonExistingSrcNode","nonExistingSrcNode");
			System.out.println("TEST RESULT: "+ test);
		} catch (NoGraphException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
		
		} catch (UnknownNameException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
