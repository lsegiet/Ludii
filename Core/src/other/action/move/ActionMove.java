package other.action.move;

import java.util.BitSet;
import java.util.List;

import game.equipment.component.Component;
import game.equipment.container.board.Track;
import game.rules.play.moves.Moves;
import game.types.board.RelationType;
import game.types.board.SiteType;
import game.util.directions.AbsoluteDirection;
import game.util.directions.DirectionFacing;
import game.util.graph.Radial;
import gnu.trove.list.array.TIntArrayList;
import main.Constants;
import other.action.Action;
import other.action.ActionType;
import other.action.BaseAction;
import other.concept.Concept;
import other.context.Context;
import other.state.container.ContainerState;
import other.state.track.OnTrackIndices;
import other.topology.Topology;
import other.topology.TopologyElement;

/**
 * Moves a piece from a site to another (only one piece or one full stack).
 *
 * @author Eric.Piette
 */
public final class ActionMove extends BaseAction
{
	private static final long serialVersionUID = 1L;

	//-------------------------------------------------------------------------

	/** The graph element type of the from site. */
	private final SiteType typeFrom;

	/** From site index. */
	private final int from;

	/** From level index (e.g. stacking game). */
	private int levelFrom;

	/** The graph element type of the to site. */
	private final SiteType typeTo;

	/** To site index. */
	private final int to;

	/** To level index (e.g. stacking game). */
	private final int levelTo;

	/** Site state value of the to site. */
	private final int state;

	/** Rotation value of the to site. */
	private final int rotation;

	/** piece value of the to site. */
	private final int value;

	/** Stacking game or not. */
	private final boolean onStacking;

	//-------------------------------------------------------------------------

	/**
	 * @param typeFrom   The graph element type of the from site.
	 * @param from       From site index.
	 * @param levelFrom  From level index.
	 * @param typeTo     The graph element type of the to site.
	 * @param to         To site index.
	 * @param levelTo    To level index.
	 * @param state      The state site of the to site.
	 * @param rotation   The rotation value of the to site.
	 * @param value      The piece value of the to site.
	 * @param onStacking True if we move a full stack.
	 */
	public ActionMove
	(
		final SiteType typeFrom,
		final int from,
		final int levelFrom,
		final SiteType typeTo,
		final int to,
		final int levelTo,
		final int state,
		final int rotation,
		final int value,
		final boolean onStacking
	)
	{
		this.typeFrom = typeFrom;
		this.from = from;
		this.levelFrom = levelFrom;
		this.typeTo = typeTo;
		this.to = to;
		this.levelTo = levelTo;
		this.state = state;
		this.rotation = rotation;
		this.value = value;
		this.onStacking = onStacking;
	}

	/**
	 * Reconstructs an ActionMove object from a detailed String
	 * (generated using toDetailedString())
	 * @param detailedString
	 */
	public ActionMove(final String detailedString)
	{
		assert (detailedString.startsWith("[Move:"));

		final String strTypeFrom = Action.extractData(detailedString, "typeFrom");
		typeFrom = (strTypeFrom.isEmpty()) ? null : SiteType.valueOf(strTypeFrom);

		final String strFrom = Action.extractData(detailedString, "from");
		from = Integer.parseInt(strFrom);

		final String strLevelFrom = Action.extractData(detailedString, "levelFrom");
		levelFrom = (strLevelFrom.isEmpty()) ? Constants.UNDEFINED : Integer.parseInt(strLevelFrom);
		
		final String strTypeTo = Action.extractData(detailedString, "typeTo");
		typeTo = (strTypeTo.isEmpty()) ? null : SiteType.valueOf(strTypeTo);

		final String strTo = Action.extractData(detailedString, "to");
		to = Integer.parseInt(strTo);
		
		final String strLevelTo = Action.extractData(detailedString, "levelTo");
		levelTo = (strLevelTo.isEmpty()) ? Constants.UNDEFINED : Integer.parseInt(strLevelTo);

		final String strState = Action.extractData(detailedString, "state");
		state = (strState.isEmpty()) ? Constants.UNDEFINED : Integer.parseInt(strState);

		final String strRotation = Action.extractData(detailedString, "rotation");
		rotation = (strRotation.isEmpty()) ? Constants.UNDEFINED : Integer.parseInt(strRotation);

		final String strValue = Action.extractData(detailedString, "value");
		value = (strValue.isEmpty()) ? Constants.UNDEFINED : Integer.parseInt(strValue);

		final String strStack = Action.extractData(detailedString, "stack");
		onStacking = (strStack.isEmpty()) ? false : Boolean.parseBoolean(strStack);

		final String strDecision = Action.extractData(detailedString, "decision");
		decision = (strDecision.isEmpty()) ? false : Boolean.parseBoolean(strDecision);
	}

	//-------------------------------------------------------------------------

