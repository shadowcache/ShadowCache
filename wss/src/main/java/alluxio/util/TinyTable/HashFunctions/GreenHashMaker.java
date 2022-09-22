package alluxio.util.TinyTable.HashFunctions;

import alluxio.util.TinyTable.HashFunctions.MurmurHash.ModifiedMurmurHash;



public class GreenHashMaker {

	//currently chain is bounded to be 64. 

	private final int fpSize;
	private final long fpMask; 
	private final long chainMask=63l; 
	private final int bucketRange;
	public FingerPrintAux fpaux;
	private final static long Seed64 =0xe17a1465;
	private final static long m = 0xc6a4a7935bd1e995L;
	private final  static int r = 47;

	public GreenHashMaker(int fingerprintsize,int bucketrange, int chainrange)
	{
		this.fpSize = fingerprintsize;
		// finger print cannot be zero so you must choose a finger-print size greater than zero.
		assert(fpSize > 0); 
		this.bucketRange =bucketrange;
		fpMask = (1l<<fpSize)-1;
		fpaux = new FingerPrintAux(0,0,0); 
	}



	public FingerPrintAux createHash(String item)
	{
		final byte[] data = item.getBytes();
////		System.out.println(data.length);
		long hash =  ModifiedMurmurHash.hash64(data, data.length);

//		long hash = item.hashCode()^0xe17a1465;
//		hash ^= (hash >>> 20) ^ (hash >>> 12);
//		 hash ^= (hash >>> 7) ^ (hash >>> 4);
		

		fpaux.fingerprint = hash&fpMask;
		if(fpaux.fingerprint ==0l)
		{
			fpaux.fingerprint++;
		}



		hash>>>=fpSize;
		fpaux.chainId = (int) (hash&chainMask);
		hash>>>=6;
		fpaux.bucketId =  (int) ((hash&Long.MAX_VALUE)%bucketRange);

		return fpaux;
	}


	public FingerPrintAux createHash(long item) {
		
		
		long h =  (Seed64) ^ (m);
		item *= m;
		item ^= item >>> r;
		item *= m;

		h ^= item;
		h *= m;
		
		fpaux.fingerprint = h&fpMask;
		fpaux.fingerprint = (fpaux.fingerprint==0l)?1:fpaux.fingerprint; 
		



		h>>>=fpSize;
		fpaux.chainId = (int) (h&chainMask);
		h>>>=6;
		fpaux.bucketId =  (int) ((h&Long.MAX_VALUE)%bucketRange);

		return fpaux;

	}


	//	public  FingerPrintAux createHash(final byte[] data) {
	//
	//		long hash =  MurmurHashTinyTable.hash64(data, data.length,0xe17a1465);
	//		
	//		fpaux.fingerprint = hash&fpMask;
	//		if(fpaux.fingerprint ==0l)
	//		{
	//			fpaux.fingerprint++;
	//		}
	//
	//
	//
	//		hash>>>=fpSize;
	//		fpaux.chainId = (int) (hash&chainMask);
	//		hash>>>=6;
	//		fpaux.bucketId =  (int) ((hash&Long.MAX_VALUE)%bucketRange);
	//		
	//		return fpaux;
	//		
	//	}



}
