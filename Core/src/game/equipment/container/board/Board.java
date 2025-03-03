package game.equipment.container.board;

import java.util.BitSet;

import annotations.Name;
import annotations.Opt;
import annotations.Or;
import annotations.Or2;
import game.Game;
import game.equipment.container.Container;
import game.functions.graph.GraphFunction;
import game.functions.ints.IntConstant;
import game.functions.range.Range;
import game.types.board.BasisType;
import game.types.board.ShapeType;
import game.types.board.SiteType;
import game.types.play.RoleType;
import game.util.equipment.Values;
import game.util.graph.Face;
import game.util.graph.Graph;
import main.Constants;
import main.math.Vector;
import metadata.graphics.util.ContainerStyleType;
import other.BaseLudeme;
import other.concept.Concept;
import other.topology.Cell;
import other.topology.Edge;
import other.topology.Topology;
import other.topology.Vertex;

/**
 * Defines a board by its graph, consisting of vertex locations and edge pairs.
 * 
 * @author Eric.Piette and cambolbro
 * 
 * @remarks The values range are used for deduction puzzles. The state model for
 *          these puzzles is a Constraint Satisfaction Problem (CSP) model,
 *          possibly with a variable for each graph element (i.e. vertex, edge
 *          and cell), each with a range of possible values.
 */
public class Board extends Container
{
	private static final long serialVersionUID = 1L;

	//-------------------------------------------------------------------------

	/** The graph generated by the graph function. */
	protected Graph graph = null;

	/** The graph function. */
	private final GraphFunction graphFunction;
	
	/** The domain of the edge variables. */
	private Range edgeRange;

	/** The domain of the cell variables. */
	private Range cellRange;

	/** The domain of the vertex variables. */
	private Range vertexRange;
	
	/** The game can involves large stack. */
	private final boolean largeStack;
	
	//-------------------------------------------------------------------------
	
	/**
	 * @param graphFn     The graph function used to build the board.
	 * @param track       The track on the board.
	 * @param tracks      The tracks on the board.
	 * @param values      The range values of a graph element used for deduction puzzle.
	 * @param valuesArray The range values of many graph elements used for deduction puzzle.
	 * @param use         Graph element type to use by default [Cell].
	 * @param largeStack  The game can involves stack(s) higher than 32.
	 * 
	 * @example (board (graph vertices:{ {1 0} {2 0} {0 1} {1 1} {2 1} {3 1}
	 *          {0 2} {1 2} {2 2} {3 2} {1 3} {2 3}} edges:{ {0 2} {0 3} {3 2} {3 4}
	 *          {1 4} {4 5} {1 5} {3 7} {4 8} {6 7} {7 8} {8 9} {6 10} {11 9} {10 7}
	 *          {11 8}} ) )
	 */
	public Board
	(
			            final GraphFunction graphFn,
		@Opt @Or        final Track         track,
		@Opt @Or        final Track[]       tracks,
		@Opt @Or2       final Values        values,
		@Opt @Or2       final Values[]      valuesArray,
		@Opt     @Name  final SiteType      use,       
		@Opt     @Name 	final Boolean       largeStack
	)
	{
		super("Board", Constants.UNDEFINED, RoleType.Neutral);

		int numNonNull = 0;
		if (track != null)
			numNonNull++;
		if (tracks != null)
			numNonNull++;

		if (numNonNull > 1)
			throw new IllegalArgumentException("Board: Only one of `track' or `tracks' can be non-null.");

		int valuesNonNull = 0;
		if (values != null)
			valuesNonNull++;
		if (valuesArray != null)
			valuesNonNull++;

		if (valuesNonNull > 1)
			throw new IllegalArgumentException("Board(): Only one of `values' or `valuesArray' parameter can be non-null.");

		defaultSite = (use == null) ? SiteType.Cell : use;
		graphFunction = graphFn;

		if (valuesNonNull == 1) // If values are used that's a deduction puzzle.
		{
			final Values[] valuesLocal = (valuesArray != null) ? valuesArray : new Values[] {values};
			
			for (final Values valuesGraphElement : valuesLocal)
			{
				switch (valuesGraphElement.type())
				{
				case Cell:
					cellRange = valuesGraphElement.range();
					break;
				case Edge:
					edgeRange = valuesGraphElement.range();
					break;
				case Vertex:
					vertexRange = valuesGraphElement.range();
					break;
				}
			}

			if (vertexRange == null)
				vertexRange = new Range(new IntConstant(0), new IntConstant(0));

			if (edgeRange == null)
				edgeRange = new Range(new IntConstant(0), new IntConstant(0));

			if (cellRange == null)
				cellRange = new Range(new IntConstant(0), new IntConstant(0));
			
			style = ContainerStyleType.Puzzle;
		}
		else
		{
			if (tracks != null)
				for (final Track t : tracks)
					this.tracks.add(t);
			else if (track != null)
				this.tracks.add(track);

			if (defaultSite == SiteType.Vertex || defaultSite == SiteType.Edge)
				style = ContainerStyleType.Graph;
			else
				style = ContainerStyleType.Board;
		}

		this.largeStack = largeStack == null ? false : largeStack.booleanValue();
	}
	
