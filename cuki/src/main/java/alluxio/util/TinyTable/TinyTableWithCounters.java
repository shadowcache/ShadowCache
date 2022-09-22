package alluxio.util.TinyTable;


import alluxio.util.TinyTable.BitManipulation.ChainHelper;
import alluxio.util.TinyTable.HashFunctions.FingerPrintAux;
import alluxio.util.TinyTable.RankIndexingTechnique.RankIndexingTechnique;

import java.util.List;
/**
 * A simple application, that extend TinyTableCBF class with the capability to efficiently handle variable sized values. 
 * The basic idea is that TinyTableCBF is used as is, however we force fingerprint items to always have 1 in their lower bit.
 * That way we can distinguish them from counters items (that are always stored with 0 in their lower bit).
 * 
 * TinyTable remains the same in functionality to store items.  Note that if this class is used, do not use TinyTable's functions
 * yourself when using it. 
 * @author gilga
 *
 */
public class TinyTableWithCounters extends TinyTable {
	public TinyTableWithCounters(int itemsize, int bucketcapacity,int nrBuckets)
	{
		super(itemsize,bucketcapacity,nrBuckets);
	}
	
	public long GetValue(String item)
	{
		FingerPrintAux fpaux = hashFunc.createHash(item);
		return this.howmany(fpaux.bucketId, fpaux.chainId, fpaux.fingerprint|1l);
	}

	public long GetValue(long item)
	{
		FingerPrintAux fpaux = hashFunc.createHash(item);
		return this.howmany(fpaux.bucketId, fpaux.chainId, fpaux.fingerprint|1l);
	}
	public void RemoveValue(long item){
		FingerPrintAux fpaux = hashFunc.createHash(item);
		fpaux.fingerprint|=1l;
		this.RemoveItemValue(fpaux);
	}
	public void RemoveItem(long item){
		FingerPrintAux fpaux = hashFunc.createHash(item);
		fpaux.fingerprint|=1l;
		this.removeItem(fpaux);
	}
	public boolean ContainItem(long item){
		FingerPrintAux fpaux = hashFunc.createHash(item);
		fpaux.fingerprint|=1;
		return this.containsItem(fpaux);
 	}





	/**
	 *  If the value of an item is reduced we may need a new number of items. 
	 *  We therefore update the index and remove an arbitrary item from its chain. 
	 * @param bucketId
	 * @param chainId
	 */
	private void shrinkChain(int bucketId, int chainId)
	{
		int bucket =0;
		this.RemoveAndShrink(bucketId);
		for(int i =bucketId+1; i<bucketId+this.I0.length;i++)
		{
			bucket = (i)%this.I0.length;
			if(A[bucket]>0)
			{
				RemoveAndShrink(bucket);
				A[bucket]--;
				continue;
			}
			else
			{

				break;
			}
		}
		removeItemFromIndex(new FingerPrintAux(bucketId, chainId,1l));
	}
	/**
	 * The basic function of this functionality, stores a value of up to 64 bits and associate it with the item. 
	 * @param item
	 * @param value
	 */
	public void StoreValue(String item, long value)
	{
		FingerPrintAux fpaux = hashFunc.createHash(item);
		fpaux.fingerprint|=1l;
		StoreValue(fpaux,value);
	}
	public void StoreValue(long item, long value)
	{
		FingerPrintAux fpaux = hashFunc.createHash(item);
		fpaux.fingerprint|=1l;
		StoreValue(fpaux,value);
	}
	protected void StoreValue(FingerPrintAux fpaux,long value)
	{
		if(!this.containsItem(fpaux)) {
			this.addItem(fpaux);
		}
		long[] chain = this.getChain(fpaux.bucketId, fpaux.chainId);
		chain = ChainHelper.storeValue(chain, fpaux.fingerprint, this.itemSize, value);
		// delete
		this.storeChain(fpaux.bucketId, fpaux.chainId, chain);
	}
	protected void RemoveItemValue(FingerPrintAux fpaux){
		long[] chain = this.getChain(fpaux.bucketId, fpaux.chainId);
		chain = ChainHelper.removeValue(chain,fpaux.fingerprint);

		this.storeChain(fpaux.bucketId,fpaux.chainId,chain);
	}
	
	/**
	 * After storing a value the number of items in the chain may change, this function adjust the number to be sufficient for all fingerprint items and counter items. 
	 * @param bucketId
	 * @param chainId
	 * @param items
	 * @return
	 */
	private List<Integer> adjustChainToItems(int bucketId, int chainId,
			long[] items) {
		List<Integer> chain = RankIndexingTechnique.getChain(chainId, I0[bucketId], IStar[bucketId]);
		FingerPrintAux fpaux = new FingerPrintAux(bucketId, chainId,1l);
		// if the chain is shorter than needed we add dummy items.
		if(chain.size()<items.length)
		{
			int diff = items.length-chain.size();
			while(diff>0)
			{
				this.addItem(fpaux);
				diff--;
			}
		}
		// if the chain is longer than needed we remove items.
		if(chain.size()>items.length)
		{
			int diff = chain.size()-items.length;
			while(diff>0)
			{
				shrinkChain(bucketId,chainId);
				diff--;
				this.nrItems--;

			}
			
		}
		chain = RankIndexingTechnique.getChain(chainId, I0[bucketId], IStar[bucketId]);
		return chain;
	}
	/**
	 * stores the following chain of items, in TinyTable. 
	 * @param bucketId
	 * @param chainId
	 * @param items
	 */
	
	private void storeChain(int bucketId, int chainId,long[] items)
	{
		// we change the chain in the table to be the same size as the new chain.
		List<Integer> chainIndexes = adjustChainToItems(bucketId, chainId, items);
		// at this point we are sure that they are the same size. 
//		System.out.println(Items[bucketId]);
//		Assert.assertTrue(chainIndexes.size() == items.length);
		
		//then we put the items in the appropriate indices. 
		for(int i =0; i<items.length;i++)
		{
			int itemOffset = chainIndexes.get(i);
			if(itemOffset<0)
				return;
			this.Put(bucketId, itemOffset,items[i]);
		}
		return;

	}
}
