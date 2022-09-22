package alluxio.util.TinyTable;

import alluxio.util.TinyTable.BitManipulation.ChainHelper;
import alluxio.util.TinyTable.BitManipulation.SimpleBitwiseArray;
import alluxio.util.TinyTable.HashFunctions.FingerPrintAux;
import alluxio.util.TinyTable.HashFunctions.GreenHashMaker;
import alluxio.util.TinyTable.RankIndexingTechnique.RankIndexingTechnique;

import java.util.Arrays;
import java.util.List;

public class TinyTable extends SimpleBitwiseArray
{

	// used for debug - counts how many items in the table. 
	protected int nrItems;
	// base index array. 
	public long I0[];
	// IStar array. 
	public long[] IStar;
	// anchor distance array.
	public short[] A;

	
	// used as an object pool for the rank indexing technique. In order to prevent dynamic memory allocation. 
	private int mBitsPerSize;
	private int mBitsPerItem;
	private long sizeMask;
	private final byte[] offsets;
	private final byte[] chain;
	private long maxIter = 500;
	public long findEmptyTime;
	public long addItemTime;
	static int chainLength = 64;
	//Hash function with an object pool... recycle! 
	GreenHashMaker hashFunc;

	public TinyTable(int itemsize,int bitsPerSize, int bucketcapacity,int nrBuckets)
	{
		super(bucketcapacity*nrBuckets, itemsize+bitsPerSize,bucketcapacity);
		this.mBitsPerSize =bitsPerSize;
		this.mBitsPerItem = itemsize;
		this.sizeMask = (1L <<bitsPerSize)-1;
		this.maxAdditionalSize = 0;
		this.nrItems = 0;
		I0 = new long[nrBuckets];
		IStar = new long[nrBuckets];
		A = new short[nrBuckets];
		hashFunc = new GreenHashMaker(itemsize+maxAdditionalSize, nrBuckets, chainLength);
		offsets = new byte[chainLength];
		chain = new byte[chainLength];

		this.BucketCapacity = bucketcapacity;
	}

	public TinyTable(int itemsize, int bucketcapacity,int nrBuckets)
	{
		super(bucketcapacity*nrBuckets, itemsize,bucketcapacity);
		this.maxAdditionalSize = 0;
		this.nrItems = 0;
		I0 = new long[nrBuckets];
		IStar = new long[nrBuckets];
		A = new short[nrBuckets];
		hashFunc = new GreenHashMaker(itemsize+maxAdditionalSize, nrBuckets, chainLength);
		offsets = new byte[chainLength];
		chain = new byte[chainLength];

		this.BucketCapacity = bucketcapacity;
	}

	public FingerPrintAux makeHashWithSize(long item,int size){
		FingerPrintAux fingerPrintAux = hashFunc.createHash(item);
		fingerPrintAux.fingerprint = (fingerPrintAux.fingerprint<<mBitsPerSize)|size;
		return fingerPrintAux;
	}

	public boolean addItem(long item, int size) {
		return this.addItem(makeHashWithSize(item,size));
	}

	public void RemoveItem(long i,int size)
	{
		FingerPrintAux fpaux = makeHashWithSize(i,size);
		this.removeItem(fpaux);	
	}

	public boolean containItem(long item)
	{
		return this.containsItem(hashFunc.createHash(item));
	}

	public boolean containItemWithSize(long item){
		FingerPrintAux fpaux = hashFunc.createHash(item);
		RankIndexingTechnique.getChainAndUpdateOffsets(fpaux, I0, IStar, offsets, chain, fpaux.chainId);
		return (this.FindItemWithSize(fpaux)>=0);
	}

	public long getItemSize(long item){
		FingerPrintAux fingerPrintAux = hashFunc.createHash(item);
		RankIndexingTechnique.getChainAndUpdateOffsets(fingerPrintAux, I0, IStar, offsets, chain, fingerPrintAux.chainId);
		for (int i=0; i<this.chain.length;i++ ) {
			if(chain[i]<0)
				break;
			long fpToCompare = this.Get(fingerPrintAux.bucketId, chain[i]);
			if((fpToCompare>>mBitsPerSize) == fingerPrintAux.fingerprint)
				return fpToCompare&sizeMask;
		}
		return -1;
	}

	public long getNum(long item){
		FingerPrintAux fpaux = hashFunc.createHash(item);
		return getNrItems(fpaux.bucketId);
	}

	public long howmany(int bucketId, int chainId,long fingerprint)
	{
		long[] chain = this.getChain(bucketId, chainId);
		return ChainHelper.howmany(chain, fingerprint, this.itemSize-1);
	}
	@Override
	public int getBucketStart(int bucketId)
	{
		return this.bucketBitSize*bucketId + this.A[bucketId]*this.itemSize;
	}
	@Override
	public int getNrItems(int bucketId)
	{
		return Long.bitCount(this.I0[bucketId]) + Long.bitCount(this.IStar[bucketId]);
	}





