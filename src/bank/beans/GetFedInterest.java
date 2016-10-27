package bank.beans;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.StreamMessage;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class GetFedInterest implements MessageListener {

	
	private TopicSession pubSession;
	private TopicSubscriber subscriber;
	private TopicConnection tConnection;
	
	private double fedInterest = 0.0;
	private boolean loaded = false;
	
	public double getFedInterest(){
		while(!loaded){	}
		return fedInterest;
	}
	
	public GetFedInterest(){
		Context ctx;
		try {
			ctx = new InitialContext();
			setupTopicConnection(ctx);
		} catch (NamingException | JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	public void onMessage(Message message) {

		try {
			StreamMessage msg = (StreamMessage) message;
			fedInterest = msg.getDoubleProperty("fed");
			loaded = true;
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void setupTopicConnection(Context ctx) throws NamingException, JMSException{
		
		TopicConnectionFactory tFactory = (TopicConnectionFactory) ctx.lookup("ECSU.CSC360.TCF");
		
		tConnection = tFactory.createTopicConnection();

		// Create the JMS Session
		pubSession = tConnection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
		
		Topic borrowTopic = (Topic)ctx.lookup("topic.fid.interest");
		
//		subscriber = pubSession.createSubscriber(borrowTopic, null, true);
		subscriber = pubSession.createDurableSubscriber(borrowTopic, "BankLender");
		
		subscriber.setMessageListener(this);
		
		// Now that setup is complete, start the Connection
		tConnection.start();
		
	}
}
