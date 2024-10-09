package blockchain;

import java.security.Security;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class Test {
	private static List<Block> blockchain = new ArrayList<Block>();
	public static Map<String, TransactionOutput> UTXOs = new HashMap<String, TransactionOutput>();
	private static int difficulty = 5;
	public static Wallet walletA;
	public static Wallet walletB;
	public static float minimumTransaction = 0.1f;
	public static Transaction genesisTransaction;
	
	public static void main(String[] args) {
		Security.addProvider(new BouncyCastleProvider());

		walletA = new Wallet();
		walletB = new Wallet();
		Wallet coinbase = new Wallet();
		
		genesisTransaction = new Transaction(coinbase.publicKey, walletA.publicKey, 100f, null);
		genesisTransaction.generateSignature(coinbase.privateKey);
		genesisTransaction.transactionId = "0";
		genesisTransaction.outputs.add(new TransactionOutput(genesisTransaction.recipient, genesisTransaction.value, genesisTransaction.transactionId));
		UTXOs.put(genesisTransaction.outputs.get(0).id, genesisTransaction.outputs.get(0));
		
		// 최초 블록 생성
		System.out.println("Creating and Mining Genesis block");
		Block genesis = new Block("0");
		genesis.addTransaction(genesisTransaction, UTXOs, minimumTransaction);
		addBlock(genesis);

		// 블록 간 코인 전송 테스트
		Block block1 = new Block(genesis.hash);
		System.out.println("\nWalletA's balance is: " + walletA.getBalance(UTXOs));
		System.out.println("\nWalletA is Attempting to send funds (40) to WalletB...");
		block1.addTransaction(walletA.sendFunds(walletB.publicKey, 40f, UTXOs), UTXOs, minimumTransaction);
		
		addBlock(block1);
		System.out.println("\nWalletA's balance is: " + walletA.getBalance(UTXOs));
		System.out.println("WalletB's balance is: " + walletB.getBalance(UTXOs));
		
		Block block2 = new Block(block1.hash);
		System.out.println("\nWalletA Attempting to send more funds (1000) than it has...");
		block2.addTransaction(walletA.sendFunds(walletB.publicKey, 1000f, UTXOs), UTXOs, minimumTransaction);
		
		addBlock(block2);
		System.out.println("\nWalletA's balance is: " + walletA.getBalance(UTXOs));
		System.out.println("WalletB's balance is: " + walletB.getBalance(UTXOs));
		
		Block block3 = new Block(block2.hash);
		System.out.println("\nWalletB is Attempting to send funds (20) to WalletA...");
		block3.addTransaction(walletB.sendFunds( walletA.publicKey, 20, UTXOs), UTXOs, minimumTransaction);
		
		addBlock(block3);
		System.out.println("\nWalletA's balance is: " + walletA.getBalance(UTXOs));
		System.out.println("WalletB's balance is: " + walletB.getBalance(UTXOs));
		
		
//		System.out.println("Private and public keys: ");
//		System.out.println(StringUtil.getStringFromKey(walletA.privateKey));
//		System.out.println(StringUtil.getStringFromKey(walletA.publicKey));
//		
//		Transaction transaction = new Transaction(walletA.publicKey, walletB.publicKey, difficulty, null);
//		transaction.generateSignature(walletA.privateKey);
//		System.out.println("Is signature verified");
//		System.out.println(transaction.verifySignature());
		
//		try {
//			Block genesisBlock = new Block("first block", "0");
//			System.out.println("Trying to Mine block 1");
//			genesisBlock.mineBlock(difficulty);
//			blockchain.add(genesisBlock);
//			
//			Block secondBlock = new Block("second block", genesisBlock.hash);
//			System.out.println("Trying to Mine block 2");
//			secondBlock.mineBlock(difficulty);
//			blockchain.add(secondBlock);
//			
//			Block thirdBlock = new Block("third block", secondBlock.hash);
//			System.out.println("Trying to Mine block 3");
//			thirdBlock.mineBlock(difficulty);
//			blockchain.add(thirdBlock);
//			
//			System.out.println("\nBlockchain is Valid : " + isChainValid());
//			
//			String blockchainJson = new GsonBuilder().setPrettyPrinting().create().toJson(blockchain);
//			System.out.println(blockchainJson);
//			
//		} catch (Exception e) {
//			System.out.println(e);
//		}
	}
	
	public static Boolean isChainValid() {

		Block currentBlock; 
		Block previousBlock;
		String hashTarget = new String(new char[difficulty]).replace('\0', '0');
		HashMap<String,TransactionOutput> tempUTXOs = new HashMap<String,TransactionOutput>();
		tempUTXOs.put(genesisTransaction.outputs.get(0).id, genesisTransaction.outputs.get(0));
		
		for(int i=1; i < blockchain.size(); i++) {
			currentBlock = blockchain.get(i);
			previousBlock = blockchain.get(i-1);
			
			if(!currentBlock.hash.equals(currentBlock.calculateHash()) ){
				System.out.println("#Current Hashes not equal");
				return false;
			}
			
			if(!previousBlock.hash.equals(currentBlock.previousHash) ) {
				System.out.println("#Previous Hashes not equal");
				return false;
			}
			
			if(!currentBlock.hash.substring( 0, difficulty).equals(hashTarget)) {
				System.out.println("#This block hasn't been mined");
				return false;
			}
			
			TransactionOutput tempOutput;
			for(int t=0; t <currentBlock.transactions.size(); t++) {
				Transaction currentTransaction = currentBlock.transactions.get(t);
				
				if(!currentTransaction.verifySignature()) {
					System.out.println("#Signature on Transaction(" + t + ") is Invalid");
					return false; 
				}
				if(currentTransaction.getInputsValue() != currentTransaction.getOutputsValue()) {
					System.out.println("#Inputs are note equal to outputs on Transaction(" + t + ")");
					return false; 
				}
				
				for(TransactionInput input: currentTransaction.inputs) {	
					tempOutput = tempUTXOs.get(input.transactionOutputId);
					
					if(tempOutput == null) {
						System.out.println("#Referenced input on Transaction(" + t + ") is Missing");
						return false;
					}
					
					if(input.UTXO.value != tempOutput.value) {
						System.out.println("#Referenced input Transaction(" + t + ") value is Invalid");
						return false;
					}
					
					tempUTXOs.remove(input.transactionOutputId);
				}
				
				for(TransactionOutput output: currentTransaction.outputs) {
					tempUTXOs.put(output.id, output);
				}
				
				if( currentTransaction.outputs.get(0).recipient != currentTransaction.recipient) {
					System.out.println("#Transaction(" + t + ") output reciepient is not who it should be");
					return false;
				}
				if( currentTransaction.outputs.get(1).recipient != currentTransaction.sender) {
					System.out.println("#Transaction(" + t + ") output 'change' is not sender.");
					return false;
				}
				
			}
			
		}
		System.out.println("Blockchain is valid");
		return true;
		
//		Block currentBlock;
//		Block previousBlock;
//		
//		for (int i = 1 ; i < blockchain.size(); i++) {
//			currentBlock = blockchain.get(i);
//			previousBlock = blockchain.get(i - 1);
//			
//			if (!previousBlock.hash.equals(currentBlock.previousHash)) {
//				System.out.println("Previous hash new equal");
//				return false;
//			}
//		}
//		
//		return true;
	}
	
	public static void addBlock(Block newBlock) {
		newBlock.mineBlock(difficulty);
		blockchain.add(newBlock);
	}
}
