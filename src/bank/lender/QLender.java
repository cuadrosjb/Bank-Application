package bank.lender;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import bank.beans.BankRules;

public class QLender implements MessageListener {

	private QueueConnection qConnect = null;
	private QueueSession qSession = null;
	private Queue requestQ = null;
	
	private TopicSession pubSession;
	private TopicSubscriber subscriber;
	private TopicConnection tConnection;
	
	
	
	
	
	public void setupTopicConnection(Context ctx) throws NamingException, JMSException{
		
		TopicConnectionFactory tFactory = (TopicConnectionFactory) ctx.lookup("ECSU.CSC360.TCF");
		
		tConnection = tFactory.createTopicConnection();

		// Create the JMS Session
		pubSession = tConnection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
		
		Topic borrowTopic = (Topic)ctx.lookup("topic.loan.request");
		
//		subscriber = pubSession.createSubscriber(borrowTopic, null, true);
		subscriber = pubSession.createDurableSubscriber(borrowTopic, "BankLender");
		
		subscriber.setMessageListener(this);
		
		// Now that setup is complete, start the Connection
		tConnection.start();
		
	}
	
	public void setupQueueConnection(String queuecf, Context ctx, String requestQueue) throws NamingException, JMSException{
		
		QueueConnectionFactory qFactory = (QueueConnectionFactory) ctx.lookup(queuecf);
		
		qConnect = qFactory.createQueueConnection();

		// Create the JMS Session
		qSession = qConnect.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

		// Lookup the request queue
		requestQ = (Queue) ctx.lookup(requestQueue);

		// Now that setup is complete, start the Connection
		qConnect.start();

		// Create the message listener
		QueueReceiver qReceiver = qSession.createReceiver(requestQ);

		qReceiver.setMessageListener(this);

		System.out.println("Waiting for loan requests...");
	}
	

	public QLender(String queuecf, String requestQueue) {
		try {
			/*Create a new topic subscriber and queue*/
			
			Context ctx = new InitialContext();
			
			setupQueueConnection(queuecf, ctx, requestQueue);
			setupTopicConnection(ctx);

		} catch (JMSException jmse) {
			jmse.printStackTrace();
			System.exit(1);
		} catch (NamingException jne) {
			jne.printStackTrace();
			System.exit(1);
		}
	}

	public void onMessage(Message message) {

		try {
			MapMessage msg = (MapMessage) message;
			
			Message loanReply = qSession.createObjectMessage();
			
			loanReply.setObjectProperty("LoanProposal", new BankRules(
					msg.getString("Borrower"),
					msg.getDouble("Salary"),
					msg.getDouble("LoanAmount"),
					msg.getInt("PaymentScheme")).canWeMakeItWork());
			loanReply.setJMSExpiration(new Date().getTime() + 10000*60*60*24*7);


			loanReply.setJMSCorrelationID(message.getJMSMessageID());
			
			QueueSender qSender = qSession.createSender((Queue) message.getJMSReplyTo());
			qSender.send(loanReply);

			System.out.println("\nWaiting for loan requests...");

		} catch (JMSException jmse) {
			jmse.printStackTrace();
			System.exit(1);
		} catch (Exception jmse) {
			jmse.printStackTrace();
			System.exit(1);
		}
	}

	private void exit() {
		try {
			qConnect.close();
		} catch (JMSException jmse) {
			jmse.printStackTrace();
		}
		System.exit(0);
	}

	public static void main(String argv[]) {
		String queuecf = null;
		String requestq = null;

		queuecf = "ECSU.CSC360.QCF";
		requestq = "queue.10253875";

		QLender lender = new QLender(queuecf, requestq);

		try {
			// Run until enter is pressed
			BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
			System.out.println("QLender application started");
			System.out.println("Press enter to quit application");
			stdin.readLine();
			lender.exit();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
}
