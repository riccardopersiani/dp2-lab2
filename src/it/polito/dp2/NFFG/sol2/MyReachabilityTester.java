package it.polito.dp2.NFFG.sol2;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import it.polito.dp2.NFFG.LinkReader;
import it.polito.dp2.NFFG.NffgReader;
import it.polito.dp2.NFFG.NffgVerifier;
import it.polito.dp2.NFFG.NffgVerifierException;
import it.polito.dp2.NFFG.NodeReader;
import it.polito.dp2.NFFG.lab2.NoGraphException;
import it.polito.dp2.NFFG.lab2.ReachabilityTester;
import it.polito.dp2.NFFG.lab2.ServiceException;
import it.polito.dp2.NFFG.lab2.UnknownNameException;


public class MyReachabilityTester implements ReachabilityTester {	
	//Settare proprietà URL...
	private WebTarget target;
	private Set<NodeReader> nodeSet;  
	private Map<NodeReader,String> mapNodeReaderId; 
	private Set<String> linkId;
	private String nffgName;

	//TODO costructor?
	/*public void MyReachabilityTester(){
		System.out.println("-----------CONSTRUCTOR-----------");
		// For each Node send a request to the Server
		nodeSet = new HashSet<>();
		linkId = new HashSet<>();
		mapNodeReaderId = new HashMap<>();
	}*/

	/* Carica su Node4J l'element NFFG con il nome inserito */
	@Override
	public void loadNFFG(String name) throws UnknownNameException, ServiceException {
		/* Questo NFFG da caricare dove si trova? E' uno di quelli generato dinamicamente dalla libreria di Sisto */
		/* Vado a prendere l'emento NFFG inserito */
		NffgVerifier monitor;
		it.polito.dp2.NFFG.NffgVerifierFactory factory = it.polito.dp2.NFFG.NffgVerifierFactory.newInstance();
		try {
			System.out.println("Instantiating the client...in loadNFFG()");
			Client c = ClientBuilder.newClient();	
			if(System.getProperty("it.polito.dp2.NFFG.lab2.URL") == null){
				throw new ServiceException();
			}
			target = c.target(System.getProperty("it.polito.dp2.NFFG.lab2.URL"));		

			this.nffgName = null;

			/* Prima devo cancellare in nodi inseriti in precedenza */
			System.out.println("Deleting nodes...");
			deleteNodes();
			
			nodeSet = new HashSet<>();
			linkId = new HashSet<>();
			mapNodeReaderId = new HashMap<>();

			monitor = factory.newNffgVerifier();
			System.out.println("Getting the nffg...");
			if(monitor.getNffg(name) == null){
				System.out.println("Wrong nffg name...launching UnknownNameException");
				throw new UnknownNameException();
			}
			NffgReader nffg = monitor.getNffg(name);	
			System.out.println("nffg Name: "+nffg.getName());
			this.nffgName = nffg.getName();

			System.out.println("Getting nodes from the nffg...");
			nodeSet = nffg.getNodes();

			int count2=0;
			for (NodeReader nr: nodeSet) {
				count2++;
				Node node = new ObjectFactory().createNode();
				Property property = new ObjectFactory().createProperty();

				property.setName("name");
				property.setValue(nr.getName());
				node.getProperty().add(property);

				Node response = target.path("resource")
						.path("node")
						.request(MediaType.APPLICATION_XML)
						.post(Entity.entity(node, MediaType.APPLICATION_XML),Node.class);

				System.out.println("Node id  "+ response.getId()+ " "+count2);
				mapNodeReaderId.put(nr, response.getId());
			}


			System.out.println("Getting links from the nffg...");
			int count=0;
			int count3=0;
			for(NodeReader nr: nodeSet) {
				count ++;
				System.out.println("Node:  "+ nr.getName()+" "+count);
				for(LinkReader lr: nr.getLinks()) {
					count3++;
					System.out.println("Link:  "+ lr.getName()+" "+count3);
					Path path = new ObjectFactory().createPath();
					Relationship relationship = new ObjectFactory().createRelationship();

					path.getRelationship().add(relationship.getId());

					relationship.setDstNode(mapNodeReaderId.get(lr.getDestinationNode()));
					relationship.setType("Link");

					Relationship response = target.path("resource")
							.path("node")
							.path(mapNodeReaderId.get(nr))
							.path("relationship")
							.request(MediaType.APPLICATION_XML)
							.post(Entity.entity(relationship, MediaType.APPLICATION_XML), Relationship.class);

					//System.out.println("Link id  "+ response.getId());
					linkId.add(response.getId());
				}
			}
			System.out.println("All nodes controlled...");	
		} catch (NffgVerifierException e) {
			System.out.println("INSIDE LOAD CATCH...");	
			throw new ServiceException();
		} catch (RuntimeException e){
			System.out.println("RuntimeException...");	
			throw new ServiceException();
		}
	}

