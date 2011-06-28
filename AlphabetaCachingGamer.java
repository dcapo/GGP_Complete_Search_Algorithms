package player.gamer.statemachine.completesearch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

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

public final class AlphabetaCachingGamer extends StateMachineGamer {
	
	private Map<MachineState, Integer> valueCache;
	private Random random;
	
	@Override
	public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		valueCache = new HashMap<MachineState, Integer>();
		random = new Random();
	}
	
	private int statesExpanded;
	private long timeout;
	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		statesExpanded = 1;
		this.timeout = timeout;
		long start = System.currentTimeMillis();
		boolean timedOut = false;
		
		int alpha = 0;
		int beta = 100;
		Move bestMove = null;
		List<Move> legalMoves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
		for (Move move : legalMoves) {
			int value = minScore(getCurrentState(), getRole(), move, alpha, beta);
			if (value == -1) {
				timedOut = true;
				break;
			}
			if (value > alpha) {
				alpha = value;
				bestMove = move;
			}
			if (beta <= alpha) {
				break;
			}
		}
		
		if (bestMove == null) {
			bestMove = legalMoves.get(random.nextInt(legalMoves.size()));
		}
		long stop = System.currentTimeMillis();		
		notifyObservers(new MinimaxMoveSelectionEvent(bestMove, stop - start, statesExpanded, timedOut));

		return bestMove;
	}
	
	private int maxScore (MachineState state, Role role, int alpha, int beta) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		if (valueCache.containsKey(state)) {
			return valueCache.get(state);
		}
		statesExpanded++;
		if (getStateMachine().isTerminal(state)) {
			return getStateMachine().getGoal(state, role);
		}
		List<Move> legalMoves = getStateMachine().getLegalMoves(state, role);
		for (Move move : legalMoves) {
			if (outOfTime()) {
				return -1;
			}
			alpha = Math.max(alpha, minScore(state, role, move, alpha, beta));
			if (beta <= alpha) {
				break;
			}
		}
		valueCache.put(state, alpha);
		return alpha;
	}

	private int minScore (MachineState state, Role role, Move move, int alpha, int beta) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		List<List<Move>> legalMoves = getStateMachine().getLegalJointMoves(state, role, move);
		for (List<Move> jointMove : legalMoves) {
			if (outOfTime()) {
				return -1;
			}
			MachineState nextState = getStateMachine().getNextState(state, jointMove);
			beta = Math.min(beta, maxScore(nextState, role, alpha, beta));
			if (beta <= alpha) {
				break;
			}
		}
		return beta;
	}
	
	private long WRAP_UP_TIME = 100;
	private boolean outOfTime() {
		return System.currentTimeMillis() + WRAP_UP_TIME > timeout;
	}
	
	@Override
	public void stateMachineStop() {
		// Do nothing.
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
		return "BiWinningGamer";
	}

	@Override
	public DetailPanel getDetailPanel() {
		return new MinimaxDetailPanel();
	}


}
