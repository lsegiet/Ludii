package other.action.state;

import other.action.Action;
import other.action.BaseAction;
import other.context.Context;

/**
 * Stores the current state of the game.
 *
 * @author Eric.Piette
 */
public class ActionStoreStateInContext extends BaseAction
{
	private static final long serialVersionUID = 1L;

	//-------------------------------------------------------------------------

	/**
	 * 
	 */
	public ActionStoreStateInContext()
	{
		// Do nothing
	}

	/**
	 * Reconstructs an ActionState object from a detailed String (generated using
	 * toDetailedString())
	 *
	 * @param detailedString
	 */
	public ActionStoreStateInContext(final String detailedString)
	{
		assert (detailedString.startsWith("[StoreStateInContext:"));

		final String strDecision = Action.extractData(detailedString, "decision");
		decision = (strDecision.isEmpty()) ? false : Boolean.parseBoolean(strDecision);
	}

	//-------------------------------------------------------------------------

	@Override
	public Action apply(final Context context, final boolean store)
	{
		context.state().storeCurrentState(context.state());
		return this;
	}

	//-------------------------------------------------------------------------

	@Override
	public String toTrialFormat(final Context context)
	{
		final StringBuilder sb = new StringBuilder();

		sb.append("[StoreStateInContext:");
		if (decision)
			sb.append("decision=" + decision);

		sb.append(']');

		return sb.toString();
	}

	@Override
	public int hashCode()
	{
		final int result = 1;
		return result;
	}

	@Override
	public boolean equals(final Object obj)
	{
		if (this == obj)
			return true;

		if (!(obj instanceof ActionStoreStateInContext))
			return false;

		return true;
	}

	//-------------------------------------------------------------------------
	
	@Override
	public String getDescription() 
	{
		return "StoreStateInContext";
	}

	@Override
	public String toTurnFormat(final Context context, final boolean useCoords)
	{
		return "Store State";
	}

	@Override
	public String toMoveFormat(final Context context, final boolean useCoords)
	{
		return "(Store State)";
	}
}
