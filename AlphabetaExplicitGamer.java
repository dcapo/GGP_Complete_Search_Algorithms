package player.gamer.statemachine.completesearch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import player.gamer.statemachine.StateMachineGamer;
import player.gamer.statemachine.event.MinimaxMoveSelectionEvent;
import player.gamer.statemachine.gui.MinimaxDetailPanel;
import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;
import util.statemachine.implementation.prover.ProverStateMachine;
import apps.player.detail.DetailPanel;

public final class AlphabetaExplicitGamer extends StateMachineGamer {
	
	private Map<MachineState, Map<Move, List<MachineState>>> gameTree;
	
	/**
	 * Does nothing for the metagame
	 */
	@Override
	public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		gameTree = new HashMap<MachineState, Map<Move, List<MachineState>>>();
		populateCache();
	}
	
	private void populateCache() throws MoveDefinitionException, TransitionDefinitionException {
		Queue<MachineState> frontier = new LinkedList<MachineState>();
		frontier.offer(getStateMachine().getInitialState());
		while (!frontier.isEmpty()) {
			MachineState state = frontier.poll();
			if (gameTree.containsKey(state) || getStateMachine().isTerminal(state)) {
				continue;
			}
			List<Move> legalMoves = getStateMachine().getLegalMoves(state, getRole());
			Map<Move, List<MachineState>> moveMap = new HashMap<Move, List<MachineState>>();
			for (Move move : legalMoves) {
				List<MachineState> neighbors = new ArrayList<MachineState>();
				List<List<Move>> legalJointMoves = getStateMachine().getLegalJointMoves(state, getRole(), move);
				for (List<Move> jointMove : legalJointMoves) {
					MachineState nextState = getStateMachine().getNextState(state, jointMove);
					neighbors.add(nextState);
					if (!gameTree.containsKey(nextState) && !getStateMachine().isTerminal(state)) {
						frontier.offer(nextState);
					}
				}
				moveMap.put(move, neighbors);
			}
			gameTree.put(state, moveMap);
		}
	}
	
	private int statesExpanded;

	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		statesExpanded = 1;
		long start = System.currentTimeMillis();

		int alpha = Integer.MIN_VALUE;
		int beta = Integer.MAX_VALUE;
		Move bestMove = null;
		Map<Move,List<MachineState>> legalMoves = gameTree.get(getCurrentState());
		for (Move move : legalMoves.keySet()) {
			int value = minScore(getCurrentState(), getRole(), move, alpha, beta);
			if (value > alpha) {
				alpha = value;
				bestMove = move;
			}
			if (beta <= alpha) {
				break;
			}
		}

		long stop = System.currentTimeMillis();		
		notifyObservers(new MinimaxMoveSelectionEvent(bestMove, stop - start, statesExpanded, false));
		
		return bestMove;		
	}
	
	private int maxScore (MachineState state, Role role, int alpha, int beta) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		statesExpanded++;
		if (getStateMachine().isTerminal(state)) {
			return getStateMachine().getGoal(state, role);
		}
		Map<Move,List<MachineState>> legalMoves = gameTree.get(state);
		for (Move move : legalMoves.keySet()) {
			alpha = Math.max(alpha, minScore(state, role, move, alpha, beta));
			if (beta <= alpha) {
				break;
			}
		}
		return alpha;
	}

	private int minScore (MachineState state, Role role, Move move, int alpha, int beta) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		Map<Move,List<MachineState>> legalMoves = gameTree.get(state);
		for (MachineState nextState : legalMoves.get(move)) {
			beta = Math.min(beta, maxScore(nextState, role, alpha, beta));
			if (beta <= alpha) {
				break;
			}
		}
		return beta;
	}

	
	@Override
	public void stateMachineStop() {
		
	}

	/**
	 * Uses a ProverStateMachine
	 */
	@Override
	public StateMachine getInitialStateMachine() {
		return new ProverStateMachine();
	}
	@Override
	public String getName() {
		return "AlphabetaExplicitGamer";
	}

	@Override
	public DetailPanel getDetailPanel() {
		return new MinimaxDetailPanel();
	}


}
