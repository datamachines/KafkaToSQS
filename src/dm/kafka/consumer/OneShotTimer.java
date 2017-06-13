package dm.kafka.consumer;


import java.util.Timer;
import java.util.TimerTask;
 

public class OneShotTimer extends TimerTask {
	DataReplaceXmit sender = null;
	Timer timer;
	
	public OneShotTimer(DataReplaceXmit inf, int timeOut){
		sender = inf;
		timer = new Timer(true);
		timer.schedule(this, timeOut*60000);
	}
	

	
	@Override
    public void run() {
        completeTask();
    }

    private void completeTask() {
    	timer.cancel();
    	if(sender != null){
    		sender.dataTimeOut();
    	}
    }
    
//    public static void main(String args[]){
//    	DMTimer timerTask = new DMTimer(null,1);
//
//        //cancel after sometime
//        try {
//        	while(true)
//            Thread.sleep(120000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//
//        /*System.out.println("TimerTask cancelled");
//        try {
//            Thread.sleep(30000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }*/
//    }
}