	@Override
	public boolean testReachability(String srcName, String destName)
			throws UnknownNameException, ServiceException, NoGraphException {
		try{
			/* true if the destination node is reachable from the source node, false otherwise */
			System.out.println("Instantiating the client...in testReachability()");
			Client c = ClientBuilder.newClient();	
			if(System.getProperty("it.polito.dp2.NFFG.lab2.URL") == null){
				System.out.println("Instantiating the client...in testReachability() FAILED!!!");
				throw new ServiceException();
			}
			System.out.println("Instantiating the target...in testReachability()");
			target = c.target(System.getProperty("it.polito.dp2.NFFG.lab2.URL"));		


			System.out.println("srcName: " + srcName);
			System.out.println("destName: " + destName);

			System.out.println("Controlling if srcName or destName are empty...");
			if(srcName.isEmpty() || destName.isEmpty()){
				System.out.println("Source or Destination are missing...launching UnknownNameException");
				throw new UnknownNameException();
			}

			System.out.println("Controlling if srcName or destname are good...");
			int flag = 0;
			for (NodeReader nr: nodeSet){
				if(nr.getName().equals(srcName)){
					System.out.println("FLAG1");
					flag++;
				}
				if(nr.getName().equals(destName)){
					System.out.println("FLAG2");
					flag++;
				}
			}
			if(flag!=2){
				System.out.println("Src or dst flags are missing...launching UnknownNameException");
				throw new UnknownNameException();
			}

			String destId = null;
			String srcId = null;

			System.out.println("finding the ids from src and dst...");
			for ( NodeReader key : mapNodeReaderId.keySet() ) {
				if(key.getName().equals(srcName)){
					srcId = mapNodeReaderId.get(key); 
				}
				if(key.getName().equals(destName)){
					destId = mapNodeReaderId.get(key); 
				}
			}

			System.out.println("srcId: " + srcId);
			System.out.println("destId: " + destId);

			System.out.println("Checking if ids are null...");
			if( srcId == null || destId == null){
				System.out.println("srcId or destId are not right bacause of unknown srcName or destName...");
				throw new ServiceException();
			}

			System.out.println("Performing the get(Paths)...");
			Paths response = target.path("resource")
					.path("node")
					.path(srcId)
					.path("paths")
					.queryParam("dst", destId)
					.request()
					.get(Paths.class);

			System.out.println("Paths Response...");
			List<Path> pathList = response.getPath();
			System.out.println("Check if empty...");
			if(!pathList.isEmpty()){
				System.out.println("NOT empty...");
				return true;
			}

			System.out.println("EMPTY...");
			return false;

		}catch(RuntimeException r){
			System.out.println("--- Throwable exception in TEST REACHABILITY --- \n");
			throw new ServiceException();
		}
	}

	@Override
	public String getCurrentGraphName() {
		System.out.println("***NFFG NAME: "+nffgName+" ****");
		return nffgName;
	}

	public void deleteNodes() throws ServiceException{
		try{
			target.path("resource")
			.path("nodes")
			.request(MediaType.APPLICATION_XML)
			.delete();
		} catch (RuntimeException e) {
			System.out.println("--- Throwable exception --- \n");
			throw new ServiceException();
		}
	}

}
