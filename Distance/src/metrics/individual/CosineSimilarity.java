package metrics.individual;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import common.LudRul;
import common.Score;
import game.Game;
import metrics.DistanceMetric;

/**
 * Uses the Cosine Similarity of keyword frequencies between two expanded game description
 * https://en.wikipedia.org/wiki/Cosine_similarity
 * @author Markus
 *
 */
public class CosineSimilarity implements DistanceMetric
{
	Map<String,Map<String, Integer>> allFrequencies = new HashMap<>();
	
	@Override
	public String getName()
	{
		return "CosineSimilarity";
	}
	
	@Override
	public void releaseResources()
	{
		allFrequencies.clear();
	}
	@Override
	public Score distance(final Game gameA, final Game gameB)
	{
		return distance(gameA.description().expanded(),gameB.description().expanded());
	}

	@Override
	public Score distance(final LudRul gameA, final LudRul gameB)
	{
		return distance(gameA.getDescriptionExpanded(),gameB.getDescriptionExpanded());
	}

	@Override
	public Score
			distance(final String gameADescriptionExpanded, final String gameBDescriptionExpanded)
	{
		
		Map<String, Integer> frequencyA = allFrequencies.get(gameADescriptionExpanded);
		if (frequencyA==null) {
			frequencyA = JensenShannonDivergence.getFrequencies(gameADescriptionExpanded);
			allFrequencies.put(gameADescriptionExpanded, frequencyA);
		}
			
		Map<String, Integer> frequencyB = allFrequencies.get(gameBDescriptionExpanded);
		if (frequencyB==null) {
			frequencyB = JensenShannonDivergence.getFrequencies(gameBDescriptionExpanded);
			allFrequencies.put(gameBDescriptionExpanded, frequencyB);
		}
		final Set<String> vocabulary = new HashSet<>(frequencyA.keySet());
		vocabulary.addAll(frequencyB.keySet());
		double nominator = 0.0;
		double denominatorA = 0.0;
		double denominatorB = 0.0;
		
		
		for (final String word : vocabulary)
		{
			final Integer frqARet = frequencyA.get(word);
			final Integer frqBRet = frequencyB.get(word);
			double frqA=0.0;
			double frqB=0.0;
			
			if (frqARet!=null)frqA=frqARet.doubleValue();
			if (frqBRet!=null)frqB=frqBRet.doubleValue();
			nominator+=frqA*frqB;
			denominatorA+=frqA*frqA;
			denominatorB+=frqB*frqB;
		}
		final double denominator = Math.sqrt(denominatorA)*Math.sqrt(denominatorB);
		final double finalVal = 1-nominator/denominator;
		
		return new Score(finalVal);
	}

	@Override
	public Score distance(
			final Game gameA, final List<Game> gameB, final int numberTrials, final int maxTurns,
			final double thinkTime, final String AIName
	)
	{
		// TODO empty clear up with cameron
		return null;
	}

	@Override
	public DistanceMetric getDefaultInstance()
	{
		return new CosineSimilarity();
	}
	

}
