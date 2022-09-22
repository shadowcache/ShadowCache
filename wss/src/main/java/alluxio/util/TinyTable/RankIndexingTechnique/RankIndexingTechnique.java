package alluxio.util.TinyTable.RankIndexingTechnique;


import alluxio.util.TinyTable.HashFunctions.FingerPrintAux;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class RankIndexingTechnique {

	/**
	 * The basic rank operation, composed of a bitwise and and a bit count instruction. 
	 * @param Istar
	 * @param idx
	 * @return
	 */
	public static int rank(long Istar, int idx)
	{
		return Long.bitCount(Istar&((1l<<idx)-1));
	}

	public static int rank128(long Istar,int idx){
		BigInteger bigInteger = BigInteger.ONE.shiftLeft(idx).subtract(BigInteger.ONE).and(BigInteger.valueOf(Istar));
		int debug = Long.bitCount(Istar&((1l<<idx)-1));
		/*
		if(debug!=bigInteger.bitCount()){
			System.out.println("????????");
			System.exit(-1);
		}*/
		return bigInteger.bitCount();
	}
	
	/**
	 *  Counts the number of items stored in this index with bitwise manipulations.
	 * @param I0
	 * @param IStar
	 * @return
	 */
	public static int getNrItems(long I0, long IStar)
	{
		int baseOffset = Long.bitCount(I0);
		
		int offset = 0;
		while(true)
		{ 
			int i =0;
			int nextOffset = Long.bitCount(IStar&((1l<<(baseOffset+offset))-1));
			if(nextOffset == offset || nextOffset == 0)
				break;
			offset= nextOffset;
			i++;

		}		
		return baseOffset+ offset;

	}
	/**
	 *  return true if the chainID bit is set in I0. 
	 * @param I0
	 * @param chainId
	 * @return
	 */
	public static boolean chainExist(long I0, int chainId)
	{
		long mask = (1l<<chainId);
		return (I0&mask)!=0;
	}

	public static void getItemsPerLevelUpTo128New(long[] I0, long[] IStar,byte[] offsets, int bucketId)
	{
		//System.out.println("before 64 new:"+offsets[64]);
		offsets[0] = (byte) Long.bitCount(I0[bucketId]);


		byte offset = 0;
		int i =1;
		long istar = IStar[bucketId];
		while(true)
		{
			byte nextOffset = (byte) rank128(istar,offsets[0]+offset);
			//Long.bitCount(IStar&((1l<<(offsets[0]+offset)-1));
			offsets[i] = (byte) (nextOffset-offset);
			if ( offsets[i++]<=0)
				break;
//			if(nextOffset == offset)
//				break;
			offset= nextOffset;
		}
		//System.out.println("after 64 new:"+offsets[64]);
		return ;
	}

	public static int[] getItemsPerLevelUpTo128(long I0, long IStar)
	{
		int[] temp = new int[129];
		temp[0] = Long.bitCount(I0);
		int offset = 0;
		int i =1;
		while(true)
		{
			int nextOffset = rank128(IStar,temp[0]+offset);
			temp[i++] = nextOffset-offset;
			if ((nextOffset-offset)<=0)
				break;
			if(nextOffset == offset)
				break;
			offset= nextOffset;
		}
		return temp;
	}
	
	/**
	 * Calculates the offset of each chain in the array relative to the last chain.
	 * @param I0
	 * @param IStar
	 * @param offsets - an array to write the result in
	 * @param bucketId
	 */

	public static void getItemsPerLevelUpTo64New(long[] I0, long[] IStar,byte[] offsets, int bucketId)
	{
		//System.out.println("before 64 new:"+offsets[64]);
		offsets[0] = (byte) Long.bitCount(I0[bucketId]);


		byte offset = 0;
		int i =1;
		while(true)
		{ 
			long tempMask = (1l<<(offsets[0]+offset))-1;// 1<<64 = 1 so , we have -64, try to fix this!
			byte nextOffset = (byte) Long.bitCount(IStar[bucketId]&tempMask);
					
					//Long.bitCount(IStar&((1l<<(offsets[0]+offset)-1));
					
//					rank(IStar,offsets[0]+offset);
			offsets[i] = (byte) (nextOffset-offset);
			if(offsets[i]==-63){
				//System.out.println("ss");
			}
			if ( offsets[i++]<=0)
				break;
//			if(nextOffset == offset)
//				break;
			offset= nextOffset;
		}
		//System.out.println("after 64 new:"+offsets[64]);
		return ;
	}


	public static int[] getItemsPerLevelUpTo64(long I0, long IStar)
	{
		int[] temp = new int[65];
		temp[0] = Long.bitCount(I0);
		int offset = 0;
		int i =1;
		while(true)
		{ 
			int nextOffset = rank(IStar,temp[0]+offset);
			temp[i++] = nextOffset-offset;
			if ((nextOffset-offset)<=0)
				break;
			if(nextOffset == offset)
				break;
			offset= nextOffset;
		}
		return temp;
	}


	/**
	 * Calculate the index of the next chain according to the index.
	 * @param chainNumber
	 * @param Index
	 * @param offset - the offset of the bit in the index that tells us where the chain is
	 * @return
	 */
	public static int getNextChainIndex(int chainNumber,long Index, int offset)
	{
		  long modified = Index>>offset;
			long mask = 1l<<chainNumber;
			return ((modified)&(mask)) != 0l?Long.bitCount(modified&((mask)-1)) : -1;
	}

	public static int getNextChainIndex128(int chainNumber,long Index, int offset){
		 return 0;
	}



	/**
	 * Get the index of an item to add. 
	 * @param fpaux
	 * @param I0
	 * @param IStar
	 * @param offsets - outputs the offset of each item
	 * @param chain - outputs the indices of all items in the chain here.
	 * @return the item's index in the bucket
	 */
	public static int addItem(FingerPrintAux fpaux, long[] I0, long[] IStar, 
			byte[] offsets, byte[] chain) {
		long mask = 1l<<fpaux.chainId;

		if((mask&I0[fpaux.bucketId]) == 0l)
		{
			I0[fpaux.bucketId]|=mask;
			int offset = Long.bitCount(I0[fpaux.bucketId]&((1l<<fpaux.chainId)-1));
					//rank(I0[fpaux.bucketId],fpaux.chainId);
			IStar[fpaux.bucketId] = extendZero(IStar[fpaux.bucketId], offset);
			return offset;
		}
		//System.out.println("before update offsets'64 : "+offsets[64]);
		int lastChainLocation  = getChainAndUpdateOffsets(fpaux,I0,IStar,offsets,chain)-1;// YARON added -1
				//getChainWithoutUpdatingOffsets(fpaux,I0, IStar,offsets,chain);
		//System.out.println("before add:"+IStar[fpaux.bucketId]);
		IStar[fpaux.bucketId]|=(1l<<chain[lastChainLocation]);
		//System.out.println("after add:"+IStar[fpaux.bucketId]);
		offsets[lastChainLocation+1]++;
		//getChainAndUpdateOffsets(fpaux,I0,IStar,offsets,chain);
		//System.out.println("before without update offsets'64 : "+offsets[64]);
		getChainWithoutUpdatingOffsets(fpaux,I0,IStar,offsets,chain);
		IStar[fpaux.bucketId] = extendZero(IStar[fpaux.bucketId],chain[lastChainLocation+1]);

		return chain[lastChainLocation+1];
	}

	public static int RemoveItem(int chainNumber, long[] I0,long[] IStar,int bucketId,byte[] offsets, 
			byte[] chain,int maxChainSize)
	{
		if(maxChainSize ==0){
			I0[bucketId]&=~(1l<<chainNumber);
			IStar[bucketId] = shrinkOffset(IStar[bucketId], chain[maxChainSize]);
			//System.out.println("after remove:"+IStar[bucketId]);
			return chain[maxChainSize];

		} else {
			IStar[bucketId]=IStar[bucketId]&(~(1l<<chain[maxChainSize-1]));
			IStar[bucketId] = shrinkOffset(IStar[bucketId],  chain[maxChainSize]);
			//System.out.println("after remove:"+IStar[bucketId]);
			return chain[maxChainSize];
		}


	}


	private static long extendZero(final long IStar, final int offset) {
		long conMask = (1l<<offset)-1;
		return  (IStar&conMask)|((IStar<<1)& (~(conMask))&(~(1l<<offset)));
	}
	/*
	 *  The 'offset' bit, is assumed to be 0. 
	 */
	private static long shrinkOffset(long IStar, int offset) {
		long conMask = ((1l<<offset) -1);
		return  (IStar&conMask)|((~(conMask)&IStar)>>>1);
	}


	public static int getFirst(int chainNumber,long I0,  long IStar)
	{
		return getNextChainIndex(chainNumber,I0,0);
	}



	/**
	 *  return the requested chain offsets in the chain byte array.
	 *  also updates the I* offsets in the offsets byte array.  
	 * @param fpaux
	 * @param I0
	 * @param IStar
	 * @param offsets
	 * @param chain
	 * @param chainId
	 * @return
	 */
	public static int getChainAndUpdateOffsets(FingerPrintAux fpaux,
			long[] I0,  long[] IStar, byte[] offsets, byte[] chain, int chainId)
	{
		//		List<Integer> itemIdx = new ArrayList<Integer>(3);
		getItemsPerLevelUpTo64New(I0,IStar,offsets,fpaux.bucketId);
		// read first item
		int firstItem = getNextChainIndex(chainId,I0[fpaux.bucketId],0);
		chain[0]= (byte) firstItem;
		//		itemIdx.add(firstItem);
		if(firstItem<0) // removed from comment by Yaron
			return 0; //removed from comment by Yaron
		
		int offset =0;
		int i =0;
		while(true)
		{
			firstItem = getNextChainIndex(firstItem,IStar[fpaux.bucketId],offset);
			if(firstItem<0){
				chain[++i]=-1;
				return i;// changed to i by Yaron return i-1; 
			}
			offset += offsets[i];
			chain[++i]=(byte) (offset+firstItem);
		}

	}
	
	/**
	 * Calculate an array of indices to each value.
	 * @param fpaux
	 * @param I0
	 * @param IStar
	 * @param offsets - this is an array of the offset of each chain from the last chain.
	 * @param chain - this is the resulting array of indices.
	 * @return
	 */
	public static int getChainAndUpdateOffsets(FingerPrintAux fpaux,long[] I0, 
			long[] IStar,byte[] offsets,byte[] chain)
	{
		return getChainAndUpdateOffsets(fpaux, I0, IStar, offsets, chain, 
				fpaux.chainId);
	}
	
	/**
	 * relatively unoptimized, as it allocates memory! 
	 * @param chainNumber
	 * @param I0
	 * @param IStar
	 * @return
	 */
	public static List<Integer> getChain(int chainNumber,long I0,  long IStar)
	{
		List<Integer> itemIdx = new ArrayList<Integer>(3);

		int[] offsets = getItemsPerLevelUpTo64(I0,IStar);
		//System.out.println("get chain return:"+offsets[64]);
		int firstItem = getNextChainIndex(chainNumber,I0,0);

		itemIdx.add(firstItem);



		int offset =0;
		int i =0;
		while(firstItem>=0)
		{
			firstItem = getNextChainIndex(firstItem,IStar,offset);
			if(firstItem<0)
				return itemIdx;
			offset += offsets[i];
			// item offset within level.  + level offset
			int levelOffset = offset;

			//			if(!itemIdx.contains(levelOffset+firstItem))
			itemIdx.add(levelOffset+firstItem);
			// go for next level. - if needed.

			i++;


		}
		return itemIdx;

	}
	
	
	
 /**
  * used to prevent calculating offsets twice. it is slightly more efficient than getchainAndUpdateOffsets, but the offsets need to be
  * already updated. used after addition/removals when offsets can be updated manually to include a new addition/removal. 
  * @param fpaux
  * @param I0
  * @param IStar
  * @param offsets
  * @param chain
  * @return
  */
	public static int getChainWithoutUpdatingOffsets(FingerPrintAux fpaux,
			long[] I0,  long[] IStar,byte[] offsets,byte[] chain)
	{

		int nextItem = getNextChainIndex(fpaux.chainId,I0[fpaux.bucketId],0);
		chain[0]= (byte) nextItem;

		int offset =0;
		int i =0;
		//System.out.println("istar:"+IStar[fpaux.bucketId]);
		while(true)
		{
			nextItem = getNextChainIndex(nextItem,IStar[fpaux.bucketId],offset);
			//System.out.print(i+":"+nextItem+" ");
			if(nextItem<0){
				chain[++i]=-1;
				//System.out.printf("\n");
				return i-1;
			}
			//System.out.print(i+":"+offset+" ");
			offset += offsets[i];
			chain[++i]=(byte) (offset+nextItem);
		}

	}

}












