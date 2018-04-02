import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class Peer implements RMI_inteface {

	private static int ID;
	private static Services services;
	private static MC_Dispatcher mcDispatcher;
	private static MDB_Dispatcher mdbDispatcher;
	public static int MEMORY = 10000000;
	private static int used_space = 0;
	public static PeerInfo info;

	@Override
	public void backup_file(File file, int replicationDegree) throws RemoteException {
		System.out.println("Starting backing up");
		String File_ID = Functions.getHashedFileID(file);

		System.out.println("FileID is:" + File_ID);

		BackupProtocol.backupFile(file, replicationDegree);

	}
	
	@Override
	public void delete_file(File file) throws RemoteException {
		System.out.println("Starting Deletion");
		String File_ID = Functions.getHashedFileID(file);
		MDB_Dispatcher.peershavingChunks.remove(File_ID);
		Services.DELETE(File_ID, ID);
		
	}

	public static int getID() {
		return ID;
	}

	public void setID(int ID) {
		this.ID = ID;
	}

	public static Services getServices() {
		return services;
	}

	public static int getUsedSpace() {
		return used_space;
	}

	

	public static void main(String[] args) throws UnknownHostException, ClassNotFoundException {
		InetAddress[] adresses = new InetAddress[3];
		ID=Integer.parseInt(args[1]);
		int[] ports = new int[] { 8000, 8001, 8002 };
		adresses[0] = InetAddress.getByName("224.0.0.0");
		adresses[1] = InetAddress.getByName("224.0.0.0");
		adresses[2] = InetAddress.getByName("224.0.0.0");
//		savedChunks = new ConcurrentHashMap<>();
//		repDegreePerFile=new ConcurrentHashMap<>();
//		peersContainingChunks=new ConcurrentHashMap<>();
		
//		 try {
//			FileInputStream fileIn = new FileInputStream("peerData/peer"+ID);
//			try {
//				ObjectInputStream in = new ObjectInputStream(fileIn);
//				info=(PeerInfo)in.readObject();
//				in.close();
//		        fileIn.close();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		} catch (FileNotFoundException e1) {
//			info=new PeerInfo();
//			try {
//		         FileOutputStream fileOut =
//		         new FileOutputStream("peerData/peer"+ID+".ser");
//		         ObjectOutputStream out = new ObjectOutputStream(fileOut);
//		         out.writeObject(info);
//		         out.close();
//		         fileOut.close();
//		      } catch (IOException i) {
//		         i.printStackTrace();
//		      }
//			
//		}
		 
		

		Peer peer = new Peer();
		try {
			RMI_inteface stub = (RMI_inteface) UnicastRemoteObject.exportObject(peer, 0);

			LocateRegistry.getRegistry().rebind(args[0], stub);
		} catch (RemoteException e) {
			System.err.println("Server exception: " + e.toString());
			e.printStackTrace();
		}

		mcDispatcher = new MC_Dispatcher(ports[0], adresses[0]);
		mdbDispatcher = new MDB_Dispatcher(ports[1], adresses[1]);
		// faltam os outros

		new Thread(mcDispatcher).start();
		new Thread(mdbDispatcher).start();
		// falta iniciar as outras threads

	}

	public static MC_Dispatcher getMcDispacther() {
		return mcDispatcher;
	}

	public static MDB_Dispatcher getMDBDispacther() {
		return mdbDispatcher;
	}

	public static void saveChunk(String file_ID, int chunkNO, int replication_degree, byte[] body) {
		if (PeerInfo.savedChunks.containsKey(file_ID) == false) {
			System.out.println("Does not contain the file yet");
			PeerInfo.savedChunks.put(file_ID, new ArrayList<>());
			PeerInfo.repDegreePerFile.put(file_ID, replication_degree);

		}
		if(PeerInfo.savedChunks.get(file_ID).contains(chunkNO))
			PeerInfo.savedChunks.get(file_ID).remove(chunkNO);
		
		
		PeerInfo.savedChunks.get(file_ID).add(chunkNO);
		

		used_space += body.length;

		File chunkFile = new File("chunksDir/", file_ID + "_" + chunkNO);
		chunkFile.getParentFile().mkdirs();
		try {
			FileOutputStream out = new FileOutputStream(chunkFile);
			out.write(body);
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Chunk Saved");
	}
	
	public static void deleteFile(String file_ID) {
		if (PeerInfo.savedChunks.containsKey(file_ID) == false) { // esta a dar false aqui...dont know why
			System.out.println("Does not contain the file");
			
		}else {
			int nchunks=PeerInfo.savedChunks.get(file_ID).size();
			PeerInfo.savedChunks.remove(file_ID);
			PeerInfo.repDegreePerFile.remove(file_ID);
			
			PeerInfo.peersContainingChunks.remove(file_ID);
			
			
			for(int i=0;i< nchunks;i++) {
				String fileName = file_ID + "_" + (i	+1);
				File chunkFile = new File("chunksDir/"+fileName);   //nao sei se isto esta a apagar os ficheiros
				chunkFile.delete();
				//como remover do used_space o tamanho do ficheiro?
			}
		}
		
		
	}

}