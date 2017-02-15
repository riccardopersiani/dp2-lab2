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
import javax.ws.rs.core.Response;

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
	private WebTarget target;
	private Set<NodeReader> nodeSet;  
	private Map<NodeReader,String> mapNodeReaderId; 
	private Set<String> linkId;
	private String nffgName;

	//TODO costructor?
	public MyReachabilityTester(){
		this.nodeSet = new HashSet<>();
		this.linkId = new HashSet<>();
		this.mapNodeReaderId = new HashMap<>();
	}

	/* Carica su Node4J l'element NFFG con il nome inserito */
	@Override
	public void loadNFFG(String name) throws UnknownNameException, ServiceException {
		NffgVerifier monitor;
		it.polito.dp2.NFFG.NffgVerifierFactory factory = it.polito.dp2.NFFG.NffgVerifierFactory.newInstance();
		try {
			System.out.println("**** Instantiating the client...in loadNFFG() ****");
			Client c = ClientBuilder.newClient();	
			if(System.getProperty("it.polito.dp2.NFFG.lab2.URL") == null){
				System.err.println("Property is null... launching ServiceException");
				throw new ServiceException();
			}
			target = c.target(System.getProperty("it.polito.dp2.NFFG.lab2.URL"));		

			this.nffgName = null;

			/* Prima devo cancellare in nodi inseriti in precedenza e controllo che neo4j sia stato lanciato */
			System.out.println("Deleting nodes...");
			deleteNodes();

			monitor = factory.newNffgVerifier();
			System.out.println("Getting the nffg...");
			
			System.out.println("nffg name inserted in LoadNFFG(): "+ monitor.getNffg(name));

			
			if(monitor.getNffg(name) == null){
				System.err.println("Wrong nffg name...launching UnknownNameException");
				throw new UnknownNameException();
			}
			
			NffgReader nffg = monitor.getNffg(name);	
			System.out.println("nffg Name: "+ nffg.getName());
			this.nffgName = nffg.getName();

			this.nodeSet = nffg.getNodes();

			/* Getting Nodes form the nffg*/
			for (NodeReader nr: nodeSet) {
				Node node = new ObjectFactory().createNode();
				Property property = new ObjectFactory().createProperty();

				property.setName("name");
				property.setValue(nr.getName());
				node.getProperty().add(property);

				Node response = target.path("resource")
						.path("node")
						.request(MediaType.APPLICATION_XML)
						.post(Entity.entity(node, MediaType.APPLICATION_XML), Node.class);

				// System.out.println("Node id  "+ response.getId() + " ");
				if(response.getId() == null){
					System.err.println("Node id is null");
					throw new ServiceException();
				}
				this.mapNodeReaderId.put(nr, response.getId());
			}

			/* Getting Links from the nffg*/
			for(NodeReader nr: nodeSet) {
				// System.out.println("Node:  "+ nr.getName());
				for(LinkReader lr: nr.getLinks()) {
					// System.out.println("Link:  "+ lr.getName());
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

					// System.out.println("Link id  "+ response.getId());
					if(response.getId() == null){
						System.err.println("Relationship id is null");
						throw new ServiceException();
					}
					this.linkId.add(response.getId());
				}
			}
			
		} catch (NffgVerifierException e) {
			System.err.println("NffgVerifierException occurred... launching ServiceException");	
			throw new ServiceException();
		} catch (RuntimeException e){
			System.out.println("RuntimeException occurred... launhching ServiceException");	
			throw new ServiceException();
		}
	}

	@Override
	public boolean testReachability(String srcName, String destName)
			throws UnknownNameException, ServiceException, NoGraphException {
		try{
			/* True if the destination node is reachable from the source node, false otherwise */
			System.out.println("**** Instantiating the client...in testReachability() ****");
			Client c = ClientBuilder.newClient();	
			if(System.getProperty("it.polito.dp2.NFFG.lab2.URL") == null){
				System.err.println("Property is null... launching ServiceException");
				throw new ServiceException();
			}
			target = c.target(System.getProperty("it.polito.dp2.NFFG.lab2.URL"));		

			/* Check if srcName or destName are empty...*/
			if(srcName.isEmpty() || destName.isEmpty()){
				System.err.println("Source or Destination passed are empty... launching UnknownNameException");
				throw new UnknownNameException();
			}

			/* Check if srcName or destName exist...*/
			int flag = 0;
			for (NodeReader nr: nodeSet){
				if(nr.getName().equals(srcName)){
					flag++;
				}
				if(nr.getName().equals(destName)){
					flag++;
				}
			}
			if (flag != 2){
				System.err.println("Source or Destination passed does not exist... launching UnknownNameException");
				throw new UnknownNameException();
			}

			String destId = null;
			String srcId = null;

			/* Saving Src and Dest Ids*/
			for ( NodeReader key : mapNodeReaderId.keySet() ) {
				if(key.getName().equals(srcName)){
					srcId = mapNodeReaderId.get(key); 
				}
				if(key.getName().equals(destName)){
					destId = mapNodeReaderId.get(key); 
				}
			}

			/* Checking if Ids are null */
			if( srcId == null || destId == null){
				System.err.println("Src or Dest Ids are null... launching ServiceException");
				throw new ServiceException();
			}
			
			/* Check if a graph is loaded */
			Nodes nodes_response = target.path("resource")
					.path("nodes")
					.request()
					.get(Nodes.class);
			
			if(nodes_response.getNode().isEmpty() == true){
				System.err.println("No graph is currently loaded... launching NoGraphException");
				throw new NoGraphException();
			}
						
			/* Check if the policy is reachable */
			System.out.println("Performing the get(Paths)...");
			Paths response = target.path("resource")
					.path("node")
					.path(srcId)
					.path("paths")
					.queryParam("dst", destId)
					.request()
					.get(Paths.class);

			List<Path> pathList = response.getPath();
			if(!pathList.isEmpty()){
				return true;
			}
			return false;

		}catch(RuntimeException r){
			System.err.println("RuntimeException in testReachability()");
			throw new ServiceException();
		}
	}

	@Override
	public String getCurrentGraphName() {
		System.out.println("getCurrentGraphName(): " + nffgName);
		if(nffgName == null){
			return null;
		}
		return nffgName;
	}

	public void deleteNodes() throws ServiceException {
		try{
			/* Perform the delete request to neo4J */
			Response response = target.path("resource")
					.path("nodes")
					.request(MediaType.APPLICATION_XML)
					.delete();

			System.out.println("Response Status Number: " + response.getStatus());
			
			/* If an error occurs launch ServiceException */
			if(response.getStatus() > 400 ){
				System.err.println("Response status > 400! Neo4J could be not active!");
				throw new ServiceException();
			}
			
		} catch (RuntimeException e) {
			System.err.println("RuntimeException in deleteNodes()");
			throw new ServiceException();
		}
	}

}
