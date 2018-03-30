

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;


public class BackupProtocol implements Runnable{
	

	private int senderID;
	private int replicationDegree;
	private static Chunk chunk;
	
	
	public BackupProtocol(Chunk chunk,int replicationDegree,int senderID) {
		this.chunk=chunk;
		this.replicationDegree= replicationDegree;
		this.senderID=senderID;
	}
	
    public static void backupFile(String path, int repDegree) {
    	
        File file = new File(path);
        int chunkNo = 1;

        try (BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(file))) {

            int temp = 0;
            do {
                byte[] buffer = new byte[Chunk.SIZE];
                temp = inputStream.read(buffer);
                if(temp == -1) {
                	byte[] emptyData = new byte[0];
                	Chunk chunk= new Chunk(Functions.getHashedFileID(file),chunkNo,repDegree,emptyData);
                BackupProtocol backup = new BackupProtocol(chunk,repDegree,Peer.getID());
                backup.run();
                }else {
                		Chunk chunk= new Chunk(Functions.getHashedFileID(file),chunkNo,repDegree,buffer);
                    BackupProtocol backup = new BackupProtocol(chunk,repDegree,Peer.getID());
                    backup.run();
                }
                
                chunkNo++;
            } while (temp == Chunk.SIZE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
	
	
	
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		
		int attempts=0;
		
		while (attempts < 5 && Peer.getRepDegreeAtual(chunk.getFileID(),chunk.getChunkNo()) < replicationDegree) {
            Services.PUTCHUNK(chunk,senderID);
            try {
                Thread.sleep(2 ^ attempts * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            attempts++;
        }
		if (attempts == 5)
            System.out.println("Couldn't backup chunk");
        else
            System.out.println("Backup for chunk finished successfully");
    }
	
	


}
