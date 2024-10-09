package blockchain;

import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

public class Block {
	public String hash;
	public String previousHash;
	public String merkleRoot;
	public ArrayList<Transaction> transactions = new ArrayList<Transaction>();
	public long timeStamp;
	public int nonce;

	public Block(String previousHash) {
		this.previousHash = previousHash;
		this.timeStamp = new Date().getTime();
		this.hash = calculateHash();
	}

	public String calculateHash() {
		String calculatedHash = StringUtil
				.applySha256(previousHash + Long.toBinaryString(timeStamp) + Integer.toString(nonce) + merkleRoot);

		return calculatedHash;
	}
	
	public void mineBlock(int difficulty) {
		String target = new String(new char[difficulty]).replace('\0', '0');
		while (!hash.substring(0, difficulty).equals(target)) {
			nonce++;
			hash = calculateHash();
		}
		System.out.println("Block Mined : " + hash);
	}
	
	public boolean addTransaction(Transaction transaction, Map<String, TransactionOutput> UTXOs, float minimumTransaction) {
		if (transaction == null) return false;
		if (previousHash != "0") {
			if (transaction.processTransaction(UTXOs, minimumTransaction) != true) {
				System.out.println("Transaction failed to process.");
				return false;
			}
		}
		transactions.add(transaction);
		System.out.println("Transaction Successfully added to Block");
		return true;
	}
}