	@Override
	public Action apply
	(
		final Context context,
		final boolean store
	)
	{
		final OnTrackIndices onTrackIndices = context.state().onTrackIndices();
		final int contIdA = typeFrom.equals(SiteType.Cell) ? context.containerId()[from] : 0;
		final int contIdB = typeTo.equals(SiteType.Cell) ? context.containerId()[to] : 0;
		
		final boolean requiresStack = context.game().isStacking();
		
		if (!requiresStack)
		{
			// System.out.println("loc is " + loc);
			final ContainerState csA = context.state().containerStates()[contIdA];
			final ContainerState csB = context.state().containerStates()[contIdB];

			// If the origin is empty we do not apply this action.
			if (csA.what(from, typeFrom) == 0 && csA.count(from, typeFrom) == 0)
				return this;

			final int what = csA.what(from, typeFrom);
			final int count = csA.count(from, typeFrom);
			int currentStateA = Constants.UNDEFINED;
			int currentRotationA = Constants.UNDEFINED;
			int currentValueA = Constants.UNDEFINED;
			Component piece = null;

			// take the local state of the site A
			currentStateA = (csA.what(from, typeFrom) == 0) ? Constants.UNDEFINED : csA.state(from, typeFrom);
			currentRotationA = csA.rotation(from, typeFrom);
			currentValueA = csA.value(from, typeFrom);

			if (count == 1)
			{
				csA.remove(context.state(), from, typeFrom);

				// to keep the site of the item in cache for each player
				if (what != 0)
				{
					piece = context.components()[what];
					final int owner = piece.owner();
					context.state().owned().remove(owner, what, from, typeFrom);
				}

				// In case of LargePiece we update the empty chunkSet
				if (piece != null && piece.isLargePiece())
				{
					final Component largePiece = piece;
					final TIntArrayList locs = largePiece.locs(context, from, currentStateA, context.topology());
					for (int i = 0; i < locs.size(); i++)
					{
						csA.addToEmpty(locs.getQuick(i), SiteType.Cell);
						csA.setCount(context.state(), locs.getQuick(i), 0);
					}
					if (largePiece.isDomino() && context.containerId()[from] == 0)
					{
						for (int i = 0; i < 4; i++)
							csA.setValueCell(context.state(), locs.getQuick(i), largePiece.getValue());

						for (int i = 4; i < 8; i++)
							csA.setValueCell(context.state(), locs.getQuick(i), largePiece.getValue2());
					}
				}
			}
			else
			{
				csA.setSite(context.state(), from, Constants.UNDEFINED, Constants.UNDEFINED, count - 1, Constants.UNDEFINED,
						Constants.UNDEFINED, (context.game().usesLineOfPlay() ? 1 : Constants.OFF), typeFrom);
			}

			// update the local state of the site B
			if (currentStateA != Constants.UNDEFINED && state == Constants.UNDEFINED)
				csB.setSite(context.state(), to, Constants.UNDEFINED, Constants.UNDEFINED, Constants.UNDEFINED, currentStateA,
						Constants.UNDEFINED, Constants.OFF, typeTo);
			else if (state != Constants.UNDEFINED)
				csB.setSite(context.state(), to, Constants.UNDEFINED, Constants.UNDEFINED, Constants.UNDEFINED, state,
						Constants.UNDEFINED, Constants.OFF, typeTo);

			// update the rotation state of the site B
			if (currentRotationA != Constants.UNDEFINED && rotation == Constants.UNDEFINED)
				csB.setSite(context.state(), to, Constants.UNDEFINED, Constants.UNDEFINED, Constants.UNDEFINED,
						Constants.UNDEFINED, currentRotationA, Constants.OFF, typeTo);
			else if (rotation != Constants.UNDEFINED)
				csB.setSite(context.state(), to, Constants.UNDEFINED, Constants.UNDEFINED, Constants.UNDEFINED,
						Constants.UNDEFINED, rotation, Constants.OFF, typeTo);

			// update the piece value of the site B
			if (currentValueA != Constants.UNDEFINED && value == Constants.UNDEFINED)
				csB.setSite(context.state(), to, Constants.UNDEFINED, Constants.UNDEFINED, Constants.UNDEFINED,
						Constants.UNDEFINED, Constants.UNDEFINED, (context.game().usesLineOfPlay() ? 1 : currentValueA),
						typeTo);
			else if (value != Constants.UNDEFINED)
				csB.setSite(context.state(), to, Constants.UNDEFINED, Constants.UNDEFINED, Constants.UNDEFINED,
						Constants.UNDEFINED, Constants.UNDEFINED, (context.game().usesLineOfPlay() ? 1 : value),
						typeTo);
			
			final int who = (what < 1) ? 0 : context.components()[what].owner();

			if (csB.what(to, typeTo) != 0 && (!context.game().requiresCount()
					|| context.game().requiresCount() && csB.what(to, typeTo) != what))
			{
				final Component pieceToRemove = context.components()[csB.what(to, typeTo)];
				final int owner = pieceToRemove.owner();
				context.state().owned().remove(owner, csB.what(to, typeTo), to, typeTo);
			}

			if (csB.what(to, typeTo) == what && csB.count(to, typeTo) > 0)
			{
				csB.setSite(context.state(), to, Constants.UNDEFINED, Constants.UNDEFINED,
						(context.game().requiresCount() ? csB.count(to, typeTo) + 1 : 1),
						Constants.UNDEFINED, Constants.UNDEFINED,
						(context.game().usesLineOfPlay() ? 1 : Constants.OFF), typeTo);
			}
			else
			{
				csB.setSite(context.state(), to, who, what, 1, Constants.UNDEFINED, Constants.UNDEFINED,
						(context.game().usesLineOfPlay() ? 1 : Constants.OFF), typeTo);
			}

			// to keep the site of the item in cache for each player
			if (what != 0 && csB.count(to, typeTo) == 1)
			{
				piece = context.components()[what];
				final int owner = piece.owner();
				context.state().owned().add(owner, what, to, typeTo);
			}

			// In case of LargePiece we update the empty chunkSet
			if (piece != null && piece.isLargePiece())
			{
				final Component largePiece = piece;
				final TIntArrayList locs = largePiece.locs(context, to, state, context.topology());
				for (int i = 0; i < locs.size(); i++)
				{
					csB.removeFromEmpty(locs.getQuick(i), SiteType.Cell);
					csB.setCount(context.state(), locs.getQuick(i),
							(context.game().usesLineOfPlay() ? piece.index() : 1));
				}

				if (context.game().usesLineOfPlay() && context.containerId()[to] == 0)
				{
					for (int i = 0; i < 4; i++)
					{
						csB.setValueCell(context.state(), locs.getQuick(i), largePiece.getValue());
					}

					for (int i = 4; i < 8; i++)
					{
						csB.setValueCell(context.state(), locs.getQuick(i), largePiece.getValue2());
					}

					// We update the line of play for dominoes
					for (int i = 0; i < context.containers()[0].numSites(); i++)
						csB.setPlayable(context.state(), i, false);

					for (int i = 0; i < context.containers()[0].numSites(); i++)
					{
						if (csB.what(i, typeTo) != 0)
						{
							final Component currentComponent = context.components()[csB.what(i, typeTo)];
							final int currentState = csB.state(i, typeTo);
							final TIntArrayList locsToUpdate = largePiece.locs(context, i, currentState,
									context.topology());

							lineOfPlayDominoes(context, locsToUpdate.getQuick(0), locsToUpdate.getQuick(1),
									getDirnDomino(0, currentState), false, true);
							lineOfPlayDominoes(context, locsToUpdate.getQuick(7), locsToUpdate.getQuick(6),
									getDirnDomino(2, currentState), false, false);

							if (currentComponent.isDoubleDomino())
							{
								lineOfPlayDominoes(context, locsToUpdate.getQuick(2), locsToUpdate.getQuick(5),
										getDirnDomino(1, currentState), true, true);
								lineOfPlayDominoes(context, locsToUpdate.getQuick(3), locsToUpdate.getQuick(4),
										getDirnDomino(3, currentState), true, true);
							}
						}
					}
				}
			}

			// We update the structure about track indices if the game uses track.
			updateOnTrackIndices(what, onTrackIndices, context.board().tracks());

//			if (context.state().onTrackIndices() != null)
//			{
//				System.out.println("TRACK UPDATED by ActionMove on non stacking game");
//				System.out.println(context.state().onTrackIndices());
//			}

			// We keep the update for hidden info.
			if (context.game().hiddenInformation())
			{
				for (int pid = 1; pid < context.players().size(); pid++)
				{
					csB.setHidden(context.state(), pid, to, 0, typeTo, csA.isHidden(pid, from, 0, typeFrom));
					csB.setHiddenWhat(context.state(), pid, to, 0, typeTo, csA.isHiddenWhat(pid, from, 0, typeFrom));
					csB.setHiddenWho(context.state(), pid, to, 0, typeTo, csA.isHiddenWho(pid, from, 0, typeFrom));
					csB.setHiddenCount(context.state(), pid, to, 0, typeTo, csA.isHiddenCount(pid, from, 0, typeFrom));
					csB.setHiddenRotation(context.state(), pid, to, 0, typeTo, csA.isHiddenRotation(pid, from, 0, typeFrom));
					csB.setHiddenState(context.state(), pid, to, 0, typeTo, csA.isHiddenState(pid, from, 0, typeFrom));
					csB.setHiddenValue(context.state(), pid, to, 0, typeTo, csA.isHiddenValue(pid, from, 0, typeFrom));
					if (csA.what(from, typeFrom) == 0)
					{
						csA.setHidden(context.state(), pid, from, 0, typeFrom, false);
						csA.setHiddenWhat(context.state(), pid, from, 0, typeFrom, false);
						csA.setHiddenWho(context.state(), pid, from, 0, typeFrom, false);
						csA.setHiddenCount(context.state(), pid, from, 0, typeFrom, false);
						csA.setHiddenRotation(context.state(), pid, from, 0, typeFrom, false);
						csA.setHiddenValue(context.state(), pid, from, 0, typeFrom, false);
						csA.setHiddenState(context.state(), pid, from, 0, typeFrom, false);
					}
				}
			}

			if (csB.isEmpty(to, typeTo))
			{
				throw new RuntimeException("Did not expect locationB to be empty at site locnB="+to+"(who, what,count,state)=("
						+ csB.who(to, typeTo) + "," + csB.what(to, typeTo) + "," + csB.count(to, typeTo)
								+ ","
								+ csB.state(to, typeTo) + "," + csB.state(to, typeTo) + ")");
			}
		}
		// on a stacking game
		else
		{
			if(from == to)
				return this;
			
			final ContainerState containerA = context.state().containerStates()[contIdA];
			final ContainerState containerB = context.state().containerStates()[contIdB];

			// To move a complete stack
			if(onStacking)
			{
				final int sizeStackA = containerA.sizeStack(from, typeFrom);
				for (int slevel = 0; slevel < containerA.sizeStack(from, typeFrom); slevel++)
				{
					if (levelTo == Constants.UNDEFINED)
						containerB.addItemGeneric(context.state(), to, containerA.what(from, slevel, typeFrom),
								containerA.who(from, slevel, typeFrom), containerA.state(from, slevel, typeFrom),
								containerA.rotation(from, slevel, typeFrom), containerA.value(from, slevel, typeFrom),
								context.game(), typeTo);
					else
					{
						containerB.insert(context.state(), typeTo, to, levelTo, containerA.what(from, slevel, typeFrom),
								containerA.who(from, slevel, typeFrom), state, rotation, value,
								context.game());
					}
				}

				// we update owned for locnA.
				for (int level = 0; level < containerA.sizeStack(from, typeFrom); level++)
				{
					final int whatA = containerA.what(from, level, typeFrom);
					if (whatA != 0)
					{
						final Component pieceA = context.components()[whatA];
						final int ownerA = pieceA.owner();
						if (ownerA != 0)
							context.state().owned().remove(ownerA, whatA, from, typeFrom);
					}
				}
				
				containerA.removeStackGeneric(context.state(), from, typeFrom);
				containerA.addToEmpty(from, typeFrom);
				containerB.removeFromEmpty(to, typeTo);

				// we update owned for locnB.
				for (int level = containerB.sizeStack(to, typeTo) - sizeStackA; level < containerB
						.sizeStack(to, typeTo); level++)
				{
					if (level < 0)
						continue;
					final int whatB = containerB.what(to, level, typeTo);
					if (whatB != 0)
					{
						final Component pieceB = context.components()[whatB];
						final int ownerB = pieceB.owner();
						if (ownerB != 0)
							context.state().owned().add(ownerB, whatB, to, level, typeTo);
					}
				}
			}
			// to move only the top piece
			else if (levelFrom == Constants.UNDEFINED)
			{
				final int what = containerA.what(from, typeFrom);

				containerA.remove(context.state(), from, typeFrom);
				
				if (containerA.sizeStack(from, typeFrom) == 0)
					containerA.addToEmpty(from, typeFrom);

				final int who = (what < 1) ? 0 : context.components()[what].owner();

				// ERIC COMMENTED
				if (context.game().hasCard())
				{
					// ERIC COMMENTED
//					final boolean[] masked = new boolean[context.players().size() - 1];
//					for (int pid = 1; pid < context.players().size(); pid++)
//						masked[pid - 1] = containerA.isMasked(from, pid, typeFrom);
//					if (levelTo == Constants.UNDEFINED)
//						containerB.addItemGeneric(context.state(), to, what, who, context.game(), masked, true,
//								typeTo);
//					else // ADD THE HIDDEN field FOR INSERTING A COMPONENT IN A STACK.
//						containerB.insert(context.state(), to, levelTo, what, who, context.game());
				}
				else
				{
					if (levelTo == Constants.UNDEFINED)
						containerB.addItemGeneric(context.state(), to, what, who, context.game(), typeTo);
					else // ADD THE HIDDEN field FOR INSERTING A COMPONENT IN A STACK.
						containerB.insertCell(context.state(), to, levelTo, what, who, state, rotation, value,
								context.game());
				}

				if (containerB.sizeStack(to, typeTo) != 0)
					containerB.removeFromEmpty(to, typeTo);

				// to keep the site of the item in cache for each player
				Component pieceA = null;
				int ownerA = 0;
				if (what != 0)
				{
					pieceA = context.components()[what];
					ownerA = pieceA.owner();
					context.state().owned().add(ownerA, what, to,
							containerB.sizeStack(to, typeTo) - 1, typeTo);
					context.state().owned().remove(ownerA, what, from,
							containerA.sizeStack(from, typeFrom), typeFrom);
				}

				// We update the structure about track indices if the game uses track.
				updateOnTrackIndices(what, onTrackIndices, context.board().tracks());

//				if (context.state().onTrackIndices() != null)
//				{
//					System.out.println("TRACK UPDATED by ActionMove on stack top piece");
//					System.out.println(context.state().onTrackIndices());
//				}

			} // to move only a level of the stack.
			else
			{
				final int what = containerA.what(from, levelFrom, typeFrom);
				final int newStateB = (state == Constants.UNDEFINED) ? containerA.state(from, levelFrom, typeFrom) : state;
				final int newRotationB = (rotation == Constants.UNDEFINED) ? containerA.rotation(from, levelFrom, typeFrom) : rotation;
				final int newValueB = (value == Constants.UNDEFINED) ? containerA.value(from, levelFrom, typeFrom)
						: value;
				
				containerA.remove(context.state(), from, levelFrom, typeFrom);

				if (containerA.sizeStack(from, typeFrom) == 0)
					containerA.addToEmpty(from, typeFrom);

				final int who = (what < 1) ? 0 : context.components()[what].owner();

				if (context.game().hasCard())
				{
					// ERIC COMMENTED
//					final boolean[] masked = new boolean[context.players().size() - 1];
//					for (int pid = 1; pid < context.players().size(); pid++)
//						masked[pid - 1] = containerA.isMasked(from, levelFrom, pid, typeFrom);
//					containerB.addItemGeneric(context.state(), to, what, who, context.game(), masked, true, typeTo);
//
//					if (containerB.sizeStack(to, typeTo) != 0)
//						containerB.removeFromEmpty(to, typeTo);
//
//					// to keep the site of the item in cache for each player
//					Component pieceA = null;
//					int ownerA = 0;
//					if (what != 0)
//					{
//						pieceA = context.components()[what];
//						ownerA = pieceA.owner();
//						if (ownerA != 0)
//						{
//							context.state().owned().add(ownerA, what, to,
//									containerB.sizeStack(to, typeTo) - 1, typeTo);
//							context.state().owned().remove(ownerA, what, from, levelFrom);
//						}
//					}
				}
				else if (levelTo == Constants.UNDEFINED)
				{
					containerB.addItemGeneric(context.state(), to, what, who, newStateB, newRotationB, newValueB,
							context.game(), typeTo);

					if (containerB.sizeStack(to, typeTo) != 0)
						containerB.removeFromEmpty(to, typeTo);

					// to keep the site of the item in cache for each player
					Component pieceA = null;
					int ownerA = 0;
					if (what != 0)
					{
						pieceA = context.components()[what];
						ownerA = pieceA.owner();
							context.state().owned().add(ownerA, what, to,
									containerB.sizeStack(to, typeTo) - 1, typeTo);
							context.state().owned().remove(ownerA, what, from, levelFrom, typeFrom);
					}
				}
				else
				{
					final int sizeStack = containerB.sizeStack(to, typeTo);
					
					// we update the own list of the pieces on the top of that piece inserted.
					for (int i = sizeStack - 1; i >= levelTo; i--)
					{
						final int owner = containerB.who(to, i, typeTo);
						final int piece = containerB.what(to, i, typeTo);
						context.state().owned().remove(owner, piece, to, i, typeTo);
						context.state().owned().add(owner, piece, to, i + 1, typeTo);
					}
					
					// We insert the piece.
					containerB.insertCell(context.state(), to, levelTo, what, who, state, rotation, value, context.game());
				
					// we update the own list with the new piece
					final Component piece = context.components()[what];
					final int owner = piece.owner();
					context.state().owned().add(owner, what, to, levelTo, typeTo);

					if (containerB.sizeStack(to, typeTo) != 0)
						containerB.removeFromEmpty(to, typeTo);

					// to keep the site of the item in cache for each player
					Component pieceA = null;
					int ownerA = 0;
					if (what != 0)
					{
						pieceA = context.components()[what];
						ownerA = pieceA.owner();
						if (ownerA != 0)
							context.state().owned().remove(ownerA, what, from, levelFrom, typeFrom);
					}
				}

				// We update the structure about track indices if the game uses track.
				updateOnTrackIndices(what, onTrackIndices, context.board().tracks());
			}
		}
		
		return this;
	}