	//-------------------------------------------------------------------------

	/**
	 * @return The graph of the game.
	 */
	public Graph graph()
	{
		return graph;
	}

	// -------------------------------------------------------------------------

	/**
	 * Built the graph and compute the trajectories.
	 * 
	 * @param siteType
	 * @param boardless
	 */
	public void init(final SiteType siteType, final boolean boardless)
	{
		graph = graphFunction.eval(null, siteType);

		//System.out.println("Graph basis and shape: " + graph.basis() + " " + graph.shape());
		
		graph.measure(boardless);
		graph.trajectories().create(graph);
		
		graph.setDim(graphFunction.dim());
		
		topology.setGraph(graph);
		
		//System.out.println("Graph basis and shape: " + graph.basis() + " " + graph.shape());
	}
	
	//-------------------------------------------------------------------------

	@Override
	public void createTopology(final int beginIndex, final int numEdges)
	{
		init(defaultSite, isBoardless());

		// Add the perimeter computed in the graph to the topology.
		topology.setPerimeter(graph.perimeters());

		// Set the trajectories.
		topology.setTrajectories(graph.trajectories());
		
		// Add the vertices to the topology
		for (int i = 0; i < graph.vertices().size(); i++)
		{	
			final game.util.graph.Vertex graphVertex = graph.vertices().get(i);
			final double x = graphVertex.pt().x();
			final double y = graphVertex.pt().y();
			final double z = graphVertex.pt().z();
			final Vertex vertex = new Vertex(i, x, y, z);

			vertex.setProperties(graphVertex.properties());
			vertex.setRow(graphVertex.situation().rcl().row());
			vertex.setColumn(graphVertex.situation().rcl().column());
			vertex.setLayer(graphVertex.situation().rcl().layer());
			vertex.setLabel(graphVertex.situation().label());

			topology.vertices().add(vertex);
		}
		
		// Link up pivots AFTER creating all vertices
		for (int i = 0; i < graph.vertices().size(); i++)
		{
			final game.util.graph.Vertex vertex = graph.vertices().get(i);
			if (vertex.pivot() != null)
				topology.vertices().get(i).setPivot(topology.vertices().get(vertex.pivot().id()));
		}
		
		// Add the edges to the topology
		for (int i = 0; i < graph.edges().size(); i++)
		{	
			final game.util.graph.Edge graphEdge = graph.edges().get(i);
			final Vertex vA = topology.vertices().get(graphEdge.vertexA().id());
			final Vertex vB = topology.vertices().get(graphEdge.vertexB().id());
			final Edge edge = new Edge(i, vA, vB);

//			if (graph.edges().get(i).curved())
//				edge.setCurved(true);
			
			edge.setProperties(graphEdge.properties());
			edge.setRow(graphEdge.situation().rcl().row());
			edge.setColumn(graphEdge.situation().rcl().column());
			edge.setLayer(graphEdge.situation().rcl().layer());
			edge.setLabel(graphEdge.situation().label());

			if (graphEdge.tangentA() != null)
				edge.setTangentA(new Vector(graphEdge.tangentA()));
			
			if (graphEdge.tangentB() != null)
				edge.setTangentB(new Vector(graphEdge.tangentB()));
			
			topology.edges().add(edge);
			vA.edges().add(edge);
			vB.edges().add(edge);
		}
		
		// Add the cells to the topology.
		for (final Face face : graph.faces())
		{
			final Cell cell = new Cell(face.id(), face.pt().x(), face.pt().y(), face.pt().z());
			
			cell.setProperties(face.properties());
			cell.setRow(face.situation().rcl().row());
			cell.setColumn(face.situation().rcl().column());
			cell.setLayer(face.situation().rcl().layer());
			cell.setLabel(face.situation().label());

			topology.cells().add(cell);

			// We add the vertices of the cells and vice versa.
			for (final game.util.graph.Vertex v : face.vertices())
			{
				final Vertex vertex = topology().vertices().get(v.id());
				cell.vertices().add(vertex);
				vertex.cells().add(cell);
			}

			// We add the edges of the cells and vice versa.
			for (final game.util.graph.Edge e : face.edges())
			{
				final Edge edge = topology().edges().get(e.id());
				cell.edges().add(edge);
				edge.cells().add(cell);
			}
		}
		
		numSites = topology.cells().size();

		// We compute the number of edges of each tiling if the graph uses a regular
		// tiling.
		topology.computeNumEdgeIfRegular();
	}
	
