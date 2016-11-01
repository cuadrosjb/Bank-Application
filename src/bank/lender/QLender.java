package bank.lender;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import bank.beans.BankRules;
import bank.beans.LoanProposalData;

public class QLender implements MessageListener {

	private QueueConnection qConnect = null;
	private QueueSession qSession = null;

	
	private TopicSession subSession;
	private TopicSubscriber subscriber;
	private TopicConnection tConnection;
	
	private final static String TOPICCONNECTIONFACTION = "ECSU.CSC360.TCF";
	private final static String QUEUECONNECTIONFACTION = "ECSU.CSC360.QCF";
	private final static String LOANREQUEST = "topic.loan.request";
	private final static String DURABLESUBSCRIBER = "bank.10253875";
	
	private Map<String, LoanProposalData> allLoanProposals;

	
	public void setupTopicConnection(Context ctx) throws NamingException, JMSException{
		
		TopicConnectionFactory tFactory = (TopicConnectionFactory) ctx.lookup(TOPICCONNECTIONFACTION);
		
		tConnection = tFactory.createTopicConnection();

		subSession = tConnection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
		
		Topic topic = (Topic)ctx.lookup(LOANREQUEST);
		
		subscriber = subSession.createDurableSubscriber(topic, DURABLESUBSCRIBER);
		
		subscriber.setMessageListener(this);
		
		tConnection.start();
	}

	public void createQueueSession(Context ctx) throws NamingException, JMSException {

		QueueConnectionFactory qFactory = (QueueConnectionFactory) ctx.lookup(QUEUECONNECTIONFACTION);

		qConnect = qFactory.createQueueConnection();

		qSession = qConnect.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

	}
	

	public QLender() {
		try {
			Context ctx = new InitialContext();
			allLoanProposals = new HashMap<String, LoanProposalData>();
			createQueueSession(ctx);
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
		System.out.println("onMessage(message)");
		try {
				
			if(message instanceof MapMessage){
				//System.out.println("We are recieved a Loan Request!"+ message.get);
				MapMessage msg = (MapMessage) message;
				
				ObjectMessage loanReply = qSession.createObjectMessage();
				
				LoanProposalData loanProp = new BankRules(
						msg.getString("Borrower"),
						msg.getDouble("Salary"),
						msg.getDouble("LoanAmount"),
						msg.getInt("PaymentScheme")).canWeMakeItWork();
				
				loanReply.setObject(loanProp);
				System.out.println(loanReply.toString());
				allLoanProposals.put(message.getJMSMessageID(), loanProp);
				
				loanReply.setJMSExpiration(new Date().getTime() + 10000*60*60*24*7);
	
	
				loanReply.setJMSCorrelationID(message.getJMSMessageID());
				
				QueueSender qSender = qSession.createSender((Queue) message.getJMSReplyTo());
				System.out.println("\nSending loan reply");
				qSender.send(loanReply);
	
				System.out.println("\nWaiting for loan requests...");
			}else if(message instanceof TextMessage){
				System.out.println("This Proposal was " + ((TextMessage)message).getText());
				allLoanProposals.get(message.getJMSCorrelationID()).toString();
				System.out.println("--------------------END---------------------------------");
				
				
			}else{
				if(message!=null){
					System.out.println(message.toString());
				}else{
					System.out.println("Message could not be CAST...");
				}
			}

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

		QLender lender = new QLender();

		try {
			// Run until enter is pressed
			BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
			System.out.println("Cuadros National Bank \rWaiting for requests...");
			System.out.println("Press enter to quit application");
			stdin.readLine();
			lender.exit();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
}
