package main.collections;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.list.array.TIntArrayList;

/**
 * Utility methods for lists
 * 
 * @author Dennis Soemers
 */
public class ListUtils 
{
	
	//-------------------------------------------------------------------------
	
	/**
	 * Constructor
	 */
	private ListUtils()
	{
		// Should not be used
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * @param list A single list
	 * @return A list containing all possible permutations of the given list
	 */
	public static List<TIntArrayList> generatePermutations(final TIntArrayList list)
	{
		if (list.size() == 0)
		{
			List<TIntArrayList> perms = new ArrayList<TIntArrayList>(1);
			perms.add(new TIntArrayList(0, list.getNoEntryValue()));
			return perms;
		}
		
		final int lastElement = list.removeAt(list.size() - 1);
		final List<TIntArrayList> perms = new ArrayList<TIntArrayList>();
		
		final List<TIntArrayList> smallPerms = generatePermutations(list);
		for (final TIntArrayList smallPerm : smallPerms)
		{
			for (int i = smallPerm.size(); i >= 0; --i)
			{
				TIntArrayList newPerm = new TIntArrayList(smallPerm);
				newPerm.insert(i, lastElement);
				perms.add(newPerm);
			}
		}
		
		return perms;
	}
	
	/**
	 * NOTE: it's theoretically possible that we generate duplicate permutations, but for large lists
	 * and a small number of samples this is very unlikely.
	 * 
	 * @param list A single list
	 * @param numPermutations Number of permutations we want to generate
	 * @return A list containing a sample of all possible permutations of the given list (with replacement)
	 */
	public static List<TIntArrayList> samplePermutations(final TIntArrayList list, final int numPermutations)
	{
		final List<TIntArrayList> perms = new ArrayList<TIntArrayList>(numPermutations);
		
		for (int i = 0; i < numPermutations; ++i)
		{
			final TIntArrayList randomPerm = new TIntArrayList(list);
			randomPerm.shuffle(ThreadLocalRandom.current());
			perms.add(randomPerm);
		}
		
		return perms;
	}
	
	/**
	 * NOTE: this may not be the most efficient implementation. Do NOT use
	 * for performance-sensitive situations
	 * 
	 * @param optionsLists List of n lists of options.
	 * @return List containing all possible n-tuples. Officially, this is called Cartesian Product :)
	 */
	public static <E> List<List<E>> generateTuples(final List<List<E>> optionsLists)
	{
		final List<List<E>> allTuples = new ArrayList<List<E>>();
		
		if (optionsLists.size() > 0)
		{
			final List<E> firstEntryOptions = optionsLists.get(0);
			final List<List<E>> remainingOptionsLists = new ArrayList<List<E>>();
			
			for (int i = 1; i < optionsLists.size(); ++i)
			{
				remainingOptionsLists.add(optionsLists.get(i));
			}
			
			final List<List<E>> nMinOneTuples = generateTuples(remainingOptionsLists);
			
			for (int i = 0; i < firstEntryOptions.size(); ++i)
			{
				for (final List<E> nMinOneTuple : nMinOneTuples)
				{
					final List<E> newTuple = new ArrayList<E>(nMinOneTuple);
					newTuple.add(0, firstEntryOptions.get(i));
					allTuples.add(newTuple);
				}
			}
		}
		else
		{
			allTuples.add(new ArrayList<E>(0));
		}
		
		return allTuples;
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * @param list
	 * @return Index of maximum entry in the list
	 * 	(breaks ties by taking the lowest index).
	 */
	public static int argMax(final TFloatArrayList list)
	{
		int argMax = 0;
		float maxVal = list.getQuick(0);
		
		for (int i = 1; i < list.size(); ++i)
		{
			final float val = list.getQuick(i);
			
			if (val > maxVal)
			{
				maxVal = val;
				argMax = i;
			}
		}
		
		return argMax;
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * Removes element at given index. Does not shift all subsequent elements,
	 * but only swaps the last element into the removed index.
	 * @param <E>
	 * @param list
	 * @param idx
	 */
	public static <E> void removeSwap(final List<E> list, final int idx)
	{
		final int lastIdx = list.size() - 1;
		list.set(idx, list.get(lastIdx));
		list.remove(lastIdx);
	}
	
	/**
	 * Removes all elements from the given list that satisfy the given predicate, using
	 * remove-swap (which means that the order of the list may not be preserved).
	 * @param list
	 * @param predicate
	 */
	public static <E> void removeSwapIf(final List<E> list, final Predicate<E> predicate)
	{
		for (int i = list.size() - 1; i >= 0; --i)
		{
			if (predicate.test(list.get(i)))
				removeSwap(list, i);
		}
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * Generates all combinations of given target combination-length from
	 * the given list of candidates (without replacement, order does not
	 * matter). Typical initial call would look like:<br>
	 * <br>
	 * <code>generateAllCombinations(candidates, targetLength, 0, new int[targetLength], outList)</code>
	 * 
	 * @param candidates
	 * @param combinationLength
	 * @param startIdx Index at which to start filling up results array
	 * @param currentCombination (partial) combination constructed so far
	 * @param combinations List of all result combinations
	 */
	public static void generateAllCombinations
	(
		final TIntArrayList candidates,
		final int combinationLength,
		final int startIdx,
		final int[] currentCombination,
		final List<TIntArrayList> combinations
	)
	{
		if (combinationLength == 0)
		{
			combinations.add(new TIntArrayList(currentCombination));
		}
		else
		{
			for (int i = startIdx; i <= candidates.size() - combinationLength; ++i)
			{
				currentCombination[currentCombination.length - combinationLength] = candidates.getQuick(i);
				generateAllCombinations(candidates, combinationLength - 1, i + 1, currentCombination, combinations);
			}
		}
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * NOTE: this should only be used for diagnosing performance issues / temporary code!
	 * This uses reflection and is way too slow and should not be necessary for 
	 * any permanent code.
	 * 
	 * @param l
	 * @return The capacity (maximum size before requiring re-allocation) of given ArrayList
	 */
	public static int getCapacity(final ArrayList<?> l)
	{
		try
		{
			final Field dataField = ArrayList.class.getDeclaredField("elementData");
	        dataField.setAccessible(true);
	        return ((Object[]) dataField.get(l)).length;
		}
		catch 
		(
			final NoSuchFieldException 	| 
			SecurityException 			| 
			IllegalArgumentException 	| 
			IllegalAccessException exception
		)
		{
			exception.printStackTrace();
		} 
        
		return -1;
    }
	
	//-------------------------------------------------------------------------

}
