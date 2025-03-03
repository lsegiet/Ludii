package metrics.support;

import java.util.ArrayList;
import java.util.List;

import other.action.Action;
import other.move.Move;
import other.trial.Trial;

public class TrialHelper
{

	/**
	 * list of all actions of this trial, sorted by their occurrence
	 * 
	 * @param trial
	 * @return A list of actions.
	 */
	public static ArrayList<Action> listAllActions(final Trial trial)
	{
		final ArrayList<Action> actions = new ArrayList<>();
		final List<Move> completeMoveList = trial.generateCompleteMovesList();
		for (final Move m : completeMoveList)
		{
			actions.addAll(m.actions());
		}
		return actions;
	}

	/**
	 * are both actions of the same Type
	 */
	public static boolean isEqualType(final Action actionA, final Action actionB)
	{
		return actionA.getClass().equals(actionB.getClass());
	}

}
