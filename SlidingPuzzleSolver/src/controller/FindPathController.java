package controller;

import java.awt.event.ActionListener;
import java.util.Stack;

import javax.swing.Timer;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import model.algorithm.AStar;
import model.algorithm.Algorithm;
import model.algorithm.AlgorithmType;
import model.algorithm.BreathFirstSearch;
import model.algorithm.IterativeDeepeningAStar;
import model.algorithm.TileByTile;
import model.exceptions.PuzzleNumbersException;
import model.heuristic.Hamming;
import model.heuristic.HeuristicType;
import model.heuristic.IHeuristic;
import model.heuristic.LinearConflict;
import model.heuristic.Manhatten;
import model.state.State;
import model.state.StateGenerator;
import view.MainView;
import javafx.scene.control.ButtonType;

public class FindPathController implements EventHandler<ActionEvent> {
	private boolean isThreadRunning=false;
	private Stack<State> path=new Stack<State>();
	private Timer timer;
	
	@Override
	public void handle(ActionEvent arg0) {
		try
		{	
			int[][] initalBoard=MainView.getInstance().getTfsStart();
			int speed=MainView.getInstance().getCmbSpeed();
			boolean usesImage=MainView.getInstance().getCbImage();
			MainView.getInstance().setPreviousStartBoard(initalBoard);
						
			State initialState=new State(initalBoard,null);
			State goalState=StateGenerator.makeGoalState();
			
			if(!validNumbers(initialState))
			{
				int maxNum=State.getRows()*State.getColumns()-1;
				throw new PuzzleNumbersException(maxNum);
			}
			
			if(usesImage)
				MainView.getInstance().setImages(initalBoard);
				
			State.setHeuristic(selectedHeuristic(initialState,goalState));
			Algorithm algorithm=selectedAlgorithm(initialState, goalState);
			
			algorithm.addSubscriber(MainView.getInstance());
			
			ActionListener listener = new ActionListener(){
				@Override
				public void actionPerformed(java.awt.event.ActionEvent e) {	
					//On first tick it will start solving
					if(!isThreadRunning)
					{
						path=algorithm.solve();
						isThreadRunning=true;
					}
					
					//When it solves it, update GUI
					if(!path.isEmpty())
					{
						//Updates GUI on main thread
						Platform.runLater(new Runnable() {
						    @Override public void run() {				    							    	
						    	if(usesImage)
						    		MainView.getInstance().setImages(path.peek().getBoard());
						    	MainView.getInstance().setTfsStart(path.peek().getBoard());
						    	path.pop();
								
								int numOfSteps=algorithm.getNumOfSteps();
								int nodeExplored=algorithm.getNodeExplored();
								MainView.getInstance().setTfLvl(numOfSteps);
								MainView.getInstance().setTfNode(nodeExplored);
								
								//When updating is finished, stop timer
								if(path.isEmpty())
								{
									isThreadRunning=false;
									timer.stop();
									MainView.getInstance().enableOrDisableCommands(true);
								}
						}});			
					}
				}	
		    };
		    
		    timer= new Timer(speed, listener);  
		    timer.start();
		    MainView.getInstance().enableOrDisableCommands(false);
		}
		catch(PuzzleNumbersException pne)
		{
			new Alert(AlertType.INFORMATION,pne.getMessage(),ButtonType.OK).show();
		}
		catch(NumberFormatException nfe)
		{
			new Alert(AlertType.INFORMATION,"Please insert only numbers",ButtonType.OK).show();
		}
	}
	
	private boolean validNumbers(State initialState) {
		int maxNum=State.getRows()*State.getColumns()-1;
		int sumOfNums=maxNum*(maxNum+1)/2;
		int sumTemp=0;
		for(int i=0;i<State.getRows();i++)
			for(int j=0;j<State.getColumns();j++)
				sumTemp+=initialState.getBoard()[i][j];
		return sumOfNums==sumTemp;
	}
	
	public Algorithm selectedAlgorithm(State initialState,State goalState)
	{
		Algorithm alg=null;
		AlgorithmType algType=MainView.getInstance().getCmbAlg();
		switch(algType)
		{
			case BFS: alg=new BreathFirstSearch(initialState, goalState);break;
			case AStar: alg=new AStar(initialState, goalState);break;
			case IDAStar: alg=new IterativeDeepeningAStar(initialState, goalState);break;
			case TileByTile: alg=new TileByTile(initialState, goalState);break;
		}
		return alg;
	}
	
	private IHeuristic selectedHeuristic(State initialState,State goalState) {
		IHeuristic heur=null;
		HeuristicType heurType=MainView.getInstance().getCmbHeur();
		switch(heurType)
		{
			case Manhatten: heur=new Manhatten();break;
			case Hamming: heur=new Hamming();break;
			case LinearConflict: heur=new LinearConflict();break;
		}
		return heur;
	}
}