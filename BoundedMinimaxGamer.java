package player.gamer.statemachine.completesearch;

import java.util.List;

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

public final class BoundedMinimaxGamer extends StateMachineGamer {
		
	/**
	 * Does nothing for the metagame
	 */
	@Override
	public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		// Do nothing.
	}

	private int statesExpanded;
	/**
	 * Selects the first legal move
	 */
	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		long start = System.currentTimeMillis();		
		statesExpanded = 1;

		int bestValue = Integer.MIN_VALUE;
		Move bestMove = null;
		List<Move> legalMoves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
		for (Move move : legalMoves) {
			int value = minScore(getCurrentState(), getRole(), move);
			if (value > bestValue) {
				bestValue = value;
				bestMove = move;
			}
			if (value == 100) {
				break;
			}
		}

		long stop = System.currentTimeMillis();		
		notifyObservers(new MinimaxMoveSelectionEvent(bestMove, stop - start, statesExpanded, false));

		return bestMove;		
	}
	
	private int maxScore (MachineState state, Role role) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		statesExpanded++;
		if (getStateMachine().isTerminal(state)) {
			return getStateMachine().getGoal(state, role);
		}
		int value = Integer.MIN_VALUE;
		List<Move> legalMoves = getStateMachine().getLegalMoves(state, getRole());
		for (Move move : legalMoves) {
			value = Math.max(value, minScore(state, role, move));
			if (value == 100) {
				break;
			}
		}
		return value;
	}

	private int minScore (MachineState state, Role role, Move move) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		int value = Integer.MAX_VALUE;
		List<List<Move>> legalMoves = getStateMachine().getLegalJointMoves(state, role, move);
		for (List<Move> jointMove : legalMoves) {
			MachineState nextState = getStateMachine().getNextState(state, jointMove);
			value = Math.min(value, maxScore(nextState, role));
			if (value == 0) {
				break;
			}
		}
		return value;
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
		return "BoundedMinimaxGamer";
	}

	@Override
	public DetailPanel getDetailPanel() {
		return new MinimaxDetailPanel();
	}


}
