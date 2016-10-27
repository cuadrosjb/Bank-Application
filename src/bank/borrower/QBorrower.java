package bank.borrower;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.StringTokenizer;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class QBorrower {

	private String queuecf = "ECSU.CSC360.QCF";
	private String topiccf = "ECSU.CSC360.TCF";
	private String requestq = "queue.10253875";
	private String responseq = "queue.sample";
	private String loanTopic = "topic.loan.request";
	
	
	private QueueConnection qConnect = null;
	private QueueSession qSession = null;
	private Queue responseQ = null;
	private Queue requestQ = null;
	//Variables for topic
	private TopicSession pubSession;
	private TopicPublisher publisher;
	private TopicConnection tConnection;

	

	public QBorrower() {
		try {
			
			Context ctx = new InitialContext();
			
			startQueueConnection(ctx);
			startTopicConnection(ctx);
			
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public void startTopicConnection(Context ctx){
		

		try {
			
			TopicConnectionFactory tFactory = (TopicConnectionFactory) ctx.lookup(topiccf);
			
			tConnection = tFactory.createTopicConnection();

			pubSession = tConnection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
			
			
			publisher = pubSession.createPublisher((Topic)ctx.lookup(loanTopic));

			tConnection.start();

		} catch (JMSException jmse) {
			jmse.printStackTrace();
			System.exit(1);
		} catch (NamingException jne) {
			jne.printStackTrace();
			System.exit(1);
		}
		
		
	}
	
	public void startQueueConnection(Context ctx){
		try {
			// Connect to the provider and get the JMS connection
			QueueConnectionFactory qFactory = (QueueConnectionFactory) ctx .lookup(queuecf);
			
			qConnect = qFactory.createQueueConnection();
			
			// Create the JMS Session
			qSession = qConnect.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
			
			// Lookup the request and response queues
			requestQ = (Queue) ctx.lookup(requestq);
			responseQ = (Queue) ctx.lookup(responseq);

			// Now that setup is complete, start the Connection
			qConnect.start();
			

		} catch (JMSException jmse) {
			jmse.printStackTrace();
			System.exit(1);
		} catch (NamingException jne) {
			jne.printStackTrace();
			System.exit(1);
		}
	}
	
	

	private void sendLoanRequest(String borrower, double salary, double loanAmt, int paymentScheme) {
		try {
			
			MapMessage topicmsg = pubSession.createMapMessage();
			topicmsg.setString("Borrower", borrower);
			topicmsg.setDouble("Salary", salary);
			topicmsg.setDouble("LoanAmount", loanAmt);
			topicmsg.setInt("PaymentScheme", paymentScheme);
			topicmsg.setJMSReplyTo(responseQ);

			topicmsg.setJMSExpiration(new Date().getTime() + 30000); //Expires in 30 seconds 
			
			publisher.publish(topicmsg);
			
			

			// Wait to see if the loan request was accepted or declined
//			String filter = "JMSCorrelationID = '" + topicmsg.getJMSMessageID() + "'";
//			QueueReceiver qReceiver = qSession.createReceiver(responseQ, filter);
//			TextMessage tmsg = (TextMessage) qReceiver.receive(30000);
//			if (tmsg == null) {
//				System.out.println("Lender not responding");
//			} else {
//				System.out.println("Loan request was " + tmsg.getText());
//			}
			//The borrower then chooses the best offer and sends the ACCEPT text message back to the lender. 
			//The JMSCorrelationID of the ACCEPT message is set to the lender’s loan proposal message ID.
			

		} catch (JMSException jmse) {
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
		
		QBorrower borrower = new QBorrower();

		try {
			// Read all standard input and send it as a message
			BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
			System.out.println("QBorrower Application Started");
			System.out.println("Press enter to quit application");
			System.out.println("Enter: Full Name, Salary, Loan_Amount, Loan Term(in terms)");
			System.out.println("\ne.g. Jeffrey Cuadros, 50000, 120000, 5");

			while (true) {
				System.out.print("> ");

				String loanRequest = stdin.readLine();
				if (loanRequest == null || loanRequest.trim().length() <= 0) {
					borrower.exit();
				}

				// Parse the deal description
				StringTokenizer st = new StringTokenizer(loanRequest, ",");
				String borrowerName = String.valueOf(st.nextToken().trim());
				double salary = Double.valueOf(st.nextToken().trim()).doubleValue();
				double loanAmt = Double.valueOf(st.nextToken().trim()).doubleValue();
				int term = Integer.valueOf(st.nextToken().trim()).intValue();

				borrower.sendLoanRequest(borrowerName, salary, loanAmt, term);
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
}
