package main;
import java.util.*;
import java.lang.*;
import java.math.BigInteger;
import java.io.*;
import java.net.*;

public class ClientHandler extends Thread{
	Socket server;
	int port;
	PeerVar parameters;
	//Map<BigInteger,Successor> fingerTable;
	ClientHandler(Socket conn,int port,PeerVar parameters){
		this.server=conn;
		this.port=port;
		this.parameters=parameters;
	}
	
	public void run(){
		System.out.println("Just connected to "
                + server.getRemoteSocketAddress());
         DataInputStream in = null;
		try {
			in = new DataInputStream(server.getInputStream());
			String received = in.readUTF();
			String[] arguments = received.split(":");
			System.out.println("recevied msg: "+received);
			
			DataOutputStream out = null;
			out = new DataOutputStream(server.getOutputStream());
			
			switch(arguments[0]) {
				case "closestPrecedingFinger" : out.writeUTF(closestPrecedingFinger(new BigInteger(arguments[1])));
												break;
				case "findPredecessor" 		  : out.writeUTF(findPredecessor(new BigInteger(arguments[1])));
												break;
				case "findSuccessor:"		  : out.writeUTF(findSuccessor(new BigInteger(arguments[1])));
												break;
				case "join"					  : join(Integer.parseInt(arguments[1]));
												break;
				case "getPredecessor"		  : out.writeUTF(getPredecessor());
												break;
				case "updateFingerTable"	  : updateFingerTable(new BigInteger(arguments[1]), Integer.parseInt(arguments[2]), Integer.parseInt(arguments[3]));
												break;
				case "getIthFinger"			  : out.writeUTF(getIthFinger(Integer.parseInt(arguments[1])));
												break;
				/*case "moveKeys"				  : out.writeUTF(moveKeys(new BigInteger(arguments[1]))));
												break;*/
			}
			
			out.writeUTF("Thank you for connecting to "
				    + server.getLocalSocketAddress() + "\nGoodbye!");
			server.close();
		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		finally{
		
	}
	
	
	public String closestPrecedingFinger(BigInteger id){
		for(int i=Vars.m-1;i>=0;i--){
			if(Vars.isInRange(false, false, parameters.nodeName, id, parameters.fingerTable.get(i).node)){
				return parameters.fingerTable.get(i).node.toString()+" "+ String.valueOf(parameters.fingerTable.get(i).port);
			}
		}
		return parameters.nodeName.toString()+" "+this.parameters.port;
	} 
	
	
	public String findPredecessor(BigInteger id){
		
		if(parameters.nodeName.compareTo(parameters.succ)==0)
			return parameters.nodeName.toString()+" "+parameters.port;
		BigInteger nodePrime=parameters.nodeName;
		// Here we need to connect to the server of succs
		BigInteger nodePrimeSucc=parameters.succ;
		String temp="";
		OurRMI ourRMI;
		String resultPort="";
		while(!Vars.isInRange(false, true, nodePrime, nodePrimeSucc, id)){
			if(nodePrime.compareTo(parameters.nodeName)==0){
				temp=this.closestPrecedingFinger(id);
			}
			else{
					ourRMI = new OurRMI(Integer.parseInt(temp.split(" ")[1]),"closestPrecedingFinger:"+id.toString()+": "+": "+": ");
					temp=ourRMI.result();
			}
			nodePrime=new BigInteger(temp.split(" ")[0]);
			resultPort=temp.split(" ")[1];
			if(nodePrime.compareTo(parameters.nodeName)!=0){
				   ourRMI=new OurRMI(Integer.parseInt(temp.split(" ")[1]),"findSuccessor:"+id.toString()+": "+": "+": ");
				   nodePrimeSucc=new BigInteger(ourRMI.result().split(" ")[0]);	   
			}
			else
				break;
		}
		return nodePrime.toString()+" "+resultPort;
	}
	
	public String findSuccessor(BigInteger id){
		OurRMI ourRMI = new OurRMI(Integer.parseInt(findPredecessor(id).split(" ")[1]),"findSuccessor:"+id.toString()+": "+": "+": ");
		return ourRMI.result();
	}
	
	
	public void join(int friend) {
		Socket  client;
		try {
			client=new Socket("localhost",friend);
			} catch (UnknownHostException e) {
				for(int i=0;i<Vars.m;i++){
					Successor temp=new Successor(new BigInteger("2").pow(i),
							new BigInteger("2").pow(i+1),parameters.nodeName,parameters.port);
					parameters.fingerTable.add(temp);
				}
				parameters.succ=parameters.nodeName;
				parameters.pred=parameters.nodeName;
				parameters.succPort=parameters.port;
				parameters.predPort=parameters.port;
				
				System.out.println("basic join executed:"+parameters.succ);
				
				return;
			//	e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
		initFingerTable(friend);
		notifyPeers();
		
		OurRMI ourRMI = new OurRMI(parameters.succPort,"getPredecessor:"+": "+": "+": "+": ");
		String res = ourRMI.result();
		
		parameters.pred = new BigInteger(res.split(" ")[0]);
		parameters.predPort = Integer.parseInt(res.split(" ")[1]);
		
	//  get successor S
	// move the keys to n
		
	}
	
	public String getPredecessor() {
		return parameters.pred.toString() + " " + String.valueOf(parameters.predPort);
	}
	
	public void initFingerTable(int port){
		OurRMI ourRMI=new OurRMI(port,"findSuccessor:"+parameters.fingerTable.get(1).intervalStart.toString()+": "+": "+": ");
		String res=ourRMI.result();
		parameters.fingerTable.get(1).node = new BigInteger(res.split(" ")[0]);
		parameters.fingerTable.get(1).port= Integer.parseInt(res.split(" ")[1]);
		parameters.succ = parameters.fingerTable.get(1).node;
		parameters.succPort = parameters.fingerTable.get(1).port;
		
		for(int i=0;i<Vars.m;i++){
			if(Vars.isInRange(true,false,parameters.nodeName, parameters.fingerTable.get(i).node,parameters.fingerTable.get(i+1).intervalStart)){
				parameters.fingerTable.get(i+1).node = parameters.fingerTable.get(i).node;
				parameters.fingerTable.get(i+1).port = parameters.fingerTable.get(i).port;
			}
			else{
				ourRMI=new OurRMI(port,"findSuccessor:"+parameters.fingerTable.get(1).intervalStart.toString()+": "+": "+": ");
				res=ourRMI.result();
				parameters.fingerTable.get(i+1).node = new BigInteger(res.split(" ")[0]);
				parameters.fingerTable.get(i+1).port= Integer.parseInt(res.split(" ")[1]);
			}
		}
	}
	
	
	public void notifyPeers(){
		OurRMI ourRMI;
		String res, res1;
		BigInteger nodeDiff;
		
		for(int i=0;i<Vars.m;i++){
			res=findPredecessor(parameters.nodeName.subtract(new BigInteger("2").pow(i)));
			if(parameters.nodeName.subtract(new BigInteger(res.split(" ")[0])).compareTo(BigInteger.ZERO) < 0) {
				// if n - p < 0 then nodeDiff = (n-p +m) %m
				nodeDiff = parameters.nodeName.subtract(new BigInteger(res.split(" ")[0])).add(new BigInteger(String.valueOf(Vars.m))).mod(new BigInteger(String.valueOf(Vars.m)));
			} else {
				nodeDiff = parameters.nodeName.subtract(new BigInteger(res.split(" ")[0]));
			}
			
			ourRMI=new OurRMI(Integer.valueOf(res.split(" ")[1]),"getIthFinger:"+i+": "+": "+": ");
			res1 = ourRMI.result();
			if(nodeDiff.compareTo(new BigInteger("2").pow(i)) < 0 || 
					!Vars.isInRange(false, false, parameters.nodeName, new BigInteger(res.split(" ")[0]), new BigInteger(res1.split(" ")[0]))) {
						continue;
			}
			
			ourRMI=new OurRMI(Integer.parseInt(res.split(" ")[1]),"updateFingerTable:"+parameters.nodeName+":"+parameters.port+":"+i+": ");
		}
	}
	
	public void updateFingerTable(BigInteger nodeName, int port, int i) {
		OurRMI ourRMI;
		if(Vars.isInRange(true, false, parameters.nodeName, parameters.fingerTable.get(i).node, nodeName)) {
			parameters.fingerTable.get(i).node = nodeName;
			parameters.fingerTable.get(i).port = port;
			
			ourRMI=new OurRMI(parameters.predPort,"updateFingerTable:"+nodeName+":"+port+":"+i+": ");
			ourRMI.result();
		}
		
	}
	
	//Send ith node and port
	public String getIthFinger(int i) {
		return parameters.fingerTable.get(i).node.toString() + " " + String.valueOf(parameters.fingerTable.get(i).port);
	}
}