	//-------------------------------------------------------------------------
	
	@Override
	public String toEnglish(final Game game) 
	{
		String text = "";
		
		final ShapeType boardShape = topology().graph().shape();
		final BasisType boardBasis = topology().graph().basis();

		// Determine the dimensions of the board. 
		if(topology().graph().dim() != null) 
		{
			for(final int dim: topology().graph().dim()) 
				text += dim + "x";
			
			if (topology().graph().dim().length == 1)
				text += topology().graph().dim()[0];
			else
				text = text.substring(0, text.length()-1);
		} 
		
		if (boardShape != null || boardBasis != null)
		{
			if(boardShape != null)
				text += " " + boardShape.name().toLowerCase();
			
			text += " " + name().toLowerCase();
			
			if(boardBasis != null)
				text += " with " + boardBasis.name().toLowerCase() + " tiling";
		}
		else
		{
			text = ((BaseLudeme) graphFunction).toEnglish(game).toLowerCase() + " " + name().toLowerCase();
		}

		return text;
	}
	
	//----------------------------------
	
	/**
	 * Set the topology.
	 * Note: use by LargePieceStyle only.
	 * 
	 * @param topo
	 */
	public void setTopology(final Topology topo)
	{
		topology = topo;
	}

	/**
	 * @return The domain of the vertex variables.
	 */
	public Range vertexRange()
	{
		return vertexRange;
	}

	/**
	 * @return The domain of the edge variables.
	 */
	public Range edgeRange()
	{
		return edgeRange;
	}

	/**
	 * @return The domain of the face variables.
	 */
	public Range cellRange()
	{
		return cellRange;
	}

	/**
	 * @param type The siteType.
	 * @return The corresponding range of the siteType.
	 */
	public Range getRange(final SiteType type)
	{
		switch (type)
		{
		case Vertex:
			return vertexRange();
		case Cell:
			return cellRange();
		case Edge:
			return edgeRange();
		}
		return null;
	}

	// ----------------------------------

	@Override
	public BitSet concepts(final Game game)
	{
		final BitSet concepts = new BitSet();
		concepts.or(super.concepts(game));
		concepts.or(graphFunction.concepts(game));
		if (style.equals(ContainerStyleType.Graph))
			concepts.set(Concept.GraphStyle.id(), true);
		return concepts;
	}

	@Override
	public BitSet writesEvalContextRecursive()
	{
		final BitSet writeEvalContext = new BitSet();
		writeEvalContext.or(super.writesEvalContextRecursive());
		writeEvalContext.or(graphFunction.writesEvalContextRecursive());
		return writeEvalContext;
	}

	@Override
	public BitSet readsEvalContextRecursive()
	{
		final BitSet readEvalContext = new BitSet();
		readEvalContext.or(super.readsEvalContextRecursive());
		readEvalContext.or(graphFunction.readsEvalContextRecursive());
		return readEvalContext;
	}
	
	/**
	 * @return The game can involves stack higher than 32.
	 */
	public boolean largeStack()
	{
		return this.largeStack;
	}
}