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

public final class AlphabetaGamer extends StateMachineGamer {
	
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
		statesExpanded = 1;
		long start = System.currentTimeMillis();

		int alpha = 0;
		int beta = 100;
		Move bestMove = null;
		List<Move> legalMoves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
		for (Move move : legalMoves) {
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
		List<Move> legalMoves = getStateMachine().getLegalMoves(state, role);
		for (Move move : legalMoves) {
			alpha = Math.max(alpha, minScore(state, role, move, alpha, beta));
			if (beta <= alpha) {
				break;
			}
		}

		return alpha;
	}

	private int minScore (MachineState state, Role role, Move move, int alpha, int beta) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		List<List<Move>> legalMoves = getStateMachine().getLegalJointMoves(state, role, move);
		for (List<Move> jointMove : legalMoves) {
			MachineState nextState = getStateMachine().getNextState(state, jointMove);
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
		return "AlphabetaGamer";
	}

	@Override
	public DetailPanel getDetailPanel() {
		return new MinimaxDetailPanel();
	}


}
