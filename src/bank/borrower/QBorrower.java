package bank.borrower;

import java.awt.SecondaryLoop;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.StreamMessage;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import bank.beans.LoanProposalData;

public class QBorrower implements MessageListener {

	private final static String QUEUECONNECTIONFACTORY = "ECSU.CSC360.QCF";
	private final static String TOPICCONNECTIONFACTORY = "ECSU.CSC360.TCF";

	private final static String RESPONSEQUEUE = "queue.10253875";
	private final static String LOANTOPIC = "topic.loan.request";

	
	
	private QueueConnection qConnect;
	private QueueSession qSession;
	private Queue responseQ;
	private MessageConsumer msgConsumer;
	private MessageProducer msgProducer;
	// Variables for topic
	private TopicSession pubSession;
	private TopicPublisher publisher;
	private TopicConnection tConnection;
	
	private Map<String,Message> allLoanProposal;

	public QBorrower() {
		try {
			allLoanProposal = new HashMap<String, Message>();
			Context ctx = new InitialContext();
			startQueueConnection(ctx);
			startTopicConnection(ctx);

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public void startTopicConnection(Context ctx) {

		try {

			TopicConnectionFactory tFactory = (TopicConnectionFactory) ctx.lookup(TOPICCONNECTIONFACTORY);

			tConnection = tFactory.createTopicConnection();

			pubSession = tConnection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

			publisher = pubSession.createPublisher((Topic) ctx.lookup(LOANTOPIC));

			tConnection.start();

		} catch (JMSException jmse) {
			jmse.printStackTrace();
			System.exit(1);
		} catch (NamingException jne) {
			jne.printStackTrace();
			System.exit(1);
		}

	}

	public void startQueueConnection(Context ctx) {
		try {
			QueueConnectionFactory qFactory = (QueueConnectionFactory) ctx.lookup(QUEUECONNECTIONFACTORY);

			qConnect = qFactory.createQueueConnection();

			qSession = qConnect.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

			responseQ = (Queue) ctx.lookup(RESPONSEQUEUE);

			msgConsumer = qSession.createConsumer(responseQ);
						
			msgConsumer.setMessageListener(this);
			
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
			System.out.println("borrower:"+borrower+"\rsalary:"+salary+"\rloanAmt:"+loanAmt+"\rpaymentScheme:"+paymentScheme);
			MapMessage topicmsg = pubSession.createMapMessage();
			topicmsg.setString("Borrower", borrower);
			topicmsg.setDouble("Salary", salary);
			topicmsg.setDouble("LoanAmount", loanAmt);
			topicmsg.setInt("PaymentScheme", paymentScheme);
			topicmsg.setJMSReplyTo(responseQ);

			//topicmsg.setJMSExpiration(new Date().getTime() + 30000); // Expires in 30 seconds

			publisher.publish(topicmsg);

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

	public void sendAcceptedLoan(String jmsMessageId) {
		try{
			//LoanProposalData acceptedLoan =  (LoanProposalData)((ObjectMessage)allLoanProposal.get(jmsMessageId)).getObject();
			//allLoanProposal.get(jmsMessageId).getJMSReplyTo();
			
			Iterator ito = allLoanProposal.entrySet().iterator();
		    while (ito.hasNext()) {
		        Map.Entry<String, LoanProposalData> element = (Map.Entry<String, LoanProposalData> )ito.next();
		        if(element.getKey().equalsIgnoreCase(jmsMessageId)){
		        	Message acceptLoan = qSession.createTextMessage("ACCEPT");
					
					acceptLoan.setJMSReplyTo(allLoanProposal.get(jmsMessageId).getJMSReplyTo());
					acceptLoan.setJMSMessageID(allLoanProposal.get(jmsMessageId).getJMSCorrelationID());
					msgProducer = qSession.createProducer(allLoanProposal.get(jmsMessageId).getJMSReplyTo());
					
					msgProducer.send(acceptLoan);
		        }else{
		        	Message rejectLoan = qSession.createTextMessage("REJECT");
					
		        	rejectLoan.setJMSReplyTo(allLoanProposal.get(jmsMessageId).getJMSReplyTo());
		        	rejectLoan.setJMSMessageID(allLoanProposal.get(jmsMessageId).getJMSCorrelationID());
					msgProducer = qSession.createProducer(allLoanProposal.get(jmsMessageId).getJMSReplyTo());
					
					msgProducer.send(rejectLoan);
		        }
		       
		    }
			
		}catch(Exception jms){
			jms.printStackTrace();
		}

	}

	@Override
	public void onMessage(Message msg) {
		System.out.println("onMessage()");
		try {
			if(msg!=null){
				if(msg instanceof  ObjectMessage){
					System.out.println("---------------------------------------------------------------------------");
					allLoanProposal.put(msg.getJMSMessageID(), msg);
					System.out.println(msg.toString());
					LoanProposalData lpd =  (LoanProposalData) ((ObjectMessage) msg).getObject();
					System.out.println(lpd.toString());
					System.out.println("If you want to accept this loan, please type \"" + msg.getJMSMessageID()+ "\"");
					
					System.out.println("---------------------------------------------------------------------------");
				}else if (msg instanceof TextMessage){
					       System.out.println(((TextMessage)msg).getText());
						
				}else{
					System.out.println(msg.toString());
				}
			}
		} catch (JMSException e) {

			e.printStackTrace();
		}
	}
	
	
	
	public static void main(String argv[]) {

		QBorrower borrower = new QBorrower();

		try {
			// Read all standard input and send it as a message
			BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
			System.out.println("Borrower Application Started");
			System.out.println("Press enter to quit application");
			System.out.println("Enter: Full Name, Salary, Loan_Amount, Loan Term(in years)");
			System.out.println("\ne.g. Jeffrey Cuadros, 50000, 120000, 5");

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
			
			String jmsMsgId = stdin.readLine();
			if (loanRequest == null || loanRequest.trim().length() <= 0) {
				borrower.exit();
			}
			borrower.sendAcceptedLoan(jmsMsgId.trim());
			
			

		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
}