	/**
	 * Adds a new fingerPrint to the following bucketNumber and chainNumber, the maximal size 
	 * of supported fingerprint is 64 bits, and it is assumed that the actual data sits on the LSB bits of
	 * long. 
	 * 
	 * According to our protocol, addition of a fingerprint may result in expending the bucket on account of neighboring buckets, 
	 * or down sizing the stored fingerprints to make room for the new one. 
	 * 
	 * In order to support deletions, deleted items are first logically deleted, and are fully 
	 * deleted only upon addition.
	 */
	protected boolean addItem(FingerPrintAux fpAux)
	{
		long start = System.currentTimeMillis();
		int nextBucket = this.findFreeBucket(fpAux.bucketId);
		findEmptyTime+= System.currentTimeMillis()-start;
		if(nextBucket == -1){
			return false;
		}

		upscaleBuckets(fpAux.bucketId,nextBucket);// slow!! scal up or down
		//System.out.println("insert bucket with num:"+getNrItems(fpAux.bucketId)+"and with offsets:"+offsets[64]);
		start = System.currentTimeMillis();
		int idxToAdd = RankIndexingTechnique.addItem(fpAux, I0, IStar,offsets,chain);// FAST 21MS AT TOTAL
		// if we need to, we steal items from other buckets.
		this.PutAndPush(fpAux.bucketId, idxToAdd, fpAux.fingerprint); // fast too 7ms???
		addItemTime+=System.currentTimeMillis()-start;
		return true;
	}

	

	
	protected void removeItem(FingerPrintAux fpaux)
	{
		moveToEnd(fpaux);
		int bucket =0;
		this.RemoveAndShrink(fpaux.bucketId);
		removeItemFromIndex(fpaux);

		for(int i =fpaux.bucketId+1; i<fpaux.bucketId+this.I0.length;i++)
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
		

	}

	private int FindItemWithSize(FingerPrintAux fpaux){
		for (int i=0; i<this.chain.length;i++ ) {
			if(chain[i]<0)
				break;
			long fpToCompare = this.Get(fpaux.bucketId, chain[i]);
			if((fpToCompare>>mBitsPerSize) == fpaux.fingerprint)
				return chain[i];
		}
		return -1;
	}

	private int FindItem(FingerPrintAux fpaux)
	{	
//		List<Integer> chain = RankIndexHashing.getChain(chainNumber, L[bucketNumber], IStar[bucketNumber]);
		for (int i=0; i<this.chain.length;i++ ) {
			if(chain[i]<0)
				break;
			long fpToCompare = this.Get(fpaux.bucketId, chain[i]);
			if(fpToCompare == fpaux.fingerprint)
				return chain[i];
		}
		return -1;

	}
	private int moveToEnd(FingerPrintAux fpaux)
	{	
		
		int chainoffset= RankIndexingTechnique.getChainAndUpdateOffsets(fpaux,I0,IStar,offsets,chain)-1;
		int removedOffset = 0;
		//		for (Integer itemOffset : chain) {
		//			
		//			if(itemOffset<0){
		//				throw new RuntimeException("Item is not there!");
		//			}

		int itemOffset = this.FindItem(fpaux);
		if(itemOffset<0)
			throw new RuntimeException("Not found!");

		removedOffset = itemOffset;
		int lastOffset = chain[chainoffset];
		long lastItem = this.Get(fpaux.bucketId, lastOffset);
//		Assert.assertTrue(chain.containsitemOffset));
		this.Put(fpaux.bucketId, removedOffset, lastItem);
		this.Put(fpaux.bucketId, lastOffset, 0l);
		return lastOffset;



}

	

	protected void removeItemFromIndex(FingerPrintAux fpaux) {
		int chainSize = RankIndexingTechnique.getChainAndUpdateOffsets(fpaux, I0, IStar,this.offsets,this.chain,fpaux.chainId)-1;
		RankIndexingTechnique.RemoveItem(fpaux.chainId, I0, IStar,fpaux.bucketId,offsets,chain,chainSize);
	}

	/**
	 * finds a the closest bucket that can accept the new item. 
	 * if the current bucket is under maximal capacity it is the current bucket, otherwise we steal fingerprints from buckets until we reach
	 * a free bucket. 
	 * @param bucketId
	 * @return
	 */
	private int findFreeBucket(int bucketId) {

		bucketId = bucketId%this.A.length;
		int iter = 0;
		while(this.getNrItems(bucketId)+this.A[bucketId] >=this.BucketCapacity)
		{

			bucketId++;
			bucketId = bucketId%this.A.length;
			iter++;
			if(iter>this.A.length|| iter>maxIter){
				return -1;
			}
		}
		return bucketId;
	}

	private void resizeBuckets(int bucketId,boolean IncrementAnchor) {
		if(!IncrementAnchor)
			return;
		this.replaceMany(bucketId, 0, 0l,this.getBucketStart(bucketId));
		this.A[bucketId]++;
		return;
	}


	public long[] getChain(int bucketId, int chainId)
	{
		List<Integer> chain = RankIndexingTechnique.getChain(chainId, I0[bucketId], IStar[bucketId]);
		long[] result = new long[chain.size()];
		int i =0;
		for (Integer itemOffset : chain) {
			if(itemOffset<0)
				return null;

			long item = this.Get(bucketId, itemOffset);
			result[i++] = item;
		}
		return result;
	}




	private void upscaleBuckets(int bucketNumber, int lastBucket)
	{
		//Bucket may be wrapped around too! 
		while(lastBucket!=bucketNumber)
		{

			resizeBuckets(lastBucket,true);


			if(--lastBucket<0)
			{
				lastBucket = A.length-1;
			}
		}
		return;

	}
	boolean containsItem(FingerPrintAux fpaux)
	{	
		RankIndexingTechnique.getChainAndUpdateOffsets(fpaux, I0, IStar, offsets, chain, fpaux.chainId);
		return (this.FindItem(fpaux)>=0);
	}








/**
 * Put a value at location idx, if the location is taken shift the items to
 * be left until an open space is discovered.
 * 
 */
protected void PutAndPush(int bucketId, int idx, final long value) {
	 this.replaceMany(bucketId, idx, value,this.getBucketStart(bucketId));
	this.nrItems++;
	return;
}

protected void RemoveAndShrink(int bucketId) {
	this.replaceBackwards(bucketId,this.getBucketStart(bucketId));
	return;
}





public int getNrItems() {

	return this.nrItems;
}












}