	/**
	 * To update the onTrackIndices after a move.
	 * 
	 * @param what           The index of the component moved.
	 * @param onTrackIndices The structure onTrackIndices
	 * @param tracks         The list of the tracks.
	 */
	public void updateOnTrackIndices(final int what, final OnTrackIndices onTrackIndices, final List<Track> tracks)
	{
		// We update the structure about track indices if the game uses track.
		if (what != 0 && onTrackIndices != null)
		{
			for (final Track track : tracks)
			{
				final int trackIdx = track.trackIdx();
				final TIntArrayList indicesLocA = onTrackIndices.locToIndex(trackIdx, from);

				for (int k = 0; k < indicesLocA.size(); k++)
				{
					final int indexA = indicesLocA.getQuick(k);
					final int countAtIndex = onTrackIndices.whats(trackIdx, what, indicesLocA.getQuick(k));

					if (countAtIndex > 0)
					{
						onTrackIndices.remove(trackIdx, what, 1, indexA);
						final TIntArrayList newWhatIndice = onTrackIndices.locToIndexFrom(trackIdx, to, indexA);

						if (newWhatIndice.size() > 0)
						{
							onTrackIndices.add(trackIdx, what, 1, newWhatIndice.getQuick(0));
						}
						else
						{
							final TIntArrayList newWhatIndiceIfNotAfter = onTrackIndices.locToIndex(trackIdx, to);
							if (newWhatIndiceIfNotAfter.size() > 0)
								onTrackIndices.add(trackIdx, what, 1, newWhatIndiceIfNotAfter.getQuick(0));
						}

						break;
					}
				}

				// If the piece was not in the track but enter on it, we update the structure
				// corresponding to that track.
				if (indicesLocA.size() == 0)
				{
					final TIntArrayList indicesLocB = onTrackIndices.locToIndex(trackIdx, to);
					if (indicesLocB.size() != 0)
						onTrackIndices.add(trackIdx, what, 1, indicesLocB.get(0));
				}
			}
		}
	}

