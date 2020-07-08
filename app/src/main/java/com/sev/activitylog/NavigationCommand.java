package com.sev.activitylog;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Stack;

public class NavigationCommand {
    protected NavigationAction action;
    protected AppCompatActivity destActivity;
    protected Object[] destState;

    /**
     * Creates command to return to this position
     * @param currentActivity
     * @param currentState
     * @param action the code that should be executed to return this activity to its current state
     */
    public NavigationCommand(AppCompatActivity currentActivity, Object[] currentState, NavigationAction action){
        this.destActivity = currentActivity;
        this.destState = currentState;
        this.action = action;
    }
    public NavigationCommand(NavigationAction action){
        this.action = action;
    }
    public NavigationCommand(NavigationAction action, AppCompatActivity destActivity){
        this(action);
        this.destActivity = destActivity;
    }
    public void setDestActivity(AppCompatActivity activity){
        destActivity = activity;
    }
    public void setDestState(Object[] state){
        destState = state;
    }
    public void goTo(AppCompatActivity currentActivity){
        action.navigate(currentActivity, destActivity, destState);
    }
    public Object[] getState() {return destState;}

}
interface NavigationAction {
    public void navigate(AppCompatActivity fromActivity, AppCompatActivity destActivity, Object[] dstState);
}
class NavigationSingleton {
    private static NavigationSingleton instance;
    public static NavigationSingleton getInstance(){
        if(instance == null)
            instance = new NavigationSingleton();
        return instance;
    }
    private NavigationSingleton(){
        navStack = new Stack<>();
    };
    private Stack<NavigationCommand> navStack;
    public void pushState(NavigationCommand goToHere){
        navStack.push(goToHere);
    }
    public void goBack(AppCompatActivity currentActivity){
        if(!navStack.empty()) {
            NavigationCommand last = navStack.peek();
            navStack.pop();
            last.goTo(currentActivity);
        }
    }
    public boolean empty() {return navStack.empty();}
}
