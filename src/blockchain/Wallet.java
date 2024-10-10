package blockchain;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.ECGenParameterSpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Wallet {
	public PrivateKey privateKey;
	public PublicKey publicKey;

	public HashMap<String, TransactionOutput> wUTXOs = new HashMap<String, TransactionOutput>();

	public Wallet() {
		generateKeyPair();
	}

	private void generateKeyPair() {
		try {
			KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ECDSA", "BC");
			SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
			ECGenParameterSpec ecSpec = new ECGenParameterSpec("prime192v1");

			keyGen.initialize(ecSpec, random);
			KeyPair keyPair = keyGen.generateKeyPair();

			privateKey = keyPair.getPrivate();
			publicKey = keyPair.getPublic();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public float getBalance(Map<String, TransactionOutput> UTXOs) {
		float total = 0;
		// 내가 가진 금액만 골라서 금액을 더하고 내역을 저장
		for (Map.Entry<String, TransactionOutput> item : UTXOs.entrySet()) {
			TransactionOutput UTXO = item.getValue();
			if (UTXO.isMine(publicKey)) {
				wUTXOs.put(UTXO.id, UTXO);
				total += UTXO.value;
			}
		}
		return total;
	}

	public Transaction sendFunds(PublicKey _recipient, float value, Map<String, TransactionOutput> UTXOs) {
		// 잔액이 부족하면 거래 거절
		if (getBalance(UTXOs) < value) {
			System.out.println("#Not Enough funds to send transaction. Transaction Discarded.");
			return null;
		}
		ArrayList<TransactionInput> inputs = new ArrayList<TransactionInput>();

		// 이전까지의 거래내역 inputs에 저장
		float total = 0;
		for (Map.Entry<String, TransactionOutput> item : wUTXOs.entrySet()) {
			TransactionOutput UTXO = item.getValue();
			total += UTXO.value;
			inputs.add(new TransactionInput(UTXO.id));
			if (total > value)
				break;
		}

		// 새로운 트랜잭션을 생성하고 서명
		Transaction newTransaction = new Transaction(publicKey, _recipient, value, inputs);
		newTransaction.generateSignature(privateKey);

		for (TransactionInput input : inputs) {
			wUTXOs.remove(input.transactionOutputId);
		}

		return newTransaction;
	}
}