	//-------------------------------------------------------------------------
	@Override
	public String toTrialFormat(final Context context)
	{
		final StringBuilder sb = new StringBuilder();

		sb.append("[Move:");

		if (typeFrom != null || (context != null && typeFrom != context.board().defaultSite()))
		{
			sb.append("typeFrom=" + typeFrom);
			sb.append(",from=" + from);
		}
		else
			sb.append("from=" + from);

		if (levelFrom != Constants.UNDEFINED)
			sb.append(",levelFrom=" + levelFrom);

		if (typeTo != null || (context != null && typeTo != context.board().defaultSite()))
			sb.append(",typeTo=" + typeTo);

		sb.append(",to=" + to);

		if (levelTo != Constants.UNDEFINED)
			sb.append(",levelTo=" + levelTo);

		if (state != Constants.UNDEFINED)
			sb.append(",state=" + state);

		if (rotation != Constants.UNDEFINED)
			sb.append(",rotation=" + rotation);

		if (value != Constants.UNDEFINED)
			sb.append(",value=" + value);

		if (onStacking)
			sb.append(",stack=" + onStacking);

		if (decision)
			sb.append(",decision=" + decision);

		sb.append(']');

		return sb.toString();
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + (decision ? 1231 : 1237);
		result = prime * result + from;
		result = prime * result + levelFrom;
		result = prime * result + to;
		result = prime * result + levelTo;
		result = prime * result + state;
		result = prime * result + rotation;
		result = prime * result + value;
		result = prime * result + (onStacking ? 1231 : 1237);
		result = prime * result + ((typeFrom == null) ? 0 : typeFrom.hashCode());
		result = prime * result + ((typeTo == null) ? 0 : typeTo.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj)
	{
		if (this == obj)
			return true;

		if (!(obj instanceof ActionMove))
			return false;

		final ActionMove other = (ActionMove) obj;

		return (decision == other.decision &&
				from == other.from &&
				levelFrom == other.levelFrom &&
				to == other.to &&
				levelTo == other.levelTo &&
				state == other.state &&
				rotation == other.rotation &&
				value == other.value &&
				onStacking == other.onStacking && typeFrom == other.typeFrom && typeTo == other.typeTo);
	}

	//-------------------------------------------------------------------------
	
	@Override
	public String getDescription() 
	{
		return "Move";
	}

	@Override
	public String toTurnFormat(final Context context, final boolean useCoords)
	{
		final StringBuilder sb = new StringBuilder();

		String newFrom = from + "";
		if (useCoords)
		{
			final int cid = (typeFrom == SiteType.Cell
					|| typeFrom == null && context.board().defaultSite() == SiteType.Cell) ? context.containerId()[from]
							: 0;
			if (cid == 0)
			{
				final SiteType realType = (typeFrom != null) ? typeFrom : context.board().defaultSite();
				newFrom = context.game().equipment().containers()[cid].topology().getGraphElements(realType).get(from)
						.label();
			}
		}

		if (typeFrom != null && !typeFrom.equals(context.board().defaultSite()))
			sb.append(typeFrom + " " + newFrom);
		else
			sb.append(newFrom);

		if (levelFrom != Constants.UNDEFINED && context.game().isStacking())
			sb.append("/" + levelFrom);

		String newTo = to + "";
		if (useCoords)
		{
			final int cid = (typeTo == SiteType.Cell
					|| typeTo == null && context.board().defaultSite() == SiteType.Cell) ? context.containerId()[to]
							: 0;
			if (cid == 0)
			{
				final SiteType realType = (typeTo != null) ? typeTo : context.board().defaultSite();
				newTo = context.game().equipment().containers()[cid].topology().getGraphElements(realType).get(to)
						.label();
			}
		}

		if (typeTo != null && !typeTo.equals(context.board().defaultSite()))
			sb.append("-" + typeTo + " " + newTo);
		else
			sb.append("-" + newTo);

		if (levelTo != Constants.UNDEFINED)
			sb.append("/" + levelTo);

		if (state != Constants.UNDEFINED)
			sb.append("=" + state);

		if (rotation != Constants.UNDEFINED)
			sb.append(" r" + rotation);

		if (value != Constants.UNDEFINED)
			sb.append(" v" + value);

		if (onStacking)
			sb.append(" ^");

		return sb.toString();
	}

	@Override
	public String toMoveFormat(final Context context, final boolean useCoords)
	{
		final StringBuilder sb = new StringBuilder();

		sb.append("(Move ");

		String newFrom = from + "";
		if (useCoords)
		{
			final int cid = (typeFrom == SiteType.Cell
					|| typeFrom == null && context.board().defaultSite() == SiteType.Cell) ? context.containerId()[from]
							: 0;
			if (cid == 0)
			{
				final SiteType realType = (typeFrom != null) ? typeFrom : context.board().defaultSite();
				newFrom = context.game().equipment().containers()[cid].topology().getGraphElements(realType).get(from)
						.label();
			}
		}

		if (typeFrom != null && typeTo != null
				&& (!typeFrom.equals(context.board().defaultSite()) || !typeFrom.equals(typeTo)))
			sb.append(typeFrom + " " + newFrom);
		else
			sb.append(newFrom);

		if (levelFrom != Constants.UNDEFINED)
			sb.append("/" + levelFrom);

		String newTo = to + "";
		if (useCoords)
		{
			final int cid = (typeTo == SiteType.Cell
					|| typeTo == null && context.board().defaultSite() == SiteType.Cell) ? context.containerId()[to]
							: 0;
			if (cid == 0)
			{
				final SiteType realType = (typeTo != null) ? typeTo : context.board().defaultSite();
				if (to < context.game().equipment().containers()[cid].topology().getGraphElements(realType).size())
					newTo = context.game().equipment().containers()[cid].topology().getGraphElements(realType).get(to)
							.label();
				else // The site is not existing.
					newTo = "??";
			}
		}

		if (typeFrom != null && typeTo != null
				&& (!typeTo.equals(context.board().defaultSite()) || !typeFrom.equals(typeTo)))
			sb.append(" - " + typeTo + " " + newTo);
		else
			sb.append("-" + newTo);

		if (levelTo != Constants.UNDEFINED)
			sb.append("/" + levelTo);

		if (state != Constants.UNDEFINED)
			sb.append(" state=" + state);

		if (rotation != Constants.UNDEFINED)
			sb.append(" rotation=" + rotation);

		if (state != Constants.UNDEFINED)
			sb.append(" state=" + state);

		if (onStacking)
			sb.append(" stack=" + onStacking);

		sb.append(')');

		return sb.toString();
	}
		
	//-------------------------------------------------------------------------

	@Override
	public SiteType fromType()
	{
		return typeFrom;
	}

	@Override
	public SiteType toType()
	{
		return typeTo;
	}

	@Override
	public int from()
	{
		return from;
	}

	@Override
	public int to()
	{
		return to;
	}

	@Override
	public int levelFrom()
	{
		return (levelFrom == Constants.UNDEFINED) ? Constants.GROUND_LEVEL : levelFrom;
	}

	@Override
	public int levelTo()
	{
		return (levelTo == Constants.UNDEFINED) ? Constants.GROUND_LEVEL : levelTo;
	}

	@Override
	public int state()
	{
		return state;
	}

	@Override
	public int rotation()
	{
		return rotation;
	}

	@Override
	public int value()
	{
		return value;
	}

	@Override
	public int count()
	{
		return 1;
	}

	@Override
	public boolean isStacking()
	{
		return onStacking;
	}

	@Override
	public void setLevelFrom(final int levelA)
	{
		this.levelFrom = levelA;
	}
	
	@Override
	public ActionType actionType()
	{
		return ActionType.Move;
	}

	// -------------------------------------------------------------------------

	@Override
	public BitSet concepts(final Context context, final Moves movesLudeme)
	{
		final BitSet ludemeConcept = (movesLudeme != null) ? movesLudeme.concepts(context.game()) : new BitSet();

		final int contIdA = typeFrom.equals(SiteType.Cell) ? context.containerId()[from] : 0;
		final int contIdB = typeTo.equals(SiteType.Cell) ? context.containerId()[to] : 0;

		final ContainerState csA = context.state().containerStates()[contIdA];
		final ContainerState csB = context.state().containerStates()[contIdB];

		final int whatA = csA.what(from, typeFrom);
		final int whatB = csB.what(to, typeTo);

		final int whoA = csA.who(from, typeFrom);
		final int whoB = csB.who(to, typeTo);

		final BitSet concepts = new BitSet();

		// ---- Hop concepts

		if (ludemeConcept.get(Concept.HopDecision.id()))
			{
			concepts.set(Concept.HopDecision.id(), true);

			if (whatA != 0)
			{
				final Topology topology = context.topology();
				final TopologyElement fromV = topology.getGraphElements(typeFrom).get(from);

				final List<DirectionFacing> directionsSupported = topology.supportedDirections(RelationType.All,
						typeFrom);
				AbsoluteDirection direction = null;
				int distance = Constants.UNDEFINED;

				for (final DirectionFacing facingDirection : directionsSupported)
				{
					final AbsoluteDirection absDirection = facingDirection.toAbsolute();
					final List<Radial> radials = topology.trajectories().radials(typeFrom, fromV.index(), absDirection);

					for (final Radial radial : radials)
					{
						for (int toIdx = 1; toIdx < radial.steps().length; toIdx++)
						{
							final int toRadial = radial.steps()[toIdx].id();
							if (toRadial == to)
							{
								direction = absDirection;
								distance = toIdx;
								break;
							}
						}
						if (direction != null)
							break;
					}
				}

				if (direction != null)
				{
					final List<Radial> radials = topology.trajectories().radials(typeFrom, fromV.index(), direction);

					for (final Radial radial : radials)
					{
						for (int toIdx = 1; toIdx < distance; toIdx++)
						{
							final int between = radial.steps()[toIdx].id();
							final int whatBetween = csA.what(between, typeFrom);
							final int whoBetween = csA.who(between, typeFrom);
							if (whatBetween != 0)
							{
								if (areEnemies(context, whoA, whoBetween))
								{
									if (whatB == 0)
										concepts.set(Concept.HopDecisionEnemyToEmpty.id(), true);
									else
									{
										if (areEnemies(context, whoA, whoB))
											concepts.set(Concept.HopDecisionEnemyToEnemy.id(), true);
										else
											concepts.set(Concept.HopDecisionEnemyToFriend.id(), true);
									}
								}
								else
								{
									if (whatB == 0)
										concepts.set(Concept.HopDecisionFriendToEmpty.id(), true);
									else
									{
										if (areEnemies(context, whoA, whoB))
											concepts.set(Concept.HopDecisionFriendToEnemy.id(), true);
										else
											concepts.set(Concept.HopDecisionFriendToFriend.id(), true);
									}
								}
							}
						}
					}
				}
			}
		}

		if (ludemeConcept.get(Concept.HopEffect.id()))
			concepts.set(Concept.HopEffect.id(), true);

		// ---- Step concepts

		if (ludemeConcept.get(Concept.StepEffect.id()))
			concepts.set(Concept.StepEffect.id(), true);

		if (ludemeConcept.get(Concept.StepDecision.id()))
		{
			concepts.set(Concept.StepDecision.id(), true);
			if (whatA != 0)
			{
				if (whatB == 0)
					concepts.set(Concept.StepDecisionToEmpty.id(), true);
				else
				{
					if (areEnemies(context, whoA, whoB))
						concepts.set(Concept.StepDecisionToEnemy.id(), true);
					else
						concepts.set(Concept.StepDecisionToFriend.id(), true);
				}
			}
		}

		// ---- Leap concepts

		if (ludemeConcept.get(Concept.LeapEffect.id()))
			concepts.set(Concept.LeapEffect.id(), true);

		if (ludemeConcept.get(Concept.LeapDecision.id()))
		{
			concepts.set(Concept.LeapDecision.id(), true);
			if (whatA != 0)
			{
				if (whatB == 0)
					concepts.set(Concept.LeapDecisionToEmpty.id(), true);
				else
				{
					if (areEnemies(context, whoA, whoB))
						concepts.set(Concept.LeapDecisionToEnemy.id(), true);
					else
						concepts.set(Concept.LeapDecisionToFriend.id(), true);
				}
			}
		}

		// ---- Slide concepts

		if (ludemeConcept.get(Concept.SlideEffect.id()))
			concepts.set(Concept.SlideEffect.id(), true);

		if (ludemeConcept.get(Concept.SlideDecision.id()))
		{
			concepts.set(Concept.SlideDecision.id(), true);
			if (whatA != 0)
			{
				if (whatB == 0)
					concepts.set(Concept.SlideDecisionToEmpty.id(), true);
				else
				{
					if (areEnemies(context, whoA, whoB))
						concepts.set(Concept.SlideDecisionToEnemy.id(), true);
					else
						concepts.set(Concept.SlideDecisionToFriend.id(), true);
				}
			}
		}

		// ---- FromTo concepts

		if (ludemeConcept.get(Concept.FromToDecision.id()))
		{
			if (contIdA == contIdB)
				concepts.set(Concept.FromToDecisionWithinBoard.id(), true);
			else
				concepts.set(Concept.FromToDecisionBetweenContainers.id(), true);

			if (whatA != 0)
			{
				if (whatB == 0)
					concepts.set(Concept.FromToDecisionEmpty.id(), true);
				else
				{
					if (areEnemies(context, whoA, whoB))
						concepts.set(Concept.FromToDecisionEnemy.id(), true);
					else
						concepts.set(Concept.FromToDecisionFriend.id(), true);
				}
			}
		}

		if (ludemeConcept.get(Concept.FromToEffect.id()))
			concepts.set(Concept.FromToEffect.id(), true);
		
		// ---- Swap Pieces concepts

		if (ludemeConcept.get(Concept.SwapPiecesEffect.id()))
			concepts.set(Concept.SwapPiecesEffect.id(), true);

		if (ludemeConcept.get(Concept.SwapPiecesDecision.id()))
			concepts.set(Concept.SwapPiecesDecision.id(), true);


		// ---- Sow capture concepts

		if (ludemeConcept.get(Concept.SowCapture.id()))
			concepts.set(Concept.SowCapture.id(), true);

		return concepts;
	}

	/**
	 * @param context The context.
	 * @param who1    The id of a player.
	 * @param who2    The id of a player.
	 * @return True if these players are enemies.
	 */
	public static boolean areEnemies(final Context context, final int who1, final int who2)
	{
		if (who1 == 0 || who2 == 0 || who1 == who2)
			return false;

		if (context.game().requiresTeams())
		{
			final TIntArrayList teamMembers = new TIntArrayList();
			final int tid = context.state().getTeam(who1);
			for (int i = 1; i < context.game().players().size(); i++)
				if (context.state().getTeam(i) == tid)
					teamMembers.add(i);
			return !teamMembers.contains(who2);
		}

		return who1 != who2;
	}
}
